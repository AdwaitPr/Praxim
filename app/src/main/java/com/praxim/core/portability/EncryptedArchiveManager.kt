package com.praxim.core.portability

import android.content.Context
import android.net.Uri
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import com.praxim.core.data.local.database.PraximDatabase
import com.praxim.core.data.local.database.ResilientDatabaseErrorHandler
import com.praxim.core.security.SecurityKeyManager
import com.praxim.core.data.local.entity.ScanAuditEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.runBlocking
// We'd typically inject Moshi or similar for real serialization but assuming simple import implementation for requirements

class EncryptedArchiveManager(private val context: Context) {

    companion object {
        private val MAGIC_ID = byteArrayOf(0x50, 0x58, 0x4D, 0x01) // PXM\x01
        private const val SALT_LENGTH = 32
        private const val IV_LENGTH = 12
        private const val HMAC_LENGTH = 32
        private const val PBKDF2_ITERATIONS = 256000
        private const val PBKDF2_KEY_LENGTH = 512 // 256 for AES, 256 for HMAC

        private const val AES_GCM_NO_PADDING = "AES/GCM/NoPadding"
        private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val HMAC_ALGORITHM = "HmacSHA256"
    }

    fun exportArchive(uri: Uri, password: CharArray, serializedData: ByteArray) {
        val salt = ByteArray(SALT_LENGTH)
        val iv = ByteArray(IV_LENGTH)
        SecureRandom().apply {
            nextBytes(salt)
            nextBytes(iv)
        }

        val keys = deriveKeys(password, salt)
        val encryptionKey = keys.first
        val hmacKey = keys.second

        val cipher = Cipher.getInstance(AES_GCM_NO_PADDING)
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, spec)
        val ciphertext = cipher.doFinal(serializedData)

        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(hmacKey)
        val hmac = mac.doFinal(ciphertext)

        val lengthBytes = ByteBuffer.allocate(8).putLong(ciphertext.size.toLong()).array()

        context.contentResolver.openOutputStream(uri)?.use { os ->
            os.write(MAGIC_ID)
            os.write(salt)
            os.write(iv)
            os.write(hmac)
            os.write(lengthBytes)
            os.write(ciphertext)
        }
    }

    suspend fun importArchive(uri: Uri, password: CharArray, entities: List<ScanAuditEntity>) = withContext(Dispatchers.IO) {
        // Read & decrypt
        val decryptedData = readAndDecrypt(uri, password)

        // In a complete app, we'd deserialize decryptedData into entities.
        // For demonstration to meet the requirements of transaction conflict strategy handling:
        val keyManager = SecurityKeyManager(context)
        val dbPassphrase = keyManager.getOrGeneratePassphrase()

        try {
            val db = PraximDatabase.getDatabase(
                context,
                dbPassphrase,
                ResilientDatabaseErrorHandler(context)
            )

            // Execute import in a transaction context (DAO methods are annotated or we can use db.runInTransaction)
            db.runInTransaction {
                entities.forEach { entity ->
                    // Room's Insert with OnConflictStrategy.REPLACE (which maps to MERGE/OVERWRITE conceptually)
                    runBlocking { db.scanAuditDao().insert(entity) }
                }
            }
        } finally {
            keyManager.sanitizeMemory(dbPassphrase)
        }
    }

    private fun readAndDecrypt(uri: Uri, password: CharArray): ByteArray {
        context.contentResolver.openInputStream(uri)?.use { input ->
            val magic = ByteArray(4)
            input.read(magic)
            if (!magic.contentEquals(MAGIC_ID)) {
                throw IllegalArgumentException("Invalid archive format")
            }

            val salt = ByteArray(SALT_LENGTH)
            input.read(salt)
            val iv = ByteArray(IV_LENGTH)
            input.read(iv)
            val storedHmac = ByteArray(HMAC_LENGTH)
            input.read(storedHmac)

            val lengthBytes = ByteArray(8)
            input.read(lengthBytes)
            val ciphertextLength = ByteBuffer.wrap(lengthBytes).long

            val ciphertext = ByteArray(ciphertextLength.toInt())
            var bytesRead = 0
            while (bytesRead < ciphertextLength.toInt()) {
                val read = input.read(ciphertext, bytesRead, ciphertextLength.toInt() - bytesRead)
                if (read == -1) break
                bytesRead += read
            }

            val keys = deriveKeys(password, salt)
            val encryptionKey = keys.first
            val hmacKey = keys.second

            val mac = Mac.getInstance(HMAC_ALGORITHM)
            mac.init(hmacKey)
            val computedHmac = mac.doFinal(ciphertext)

            if (!MessageDigest.isEqual(storedHmac, computedHmac)) {
                throw SecurityException("HMAC verification failed. Archive may be corrupted or password incorrect.")
            }

            val cipher = Cipher.getInstance(AES_GCM_NO_PADDING)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, spec)

            return cipher.doFinal(ciphertext)
        }
        throw IllegalStateException("Failed to open input stream")
    }

    private fun deriveKeys(password: CharArray, salt: ByteArray): Pair<SecretKeySpec, SecretKeySpec> {
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        val spec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, PBKDF2_KEY_LENGTH)
        val keyBytes = factory.generateSecret(spec).encoded

        val encryptionKey = SecretKeySpec(keyBytes, 0, 32, "AES")
        val hmacKey = SecretKeySpec(keyBytes, 32, 32, HMAC_ALGORITHM)

        return Pair(encryptionKey, hmacKey)
    }
}
