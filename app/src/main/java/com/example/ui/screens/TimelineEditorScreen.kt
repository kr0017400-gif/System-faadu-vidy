package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AspectRatioMode
import com.example.data.model.AudioMixConfig
import com.example.data.model.ColorGrade
import com.example.data.model.DocumentaryProject
import com.example.data.model.MusicMood
import com.example.data.model.SfxType
import com.example.data.model.SubtitleFontStyle
import com.example.data.model.SubtitlePosition
import com.example.data.model.SubtitleSegment
import com.example.data.model.SubtitleStyleConfig
import com.example.media.render.CinematicFrameRenderer
import com.example.ui.StudioUiStatus
import com.example.ui.theme.CinemaAmber
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

@Composable
fun TimelineEditorScreen(
  project: DocumentaryProject,
  subtitles: List<SubtitleSegment>,
  previewTimeMs: Long,
  isPlaying: Boolean,
  uiStatus: StudioUiStatus,
  onTogglePlay: () -> Unit,
  onSeek: (Long) -> Unit,
  onColorGradeSelect: (ColorGrade) -> Unit,
  onAudioMixChange: ((AudioMixConfig) -> AudioMixConfig) -> Unit,
  onSubtitleStyleChange: ((SubtitleStyleConfig) -> SubtitleStyleConfig) -> Unit,
  onImportCustomBgm: (Uri) -> Unit,
  onProceedToExport: () -> Unit,
  onGoToScenes: () -> Unit,
  modifier: Modifier = Modifier,
) {
  if (project.scenes.isEmpty()) {
    Box(modifier = modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
      Surface(
        color = StudioSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, HairlineBorder),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text("Timeline Requires Scenes", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
          Text(
            "Plan your documentary scenes first to unlock the multi-track editor and live preview player.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
          )
          Button(onClick = onGoToScenes, colors = ButtonDefaults.buttonColors(containerColor = CinemaAmber)) {
            Text("Go to Scenes")
          }
        }
      }
    }
    return
  }

  val context = LocalContext.current
  val renderer = remember(context) { CinematicFrameRenderer(context) }
  DisposableEffect(Unit) {
    onDispose { renderer.clearCache() }
  }

  val audioPicker =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      if (uri != null) onImportCustomBgm(uri)
    }

  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val isTabletWide = maxWidth >= 880.dp
    if (isTabletWide) {
      Row(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        Column(
          modifier = Modifier.weight(1.15f).verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          MonitorAndTransportCard(
            project = project,
            subtitles = subtitles,
            renderer = renderer,
            previewTimeMs = previewTimeMs,
            isPlaying = isPlaying,
            isPreparingAudio = uiStatus.isPreparingPreviewAudio,
            onTogglePlay = onTogglePlay,
            onSeek = onSeek,
          )
          MultiTrackTimelineCard(
            project = project,
            subtitles = subtitles,
            previewTimeMs = previewTimeMs,
            onSeek = onSeek,
          )
        }
        Column(
          modifier = Modifier.weight(0.95f).verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          StudioInspectorCard(
            project = project,
            onColorGradeSelect = onColorGradeSelect,
            onAudioMixChange = onAudioMixChange,
            onSubtitleStyleChange = onSubtitleStyleChange,
            onPickAudio = { audioPicker.launch(arrayOf("audio/*")) },
          )
          Button(
            onClick = onProceedToExport,
            colors = ButtonDefaults.buttonColors(containerColor = CinemaAmber),
            contentPadding = PaddingValues(vertical = 14.dp),
            modifier = Modifier.fillMaxWidth().testTag("go_to_export_button"),
          ) {
            Text("Render 1080p MP4 Video")
            Spacer(Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
          }
        }
      }
    } else {
      Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        MonitorAndTransportCard(
          project = project,
          subtitles = subtitles,
          renderer = renderer,
          previewTimeMs = previewTimeMs,
          isPlaying = isPlaying,
          isPreparingAudio = uiStatus.isPreparingPreviewAudio,
          onTogglePlay = onTogglePlay,
          onSeek = onSeek,
        )
        MultiTrackTimelineCard(
          project = project,
          subtitles = subtitles,
          previewTimeMs = previewTimeMs,
          onSeek = onSeek,
        )
        StudioInspectorCard(
          project = project,
          onColorGradeSelect = onColorGradeSelect,
          onAudioMixChange = onAudioMixChange,
          onSubtitleStyleChange = onSubtitleStyleChange,
          onPickAudio = { audioPicker.launch(arrayOf("audio/*")) },
        )
        Button(
          onClick = onProceedToExport,
          colors = ButtonDefaults.buttonColors(containerColor = CinemaAmber),
          contentPadding = PaddingValues(vertical = 14.dp),
          modifier = Modifier.fillMaxWidth().testTag("go_to_export_button"),
        ) {
          Text("Render 1080p MP4 Video")
          Spacer(Modifier.width(8.dp))
          Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
        }
      }
    }
  }
}

