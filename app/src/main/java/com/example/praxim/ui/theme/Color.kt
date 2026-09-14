package com.example.praxim.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Obsidian Void Hierarchy
val VoidBase = Color(0xFF07090E)            // Root background
val SurfaceTier1 = Color(0xFF0F131C)        // Base card container
val SurfaceTier2 = Color(0xFF171C28)        // Elevated card & active items
val SurfaceHighlight = Color(0xFF22293A)    // Interactive / focused pill state

// Active Energy Accents
val HyperLime = Color(0xFFCCFF00)           // Primary action / intelligence core
val CyberCyan = Color(0xFF00F0FF)           // Diagnostic / secondary telemetry
val ShieldEmerald = Color(0xFF00E699)       // 100% on-device cryptographic trust
val AlertAmber = Color(0xFFFF9500)          // Tracking / warning alerts
val TextPrimaryDark = Color(0xFFF4F6FC)     // Specular white
val TextSecondaryDark = Color(0xFF8E95A5)   // Slate telemetry gray
val TextTertiaryDark = Color(0x668E95A5)

// Legacy Compatibility Mappings
val AmoledBlack = VoidBase
val DarkObsidian = SurfaceTier1
val SurfaceDark = SurfaceTier2
val GlassBorder = Color(0x33F4F6FC)
val NeonGreen = HyperLime
val NeonCyan = CyberCyan
val ElectricBlue = CyberCyan
val Purple80 = HyperLime
val PurpleGrey80 = TextSecondaryDark
val Pink80 = ShieldEmerald

// Dynamic Gradients
val CoreEnergyGradient = Brush.linearGradient(listOf(HyperLime, CyberCyan))
val EdgeHandleGradient = Brush.verticalGradient(listOf(HyperLime, CyberCyan, HyperLime))
