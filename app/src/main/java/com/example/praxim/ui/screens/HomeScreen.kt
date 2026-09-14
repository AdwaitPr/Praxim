package com.example.praxim.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.praxim.data.ScanHistoryRepository
import com.example.praxim.service.OverlayHUDService
import com.example.praxim.ui.theme.CyberCyan
import com.example.praxim.ui.theme.GlassBorder
import com.example.praxim.ui.theme.HyperLime
import com.example.praxim.ui.theme.ShieldEmerald
import com.example.praxim.ui.theme.SurfaceTier1
import com.example.praxim.ui.theme.SurfaceTier2
import com.example.praxim.ui.theme.TextPrimaryDark
import com.example.praxim.ui.theme.TextSecondaryDark
import com.example.praxim.ui.theme.VoidBase
import com.example.praxim.ui.theme.specularGlassCard

@Composable
fun HomeScreen(
    repository: ScanHistoryRepository,
    onNavigateSimulator: () -> Unit,
    onNavigateHistory: () -> Unit,
    onStartOverlayService: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isOverlayPermissionGranted by remember {
        mutableStateOf(Settings.canDrawOverlays(context))
    }
    var isServiceRunning by remember { mutableStateOf(false) }

    val scanCount by repository.totalScans.collectAsState(initial = 0)
    val recentScans by repository.allHistory.collectAsState(initial = emptyList())

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(VoidBase)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header
        item {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = HyperLime.copy(alpha = 0.15f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = HyperLime,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "LUMINATE",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimaryDark,
                                letterSpacing = 2.sp
                            )
                            Text(
                                text = "Intent-First Screen Intelligence",
                                fontSize = 11.sp,
                                color = HyperLime,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .specularGlassCard(
                                shape = RoundedCornerShape(12.dp),
                                backgroundColor = SurfaceTier2.copy(alpha = 0.9f),
                                ambientGlow = (if (isServiceRunning) HyperLime else Color.Gray).copy(alpha = 0.1f),
                                elevation = 4.dp
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isServiceRunning) HyperLime else Color.Gray)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isServiceRunning) "ACTIVE" else "STANDBY",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isServiceRunning) HyperLime else TextSecondaryDark
                            )
                        }
                    }
                }
            }
        }

        // Overlay Permission Banner if missing
        if (!isOverlayPermissionGranted) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .specularGlassCard(
                            shape = RoundedCornerShape(18.dp),
                            backgroundColor = Color(0x22FF9100),
                            ambientGlow = Color(0xFFFF9100).copy(alpha = 0.15f),
                            elevation = 8.dp
                        )
                        .padding(16.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = Color(0xFFFF9100)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "System Overlay Permission Required",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF9100)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Luminate requires System Window Overlay permission to display the edge-handle and micro-HUD card over target apps.",
                            fontSize = 12.sp,
                            color = TextSecondaryDark
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9100)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Grant Overlay Permission",
                                fontWeight = FontWeight.Bold,
                                color = VoidBase
                            )
                        }
                    }
                }
            }
        }

        // Main Service Toggle Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .specularGlassCard(
                        shape = RoundedCornerShape(22.dp),
                        backgroundColor = SurfaceTier1.copy(alpha = 0.9f),
                        ambientGlow = HyperLime.copy(alpha = 0.08f),
                        elevation = 12.dp
                    )
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Micro-HUD Overlay Service",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Edge-Anchored handle resting at 0% idle CPU",
                            fontSize = 12.sp,
                            color = TextSecondaryDark
                        )
                    }

                    Switch(
                        checked = isServiceRunning,
                        onCheckedChange = { active ->
                            if (active) {
                                if (!Settings.canDrawOverlays(context)) {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    context.startActivity(intent)
                                } else {
                                    if (onStartOverlayService != null) {
                                        onStartOverlayService()
                                    } else {
                                        OverlayHUDService.start(context)
                                    }
                                    isServiceRunning = true
                                }
                            } else {
                                OverlayHUDService.stop(context)
                                isServiceRunning = false
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = VoidBase,
                            checkedTrackColor = HyperLime,
                            uncheckedThumbColor = TextSecondaryDark,
                            uncheckedTrackColor = SurfaceTier2
                        )
                    )
                }
            }
        }

        // Action Buttons Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onNavigateSimulator,
                    colors = ButtonDefaults.buttonColors(containerColor = HyperLime),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = VoidBase
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Test Simulator",
                        fontWeight = FontWeight.ExtraBold,
                        color = VoidBase,
                        fontSize = 13.sp
                    )
                }

                OutlinedButton(
                    onClick = onNavigateHistory,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = CyberCyan
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Audit Log ($scanCount)",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Architecture Pillars Grid
        item {
            Text(
                text = "ZERO-CLOUD ARCHITECTURE GUARANTEES",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextSecondaryDark,
                letterSpacing = 1.2.sp
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 100% Private - Tinted ShieldEmerald Icon Badge
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .specularGlassCard(
                            shape = RoundedCornerShape(16.dp),
                            backgroundColor = SurfaceTier2.copy(alpha = 0.85f),
                            ambientGlow = ShieldEmerald.copy(alpha = 0.08f),
                            elevation = 8.dp
                        )
                        .padding(14.dp)
                ) {
                    Column {
                        Surface(
                            shape = CircleShape,
                            color = ShieldEmerald.copy(alpha = 0.15f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = ShieldEmerald,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = "100% Private", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "Zero Cloud or Network calls", fontSize = 10.sp, color = TextSecondaryDark)
                    }
                }

                // RAM Buffer - Tinted CyberCyan Icon Badge
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .specularGlassCard(
                            shape = RoundedCornerShape(16.dp),
                            backgroundColor = SurfaceTier2.copy(alpha = 0.85f),
                            ambientGlow = CyberCyan.copy(alpha = 0.08f),
                            elevation = 8.dp
                        )
                        .padding(14.dp)
                ) {
                    Column {
                        Surface(
                            shape = CircleShape,
                            color = CyberCyan.copy(alpha = 0.15f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Memory,
                                    contentDescription = null,
                                    tint = CyberCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = "RAM Buffer", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "Zero disk storage capture", fontSize = 10.sp, color = TextSecondaryDark)
                    }
                }

                // <100ms OCR - Tinted HyperLime Icon Badge
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .specularGlassCard(
                            shape = RoundedCornerShape(16.dp),
                            backgroundColor = SurfaceTier2.copy(alpha = 0.85f),
                            ambientGlow = HyperLime.copy(alpha = 0.08f),
                            elevation = 8.dp
                        )
                        .padding(14.dp)
                ) {
                    Column {
                        Surface(
                            shape = CircleShape,
                            color = HyperLime.copy(alpha = 0.15f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = HyperLime,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = "<100ms OCR", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "Instant entity intent routing", fontSize = 10.sp, color = TextSecondaryDark)
                    }
                }
            }
        }

        // Recent Scans Stream
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RECENT ON-DEVICE PARSED ENTITIES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextSecondaryDark,
                    letterSpacing = 1.2.sp
                )
                if (recentScans.isNotEmpty()) {
                    Text(
                        text = "View All",
                        fontSize = 11.sp,
                        color = HyperLime,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }
        }

        if (recentScans.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .specularGlassCard(
                            shape = RoundedCornerShape(18.dp),
                            backgroundColor = SurfaceTier1.copy(alpha = 0.9f),
                            ambientGlow = CyberCyan.copy(alpha = 0.05f),
                            elevation = 8.dp
                        )
                        .padding(24.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = null,
                                tint = TextSecondaryDark,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No scan history recorded yet",
                                fontSize = 13.sp,
                                color = TextSecondaryDark
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Open Test Simulator or pull edge handle to trigger scan",
                                fontSize = 11.sp,
                                color = TextSecondaryDark.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        } else {
            items(recentScans.take(4), key = { it.id }) { scan ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .specularGlassCard(
                            shape = RoundedCornerShape(14.dp),
                            backgroundColor = SurfaceTier2.copy(alpha = 0.85f),
                            ambientGlow = HyperLime.copy(alpha = 0.05f),
                            elevation = 6.dp
                        )
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = scan.formattedValue,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Type: ${scan.entityType} • Action: ${scan.primaryActionLabel}",
                                fontSize = 10.sp,
                                color = TextSecondaryDark
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = HyperLime.copy(alpha = 0.12f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = HyperLime,
                                modifier = Modifier
                                    .padding(6.dp)
                                    .size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
