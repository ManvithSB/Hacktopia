package com.scamsheild.app

import android.content.Context
import android.content.SharedPreferences

/**
 * SmsProtectionPrefs
 * ──────────────────
 * Stores whether the automatic SMS protection feature is enabled.
 * Uses the app's private SharedPreferences — no sensitive data is persisted here.
 *
 * PROTOTYPE NOTE: This feature is a local hackathon prototype.
 * It is NOT eligible for Google Play Store distribution under the current
 * notification-enhancement use-case policy without being the default SMS handler.
 */
object SmsProtectionPrefs {

    private const val PREF_FILE   = "scamshield_sms_prefs"
    private const val KEY_ENABLED = "sms_auto_protection_enabled"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLED, false)   // off by default

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }
}
