package com.example.media.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.data.model.CameraMovement
import com.example.data.model.ColorGrade
import com.example.data.model.DocumentaryProject
import com.example.data.model.DocumentaryScene
import com.example.data.model.SceneTransition
import com.example.data.model.SubtitleFontStyle
import com.example.data.model.SubtitlePosition
import com.example.data.model.SubtitleSegment
import kotlin.math.min

/**
 * Unified frame-rendering engine used identically by both the Live Preview Canvas
 * and the 1080p MP4 MediaCodec Surface exporter.
 */
class CinematicFrameRenderer(private val context: Context) {

  private val bitmapCache = LinkedHashMap<String, Bitmap>(16, 0.75f, true)
  private val typefaceCache = mutableMapOf<SubtitleFontStyle, Typeface>()

  fun clearCache() {
    bitmapCache.values.forEach { if (!it.isRecycled) it.recycle() }
    bitmapCache.clear()
  }

  fun drawFrame(
    canvas: Canvas,
    width: Int,
    height: Int,
    project: DocumentaryProject,
    subtitles: List<SubtitleSegment>,
    timeMs: Long,
  ) {
    canvas.drawColor(Color.rgb(8, 10, 15))
    val scenes = project.scenes
    if (scenes.isEmpty()) return

    // Resolve active scene and previous scene
    var elapsed = 0L
    var activeIdx = scenes.lastIndex
    var sceneStartMs = 0L
    for (i in scenes.indices) {
      val dur = scenes[i].effectiveDurationMs
      if (timeMs < elapsed + dur || i == scenes.lastIndex) {
        activeIdx = i
        sceneStartMs = elapsed
        break
      }
      elapsed += dur
    }

    val activeScene = scenes[activeIdx]
    val sceneDur = activeScene.effectiveDurationMs.coerceAtLeast(1L)
    val localTimeMs = (timeMs - sceneStartMs).coerceIn(0L, sceneDur)
    val progress = (localTimeMs.toFloat() / sceneDur.toFloat()).coerceIn(0f, 1f)

    val transitionDurMs = 520f
    val transProgress = (localTimeMs / transitionDurMs).coerceIn(0f, 1f)
    val prevScene = if (activeIdx > 0 && localTimeMs < transitionDurMs) scenes[activeIdx - 1] else null

    val colorFilter = buildColorGradeFilter(project.colorGrade)

    // 1. Draw incoming transition or active scene
    if (prevScene != null) {
      when (activeScene.transition) {
        SceneTransition.CROSSFADE -> {
          drawSceneVisual(canvas, width, height, prevScene, 1f, 255, colorFilter, 0f)
          drawSceneVisual(
            canvas,
            width,
            height,
            activeScene,
            progress,
            (transProgress * 255).toInt(),
            colorFilter,
            0f,
          )
        }
        SceneTransition.DIP_TO_BLACK -> {
          if (transProgress < 0.5f) {
            val alpha = ((1f - transProgress * 2f) * 255).toInt().coerceIn(0, 255)
            drawSceneVisual(canvas, width, height, prevScene, 1f, alpha, colorFilter, 0f)
          } else {
            val alpha = (((transProgress - 0.5f) * 2f) * 255).toInt().coerceIn(0, 255)
            drawSceneVisual(canvas, width, height, activeScene, progress, alpha, colorFilter, 0f)
          }
        }
        SceneTransition.SLIDE_LEFT -> {
          val offset = (1f - easeOutCubic(transProgress)) * width
          drawSceneVisual(canvas, width, height, prevScene, 1f, 255, colorFilter, offset - width)
          drawSceneVisual(canvas, width, height, activeScene, progress, 255, colorFilter, offset)
        }
        SceneTransition.WHIP_ZOOM -> {
          drawSceneVisual(canvas, width, height, prevScene, 1f, ((1f - transProgress) * 255).toInt(), colorFilter, 0f)
          val extraScale = 1.0f + (1f - easeOutCubic(transProgress)) * 0.28f
          drawSceneVisual(
            canvas,
            width,
            height,
            activeScene,
            progress,
            (transProgress * 255).toInt(),
            colorFilter,
            0f,
            extraScaleMultiplier = extraScale,
          )
        }
      }
    } else {
      drawSceneVisual(canvas, width, height, activeScene, progress, 255, colorFilter, 0f)
    }

    // 2. Subtle cinema letterbox / matte framing for 16:9 or editorial framing for 9:16
    val barHeight = (height * 0.045f)
    val barPaint = Paint().apply { color = Color.argb(230, 5, 7, 11) }
    canvas.drawRect(0f, 0f, width.toFloat(), barHeight, barPaint)
    canvas.drawRect(0f, height - barHeight, width.toFloat(), height.toFloat(), barPaint)

    // 3. Lower-third / Motion Graphics overlay during first 3.4s of scene
    if (activeScene.onScreenText.isNotBlank() && localTimeMs < 3600L) {
      val appear = (localTimeMs / 350f).coerceIn(0f, 1f)
      val fadeOut = ((3600L - localTimeMs) / 400f).coerceIn(0f, 1f)
      val alpha = (min(appear, fadeOut) * 255).toInt().coerceIn(0, 255)
      drawMotionGraphicBanner(canvas, width, height, activeScene, alpha, easeOutCubic(appear))
    }

    // 4. Burned-in / Live Synchronized Subtitles
    if (project.subtitleStyle.enabled) {
      val activeSub = subtitles.firstOrNull { timeMs in it.startMs..it.endMs }
      if (activeSub != null) {
        val subDur = (activeSub.endMs - activeSub.startMs).coerceAtLeast(1L)
        val subProgress = ((timeMs - activeSub.startMs).toFloat() / subDur).coerceIn(0f, 1f)
        drawSubtitleOverlay(canvas, width, height, activeSub, project, subProgress)
      }
    }
  }

