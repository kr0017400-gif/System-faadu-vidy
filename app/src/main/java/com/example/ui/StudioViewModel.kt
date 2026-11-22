package com.example.ui

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AiRepository
import com.example.data.ai.SampleAiRepository
import com.example.data.local.AppDatabase
import com.example.data.local.ProjectRepository
import com.example.data.model.AspectRatioMode
import com.example.data.model.AudioMixConfig
import com.example.data.model.ColorGrade
import com.example.data.model.ContentCategory
import com.example.data.model.DocumentaryProject
import com.example.data.model.DocumentaryScene
import com.example.data.model.ExportResolution
import com.example.data.model.MusicMood
import com.example.data.model.NarrationLanguage
import com.example.data.model.ProductionStep
import com.example.data.model.SubtitleSegment
import com.example.data.model.SubtitleStyleConfig
import com.example.data.model.TargetDuration
import com.example.data.model.VisualStyle
import com.example.data.model.VoiceOption
import com.example.data.model.VoiceSettings
import com.example.media.render.AudioMixEngine
import com.example.media.render.ExportProgress
import com.example.media.render.Mp4VideoExporter
import com.example.media.render.SubtitleTimelineBuilder
import com.example.media.voice.VoiceoverEngine
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "StudioViewModel"

data class StudioUiStatus(
  val isGeneratingScript: Boolean = false,
  val isPlanningScenes: Boolean = false,
  val isBatchProducingAssets: Boolean = false,
  val batchProgressLabel: String = "",
  val batchProgress: Float = 0f,
  val isExporting: Boolean = false,
  val exportProgress: ExportProgress? = null,
  val isPreparingPreviewAudio: Boolean = false,
  val playingSceneAudioId: String? = null,
  val statusMessage: String? = null,
  val errorMessage: String? = null,
  val exportedGalleryUri: Uri? = null,
  val showApiInfoDialog: Boolean = false,
)

