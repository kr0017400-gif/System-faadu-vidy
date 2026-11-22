package com.example

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.ProductionStep
import com.example.ui.StudioViewModel
import com.example.ui.components.ApiTransparencyDialog
import com.example.ui.components.ProductionStepperBar
import com.example.ui.components.StudioTopHeader
import com.example.ui.screens.CreateBriefScreen
import com.example.ui.screens.ExportHistoryScreen
import com.example.ui.screens.ScenesAssetsScreen
import com.example.ui.screens.TimelineEditorScreen
import com.example.ui.theme.CinemaAmber
import com.example.ui.theme.CrimsonRecord
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VideoAgentTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
    )
    setContent {
      VideoAgentTheme {
        VideoAgentStudioApp()
      }
    }
  }
}

@Composable
fun VideoAgentStudioApp() {
  val context = LocalContext.current
  val viewModel: StudioViewModel =
    viewModel(factory = remember(context) { StudioViewModel.provideFactory(context) })

  val project by viewModel.currentProject.collectAsStateWithLifecycle()
  val activeStep by viewModel.activeStep.collectAsStateWithLifecycle()
  val uiStatus by viewModel.uiStatus.collectAsStateWithLifecycle()
  val availableVoices by viewModel.availableVoices.collectAsStateWithLifecycle()
  val subtitles by viewModel.subtitleTimeline.collectAsStateWithLifecycle()
  val previewTimeMs by viewModel.previewTimeMs.collectAsStateWithLifecycle()
  val isPreviewPlaying by viewModel.isPreviewPlaying.collectAsStateWithLifecycle()
  val history by viewModel.projectHistory.collectAsStateWithLifecycle()

  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(uiStatus.errorMessage) {
    val err = uiStatus.errorMessage
    if (!err.isNullOrBlank()) {
      snackbarHostState.showSnackbar(err)
      viewModel.clearMessages()
    }
  }

  LaunchedEffect(uiStatus.statusMessage) {
    val msg = uiStatus.statusMessage
    if (!msg.isNullOrBlank()) {
      snackbarHostState.showSnackbar(msg)
      viewModel.clearMessages()
    }
  }

  // Step backward smoothly on secondary screens before exiting
  BackHandler(enabled = activeStep != ProductionStep.CREATE) {
    val prev =
      when (activeStep) {
        ProductionStep.EXPORT -> ProductionStep.EDITOR
        ProductionStep.EDITOR -> ProductionStep.SCENES
        ProductionStep.SCENES -> ProductionStep.CREATE
        ProductionStep.CREATE -> ProductionStep.CREATE
      }
    viewModel.setStep(prev)
  }

  Scaffold(
    containerColor = ObsidianBg,
    contentWindowInsets = WindowInsets.safeDrawing,
    snackbarHost = {
      SnackbarHost(hostState = snackbarHostState) { data ->
        Snackbar(
          snackbarData = data,
          containerColor =
            if (uiStatus.errorMessage != null) CrimsonRecord else StudioSurfaceElevated,
          contentColor = TextPrimary,
        )
      }
    },
  ) { innerPadding ->
    BoxWithConstraints(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
    ) {
      val useSideRail = maxWidth >= 840.dp

      Column(modifier = Modifier.fillMaxSize()) {
        StudioTopHeader(
          projectTitle = project.title,
          isSampleMode = project.isSampleAiGenerated,
          onNewProject = { viewModel.createNewProject() },
          onShowApiInfo = { viewModel.toggleApiInfoDialog(true) },
        )

        if (!useSideRail) {
          ProductionStepperBar(
            activeStep = activeStep,
            sceneCount = project.scenes.size,
            hasExported = project.exportedVideoPath != null,
            onSelectStep = { viewModel.setStep(it) },
          )
        }

        Row(modifier = Modifier.weight(1f)) {
          if (useSideRail) {
            NavigationRail(
              containerColor = StudioSurface,
              modifier = Modifier.fillMaxHeight(),
            ) {
              ProductionStep.entries.forEach { step ->
                val icon =
                  when (step) {
                    ProductionStep.CREATE -> Icons.Rounded.EditNote
                    ProductionStep.SCENES -> Icons.Rounded.ViewCarousel
                    ProductionStep.EDITOR -> Icons.Rounded.Tune
                    ProductionStep.EXPORT -> Icons.Rounded.Movie
                  }
                NavigationRailItem(
                  selected = activeStep == step,
                  onClick = { viewModel.setStep(step) },
                  icon = { Icon(icon, contentDescription = step.label) },
                  label = { Text(step.label) },
                  colors =
                    NavigationRailItemDefaults.colors(
                      selectedIconColor = ObsidianBg,
                      selectedTextColor = CinemaAmber,
                      indicatorColor = CinemaAmber,
                      unselectedIconColor = TextSecondary,
                      unselectedTextColor = TextSecondary,
                    ),
                )
              }
            }
          }

          Box(modifier = Modifier.weight(1f).fillMaxSize()) {
            Crossfade(targetState = activeStep, label = "studio_step_crossfade") { step ->
              when (step) {
                ProductionStep.CREATE ->
                  CreateBriefScreen(
                    project = project,
                    uiStatus = uiStatus,
                    availableVoices = availableVoices,
                    onTopicChange = viewModel::updateTopic,
                    onCategorySelect = viewModel::updateCategory,
                    onDurationSelect = viewModel::updateDuration,
                    onRatioSelect = viewModel::updateAspectRatio,
                    onLanguageSelect = viewModel::updateLanguage,
                    onVisualStyleSelect = viewModel::updateVisualStyle,
                    onVoiceSettingsChange = viewModel::updateVoiceSettings,
                    onScriptChange = viewModel::updateScriptText,
                    onGenerateScript = viewModel::generateScript,
                    onApproveAndPlanScenes = viewModel::approveScriptAndPlanScenes,
                    onShowApiInfo = { viewModel.toggleApiInfoDialog(true) },
                  )

                ProductionStep.SCENES ->
                  ScenesAssetsScreen(
                    project = project,
                    uiStatus = uiStatus,
                    onProduceAll = viewModel::produceAllScenesVisualsAndVoice,
                    onRegenerateVisual = viewModel::regenerateSceneVisual,
                    onRegenerateVoice = viewModel::regenerateSceneVoiceover,
                    onPickCustomPhoto = viewModel::replaceSceneVisualFromUri,
                    onPreviewAudio = viewModel::previewSceneAudio,
                    onUpdateScene = { id, fn -> viewModel.updateScene(id, save = true, transform = fn) },
                    onMoveScene = viewModel::moveScene,
                    onDeleteScene = viewModel::deleteScene,
                    onGoToCreate = { viewModel.setStep(ProductionStep.CREATE) },
                    onProceedToEditor = { viewModel.setStep(ProductionStep.EDITOR) },
                    onShowApiInfo = { viewModel.toggleApiInfoDialog(true) },
                  )

                ProductionStep.EDITOR ->
                  TimelineEditorScreen(
                    project = project,
                    subtitles = subtitles,
                    previewTimeMs = previewTimeMs,
                    isPlaying = isPreviewPlaying,
                    uiStatus = uiStatus,
                    onTogglePlay = viewModel::togglePreviewPlayback,
                    onSeek = viewModel::seekPreview,
                    onColorGradeSelect = viewModel::updateColorGrade,
                    onAudioMixChange = viewModel::updateAudioMix,
                    onSubtitleStyleChange = viewModel::updateSubtitleStyle,
                    onImportCustomBgm = viewModel::importCustomBgm,
                    onProceedToExport = { viewModel.setStep(ProductionStep.EXPORT) },
                    onGoToScenes = { viewModel.setStep(ProductionStep.SCENES) },
                  )

                ProductionStep.EXPORT ->
                  ExportHistoryScreen(
                    project = project,
                    history = history,
                    uiStatus = uiStatus,
                    onSelectResolution = viewModel::updateExportResolution,
                    onRenderMp4 = viewModel::exportFinalMp4,
                    onCancelRender = viewModel::cancelExport,
                    onSaveToMovies = viewModel::saveExportedMp4ToMovies,
                    onLoadProject = viewModel::loadProject,
                    onDeleteProject = viewModel::deleteProject,
                  )
              }
            }
          }
        }
      }
    }

    if (uiStatus.showApiInfoDialog) {
      ApiTransparencyDialog(onDismiss = { viewModel.toggleApiInfoDialog(false) })
    }
  }
}
