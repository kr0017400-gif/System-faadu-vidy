package com.example.media.render

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLExt
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.opengl.GLUtils
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.Surface
import com.example.data.model.DocumentaryProject
import com.example.data.model.ExportResolution
import com.example.media.audio.MIX_CHANNELS
import com.example.media.audio.MIX_SAMPLE_RATE
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

private const val TAG = "Mp4VideoExporter"

data class ExportProgress(
  val stage: String,
  val progress: Float,
)

private data class QueuedSample(
  val isVideo: Boolean,
  val data: ByteArray,
  val ptsUs: Long,
  val flags: Int,
)

object Mp4VideoExporter {

  suspend fun exportProjectToMp4(
    context: Context,
    project: DocumentaryProject,
    outputFile: File,
    onProgress: (ExportProgress) -> Unit,
  ): Result<File> =
    withContext(Dispatchers.Default) {
      var videoCodec: MediaCodec? = null
      var audioCodec: MediaCodec? = null
      var muxer: MediaMuxer? = null
      var eglSurfaceHelper: EglBitmapInputSurface? = null
      var frameBitmap: Bitmap? = null
      val renderer = CinematicFrameRenderer(context)

      try {
        if (project.scenes.isEmpty()) {
          return@withContext Result.failure(
            IllegalStateException("Add at least one scene before exporting.")
          )
        }
        outputFile.parentFile?.mkdirs()
        if (outputFile.exists()) outputFile.delete()

        val (width, height) =
          when (project.exportedResolution) {
            ExportResolution.FHD_1080P ->
              project.aspectRatio.width1080 to project.aspectRatio.height1080
            ExportResolution.HD_720P ->
              project.aspectRatio.width720 to project.aspectRatio.height720
          }
        val fps = 24
        val bitrate =
          if (project.exportedResolution == ExportResolution.FHD_1080P) 6_000_000 else 3_200_000

        // 1. Mix synchronized audio track (Voiceover + ducked BGM + SFX)
        onProgress(ExportProgress("Mixing voiceover, ducked BGM & SFX...", 0.08f))
        val mixedPcm = AudioMixEngine.renderMixedStereoPcm(context, project)
        val subtitles = SubtitleTimelineBuilder.buildTimeline(project.scenes)
        val totalDurationMs = project.totalDurationMs.coerceAtLeast(3000L)
        val totalVideoFrames = ((totalDurationMs * fps) / 1000L).toInt().coerceAtLeast(24)

        // 2. Configure H.264 Video Encoder with EGL Surface for exact deterministic PTS
        val videoFormat =
          MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
            setInteger(
              MediaFormat.KEY_COLOR_FORMAT,
              MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface,
            )
            setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2)
          }
        videoCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        videoCodec.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        val inputSurface = videoCodec.createInputSurface()
        videoCodec.start()

        eglSurfaceHelper = EglBitmapInputSurface(inputSurface, width, height)
        eglSurfaceHelper.makeCurrent()
        frameBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val frameCanvas = Canvas(frameBitmap)

        // 3. Configure AAC Audio Encoder
        val audioFormat =
          MediaFormat.createAudioFormat(
              MediaFormat.MIMETYPE_AUDIO_AAC,
              MIX_SAMPLE_RATE,
              MIX_CHANNELS,
            )
            .apply {
              setInteger(
                MediaFormat.KEY_AAC_PROFILE,
                MediaCodecInfo.CodecProfileLevel.AACObjectLC,
              )
              setInteger(MediaFormat.KEY_BIT_RATE, 128_000)
              setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
            }
        audioCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        audioCodec.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        audioCodec.start()

        muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

        var videoTrackIndex = -1
        var audioTrackIndex = -1
        var muxerStarted = false
        val pendingSamples = mutableListOf<QueuedSample>()

        val vInfo = MediaCodec.BufferInfo()
        val aInfo = MediaCodec.BufferInfo()

        var frameIndex = 0
        var videoEosSent = false
        var videoEosDone = false

        var pcmSampleCursor = 0
        var audioEosSent = false
        var audioEosDone = false

