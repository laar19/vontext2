package dev.laar19.vontext

import dev.laar19.vontext.data.SecureKeyStoreHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SecureKeyStoreUnitTest {

    @Test
    fun testEncryptionAndDecryption() {
        val originalKey = "sk-proj-superSecretApiKey1234567890abcdef"
        val encrypted = SecureKeyStoreHelper.encrypt(originalKey)
        
        assertTrue(encrypted.isNotEmpty())
        assertNotEquals(originalKey, encrypted)

        val decrypted = SecureKeyStoreHelper.decrypt(encrypted)
        assertEquals(originalKey, decrypted)
    }

    @Test
    fun testEmptyStringEncryption() {
        val emptyEncrypted = SecureKeyStoreHelper.encrypt("")
        assertEquals("", emptyEncrypted)

        val emptyDecrypted = SecureKeyStoreHelper.decrypt("")
        assertEquals("", emptyDecrypted)
    }
}
