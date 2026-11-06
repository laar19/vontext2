package com.example.processor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

object FrameOcrHelper {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    /**
     * Extracts real screen text using on-device ML Kit Text Recognition with the embedded model.
     * Runs in Dispatchers.Default, collapses consecutive whitespaces and caps output to ~600 chars.
     * Bitmaps are freed immediately to avoid OOM.
     */
    suspend fun extractScreenText(imagePath: String): String = withContext(Dispatchers.Default) {
        var bitmap: Bitmap? = null
        try {
            val file = File(imagePath)
            if (!file.exists() || file.length() == 0L) return@withContext ""

            bitmap = BitmapFactory.decodeFile(imagePath) ?: return@withContext ""
            val inputImage = InputImage.fromBitmap(bitmap, 0)

            val visionText = Tasks.await(recognizer.process(inputImage), 10, TimeUnit.SECONDS)
            val rawText = visionText.text ?: ""
            // Collapse whitespaces
            val clean = rawText.replace(Regex("\\s+"), " ").trim()
            if (clean.length > 600) {
                clean.take(600).trimEnd() + "..."
            } else {
                clean
            }
        } catch (e: Exception) {
            ""
        } finally {
            try {
                bitmap?.recycle()
            } catch (e: Exception) {
                // Ignore recycle failure
            }
        }
    }
}
