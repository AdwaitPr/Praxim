package com.praxim.core.data.repository

import androidx.paging.PagingData
import com.praxim.core.domain.model.ScanAuditRecord
import kotlinx.coroutines.flow.Flow

interface ScanHistoryRepository {
    fun getPagedHistory(): Flow<PagingData<ScanAuditRecord>>
    fun searchHistory(query: String): Flow<PagingData<ScanAuditRecord>>
}
