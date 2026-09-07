package com.example.praxim.ui.polish.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ItemAnimatable {
    val translationY = Animatable(24f)
    val scale = Animatable(0.88f)
    val alpha = Animatable(0f)
}

@Composable
fun BentoGridCascade(isVisible: Boolean) {
    val items = listOf(
        BentoItem("AUTO-ROUTE INTENT", 296.dp, 92.dp),
        BentoItem("COPY TREE", 142.dp, 84.dp),
        BentoItem("CAPTURE", 142.dp, 84.dp),
        BentoItem("PIPELINE: 120 FPS LOCKED", 296.dp, 56.dp)
    )

    val animatables = remember {
        items.map { ItemAnimatable() }
    }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            animatables.forEachIndexed { i, animatable ->
                delay(i * 16L) // ~16.67ms
                launch {
                    animatable.translationY.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(stiffness = 400f, dampingRatio = 0.75f)
                    )
                }
                launch {
                    animatable.scale.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(stiffness = 400f, dampingRatio = 0.75f)
                    )
                }
                launch {
                    animatable.alpha.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(stiffness = 400f, dampingRatio = 0.75f)
                    )
                }
            }
        } else {
            animatables.forEach { animatable ->
                launch {
                    animatable.alpha.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(stiffness = 10000f, dampingRatio = 1.0f)
                    )
                }
            }
        }
    }

    Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Item 0: Hero Action
        BentoGridItem(items[0], animatables[0])

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Item 1: Secondary Micro Action
            BentoGridItem(items[1], animatables[1])
            Spacer(modifier = Modifier.width(8.dp))
            // Item 2: Tertiary Micro Action
            BentoGridItem(items[2], animatables[2])
        }

        // Item 3: Telemetry Tile
        BentoGridItem(items[3], animatables[3])
    }
}

private data class BentoItem(val title: String, val width: Dp, val height: Dp)

@Composable
private fun BentoGridItem(item: BentoItem, animatable: ItemAnimatable) {
    Box(
        modifier = Modifier
            .size(item.width, item.height)
            .graphicsLayer {
                translationY = animatable.translationY.value.dp.toPx()
                scaleX = animatable.scale.value
                scaleY = animatable.scale.value
                alpha = animatable.alpha.value
            }
            .background(Color(0xFF1E1E1E), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = item.title,
            color = Color.White
        )
    }
}
