package com.example.praxim.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.AlertAmber
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen

enum class EntityType(val displayName: String, val chipColor: Color) {
    UPI_ID("UPI Payment", NeonGreen),
    IFSC_CODE("Bank IFSC", NeonCyan),
    PHONE_NUMBER("Phone Call/WhatsApp", ElectricBlue),
    URL_LINK("Web Link", ElectricBlue),
    TRACKING_ID("Courier Tracking", AlertAmber),
    DATE_EVENT("Calendar Event", NeonCyan),
    GENERIC_TEXT("Raw Text", Color.Gray)
}

data class NormalizedRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

data class RecognizedEntity(
    val id: String,
    val rawText: String,
    val formattedValue: String,
    val type: EntityType,
    val primaryActionLabel: String,
    val secondaryActionLabel: String? = null,
    val boundingBox: NormalizedRect? = null,
    val timestamp: Long = System.currentTimeMillis()
)
