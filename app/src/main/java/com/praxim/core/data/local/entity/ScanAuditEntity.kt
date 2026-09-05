package com.praxim.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "scan_audit_log",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["entity_type"]),
        Index(value = ["upi_vpa"]),
        Index(value = ["ifsc_code"])
    ]
)
data class ScanAuditEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    @ColumnInfo(name = "raw_payload")
    val rawPayload: String,
    @ColumnInfo(name = "entity_type")
    val entityType: EntityType,
    @ColumnInfo(name = "target_app_package")
    val targetAppPackage: String?,

    // Sparse variant-specific fields for UPI
    @ColumnInfo(name = "upi_vpa")
    val upiVpa: String? = null,
    @ColumnInfo(name = "upi_payee_name")
    val upiPayeeName: String? = null,
    @ColumnInfo(name = "upi_amount")
    val upiAmount: Double? = null,
    @ColumnInfo(name = "upi_transaction_note")
    val upiTransactionNote: String? = null,

    // Sparse variant-specific fields for IFSC
    @ColumnInfo(name = "ifsc_code")
    val ifscCode: String? = null,
    @ColumnInfo(name = "ifsc_account_number")
    val ifscAccountNumber: String? = null,
    @ColumnInfo(name = "ifsc_bank_name")
    val ifscBankName: String? = null,

    // Sparse variant-specific fields for Phone
    @ColumnInfo(name = "phone_raw_number")
    val phoneRawNumber: String? = null,
    @ColumnInfo(name = "phone_formatted_number")
    val phoneFormattedNumber: String? = null
)
