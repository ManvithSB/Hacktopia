package com.scamsheild.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// ScamShield always uses a dark theme — no dynamic color, no light mode switch.
private val ScamShieldColorScheme = darkColorScheme(
    primary          = TealBright,
    onPrimary        = NavyDeep,
    primaryContainer = TealDark,
    onPrimaryContainer = TextPrimary,

    secondary        = TealMid,
    onSecondary      = NavyDeep,

    background       = NavyMid,
    onBackground     = TextPrimary,

    surface          = NavySurface,
    onSurface        = TextPrimary,
    surfaceVariant   = NavyCard,
    onSurfaceVariant = TextSecondary,

    outline          = BorderColor,
    error            = RiskHigh,
    onError          = TextPrimary,
)

@Composable
fun ScamSheildTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ScamShieldColorScheme,
        typography  = Typography,
        content     = content
    )
}