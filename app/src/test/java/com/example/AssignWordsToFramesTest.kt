package com.example

import com.example.processor.TranscriptWord
import com.example.processor.assignWordsToFrames
import org.junit.Assert.assertEquals
import org.junit.Test

class AssignWordsToFramesTest {

    @Test
    fun testRequiredSpecification() {
        // frames = [1f,2f,3f,...,10f]
        val frames = listOf(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f, 10f)
        val words = listOf(
            TranscriptWord(6.1f, 6.4f, "Call"),
            TranscriptWord(6.5f, 7.2f, "Communication"),
            TranscriptWord(7.6f, 8.3f, "Entertainment"),
            TranscriptWord(9.1f, 9.4f, "Tools"),
            TranscriptWord(0.3f, 0.6f, "Hola")
        )

        val result = assignWordsToFrames(words, frames)
        assertEquals(10, result.size)

        // Esperado: frame idx0 (1s) = "Hola"; idx5 (6s) = "Call Communication";
        // idx6 (7s) = "Entertainment"; idx8 (9s) = "Tools"; el resto = "".
        assertEquals("Hola", result[0])
        assertEquals("", result[1])
        assertEquals("", result[2])
        assertEquals("", result[3])
        assertEquals("", result[4])
        assertEquals("Call Communication", result[5])
        assertEquals("Entertainment", result[6])
        assertEquals("", result[7])
        assertEquals("Tools", result[8])
        assertEquals("", result[9])
    }

    @Test
    fun testEmptyWords() {
        val frames = listOf(1f, 2f, 3f)
        val result = assignWordsToFrames(emptyList(), frames)
        assertEquals(listOf("", "", ""), result)
    }

    @Test
    fun testWordAfterLastFrame() {
        val frames = listOf(1f, 5f, 10f)
        val words = listOf(
            TranscriptWord(12.5f, 13.0f, "Final")
        )
        val result = assignWordsToFrames(words, frames)
        assertEquals("", result[0])
        assertEquals("", result[1])
        assertEquals("Final", result[2])
    }

    @Test
    fun testWordExactlyAtTimestampBoundary() {
        val frames = listOf(1f, 5f, 10f)
        // palabra exactamente en t_i (va al frame i, no al i-1)
        val words = listOf(
            TranscriptWord(5.0f, 5.5f, "Exacto")
        )
        val result = assignWordsToFrames(words, frames)
        assertEquals("", result[0])
        assertEquals("Exacto", result[1])
        assertEquals("", result[2])
    }
}
