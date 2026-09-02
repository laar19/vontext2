package com.example.processor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import java.io.File

object FrameOcrHelper {

    /**
     * Performs lightweight visual text extraction / region analysis on a frame bitmap.
     * Detects high-contrast text regions, status bars, action dialogs, or error banners.
     */
    fun extractScreenTextSummary(imagePath: String): String {
        return try {
            val file = File(imagePath)
            if (!file.exists() || file.length() == 0L) return ""

            // 1. Measure dimensions without allocating memory
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(imagePath, boundsOptions)
            val origW = boundsOptions.outWidth
            val origH = boundsOptions.outHeight
            if (origW <= 0 || origH <= 0) return ""

            // Calculate sample size to limit analysis size to ~360px
            val maxDim = Math.max(origW, origH)
            val sample = if (maxDim > 360) maxDim / 360 else 1

            val options = BitmapFactory.Options().apply {
                inSampleSize = sample.coerceAtLeast(1)
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inMutable = false
            }
            val bitmap = BitmapFactory.decodeFile(imagePath, options) ?: return ""

            val width = bitmap.width
            val height = bitmap.height
            if (width <= 0 || height <= 0) {
                bitmap.recycle()
                return ""
            }

            // Analyze screen regions: Top (Appbar/Status), Center (Content/Dialog), Bottom (Nav/Action)
            val topBannerDetected = hasHighContrastBanner(bitmap, 0, (height * 0.12f).toInt(), width)
            val centerDialogDetected = hasHighContrastBanner(bitmap, (height * 0.35f).toInt(), (height * 0.65f).toInt(), width)
            val bottomBarDetected = hasHighContrastBanner(bitmap, (height * 0.88f).toInt(), height, width)

            val tags = mutableListOf<String>()
            if (topBannerDetected) tags.add("Encabezado/Barra superior activa")
            if (centerDialogDetected) tags.add("Ventana emergente/Diálogo modal o formulario centrado")
            if (bottomBarDetected) tags.add("Navegación inferior/Barra de acciones presente")

            bitmap.recycle()

            if (tags.isNotEmpty()) {
                tags.joinToString(" • ")
            } else {
                "Pantalla de contenido interactivo"
            }
        } catch (e: Exception) {
            ""
        }
    }

    private fun hasHighContrastBanner(bitmap: Bitmap, startY: Int, endY: Int, width: Int): Boolean {
        var edgeTransitions = 0
        val sampleStep = 8
        val safeStartY = Math.max(0, startY)
        val safeEndY = Math.min(bitmap.height, endY)

        for (y in safeStartY until safeEndY step sampleStep * 2) {
            var prevBrightness = -1
            for (x in 0 until width step sampleStep) {
                val pixel = bitmap.getPixel(x, y)
                val brightness = (Color.red(pixel) * 299 + Color.green(pixel) * 587 + Color.blue(pixel) * 114) / 1000
                if (prevBrightness != -1) {
                    if (Math.abs(brightness - prevBrightness) > 75) {
                        edgeTransitions++
                    }
                }
                prevBrightness = brightness
            }
        }
        return edgeTransitions > 20
    }
}
