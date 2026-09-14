package com.example.praxim.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.praxim.ui.hud.HudSettings
import com.example.praxim.ui.theme.CyberCyan
import com.example.praxim.ui.theme.HyperLime
import com.example.praxim.ui.theme.SurfaceTier1
import com.example.praxim.ui.theme.SurfaceTier2
import com.example.praxim.ui.theme.TextPrimaryDark
import com.example.praxim.ui.theme.TextSecondaryDark
import com.example.praxim.ui.theme.VoidBase
import com.example.praxim.ui.theme.specularGlassCard

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var settings by remember { mutableStateOf(HudSettings()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VoidBase)
            .padding(18.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimaryDark)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "HUD PREFERENCES",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
                Text(
                    text = "Configure edge handle & gesture sensitivity",
                    fontSize = 11.sp,
                    color = CyberCyan
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Anchor Side Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .specularGlassCard(
                    shape = RoundedCornerShape(20.dp),
                    backgroundColor = SurfaceTier1.copy(alpha = 0.9f),
                    ambientGlow = CyberCyan.copy(alpha = 0.08f),
                    elevation = 12.dp
                )
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "Edge Handle Screen Anchor",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Anchor to Right Edge", fontSize = 13.sp, color = TextSecondaryDark)
                    RadioButton(
                        selected = settings.anchorRight,
                        onClick = { settings = settings.copy(anchorRight = true) },
                        colors = RadioButtonDefaults.colors(selectedColor = CyberCyan)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Anchor to Left Edge", fontSize = 13.sp, color = TextSecondaryDark)
                    RadioButton(
                        selected = !settings.anchorRight,
                        onClick = { settings = settings.copy(anchorRight = false) },
                        colors = RadioButtonDefaults.colors(selectedColor = CyberCyan)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Haptic Feedback & Gesture Sensitivity
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .specularGlassCard(
                    shape = RoundedCornerShape(20.dp),
                    backgroundColor = SurfaceTier1.copy(alpha = 0.9f),
                    ambientGlow = CyberCyan.copy(alpha = 0.08f),
                    elevation = 12.dp
                )
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Tactile Haptic Vibration",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = "Trigger vibration pulse when pull threshold reached",
                            fontSize = 11.sp,
                            color = TextSecondaryDark
                        )
                    }

                    Switch(
                        checked = settings.hapticEnabled,
                        onCheckedChange = { settings = settings.copy(hapticEnabled = it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = VoidBase, checkedTrackColor = CyberCyan)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Copy Single Match",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = "Instantly copy entity if only one result detected",
                            fontSize = 11.sp,
                            color = TextSecondaryDark
                        )
                    }

                    Switch(
                        checked = settings.autoCopyOnSingleMatch,
                        onCheckedChange = { settings = settings.copy(autoCopyOnSingleMatch = it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = VoidBase, checkedTrackColor = CyberCyan)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Core Philosophy & Privacy Info Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .specularGlassCard(
                    shape = RoundedCornerShape(20.dp),
                    backgroundColor = SurfaceTier2.copy(alpha = 0.85f),
                    ambientGlow = HyperLime.copy(alpha = 0.05f),
                    elevation = 8.dp
                )
                .padding(16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = CyberCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Intent-First Screen Intelligence",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Luminate processes screen frames strictly inside RAM buffers using local ML Kit OCR. Extracted UPI IDs, IFSC codes, phone numbers, and URLs are converted into single-tap native intents without cloud servers or persistent background video streams.",
                    fontSize = 11.sp,
                    color = TextSecondaryDark,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
