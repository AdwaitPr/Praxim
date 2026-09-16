package com.praxim.engine.persistence

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.util.Arrays
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class KeyStoreSecretProvider {

    companion object {
        private const val KEY_ALIAS = "PraximDbEncryptionKey"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"

        fun getDatabasePassphrase(): ByteArray {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)

            if (!keyStore.containsAlias(KEY_ALIAS)) {
                generateKey()
            }

            val entry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry
            val secretKey = entry.secretKey
            return secretKey.encoded
        }

        private fun generateKey() {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()

            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()
        }

        fun shredCharArray(buffer: CharArray?) {
            if (buffer != null) {
                Arrays.fill(buffer, '\u0000')
            }
        }

        fun shredByteArray(buffer: ByteArray?) {
            if (buffer != null) {
                Arrays.fill(buffer, 0.toByte())
            }
        }
    }
}
