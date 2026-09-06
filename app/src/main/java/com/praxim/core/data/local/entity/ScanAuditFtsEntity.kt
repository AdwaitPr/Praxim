package com.praxim.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4

@Entity(tableName = "scan_audit_fts")
@Fts4(contentEntity = ScanAuditEntity::class)
data class ScanAuditFtsEntity(
    @ColumnInfo(name = "upi_payee_name")
    val upiPayeeName: String?,
    @ColumnInfo(name = "upi_transaction_note")
    val upiTransactionNote: String?,
    @ColumnInfo(name = "ifsc_bank_name")
    val ifscBankName: String?
)
