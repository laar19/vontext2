package dev.laar19.vontext.processor

data class FrameInfo(
    val frameNum: Int,
    val timestampMs: Long,
    val path: String,
    val ocrText: String = ""
)
