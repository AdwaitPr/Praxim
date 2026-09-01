package com.example.praxim.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.praxim.data.ScanHistoryEntity
import com.example.praxim.data.ScanHistoryRepository
import com.example.praxim.engine.EntityRecognizerEngine
import com.example.praxim.model.RecognizedEntity
import com.example.praxim.ui.hud.EdgePillView
import com.example.praxim.ui.hud.ExpandedActionHudView
import com.example.praxim.ui.hud.HudDisplayMode
import com.example.praxim.ui.hud.HudSettings
import com.example.praxim.ui.hud.ProcessingShimmerView
import com.example.praxim.ui.theme.AmoledBlack
import com.example.praxim.ui.theme.DarkObsidian
import com.example.praxim.ui.theme.GlassBorder
import com.example.praxim.ui.theme.NeonCyan
import com.example.praxim.ui.theme.NeonGreen
import com.example.praxim.ui.theme.SurfaceDark
import com.example.praxim.ui.theme.TextPrimaryDark
import com.example.praxim.ui.theme.TextSecondaryDark
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class ScenarioSample(
    val title: String,
    val category: String,
    val screenText: String
)

val SAMPLE_SCENARIOS = listOf(
    ScenarioSample(
        title = "Swiggy UPI Bill",
        category = "UPI Payment",
        screenText = "Order #82914 Delivered!\nTotal Bill: ₹450 paid to swiggy.pay@okicici\nBackup VPA: merchant.swiggy@ybl\nSupport: +91 9876543210"
    ),
    ScenarioSample(
        title = "Bank NEFT Transfer",
        category = "Bank IFSC",
        screenText = "Bank Account Credited: ₹15,000\nFrom Sender: Alex Kumar\nBank IFSC Code: SBIN0001234 (State Bank of India)\nRef ID: 9812039481"
    ),
    ScenarioSample(
        title = "WhatsApp Chat",
        category = "Contact & Link",
        screenText = "Hey! Please call me at +91 9123456789 or check the link: https://upi.link/pay/alex to complete your transaction."
    ),
    ScenarioSample(
        title = "Amazon Courier SMS",
        category = "Tracking ID",
        screenText = "Your Amazon shipment is out for delivery with Ekart.\nTracking AWB: EK10293847501\nTrack here: www.amazon.in/orders"
    )
)

@Composable
fun InteractiveSimulatorScreen(
    repository: ScanHistoryRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedScenario by remember { mutableStateOf(SAMPLE_SCENARIOS[0]) }
    var customText by remember { mutableStateOf(selectedScenario.screenText) }
    var hudMode by remember { mutableStateOf<HudDisplayMode>(HudDisplayMode.Collapsed) }

    fun triggerSimulatorScan() {
        coroutineScope.launch {
            hudMode = HudDisplayMode.Processing
            delay(150) // Simulate <100ms local RAM Frame Buffer ML Kit OCR
            val parsed = EntityRecognizerEngine.parseTextEntities(customText)
            hudMode = HudDisplayMode.Expanded(parsed)

            // Save to Room DB
            parsed.forEach { entity ->
                repository.insert(
                    ScanHistoryEntity(
                        rawText = entity.rawText,
                        formattedValue = entity.formattedValue,
                        entityType = entity.type.name,
                        primaryActionLabel = entity.primaryActionLabel,
                        timestamp = entity.timestamp
                    )
                )
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimaryDark)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "MICRO-HUD SIMULATOR",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    Text(
                        text = "Test edge handle gesture & instant entity parsing",
                        fontSize = 11.sp,
                        color = NeonGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Preset Scenario Chips
            Text(
                text = "SELECT MOCK SCREEN SCENARIO",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondaryDark,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(SAMPLE_SCENARIOS) { sample ->
                    val isSelected = sample.title == selectedScenario.title
                    AssistChip(
                        onClick = {
                            selectedScenario = sample
                            customText = sample.screenText
                            hudMode = HudDisplayMode.Collapsed
                        },
                        label = {
                            Text(
                                text = sample.title,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) AmoledBlack else TextPrimaryDark
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (isSelected) NeonGreen else SurfaceDark
                        ),
                        border = AssistChipDefaults.assistChipBorder(
                            enabled = !isSelected,
                            borderColor = GlassBorder
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Simulated Mobile Screen Container
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .border(1.dp, GlassBorder, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkObsidian),
                shape = RoundedCornerShape(24.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Phone Mock Status Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "9:41", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondaryDark)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.PhoneAndroid, contentDescription = null, tint = TextSecondaryDark, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "5G", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondaryDark)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Active Screen Header
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SurfaceDark
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(NeonCyan))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Mock App: ${selectedScenario.category}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Editable Screen Text Content
                        OutlinedTextField(
                            value = customText,
                            onValueChange = { customText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GlassBorder,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = SurfaceDark.copy(alpha = 0.5f),
                                unfocusedContainerColor = SurfaceDark.copy(alpha = 0.5f),
                                focusedTextColor = TextPrimaryDark,
                                unfocusedTextColor = TextPrimaryDark
                            ),
                            shape = RoundedCornerShape(14.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 20.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { triggerSimulatorScan() },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Bolt, contentDescription = null, tint = AmoledBlack)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "PULL EDGE HANDLE OR TAP TO OCR SCAN",
                                fontWeight = FontWeight.ExtraBold,
                                color = AmoledBlack,
                                fontSize = 12.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    // Simulated Edge Pill Handle on Mock Screen Right Border
                    if (hudMode is HudDisplayMode.Collapsed) {
                        EdgePillView(
                            settings = HudSettings(anchorRight = true),
                            onTriggerScan = { triggerSimulatorScan() },
                            modifier = Modifier.align(Alignment.CenterEnd)
                        )
                    }
                }
            }
        }

        // Processing Shimmer or Expanded HUD overlay inside Simulator
        when (val mode = hudMode) {
            is HudDisplayMode.Processing -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    ProcessingShimmerView()
                }
            }
            is HudDisplayMode.Expanded -> {
                ExpandedActionHudView(
                    entities = mode.entities,
                    onDismiss = { hudMode = HudDisplayMode.Collapsed },
                    onEntityActionExecuted = { entity, action ->
                        coroutineScope.launch {
                            repository.insert(
                                ScanHistoryEntity(
                                    rawText = entity.rawText,
                                    formattedValue = entity.formattedValue,
                                    entityType = entity.type.name,
                                    primaryActionLabel = entity.primaryActionLabel,
                                    actionExecuted = action
                                )
                            )
                        }
                    }
                )
            }
            else -> {}
        }
    }
}
