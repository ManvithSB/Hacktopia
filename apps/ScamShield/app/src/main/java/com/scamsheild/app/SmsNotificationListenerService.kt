package com.scamsheild.app

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * SmsNotificationListenerService
 * ──────────────────────────────────────────────────────────────────────────────
 * PROTOTYPE — Local hackathon demo only.
 *
 * Google Play Store policy: apps that are NOT the default SMS handler are
 * generally not permitted to use NotificationListenerService for SMS
 * scam-warning or notification-enhancement use cases. This service must NOT
 * be submitted to the Play Store without reviewing and complying with current
 * policy. It is safe for local/emulator/sideload testing only.
 *
 * What this service does:
 *  1. Receives every posted notification via onNotificationPosted().
 *  2. Immediately skips notifications that don't come from a recognised
 *     Android SMS messaging package.
 *  3. Skips our own ScamShield notifications, group-summary notifications,
 *     and notifications with no usable preview text.
 *  4. Uses an in-memory LRU-style set to suppress duplicate analyses for the
 *     same preview content.
 *  5. Posts the preview text to the existing POST /analyze endpoint (same
 *     contract used by the manual scan UI).
 *  6. If the backend returns SUSPICIOUS or HIGH_RISK, calls ScamShieldNotifier
 *     to post a warning.
 *  7. Never logs raw message text. Never stores phone numbers, OTPs, or
 *     message content to disk.
 *
 * Privacy design:
 *  - Only the notification PREVIEW (the short tickerText / bigText snippet
 *    shown in the status bar) is sent to the backend — not the full SMS body.
 *  - The preview is sent over the loopback network (10.0.2.2) to a local
 *    FastAPI server on the developer's own machine. No third-party cloud.
 *  - The preview text is not persisted locally.
 */
class SmsNotificationListenerService : NotificationListenerService() {

    // ── Constants ─────────────────────────────────────────────────────────────

    companion object {
        /** URL of the existing FastAPI backend (emulator loopback). */
        private const val BACKEND_URL = "http://10.149.198.250:8000/analyze"
        private const val CONNECT_TIMEOUT  = 5_000
        private const val READ_TIMEOUT     = 15_000

        /** How many content hashes we keep in memory for dedup. */
        private const val DEDUP_CACHE_SIZE = 50

        /**
         * Returns true if notification-listener access has been granted by the user.
         * Call this from the UI to decide whether to show the "Enable Access" prompt.
         */
        fun isListenerEnabled(context: Context): Boolean {
            val flat = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: return false
            val component = ComponentName(context, SmsNotificationListenerService::class.java)
            return flat.contains(component.flattenToString())
        }
    }

    // ── Coroutine scope (cancelled in onDestroy via SupervisorJob) ─────────────

    private val job   = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    /**
     * In-memory dedup set. Stores the hash of recently seen preview strings.
     * When it exceeds DEDUP_CACHE_SIZE the oldest entries are dropped.
     */
    private val recentHashes = ArrayDeque<Int>(DEDUP_CACHE_SIZE)

    // ── Service lifecycle ─────────────────────────────────────────────────────

    override fun onDestroy() {
        job.cancel()
        super.onDestroy()
    }

    // ── Core callback ─────────────────────────────────────────────────────────

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        // 1. Respect the user's toggle — if disabled, do nothing.
        if (!SmsProtectionPrefs.isEnabled(applicationContext)) return

        // 2. Ignore ScamShield's own notifications.
        if (sbn.packageName == applicationContext.packageName) return
        
        android.util.Log.d("ScamShield", "Notification posted: pkg=${sbn.packageName}")

        // 3. Only process notifications from recognised SMS apps.
        if (!SmsListenerLogic.isAllowedSmsPackage(sbn.packageName)) {
            android.util.Log.d("ScamShield", "Skipping: ${sbn.packageName} is not a recognised SMS app")
            return
        }

        // 4. Skip group-summary notifications (they don't carry per-message preview).
        val extras = sbn.notification?.extras ?: return
        val isGroupSummary = (sbn.notification.flags and
                android.app.Notification.FLAG_GROUP_SUMMARY) != 0
        if (isGroupSummary) {
            android.util.Log.d("ScamShield", "Skipping: Group summary notification")
            return
        }

