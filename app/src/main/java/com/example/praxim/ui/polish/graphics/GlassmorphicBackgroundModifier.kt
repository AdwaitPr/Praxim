package com.example.praxim.ui.polish.graphics

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.glassmorphicBackground(cornerRadius: Dp): Modifier {
    val baseColor = Color(0xDE050505)

    return this.then(
        Modifier
            .graphicsLayer {
                shape = RoundedCornerShape(cornerRadius)
                clip = true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    renderEffect = android.graphics.RenderEffect.createBlurEffect(
                        28f, 28f, android.graphics.Shader.TileMode.CLAMP
                    ).asComposeRenderEffect()
                }
            }
            .background(baseColor)
            .run {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    // API 29-30 fallback
                    this.background(Color(0x0A0A0A).copy(alpha = 0.96f))
                        .border(1.dp, Color(0x1F00E676), RoundedCornerShape(cornerRadius))
                } else {
                    this
                }
            }
    )
}
