package dev.laar19.vontext.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Hardware-backed AES-256-GCM encryption helper using Android Keystore.
 * Guarantees cryptographic protection for API keys and sensitive tokens on-device,
 * with graceful provider fallback for JVM unit test environments.
 */
object SecureKeyStoreHelper {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "vontext_aes_keystore_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128

    @Volatile
    private var cachedKey: SecretKey? = null

    private fun getKeyStore(): Pair<KeyStore, Boolean> {
        return try {
            val ks = KeyStore.getInstance(ANDROID_KEYSTORE)
            ks.load(null)
            Pair(ks, true)
        } catch (e: Exception) {
            // JVM / local unit test fallback
            val ks = KeyStore.getInstance(KeyStore.getDefaultType())
            ks.load(null)
            Pair(ks, false)
        }
    }

    private fun initOrGetKey(): SecretKey {
        cachedKey?.let { return it }

        synchronized(this) {
            cachedKey?.let { return it }

            val (keyStore, isAndroidKeyStore) = getKeyStore()

            if (isAndroidKeyStore) {
                if (!keyStore.containsAlias(KEY_ALIAS)) {
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
                    val generated = keyGenerator.generateKey()
                    cachedKey = generated
                    return generated
                }
                val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
                val key = entry?.secretKey ?: throw IllegalStateException("No se pudo obtener la clave secreta de Android Keystore")
                cachedKey = key
                return key
            } else {
                // Local JVM Fallback (e.g. Robolectric / JUnit tests)
                val keyGenerator = KeyGenerator.getInstance("AES")
                keyGenerator.init(256)
                val generated = keyGenerator.generateKey()
                cachedKey = generated
                return generated
            }
        }
    }

    /**
     * Encrypts plaintext string using AES-GCM.
     * Returns Base64-encoded payload containing [IV length (1 byte) + IV + Ciphertext].
     */
    fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return ""
        return try {
            val secretKey = initOrGetKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

            val combined = ByteArray(1 + iv.size + cipherText.size)
            combined[0] = iv.size.toByte()
            System.arraycopy(iv, 0, combined, 1, iv.size)
            System.arraycopy(cipherText, 0, combined, 1 + iv.size, cipherText.size)

            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * Decrypts Base64-encoded payload containing [IV length + IV + Ciphertext].
     */
    fun decrypt(encryptedBase64: String): String {
        if (encryptedBase64.isEmpty()) return ""
        return try {
            val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            if (combined.isEmpty()) return ""

            val ivSize = combined[0].toInt() and 0xFF
            if (combined.size < 1 + ivSize) return ""

            val iv = ByteArray(ivSize)
            System.arraycopy(combined, 1, iv, 0, ivSize)

            val cipherTextSize = combined.size - 1 - ivSize
            val cipherText = ByteArray(cipherTextSize)
            System.arraycopy(combined, 1 + ivSize, cipherText, 0, cipherTextSize)

            val secretKey = initOrGetKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            val decryptedBytes = cipher.doFinal(cipherText)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
}
