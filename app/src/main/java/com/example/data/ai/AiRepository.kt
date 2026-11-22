package com.example.data.ai

import com.example.data.model.AspectRatioMode
import com.example.data.model.ContentCategory
import com.example.data.model.DocumentaryScene
import com.example.data.model.GeneratedScriptResult
import com.example.data.model.NarrationLanguage
import com.example.data.model.TargetDuration
import com.example.data.model.VisualStyle
import java.io.File

/**
 * Single AI abstraction for documentary scriptwriting, scene planning, and visual generation.
 * Currently backed by [SampleAiRepository] because Firebase AI Logic setup was declined.
 * Enabling Firebase allows swapping in a live Gemini 3.8 Flash + Nano Banana 2.1 implementation.
 */
interface AiRepository {
  val isSampleMode: Boolean

  suspend fun generateScript(
    topic: String,
    category: ContentCategory,
    duration: TargetDuration,
    language: NarrationLanguage,
    visualStyle: VisualStyle,
  ): Result<GeneratedScriptResult>

  suspend fun planScenes(
    topic: String,
    script: String,
    category: ContentCategory,
    duration: TargetDuration,
    language: NarrationLanguage,
    visualStyle: VisualStyle,
  ): Result<List<DocumentaryScene>>

  suspend fun generateSceneVisual(
    scene: DocumentaryScene,
    aspectRatio: AspectRatioMode,
    visualStyle: VisualStyle,
    category: ContentCategory,
    outputDir: File,
    variationSeed: Int = 0,
  ): Result<String>
}