        fun tryStartMuxer() {
          if (!muxerStarted && videoTrackIndex >= 0 && audioTrackIndex >= 0) {
            muxer.start()
            muxerStarted = true
            val info = MediaCodec.BufferInfo()
            for (s in pendingSamples) {
              info.set(0, s.data.size, s.ptsUs, s.flags)
              val track = if (s.isVideo) videoTrackIndex else audioTrackIndex
              muxer.writeSampleData(track, ByteBuffer.wrap(s.data), info)
            }
            pendingSamples.clear()
          }
        }

        fun drainVideo() {
          while (true) {
            val outIdx = videoCodec.dequeueOutputBuffer(vInfo, 2_000)
            if (outIdx == MediaCodec.INFO_TRY_AGAIN_LATER) {
              break
            } else if (outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
              if (videoTrackIndex < 0) {
                videoTrackIndex = muxer.addTrack(videoCodec.outputFormat)
                tryStartMuxer()
              }
            } else if (outIdx >= 0) {
              val encoded = videoCodec.getOutputBuffer(outIdx)
              if (
                encoded != null &&
                  vInfo.size > 0 &&
                  (vInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0
              ) {
                encoded.position(vInfo.offset)
                encoded.limit(vInfo.offset + vInfo.size)
                if (muxerStarted) {
                  muxer.writeSampleData(videoTrackIndex, encoded, vInfo)
                } else {
                  val copy = ByteArray(vInfo.size)
                  encoded.get(copy)
                  pendingSamples += QueuedSample(true, copy, vInfo.presentationTimeUs, vInfo.flags)
                }
              }
              videoCodec.releaseOutputBuffer(outIdx, false)
              if ((vInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                videoEosDone = true
                break
              }
            }
          }
        }

        fun drainAudio() {
          while (true) {
            val outIdx = audioCodec.dequeueOutputBuffer(aInfo, 2_000)
            if (outIdx == MediaCodec.INFO_TRY_AGAIN_LATER) {
              break
            } else if (outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
              if (audioTrackIndex < 0) {
                audioTrackIndex = muxer.addTrack(audioCodec.outputFormat)
                tryStartMuxer()
              }
            } else if (outIdx >= 0) {
              val encoded = audioCodec.getOutputBuffer(outIdx)
              if (
                encoded != null &&
                  aInfo.size > 0 &&
                  (aInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0
              ) {
                encoded.position(aInfo.offset)
                encoded.limit(aInfo.offset + aInfo.size)
                if (muxerStarted) {
                  muxer.writeSampleData(audioTrackIndex, encoded, aInfo)
                } else {
                  val copy = ByteArray(aInfo.size)
                  encoded.get(copy)
                  pendingSamples += QueuedSample(false, copy, aInfo.presentationTimeUs, aInfo.flags)
                }
              }
              audioCodec.releaseOutputBuffer(outIdx, false)
              if ((aInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                audioEosDone = true
                break
              }
            }
          }
        }

        // 4. Encode video frames & AAC audio concurrently
        while (!videoEosDone || !audioEosDone) {
          coroutineContext.ensureActive()

          if (!audioEosSent) {
            val inIdx = audioCodec.dequeueInputBuffer(2_000)
            if (inIdx >= 0) {
              val inBuf = audioCodec.getInputBuffer(inIdx)!!
              inBuf.clear()
              val maxSamples = minOf(inBuf.capacity() / 2, mixedPcm.size - pcmSampleCursor)
              val frameIdx = pcmSampleCursor / MIX_CHANNELS
              val ptsUs = (frameIdx.toLong() * 1_000_000L) / MIX_SAMPLE_RATE
              if (maxSamples > 0) {
                val byteBuf = ByteBuffer.allocate(maxSamples * 2).order(ByteOrder.LITTLE_ENDIAN)
                for (s in 0 until maxSamples) {
                  byteBuf.putShort(mixedPcm[pcmSampleCursor + s])
                }
                inBuf.put(byteBuf.array())
                audioCodec.queueInputBuffer(inIdx, 0, maxSamples * 2, ptsUs, 0)
                pcmSampleCursor += maxSamples
              } else {
                audioCodec.queueInputBuffer(
                  inIdx,
                  0,
                  0,
                  ptsUs,
                  MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                )
                audioEosSent = true
              }
            }
          }

          if (!videoEosSent) {
            if (frameIndex < totalVideoFrames) {
              val timeMs = (frameIndex.toLong() * 1000L) / fps
              renderer.drawFrame(frameCanvas, width, height, project, subtitles, timeMs)
              val ptsNs = (frameIndex.toLong() * 1_000_000_000L) / fps
              eglSurfaceHelper.drawBitmapAndSwap(frameBitmap, ptsNs)
              frameIndex++
              val renderFraction = 0.12f + 0.85f * (frameIndex.toFloat() / totalVideoFrames)
              if (frameIndex % 6 == 0 || frameIndex == totalVideoFrames) {
                onProgress(
                  ExportProgress(
                    "Rendering ${project.exportedResolution.label} · Frame $frameIndex/$totalVideoFrames",
                    renderFraction.coerceIn(0.12f, 0.97f),
                  )
                )
              }
            } else {
              videoCodec.signalEndOfInputStream()
              videoEosSent = true
            }
          }

          drainAudio()
          drainVideo()
        }

        onProgress(ExportProgress("Finalizing MP4 container...", 1f))
        Result.success(outputFile)
      } catch (e: Exception) {
        Log.e(TAG, "MP4 export failed", e)
        Result.failure(e)
      } finally {
        renderer.clearCache()
        runCatching { frameBitmap?.recycle() }
        runCatching { eglSurfaceHelper?.release() }
        runCatching { videoCodec?.stop(); videoCodec?.release() }
        runCatching { audioCodec?.stop(); audioCodec?.release() }
        runCatching { muxer?.stop(); muxer?.release() }
      }
    }

  suspend fun saveToDeviceMovies(context: Context, mp4File: File, title: String): Result<Uri> =
    withContext(Dispatchers.IO) {
      runCatching {
        val safeName =
          title.replace(Regex("[^a-zA-Z0-9_\\- ]"), "").trim().ifBlank { "VideoAgent_Doc" }
        val fileName = "${safeName}_${System.currentTimeMillis()}.mp4"

        val values =
          ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
              put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/VideoAgent")
              put(MediaStore.Video.Media.IS_PENDING, 1)
            }
          }

        val resolver = context.contentResolver
        val uri =
          resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("Could not create MediaStore entry for MP4.")

        resolver.openOutputStream(uri)?.use { out ->
          mp4File.inputStream().use { input -> input.copyTo(out) }
        } ?: throw IllegalStateException("Could not open MediaStore output stream.")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          values.clear()
          values.put(MediaStore.Video.Media.IS_PENDING, 0)
          resolver.update(uri, values, null, null)
        }
        uri
      }
    }
}

