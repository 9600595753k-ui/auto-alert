package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = NavyBackground,
    primaryContainer = NavySurfaceHigh,
    onPrimaryContainer = ElectricCyan,
    secondary = EmeraldGreen,
    onSecondary = NavyBackground,
    secondaryContainer = EmeraldContainer,
    onSecondaryContainer = EmeraldGreenLight,
    tertiary = CoralRed,
    onTertiary = NavyBackground,
    tertiaryContainer = CoralContainer,
    onTertiaryContainer = CoralRedLight,
    background = NavyBackground,
    onBackground = TextPrimaryDark,
    surface = NavySurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = NavySurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    outline = OutlineDark,
    error = CoralRed,
    onError = TextPrimaryDark
)

private val LightColorScheme = lightColorScheme(
    primary = ElectricBlue,
    onPrimary = LightSurface,
    primaryContainer = ElectricCyan.copy(alpha = 0.2f),
    onPrimaryContainer = ElectricBlue,
    secondary = EmeraldGreen,
    onSecondary = LightSurface,
    secondaryContainer = EmeraldGreen.copy(alpha = 0.15f),
    onSecondaryContainer = EmeraldGreen,
    tertiary = CoralRed,
    onTertiary = LightSurface,
    tertiaryContainer = CoralRed.copy(alpha = 0.15f),
    onTertiaryContainer = CoralRed,
    background = LightBackground,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = OutlineLight,
    error = CoralRed,
    onError = LightSurface
)

@Composable
fun AutoPayAlertTheme(
    darkTheme: Boolean = true, // Default to sleek fintech dark theme
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
