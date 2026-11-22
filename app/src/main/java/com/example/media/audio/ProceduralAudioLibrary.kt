package com.example.media.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import com.example.data.model.MusicMood
import com.example.data.model.SfxType
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "ProceduralAudio"
const val MIX_SAMPLE_RATE = 44100
const val MIX_CHANNELS = 2

/**
 * Generates royalty-free PCM/WAV cinematic background beds and sound effects locally,
 * and decodes user-uploaded audio files (MP3/M4A/WAV) via MediaExtractor + MediaCodec.
 */
object ProceduralAudioLibrary {

  suspend fun ensureBgmWav(context: Context, mood: MusicMood): File =
    withContext(Dispatchers.IO) {
      val dir = File(context.filesDir, "audio_assets").apply { mkdirs() }
      val file = File(dir, "bgm_${mood.name.lowercase()}.wav")
      if (file.exists() && file.length() > 1024L) return@withContext file

      val durationSec = 12
      val totalFrames = MIX_SAMPLE_RATE * durationSec
      val samples = ShortArray(totalFrames * MIX_CHANNELS)

      when (mood) {
        MusicMood.CINEMATIC_EPIC, MusicMood.CUSTOM_USER_AUDIO ->
          synthesizePadBed(
            samples = samples,
            totalFrames = totalFrames,
            rootHz = 55.0, // A1 sub bass
            chordIntervals = doubleArrayOf(1.0, 1.5, 2.0, 2.378, 3.0),
            pulseBpm = 84.0,
          )
        MusicMood.SUSPENSE_PULSE ->
          synthesizePadBed(
            samples = samples,
            totalFrames = totalFrames,
            rootHz = 49.0, // G1 dark drone
            chordIntervals = doubleArrayOf(1.0, 1.414, 1.887, 2.245),
            pulseBpm = 108.0,
          )
        MusicMood.EMOTIONAL_STRINGS ->
          synthesizePadBed(
            samples = samples,
            totalFrames = totalFrames,
            rootHz = 65.41, // C2 warm major9 pad
            chordIntervals = doubleArrayOf(1.0, 1.5, 2.0, 2.52, 3.0, 3.78),
            pulseBpm = 62.0,
          )
        MusicMood.DOC_AMBIENCE ->
          synthesizePadBed(
            samples = samples,
            totalFrames = totalFrames,
            rootHz = 73.42, // D2 clean ambient
            chordIntervals = doubleArrayOf(1.0, 1.5, 2.25, 3.0),
            pulseBpm = 70.0,
          )
      }

      writeWavFile(file, samples, MIX_SAMPLE_RATE, MIX_CHANNELS)
      file
    }

  suspend fun ensureSfxWav(context: Context, sfx: SfxType): File? =
    withContext(Dispatchers.IO) {
      if (sfx == SfxType.NONE) return@withContext null
      val dir = File(context.filesDir, "audio_assets").apply { mkdirs() }
      val file = File(dir, "sfx_${sfx.name.lowercase()}.wav")
      if (file.exists() && file.length() > 512L) return@withContext file

      val durationMs =
        when (sfx) {
          SfxType.DEEP_IMPACT -> 1400
          SfxType.CINEMATIC_WHOOSH -> 900
          SfxType.RISER_TENSION -> 1800
          SfxType.TECH_GLITCH -> 650
          SfxType.NONE -> 0
        }
      val totalFrames = (MIX_SAMPLE_RATE * durationMs) / 1000
      val samples = ShortArray(totalFrames * MIX_CHANNELS)
      val rng = Random(sfx.ordinal * 91)

      for (i in 0 until totalFrames) {
        val t = i.toDouble() / MIX_SAMPLE_RATE
        val progress = i.toDouble() / totalFrames.coerceAtLeast(1)
        val mono =
          when (sfx) {
            SfxType.DEEP_IMPACT -> {
              val freq = 95.0 * exp(-t * 4.2) + 32.0
              val thump = sin(2.0 * PI * freq * t) * exp(-t * 2.8)
              val crack = (rng.nextDouble() * 2.0 - 1.0) * exp(-t * 18.0) * 0.45
              (thump * 0.85 + crack) * 0.9
            }
            SfxType.CINEMATIC_WHOOSH -> {
              val env = sin(PI * progress).pow(1.6)
              val noise = rng.nextDouble() * 2.0 - 1.0
              val sweep = sin(2.0 * PI * (120.0 + 520.0 * env) * t) * 0.3
              (noise * 0.45 + sweep) * env * 0.85
            }
            SfxType.RISER_TENSION -> {
              val freq = 90.0 * (1.0 + progress * 3.8)
              val env = progress.pow(1.4) * (if (progress > 0.94) (1.0 - progress) / 0.06 else 1.0)
              (sin(2.0 * PI * freq * t) * 0.6 + sin(2.0 * PI * freq * 1.5 * t) * 0.3) * env
            }
            SfxType.TECH_GLITCH -> {
              val step = (progress * 12).toInt()
              val freq = 320.0 + (step % 4) * 240.0
              val env = exp(-t * 4.0)
              (sin(2.0 * PI * freq * t) * 0.55 + (rng.nextDouble() * 2.0 - 1.0) * 0.25) * env
            }
            SfxType.NONE -> 0.0
          }
        val clamped = (mono * 32767.0).toInt().coerceIn(-32767, 32767).toShort()
        samples[i * 2] = clamped
        samples[i * 2 + 1] = clamped
      }

      writeWavFile(file, samples, MIX_SAMPLE_RATE, MIX_CHANNELS)
      file
    }