class StudioViewModel(
  private val appContext: Context,
  private val projectRepository: ProjectRepository,
  private val aiRepository: AiRepository,
  private val voiceoverEngine: VoiceoverEngine,
) : ViewModel() {

  private val _currentProject = MutableStateFlow(createInitialSampleProject())
  val currentProject: StateFlow<DocumentaryProject> = _currentProject.asStateFlow()

  private val _activeStep = MutableStateFlow(ProductionStep.CREATE)
  val activeStep: StateFlow<ProductionStep> = _activeStep.asStateFlow()

  private val _uiStatus = MutableStateFlow(StudioUiStatus())
  val uiStatus: StateFlow<StudioUiStatus> = _uiStatus.asStateFlow()

  private val _availableVoices = MutableStateFlow<List<VoiceOption>>(emptyList())
  val availableVoices: StateFlow<List<VoiceOption>> = _availableVoices.asStateFlow()

  private val _previewTimeMs = MutableStateFlow(0L)
  val previewTimeMs: StateFlow<Long> = _previewTimeMs.asStateFlow()

  private val _isPreviewPlaying = MutableStateFlow(false)
  val isPreviewPlaying: StateFlow<Boolean> = _isPreviewPlaying.asStateFlow()

  val subtitleTimeline: StateFlow<List<SubtitleSegment>> =
    combine(_currentProject) { arr -> SubtitleTimelineBuilder.buildTimeline(arr[0].scenes) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val projectHistory: StateFlow<List<DocumentaryProject>> =
    projectRepository
      .observeProjects()
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  private var previewJob: Job? = null
  private var exportJob: Job? = null
  private var previewAudioPlayer: MediaPlayer? = null
  private var visualSeedCounter = 1

  init {
    refreshAvailableVoices()
    // Ensure initial workspace project is saved/persisted or ready
    viewModelScope.launch {
      val existing = projectRepository.getProject(_currentProject.value.id)
      if (existing != null) {
        _currentProject.value = existing
      }
    }
  }

  fun setStep(step: ProductionStep) {
    stopPreviewPlayback()
    _activeStep.value = step
  }

  fun toggleApiInfoDialog(show: Boolean) {
    _uiStatus.value = _uiStatus.value.copy(showApiInfoDialog = show)
  }

  fun clearMessages() {
    _uiStatus.value = _uiStatus.value.copy(errorMessage = null, statusMessage = null)
  }

  fun updateTopic(topic: String) = mutateProject { it.copy(topic = topic) }

  fun updateTitle(title: String) = mutateProject { it.copy(title = title) }

  fun updateCategory(category: ContentCategory) = mutateProject { it.copy(category = category) }

  fun updateDuration(duration: TargetDuration) = mutateProject { it.copy(targetDuration = duration) }

  fun updateAspectRatio(ratio: AspectRatioMode) = mutateProject { it.copy(aspectRatio = ratio) }

  fun updateLanguage(language: NarrationLanguage) {
    mutateProject { it.copy(language = language) }
    refreshAvailableVoices()
  }

  fun updateVisualStyle(style: VisualStyle) = mutateProject { it.copy(visualStyle = style) }

  fun updateColorGrade(grade: ColorGrade) = mutateProject { it.copy(colorGrade = grade) }

  fun updateExportResolution(res: ExportResolution) = mutateProject {
    it.copy(exportedResolution = res)
  }

  fun updateScriptText(script: String) = mutateProject {
    it.copy(scriptText = script, isScriptApproved = false)
  }

  fun updateVoiceSettings(transform: (VoiceSettings) -> VoiceSettings) = mutateProject {
    it.copy(voiceSettings = transform(it.voiceSettings))
  }

  fun updateAudioMix(transform: (AudioMixConfig) -> AudioMixConfig) = mutateProject {
    it.copy(audioMix = transform(it.audioMix))
  }

  fun updateSubtitleStyle(transform: (SubtitleStyleConfig) -> SubtitleStyleConfig) = mutateProject {
    it.copy(subtitleStyle = transform(it.subtitleStyle))
  }

  fun refreshAvailableVoices() {
    viewModelScope.launch {
      val voices = voiceoverEngine.getAvailableVoices(_currentProject.value.language)
      _availableVoices.value = voices
      val currentId = _currentProject.value.voiceSettings.voiceId
      if (voices.isNotEmpty() && voices.none { it.id == currentId }) {
        val first = voices.first()
        mutateProject {
          it.copy(
            voiceSettings =
              it.voiceSettings.copy(voiceId = first.id, voiceName = first.displayName)
          )
        }
      }
    }
  }

  fun generateScript() {
    val proj = _currentProject.value
    if (proj.topic.isBlank()) {
      _uiStatus.value = _uiStatus.value.copy(errorMessage = "Enter a documentary topic first.")
      return
    }
    viewModelScope.launch {
      _uiStatus.value = _uiStatus.value.copy(isGeneratingScript = true, errorMessage = null)
      val result =
        aiRepository.generateScript(
          topic = proj.topic,
          category = proj.category,
          duration = proj.targetDuration,
          language = proj.language,
          visualStyle = proj.visualStyle,
        )
      result
        .onSuccess { generated ->
          mutateProject(saveToDb = true) {
            it.copy(
              title = generated.title,
              scriptText = generated.script,
              isScriptApproved = false,
              isSampleAiGenerated = generated.isSampleMode,
            )
          }
          _uiStatus.value =
            _uiStatus.value.copy(
              isGeneratingScript = false,
              statusMessage = "Script generated! Review or edit below, then approve to plan scenes.",
            )
        }
        .onFailure { e ->
          if (e is CancellationException) throw e
          _uiStatus.value =
            _uiStatus.value.copy(
              isGeneratingScript = false,
              errorMessage = e.localizedMessage ?: "Script generation failed.",
            )
        }
    }
  }

  fun approveScriptAndPlanScenes() {
    val proj = _currentProject.value
    if (proj.scriptText.isBlank()) {
      _uiStatus.value = _uiStatus.value.copy(errorMessage = "Generate or write a script first.")
      return
    }
    viewModelScope.launch {
      _uiStatus.value = _uiStatus.value.copy(isPlanningScenes = true, errorMessage = null)
      val res =
        aiRepository.planScenes(
          topic = proj.topic.ifBlank { proj.title },
          script = proj.scriptText,
          category = proj.category,
          duration = proj.targetDuration,
          language = proj.language,
          visualStyle = proj.visualStyle,
        )
      res
        .onSuccess { scenes ->
          mutateProject(saveToDb = true) {
            it.copy(
              isScriptApproved = true,
              scenes = scenes,
              exportedVideoPath = null,
            )
          }
          _uiStatus.value =
            _uiStatus.value.copy(
              isPlanningScenes = false,
              statusMessage = "Planned ${scenes.size} documentary scenes. Ready to produce visuals & voiceover!",
            )
          _activeStep.value = ProductionStep.SCENES
        }
        .onFailure { e ->
          if (e is CancellationException) throw e
          _uiStatus.value =
            _uiStatus.value.copy(
              isPlanningScenes = false,
              errorMessage = e.localizedMessage ?: "Scene planning failed.",
            )
        }
    }
  }

  fun produceAllScenesVisualsAndVoice() {
    val proj = _currentProject.value
    if (proj.scenes.isEmpty()) {
      _uiStatus.value = _uiStatus.value.copy(errorMessage = "Plan scenes first.")
      return
    }
    viewModelScope.launch {
      _uiStatus.value =
        _uiStatus.value.copy(
          isBatchProducingAssets = true,
          batchProgress = 0f,
          batchProgressLabel = "Starting scene production...",
          errorMessage = null,
        )

      val scenes = _currentProject.value.scenes
      val totalSteps = scenes.size * 2
      var completedSteps = 0

      val visualsDir = projectVisualsDir(proj.id)
      val audioDir = projectVoiceDir(proj.id)

      for (scene in scenes) {
        // 1. Visual
        _uiStatus.value =
          _uiStatus.value.copy(
            batchProgressLabel = "Scene ${scene.sceneNumber}/${scenes.size}: Rendering B-roll visual...",
          )
        updateScene(scene.id) { it.copy(isGeneratingVisual = true, visualError = null) }
        val visRes =
          aiRepository.generateSceneVisual(
            scene = scene,
            aspectRatio = _currentProject.value.aspectRatio,
            visualStyle = _currentProject.value.visualStyle,
            category = _currentProject.value.category,
            outputDir = visualsDir,
            variationSeed = visualSeedCounter++,
          )
        visRes
          .onSuccess { path ->
            updateScene(scene.id) {
              it.copy(
                visualPath = path,
                isGeneratingVisual = false,
                visualError = null,
                isUsingSampleVisual = aiRepository.isSampleMode,
              )
            }
          }
          .onFailure { err ->
            if (err is CancellationException) throw err
            updateScene(scene.id) {
              it.copy(isGeneratingVisual = false, visualError = err.localizedMessage ?: "Visual failed")
            }
          }
        completedSteps++
        _uiStatus.value =
          _uiStatus.value.copy(batchProgress = completedSteps.toFloat() / totalSteps)

        // 2. Voiceover
        _uiStatus.value =
          _uiStatus.value.copy(
            batchProgressLabel = "Scene ${scene.sceneNumber}/${scenes.size}: Synthesizing TTS narration...",
          )
        updateScene(scene.id) { it.copy(isGeneratingVoice = true, voiceError = null) }
        val voFile = File(audioDir, "vo_scene_${scene.sceneNumber}_${System.currentTimeMillis()}.wav")
        val voRes =
          voiceoverEngine.synthesizeSceneVoiceover(
            text = scene.narration,
            language = _currentProject.value.language,
            voiceSettings = _currentProject.value.voiceSettings,
            outputFile = voFile,
          )
        voRes
          .onSuccess { synth ->
            updateScene(scene.id) {
              it.copy(
                voiceoverPath = synth.audioPath,
                voiceoverDurationMs = synth.durationMs,
                isGeneratingVoice = false,
                voiceError = null,
              )
            }
          }
          .onFailure { err ->
            if (err is CancellationException) throw err
            updateScene(scene.id) {
              it.copy(isGeneratingVoice = false, voiceError = err.localizedMessage ?: "TTS failed")
            }
          }
        completedSteps++
        _uiStatus.value =
          _uiStatus.value.copy(batchProgress = completedSteps.toFloat() / totalSteps)
      }

      saveCurrentProjectNow()
      _uiStatus.value =
        _uiStatus.value.copy(
          isBatchProducingAssets = false,
          batchProgressLabel = "All scenes processed",
          statusMessage = "Scene visuals & voiceovers ready. Open Timeline & Mix to refine and preview!",
        )
    }
  }

  fun regenerateSceneVisual(sceneId: String) {
    val proj = _currentProject.value
    val scene = proj.scenes.firstOrNull { it.id == sceneId } ?: return
    viewModelScope.launch {
      updateScene(sceneId) { it.copy(isGeneratingVisual = true, visualError = null) }
      val res =
        aiRepository.generateSceneVisual(
          scene = scene,
          aspectRatio = proj.aspectRatio,
          visualStyle = proj.visualStyle,
          category = proj.category,
          outputDir = projectVisualsDir(proj.id),
          variationSeed = visualSeedCounter++,
        )
      res
        .onSuccess { path ->
          updateScene(sceneId, save = true) {
            it.copy(
              visualPath = path,
              isGeneratingVisual = false,
              visualError = null,
              isUsingSampleVisual = aiRepository.isSampleMode,
            )
          }
        }
        .onFailure { e ->
          if (e is CancellationException) throw e
          updateScene(sceneId) {
            it.copy(isGeneratingVisual = false, visualError = e.localizedMessage ?: "Retry failed")
          }
        }
    }
  }

  fun regenerateSceneVoiceover(sceneId: String) {
    val proj = _currentProject.value
    val scene = proj.scenes.firstOrNull { it.id == sceneId } ?: return
    viewModelScope.launch {
      updateScene(sceneId) { it.copy(isGeneratingVoice = true, voiceError = null) }
      val voFile =
        File(projectVoiceDir(proj.id), "vo_scene_${scene.sceneNumber}_${System.currentTimeMillis()}.wav")
      val res =
        voiceoverEngine.synthesizeSceneVoiceover(
          text = scene.narration,
          language = proj.language,
          voiceSettings = proj.voiceSettings,
          outputFile = voFile,
        )
      res
        .onSuccess { synth ->
          updateScene(sceneId, save = true) {
            it.copy(
              voiceoverPath = synth.audioPath,
              voiceoverDurationMs = synth.durationMs,
              isGeneratingVoice = false,
              voiceError = null,
            )
          }
        }
        .onFailure { e ->
          if (e is CancellationException) throw e
          updateScene(sceneId) {
            it.copy(isGeneratingVoice = false, voiceError = e.localizedMessage ?: "TTS retry failed")
          }
        }
    }
  }

  fun replaceSceneVisualFromUri(sceneId: String, uri: Uri) {
    val proj = _currentProject.value
    val scene = proj.scenes.firstOrNull { it.id == sceneId } ?: return
    viewModelScope.launch {
      val copied = copyUriToFile(uri, File(projectVisualsDir(proj.id), "custom_${scene.sceneNumber}_${System.currentTimeMillis()}.jpg"))
      if (copied != null) {
        updateScene(sceneId, save = true) {
          it.copy(visualPath = copied.absolutePath, isUsingSampleVisual = false, visualError = null)
        }
        _uiStatus.value = _uiStatus.value.copy(statusMessage = "Custom photo applied to Scene ${scene.sceneNumber}.")
      } else {
        _uiStatus.value = _uiStatus.value.copy(errorMessage = "Could not import selected photo.")
      }
    }
  }

  fun importCustomBgm(uri: Uri) {
    val proj = _currentProject.value
    viewModelScope.launch {
      val displayName = resolveFileName(uri) ?: "Custom_BGM"
      val dest = File(projectAudioDir(proj.id), "custom_bgm_${System.currentTimeMillis()}_$displayName")
      val copied = copyUriToFile(uri, dest)
      if (copied != null) {
        mutateProject(saveToDb = true) {
          it.copy(
            audioMix =
              it.audioMix.copy(
                musicMood = MusicMood.CUSTOM_USER_AUDIO,
                customBgmPath = copied.absolutePath,
                customBgmName = displayName,
              )
          )
        }
        _uiStatus.value = _uiStatus.value.copy(statusMessage = "Imported custom audio: $displayName")
      } else {
        _uiStatus.value = _uiStatus.value.copy(errorMessage = "Failed to import custom audio file.")
      }
    }
  }

  fun previewSceneAudio(scene: DocumentaryScene) {
    val path = scene.voiceoverPath ?: return
    if (_uiStatus.value.playingSceneAudioId == scene.id) {
      voiceoverEngine.stopPlayback()
      _uiStatus.value = _uiStatus.value.copy(playingSceneAudioId = null)
    } else {
      _uiStatus.value = _uiStatus.value.copy(playingSceneAudioId = scene.id)
      voiceoverEngine.playFile(path) {
        _uiStatus.value = _uiStatus.value.copy(playingSceneAudioId = null)
      }
    }
  }

  fun updateScene(sceneId: String, save: Boolean = false, transform: (DocumentaryScene) -> DocumentaryScene) {
    mutateProject(saveToDb = save) { proj ->
      proj.copy(
        scenes = proj.scenes.map { if (it.id == sceneId) transform(it) else it }
      )
    }
  }

  fun deleteScene(sceneId: String) {
    mutateProject(saveToDb = true) { proj ->
      val remaining =
        proj.scenes
          .filterNot { it.id == sceneId }
          .mapIndexed { idx, s -> s.copy(sceneNumber = idx + 1) }
      proj.copy(scenes = remaining)
    }
  }

  fun moveScene(sceneId: String, delta: Int) {
    mutateProject(saveToDb = true) { proj ->
      val list = proj.scenes.toMutableList()
      val idx = list.indexOfFirst { it.id == sceneId }
      val target = idx + delta
      if (idx in list.indices && target in list.indices) {
        val item = list.removeAt(idx)
        list.add(target, item)
        proj.copy(scenes = list.mapIndexed { i, s -> s.copy(sceneNumber = i + 1) })
      } else proj
    }
  }

  fun togglePreviewPlayback() {
    if (_isPreviewPlaying.value) {
      stopPreviewPlayback()
    } else {
      startPreviewPlayback()
    }
  }

  fun seekPreview(timeMs: Long) {
    val maxDur = _currentProject.value.totalDurationMs.coerceAtLeast(1000L)
    val clamped = timeMs.coerceIn(0L, maxDur)
    _previewTimeMs.value = clamped
    runCatching {
      previewAudioPlayer?.let { mp ->
        if (mp.isPlaying) mp.seekTo(clamped.toInt())
      }
    }
  }

  private fun startPreviewPlayback() {
    val proj = _currentProject.value
    if (proj.scenes.isEmpty()) return

    previewJob?.cancel()
    previewJob =
      viewModelScope.launch {
        _uiStatus.value = _uiStatus.value.copy(isPreparingPreviewAudio = true)
        val previewWav = File(appContext.cacheDir, "preview_mix_${proj.id}.wav")
        runCatching { AudioMixEngine.renderMixedPreviewWav(appContext, proj, previewWav) }
          .onFailure { Log.e(TAG, "Preview audio mix failed", it) }

        _uiStatus.value = _uiStatus.value.copy(isPreparingPreviewAudio = false)
        _isPreviewPlaying.value = true

        val totalMs = proj.totalDurationMs.coerceAtLeast(3000L)
        if (_previewTimeMs.value >= totalMs - 200L) {
          _previewTimeMs.value = 0L
        }
        val startOffsetMs = _previewTimeMs.value

        runCatching {
          previewAudioPlayer?.release()
          if (previewWav.exists() && previewWav.length() > 44L) {
            previewAudioPlayer =
              MediaPlayer().apply {
                setDataSource(previewWav.absolutePath)
                prepare()
                if (startOffsetMs > 0L) seekTo(startOffsetMs.toInt())
                start()
              }
          }
        }

        val wallStart = System.currentTimeMillis() - startOffsetMs
        while (isActive && _isPreviewPlaying.value) {
          val elapsed = System.currentTimeMillis() - wallStart
          if (elapsed >= totalMs) {
            _previewTimeMs.value = totalMs
            stopPreviewPlayback()
            break
          }
          _previewTimeMs.value = elapsed
          delay(33L) // ~30fps live preview
        }
      }
  }

  fun stopPreviewPlayback() {
    _isPreviewPlaying.value = false
    previewJob?.cancel()
    previewJob = null
    runCatching {
      previewAudioPlayer?.stop()
      previewAudioPlayer?.release()
    }
    previewAudioPlayer = null
  }

  fun exportFinalMp4() {
    val proj = _currentProject.value
    if (proj.scenes.isEmpty()) {
      _uiStatus.value = _uiStatus.value.copy(errorMessage = "Plan at least one scene before rendering MP4.")
      return
    }
    stopPreviewPlayback()
    exportJob?.cancel()
    exportJob =
      viewModelScope.launch {
        _uiStatus.value =
          _uiStatus.value.copy(
            isExporting = true,
            exportProgress = ExportProgress("Initializing MP4 studio pipeline...", 0.02f),
            errorMessage = null,
            exportedGalleryUri = null,
          )
        val outDir = File(appContext.filesDir, "exports").apply { mkdirs() }
        val outFile = File(outDir, "doc_${proj.id}_${System.currentTimeMillis()}.mp4")
        val res =
          Mp4VideoExporter.exportProjectToMp4(appContext, proj, outFile) { progress ->
            _uiStatus.value = _uiStatus.value.copy(exportProgress = progress)
          }
        res
          .onSuccess { file ->
            mutateProject(saveToDb = true) { it.copy(exportedVideoPath = file.absolutePath) }
            _uiStatus.value =
              _uiStatus.value.copy(
                isExporting = false,
                exportProgress = null,
                statusMessage = "1080p/HD MP4 rendered successfully (${file.length() / 1024} KB)!",
              )
          }
          .onFailure { e ->
            if (e is CancellationException) {
              _uiStatus.value =
                _uiStatus.value.copy(
                  isExporting = false,
                  exportProgress = null,
                  statusMessage = "Render cancelled.",
                )
              throw e
            }
            _uiStatus.value =
              _uiStatus.value.copy(
                isExporting = false,
                exportProgress = null,
                errorMessage = e.localizedMessage ?: "MP4 rendering failed.",
              )
          }
      }
  }

  fun cancelExport() {
    exportJob?.cancel()
    exportJob = null
  }

  fun saveExportedMp4ToMovies() {
    val path = _currentProject.value.exportedVideoPath ?: return
    val file = File(path)
    if (!file.exists()) {
      _uiStatus.value = _uiStatus.value.copy(errorMessage = "Rendered MP4 file not found; please re-render.")
      return
    }
    viewModelScope.launch {
      Mp4VideoExporter.saveToDeviceMovies(appContext, file, _currentProject.value.title)
        .onSuccess { uri ->
          _uiStatus.value =
            _uiStatus.value.copy(
              exportedGalleryUri = uri,
              statusMessage = "Saved MP4 to Movies/VideoAgent on your device!",
            )
        }
        .onFailure { e ->
          _uiStatus.value =
            _uiStatus.value.copy(errorMessage = e.localizedMessage ?: "Failed to save to Movies.")
        }
    }
  }

  fun createNewProject() {
    stopPreviewPlayback()
    _currentProject.value =
      DocumentaryProject(
        id = UUID.randomUUID().toString(),
        title = "Untitled Documentary",
      )
    _activeStep.value = ProductionStep.CREATE
    _previewTimeMs.value = 0L
  }

  fun loadProject(projectId: String) {
    stopPreviewPlayback()
    viewModelScope.launch {
      val loaded = projectRepository.getProject(projectId) ?: return@launch
      _currentProject.value = loaded
      _previewTimeMs.value = 0L
      _activeStep.value =
        when {
          loaded.exportedVideoPath != null -> ProductionStep.EXPORT
          loaded.scenes.isNotEmpty() -> ProductionStep.SCENES
          else -> ProductionStep.CREATE
        }
    }
  }

  fun deleteProject(projectId: String) {
    viewModelScope.launch {
      projectRepository.deleteProject(projectId)
      if (_currentProject.value.id == projectId) {
        createNewProject()
      }
    }
  }

  private fun mutateProject(
    saveToDb: Boolean = false,
    transform: (DocumentaryProject) -> DocumentaryProject,
  ) {
    val updated = transform(_currentProject.value).copy(updatedAtMillis = System.currentTimeMillis())
    _currentProject.value = updated
    if (saveToDb) {
      viewModelScope.launch { projectRepository.saveProject(updated) }
    }
  }

  private fun saveCurrentProjectNow() {
    val snap = _currentProject.value
    viewModelScope.launch { projectRepository.saveProject(snap) }
  }

  private fun projectVisualsDir(projectId: String): File =
    File(appContext.filesDir, "projects/$projectId/visuals").apply { mkdirs() }

  private fun projectVoiceDir(projectId: String): File =
    File(appContext.filesDir, "projects/$projectId/voice").apply { mkdirs() }

  private fun projectAudioDir(projectId: String): File =
    File(appContext.filesDir, "projects/$projectId/audio").apply { mkdirs() }

  private suspend fun copyUriToFile(uri: Uri, destFile: File): File? =
    withContext(Dispatchers.IO) {
      runCatching {
        destFile.parentFile?.mkdirs()
        appContext.contentResolver.openInputStream(uri)?.use { input ->
          FileOutputStream(destFile).use { output -> input.copyTo(output) }
        } ?: return@withContext null
        destFile
      }.getOrNull()
    }

  private fun resolveFileName(uri: Uri): String? {
    return runCatching {
      appContext.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
      }
    }.getOrNull()
  }

  override fun onCleared() {
    super.onCleared()
    stopPreviewPlayback()
    voiceoverEngine.release()
  }

  private fun createInitialSampleProject(): DocumentaryProject =
    DocumentaryProject(
      topic = "The Hidden Physics of Black Holes",
      title = "Untitled Documentary",
      category = ContentCategory.SCIENCE_TECH,
      targetDuration = TargetDuration.SEC_60,
      aspectRatio = AspectRatioMode.LANDSCAPE_16_9,
      language = NarrationLanguage.ENGLISH,
      visualStyle = VisualStyle.CINEMATIC_DOC,
    )

  companion object {
    fun provideFactory(context: Context): ViewModelProvider.Factory =
      object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
          val appCtx = context.applicationContext
          val db = AppDatabase.getInstance(appCtx)
          val repo = ProjectRepository(db.documentaryDao())
          val ai = SampleAiRepository(appCtx)
          val tts = VoiceoverEngine(appCtx)
          return StudioViewModel(appCtx, repo, ai, tts) as T
        }
      }
  }
}