        // 5. Extract preview text. Try several notification extras in order.
        val preview: String = extractPreview(extras) ?: run {
            android.util.Log.d("ScamShield", "Skipping: Could not extract preview text")
            return
        }

        // 6. Skip if preview is clearly non-actionable (too short, hidden, or a template).
        if (!SmsListenerLogic.isUsablePreview(preview)) {
            android.util.Log.d("ScamShield", "Skipping: Preview text is not actionable")
            return
        }

        // 7. Dedup: skip if we recently analysed the exact same preview.
        val hash = preview.hashCode()
        synchronized(recentHashes) {
            if (recentHashes.contains(hash)) {
                android.util.Log.d("ScamShield", "Skipping: Duplicate notification (hash=$hash)")
                return
            }
            if (recentHashes.size >= DEDUP_CACHE_SIZE) recentHashes.removeFirst()
            recentHashes.addLast(hash)
        }

        android.util.Log.d("ScamShield", "Queuing SMS analysis for hash=$hash")

        // 8. Send to backend asynchronously. Never block the main thread.
        scope.launch {
            analyzeAndNotify(preview, hash)
        }
    }

    // ── Helper: extract preview text from notification extras ─────────────────

    private fun extractPreview(extras: android.os.Bundle): String? {
        // BigText holds the longest available snippet; fall back to ContentText.
        val candidates = listOf(
            extras.getCharSequence("android.bigText"),
            extras.getCharSequence("android.text"),
            extras.getCharSequence("android.title")   // last resort — often sender name only
        )
        return candidates
            .firstOrNull { !it.isNullOrBlank() }
            ?.toString()
            ?.take(SmsListenerLogic.MAX_PREVIEW_LENGTH)
    }

    // ── Backend call + notification posting ───────────────────────────────────

    private suspend fun analyzeAndNotify(preview: String, contentHash: Int) {
        val result = withTimeoutOrNull(READ_TIMEOUT.toLong()) {
            try {
                callBackend(preview)
            } catch (e: Exception) {
                android.util.Log.e("ScamShield", "Backend request failed for hash=$contentHash", e)
                null
            }
        }

        // If the backend is unreachable or returns an error, do nothing —
        // we must never falsely claim a message is safe after a failed analysis.
        if (result == null) {
            android.util.Log.e("ScamShield", "Analysis timed out or failed for hash=$contentHash")
            return
        }

        android.util.Log.d("ScamShield", "Backend returned score=${result.score} for hash=$contentHash")

        val (score, riskLevel, firstReason) = result

        // Only warn the user if the backend confirms the message is risky.
        if (score >= SmsListenerLogic.WARN_SCORE_THRESHOLD) {
            // Notification ID: derive from content hash so we don't spam
            // duplicate alerts for the same preview text.
            val notifId = contentHash and Int.MAX_VALUE  // ensure positive

            ScamShieldNotifier.postWarning(
                context   = applicationContext,
                riskLabel = riskLevel,
                reason    = firstReason,
                score     = score,
                notifId   = notifId
            )
        }
    }

    // ── HTTP client — reuses the same contract as ScamShieldScreen.kt ─────────

    private suspend fun callBackend(preview: String): SmsListenerLogic.BackendResult =
        withContext(Dispatchers.IO) {
            val conn = (URL(BACKEND_URL).openConnection() as HttpURLConnection).apply {
                requestMethod  = "POST"
                connectTimeout = CONNECT_TIMEOUT
                readTimeout    = READ_TIMEOUT
                doOutput       = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
            }
            try {
                val body = JSONObject().apply {
                    put("message",  preview)
                    put("language", "en")
                }.toString()

                OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(body) }

                val code = conn.responseCode
                if (code != 200) throw Exception("HTTP $code")

                val responseText =
                    BufferedReader(InputStreamReader(conn.inputStream)).readText()

                SmsListenerLogic.parseBackendResponse(responseText)
            } finally {
                conn.disconnect()
            }
        }
}
