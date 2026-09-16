package com.praxim.engine.persistence

import androidx.room.Entity
import androidx.room.Fts4

@Fts4(contentEntity = ScanAuditEntity::class)
@Entity(tableName = "scan_audits_fts")
data class ScanAuditFtsEntity(
    val riskLevel: String,
    val payloadMetadata: String
)