@Composable
private fun MonitorAndTransportCard(
  project: DocumentaryProject,
  subtitles: List<SubtitleSegment>,
  renderer: CinematicFrameRenderer,
  previewTimeMs: Long,
  isPlaying: Boolean,
  isPreparingAudio: Boolean,
  onTogglePlay: () -> Unit,
  onSeek: (Long) -> Unit,
) {
  val totalMs = project.totalDurationMs.coerceAtLeast(3000L)

  Surface(
    color = StudioSurface,
    shape = RoundedCornerShape(16.dp),
    border = BorderStroke(1.dp, HairlineBorder),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text("PREVIEW MONITOR (${project.aspectRatio.label})", style = MaterialTheme.typography.labelMedium, color = CinemaAmber)
        Text(
          "${formatTimecode(previewTimeMs)} / ${formatTimecode(totalMs)}",
          style = MaterialTheme.typography.labelMedium,
          color = ElectricCyan,
        )
      }

      Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(ObsidianBg),
        contentAlignment = Alignment.Center,
      ) {
        val ratio = if (project.aspectRatio == AspectRatioMode.LANDSCAPE_16_9) 16f / 9f else 9f / 16f
        Canvas(
          modifier =
            Modifier.then(
              if (project.aspectRatio == AspectRatioMode.PORTRAIT_9_16) {
                Modifier.heightIn(max = 320.dp).aspectRatio(ratio)
              } else {
                Modifier.fillMaxWidth().aspectRatio(ratio)
              }
            )
              .testTag("preview_monitor_canvas")
        ) {
          drawIntoCanvas { canvas ->
            renderer.drawFrame(
              canvas = canvas.nativeCanvas,
              width = size.width.toInt().coerceAtLeast(1),
              height = size.height.toInt().coerceAtLeast(1),
              project = project,
              subtitles = subtitles,
              timeMs = previewTimeMs,
            )
          }
        }
      }

      // Scrubber & Transport Controls
      Slider(
        value = previewTimeMs.toFloat().coerceIn(0f, totalMs.toFloat()),
        onValueChange = { onSeek(it.toLong()) },
        valueRange = 0f..totalMs.toFloat(),
        colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = ElectricCyan),
        modifier = Modifier.fillMaxWidth().testTag("preview_scrubber"),
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconButton(
          onClick = { onSeek(0L) },
          modifier = Modifier.testTag("preview_restart_button"),
        ) {
          Icon(Icons.Rounded.Replay, contentDescription = "Restart Preview", tint = TextSecondary)
        }

        Button(
          onClick = onTogglePlay,
          enabled = !isPreparingAudio,
          colors = ButtonDefaults.buttonColors(containerColor = if (isPlaying) CinemaAmber else ElectricCyan),
          shape = RoundedCornerShape(999.dp),
          modifier = Modifier.testTag("preview_play_pause_button"),
        ) {
          if (isPreparingAudio) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = ObsidianBg)
            Spacer(Modifier.width(8.dp))
            Text("Mixing Audio...", color = ObsidianBg)
          } else {
            Icon(
              if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
              contentDescription = if (isPlaying) "Pause" else "Play",
              tint = ObsidianBg,
            )
            Spacer(Modifier.width(6.dp))
            Text(if (isPlaying) "Pause" else "Play Synced Preview", color = ObsidianBg)
          }
        }

        Text(
          text = project.colorGrade.label,
          style = MaterialTheme.typography.labelSmall,
          color = TextMuted,
        )
      }
    }
  }
}

