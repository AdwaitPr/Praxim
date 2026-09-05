package com.praxim.core.data.local.database

import android.content.Context
import android.util.Log
import net.sqlcipher.DatabaseErrorHandler
import net.sqlcipher.database.SQLiteDatabase
import java.io.File

class ResilientDatabaseErrorHandler(private val context: Context) : DatabaseErrorHandler {
    override fun onCorruption(dbObj: SQLiteDatabase?) {
        Log.e("DatabaseError", "Corruption detected in database.")
        dbObj?.let {
            if (it.isOpen) {
                try {
                    it.close()
                } catch (e: Exception) {
                    Log.e("DatabaseError", "Error closing corrupt database.", e)
                }
            }
        }

        val dbPath = dbObj?.path ?: context.getDatabasePath("praxim_audit.db").absolutePath
        val dbFile = File(dbPath)

        if (dbFile.exists()) {
            val quarantineDir = File(context.filesDir, "databases/quarantine")
            if (!quarantineDir.exists()) {
                quarantineDir.mkdirs()
            }

            val timestamp = System.currentTimeMillis()
            val dbName = dbFile.name

            val filesToMove = listOf(
                dbFile,
                File(dbFile.parent, "$dbName-wal"),
                File(dbFile.parent, "$dbName-shm")
            )

            filesToMove.forEach { file ->
                if (file.exists()) {
                    val destFile = File(quarantineDir, "${file.name}_$timestamp")
                    file.renameTo(destFile)
                }
            }
        }
    }
}
