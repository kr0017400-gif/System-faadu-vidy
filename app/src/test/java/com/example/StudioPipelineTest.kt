package com.example

import android.content.Context
import android.graphics.BitmapFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.ai.SampleAiRepository
import com.example.data.local.AppDatabase
import com.example.data.local.ProjectRepository
import com.example.data.model.AspectRatioMode
import com.example.data.model.ContentCategory
import com.example.data.model.DocumentaryProject
import com.example.data.model.MusicMood
import com.example.data.model.NarrationLanguage
import com.example.data.model.SfxType
import com.example.data.model.TargetDuration
import com.example.data.model.VisualStyle
import com.example.media.audio.ProceduralAudioLibrary
import com.example.media.render.SubtitleTimelineBuilder
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class StudioPipelineTest {

  @Test
  fun `sample ai generates multi-language scripts and plans scenes`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val ai = SampleAiRepository(context)

    for (lang in NarrationLanguage.entries) {
      val scriptRes =
        ai.generateScript(
          topic = "Free Fire World Series",
          category = ContentCategory.GAMING_FREE_FIRE,
          duration = TargetDuration.SEC_60,
          language = lang,
          visualStyle = VisualStyle.ESPORTS_GAMING,
        )
      assertTrue(scriptRes.isSuccess)
      val generated = scriptRes.getOrThrow()
      assertTrue(generated.script.contains("[SCENE 1]"))
      assertTrue(generated.isSampleMode)

      val scenesRes =
        ai.planScenes(
          topic = "Free Fire World Series",
          script = generated.script,
          category = ContentCategory.GAMING_FREE_FIRE,
          duration = TargetDuration.SEC_60,
          language = lang,
          visualStyle = VisualStyle.ESPORTS_GAMING,
        )
      assertTrue(scenesRes.isSuccess)
      val scenes = scenesRes.getOrThrow()
      assertEquals(TargetDuration.SEC_60.defaultSceneCount, scenes.size)
      assertTrue(scenes.all { it.narration.isNotBlank() && it.visualPrompt.isNotBlank() })
    }
  }

  @Test
  fun `visual generator produces real aspect-matched jpeg files`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val ai = SampleAiRepository(context)
    val scenes =
      ai.planScenes(
          topic = "Black Holes",
          script = "[SCENE 1]\nLook into the abyss.\n\n[SCENE 2]\nGravity bends light itself.",
          category = ContentCategory.SCIENCE_TECH,
          duration = TargetDuration.SEC_30,
          language = NarrationLanguage.ENGLISH,
          visualStyle = VisualStyle.SCI_FI_TECH,
        )
        .getOrThrow()

    val outDir = File(context.cacheDir, "test_visuals_${System.currentTimeMillis()}")
    val resPortrait =
      ai.generateSceneVisual(
        scene = scenes.first(),
        aspectRatio = AspectRatioMode.PORTRAIT_9_16,
        visualStyle = VisualStyle.SCI_FI_TECH,
        category = ContentCategory.SCIENCE_TECH,
        outputDir = outDir,
      )
    assertTrue(resPortrait.isSuccess)
    val file = File(resPortrait.getOrThrow())
    assertTrue(file.exists() && file.length() > 10_000L)
    val bmp = BitmapFactory.decodeFile(file.absolutePath)
    assertNotNull(bmp)
    assertEquals(720, bmp.width)
    assertEquals(1280, bmp.height)
    bmp.recycle()
    outDir.deleteRecursively()
  }

  @Test
  fun `subtitle timeline and procedural audio wav synthesis succeed`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val ai = SampleAiRepository(context)
    val scenes =
      ai.planScenes(
          topic = "Ancient Ruins",
          script = "[SCENE 1]\nHidden beneath centuries of jungle canopy lies an astronomical temple.",
          category = ContentCategory.HISTORY_GEOGRAPHY,
          duration = TargetDuration.SEC_30,
          language = NarrationLanguage.ENGLISH,
          visualStyle = VisualStyle.ARCHIVAL_HISTORY,
        )
        .getOrThrow()

    val subs = SubtitleTimelineBuilder.buildTimeline(scenes)
    assertTrue(subs.isNotEmpty())
    assertTrue(subs.all { it.endMs > it.startMs && it.text.isNotBlank() })

    val bgm = ProceduralAudioLibrary.ensureBgmWav(context, MusicMood.SUSPENSE_PULSE)
    assertTrue(bgm.exists() && bgm.length() > 44L)
    val sfx = ProceduralAudioLibrary.ensureSfxWav(context, SfxType.DEEP_IMPACT)
    assertNotNull(sfx)
    assertTrue(sfx!!.exists() && sfx.length() > 44L)
  }

  @Test
  fun `room repository persists and reloads documentary project`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    try {
      val repo = ProjectRepository(db.documentaryDao())
      val ai = SampleAiRepository(context)
      val scenes =
        ai.planScenes(
            topic = "Quantum Tech",
            script = "[SCENE 1]\nQubits compute in superposition.",
            category = ContentCategory.SCIENCE_TECH,
            duration = TargetDuration.SEC_30,
            language = NarrationLanguage.ENGLISH,
            visualStyle = VisualStyle.SCI_FI_TECH,
          )
          .getOrThrow()

      val project =
        DocumentaryProject(
          id = "proj_test_1",
          title = "Quantum Breakthrough",
          topic = "Quantum Tech",
          category = ContentCategory.SCIENCE_TECH,
          scenes = scenes,
        )
      repo.saveProject(project)

      val loaded = repo.getProject("proj_test_1")
      assertNotNull(loaded)
      assertEquals("Quantum Breakthrough", loaded!!.title)
      assertEquals(1, loaded.scenes.size)
      assertFalse(loaded.scenes.first().narration.isBlank())
    } finally {
      db.close()
    }
  }
}
