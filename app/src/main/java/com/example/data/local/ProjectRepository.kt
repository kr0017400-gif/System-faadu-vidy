package com.example.data.local

import com.example.data.model.AspectRatioMode
import com.example.data.model.AudioMixConfig
import com.example.data.model.CameraMovement
import com.example.data.model.ColorGrade
import com.example.data.model.ContentCategory
import com.example.data.model.DocumentaryProject
import com.example.data.model.DocumentaryScene
import com.example.data.model.ExportResolution
import com.example.data.model.MusicMood
import com.example.data.model.NarrationLanguage
import com.example.data.model.SceneTransition
import com.example.data.model.SfxType
import com.example.data.model.ShotType
import com.example.data.model.SubtitleFontStyle
import com.example.data.model.SubtitlePosition
import com.example.data.model.SubtitleStyleConfig
import com.example.data.model.TargetDuration
import com.example.data.model.VisualStyle
import com.example.data.model.VoiceSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

class ProjectRepository(private val dao: DocumentaryDao) {

  fun observeProjects(): Flow<List<DocumentaryProject>> =
    dao.observeAllProjects().map { list -> list.map { it.toDomain() } }

  suspend fun getProject(id: String): DocumentaryProject? = dao.getProjectById(id)?.toDomain()

  suspend fun saveProject(project: DocumentaryProject) {
    dao.upsertProject(project.copy(updatedAtMillis = System.currentTimeMillis()).toEntity())
  }

  suspend fun deleteProject(id: String) {
    dao.deleteProjectById(id)
  }

  private fun DocumentaryProject.toEntity(): DocumentaryEntity {
    val array = JSONArray()
    scenes.forEach { scene ->
      array.put(
        JSONObject().apply {
          put("id", scene.id)
          put("sceneNumber", scene.sceneNumber)
          put("durationSec", scene.durationSec.toDouble())
          put("narration", scene.narration)
          put("visualPrompt", scene.visualPrompt)
          put("cameraMovement", scene.cameraMovement.name)
          put("shotType", scene.shotType.name)
          put("lightingAtmosphere", scene.lightingAtmosphere)
          put("motionGraphicsInstruction", scene.motionGraphicsInstruction)
          put("onScreenText", scene.onScreenText)
          put("sfxType", scene.sfxType.name)
          put("musicCue", scene.musicCue)
          put("transition", scene.transition.name)
          put("visualPath", scene.visualPath ?: JSONObject.NULL)
          put("voiceoverPath", scene.voiceoverPath ?: JSONObject.NULL)
          put("voiceoverDurationMs", scene.voiceoverDurationMs)
          put("isUsingSampleVisual", scene.isUsingSampleVisual)
        }
      )
    }

    return DocumentaryEntity(
      id = id,
      title = title,
      topic = topic,
      categoryName = category.name,
      targetDurationName = targetDuration.name,
      aspectRatioName = aspectRatio.name,
      languageName = language.name,
      visualStyleName = visualStyle.name,
      colorGradeName = colorGrade.name,
      scriptText = scriptText,
      isScriptApproved = isScriptApproved,
      isSampleAiGenerated = isSampleAiGenerated,
      scenesJson = array.toString(),
      voiceId = voiceSettings.voiceId,
      voiceName = voiceSettings.voiceName,
      voiceSpeed = voiceSettings.speed,
      voicePitch = voiceSettings.pitch,
      musicMoodName = audioMix.musicMood.name,
      customBgmPath = audioMix.customBgmPath,
      customBgmName = audioMix.customBgmName,
      bgmVolume = audioMix.bgmVolume,
      voiceoverVolume = audioMix.voiceoverVolume,
      sfxVolume = audioMix.sfxVolume,
      autoDucking = audioMix.autoDucking,
      subtitlesEnabled = subtitleStyle.enabled,
      subtitlePositionName = subtitleStyle.position.name,
      subtitleFontName = subtitleStyle.fontStyle.name,
      subtitleFontSizeSp = subtitleStyle.fontSizeSp,
      subtitleHighlightKeywords = subtitleStyle.highlightKeywords,
      subtitleAnimatedCaptions = subtitleStyle.animatedCaptions,
      exportedVideoPath = exportedVideoPath,
      exportedResolutionName = exportedResolution.name,
      updatedAtMillis = updatedAtMillis,
    )
  }

