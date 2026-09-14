package com.example.praxim.ui.hud

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.praxim.ui.theme.CyberCyan
import com.example.praxim.ui.theme.EdgeHandleGradient
import com.example.praxim.ui.theme.HyperLime
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun EdgePillView(
    settings: HudSettings,
    onTriggerScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var dragAmountX by remember { mutableFloatStateOf(0f) }
    var hasTriggeredHaptic by remember { mutableStateOf(false) }
    var lastTickStep by remember { mutableIntStateOf(0) }

    val pullThreshold = 100f // pixels

    val pillWidthDp by animateDpAsState(
        targetValue = if (dragAmountX != 0f) (8.dp + (abs(dragAmountX) / 10).dp) else 5.dp,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow),
        label = "pillWidth"
    )

    val vibrator = remember {
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    fun triggerTickHaptic() {
        if (!settings.hapticEnabled) return
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                it.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_TICK)
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    it.vibrate(
                        VibrationEffect.startComposition()
                            .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.4f)
                            .compose()
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createOneShot(8, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(8)
            }
        }
    }

    fun triggerClickHaptic() {
        if (!settings.hapticEnabled) return
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                it.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_CLICK)
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    it.vibrate(
                        VibrationEffect.startComposition()
                            .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f)
                            .compose()
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(25)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .pointerInput(settings.anchorRight) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        dragAmountX = 0f
                        hasTriggeredHaptic = false
                        lastTickStep = 0
                    },
                    onDragEnd = {
                        val isTriggered = if (settings.anchorRight) {
                            dragAmountX < -pullThreshold
                        } else {
                            dragAmountX > pullThreshold
                        }
                        if (isTriggered) {
                            triggerClickHaptic()
                            onTriggerScan()
                        }
                        dragAmountX = 0f
                        hasTriggeredHaptic = false
                        lastTickStep = 0
                    },
                    onDragCancel = {
                        dragAmountX = 0f
                        hasTriggeredHaptic = false
                        lastTickStep = 0
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        // Tactile pull resistance
                        dragAmountX += dragAmount * 0.65f
                        val currentPull = if (settings.anchorRight) -dragAmountX else dragAmountX

                        val tickStep = (currentPull / 25f).toInt()
                        if (tickStep > lastTickStep && currentPull > 0) {
                            lastTickStep = tickStep
                            triggerTickHaptic()
                        }

                        if (currentPull > pullThreshold && !hasTriggeredHaptic) {
                            hasTriggeredHaptic = true
                            triggerClickHaptic()
                        }
                    }
                )
            },
        contentAlignment = if (settings.anchorRight) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        val calculatedOffset = if (settings.anchorRight) {
            dragAmountX.coerceAtMost(0f).roundToInt()
        } else {
            dragAmountX.coerceAtLeast(0f).roundToInt()
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(calculatedOffset, 0) }
                .height(settings.handleHeightDp.dp)
                .width(pillWidthDp)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(12.dp),
                    ambientColor = CyberCyan.copy(alpha = 0.3f),
                    spotColor = HyperLime.copy(alpha = 0.4f)
                )
                .clip(RoundedCornerShape(12.dp))
                .background(EdgeHandleGradient)
                .padding(vertical = 4.dp)
        )
    }
}