  private fun synthesizePadBed(
    samples: ShortArray,
    totalFrames: Int,
    rootHz: Double,
    chordIntervals: DoubleArray,
    pulseBpm: Double,
  ) {
    val beatFreq = pulseBpm / 60.0
    val durationSec = totalFrames.toDouble() / MIX_SAMPLE_RATE
    for (i in 0 until totalFrames) {
      val t = i.toDouble() / MIX_SAMPLE_RATE
      // Crossfade loop ends smoothly
      val loopEnv =
        when {
          t < 0.25 -> t / 0.25
          t > durationSec - 0.25 -> (durationSec - t) / 0.25
          else -> 1.0
        }.coerceIn(0.0, 1.0)

      val pulse = 0.65 + 0.35 * sin(2.0 * PI * beatFreq * t)
      var left = 0.0
      var right = 0.0
      for (idx in chordIntervals.indices) {
        val freq = rootHz * chordIntervals[idx]
        val detune = 1.0 + (idx - chordIntervals.size / 2.0) * 0.0022
        val oscL = sin(2.0 * PI * freq * t)
        val oscR = sin(2.0 * PI * freq * detune * t + 0.35)
        val weight = 1.0 / (1.0 + idx * 0.45)
        left += oscL * weight
        right += oscR * weight
      }
      val norm = 0.28 * pulse * loopEnv
      samples[i * 2] = (left * norm * 32767.0).toInt().coerceIn(-32767, 32767).toShort()
      samples[i * 2 + 1] = (right * norm * 32767.0).toInt().coerceIn(-32767, 32767).toShort()
    }
  }

  fun writeWavFile(file: File, pcmSamples: ShortArray, sampleRate: Int, channels: Int) {
    val byteDataLen = pcmSamples.size * 2
    val byteRate = sampleRate * channels * 2
    val header =
      ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
        put("RIFF".toByteArray())
        putInt(36 + byteDataLen)
        put("WAVE".toByteArray())
        put("fmt ".toByteArray())
        putInt(16) // PCM subchunk size
        putShort(1) // PCM format
        putShort(channels.toShort())
        putInt(sampleRate)
        putInt(byteRate)
        putShort((channels * 2).toShort())
        putShort(16)
        put("data".toByteArray())
        putInt(byteDataLen)
      }

