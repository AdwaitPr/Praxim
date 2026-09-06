package com.praxim.core.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.praxim.core.data.local.converter.AuditConverters
import com.praxim.core.data.local.dao.ScanAuditDao
import com.praxim.core.data.local.entity.ScanAuditEntity
import com.praxim.core.data.local.entity.ScanAuditFtsEntity
import net.sqlcipher.database.SupportFactory
import net.sqlcipher.database.SQLiteDatabaseHook

@Database(
    entities = [ScanAuditEntity::class, ScanAuditFtsEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(AuditConverters::class)
abstract class PraximDatabase : RoomDatabase() {

    abstract fun scanAuditDao(): ScanAuditDao

    companion object {
        @Volatile
        private var INSTANCE: PraximDatabase? = null

        fun getDatabase(
            context: Context,
            passphrase: ByteArray,
            errorHandler: ResilientDatabaseErrorHandler
        ): PraximDatabase {
            return INSTANCE ?: synchronized(this) {
                // Initialize SQLCipher native libraries
                net.sqlcipher.database.SQLiteDatabase.loadLibs(context)

                val sqliteDatabaseHook = object : SQLiteDatabaseHook {
                    override fun preKey(database: net.sqlcipher.database.SQLiteDatabase?) {}
                    override fun postKey(database: net.sqlcipher.database.SQLiteDatabase?) {
                        database?.rawExecSQL("PRAGMA journal_mode = WAL;")
                        database?.rawExecSQL("PRAGMA synchronous = NORMAL;")
                        database?.rawExecSQL("PRAGMA busy_timeout = 30000;")
                        database?.rawExecSQL("PRAGMA auto_vacuum = INCREMENTAL;")
                    }
                }

                // SupportFactory doesn't take an error handler directly in constructor, it takes boolean for clearPassphrase
                val factory = SupportFactory(passphrase, sqliteDatabaseHook, true)

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PraximDatabase::class.java,
                    "praxim_audit.db"
                )
                .openHelperFactory(factory)
                .fallbackToDestructiveMigration() // Useful for testing or simpler handling if schema changes during dev
                .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