/**
 * Lightweight EGL14 + GLES20 quad drawer that uploads rendered documentary frames to the
 * MediaCodec input Surface with exact presentation timestamps (EGLExt.eglPresentationTimeANDROID).
 */
private class EglBitmapInputSurface(
  private val surface: Surface,
  private val width: Int,
  private val height: Int,
) {
  private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
  private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
  private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE
  private var programId = 0
  private var textureId = 0
  private val vertexBuffer: FloatBuffer

  init {
    val coords =
      floatArrayOf(
        // X, Y, U, V (flip V so Bitmap top-left aligns with GL bottom-left)
        -1f, -1f, 0f, 1f,
        1f, -1f, 1f, 1f,
        -1f, 1f, 0f, 0f,
        1f, 1f, 1f, 0f,
      )
    vertexBuffer =
      ByteBuffer.allocateDirect(coords.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply {
          put(coords)
          position(0)
        }
    setupEgl()
    setupGl()
  }

  private fun setupEgl() {
    eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
    val version = IntArray(2)
    EGL14.eglInitialize(eglDisplay, version, 0, version, 1)

    val attribList =
      intArrayOf(
        EGL14.EGL_RED_SIZE, 8,
        EGL14.EGL_GREEN_SIZE, 8,
        EGL14.EGL_BLUE_SIZE, 8,
        EGL14.EGL_ALPHA_SIZE, 8,
        EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
        0x3142, 1, // EGL_RECORDABLE_ANDROID
        EGL14.EGL_NONE,
      )
    val configs = arrayOfNulls<EGLConfig>(1)
    val numConfigs = IntArray(1)
    EGL14.eglChooseConfig(eglDisplay, attribList, 0, configs, 0, 1, numConfigs, 0)
    val config = configs[0] ?: throw IllegalStateException("No matching EGLConfig for recording")

    val contextAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
    eglContext =
      EGL14.eglCreateContext(eglDisplay, config, EGL14.EGL_NO_CONTEXT, contextAttribs, 0)

    val surfaceAttribs = intArrayOf(EGL14.EGL_NONE)
    eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, config, surface, surfaceAttribs, 0)
  }

  private fun setupGl() {
    makeCurrent()
    val vs =
      """
      attribute vec2 aPos;
      attribute vec2 aTex;
      varying vec2 vTex;
      void main() {
        gl_Position = vec4(aPos, 0.0, 1.0);
        vTex = aTex;
      }
      """.trimIndent()
    val fs =
      """
      precision mediump float;
      varying vec2 vTex;
      uniform sampler2D uTex;
      void main() {
        gl_FragColor = texture2D(uTex, vTex);
      }
      """.trimIndent()

    val vShader = compileShader(GLES20.GL_VERTEX_SHADER, vs)
    val fShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fs)
    programId = GLES20.glCreateProgram()
    GLES20.glAttachShader(programId, vShader)
    GLES20.glAttachShader(programId, fShader)
    GLES20.glLinkProgram(programId)

    val textures = IntArray(1)
    GLES20.glGenTextures(1, textures, 0)
    textureId = textures[0]
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
  }

  private fun compileShader(type: Int, code: String): Int {
    val shader = GLES20.glCreateShader(type)
    GLES20.glShaderSource(shader, code)
    GLES20.glCompileShader(shader)
    return shader
  }

  fun makeCurrent() {
    EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)
  }

  fun drawBitmapAndSwap(bitmap: Bitmap, ptsNs: Long) {
    makeCurrent()
    GLES20.glViewport(0, 0, width, height)
    GLES20.glUseProgram(programId)

    GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
    GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)

    val aPos = GLES20.glGetAttribLocation(programId, "aPos")
    val aTex = GLES20.glGetAttribLocation(programId, "aTex")
    val uTex = GLES20.glGetUniformLocation(programId, "uTex")

    vertexBuffer.position(0)
    GLES20.glEnableVertexAttribArray(aPos)
    GLES20.glVertexAttribPointer(aPos, 2, GLES20.GL_FLOAT, false, 16, vertexBuffer)

    vertexBuffer.position(2)
    GLES20.glEnableVertexAttribArray(aTex)
    GLES20.glVertexAttribPointer(aTex, 2, GLES20.GL_FLOAT, false, 16, vertexBuffer)

    GLES20.glUniform1i(uTex, 0)
    GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

    EGLExt.eglPresentationTimeANDROID(eglDisplay, eglSurface, ptsNs)
    EGL14.eglSwapBuffers(eglDisplay, eglSurface)
  }

  fun release() {
    if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
      EGL14.eglMakeCurrent(
        eglDisplay,
        EGL14.EGL_NO_SURFACE,
        EGL14.EGL_NO_SURFACE,
        EGL14.EGL_NO_CONTEXT,
      )
      EGL14.eglDestroySurface(eglDisplay, eglSurface)
      EGL14.eglDestroyContext(eglDisplay, eglContext)
      EGL14.eglReleaseThread()
      EGL14.eglTerminate(eglDisplay)
    }
    surface.release()
  }
}
