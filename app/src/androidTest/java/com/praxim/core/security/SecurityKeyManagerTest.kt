package com.praxim.core.security

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class SecurityKeyManagerTest {

    @Test
    fun testPassphraseMemorySanitization() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val keyManager = SecurityKeyManager(context)

        // Clean up any existing file for a fresh test
        val file = File(context.filesDir, "praxim_db_pass.enc")
        if (file.exists()) file.delete()

        val passphrase = keyManager.getOrGeneratePassphrase()

        // Verify it's generated and not all zeros initially
        var allZeros = true
        for (b in passphrase) {
            if (b != 0.toByte()) {
                allZeros = false
                break
            }
        }
        assertEquals(false, allZeros)

        keyManager.sanitizeMemory(passphrase)

        // Verify it's all zeros after sanitization
        val expectedZeros = ByteArray(passphrase.size) { 0 }
        assertArrayEquals(expectedZeros, passphrase)
    }

    @Test
    fun testPassphrasePersistence() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val keyManager = SecurityKeyManager(context)

        // Clean up
        val file = File(context.filesDir, "praxim_db_pass.enc")
        if (file.exists()) file.delete()

        val passphrase1 = keyManager.getOrGeneratePassphrase()

        // Need to copy because the original will be sanitized or could be
        val passphrase1Copy = passphrase1.copyOf()
        keyManager.sanitizeMemory(passphrase1)

        val passphrase2 = keyManager.getOrGeneratePassphrase()

        assertArrayEquals(passphrase1Copy, passphrase2)

        keyManager.sanitizeMemory(passphrase2)
    }
}
