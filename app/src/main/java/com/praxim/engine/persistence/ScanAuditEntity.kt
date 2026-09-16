package com.praxim.engine.persistence

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_audits")
data class ScanAuditEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val isFraud: Boolean,
    val riskLevel: String,
    val payloadMetadata: String // Encrypted payload info
)
