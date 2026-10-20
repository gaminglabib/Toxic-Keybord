package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ToxicDarkColorScheme = darkColorScheme(
    primary = ToxicEmeraldPrimary,
    onPrimary = Color(0xFF03140B),
    primaryContainer = ToxicEmeraldMuted,
    onPrimaryContainer = ToxicTextPrimary,
    secondary = ToxicEmeraldSoft,
    onSecondary = Color(0xFF04140B),
    secondaryContainer = ToxicGlassElevated,
    onSecondaryContainer = ToxicTextSecondary,
    tertiary = ToxicCrimsonAccent,
    onTertiary = Color.White,
    tertiaryContainer = ToxicCrimsonDark,
    onTertiaryContainer = ToxicCrimsonSoft,
    background = ToxicObsidianBlack,
    onBackground = ToxicTextPrimary,
    surface = ToxicDeepSurface,
    onSurface = ToxicTextPrimary,
    surfaceVariant = ToxicGlassCard,
    onSurfaceVariant = ToxicTextSecondary,
    outline = ToxicBorderSubtle,
    error = ToxicCrimsonAccent,
    onError = Color.White
)

private val ToxicLightColorScheme = lightColorScheme(
    primary = LightEmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = LightCard,
    onPrimaryContainer = LightTextPrimary,
    secondary = Color(0xFF00695C),
    onSecondary = Color.White,
    tertiary = ToxicCrimsonAccent,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightCard,
    onSurfaceVariant = LightTextSecondary,
    outline = Color(0xFFB2DFDB)
)

@Composable
fun ToxicKeyboardStudioTheme(
    useLightMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (useLightMode) ToxicLightColorScheme else ToxicDarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
