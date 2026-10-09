package com.scamsheild.app

import org.json.JSONObject

/**
 * SmsListenerLogic
 * ─────────────────────────────────────────────────────────────────────────────
 * Pure, framework-free business logic extracted from SmsNotificationListenerService.
 * Kept in a standalone object so it can be tested with plain JVM unit tests
 * without instantiating the Android service class or mocking the framework.
 *
 * SmsNotificationListenerService delegates its logic calls here.
 */
object SmsListenerLogic {

    /** Risk score threshold at/above which we post a warning. Matches SUSPICIOUS band. */
    const val WARN_SCORE_THRESHOLD = 30

    /** Maximum preview text length sent to the backend. */
    const val MAX_PREVIEW_LENGTH = 500

    /**
     * Recognised AOSP / common SMS app package names.
     * Only notifications from these packages are processed.
     * WhatsApp, Telegram, etc. are intentionally excluded.
     */
    val ALLOWED_SMS_PACKAGES: Set<String> = setOf(
        "com.google.android.apps.messaging",
        "com.samsung.android.messaging",
        "com.android.mms",
        "com.android.messaging",
        "com.sonyericsson.conversations",
        "com.motorola.messaging"
    )

    data class BackendResult(
        val score      : Int,
        val riskLevel  : String,
        val firstReason: String
    )

    /**
     * Returns true if [text] is worth sending to the backend.
     * Rejects empty, too-short, and known hidden/placeholder previews.
     */
    fun isUsablePreview(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.length < 10) return false
        val lower = trimmed.lowercase()
        val ignoredPrefixes = listOf(
            "new message", "new sms", "1 new message", "2 new messages",
            "you have a new message", "notification content hidden"
        )
        if (ignoredPrefixes.any { lower.startsWith(it) && trimmed.length < 30 }) return false
        return true
    }

    /**
     * Parses the raw JSON response from POST /analyze into a [BackendResult].
     * Throws on malformed JSON (caller must handle).
     */
    fun parseBackendResponse(json: String): BackendResult {
        val obj     = JSONObject(json)
        val score   = obj.optInt("score", 0)
        val level   = obj.optString("risk_level", "SAFE").uppercase()
        val reasons = obj.optJSONArray("reasons")
        val first   = if (reasons != null && reasons.length() > 0)
            reasons.getString(0) else "No reason provided."
        return BackendResult(score, level, first)
    }

    /** Returns true if [packageName] is a recognised SMS app. */
    fun isAllowedSmsPackage(packageName: String): Boolean =
        packageName in ALLOWED_SMS_PACKAGES
}
