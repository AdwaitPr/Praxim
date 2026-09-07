package com.example.praxim.ui.polish.components

import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.example.praxim.ui.polish.graphics.glassmorphicBackground
import com.example.praxim.ui.polish.graphics.neonBorderBeam
import com.example.praxim.ui.polish.haptics.PraximHapticEngine
import com.example.praxim.ui.polish.model.HudState

@Composable
fun PraximOverlayHud(
    currentState: HudState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // In a real scenario, fallbackView would be provided via LocalView.current,
    // but in a WindowManager overlay without a traditional decor view it might be null.
    val hapticEngine = PraximHapticEngine(context, null)

    val transition = updateTransition(targetState = currentState, label = "PraximHudMasterTransition")

    val animatedWidth by transition.animateDp(
        transitionSpec = {
            when {
                HudState.EdgePill isTransitioningTo HudState.Processing ->
                    spring(stiffness = 400f, dampingRatio = 0.82f)
                HudState.Processing isTransitioningTo HudState.Expanded ->
                    spring(stiffness = 1500f, dampingRatio = 0.78f)
                else -> spring(stiffness = 1500f, dampingRatio = 1.0f)
            }
        },
        label = "width"
    ) { state -> state.width }

    val animatedHeight by transition.animateDp(
        transitionSpec = {
            when {
                HudState.EdgePill isTransitioningTo HudState.Processing ->
                    spring(stiffness = 400f, dampingRatio = 0.82f)
                HudState.Processing isTransitioningTo HudState.Expanded ->
                    spring(stiffness = 1500f, dampingRatio = 0.78f)
                else -> spring(stiffness = 1500f, dampingRatio = 1.0f)
            }
        },
        label = "height"
    ) { state -> state.height }

    val animatedCornerRadius by transition.animateDp(
        transitionSpec = { spring(stiffness = 1500f, dampingRatio = 1.0f) },
        label = "cornerRadius"
    ) { state -> state.cornerRadius }

    LaunchedEffect(currentState) {
        when (currentState) {
            HudState.Processing -> hapticEngine.triggerEngagementNotch()
            HudState.Expanded -> hapticEngine.triggerExpandedLatch()
            HudState.EdgePill -> hapticEngine.triggerChipDetent()
        }
    }

    Box(
        modifier = modifier
            .layout { measurable, _ ->
                val widthPx = animatedWidth.roundToPx()
                val heightPx = animatedHeight.roundToPx()
                val placeable = measurable.measure(Constraints.fixed(widthPx, heightPx))
                layout(widthPx, heightPx) {
                    placeable.place(0, 0)
                }
            }
            .glassmorphicBackground(animatedCornerRadius)
            .neonBorderBeam(
                cornerRadiusDp = animatedCornerRadius.value,
                strokeWidthDp = 2f,
                progress = 0f, // You'd typically animate this with an infinite transition
                density = context.resources.displayMetrics.density
            ),
        contentAlignment = Alignment.Center
    ) {
        // Child content (Bento Grid) only visible when expanded
        BentoGridCascade(isVisible = currentState == HudState.Expanded)
    }
}
