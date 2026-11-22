package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MovieFilter
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.AspectRatioMode
import com.example.data.model.CameraMovement
import com.example.data.model.DocumentaryProject
import com.example.data.model.DocumentaryScene
import com.example.data.model.SceneTransition
import com.example.data.model.SfxType
import com.example.data.model.ShotType
import com.example.ui.StudioUiStatus
import com.example.ui.components.SampleAiNoticeBanner
import com.example.ui.theme.CinemaAmber
import com.example.ui.theme.CrimsonRecord
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldReady
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioSurfaceHighest
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletMotion
import java.io.File

@Composable
fun ScenesAssetsScreen(
  project: DocumentaryProject,
  uiStatus: StudioUiStatus,
  onProduceAll: () -> Unit,
  onRegenerateVisual: (String) -> Unit,
  onRegenerateVoice: (String) -> Unit,
  onPickCustomPhoto: (String, Uri) -> Unit,
  onPreviewAudio: (DocumentaryScene) -> Unit,
  onUpdateScene: (String, (DocumentaryScene) -> DocumentaryScene) -> Unit,
  onMoveScene: (String, Int) -> Unit,
  onDeleteScene: (String) -> Unit,
  onGoToCreate: () -> Unit,
  onProceedToEditor: () -> Unit,
  onShowApiInfo: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var photoTargetSceneId by remember { mutableStateOf<String?>(null) }
  var editingScene by remember { mutableStateOf<DocumentaryScene?>(null) }

  val photoPicker =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      val targetId = photoTargetSceneId
      if (uri != null && targetId != null) {
        onPickCustomPhoto(targetId, uri)
      }
      photoTargetSceneId = null
    }

  if (project.scenes.isEmpty()) {
    Box(
      modifier = modifier.fillMaxSize().padding(24.dp),
      contentAlignment = Alignment.Center,
    ) {
      Surface(
        color = StudioSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, HairlineBorder),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Icon(
            Icons.Rounded.MovieFilter,
            contentDescription = null,
            tint = CinemaAmber,
            modifier = Modifier.size(48.dp),
          )
          Text("No Scenes Planned Yet", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
          Text(
            "Generate and approve a script in Step 1 to automatically plan scenes, camera directions, B-roll prompts, and SFX cues.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
          )
          Button(
            onClick = onGoToCreate,
            colors = ButtonDefaults.buttonColors(containerColor = CinemaAmber),
            modifier = Modifier.testTag("back_to_brief_button"),
          ) {
            Text("Go to Brief & Script")
          }
        }
      }
    }
    return
  }

  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val columns = if (maxWidth >= 760.dp) 2 else 1

    LazyVerticalGrid(
      columns = GridCells.Fixed(columns),
      contentPadding = PaddingValues(16.dp),
      horizontalArrangement = Arrangement.spacedBy(14.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
      modifier = Modifier.fillMaxSize(),
    ) {
      item(span = { GridItemSpan(columns) }) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          SampleAiNoticeBanner(onLearnMore = onShowApiInfo)

          Surface(
            color = StudioSurface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, HairlineBorder),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    "${project.scenes.size} Scenes · ${"%.1f".format(project.totalDurationMs / 1000f)}s Total",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                  )
                  Text(
                    "Generate stylized B-roll stills (animated with Ken Burns motion) & TTS narration per scene.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                  )
                }
              }

              if (uiStatus.isBatchProducingAssets) {
                LinearProgressIndicator(
                  progress = { uiStatus.batchProgress },
                  color = ElectricCyan,
                  trackColor = StudioSurfaceHighest,
                  modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                )
                Text(
                  uiStatus.batchProgressLabel,
                  style = MaterialTheme.typography.labelMedium,
                  color = ElectricCyan,
                )
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Button(
                  onClick = onProduceAll,
                  enabled = !uiStatus.isBatchProducingAssets,
                  colors = ButtonDefaults.buttonColors(containerColor = CinemaAmber),
                  modifier = Modifier.weight(1f).testTag("produce_all_assets_button"),
                ) {
                  Icon(Icons.Rounded.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(Modifier.width(8.dp))
                  Text(
                    if (uiStatus.isBatchProducingAssets) "Producing..." else "Produce All Visuals & Voice",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }

                OutlinedButton(
                  onClick = onProceedToEditor,
                  border = BorderStroke(1.dp, ElectricCyan),
                  colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricCyan),
                  modifier = Modifier.testTag("open_timeline_button"),
                ) {
                  Text("Editor")
                  Spacer(Modifier.width(4.dp))
                  Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
              }
            }
          }
        }
      }

      items(project.scenes, key = { it.id }) { scene ->
        DocumentarySceneCard(
          scene = scene,
          aspectRatio = project.aspectRatio,
          isPlayingAudio = uiStatus.playingSceneAudioId == scene.id,
          isFirst = scene.sceneNumber == 1,
          isLast = scene.sceneNumber == project.scenes.size,
          onRegenerateVisual = { onRegenerateVisual(scene.id) },
          onPickPhoto = {
            photoTargetSceneId = scene.id
            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
          },
          onRegenerateVoice = { onRegenerateVoice(scene.id) },
          onPreviewAudio = { onPreviewAudio(scene) },
          onEdit = { editingScene = scene },
          onMoveUp = { onMoveScene(scene.id, -1) },
          onMoveDown = { onMoveScene(scene.id, 1) },
          onDelete = { onDeleteScene(scene.id) },
        )
      }
    }
  }

  editingScene?.let { current ->
    EditSceneDialog(
      scene = current,
      onDismiss = { editingScene = null },
      onSave = { updated ->
        onUpdateScene(current.id) { updated }
        editingScene = null
      },
    )
  }
}

