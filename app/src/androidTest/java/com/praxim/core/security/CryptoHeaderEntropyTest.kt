package com.praxim.core.security

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.praxim.core.data.local.database.PraximDatabase
import com.praxim.core.data.local.database.ResilientDatabaseErrorHandler
import com.praxim.core.data.local.entity.EntityType
import com.praxim.core.data.local.entity.ScanAuditEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileInputStream

@RunWith(AndroidJUnit4::class)
class CryptoHeaderEntropyTest {

    @Test
    fun testDatabaseIsEncrypted() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        // Ensure database exists and is populated
        val keyManager = SecurityKeyManager(context)
        val passphrase = keyManager.getOrGeneratePassphrase()

        val db = PraximDatabase.getDatabase(context, passphrase, ResilientDatabaseErrorHandler(context))

        db.scanAuditDao().insert(
            ScanAuditEntity(
                timestamp = System.currentTimeMillis(),
                rawPayload = "Test Payload",
                entityType = EntityType.UPI,
                targetAppPackage = "com.test",
                upiVpa = "test@upi"
            )
        )

        db.close()

        val dbFile = context.getDatabasePath("praxim_audit.db")
        assertTrue(dbFile.exists())

        // Read the first 16 bytes of the file.
        // A standard SQLite database starts with "SQLite format 3\000".
        val headerBytes = ByteArray(16)
        FileInputStream(dbFile).use { it.read(headerBytes) }

        val headerString = String(headerBytes, Charsets.UTF_8)

        // Verify the file does not contain the standard SQLite header
        assertFalse(headerString.startsWith("SQLite format 3"))

        keyManager.sanitizeMemory(passphrase)
    }
}
