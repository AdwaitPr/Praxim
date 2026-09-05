package com.praxim.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import java.util.Arrays
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecurityKeyManager(private val context: Context) {

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val MASTER_KEK_ALIAS = "praxim_database_kek"
        private const val PASSPHRASE_FILE_NAME = "praxim_db_pass.enc"
        private const val AES_GCM_NO_PADDING = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128
        private const val PASSPHRASE_LENGTH = 32
    }

    private val keyStore: KeyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply {
        load(null)
    }

    fun getOrGeneratePassphrase(): ByteArray {
        val file = File(context.filesDir, PASSPHRASE_FILE_NAME)
        if (file.exists()) {
            return decryptPassphrase(file.readBytes())
        }

        val passphrase = ByteArray(PASSPHRASE_LENGTH)
        SecureRandom().nextBytes(passphrase)
        val encrypted = encryptPassphrase(passphrase)
        file.writeBytes(encrypted)
        return passphrase
    }

    private fun getMasterKek(): SecretKey {
        if (keyStore.containsAlias(MASTER_KEK_ALIAS)) {
            val entry = keyStore.getEntry(MASTER_KEK_ALIAS, null) as? KeyStore.SecretKeyEntry
            if (entry != null) return entry.secretKey
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_PROVIDER
        )

        val specBuilder = KeyGenParameterSpec.Builder(
            MASTER_KEK_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
         .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
         .setKeySize(256)

        return try {
            keyGenerator.init(if (android.os.Build.VERSION.SDK_INT >= 28) { specBuilder.setIsStrongBoxBacked(true).build() } else { specBuilder.build() })
            keyGenerator.generateKey()
        } catch (e: Exception) {
            // Fallback to TEE
            keyGenerator.init(if (android.os.Build.VERSION.SDK_INT >= 28) { specBuilder.setIsStrongBoxBacked(false).build() } else { specBuilder.build() })
            keyGenerator.generateKey()
        }
    }

    private fun encryptPassphrase(passphrase: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(AES_GCM_NO_PADDING)
        cipher.init(Cipher.ENCRYPT_MODE, getMasterKek())
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(passphrase)

        return iv + ciphertext
    }

    private fun decryptPassphrase(encryptedData: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(AES_GCM_NO_PADDING)
        val iv = encryptedData.copyOfRange(0, GCM_IV_LENGTH)
        val ciphertext = encryptedData.copyOfRange(GCM_IV_LENGTH, encryptedData.size)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, getMasterKek(), spec)
        return cipher.doFinal(ciphertext)
    }

    fun sanitizeMemory(passphrase: ByteArray) {
        Arrays.fill(passphrase, 0.toByte())
    }
}
