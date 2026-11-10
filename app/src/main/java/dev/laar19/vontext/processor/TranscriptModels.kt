package dev.laar19.vontext.processor

data class TranscriptWord(
    val start: Float,
    val end: Float,
    val text: String
)

data class Transcript(
    val words: List<TranscriptWord>,
    val fullText: String
)

/**
 * Pure function to assign words to frames without any Android framework dependencies.
 * Rule:
 * - Frame i owns the interval [t_i, t_{i+1}).
 * - Frame 0 also receives all words strictly prior to t_0 (word.start < t_0).
 * - The last frame receives all words from t_last until the end (word.start >= t_last).
 * - Each word belongs to the frame whose interval contains word.start.
 * - A word NEVER repeats in multiple frames.
 * - Words for each frame are joined by space in chronological order.
 * - Frames without words get an empty string "" (never repeat previous or fill nearest).
 */
fun assignWordsToFrames(words: List<TranscriptWord>, frameTimesSec: List<Float>): List<String> {
    if (frameTimesSec.isEmpty()) return emptyList()

    val sortedWords = words.sortedBy { it.start }
    val n = frameTimesSec.size
    val buckets = Array(n) { mutableListOf<String>() }

    for (w in sortedWords) {
        val t = w.start
        val targetIndex = when {
            t < frameTimesSec[0] -> 0
            t >= frameTimesSec[n - 1] -> n - 1
            else -> {
                var assigned = -1
                for (i in 0 until n - 1) {
                    if (t >= frameTimesSec[i] && t < frameTimesSec[i + 1]) {
                        assigned = i
                        break
                    }
                }
                if (assigned != -1) assigned else n - 1
            }
        }
        if (w.text.isNotBlank()) {
            buckets[targetIndex].add(w.text.trim())
        }
    }

    return buckets.map { it.joinToString(" ") }
}
