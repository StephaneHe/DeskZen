package com.deskzen.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// Solo Leveling — Shadow Monarch color scheme (always dark)
val SoloLevelingColorScheme = darkColorScheme(
    primary = SoloElectricBlue,
    onPrimary = SoloTextPrimary,
    primaryContainer = SoloNavy,
    onPrimaryContainer = SoloBrightBlue,
    secondary = SoloPurple,
    onSecondary = SoloTextPrimary,
    secondaryContainer = SoloNavy,
    onSecondaryContainer = SoloBrightPurple,
    tertiary = SoloCyan,
    onTertiary = SoloDeepBlack,
    tertiaryContainer = SoloNavy,
    onTertiaryContainer = SoloCyan,
    surface = SoloSurface,
    onSurface = SoloTextPrimary,
    surfaceVariant = SoloSurfaceLight,
    onSurfaceVariant = SoloTextSecondary,
    background = SoloDeepBlack,
    onBackground = SoloTextPrimary,
    error = SoloError,
    onError = SoloTextPrimary,
    outline = SoloTextMuted,
    outlineVariant = SoloSurfaceLight
)

@Composable
fun DeskZenTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SoloLevelingColorScheme,
        typography = DeskZenTypography,
        shapes = DeskZenShapes,
        content = content
    )
}
