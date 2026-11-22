package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MovieCreation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ProductionStep
import com.example.ui.theme.CinemaAmber
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldReady
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioSurfaceHighest
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun StudioTopHeader(
  projectTitle: String,
  isSampleMode: Boolean,
  onNewProject: () -> Unit,
  onShowApiInfo: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = StudioSurface,
    tonalElevation = 2.dp,
    modifier = modifier.fillMaxWidth(),
  ) {
    Column {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f),
        ) {
          Box(
            modifier =
              Modifier.size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                  Brush.linearGradient(listOf(CinemaAmber, ElectricCyan))
                ),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Rounded.MovieCreation,
              contentDescription = "Video Agent Studio",
              tint = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.size(20.dp),
            )
          }
          Spacer(Modifier.width(10.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "UNIVERSAL VIDEO AGENT",
              style = MaterialTheme.typography.labelMedium,
              color = CinemaAmber,
              maxLines = 1,
            )
            Text(
              text = projectTitle.ifBlank { "Untitled Documentary" },
              style = MaterialTheme.typography.titleMedium,
              color = TextPrimary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Surface(
            color = if (isSampleMode) StudioSurfaceHighest else MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(999.dp),
            border = BorderStroke(1.dp, if (isSampleMode) CinemaAmber.copy(alpha = 0.5f) else ElectricCyan),
            modifier =
              Modifier.minimumInteractiveComponentSize()
                .clip(RoundedCornerShape(999.dp))
                .clickable(onClick = onShowApiInfo)
                .testTag("api_status_badge"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = if (isSampleMode) Icons.Rounded.Info else Icons.Rounded.AutoAwesome,
                contentDescription = "AI & Pipeline Status",
                tint = if (isSampleMode) CinemaAmber else ElectricCyan,
                modifier = Modifier.size(14.dp),
              )
              Spacer(Modifier.width(4.dp))
              Text(
                text = if (isSampleMode) "Sample AI" else "Gemini AI",
                style = MaterialTheme.typography.labelSmall,
                color = TextPrimary,
              )
            }
          }

          Surface(
            color = StudioSurfaceElevated,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, HairlineBorder),
            modifier =
              Modifier.minimumInteractiveComponentSize()
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onNewProject)
                .testTag("new_project_button"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "New Project",
                tint = TextPrimary,
                modifier = Modifier.size(16.dp),
              )
              Spacer(Modifier.width(4.dp))
              Text("New", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
            }
          }
        }
      }
    }
  }
}

@Composable
fun SampleAiNoticeBanner(
  onLearnMore: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = StudioSurfaceElevated,
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(1.dp, CinemaAmber.copy(alpha = 0.45f)),
    modifier = modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = Icons.Rounded.CloudOff,
        contentDescription = "Sample AI mode active",
        tint = CinemaAmber,
        modifier = Modifier.size(18.dp),
      )
      Spacer(Modifier.width(10.dp))
      Text(
        text = "Sample AI Mode: Firebase was declined, so script/B-roll generation uses structured local templates. Voiceover TTS, audio ducking & 1080p MP4 export run live on-device.",
        style = MaterialTheme.typography.bodySmall,
        color = TextSecondary,
        modifier = Modifier.weight(1f),
      )
      TextButton(
        onClick = onLearnMore,
        modifier = Modifier.testTag("sample_ai_details_button"),
      ) {
        Text("APIs", color = CinemaAmber, style = MaterialTheme.typography.labelLarge)
      }
    }
  }
}

