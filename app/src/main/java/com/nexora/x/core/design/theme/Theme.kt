package com.nexora.x.core.design.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val NexoraDarkColorScheme = darkColorScheme(
    primary = NexoraColors.ElectricPurple,
    onPrimary = NexoraColors.TextPrimary,
    primaryContainer = NexoraColors.ElectricPurpleSubtle,
    onPrimaryContainer = NexoraColors.TextPrimary,
    secondary = NexoraColors.CyanHighlight,
    onSecondary = NexoraColors.DeepNavyBackground,
    secondaryContainer = NexoraColors.CyanHighlightDim,
    onSecondaryContainer = NexoraColors.CyanHighlight,
    background = NexoraColors.DeepNavyBackground,
    onBackground = NexoraColors.TextPrimary,
    surface = NexoraColors.BlackSurface,
    onSurface = NexoraColors.TextPrimary,
    surfaceVariant = NexoraColors.ElevatedCardSurface,
    onSurfaceVariant = NexoraColors.TextSecondary,
    outline = NexoraColors.HairlineBorder,
    error = NexoraColors.StatusErrorCrimson
)

private val NexoraLightColorScheme = lightColorScheme(
    primary = NexoraColors.ElectricPurple,
    onPrimary = NexoraColors.LightSurface,
    primaryContainer = NexoraColors.ElectricPurpleSubtle,
    onPrimaryContainer = NexoraColors.LightTextPrimary,
    secondary = NexoraColors.CyanHighlightLightMode,
    onSecondary = NexoraColors.LightSurface,
    secondaryContainer = NexoraColors.CyanHighlightDim,
    onSecondaryContainer = NexoraColors.CyanHighlightLightMode,
    background = NexoraColors.LightBackground,
    onBackground = NexoraColors.LightTextPrimary,
    surface = NexoraColors.LightSurface,
    onSurface = NexoraColors.LightTextPrimary,
    surfaceVariant = NexoraColors.LightElevatedSurface,
    onSurfaceVariant = NexoraColors.LightTextSecondary,
    outline = NexoraColors.LightBorder,
    error = NexoraColors.StatusErrorCrimson
)

@Composable
fun NexoraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) NexoraDarkColorScheme else NexoraLightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NexoraTypography,
        shapes = NexoraShapes,
        content = content
    )
}
