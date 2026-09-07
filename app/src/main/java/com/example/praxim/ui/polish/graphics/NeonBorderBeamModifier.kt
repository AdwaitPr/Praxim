package com.example.praxim.ui.polish.graphics

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

fun Modifier.neonBorderBeam(
    cornerRadiusDp: Float,
    strokeWidthDp: Float,
    progress: Float,
    density: Float
) = this.drawWithCache {
    val w = size.width
    val h = size.height
    val strokePx = strokeWidthDp * density
    val cornerRadiusPx = cornerRadiusDp * density

    // Inset the bounds to prevent clipping at edges
    val halfStroke = strokePx / 2f
    val outerRadius = cornerRadiusPx
    val innerRadius = (outerRadius - halfStroke).coerceAtLeast(0f)

    val baseBorderColor = Color(0x1F00E676)
    val startColor = Color.Transparent
    val midColor = Color(0x9900E676)
    val endColor = Color(0xFF00E676)

    val fullPath = Path().apply {
        addRoundRect(
            RoundRect(
                left = halfStroke,
                top = halfStroke,
                right = w - halfStroke,
                bottom = h - halfStroke,
                cornerRadius = CornerRadius(innerRadius, innerRadius)
            )
        )
    }

    val pathMeasure = PathMeasure().apply {
        setPath(fullPath, false)
    }

    val length = pathMeasure.length
    val deltaL = 0.28f * length

    val destPath = Path()
    val baseStroke = Stroke(width = strokePx)
    val beamStroke = Stroke(width = strokePx, cap = StrokeCap.Round)

    // Pre-allocate the brush
    val beamBrush = Brush.sweepGradient(
        0.0f to startColor,
        0.5f to midColor,
        1.0f to endColor
    )

    onDrawWithContent {
        drawContent()

        // Base static border
        drawPath(
            path = fullPath,
            color = baseBorderColor,
            style = baseStroke
        )

        // Animated beam
        destPath.reset()
        val start = progress * length
        val end = start + deltaL

        if (end <= length) {
            pathMeasure.getSegment(start, end, destPath, true)
        } else {
            pathMeasure.getSegment(start, length, destPath, true)
            pathMeasure.getSegment(0f, end - length, destPath, false)
        }

        drawPath(
            path = destPath,
            brush = beamBrush,
            style = beamStroke
        )
    }
}
