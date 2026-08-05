package com.example.praxim.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.praxim.ui.hud.HudSettings
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.DarkObsidian
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var settings by remember { mutableStateOf(HudSettings()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBlack)
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
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = TextPrimaryDark
                )
                Text(
                    text = "Configure edge handle & gesture sensitivity",
                    fontSize = 11.sp,
                    color = NeonGreen
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Anchor Side Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, GlassBorder, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkObsidian),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Edge Handle Screen Anchor",
                    fontSize = 14.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
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
                        colors = RadioButtonDefaults.colors(selectedColor = NeonGreen)
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
                        colors = RadioButtonDefaults.colors(selectedColor = NeonGreen)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Haptic Feedback & Gesture Sensitivity
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, GlassBorder, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkObsidian),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Tactile Haptic Vibration",
                            fontSize = 14.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
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
                        colors = SwitchDefaults.colors(checkedThumbColor = AmoledBlack, checkedTrackColor = NeonGreen)
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
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
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
                        colors = SwitchDefaults.colors(checkedThumbColor = AmoledBlack, checkedTrackColor = NeonGreen)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Core Philosophy & Privacy Info Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, GlassBorder, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = NeonGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Intent-First Screen Intelligence",
                        fontSize = 13.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
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
