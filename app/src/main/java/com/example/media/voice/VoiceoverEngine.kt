package com.example.media.voice

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.data.model.NarrationLanguage
import com.example.data.model.VoiceOption
import com.example.data.model.VoiceSettings
import java.io.File
import java.io.RandomAccessFile
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

private const val TAG = "VoiceoverEngine"

data class VoiceSynthesisResult(
  val audioPath: String,
  val durationMs: Long,
)

class VoiceoverEngine(context: Context) {
  private val appContext = context.applicationContext
  private val ttsMutex = Mutex()
  private var tts: TextToSpeech? = null
  private var initDeferred: CompletableDeferred<Boolean>? = null
  private var mediaPlayer: MediaPlayer? = null

  suspend fun ensureInitialized(): Boolean =
    ttsMutex.withLock {
      if (tts != null && initDeferred?.isCompleted == true) {
        return initDeferred?.getCompleted() == true
      }
      val deferred = CompletableDeferred<Boolean>()
      initDeferred = deferred
      withContext(Dispatchers.Main) {
        try {
          tts =
            TextToSpeech(appContext) { status ->
              deferred.complete(status == TextToSpeech.SUCCESS)
            }
        } catch (e: Exception) {
          Log.e(TAG, "Failed to construct TextToSpeech", e)
          if (!deferred.isCompleted) deferred.complete(false)
        }
      }
      withTimeoutOrNull(8_000L) { deferred.await() } ?: false
    }

  suspend fun getAvailableVoices(language: NarrationLanguage): List<VoiceOption> {
    val ready = ensureInitialized()
    val defaultOption =
      VoiceOption(
        id = "default",
        displayName = "System Narrator (${language.label})",
        localeTag = language.localeTag,
        isSystemDefault = true,
      )
    if (!ready) return listOf(defaultOption)

    val engine = tts ?: return listOf(defaultOption)
    val voices =
      runCatching { engine.voices?.toList().orEmpty() }.getOrElse { emptyList() }
    val targetLangPrefix =
      when (language) {
        NarrationLanguage.HINDI -> "hi"
        NarrationLanguage.HINGLISH -> "en"
        NarrationLanguage.ENGLISH -> "en"
      }
    val matching =
      voices
        .filter { voice ->
          voice.locale.language.equals(targetLangPrefix, ignoreCase = true) &&
            !voice.isNetworkConnectionRequired
        }
        .ifEmpty {
          voices.filter { it.locale.language.equals(targetLangPrefix, ignoreCase = true) }
        }
        .take(8)
        .mapIndexed { idx, voice ->
          val region = voice.locale.country.takeIf { it.isNotBlank() } ?: "Global"
          val qualityTag = if (voice.quality >= 400) "HD" else "Std"
          VoiceOption(
            id = voice.name,
            displayName = "Voice ${idx + 1} ($region · $qualityTag)",
            localeTag = voice.locale.toLanguageTag(),
            isSystemDefault = false,
          )
        }
    return listOf(defaultOption) + matching
  }

