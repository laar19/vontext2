package com.example

import com.example.processor.FrameOcrHelper
import com.example.util.LogcatHelper
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VontextHelperUnitTest {

    @Test
    fun testLogcatCaptureHeader() {
        val output = LogcatHelper.captureRecentLogs(maxLines = 10)
        assertNotNull(output)
        assertTrue(output.contains("VONTEXT LOGCAT DUMP"))
    }

    @Test
    fun testOcrEmptyFileHandling() = runBlocking {
        val dummyPath = "/tmp/non_existent_file_${System.currentTimeMillis()}.png"
        val result = FrameOcrHelper.extractScreenText(dummyPath)
        assertTrue(result.isEmpty())
    }
}
