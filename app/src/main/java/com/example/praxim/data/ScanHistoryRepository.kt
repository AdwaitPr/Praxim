package com.example.praxim.data

import kotlinx.coroutines.flow.Flow

class ScanHistoryRepository(private val dao: ScanHistoryDao) {
    val allHistory: Flow<List<ScanHistoryEntity>> = dao.getAllScanHistory()
    val totalScans: Flow<Int> = dao.getScanCount()

    suspend fun insert(entity: ScanHistoryEntity) = dao.insertScan(entity)

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun clearAll() = dao.clearAll()
}
