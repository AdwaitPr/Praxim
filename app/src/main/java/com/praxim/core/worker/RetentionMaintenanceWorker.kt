package com.praxim.core.worker

import android.content.Context
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.praxim.core.data.local.database.PraximDatabase
import com.praxim.core.data.local.database.ResilientDatabaseErrorHandler
import com.praxim.core.security.SecurityKeyManager

class RetentionMaintenanceWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val appContext = applicationContext

        // In a real scenario, preferences would be injected or read from DataStore.
        // Hardcoded defaults per requirements.
        val retentionDays = 30L // e.g. 30 days
        val maxCapacity = 5000

        val oldestAllowedTimestamp = System.currentTimeMillis() - (retentionDays * 24 * 60 * 60 * 1000)

        val keyManager = SecurityKeyManager(appContext)
        val passphrase = keyManager.getOrGeneratePassphrase()

        return try {
            val db = PraximDatabase.getDatabase(
                appContext,
                passphrase,
                ResilientDatabaseErrorHandler(appContext)
            )

            // Apply pruning inside transaction
            db.scanAuditDao().applyVolumetricRetention(maxCapacity, oldestAllowedTimestamp)

            // Execute incremental vacuum
            db.openHelper.writableDatabase.query(SimpleSQLiteQuery("PRAGMA incremental_vacuum(500);")).moveToNext()

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        } finally {
            keyManager.sanitizeMemory(passphrase)
        }
    }
}
