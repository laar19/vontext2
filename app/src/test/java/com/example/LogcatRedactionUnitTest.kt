package com.example

import com.example.util.LogcatHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LogcatRedactionUnitTest {

    @Test
    fun testBearerTokenRedaction() {
        val input = "Authorization: Bearer abc123def456ghijk.xyz999=="
        val redacted = LogcatHelper.redactSensitiveInfo(input)
        assertEquals("Authorization: Bearer [REDACTED]", redacted)
        assertFalse(redacted.contains("abc123def456ghijk"))
    }

    @Test
    fun testOpenAiApiKeyRedaction() {
        val input = "Error using key sk-proj-1234567890abcdef1234567890 in OpenAI client"
        val redacted = LogcatHelper.redactSensitiveInfo(input)
        assertTrue(redacted.contains("[REDACTED]"))
        assertFalse(redacted.contains("sk-proj-1234567890abcdef"))
    }

    @Test
    fun testGoogleApiKeyRedaction() {
        val input = "GET https://maps.googleapis.com/maps/api/geocode/json?key=AIzaSyD1234567890abcdef1234567890abcdef"
        val redacted = LogcatHelper.redactSensitiveInfo(input)
        assertTrue(redacted.contains("[REDACTED]"))
        assertFalse(redacted.contains("AIzaSyD1234567890abcdef1234567890abcdef"))
    }

    @Test
    fun testJwtTokenRedaction() {
        val jwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
        val input = "Received token: $jwt in response body"
        val redacted = LogcatHelper.redactSensitiveInfo(input)
        assertTrue(redacted.contains("[REDACTED]"))
        assertFalse(redacted.contains("SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"))
    }

    @Test
    fun testEmailAndParamRedaction() {
        val input = "User login with email test.user@example.com and api_key=superSecretKey12345"
        val redacted = LogcatHelper.redactSensitiveInfo(input)
        assertTrue(redacted.contains("[REDACTED]"))
        assertFalse(redacted.contains("test.user@example.com"))
        assertFalse(redacted.contains("superSecretKey12345"))
    }
}
