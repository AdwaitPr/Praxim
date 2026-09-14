package com.example.praxim.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Obsidian Void Hierarchy (~90% surface area)
val VoidBase = Color(0xFF07090E)            // Root canvas background
val SurfaceTier1 = Color(0xFF0F131C)        // Base card container
val SurfaceTier2 = Color(0xFF171C28)        // Elevated card & active item
val SurfaceHighlight = Color(0xFF22293A)    // Interactive / focused state

// Disciplined Accent Palette (~10% budget)
val CyberCyan = Color(0xFF00F0FF)           // Primary interactive accent
val HyperLime = Color(0xFFCCFF00)           // Rare success / confirmation flash
val ShieldEmerald = Color(0xFF00E699)       // Strictly 100% on-device trust badge
val AlertAmber = Color(0xFFFF9500)          // Strictly warnings / tracking alerts

// High-Contrast Specular Typography
val TextPrimaryDark = Color(0xFFF4F6FC)     // Specular white
val TextSecondaryDark = Color(0xFF8E95A5)   // Slate telemetry gray
val TextTertiaryDark = Color(0xFFA6ACBD)    // WCAG AA compliant muted gray

// Glass & Border Overlays
val GlassBorder = Color(0x33F4F6FC)

// Dynamic Telemetry Gradients
val CoreEnergyGradient = Brush.linearGradient(listOf(CyberCyan, ShieldEmerald))
val EdgeHandleGradient = Brush.verticalGradient(listOf(CyberCyan, ShieldEmerald, CyberCyan))
