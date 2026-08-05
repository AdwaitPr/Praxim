package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LuminateDarkColorScheme = darkColorScheme(
    primary = NeonGreen,
    onPrimary = AmoledBlack,
    primaryContainer = Color(0xFF00381B),
    onPrimaryContainer = NeonGreen,
    secondary = NeonCyan,
    onSecondary = AmoledBlack,
    secondaryContainer = Color(0xFF00363D),
    onSecondaryContainer = NeonCyan,
    tertiary = ElectricBlue,
    background = AmoledBlack,
    onBackground = TextPrimaryDark,
    surface = DarkObsidian,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = GlassBorder,
    outlineVariant = Color(0x22FFFFFF)
)

@Composable
fun PraximTheme(
    darkTheme: Boolean = true, // Default to sleek dark glass aesthetic
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LuminateDarkColorScheme,
        typography = Typography,
        content = content
    )
}
