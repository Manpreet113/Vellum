package com.reader.vellum.ui.theme

import android.app.Activity
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
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
    tertiary = WarmTertiary,
    onTertiary = Color(0xFF4F2500)
)

@Composable
fun VellumTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = NeoDarkColorScheme,
        typography = Typography,
        content = content
    )
}
