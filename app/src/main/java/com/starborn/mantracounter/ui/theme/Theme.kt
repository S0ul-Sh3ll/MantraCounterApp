package com.starborn.mantracounter.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

private val Saffron = Color(0xFFC2571E)
private val SaffronLight = Color(0xFFFFB77A)
private val Sandal = Color(0xFFF8F2EA)
private val Ink = Color(0xFF231A14)

private val LightColors = lightColorScheme(
    primary = Saffron,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBC6),
    onPrimaryContainer = Color(0xFF3A1300),
    secondary = Color(0xFF77574A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDBCC),
    onSecondaryContainer = Color(0xFF2C160D),
    tertiary = Color(0xFF6A5D2F),
    background = Sandal,
    onBackground = Ink,
    surface = Sandal,
    onSurface = Ink,
    surfaceVariant = Color(0xFFF2DFD3),
    onSurfaceVariant = Color(0xFF52443C),
    outline = Color(0xFF85736B),
    error = Color(0xFFB3261E),
)

private val DarkColors = darkColorScheme(
    primary = SaffronLight,
    onPrimary = Color(0xFF551D00),
    primaryContainer = Color(0xFF793300),
    onPrimaryContainer = Color(0xFFFFDBC6),
    secondary = Color(0xFFE7BEAC),
    onSecondary = Color(0xFF442A1F),
    secondaryContainer = Color(0xFF5D4034),
    onSecondaryContainer = Color(0xFFFFDBCC),
    tertiary = Color(0xFFD5C58F),
    background = Color(0xFF15100D),
    onBackground = Color(0xFFF0DFD6),
    surface = Color(0xFF15100D),
    onSurface = Color(0xFFF0DFD6),
    surfaceVariant = Color(0xFF52443C),
    onSurfaceVariant = Color(0xFFD7C2B8),
    outline = Color(0xFF9F8D84),
    error = Color(0xFFF2B8B5),
)

/**
 * Card accents used when a japa has no background image. Index is stored on the entity so a
 * japa keeps its colour for life rather than shifting when the list is filtered or reordered.
 */
val AccentPalette = listOf(
    // The first six are unchanged: accentIndex is stored on the row, so reordering or inserting
    // above them would silently repaint every japa already created.
    Color(0xFFC2571E), // saffron
    Color(0xFF7A4E9B), // amethyst
    Color(0xFF1F7A6B), // tulsi
    Color(0xFF9B1B30), // kumkum
    Color(0xFF2A5C9B), // indigo
    Color(0xFF8A6A18), // brass
    Color(0xFFB03A6E), // lotus
    Color(0xFF3F7A22), // banana leaf
    Color(0xFF0E6E86), // peacock
    Color(0xFFD08A00), // marigold
    Color(0xFF6B4226), // sandalwood
    Color(0xFF4A4E9B), // twilight
    Color(0xFF8C1F1F), // vermilion
    Color(0xFF2E7D5B), // emerald
    Color(0xFF7A6A57), // ash
    Color(0xFF1F2E5A), // midnight
)

private val AppTypography = Typography(
    displayLarge = TextStyle(
        fontSize = 64.sp,
        lineHeight = 72.sp,
        fontWeight = FontWeight.Light
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.SemiBold
    ),
    labelMedium = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium
    ),
)

@Composable
fun MantraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        content = content
    )
}
