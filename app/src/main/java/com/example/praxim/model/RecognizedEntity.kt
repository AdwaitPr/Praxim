package com.example.praxim.model

import androidx.compose.ui.graphics.Color
import com.example.praxim.ui.theme.AlertAmber
import com.example.praxim.ui.theme.CyberCyan

enum class EntityType(val displayName: String, val chipColor: Color) {
    UPI_ID("UPI Payment", CyberCyan),
    IFSC_CODE("Bank IFSC", CyberCyan),
    PHONE_NUMBER("Phone Call/WhatsApp", CyberCyan),
    URL_LINK("Web Link", CyberCyan),
    TRACKING_ID("Courier Tracking", AlertAmber),
    DATE_EVENT("Calendar Event", CyberCyan),
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
