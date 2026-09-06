package com.praxim.core.domain.model

sealed interface RecognizedEntity {
    data class Upi(
        val vpa: String,
        val payeeName: String?,
        val amount: Double?,
        val transactionNote: String?
    ) : RecognizedEntity

    data class Ifsc(
        val ifscCode: String,
        val accountNumber: String?,
        val bankName: String?
    ) : RecognizedEntity

    data class Phone(
        val rawNumber: String,
        val formattedNumber: String?
    ) : RecognizedEntity
}
