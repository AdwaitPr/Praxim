package com.praxim.luminate.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.praxim.engine.pipeline.HudUiState
import kotlin.math.roundToInt

@Composable
fun PraximHudRootView(
    hudState: HudUiState,
    onDismiss: () -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                }
            }
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .padding(16.dp)
        ) {
            AnimatedContent(
                targetState = hudState,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                }, label = "hudStateAnimation"
            ) { state ->
                when (state) {
                    is HudUiState.Idle -> EdgePillView(Color(0xFF00E676), "Ready")
                    is HudUiState.Analyzing -> EdgePillView(Color(0xFFFFC107), "Auditing...")
                    is HudUiState.SecureScreenDetected -> EdgePillView(Color(0xFF9E9E9E), "Protected Surface")
                    is HudUiState.Throttled -> EdgePillView(Color(0xFFFF9800), "Throttled")
                    is HudUiState.ThreatDetected -> ExpandedActionHudView(
                        state = state,
                        onDismiss = onDismiss
                    )
                    is HudUiState.ActionReady -> ExpandedActionHudView(
                        state = state,
                        onDismiss = onDismiss
                    )
                }
            }
        }
    }
}

@Composable
fun EdgePillView(dotColor: Color, label: String) {
    Row(
        modifier = Modifier
            .background(Color(0xBB000000), RoundedCornerShape(24.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(dotColor, RoundedCornerShape(50))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, color = Color.White)
    }
}

@Composable
fun ExpandedActionHudView(
    state: HudUiState,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val (bgColor, title, subtitle, showAction) = when (state) {
        is HudUiState.ThreatDetected -> listOf(
            Color(0xDDFF1744),
            "Suspected Fraud",
            state.outcome.result.risk,
            false
        )
        is HudUiState.ActionReady -> listOf(
            Color(0xDD2979FF),
            "Verified Merchant",
            state.outcome.result.payee,
            true
        )
        else -> listOf(Color.DarkGray, "", "", false)
    }

    Column(
        modifier = Modifier
            .width(280.dp)
            .background(bgColor as Color, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(text = title as String, color = Color.White, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = subtitle as String, color = Color.White, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)

        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.White)
            ) {
                Text("Dismiss")
            }
            if (showAction as Boolean) {
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val outcome = (state as HudUiState.ActionReady).outcome
                        val entity = outcome.entities.firstOrNull { it.type == "VPA" || it.type == "PHONE" }
                        val uriStr = if (entity?.type == "VPA") "upi://pay?pa=${entity.value}" else "tel:${entity?.value}"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriStr)).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = bgColor as Color)
                ) {
                    Text("Pay Safely")
                }
            }
        }
    }
}
