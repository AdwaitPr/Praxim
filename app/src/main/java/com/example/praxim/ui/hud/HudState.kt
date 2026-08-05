package com.example.praxim.ui.hud

import com.example.praxim.model.RecognizedEntity

sealed class HudDisplayMode {
    object Collapsed : HudDisplayMode()
    object Processing : HudDisplayMode()
    data class Expanded(val entities: List<RecognizedEntity>) : HudDisplayMode()
}

data class HudSettings(
    val anchorRight: Boolean = true,
    val handleHeightDp: Int = 56,
    val hapticEnabled: Boolean = true,
    val autoCopyOnSingleMatch: Boolean = false,
    val darkGlassTheme: Boolean = true
)