@Composable
private fun DocumentarySceneCard(
  scene: DocumentaryScene,
  aspectRatio: AspectRatioMode,
  isPlayingAudio: Boolean,
  isFirst: Boolean,
  isLast: Boolean,
  onRegenerateVisual: () -> Unit,
  onPickPhoto: () -> Unit,
  onRegenerateVoice: () -> Unit,
  onPreviewAudio: () -> Unit,
  onEdit: () -> Unit,
  onMoveUp: () -> Unit,
  onMoveDown: () -> Unit,
  onDelete: () -> Unit,
) {
  Surface(
    color = StudioSurface,
    shape = RoundedCornerShape(16.dp),
    border = BorderStroke(1.dp, HairlineBorder),
    modifier = Modifier.fillMaxWidth().testTag("scene_card_${scene.sceneNumber}"),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      // Top Scene Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Surface(color = CinemaAmber, shape = RoundedCornerShape(6.dp)) {
            Text(
              text = "SCENE ${scene.sceneNumber}",
              style = MaterialTheme.typography.labelSmall,
              color = ObsidianBg,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            )
          }
          Text(
            text = "${"%.1f".format(scene.effectiveDurationMs / 1000f)}s · ${scene.cameraMovement.label}",
            style = MaterialTheme.typography.labelMedium,
            color = ElectricCyan,
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(onClick = onMoveUp, enabled = !isFirst, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Rounded.ArrowUpward, contentDescription = "Move Up", tint = if (isFirst) TextMuted else TextSecondary)
          }
          IconButton(onClick = onMoveDown, enabled = !isLast, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Rounded.ArrowDownward, contentDescription = "Move Down", tint = if (isLast) TextMuted else TextSecondary)
          }
          IconButton(onClick = onEdit, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Rounded.Edit, contentDescription = "Edit Scene", tint = CinemaAmber)
          }
          IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Rounded.Delete, contentDescription = "Delete Scene", tint = CrimsonRecord)
          }
        }
      }

      // Visual Preview Container
      val ratioValue = if (aspectRatio == AspectRatioMode.LANDSCAPE_16_9) 16f / 9f else 9f / 16f
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .then(
              if (aspectRatio == AspectRatioMode.PORTRAIT_9_16) Modifier.height(230.dp)
              else Modifier.aspectRatio(ratioValue)
            )
            .clip(RoundedCornerShape(12.dp))
            .background(ObsidianBg),
        contentAlignment = Alignment.Center,
      ) {
        if (scene.visualPath != null && File(scene.visualPath).exists()) {
          AsyncImage(
            model = File(scene.visualPath),
            contentDescription = "Scene ${scene.sceneNumber} visual",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
          )
        } else {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(16.dp),
          ) {
            Text(
              "Visual not yet rendered",
              style = MaterialTheme.typography.labelMedium,
              color = TextMuted,
            )
            Text(
              scene.visualPrompt,
              style = MaterialTheme.typography.bodySmall,
              color = TextSecondary,
              maxLines = 3,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        if (scene.isGeneratingVisual) {
          Box(
            modifier = Modifier.fillMaxSize().background(ObsidianBg.copy(alpha = 0.72f)),
            contentAlignment = Alignment.Center,
          ) {
            CircularProgressIndicator(color = CinemaAmber)
          }
        }

        // Honest badge: never pretend a still image is a generated video clip
        Surface(
          color = ObsidianBg.copy(alpha = 0.82f),
          shape = RoundedCornerShape(6.dp),
          modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
        ) {
          Text(
            text =
              if (scene.visualPath == null) "PENDING"
              else if (scene.isUsingSampleVisual) "SAMPLE STILL + KEN BURNS"
              else "USER PHOTO + KEN BURNS",
            style = MaterialTheme.typography.labelSmall,
            color = if (scene.isUsingSampleVisual) CinemaAmber else EmeraldReady,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
          )
        }
      }

      if (scene.visualError != null) {
        Surface(
          color = CrimsonRecord.copy(alpha = 0.16f),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = CrimsonRecord, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(scene.visualError, style = MaterialTheme.typography.bodySmall, color = CrimsonRecord, modifier = Modifier.weight(1f))
            TextButton(onClick = onRegenerateVisual) { Text("Retry") }
          }
        }
      }

      // Narration & Metadata
      Text(scene.narration, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)

      Surface(
        color = StudioSurfaceElevated,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(10.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Text(
            "Shot: ${scene.shotType.label} · Transition: ${scene.transition.label}",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
          )
          Text(
            "Lighting: ${scene.lightingAtmosphere}",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
          )
          Text(
            "Motion GFX: ${scene.motionGraphicsInstruction} (\"${scene.onScreenText}\")",
            style = MaterialTheme.typography.bodySmall,
            color = VioletMotion,
          )
          Text(
            "SFX: ${scene.sfxType.label} · Music: ${scene.musicCue}",
            style = MaterialTheme.typography.bodySmall,
            color = ElectricCyan,
          )
        }
      }

      if (scene.voiceError != null) {
        Surface(
          color = CrimsonRecord.copy(alpha = 0.16f),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = CrimsonRecord, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(scene.voiceError, style = MaterialTheme.typography.bodySmall, color = CrimsonRecord, modifier = Modifier.weight(1f))
            TextButton(onClick = onRegenerateVoice) { Text("Retry TTS") }
          }
        }
      }

      // Per-Scene Action Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedButton(
          onClick = onRegenerateVisual,
          enabled = !scene.isGeneratingVisual,
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
          modifier = Modifier.weight(1f).testTag("regen_visual_${scene.sceneNumber}"),
        ) {
          Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
          Spacer(Modifier.width(4.dp))
          Text("Regen B-Roll", maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        OutlinedButton(
          onClick = onPickPhoto,
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
          modifier = Modifier.weight(1f).testTag("pick_photo_${scene.sceneNumber}"),
        ) {
          Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(15.dp))
          Spacer(Modifier.width(4.dp))
          Text("Photo", maxLines = 1)
        }

        if (scene.voiceoverPath != null && File(scene.voiceoverPath).exists()) {
          Button(
            onClick = onPreviewAudio,
            colors =
              ButtonDefaults.buttonColors(
                containerColor = if (isPlayingAudio) CrimsonRecord else EmeraldReady,
              ),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            modifier = Modifier.testTag("play_vo_${scene.sceneNumber}"),
          ) {
            Icon(
              if (isPlayingAudio) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
              contentDescription = "Preview Voiceover",
              modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text("${"%.1f".format(scene.voiceoverDurationMs / 1000f)}s")
          }
        } else {
          OutlinedButton(
            onClick = onRegenerateVoice,
            enabled = !scene.isGeneratingVoice,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            modifier = Modifier.testTag("gen_vo_${scene.sceneNumber}"),
          ) {
            if (scene.isGeneratingVoice) {
              CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
            } else {
              Icon(Icons.Rounded.GraphicEq, contentDescription = null, modifier = Modifier.size(15.dp))
            }
            Spacer(Modifier.width(4.dp))
            Text("Voice")
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditSceneDialog(
  scene: DocumentaryScene,
  onDismiss: () -> Unit,
  onSave: (DocumentaryScene) -> Unit,
) {
  var narration by remember { mutableStateOf(scene.narration) }
  var onScreenText by remember { mutableStateOf(scene.onScreenText) }
  var visualPrompt by remember { mutableStateOf(scene.visualPrompt) }
  var durationSec by remember { mutableStateOf(scene.durationSec) }
  var camera by remember { mutableStateOf(scene.cameraMovement) }
  var shot by remember { mutableStateOf(scene.shotType) }
  var sfx by remember { mutableStateOf(scene.sfxType) }
  var transition by remember { mutableStateOf(scene.transition) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = StudioSurfaceElevated,
    title = { Text("Edit Scene ${scene.sceneNumber}", color = TextPrimary) },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedTextField(
          value = narration,
          onValueChange = { narration = it },
          label = { Text("Voiceover Narration") },
          minLines = 3,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = onScreenText,
          onValueChange = { onScreenText = it },
          label = { Text("Motion Graphics Lower-Third Text") },
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = visualPrompt,
          onValueChange = { visualPrompt = it },
          label = { Text("Visual B-Roll Prompt") },
          minLines = 2,
          modifier = Modifier.fillMaxWidth(),
        )
        Text("Duration: ${"%.1f".format(durationSec)}s", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Slider(
          value = durationSec,
          onValueChange = { durationSec = it },
          valueRange = 3f..30f,
        )
        Text("Camera Movement", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          CameraMovement.entries.forEach { cm ->
            FilterChip(selected = camera == cm, onClick = { camera = cm }, label = { Text(cm.label) })
          }
        }
        Text("Shot Type", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          ShotType.entries.forEach { st ->
            FilterChip(selected = shot == st, onClick = { shot = st }, label = { Text(st.label) })
          }
        }
        Text("Sound Effect (SFX)", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          SfxType.entries.forEach { sf ->
            FilterChip(selected = sfx == sf, onClick = { sfx = sf }, label = { Text(sf.label) })
          }
        }
        Text("Transition", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          SceneTransition.entries.forEach { tr ->
            FilterChip(selected = transition == tr, onClick = { transition = tr }, label = { Text(tr.label) })
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onSave(
            scene.copy(
              narration = narration,
              onScreenText = onScreenText,
              visualPrompt = visualPrompt,
              durationSec = durationSec,
              cameraMovement = camera,
              shotType = shot,
              sfxType = sfx,
              transition = transition,
            )
          )
        },
        colors = ButtonDefaults.buttonColors(containerColor = CinemaAmber),
      ) {
        Text("Save Scene")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}
