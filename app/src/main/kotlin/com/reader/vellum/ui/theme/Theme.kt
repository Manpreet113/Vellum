package com.reader.vellum.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val NeoDarkColorScheme = darkColorScheme(
    primary = PrimaryNeo,
    onPrimary = OnPrimaryNeo,
    background = BackgroundNeo,
    onBackground = OnBackgroundNeo,
    surface = SurfaceNeo,
    onSurface = OnSurfaceNeo,
    surfaceVariant = SurfaceVariantNeo,
    onSurfaceVariant = OnSurfaceVariantNeo,
    outline = OutlineNeo,
    secondary = PrimaryNeo,
    onSecondary = OnPrimaryNeo,
    tertiary = PrimaryNeo,
    onTertiary = OnPrimaryNeo
)

@Composable
fun VellumTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Neo-Reader is dark-mode first, but we respect system theme if light is needed.
    // However, the spec is strongly Dark Neo-minimalist.
    val colorScheme = if (darkTheme) NeoDarkColorScheme else NeoDarkColorScheme // Defaulting to Dark for Neo feel

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false // Dark background
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
