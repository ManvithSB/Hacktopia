package com.scamsheild.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * ScamShieldNotifier
 * ──────────────────
 * Responsible for posting ScamShield warning notifications to the user.
 *
 * Design constraints:
 *  - Never posts full-screen intents or overlay windows.
 *  - Checks POST_NOTIFICATIONS permission before posting on API 33+.
 *  - Never includes raw SMS message content in the notification.
 *  - Uses a stable numeric ID derived from the preview hash to prevent
 *    redundant alerts for the same content (complements in-memory dedup
 *    in SmsNotificationListenerService).
 */
object ScamShieldNotifier {

    private const val CHANNEL_ID   = "scamshield_warnings"
    private const val CHANNEL_NAME = "ScamShield Warnings"
    private const val CHANNEL_DESC = "Alerts when an incoming SMS preview appears suspicious"

    /** Ensure the notification channel exists (required for Android 8+). */
    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    /**
     * Returns true if the app is allowed to post notifications.
     * On API < 33 the permission is granted at install time; on 33+ it requires
     * a runtime grant.
     */
    fun canPostNotifications(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    /**
     * Posts a warning notification.
     *
     * @param context      Application context.
     * @param riskLabel    "SUSPICIOUS" or "HIGH RISK" — shown as the title.
     * @param reason       First reason returned by the backend (truncated to 120 chars).
     *                     Never raw SMS text.
     * @param score        Numeric risk score 0-100 from the backend.
     * @param notifId      Stable ID derived from the content hash, used for dedup.
     */
    fun postWarning(
        context   : Context,
        riskLabel : String,
        reason    : String,
        score     : Int,
        notifId   : Int
    ) {
        if (!canPostNotifications(context)) return

        createChannel(context)   // no-op if already created

        val safeReason = reason.take(120).ifBlank { "No specific reason returned." }

        val title = if (riskLabel.contains("HIGH", ignoreCase = true)) {
            context.getString(R.string.notif_title_high_risk)
        } else {
            context.getString(R.string.notif_title_suspicious)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("\uD83D\uDED1 $title")
            .setContentText(context.getString(R.string.notif_score, score) + " \u2022 $safeReason")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(context.getString(R.string.notif_score, score) + "\n\n$safeReason")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(notifId, notification)
    }
}