    FileOutputStream(file).use { fos ->
      fos.write(header.array())
      val pcmBytes = ByteBuffer.allocate(byteDataLen).order(ByteOrder.LITTLE_ENDIAN)
      for (sample in pcmSamples) {
        pcmBytes.putShort(sample)
      }
      fos.write(pcmBytes.array())
    }
  }

  /**
   * Reads a WAV or compressed audio file (MP3/M4A/AAC/OGG) and returns stereo 44.1kHz ShortArray PCM.
   */
  suspend fun decodeAudioFileToStereo44k(context: Context, pathOrUri: String): ShortArray? =
    withContext(Dispatchers.IO) {
      val file = File(pathOrUri)
      if (file.exists() && pathOrUri.endsWith(".wav", ignoreCase = true)) {
        return@withContext readWavAsStereo44k(file)
      }
      decodeCompressedAudio(context, pathOrUri)
    }

  private fun readWavAsStereo44k(file: File): ShortArray? {
    return runCatching {
      val bytes = file.readBytes()
      if (bytes.size <= 44) return null
      val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
      val channels = bb.getShort(22).toInt().coerceAtLeast(1)
      val sampleRate = bb.getInt(24).coerceAtLeast(8000)

      // Locate 'data' chunk safely
      var offset = 12
      var dataStart = 44
      var dataLen = bytes.size - 44
      while (offset + 8 <= bytes.size) {
        val id = String(bytes, offset, 4)
        val chunkSize = bb.getInt(offset + 4)
        if (id == "data") {
          dataStart = offset + 8
          dataLen = chunkSize.coerceAtMost(bytes.size - dataStart)
          break
        }
        offset += 8 + chunkSize
      }

      val frameCount = dataLen / (2 * channels)
      if (frameCount <= 0) return null

      val inLeft = ShortArray(frameCount)
      val inRight = ShortArray(frameCount)
      bb.position(dataStart)
      for (i in 0 until frameCount) {
        val l = bb.getShort()
        val r = if (channels >= 2) bb.getShort() else l
        inLeft[i] = l
        inRight[i] = r
        for (extra in 2 until channels) {
          bb.getShort()
        }
      }

      resampleStereo(inLeft, inRight, sampleRate, MIX_SAMPLE_RATE)
    }.onFailure { Log.e(TAG, "Failed to read WAV ${file.name}", it) }.getOrNull()
  }

  private fun decodeCompressedAudio(context: Context, pathOrUri: String): ShortArray? {
    val extractor = MediaExtractor()
    var codec: MediaCodec? = null
    return try {
      if (pathOrUri.startsWith("content://")) {
        extractor.setDataSource(context, Uri.parse(pathOrUri), null)
      } else {
        extractor.setDataSource(pathOrUri)
      }
      var audioTrackIdx = -1
      var format: MediaFormat? = null
      for (i in 0 until extractor.trackCount) {
        val trackFormat = extractor.getTrackFormat(i)
        val mime = trackFormat.getString(MediaFormat.KEY_MIME) ?: ""
        if (mime.startsWith("audio/")) {
          audioTrackIdx = i
          format = trackFormat
          break
        }
      }
      if (audioTrackIdx < 0 || format == null) return null

      extractor.selectTrack(audioTrackIdx)
      val mime = format.getString(MediaFormat.KEY_MIME)!!
      var sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
      var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

      codec = MediaCodec.createDecoderByType(mime)
      codec.configure(format, null, null, 0)
      codec.start()

      val decodedShorts = ArrayList<Short>(44100 * 10)
      val info = MediaCodec.BufferInfo()
      var inputDone = false
      var outputDone = false
      val maxSamples = MIX_SAMPLE_RATE * 2 * 45 // Cap BGM decode to 45s loop to protect memory

      while (!outputDone && decodedShorts.size < maxSamples) {
        if (!inputDone) {
          val inIdx = codec.dequeueInputBuffer(10_000)
          if (inIdx >= 0) {
            val inBuf = codec.getInputBuffer(inIdx)!!
            val sampleSize = extractor.readSampleData(inBuf, 0)
            if (sampleSize < 0) {
              codec.queueInputBuffer(inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
              inputDone = true
            } else {
              codec.queueInputBuffer(inIdx, 0, sampleSize, extractor.sampleTime, 0)
              extractor.advance()
            }
          }
        }
        val outIdx = codec.dequeueOutputBuffer(info, 10_000)
        if (outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
          val outFmt = codec.outputFormat
          if (outFmt.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
            sampleRate = outFmt.getInteger(MediaFormat.KEY_SAMPLE_RATE)
          }
          if (outFmt.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
            channels = outFmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
          }
        } else if (outIdx >= 0) {
          val outBuf = codec.getOutputBuffer(outIdx)
          if (outBuf != null && info.size > 0) {
            outBuf.position(info.offset)
            outBuf.limit(info.offset + info.size)
            val shortBuf = outBuf.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
            while (shortBuf.hasRemaining() && decodedShorts.size < maxSamples) {
              decodedShorts.add(shortBuf.get())
            }
          }
          codec.releaseOutputBuffer(outIdx, false)
          if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            outputDone = true
          }
        }
      }

      val frameCount = decodedShorts.size / channels.coerceAtLeast(1)
      if (frameCount <= 0) return null
      val inLeft = ShortArray(frameCount)
      val inRight = ShortArray(frameCount)
      for (i in 0 until frameCount) {
        val base = i * channels
        inLeft[i] = decodedShorts[base]
        inRight[i] = if (channels >= 2) decodedShorts[base + 1] else inLeft[i]
      }
      resampleStereo(inLeft, inRight, sampleRate, MIX_SAMPLE_RATE)
    } catch (e: Exception) {
      Log.e(TAG, "Failed to decode custom audio: $pathOrUri", e)
      null
    } finally {
      runCatching { codec?.stop(); codec?.release() }
      runCatching { extractor.release() }
    }
  }

  private fun resampleStereo(
    left: ShortArray,
    right: ShortArray,
    fromRate: Int,
    toRate: Int,
  ): ShortArray {
    if (fromRate == toRate) {
      val out = ShortArray(left.size * 2)
      for (i in left.indices) {
        out[i * 2] = left[i]
        out[i * 2 + 1] = right[i]
      }
      return out
    }
    val ratio = fromRate.toDouble() / toRate
    val outFrames = ((left.size.toDouble() / ratio).toInt()).coerceAtLeast(1)
    val out = ShortArray(outFrames * 2)
    for (i in 0 until outFrames) {
      val srcPos = i * ratio
      val idx = srcPos.toInt().coerceIn(0, left.lastIndex)
      val next = (idx + 1).coerceIn(0, left.lastIndex)
      val frac = srcPos - idx
      val l = left[idx] * (1.0 - frac) + left[next] * frac
      val r = right[idx] * (1.0 - frac) + right[next] * frac
      out[i * 2] = l.toInt().coerceIn(-32767, 32767).toShort()
      out[i * 2 + 1] = r.toInt().coerceIn(-32767, 32767).toShort()
    }
    return out
  }
}