  suspend fun synthesizeSceneVoiceover(
    text: String,
    language: NarrationLanguage,
    voiceSettings: VoiceSettings,
    outputFile: File,
  ): Result<VoiceSynthesisResult> {
    val cleanText = text.trim()
    if (cleanText.isBlank()) {
      return Result.failure(IllegalArgumentException("Scene narration is empty."))
    }
    val ready = ensureInitialized()
    if (!ready) {
      return Result.failure(
        IllegalStateException("Android TextToSpeech engine is unavailable on this device.")
      )
    }

    return ttsMutex.withLock {
      val engine =
        tts
          ?: return@withLock Result.failure(
            IllegalStateException("TextToSpeech instance was released.")
          )

      outputFile.parentFile?.mkdirs()
      if (outputFile.exists()) outputFile.delete()

      val locale =
        when (language) {
          NarrationLanguage.HINDI -> Locale.forLanguageTag("hi-IN")
          NarrationLanguage.HINGLISH -> Locale.forLanguageTag("en-IN")
          NarrationLanguage.ENGLISH -> Locale.US
        }

      val langStatus = engine.setLanguage(locale)
      if (langStatus == TextToSpeech.LANG_MISSING_DATA || langStatus == TextToSpeech.LANG_NOT_SUPPORTED) {
        // Fallback to US English if device lacks Hindi pack, still fail transparently if totally unsupported
        val fallbackStatus = engine.setLanguage(Locale.US)
        if (fallbackStatus == TextToSpeech.LANG_MISSING_DATA || fallbackStatus == TextToSpeech.LANG_NOT_SUPPORTED) {
          return@withLock Result.failure(
            IllegalStateException("Voice pack for ${language.label} is not installed on this device.")
          )
        }
      }

      if (voiceSettings.voiceId != "default") {
        val targetVoice =
          runCatching { engine.voices?.firstOrNull { it.name == voiceSettings.voiceId } }
            .getOrNull()
        if (targetVoice != null) {
          engine.voice = targetVoice
        }
      }

      engine.setSpeechRate(voiceSettings.speed.coerceIn(0.65f, 1.6f))
      engine.setPitch(voiceSettings.pitch.coerceIn(0.65f, 1.5f))

      val utteranceId = "va_${UUID.randomUUID()}"
      val synthOk =
        withTimeoutOrNull(35_000L) {
          suspendCancellableCoroutine<Boolean> { cont ->
            engine.setOnUtteranceProgressListener(
              object : UtteranceProgressListener() {
                override fun onStart(id: String?) {}

                override fun onDone(id: String?) {
                  if (id == utteranceId && cont.isActive) cont.resume(true)
                }

                @Deprecated("Deprecated in Java")
                override fun onError(id: String?) {
                  if (id == utteranceId && cont.isActive) cont.resume(false)
                }

                override fun onError(id: String?, errorCode: Int) {
                  Log.e(TAG, "TTS synthesizeToFile failed code=$errorCode")
                  if (id == utteranceId && cont.isActive) cont.resume(false)
                }
              }
            )
            val params = Bundle()
            val queued = engine.synthesizeToFile(cleanText, params, outputFile, utteranceId)
            if (queued != TextToSpeech.SUCCESS && cont.isActive) {
              cont.resume(false)
            }
          }
        } ?: false

      if (!synthOk || !outputFile.exists() || outputFile.length() <= 44L) {
        return@withLock Result.failure(
          IllegalStateException(
            "TTS failed to synthesize audio file. Ensure a system TTS voice is active in Android Settings."
          )
        )
      }

      val durationMs = readWavDurationMs(outputFile).coerceAtLeast(1500L)
      Result.success(VoiceSynthesisResult(outputFile.absolutePath, durationMs))
    }
  }

  fun playFile(path: String, onCompletion: () -> Unit = {}) {
    stopPlayback()
    try {
      val player =
        MediaPlayer().apply {
          setDataSource(path)
          prepare()
          setOnCompletionListener {
            onCompletion()
          }
          start()
        }
      mediaPlayer = player
    } catch (e: Exception) {
      Log.e(TAG, "Failed to play voiceover file: $path", e)
      onCompletion()
    }
  }

  fun stopPlayback() {
    runCatching {
      mediaPlayer?.stop()
      mediaPlayer?.release()
    }
    mediaPlayer = null
  }

  fun release() {
    stopPlayback()
    runCatching { tts?.shutdown() }
    tts = null
  }

  companion object {
    fun readWavDurationMs(file: File): Long {
      if (!file.exists() || file.length() < 44L) return 0L
      return runCatching {
        RandomAccessFile(file, "r").use { raf ->
          val header = ByteArray(44)
          raf.readFully(header)
          val byteRate =
            (header[28].toInt() and 0xFF) or
              ((header[29].toInt() and 0xFF) shl 8) or
              ((header[30].toInt() and 0xFF) shl 16) or
              ((header[31].toInt() and 0xFF) shl 24)
          val dataSize = (file.length() - 44L).coerceAtLeast(0L)
          if (byteRate > 0) (dataSize * 1000L) / byteRate else 0L
        }
      }.getOrDefault(0L)
    }
  }
}
