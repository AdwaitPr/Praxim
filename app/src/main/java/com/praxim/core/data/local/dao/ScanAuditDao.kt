package com.praxim.core.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.praxim.core.data.local.entity.EntityType
import com.praxim.core.data.local.entity.ScanAuditEntity

@Dao
interface ScanAuditDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ScanAuditEntity): Long

    @Query("SELECT * FROM scan_audit_log ORDER BY timestamp DESC")
    fun getPagedScans(): PagingSource<Int, ScanAuditEntity>

    @Query("SELECT * FROM scan_audit_log WHERE entity_type = :type ORDER BY timestamp DESC")
    fun getPagedScansByType(type: EntityType): PagingSource<Int, ScanAuditEntity>

    @Query("""
        SELECT log.* FROM scan_audit_log log
        JOIN scan_audit_fts fts ON log.id = fts.rowid
        WHERE scan_audit_fts MATCH :query
        ORDER BY timestamp DESC
    """)
    fun searchAuditLogs(query: String): PagingSource<Int, ScanAuditEntity>

    @Query("DELETE FROM scan_audit_log WHERE timestamp < :thresholdTimestamp")
    fun pruneOlderThan(thresholdTimestamp: Long): Int

    @Query("""
        DELETE FROM scan_audit_log
        WHERE id IN (
            SELECT id FROM scan_audit_log
            ORDER BY timestamp DESC
            LIMIT -1 OFFSET :maxCapacity
        )
    """)
    fun pruneExceedingCapacity(maxCapacity: Int): Int

    @Transaction
    fun applyVolumetricRetention(maxCapacity: Int, oldestAllowedTimestamp: Long) {
        pruneOlderThan(oldestAllowedTimestamp)
        pruneExceedingCapacity(maxCapacity)
    }
}
