package com.praxim.core.domain.model

data class ScanAuditRecord(
    val id: Long,
    val timestamp: Long,
    val rawPayload: String,
    val targetAppPackage: String?,
    val recognizedEntity: RecognizedEntity
)
