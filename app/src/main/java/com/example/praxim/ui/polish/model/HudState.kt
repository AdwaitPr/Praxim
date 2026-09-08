package com.example.praxim.ui.polish.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
sealed class HudState(val width: Dp, val height: Dp, val cornerRadius: Dp) {
    object EdgePill : HudState(width = 48.dp, height = 36.dp, cornerRadius = 18.dp)
    object Processing : HudState(width = 240.dp, height = 64.dp, cornerRadius = 20.dp)
    object Expanded : HudState(width = 336.dp, height = 288.dp, cornerRadius = 24.dp)
}