  private fun drawSceneVisual(
    canvas: Canvas,
    width: Int,
    height: Int,
    scene: DocumentaryScene,
    progress: Float,
    alpha: Int,
    colorFilter: ColorMatrixColorFilter?,
    translateXOffset: Float,
    extraScaleMultiplier: Float = 1f,
  ) {
    val bmp = loadSceneBitmap(scene) ?: return
    val baseScale = maxOf(width.toFloat() / bmp.width, height.toFloat() / bmp.height)

    // Ken Burns camera motion
    val (zoom, panXFrac, panYFrac) =
      when (scene.cameraMovement) {
        CameraMovement.SLOW_ZOOM_IN -> Triple(1.04f + 0.14f * progress, 0f, 0f)
        CameraMovement.SLOW_ZOOM_OUT -> Triple(1.18f - 0.14f * progress, 0f, 0f)
        CameraMovement.PAN_LEFT -> Triple(1.14f, 0.05f - 0.10f * progress, 0f)
        CameraMovement.PAN_RIGHT -> Triple(1.14f, -0.05f + 0.10f * progress, 0f)
        CameraMovement.TRACKING_DRIFT -> Triple(1.10f + 0.06f * progress, -0.03f + 0.06f * progress, 0.02f - 0.04f * progress)
      }

    val finalScale = baseScale * zoom * extraScaleMultiplier
    val drawnW = bmp.width * finalScale
    val drawnH = bmp.height * finalScale
    val dx = (width - drawnW) / 2f + panXFrac * width + translateXOffset
    val dy = (height - drawnH) / 2f + panYFrac * height

    val matrix = Matrix().apply {
      postScale(finalScale, finalScale)
      postTranslate(dx, dy)
    }

    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
      this.alpha = alpha.coerceIn(0, 255)
      this.colorFilter = colorFilter
    }
    canvas.drawBitmap(bmp, matrix, paint)
  }

  private fun drawMotionGraphicBanner(
    canvas: Canvas,
    width: Int,
    height: Int,
    scene: DocumentaryScene,
    alpha: Int,
    slideIn: Float,
  ) {
    val scale = width / 1280f
    val padX = 48f * scale
    val cardBottom = height * 0.80f
    val cardHeight = 82f * scale
    val cardTop = cardBottom - cardHeight
    val slideOffset = (1f - slideIn) * (-80f * scale)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      shader =
        LinearGradient(
          padX + slideOffset,
          cardTop,
          width * 0.65f,
          cardBottom,
          Color.argb((alpha * 0.88f).toInt(), 12, 16, 26),
          Color.argb((alpha * 0.35f).toInt(), 12, 16, 26),
          Shader.TileMode.CLAMP,
        )
    }
    val rect = RectF(padX + slideOffset, cardTop, width * 0.72f + slideOffset, cardBottom)
    canvas.drawRoundRect(rect, 10f * scale, 10f * scale, bgPaint)

    // Amber kinetic accent bar
    val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.argb(alpha, 245, 158, 11)
    }
    canvas.drawRoundRect(
      RectF(padX + slideOffset, cardTop, padX + slideOffset + 7f * scale, cardBottom),
      4f * scale,
      4f * scale,
      accentPaint,
    )

    val monoTypeface = getTypeface(SubtitleFontStyle.JETBRAINS_MONO)
    val displayTypeface = getTypeface(SubtitleFontStyle.SPACE_GROTESK)

    val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.argb(alpha, 34, 211, 238)
      textSize = 17f * scale
      typeface = monoTypeface
    }
    canvas.drawText(
      "SCENE 0${scene.sceneNumber} · ${scene.shotType.label.uppercase()}",
      padX + slideOffset + 22f * scale,
      cardTop + 28f * scale,
      badgePaint,
    )

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.argb(alpha, 248, 250, 252)
      textSize = 27f * scale
      typeface = Typeface.create(displayTypeface, Typeface.BOLD)
    }
    canvas.drawText(
      scene.onScreenText.take(34),
      padX + slideOffset + 22f * scale,
      cardTop + 62f * scale,
      titlePaint,
    )
  }

  private fun drawSubtitleOverlay(
    canvas: Canvas,
    width: Int,
    height: Int,
    segment: SubtitleSegment,
    project: DocumentaryProject,
    subProgress: Float,
  ) {
    val style = project.subtitleStyle
    val scale = min(width, height) / 720f
    val textSizePx = style.fontSizeSp * 1.45f * scale
    val animScale =
      if (style.animatedCaptions && subProgress < 0.14f) {
        0.92f + 0.08f * (subProgress / 0.14f)
      } else {
        1f
      }
    val effectiveSize = textSizePx * animScale
    val typeface = Typeface.create(getTypeface(style.fontStyle), Typeface.BOLD)

    val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      this.textSize = effectiveSize
      color = Color.WHITE
    }
    val highlightPaint = Paint(basePaint).apply {
      color = Color.rgb(245, 190, 35)
    }

    val words = segment.text.split(" ").filter { it.isNotBlank() }
    val maxLineWidth = width * 0.84f
    val lines = mutableListOf<List<String>>()
    var currentLine = mutableListOf<String>()
    var currentWidth = 0f
    val spaceW = basePaint.measureText(" ")

    for (w in words) {
      val ww = basePaint.measureText(w)
      if (currentLine.isNotEmpty() && currentWidth + spaceW + ww > maxLineWidth) {
        lines.add(currentLine)
        currentLine = mutableListOf(w)
        currentWidth = ww
      } else {
        if (currentLine.isNotEmpty()) currentWidth += spaceW
        currentLine.add(w)
        currentWidth += ww
      }
    }
    if (currentLine.isNotEmpty()) lines.add(currentLine)

    val lineHeight = effectiveSize * 1.32f
    val totalHeight = lines.size * lineHeight
    val centerY =
      when (style.position) {
        SubtitlePosition.TOP -> height * 0.14f + totalHeight / 2f
        SubtitlePosition.CENTER -> height * 0.50f
        SubtitlePosition.BOTTOM -> height * 0.85f - totalHeight / 2f
      }

    // Measure widest line for pill background
    val maxActualW =
      lines.maxOfOrNull { line ->
        line.sumOf { basePaint.measureText(it).toDouble() }.toFloat() +
          spaceW * (line.size - 1).coerceAtLeast(0)
      } ?: 0f

    val padH = 24f * scale
    val padV = 14f * scale
    val boxRect =
      RectF(
        width / 2f - maxActualW / 2f - padH,
        centerY - totalHeight / 2f - padV,
        width / 2f + maxActualW / 2f + padH,
        centerY + totalHeight / 2f + padV * 0.6f,
      )
    val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.argb(185, 8, 10, 16)
    }
    canvas.drawRoundRect(boxRect, 14f * scale, 14f * scale, boxPaint)

    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.argb(70, 255, 255, 255)
      this.style = Paint.Style.STROKE
      strokeWidth = 1.5f * scale
    }
    canvas.drawRoundRect(boxRect, 14f * scale, 14f * scale, strokePaint)

    val cleanHighlight = segment.highlightWord?.trim('.', ',', '!', '?', ':', ';')?.lowercase()
    var y = centerY - totalHeight / 2f + effectiveSize * 0.92f
    for (line in lines) {
      val lineW =
        line.sumOf { basePaint.measureText(it).toDouble() }.toFloat() +
          spaceW * (line.size - 1).coerceAtLeast(0)
      var x = width / 2f - lineW / 2f
      for (word in line) {
        val cleanW = word.trim('.', ',', '!', '?', ':', ';').lowercase()
        val useHighlight = style.highlightKeywords && cleanHighlight != null && cleanW == cleanHighlight
        val p = if (useHighlight) highlightPaint else basePaint
        canvas.drawText(word, x, y, p)
        x += p.measureText(word) + spaceW
      }
      y += lineHeight
    }
  }

  private fun loadSceneBitmap(scene: DocumentaryScene): Bitmap? {
    val path = scene.visualPath
    if (!path.isNullOrBlank()) {
      bitmapCache[path]?.let { if (!it.isRecycled) return it }
      val decoded = BitmapFactory.decodeFile(path)
      if (decoded != null) {
        if (bitmapCache.size >= 12) {
          val eldestKey = bitmapCache.keys.firstOrNull()
          if (eldestKey != null) {
            bitmapCache.remove(eldestKey)?.recycle()
          }
        }
        bitmapCache[path] = decoded
        return decoded
      }
    }
    // Fallback to studio hero drawable if scene visual has not yet been generated
    val fallbackKey = "studio_hero_fallback"
    bitmapCache[fallbackKey]?.let { if (!it.isRecycled) return it }
    val fallback = BitmapFactory.decodeResource(context.resources, R.drawable.img_studio_hero_1795268398221)
    if (fallback != null) bitmapCache[fallbackKey] = fallback
    return fallback
  }

  private fun getTypeface(style: SubtitleFontStyle): Typeface {
    return typefaceCache.getOrPut(style) {
      val fontRes =
        when (style) {
          SubtitleFontStyle.SPACE_GROTESK -> R.font.space_grotesk
          SubtitleFontStyle.PLAYFAIR_SERIF -> R.font.playfair_display
          SubtitleFontStyle.JETBRAINS_MONO -> R.font.jetbrains_mono
          SubtitleFontStyle.PLUS_JAKARTA -> R.font.plus_jakarta_sans
        }
      runCatching { ResourcesCompat.getFont(context, fontRes) }.getOrNull() ?: Typeface.DEFAULT_BOLD
    }
  }

  private fun buildColorGradeFilter(grade: ColorGrade): ColorMatrixColorFilter? {
    val matrix = ColorMatrix()
    when (grade) {
      ColorGrade.NATURAL_CINEMA -> return null
      ColorGrade.TEAL_AND_ORANGE -> {
        matrix.set(
          floatArrayOf(
            1.12f, 0f, -0.04f, 0f, 8f,
            0f, 1.02f, 0.04f, 0f, 0f,
            -0.06f, 0.06f, 1.14f, 0f, 6f,
            0f, 0f, 0f, 1f, 0f,
          )
        )
      }
      ColorGrade.NOIR_CONTRAST -> {
        val sat = ColorMatrix().apply { setSaturation(0.28f) }
        val contrast =
          ColorMatrix(
            floatArrayOf(
              1.25f, 0f, 0f, 0f, -22f,
              0f, 1.25f, 0f, 0f, -22f,
              0f, 0f, 1.28f, 0f, -18f,
              0f, 0f, 0f, 1f, 0f,
            )
          )
        matrix.postConcat(sat)
        matrix.postConcat(contrast)
      }
      ColorGrade.WARM_ARCHIVAL -> {
        matrix.set(
          floatArrayOf(
            1.18f, 0.08f, 0f, 0f, 10f,
            0.04f, 1.04f, 0f, 0f, 4f,
            0f, 0f, 0.82f, 0f, -6f,
            0f, 0f, 0f, 1f, 0f,
          )
        )
      }
      ColorGrade.CYBER_NEON -> {
        matrix.set(
          floatArrayOf(
            0.94f, 0f, 0.14f, 0f, 0f,
            0.04f, 1.08f, 0.12f, 0f, 4f,
            0.08f, 0.08f, 1.24f, 0f, 12f,
            0f, 0f, 0f, 1f, 0f,
          )
        )
      }
    }
    return ColorMatrixColorFilter(matrix)
  }

  private fun easeOutCubic(t: Float): Float {
    val inv = 1f - t.coerceIn(0f, 1f)
    return 1f - inv * inv * inv
  }
}
