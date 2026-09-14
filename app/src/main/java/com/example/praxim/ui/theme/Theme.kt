package com.example.praxim.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * Precision Instrument Pure Dark Theme Scheme
 */
private val DarkColorScheme = darkColorScheme(
    primary = CyberCyan,
    secondary = HyperLime,
    tertiary = ShieldEmerald,
    background = VoidBase,
    surface = SurfaceTier1,
    surfaceVariant = SurfaceTier2,
    onPrimary = VoidBase,
    onSecondary = VoidBase,
    onTertiary = VoidBase,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = GlassBorder,
    error = AlertAmber
)

@Composable
fun PraximTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        shapes = InstrumentShapes,
        content = content
    )
}