@Composable
fun ProductionStepperBar(
  activeStep: ProductionStep,
  sceneCount: Int,
  hasExported: Boolean,
  onSelectStep: (ProductionStep) -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = StudioSurface,
    border = BorderStroke(0.5.dp, HairlineBorder),
    modifier = modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      ProductionStep.entries.forEach { step ->
        val selected = step == activeStep
        val isComplete =
          when (step) {
            ProductionStep.CREATE -> sceneCount > 0
            ProductionStep.SCENES -> sceneCount > 0
            ProductionStep.EDITOR -> hasExported
            ProductionStep.EXPORT -> hasExported
          }
        Surface(
          color =
            when {
              selected -> CinemaAmber.copy(alpha = 0.18f)
              else -> StudioSurfaceElevated
            },
          shape = RoundedCornerShape(10.dp),
          border =
            BorderStroke(
              1.dp,
              when {
                selected -> CinemaAmber
                isComplete -> EmeraldReady.copy(alpha = 0.5f)
                else -> HairlineBorder
              },
            ),
          modifier =
            Modifier.weight(1f)
              .minimumInteractiveComponentSize()
              .clip(RoundedCornerShape(10.dp))
              .clickable { onSelectStep(step) }
              .testTag("step_tab_${step.name.lowercase()}"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
          ) {
            Box(
              modifier =
                Modifier.size(18.dp)
                  .clip(CircleShape)
                  .background(
                    when {
                      selected -> CinemaAmber
                      isComplete -> EmeraldReady
                      else -> StudioSurfaceHighest
                    }
                  ),
              contentAlignment = Alignment.Center,
            ) {
              if (isComplete && !selected) {
                Icon(
                  imageVector = Icons.Rounded.CheckCircle,
                  contentDescription = null,
                  tint = StudioSurface,
                  modifier = Modifier.size(14.dp),
                )
              } else {
                Text(
                  text = "${step.index + 1}",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = if (selected) MaterialTheme.colorScheme.onPrimary else TextPrimary,
                )
              }
            }
            Spacer(Modifier.width(6.dp))
            Text(
              text = step.label,
              style = MaterialTheme.typography.labelSmall,
              color = if (selected) CinemaAmber else TextSecondary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    }
  }
}

@Composable
fun ApiTransparencyDialog(onDismiss: () -> Unit) {
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = StudioSurfaceElevated,
    title = {
      Text(
        "Production Pipeline & External APIs",
        style = MaterialTheme.typography.titleLarge,
        color = TextPrimary,
      )
    },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          "1. Active On-Device Engine (Working Now)",
          style = MaterialTheme.typography.titleSmall,
          color = EmeraldReady,
        )
        Text(
          "• Multi-track Documentary Editor & Ken Burns Camera Motion\n" +
            "• Color Grading, Motion Graphics Lower-Thirds & Subtitles\n" +
            "• Android TextToSpeech WAV Voiceover Synthesis (EN / HI / Hinglish)\n" +
            "• Royalty-Free Procedural BGM/SFX + Custom Audio Upload & Ducking\n" +
            "• Hardware H.264 + AAC 1080p/720p MP4 Export & Room History",
          style = MaterialTheme.typography.bodySmall,
          color = TextSecondary,
        )

        Spacer(Modifier.height(4.dp))
        Text(
          "2. External / Paid APIs Required for Cloud AI",
          style = MaterialTheme.typography.titleSmall,
          color = CinemaAmber,
        )
        Text(
          "• Gemini LLM Script & Scene Planner: Connects via Firebase AI Logic (gemini-3.8-flash). Currently in Sample AI Mode because Firebase setup was declined.\n" +
            "• AI Image / Video Generation: Requires Firebase Blaze + Nano Banana (gemini-nano-banana-2.1) or external video generation APIs (Veo / Runway / Kling). Local mode uses stylized documentary stills with Ken Burns animation.\n" +
            "• Cloud Neural TTS (Optional): ElevenLabs or Google Cloud Neural2 voices require a server-side API key if you want studio voices beyond Android's built-in TTS.",
          style = MaterialTheme.typography.bodySmall,
          color = TextSecondary,
        )
      }
    },
    confirmButton = {
      Button(
        onClick = onDismiss,
        colors = ButtonDefaults.buttonColors(containerColor = CinemaAmber),
        modifier = Modifier.testTag("close_api_dialog_button"),
      ) {
        Text("Got It")
      }
    },
  )
}
