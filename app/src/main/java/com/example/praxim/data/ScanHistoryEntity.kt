package com.example.praxim.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_history")
data class ScanHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rawText: String,
    val formattedValue: String,
    val entityType: String,
    val primaryActionLabel: String,
    val timestamp: Long = System.currentTimeMillis(),
    val actionExecuted: String? = null
)
