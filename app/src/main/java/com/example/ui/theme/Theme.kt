package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val NocDarkColorScheme = darkColorScheme(
    primary = NocAmber,
    onPrimary = Color(0xFF1A1000),
    primaryContainer = NocAmberContainer,
    onPrimaryContainer = NocAmberBright,
    secondary = NocBlueDownload,
    onSecondary = Color.White,
    secondaryContainer = NocBlueContainer,
    onSecondaryContainer = Color(0xFF93C5FD),
    tertiary = NocEmeraldOnline,
    onTertiary = Color(0xFF002116),
    tertiaryContainer = NocEmeraldContainer,
    onTertiaryContainer = Color(0xFF6EE7B7),
    error = NocRedOffline,
    onError = Color.White,
    errorContainer = NocRedContainer,
    onErrorContainer = Color(0xFFFCA5A5),
    background = NocObsidianBg,
    onBackground = NocTextPrimary,
    surface = NocObsidianSurface,
    onSurface = NocTextPrimary,
    surfaceVariant = NocObsidianElevated,
    onSurfaceVariant = NocTextSecondary,
    outline = NocObsidianBorder,
    outlineVariant = Color(0xFF1B2230)
)

private val NocLightColorScheme = lightColorScheme(
    primary = Color(0xFFD97706),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFEF3C7),
    onPrimaryContainer = Color(0xFF78350F),
    secondary = Color(0xFF2563EB),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDBEAFE),
    onSecondaryContainer = Color(0xFF1E3A8A),
    tertiary = Color(0xFF059669),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD1FAE5),
    onTertiaryContainer = Color(0xFF064E3B),
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
    background = LightBg,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightElevated,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder,
    outlineVariant = Color(0xFFE2E8F0)
)

val MikroMonShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun MikroMonTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) NocDarkColorScheme else NocLightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = MikroMonShapes,
        content = content
    )
}
