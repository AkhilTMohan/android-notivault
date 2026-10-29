package com.bc.notivault.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = VaultElectricCyan,
    onPrimary = Color(0xFF0B0F19),
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFE0F2FE),
    secondary = VaultRoyalBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF172554),
    onSecondaryContainer = Color(0xFFBAE6FD),
    background = VaultMidnightBg,
    onBackground = VaultTextPrimary,
    surface = VaultCardSurface,
    onSurface = VaultTextPrimary,
    surfaceVariant = VaultCardSurfaceVariant,
    onSurfaceVariant = VaultTextSecondary,
    outline = VaultCardBorder
)

private val LightColorScheme = lightColorScheme(
    primary = VaultRoyalBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0369A1),
    secondary = VaultCyanGlow,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF0F9FF),
    onSecondaryContainer = Color(0xFF0C4A6E),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFE2E8F0)
)

@Composable
fun NotivaultTheme(
    themeMode: String = "SYSTEM",
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        "LIGHT" -> false
        "DARK" -> true
        else -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
