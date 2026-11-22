package com.example.media.render

import android.content.Context
import com.example.data.model.DocumentaryProject
import com.example.data.model.MusicMood
import com.example.data.model.SfxType
import com.example.media.audio.MIX_CHANNELS
import com.example.media.audio.MIX_SAMPLE_RATE
import com.example.media.audio.ProceduralAudioLibrary
import java.io.File
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Mixes multi-track audio:
 * - Track 1: Per-scene Voiceover WAVs
 * - Track 2: Looped BGM (synthesized mood or user-uploaded custom audio) with smooth Auto-Ducking
 * - Track 3: Per-scene Sound Effects (SFX) aligned to scene starts
 */
object AudioMixEngine {

  suspend fun renderMixedStereoPcm(
    context: Context,
    project: DocumentaryProject,
  ): ShortArray =
    withContext(Dispatchers.IO) {
      val totalDurationMs = project.totalDurationMs.coerceAtLeast(3000L)
      val totalFrames = ((totalDurationMs * MIX_SAMPLE_RATE) / 1000L).toInt().coerceAtLeast(1)
      val mixFloat = FloatArray(totalFrames * MIX_CHANNELS)
      val speechActive = BooleanArray(totalFrames)

      // 1. Place voiceover tracks + record speech envelope for auto-ducking
      var cursorMs = 0L
      for (scene in project.scenes) {
        val sceneFramesStart = ((cursorMs * MIX_SAMPLE_RATE) / 1000L).toInt()
        val voPath = scene.voiceoverPath
        if (!voPath.isNullOrBlank() && File(voPath).exists()) {
          val voPcm = ProceduralAudioLibrary.decodeAudioFileToStereo44k(context, voPath)
          if (voPcm != null) {
            val voFrames = voPcm.size / MIX_CHANNELS
            for (f in 0 until voFrames) {
              val destFrame = sceneFramesStart + f
              if (destFrame >= totalFrames) break
              val l = (voPcm[f * 2] / 32767f) * project.audioMix.voiceoverVolume
              val r = (voPcm[f * 2 + 1] / 32767f) * project.audioMix.voiceoverVolume
              mixFloat[destFrame * 2] += l
              mixFloat[destFrame * 2 + 1] += r
              if (abs(l) > 0.015f || abs(r) > 0.015f) {
                speechActive[destFrame] = true
              }
            }
          }
        }

        // 2. Place scene SFX trigger at scene transition boundary
        if (scene.sfxType != SfxType.NONE) {
          val sfxFile = ProceduralAudioLibrary.ensureSfxWav(context, scene.sfxType)
          if (sfxFile != null) {
            val sfxPcm = ProceduralAudioLibrary.decodeAudioFileToStereo44k(context, sfxFile.absolutePath)
            if (sfxPcm != null) {
              val sfxFrames = sfxPcm.size / MIX_CHANNELS
              for (f in 0 until sfxFrames) {
                val destFrame = sceneFramesStart + f
                if (destFrame >= totalFrames) break
                mixFloat[destFrame * 2] += (sfxPcm[f * 2] / 32767f) * project.audioMix.sfxVolume
                mixFloat[destFrame * 2 + 1] += (sfxPcm[f * 2 + 1] / 32767f) * project.audioMix.sfxVolume
              }
            }
          }
        }

        cursorMs += scene.effectiveDurationMs
      }

      // 3. Load & mix BGM with smooth envelope ducking
      val customPath = project.audioMix.customBgmPath
      val bgmPcm =
        if (project.audioMix.musicMood == MusicMood.CUSTOM_USER_AUDIO && !customPath.isNullOrBlank()) {
          ProceduralAudioLibrary.decodeAudioFileToStereo44k(context, customPath)
            ?: ProceduralAudioLibrary.decodeAudioFileToStereo44k(
              context,
              ProceduralAudioLibrary.ensureBgmWav(context, MusicMood.CINEMATIC_EPIC).absolutePath,
            )
        } else {
          val bgmFile = ProceduralAudioLibrary.ensureBgmWav(context, project.audioMix.musicMood)
          ProceduralAudioLibrary.decodeAudioFileToStereo44k(context, bgmFile.absolutePath)
        }

      if (bgmPcm != null && bgmPcm.size >= 2) {
        val bgmFrames = bgmPcm.size / MIX_CHANNELS
        val baseBgmVol = project.audioMix.bgmVolume
        var duckEnvelope = 1.0f
        val fadeOutStartFrame = (totalFrames - MIX_SAMPLE_RATE * 2).coerceAtLeast(0)

        for (f in 0 until totalFrames) {
          val targetDuck =
            if (project.audioMix.autoDucking && speechActive[f]) 0.22f else 1.0f
          // Smooth attack & release so ducking sounds broadcast-grade
          val coeff = if (targetDuck < duckEnvelope) 0.0025f else 0.0004f
          duckEnvelope += (targetDuck - duckEnvelope) * coeff

          val masterFade =
            when {
              f < MIX_SAMPLE_RATE / 2 -> f / (MIX_SAMPLE_RATE / 2f)
              f > fadeOutStartFrame ->
                ((totalFrames - f).toFloat() / (totalFrames - fadeOutStartFrame).coerceAtLeast(1))
              else -> 1.0f
            }.coerceIn(0f, 1f)

          val bgmIdx = (f % bgmFrames) * 2
          val gain = baseBgmVol * duckEnvelope * masterFade
          mixFloat[f * 2] += (bgmPcm[bgmIdx] / 32767f) * gain
          mixFloat[f * 2 + 1] += (bgmPcm[bgmIdx + 1] / 32767f) * gain
        }
      }

      // 4. Soft-clip master limiter to prevent digital clipping
      val outShorts = ShortArray(totalFrames * MIX_CHANNELS)
      for (i in mixFloat.indices) {
        val v = mixFloat[i]
        val limited = v / (1f + abs(v) * 0.18f)
        outShorts[i] = (limited * 32767f).toInt().coerceIn(-32767, 32767).toShort()
      }
      outShorts
    }

  suspend fun renderMixedPreviewWav(
    context: Context,
    project: DocumentaryProject,
    outputFile: File,
  ): File {
    val pcm = renderMixedStereoPcm(context, project)
    withContext(Dispatchers.IO) {
      outputFile.parentFile?.mkdirs()
      ProceduralAudioLibrary.writeWavFile(outputFile, pcm, MIX_SAMPLE_RATE, MIX_CHANNELS)
    }
    return outputFile
  }
}
