package com.praxim.engine.persistence

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

class EncryptedDatabaseProvider {
    companion object {
        @Volatile
        private var instance: RoomDatabase? = null

        fun getDatabase(context: Context, dbClass: Class<out RoomDatabase>, dbName: String): RoomDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context, dbClass, dbName).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context, dbClass: Class<out RoomDatabase>, dbName: String): RoomDatabase {
            val passphrase = KeyStoreSecretProvider.getDatabasePassphrase()
            val factory = SupportFactory(passphrase)

            val builder = Room.databaseBuilder(context.applicationContext, dbClass, dbName)
                .openHelperFactory(factory)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        // Perform any necessary runtime pragmas for SQLCipher here if needed
                    }
                })

            val db = builder.build()

            // Shred passphrase array in memory after use
            KeyStoreSecretProvider.shredByteArray(passphrase)

            return db
        }
    }
}
