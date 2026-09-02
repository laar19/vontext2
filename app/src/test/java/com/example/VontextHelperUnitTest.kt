package com.example

import com.example.processor.FrameOcrHelper
import com.example.util.LogcatHelper
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VontextHelperUnitTest {

    @Test
    fun testLogcatCaptureFormat() {
        val output = LogcatHelper.captureRecentLogs(10)
        assertNotNull(output)
        assertTrue(output.contains("VONTEXT LOGCAT DUMP"))
    }

    @Test
    fun testOcrEmptyFileHandling() {
        val dummyPath = "/tmp/non_existent_file_${System.currentTimeMillis()}.png"
        val result = FrameOcrHelper.extractScreenTextSummary(dummyPath)
        assertTrue(result.isEmpty())
    }
}
