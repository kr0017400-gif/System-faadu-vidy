package com.example.ui.screens

import android.content.Intent
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import com.example.data.model.AspectRatioMode
import com.example.data.model.DocumentaryProject
import com.example.data.model.ExportResolution
import com.example.ui.StudioUiStatus
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
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ExportHistoryScreen(
  project: DocumentaryProject,
  history: List<DocumentaryProject>,
  uiStatus: StudioUiStatus,
  onSelectResolution: (ExportResolution) -> Unit,
  onRenderMp4: () -> Unit,
  onCancelRender: () -> Unit,
  onSaveToMovies: () -> Unit,
  onLoadProject: (String) -> Unit,
  onDeleteProject: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val scrollState = rememberScrollState()

  Column(
    modifier = modifier.fillMaxSize().verticalScroll(scrollState).padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    // 1. Final MP4 Render Engine Card
    Surface(
      color = StudioSurface,
      shape = RoundedCornerShape(16.dp),
      border = BorderStroke(1.dp, HairlineBorder),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Rounded.Movie, contentDescription = null, tint = CinemaAmber, modifier = Modifier.size(22.dp))
          Spacer(Modifier.width(8.dp))
          Text("MP4 Video Render Engine", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        }

        Text(
          "Renders H.264 video + 44.1kHz AAC audio with Ken Burns motion, color grading, transitions, motion graphics, auto-ducked BGM, SFX & embedded subtitles.",
          style = MaterialTheme.typography.bodySmall,
          color = TextSecondary,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          ExportResolution.entries.forEach { res ->
            FilterChip(
              selected = project.exportedResolution == res,
              onClick = { onSelectResolution(res) },
              label = { Text(res.label) },
              modifier = Modifier.testTag("export_res_${res.name.lowercase()}"),
            )
          }
        }

        Surface(
          color = StudioSurfaceElevated,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              "Format: ${project.aspectRatio.label} MP4",
              style = MaterialTheme.typography.labelMedium,
              color = TextSecondary,
            )
            Text(
              "Scenes: ${project.scenes.size} · ${"%.1f".format(project.totalDurationMs / 1000f)}s",
              style = MaterialTheme.typography.labelMedium,
              color = ElectricCyan,
            )
          }
        }

        if (uiStatus.isExporting && uiStatus.exportProgress != null) {
          LinearProgressIndicator(
            progress = { uiStatus.exportProgress.progress },
            color = CinemaAmber,
            trackColor = StudioSurfaceHighest,
            modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              uiStatus.exportProgress.stage,
              style = MaterialTheme.typography.labelMedium,
              color = CinemaAmber,
              modifier = Modifier.weight(1f),
            )
            OutlinedButton(onClick = onCancelRender) {
              Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(4.dp))
              Text("Cancel")
            }
          }
        } else {
          Button(
            onClick = onRenderMp4,
            enabled = project.scenes.isNotEmpty(),
            colors = ButtonDefaults.buttonColors(containerColor = CinemaAmber),
            contentPadding = PaddingValues(vertical = 14.dp),
            modifier = Modifier.fillMaxWidth().testTag("render_mp4_button"),
          ) {
            Icon(Icons.Rounded.Movie, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Render Final MP4 Video")
          }
        }
      }
    }

    // 2. Rendered MP4 Player & Download / Share
    val exportedFile = project.exportedVideoPath?.let { File(it) }?.takeIf { it.exists() && it.length() > 0L }
    if (exportedFile != null) {
      Surface(
        color = StudioSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, EmeraldReady.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = EmeraldReady, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Column {
              Text("Final Documentary Ready", style = MaterialTheme.typography.titleMedium, color = EmeraldReady)
              Text(
                "${exportedFile.name} · ${(exportedFile.length() / 1024)} KB",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
              )
            }
          }

          val ratio = if (project.aspectRatio == AspectRatioMode.LANDSCAPE_16_9) 16f / 9f else 9f / 16f
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(ObsidianBg),
            contentAlignment = Alignment.Center,
          ) {
            AndroidView(
              factory = { ctx ->
                VideoView(ctx).apply {
                  val controller = MediaController(ctx)
                  controller.setAnchorView(this)
                  setMediaController(controller)
                  setVideoPath(exportedFile.absolutePath)
                  setOnPreparedListener { mp ->
                    mp.isLooping = false
                    seekTo(100)
                  }
                }
              },
              update = { view ->
                if (view.tag != exportedFile.absolutePath) {
                  view.tag = exportedFile.absolutePath
                  view.setVideoPath(exportedFile.absolutePath)
                }
              },
              modifier =
                Modifier.then(
                  if (project.aspectRatio == AspectRatioMode.PORTRAIT_9_16) {
                    Modifier.heightIn(max = 320.dp).aspectRatio(ratio)
                  } else {
                    Modifier.fillMaxWidth().aspectRatio(ratio)
                  }
                )
                  .testTag("exported_mp4_player"),
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Button(
              onClick = onSaveToMovies,
              colors = ButtonDefaults.buttonColors(containerColor = EmeraldReady),
              modifier = Modifier.weight(1f).testTag("download_mp4_button"),
            ) {
              Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(6.dp))
              Text("Save to Movies")
            }

            OutlinedButton(
              onClick = {
                val uri =
                  FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    exportedFile,
                  )
                val shareIntent =
                  Intent(Intent.ACTION_SEND).apply {
                    type = "video/mp4"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                  }
                context.startActivity(Intent.createChooser(shareIntent, "Share Documentary MP4"))
              },
              border = BorderStroke(1.dp, ElectricCyan),
              colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricCyan),
              modifier = Modifier.weight(1f).testTag("share_mp4_button"),
            ) {
              Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(6.dp))
              Text("Share MP4")
            }
          }
        }
      }
    }

    // 3. Local Project History (Room Database)
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
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Rounded.History, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(20.dp))
          Spacer(Modifier.width(8.dp))
          Text("Saved Project History (${history.size})", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        }

        if (history.isEmpty()) {
          Text(
            "No saved projects yet. Generate a script or plan scenes to automatically save progress here.",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
          )
        } else {
          val dateFmt = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
          history.forEach { item ->
            val isCurrent = item.id == project.id
            Surface(
              color = if (isCurrent) CinemaAmber.copy(alpha = 0.12f) else StudioSurfaceElevated,
              shape = RoundedCornerShape(12.dp),
              border = BorderStroke(1.dp, if (isCurrent) CinemaAmber else HairlineBorder),
              modifier =
                Modifier.fillMaxWidth()
                  .clip(RoundedCornerShape(12.dp))
                  .clickable { onLoadProject(item.id) }
                  .testTag("history_item_${item.id}"),
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                  Text(
                    item.title.ifBlank { item.topic },
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(
                    "${item.category.label} · ${item.aspectRatio.label} · ${item.scenes.size} scenes · ${dateFmt.format(Date(item.updatedAtMillis))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                  )
                  if (item.exportedVideoPath != null) {
                    Text("MP4 Exported", style = MaterialTheme.typography.labelSmall, color = EmeraldReady)
                  }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                  IconButton(onClick = { onLoadProject(item.id) }) {
                    Icon(Icons.Rounded.FolderOpen, contentDescription = "Open Project", tint = ElectricCyan)
                  }
                  IconButton(onClick = { onDeleteProject(item.id) }) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Delete Project", tint = CrimsonRecord)
                  }
                }
              }
            }
          }
        }
      }
    }

    Spacer(Modifier.height(16.dp))
  }
}
