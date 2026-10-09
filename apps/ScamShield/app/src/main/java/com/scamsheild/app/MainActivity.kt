package com.scamsheild.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.scamsheild.app.ui.theme.ScamSheildTheme

class MainActivity : ComponentActivity() {

    /**
     * Launcher for the POST_NOTIFICATIONS runtime permission dialog (Android 13+).
     * The result is intentionally ignored — we check permission again in
     * ScamShieldNotifier before posting any notification, and the UI will
     * display a clear message if permission is not granted.
     */
    private val requestNotifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            // Result handled gracefully in ScamShieldNotifier.canPostNotifications()
        }

    override fun attachBaseContext(newBase: Context) {
        val lang = LanguagePrefs.getLanguage(newBase)
        val locale = java.util.Locale(lang)
        java.util.Locale.setDefault(locale)
        val config = android.content.res.Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request POST_NOTIFICATIONS at launch on Android 13+ (API 33).
        // We do this early so the user can grant permission before
        // the automatic protection feature is ever enabled.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                requestNotifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Create the warning notification channel now so it's ready
        // before the user enables auto-protection.
        ScamShieldNotifier.createChannel(this)

        setContent {
            ScamSheildTheme {
                ScamShieldScreen()
            }
        }
    }
}