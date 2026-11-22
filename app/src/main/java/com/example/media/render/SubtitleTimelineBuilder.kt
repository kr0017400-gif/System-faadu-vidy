package com.example.media.render

import com.example.data.model.DocumentaryScene
import com.example.data.model.SubtitleSegment

object SubtitleTimelineBuilder {

  private val stopWords =
    setOf(
      "the", "and", "for", "with", "that", "this", "from", "into", "your", "have", "what",
      "when", "where", "will", "they", "been", "were", "more", "than", "then", "only", "about",
      "hai", "aur", "mein", "nahi", "kya", "aap", "ek", "ko", "se", "ka", "ki", "ke",
    )

  fun buildTimeline(scenes: List<DocumentaryScene>): List<SubtitleSegment> {
    val segments = mutableListOf<SubtitleSegment>()
    var cursorMs = 0L

    for (scene in scenes) {
      val sceneDuration = scene.effectiveDurationMs
      val words =
        scene.narration
          .trim()
          .split(Regex("\\s+"))
          .filter { it.isNotBlank() }

      if (words.isEmpty()) {
        cursorMs += sceneDuration
        continue
      }

      // Group into crisp 4-6 word caption chunks for comfortable reading in 9:16 and 16:9
      val chunkSize = 5
      val chunks = words.chunked(chunkSize)
      val speechWindow =
        if (scene.voiceoverDurationMs > 1200L) {
          scene.voiceoverDurationMs.coerceAtMost(sceneDuration)
        } else {
          (sceneDuration * 0.92f).toLong().coerceAtLeast(1500L)
        }

      val totalWords = words.size.coerceAtLeast(1)
      var chunkOffsetMs = 0L

      chunks.forEachIndexed { idx, chunkWords ->
        val weight = chunkWords.size.toFloat() / totalWords
        val chunkDur =
          if (idx == chunks.lastIndex) (speechWindow - chunkOffsetMs).coerceAtLeast(450L)
          else (speechWindow * weight).toLong().coerceAtLeast(500L)

        val highlight =
          chunkWords
            .map { it.trim('.', ',', '!', '?', ':', ';', '"', '\'', '—', '-') }
            .firstOrNull { w -> w.length >= 5 && w.lowercase() !in stopWords }
            ?: chunkWords.maxByOrNull { it.length }

        segments +=
          SubtitleSegment(
            text = chunkWords.joinToString(" "),
            startMs = cursorMs + chunkOffsetMs,
            endMs = (cursorMs + chunkOffsetMs + chunkDur).coerceAtMost(cursorMs + sceneDuration),
            highlightWord = highlight,
          )
        chunkOffsetMs += chunkDur
      }

      cursorMs += sceneDuration
    }

    return segments
  }
}
