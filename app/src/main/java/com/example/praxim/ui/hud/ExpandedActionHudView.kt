package com.example.praxim.ui.hud

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.praxim.engine.EntityRecognizerEngine
import com.example.praxim.model.EntityType
import com.example.praxim.model.RecognizedEntity
import com.example.praxim.ui.theme.GlassBorder
import com.example.praxim.ui.theme.HyperLime
import com.example.praxim.ui.theme.ShieldEmerald
import com.example.praxim.ui.theme.SurfaceTier1
import com.example.praxim.ui.theme.SurfaceTier2
import com.example.praxim.ui.theme.TextPrimaryDark
import com.example.praxim.ui.theme.TextSecondaryDark
import com.example.praxim.ui.theme.VoidBase
import com.example.praxim.ui.theme.specularGlassCard

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpandedActionHudView(
    entities: List<RecognizedEntity>,
    onDismiss: () -> Unit,
    onEntityActionExecuted: (RecognizedEntity, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var lastCopiedId by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VoidBase.copy(alpha = 0.65f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        // Prevent click pass-through on the main card container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = true, onClick = {})
                .specularGlassCard(
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    backgroundColor = SurfaceTier1.copy(alpha = 0.96f),
                    ambientGlow = HyperLime.copy(alpha = 0.12f),
                    elevation = 24.dp
                )
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Handle Bar
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Header Row: App Identity + Privacy Status Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(HyperLime)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "LUMINATE MICRO-HUD",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = HyperLime,
                            letterSpacing = 1.2.sp
                        )
                    }

                    // 100% On-Device Badge
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = ShieldEmerald.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ShieldEmerald.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Private On-Device",
                                tint = ShieldEmerald,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "100% ON-DEVICE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ShieldEmerald
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close HUD",
                            tint = TextSecondaryDark
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (entities.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = TextSecondaryDark,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No actionable entities detected on active screen",
                                fontSize = 13.sp,
                                color = TextSecondaryDark
                            )
                        }
                    }
                } else {
                    Text(
                        text = "DETECTED SCREEN ENTITIES (${entities.size})",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextSecondaryDark,
                        letterSpacing = 1.2.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((entities.size * 130).coerceAtMost(320).dp)
                    ) {
                        items(entities, key = { it.id }) { entity ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .specularGlassCard(
                                        shape = RoundedCornerShape(16.dp),
                                        backgroundColor = SurfaceTier2.copy(alpha = 0.85f),
                                        ambientGlow = entity.type.chipColor.copy(alpha = 0.08f),
                                        elevation = 8.dp
                                    )
                                    .padding(14.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Entity Type Badge
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = entity.type.chipColor.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = entity.type.displayName.uppercase(),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = entity.type.chipColor,
                                                letterSpacing = 0.8.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                lastCopiedId = entity.id
                                                EntityRecognizerEngine.executeEntityAction(context, entity, isSecondaryAction = true)
                                                onEntityActionExecuted(entity, "Copy")
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (lastCopiedId == entity.id) Icons.Default.Check else Icons.Default.ContentCopy,
                                                contentDescription = "Copy text",
                                                tint = if (lastCopiedId == entity.id) HyperLime else TextSecondaryDark,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Monospaced entity value for precise layout
                                    Text(
                                        text = entity.formattedValue,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryDark,
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 0.5.sp
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Action Chips Row
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        // Primary Action Button (HyperLime core action)
                                        AssistChip(
                                            onClick = {
                                                EntityRecognizerEngine.executeEntityAction(context, entity, isSecondaryAction = false)
                                                onEntityActionExecuted(entity, entity.primaryActionLabel)
                                                onDismiss()
                                            },
                                            label = {
                                                Text(
                                                    text = entity.primaryActionLabel,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = VoidBase
                                                )
                                            },
                                            leadingIcon = {
                                                val icon = when (entity.type) {
                                                    EntityType.UPI_ID -> Icons.Default.Payment
                                                    EntityType.PHONE_NUMBER -> Icons.Default.Call
                                                    EntityType.URL_LINK -> Icons.Default.Language
                                                    EntityType.TRACKING_ID -> Icons.Default.LocalShipping
                                                    else -> Icons.Default.Send
                                                }
                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = null,
                                                    tint = VoidBase,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            },
                                            colors = AssistChipDefaults.assistChipColors(
                                                containerColor = HyperLime
                                            ),
                                            border = null
                                        )

                                        // Secondary Action Button with subtle glass border
                                        entity.secondaryActionLabel?.let { secondaryLabel ->
                                            AssistChip(
                                                onClick = {
                                                    EntityRecognizerEngine.executeEntityAction(context, entity, isSecondaryAction = true)
                                                    onEntityActionExecuted(entity, secondaryLabel)
                                                },
                                                label = {
                                                    Text(
                                                        text = secondaryLabel,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = TextPrimaryDark
                                                    )
                                                },
                                                colors = AssistChipDefaults.assistChipColors(
                                                    containerColor = Color.White.copy(alpha = 0.08f)
                                                ),
                                                border = AssistChipDefaults.assistChipBorder(
                                                    enabled = true,
                                                    borderColor = GlassBorder
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