@Composable
private fun MultiTrackTimelineCard(
  project: DocumentaryProject,
  subtitles: List<SubtitleSegment>,
  previewTimeMs: Long,
  onSeek: (Long) -> Unit,
) {
  val totalMs = project.totalDurationMs.coerceAtLeast(1L)
  val scrollState = rememberScrollState()
  val dpPerSec = 26f

  Surface(
    color = StudioSurface,
    shape = RoundedCornerShape(16.dp),
    border = BorderStroke(1.dp, HairlineBorder),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text("MULTI-TRACK TIMELINE (Tap clip to seek)", style = MaterialTheme.typography.labelMedium, color = TextSecondary)

      Row(modifier = Modifier.fillMaxWidth()) {
        // Track headers column
        Column(
          modifier = Modifier.width(64.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          TrackHeaderLabel("V1 VID", CinemaAmber)
          TrackHeaderLabel("A1 VOX", ElectricCyan)
          TrackHeaderLabel("T1 SUB", VioletMotion)
          TrackHeaderLabel("A2 BGM", EmeraldReady)
          TrackHeaderLabel("A3 SFX", TextSecondary)
        }

        // Scrollable horizontal tracks
        Row(
          modifier = Modifier.weight(1f).horizontalScroll(scrollState),
        ) {
          val totalWidthDp = ((totalMs / 1000f) * dpPerSec).coerceAtLeast(240f).dp
          Column(
            modifier = Modifier.width(totalWidthDp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            // V1 Video Track
            Row(modifier = Modifier.height(34.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
              var cursor = 0L
              project.scenes.forEach { scene ->
                val clipStart = cursor
                val dur = scene.effectiveDurationMs
                val widthDp = ((dur / 1000f) * dpPerSec).coerceAtLeast(42f).dp
                val isActive = previewTimeMs in clipStart until (clipStart + dur)
                Surface(
                  color = if (isActive) CinemaAmber.copy(alpha = 0.32f) else StudioSurfaceHighest,
                  shape = RoundedCornerShape(6.dp),
                  border = BorderStroke(1.dp, if (isActive) CinemaAmber else HairlineBorder),
                  modifier =
                    Modifier.width(widthDp).fillMaxHeight().clickable { onSeek(clipStart) },
                ) {
                  Box(modifier = Modifier.padding(horizontal = 6.dp), contentAlignment = Alignment.CenterStart) {
                    Text(
                      "S${scene.sceneNumber} · ${scene.cameraMovement.label.take(8)}",
                      style = MaterialTheme.typography.labelSmall,
                      color = TextPrimary,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                }
                cursor += dur
              }
            }

            // A1 Voiceover Track
            Row(modifier = Modifier.height(28.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
              project.scenes.forEach { scene ->
                val widthDp = ((scene.effectiveDurationMs / 1000f) * dpPerSec).coerceAtLeast(42f).dp
                val hasVo = !scene.voiceoverPath.isNullOrBlank()
                Surface(
                  color = if (hasVo) ElectricCyan.copy(alpha = 0.22f) else StudioSurfaceElevated,
                  shape = RoundedCornerShape(5.dp),
                  border = BorderStroke(0.5.dp, if (hasVo) ElectricCyan.copy(alpha = 0.6f) else HairlineBorder),
                  modifier = Modifier.width(widthDp).fillMaxHeight(),
                ) {
                  Box(modifier = Modifier.padding(horizontal = 5.dp), contentAlignment = Alignment.CenterStart) {
                    Text(
                      if (hasVo) "VO ${"%.1f".format(scene.voiceoverDurationMs / 1000f)}s" else "No VO",
                      style = MaterialTheme.typography.labelSmall,
                      color = if (hasVo) ElectricCyan else TextMuted,
                      maxLines = 1,
                    )
                  }
                }
              }
            }

            // T1 Subtitles Track
            Row(modifier = Modifier.height(26.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
              subtitles.forEach { sub ->
                val subDur = (sub.endMs - sub.startMs).coerceAtLeast(400L)
                val widthDp = ((subDur / 1000f) * dpPerSec).coerceAtLeast(24f).dp
                Surface(
                  color = if (project.subtitleStyle.enabled) VioletMotion.copy(alpha = 0.24f) else StudioSurfaceElevated,
                  shape = RoundedCornerShape(4.dp),
                  modifier = Modifier.width(widthDp).fillMaxHeight().clickable { onSeek(sub.startMs) },
                ) {
                  Box(modifier = Modifier.padding(horizontal = 4.dp), contentAlignment = Alignment.CenterStart) {
                    Text(
                      sub.text,
                      style = MaterialTheme.typography.labelSmall,
                      color = TextPrimary,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                }
              }
            }

            // A2 BGM Track
            Surface(
              color = EmeraldReady.copy(alpha = 0.18f),
              shape = RoundedCornerShape(5.dp),
              border = BorderStroke(0.5.dp, EmeraldReady.copy(alpha = 0.55f)),
              modifier = Modifier.height(26.dp).fillMaxWidth(),
            ) {
              Box(modifier = Modifier.padding(horizontal = 8.dp), contentAlignment = Alignment.CenterStart) {
                Text(
                  "${project.audioMix.customBgmName ?: project.audioMix.musicMood.label} · ${if (project.audioMix.autoDucking) "Auto-Duck ON" else "Flat Gain"}",
                  style = MaterialTheme.typography.labelSmall,
                  color = EmeraldReady,
                  maxLines = 1,
                )
              }
            }

            // A3 SFX Track
            Row(modifier = Modifier.height(24.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
              project.scenes.forEach { scene ->
                val widthDp = ((scene.effectiveDurationMs / 1000f) * dpPerSec).coerceAtLeast(42f).dp
                Box(modifier = Modifier.width(widthDp).fillMaxHeight(), contentAlignment = Alignment.CenterStart) {
                  if (scene.sfxType != SfxType.NONE) {
                    Surface(
                      color = CinemaAmber.copy(alpha = 0.25f),
                      shape = RoundedCornerShape(4.dp),
                    ) {
                      Text(
                        scene.sfxType.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = CinemaAmber,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun TrackHeaderLabel(text: String, accent: androidx.compose.ui.graphics.Color) {
  Surface(
    color = StudioSurfaceElevated,
    shape = RoundedCornerShape(5.dp),
    modifier = Modifier.fillMaxWidth().height(26.dp),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(accent))
      Text(text, style = MaterialTheme.typography.labelSmall, color = TextSecondary, maxLines = 1)
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StudioInspectorCard(
  project: DocumentaryProject,
  onColorGradeSelect: (ColorGrade) -> Unit,
  onAudioMixChange: ((AudioMixConfig) -> AudioMixConfig) -> Unit,
  onSubtitleStyleChange: ((SubtitleStyleConfig) -> SubtitleStyleConfig) -> Unit,
  onPickAudio: () -> Unit,
) {
  var selectedTab by remember { mutableIntStateOf(0) }
  val tabs = listOf("Color Grade", "Subtitles", "Audio & SFX")

  Surface(
    color = StudioSurface,
    shape = RoundedCornerShape(16.dp),
    border = BorderStroke(1.dp, HairlineBorder),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column {
      TabRow(
        selectedTabIndex = selectedTab,
        containerColor = StudioSurfaceElevated,
        contentColor = CinemaAmber,
      ) {
        tabs.forEachIndexed { idx, label ->
          Tab(
            selected = selectedTab == idx,
            onClick = { selectedTab = idx },
            text = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            icon = {
              Icon(
                imageVector =
                  when (idx) {
                    0 -> Icons.Rounded.ColorLens
                    1 -> Icons.Rounded.Subtitles
                    else -> Icons.Rounded.MusicNote
                  },
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
            },
          )
        }
      }

      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        when (selectedTab) {
          0 -> {
            Text("Cinematic Color Grading LUT", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              ColorGrade.entries.forEach { grade ->
                FilterChip(
                  selected = project.colorGrade == grade,
                  onClick = { onColorGradeSelect(grade) },
                  label = { Text(grade.label) },
                  modifier = Modifier.testTag("grade_chip_${grade.name.lowercase()}"),
                )
              }
            }
          }

          1 -> {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text("Burned-in Subtitles", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
              Switch(
                checked = project.subtitleStyle.enabled,
                onCheckedChange = { en -> onSubtitleStyleChange { it.copy(enabled = en) } },
                modifier = Modifier.testTag("subtitles_toggle"),
              )
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text("Highlight Keywords", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
              Switch(
                checked = project.subtitleStyle.highlightKeywords,
                onCheckedChange = { hk -> onSubtitleStyleChange { it.copy(highlightKeywords = hk) } },
              )
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text("Animated Pop-In Captions", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
              Switch(
                checked = project.subtitleStyle.animatedCaptions,
                onCheckedChange = { ac -> onSubtitleStyleChange { it.copy(animatedCaptions = ac) } },
              )
            }

            Text("Position", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              SubtitlePosition.entries.forEach { pos ->
                FilterChip(
                  selected = project.subtitleStyle.position == pos,
                  onClick = { onSubtitleStyleChange { it.copy(position = pos) } },
                  label = { Text(pos.label) },
                )
              }
            }

            Text("Font Family", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              SubtitleFontStyle.entries.forEach { font ->
                FilterChip(
                  selected = project.subtitleStyle.fontStyle == font,
                  onClick = { onSubtitleStyleChange { it.copy(fontStyle = font) } },
                  label = { Text(font.label) },
                )
              }
            }

            Text(
              "Caption Size: ${project.subtitleStyle.fontSizeSp.toInt()}sp",
              style = MaterialTheme.typography.labelMedium,
              color = TextSecondary,
            )
            Slider(
              value = project.subtitleStyle.fontSizeSp,
              onValueChange = { sz -> onSubtitleStyleChange { it.copy(fontSizeSp = sz) } },
              valueRange = 18f..38f,
            )
          }

          else -> {
            Text("Background Music Bed", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              MusicMood.entries.forEach { mood ->
                FilterChip(
                  selected = project.audioMix.musicMood == mood,
                  onClick = { onAudioMixChange { it.copy(musicMood = mood) } },
                  label = { Text(mood.label) },
                )
              }
            }

            OutlinedButton(
              onClick = onPickAudio,
              modifier = Modifier.fillMaxWidth().testTag("upload_custom_bgm_button"),
            ) {
              Icon(Icons.Rounded.AudioFile, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(8.dp))
              Text(
                project.audioMix.customBgmName?.let { "Custom: $it" } ?: "Upload Your Own Audio (MP3/WAV/M4A)",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = EmeraldReady, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Auto-Duck Music Under Voice", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
              }
              Switch(
                checked = project.audioMix.autoDucking,
                onCheckedChange = { d -> onAudioMixChange { it.copy(autoDucking = d) } },
                modifier = Modifier.testTag("auto_ducking_toggle"),
              )
            }

            Text("Voiceover Level: ${(project.audioMix.voiceoverVolume * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Slider(
              value = project.audioMix.voiceoverVolume,
              onValueChange = { v -> onAudioMixChange { it.copy(voiceoverVolume = v) } },
              valueRange = 0f..1.4f,
            )

            Text("Music Level: ${(project.audioMix.bgmVolume * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Slider(
              value = project.audioMix.bgmVolume,
              onValueChange = { v -> onAudioMixChange { it.copy(bgmVolume = v) } },
              valueRange = 0f..1f,
            )

            Text("SFX Level: ${(project.audioMix.sfxVolume * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Slider(
              value = project.audioMix.sfxVolume,
              onValueChange = { v -> onAudioMixChange { it.copy(sfxVolume = v) } },
              valueRange = 0f..1.2f,
            )
          }
        }
      }
    }
  }
}

private fun formatTimecode(ms: Long): String {
  val totalSec = (ms / 1000f)
  val mins = (totalSec / 60).toInt()
  val secs = totalSec % 60f
  return "%02d:%04.1f".format(mins, secs)
}