  private fun DocumentaryEntity.toDomain(): DocumentaryProject {
    val parsedScenes = mutableListOf<DocumentaryScene>()
    runCatching {
      val array = JSONArray(scenesJson)
      for (i in 0 until array.length()) {
        val obj = array.getJSONObject(i)
        parsedScenes +=
          DocumentaryScene(
            id = obj.optString("id"),
            sceneNumber = obj.optInt("sceneNumber", i + 1),
            durationSec = obj.optDouble("durationSec", 6.0).toFloat(),
            narration = obj.optString("narration"),
            visualPrompt = obj.optString("visualPrompt"),
            cameraMovement = safeEnum(obj.optString("cameraMovement"), CameraMovement.SLOW_ZOOM_IN),
            shotType = safeEnum(obj.optString("shotType"), ShotType.WIDE_ESTABLISHING),
            lightingAtmosphere = obj.optString("lightingAtmosphere"),
            motionGraphicsInstruction = obj.optString("motionGraphicsInstruction"),
            onScreenText = obj.optString("onScreenText"),
            sfxType = safeEnum(obj.optString("sfxType"), SfxType.CINEMATIC_WHOOSH),
            musicCue = obj.optString("musicCue"),
            transition = safeEnum(obj.optString("transition"), SceneTransition.CROSSFADE),
            visualPath = obj.optString("visualPath").takeIf { it.isNotBlank() && it != "null" },
            voiceoverPath =
              obj.optString("voiceoverPath").takeIf { it.isNotBlank() && it != "null" },
            voiceoverDurationMs = obj.optLong("voiceoverDurationMs", 0L),
            isUsingSampleVisual = obj.optBoolean("isUsingSampleVisual", true),
          )
      }
    }

    return DocumentaryProject(
      id = id,
      title = title,
      topic = topic,
      category = safeEnum(categoryName, ContentCategory.FACTS_MYSTERIES),
      targetDuration = safeEnum(targetDurationName, TargetDuration.SEC_60),
      aspectRatio = safeEnum(aspectRatioName, AspectRatioMode.LANDSCAPE_16_9),
      language = safeEnum(languageName, NarrationLanguage.ENGLISH),
      visualStyle = safeEnum(visualStyleName, VisualStyle.CINEMATIC_DOC),
      colorGrade = safeEnum(colorGradeName, ColorGrade.TEAL_AND_ORANGE),
      scriptText = scriptText,
      isScriptApproved = isScriptApproved,
      isSampleAiGenerated = isSampleAiGenerated,
      scenes = parsedScenes,
      voiceSettings =
        VoiceSettings(
          voiceId = voiceId,
          voiceName = voiceName,
          speed = voiceSpeed,
          pitch = voicePitch,
        ),
      audioMix =
        AudioMixConfig(
          musicMood = safeEnum(musicMoodName, MusicMood.CINEMATIC_EPIC),
          customBgmPath = customBgmPath,
          customBgmName = customBgmName,
          bgmVolume = bgmVolume,
          voiceoverVolume = voiceoverVolume,
          sfxVolume = sfxVolume,
          autoDucking = autoDucking,
        ),
      subtitleStyle =
        SubtitleStyleConfig(
          enabled = subtitlesEnabled,
          position = safeEnum(subtitlePositionName, SubtitlePosition.BOTTOM),
          fontStyle = safeEnum(subtitleFontName, SubtitleFontStyle.SPACE_GROTESK),
          fontSizeSp = subtitleFontSizeSp,
          highlightKeywords = subtitleHighlightKeywords,
          animatedCaptions = subtitleAnimatedCaptions,
        ),
      exportedVideoPath = exportedVideoPath,
      exportedResolution = safeEnum(exportedResolutionName, ExportResolution.FHD_1080P),
      updatedAtMillis = updatedAtMillis,
    )
  }

  private inline fun <reified T : Enum<T>> safeEnum(raw: String, fallback: T): T =
    enumValues<T>().firstOrNull { it.name == raw } ?: fallback
}
