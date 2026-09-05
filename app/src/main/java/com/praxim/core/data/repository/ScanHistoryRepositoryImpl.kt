package com.praxim.core.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.praxim.core.data.local.dao.ScanAuditDao
import com.praxim.core.data.local.entity.EntityType
import com.praxim.core.data.local.entity.ScanAuditEntity
import com.praxim.core.domain.model.RecognizedEntity
import com.praxim.core.domain.model.ScanAuditRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class ScanHistoryRepositoryImpl(
    private val scanAuditDao: ScanAuditDao
) : ScanHistoryRepository {

    private val dbDispatcher = Dispatchers.IO.limitedParallelism(4)

    private val pagingConfig = PagingConfig(
        pageSize = 30,
        prefetchDistance = 10,
        enablePlaceholders = false
    )

    override fun getPagedHistory(): Flow<PagingData<ScanAuditRecord>> {
        return Pager(
            config = pagingConfig,
            pagingSourceFactory = { scanAuditDao.getPagedScans() }
        ).flow.map { pagingData ->
            pagingData.map { entity -> entity.toDomainModel() }
        }.flowOn(dbDispatcher)
    }

    override fun searchHistory(query: String): Flow<PagingData<ScanAuditRecord>> {
        return Pager(
            config = pagingConfig,
            pagingSourceFactory = { scanAuditDao.searchAuditLogs(query) }
        ).flow.map { pagingData ->
            pagingData.map { entity -> entity.toDomainModel() }
        }.flowOn(dbDispatcher)
    }

    private fun ScanAuditEntity.toDomainModel(): ScanAuditRecord {
        val recognizedEntity = when (entityType) {
            EntityType.UPI -> RecognizedEntity.Upi(
                vpa = upiVpa ?: "",
                payeeName = upiPayeeName,
                amount = upiAmount,
                transactionNote = upiTransactionNote
            )
            EntityType.IFSC -> RecognizedEntity.Ifsc(
                ifscCode = ifscCode ?: "",
                accountNumber = ifscAccountNumber,
                bankName = ifscBankName
            )
            EntityType.PHONE -> RecognizedEntity.Phone(
                rawNumber = phoneRawNumber ?: "",
                formattedNumber = phoneFormattedNumber
            )
        }

        return ScanAuditRecord(
            id = id,
            timestamp = timestamp,
            rawPayload = rawPayload,
            targetAppPackage = targetAppPackage,
            recognizedEntity = recognizedEntity
        )
    }
}
