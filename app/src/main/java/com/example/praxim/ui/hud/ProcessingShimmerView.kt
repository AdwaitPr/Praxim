package com.example.praxim.ui.hud

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.praxim.ui.theme.HyperLime
import com.example.praxim.ui.theme.SurfaceTier1
import com.example.praxim.ui.theme.TextSecondaryDark
import com.example.praxim.ui.theme.specularGlassCard

@Composable
fun ProcessingShimmerView(
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "sweep_shimmer")

    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep_translation"
    )

    val haloScale by transition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_pulse"
    )

    val sweepBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.02f),
            HyperLime.copy(alpha = 0.18f),
            Color.White.copy(alpha = 0.02f)
        ),
        start = Offset(translateAnim - 300f, translateAnim - 300f),
        end = Offset(translateAnim, translateAnim)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .specularGlassCard(
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                backgroundColor = SurfaceTier1.copy(alpha = 0.92f),
                ambientGlow = HyperLime.copy(alpha = 0.12f)
            )
            .background(sweepBrush)
            .padding(20.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Primary status dot with subtle pulsing halo
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .scale(haloScale)
                            .clip(CircleShape)
                            .background(HyperLime.copy(alpha = 0.25f))
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(HyperLime)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = "PROCESSING RAM FRAME BUFFER",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = HyperLime,
                    letterSpacing = 1.4.sp
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.04f),
                                HyperLime.copy(alpha = 0.12f),
                                Color.White.copy(alpha = 0.04f)
                            )
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.68f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.04f),
                                HyperLime.copy(alpha = 0.10f),
                                Color.White.copy(alpha = 0.04f)
                            )
                        )
                    )
            )

            Text(
                text = "Sub-100ms Local ML Kit OCR • Zero Network Calls",
                fontSize = 11.sp,
                color = TextSecondaryDark,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
