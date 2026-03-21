package com.deskzen.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

val DeskZenLightColorScheme = lightColorScheme(
    primary = DeskZenGreen,
    onPrimary = DeskZenOnPrimary,
    secondary = DeskZenSage,
    onSecondary = DeskZenOnPrimary,
    tertiary = DeskZenGold,
    surface = DeskZenSurface,
    onSurface = DeskZenOnSurface,
    background = DeskZenBackground,
    onBackground = DeskZenOnSurface,
    error = DeskZenError,
    onError = DeskZenOnError
)

val DeskZenDarkColorScheme = darkColorScheme(
    primary = DeskZenGreenLight,
    onPrimary = DeskZenOnPrimaryDark,
    secondary = DeskZenSageLight,
    onSecondary = DeskZenOnPrimaryDark,
    tertiary = DeskZenGoldLight,
    surface = DeskZenSurfaceDark,
    onSurface = DeskZenOnSurfaceDark,
    background = DeskZenBackgroundDark,
    onBackground = DeskZenOnSurfaceDark,
    error = DeskZenErrorDark,
    onError = DeskZenOnErrorDark
)

@Composable
fun DeskZenTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DeskZenDarkColorScheme
        else -> DeskZenLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = DeskZenTypography,
        shapes = DeskZenShapes,
        content = content
    )
}
