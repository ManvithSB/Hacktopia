package com.scamsheild.app

import android.content.Context
import android.content.SharedPreferences

object LanguagePrefs {
    private const val PREFS_NAME = "scamshield_lang_prefs"
    private const val KEY_LANGUAGE = "app_language"

    // Default language is English
    fun getLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, "en") ?: "en"
    }

    fun setLanguage(context: Context, languageCode: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply()
    }
}
