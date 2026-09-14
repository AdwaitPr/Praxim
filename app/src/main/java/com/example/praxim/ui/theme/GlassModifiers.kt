package com.example.praxim.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.specularGlassCard(
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    backgroundColor: Color = SurfaceTier1.copy(alpha = 0.88f),
    ambientGlow: Color = CyberCyan.copy(alpha = 0.08f),
    elevation: Dp = 16.dp
): Modifier = this
    .shadow(
        elevation = elevation,
        shape = shape,
        ambientColor = ambientGlow,
        spotColor = ambientGlow
    )
    .clip(shape)
    .background(backgroundColor)
    .border(
        width = 1.dp,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.22f), // Specular light hit on top rim
                Color.White.copy(alpha = 0.03f)  // Dark shadow drop on bottom rim
            )
        ),
        shape = shape
    )
