package com.example.gymlogger.android

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        darkColorScheme(
            primary = Color(0xFF080808),
            secondary = Color(0xFF423658),
            tertiary = Color(0xFF5C415F),
            surface = Color(0xFF0A0A0A),
            background = Color(0xFF000000)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF6200EE),
            secondary = Color(0xFF03DAC5),
            tertiary = Color(0xFF3700B3)
        )
    }
    val typography = Typography(
        bodyMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp
        )
    )
    val shapes = Shapes(
        small = RoundedCornerShape(4.dp),
        medium = RoundedCornerShape(8.dp),
        large = RoundedCornerShape(0.dp)
    )

    MaterialTheme(
        colorScheme = createColorScheme(GymThemePalette), // For now set to a color scheme but this should be changed when we need to dynamic set a theme.
        typography = typography,
        shapes = shapes,
        content = content
    )
}

interface GymColorPalette {
    val PrimaryBackground: Color
    val SecondaryBackground: Color
    val SurfaceBackground: Color
    val ElevatedSurface: Color

    val PrimaryAccent: Color
    val SecondaryAccent: Color
    val AccentVariant: Color

    val PrimaryText: Color
    val SecondaryText: Color
    val TertiaryText: Color
    val AccentText: Color

    val Success: Color
    val Warning: Color
    val Error: Color
    val Info: Color

    val Divider: Color
    val Border: Color
}

fun createColorScheme(palette: GymColorPalette): ColorScheme {
    return darkColorScheme(
        primary = palette.PrimaryAccent,
        onPrimary = palette.PrimaryBackground,
        primaryContainer = palette.AccentVariant,
        onPrimaryContainer = palette.PrimaryText,

        secondary = palette.SecondaryAccent,
        onSecondary = palette.PrimaryBackground,
        secondaryContainer = palette.ElevatedSurface,
        onSecondaryContainer = palette.SecondaryText,

        tertiary = palette.AccentVariant,
        onTertiary = palette.PrimaryBackground,
        tertiaryContainer = palette.SurfaceBackground,
        onTertiaryContainer = palette.TertiaryText,

        background = palette.PrimaryBackground,
        onBackground = palette.PrimaryText,

        surface = palette.SurfaceBackground,
        onSurface = palette.PrimaryText,
        surfaceVariant = palette.ElevatedSurface,
        onSurfaceVariant = palette.SecondaryText,
        surfaceTint = palette.PrimaryAccent,

        inverseSurface = palette.PrimaryText,
        inverseOnSurface = palette.PrimaryBackground,
        inversePrimary = palette.AccentVariant,

        error = palette.Error,
        onError = palette.PrimaryText,
        errorContainer = palette.Error.copy(alpha = 0.12f),
        onErrorContainer = palette.Error,

        outline = palette.Border,
        outlineVariant = palette.Divider,
        scrim = Color.Black.copy(alpha = 0.32f)
    )
}

object GymThemePalette: GymColorPalette {

    // Background Colors
    override val PrimaryBackground = Color(0xFF000000)        // Pure AMOLED Black
    override val SecondaryBackground = Color(0xFF0A0A0A)      // Slightly lighter black
    override val SurfaceBackground = Color(0xFF121212)        // Card/surface background
    override val ElevatedSurface = Color(0xFF1A1A1A)          // Elevated components

    // Accent Colors
    override val PrimaryAccent = Color(0xFF9C88FF)            // Soft pastel purple
    override val SecondaryAccent = Color(0xFFB19CD9)          // Lighter pastel purple
    override val AccentVariant = Color(0xFF7B68EE)            // Medium slate blue

    // Text Colors
    override val PrimaryText = Color(0xFFE8E8E8)              // Near white for primary text
    override val SecondaryText = Color(0xFFB3B3B3)            // Gray for secondary text
    override val TertiaryText = Color(0xFF808080)             // Dimmer gray for hints
    override val AccentText = Color(0xFF9C88FF)               // Purple for accent text

    // Status Colors
    override val Success = Color(0xFF4CAF50)                  // Green for success
    override val Warning = Color(0xFFFF9800)                  // Orange for warnings
    override val Error = Color(0xFFFF5252)                    // Red for errors
    override val Info = Color(0xFF64B5F6)                     // Blue for info

    // Dividers & Borders
    override val Divider = Color(0xFF2A2A2A)                  // Subtle dividers
    override val Border = Color(0xFF333333)                   // Input borders
}
