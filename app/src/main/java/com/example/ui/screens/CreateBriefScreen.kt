package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AspectRatioMode
import com.example.data.model.ContentCategory
import com.example.data.model.DocumentaryProject
import com.example.data.model.NarrationLanguage
import com.example.data.model.TargetDuration
import com.example.data.model.VisualStyle
import com.example.data.model.VoiceOption
import com.example.data.model.VoiceSettings
import com.example.ui.StudioUiStatus
import com.example.ui.components.SampleAiNoticeBanner
import com.example.ui.theme.CinemaAmber
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldReady
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioSurfaceHighest
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CreateBriefScreen(
  project: DocumentaryProject,
  uiStatus: StudioUiStatus,
  availableVoices: List<VoiceOption>,
  onTopicChange: (String) -> Unit,
  onCategorySelect: (ContentCategory) -> Unit,
  onDurationSelect: (TargetDuration) -> Unit,
  onRatioSelect: (AspectRatioMode) -> Unit,
  onLanguageSelect: (NarrationLanguage) -> Unit,
  onVisualStyleSelect: (VisualStyle) -> Unit,
  onVoiceSettingsChange: ((VoiceSettings) -> VoiceSettings) -> Unit,
  onScriptChange: (String) -> Unit,
  onGenerateScript: () -> Unit,
  onApproveAndPlanScenes: () -> Unit,
  onShowApiInfo: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val scrollState = rememberScrollState()
  val quickTopics = remember {
    listOf(
      "The Bermuda Triangle Mystery" to ContentCategory.FACTS_MYSTERIES,
      "Rise of Free Fire Esports" to ContentCategory.GAMING_FREE_FIRE,
      "How Quantum Computers Break Reality" to ContentCategory.SCIENCE_TECH,
      "Lost Underwater City of Dwarka" to ContentCategory.HISTORY_GEOGRAPHY,
      "Why 1% Daily Discipline Wins" to ContentCategory.MOTIVATION_STORY,
    )
  }

  Column(
    modifier = modifier.fillMaxSize().verticalScroll(scrollState).padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    // Hero Studio Card
    Surface(
      shape = RoundedCornerShape(18.dp),
      border = BorderStroke(1.dp, HairlineBorder),
      modifier = Modifier.fillMaxWidth().height(148.dp),
    ) {
      Box(modifier = Modifier.fillMaxSize()) {
        Image(
          painter = painterResource(id = R.drawable.img_studio_hero_1795268398221),
          contentDescription = "Documentary Editing Studio",
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )
        Box(
          modifier =
            Modifier.fillMaxSize()
              .background(
                Brush.horizontalGradient(
                  listOf(
                    ObsidianBg.copy(alpha = 0.94f),
                    ObsidianBg.copy(alpha = 0.72f),
                    ObsidianBg.copy(alpha = 0.35f),
                  )
                )
              )
        )
        Column(
          modifier = Modifier.align(Alignment.CenterStart).padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text(
            text = "AI DOCUMENTARY DIRECTOR",
            style = MaterialTheme.typography.labelMedium,
            color = CinemaAmber,
          )
          Text(
            text = "Turn any topic into a cinematic film",
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary,
          )
          Text(
            text = "Script → Scene Plan → B-Roll + TTS Voiceover → Timeline → 1080p MP4",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
          )
        }
      }
    }

    SampleAiNoticeBanner(onLearnMore = onShowApiInfo)

    // Topic & Category Card
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
        Text("1. Topic & Content Niche", style = MaterialTheme.typography.titleLarge, color = TextPrimary)

        OutlinedTextField(
          value = project.topic,
          onValueChange = onTopicChange,
          label = { Text("Documentary Topic or Idea") },
          placeholder = { Text("e.g., How Black Holes Warp Time, Free Fire World Series...") },
          singleLine = false,
          minLines = 2,
          maxLines = 4,
          colors =
            OutlinedTextFieldDefaults.colors(
              focusedBorderColor = CinemaAmber,
              unfocusedBorderColor = HairlineBorder,
              focusedLabelColor = CinemaAmber,
            ),
          modifier = Modifier.fillMaxWidth().testTag("topic_input"),
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          items(quickTopics) { (label, cat) ->
            Surface(
              color = StudioSurfaceElevated,
              shape = RoundedCornerShape(999.dp),
              border = BorderStroke(1.dp, HairlineBorder),
              modifier =
                Modifier.clip(RoundedCornerShape(999.dp))
                  .clickable {
                    onTopicChange(label)
                    onCategorySelect(cat)
                  }
                  .testTag("quick_topic_chip"),
            ) {
              Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = ElectricCyan,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
              )
            }
          }
        }

        Text("Category", style = MaterialTheme.typography.titleSmall, color = TextSecondary)
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          ContentCategory.entries.forEach { cat ->
            val selected = project.category == cat
            FilterChip(
              selected = selected,
              onClick = { onCategorySelect(cat) },
              label = { Text(cat.label) },
              colors =
                FilterChipDefaults.filterChipColors(
                  selectedContainerColor = CinemaAmber.copy(alpha = 0.2f),
                  selectedLabelColor = CinemaAmber,
                ),
              modifier = Modifier.testTag("category_chip_${cat.name.lowercase()}"),
            )
          }
        }
      }
    }

    // Format, Duration, Language & Style
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
        Text("2. Format, Language & Visual Style", style = MaterialTheme.typography.titleLarge, color = TextPrimary)

        Text("Target Duration", style = MaterialTheme.typography.titleSmall, color = TextSecondary)
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          TargetDuration.entries.forEach { dur ->
            FilterChip(
              selected = project.targetDuration == dur,
              onClick = { onDurationSelect(dur) },
              label = { Text(dur.label) },
              colors =
                FilterChipDefaults.filterChipColors(
                  selectedContainerColor = ElectricCyan.copy(alpha = 0.18f),
                  selectedLabelColor = ElectricCyan,
                ),
              modifier = Modifier.testTag("duration_chip_${dur.name.lowercase()}"),
            )
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Video Ratio", style = MaterialTheme.typography.titleSmall, color = TextSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              AspectRatioMode.entries.forEach { ratio ->
                FilterChip(
                  selected = project.aspectRatio == ratio,
                  onClick = { onRatioSelect(ratio) },
                  label = { Text(ratio.label) },
                  modifier = Modifier.testTag("ratio_chip_${ratio.name.lowercase()}"),
                )
              }
            }
          }

          Column(modifier = Modifier.weight(1.3f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Language", style = MaterialTheme.typography.titleSmall, color = TextSecondary)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              items(NarrationLanguage.entries) { lang ->
                FilterChip(
                  selected = project.language == lang,
                  onClick = { onLanguageSelect(lang) },
                  label = { Text(lang.label) },
                  modifier = Modifier.testTag("lang_chip_${lang.name.lowercase()}"),
                )
              }
            }
          }
        }

        Text("Visual Direction", style = MaterialTheme.typography.titleSmall, color = TextSecondary)
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          VisualStyle.entries.forEach { style ->
            FilterChip(
              selected = project.visualStyle == style,
              onClick = { onVisualStyleSelect(style) },
              label = { Text(style.label) },
              modifier = Modifier.testTag("style_chip_${style.name.lowercase()}"),
            )
          }
        }
      }
    }

    // Voiceover Configuration
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
          Icon(Icons.Rounded.RecordVoiceOver, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(20.dp))
          Spacer(Modifier.width(8.dp))
          Text("3. Narrator Voice & Pacing (Android TTS)", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        }

        var voiceMenuExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
          expanded = voiceMenuExpanded,
          onExpandedChange = { voiceMenuExpanded = it },
        ) {
          OutlinedTextField(
            value = project.voiceSettings.voiceName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Voice Profile") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = voiceMenuExpanded) },
            modifier =
              Modifier.fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .testTag("voice_dropdown"),
          )
          ExposedDropdownMenu(
            expanded = voiceMenuExpanded,
            onDismissRequest = { voiceMenuExpanded = false },
          ) {
            availableVoices.forEach { option ->
              DropdownMenuItem(
                text = { Text("${option.displayName} [${option.localeTag}]") },
                onClick = {
                  onVoiceSettingsChange {
                    it.copy(voiceId = option.id, voiceName = option.displayName)
                  }
                  voiceMenuExpanded = false
                },
              )
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Rounded.Speed, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
              Spacer(Modifier.width(4.dp))
              Text(
                "Speed: ${"%.2f".format(project.voiceSettings.speed)}x",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
              )
            }
            Slider(
              value = project.voiceSettings.speed,
              onValueChange = { v -> onVoiceSettingsChange { it.copy(speed = v) } },
              valueRange = 0.75f..1.45f,
              colors = SliderDefaults.colors(thumbColor = CinemaAmber, activeTrackColor = CinemaAmber),
              modifier = Modifier.testTag("voice_speed_slider"),
            )
          }

          Column(modifier = Modifier.weight(1f)) {
            Text(
              "Pitch: ${"%.2f".format(project.voiceSettings.pitch)}x",
              style = MaterialTheme.typography.labelMedium,
              color = TextSecondary,
            )
            Slider(
              value = project.voiceSettings.pitch,
              onValueChange = { v -> onVoiceSettingsChange { it.copy(pitch = v) } },
              valueRange = 0.75f..1.35f,
              colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = ElectricCyan),
              modifier = Modifier.testTag("voice_pitch_slider"),
            )
          }
        }
      }
    }

    // Generate Script + Script Editor
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
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("4. Documentary Script", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            Text(
              "Includes 0–5s hook, curiosity arc & scene breaks. Edit freely before production.",
              style = MaterialTheme.typography.bodySmall,
              color = TextSecondary,
            )
          }
          if (project.isScriptApproved) {
            Surface(
              color = EmeraldReady.copy(alpha = 0.18f),
              shape = RoundedCornerShape(8.dp),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = EmeraldReady, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Approved", style = MaterialTheme.typography.labelSmall, color = EmeraldReady)
              }
            }
          }
        }

        Button(
          onClick = onGenerateScript,
          enabled = !uiStatus.isGeneratingScript,
          colors = ButtonDefaults.buttonColors(containerColor = CinemaAmber),
          contentPadding = PaddingValues(vertical = 14.dp, horizontal = 18.dp),
          modifier = Modifier.fillMaxWidth().testTag("generate_script_button"),
        ) {
          if (uiStatus.isGeneratingScript) {
            CircularProgressIndicator(
              modifier = Modifier.size(18.dp),
              color = MaterialTheme.colorScheme.onPrimary,
              strokeWidth = 2.dp,
            )
            Spacer(Modifier.width(10.dp))
            Text("Writing Documentary Script...")
          } else {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (project.scriptText.isBlank()) "Generate Script (Sample AI)" else "Regenerate Script")
          }
        }

        OutlinedTextField(
          value = project.scriptText,
          onValueChange = onScriptChange,
          label = { Text("Editable Narration Script ([SCENE 1], [SCENE 2]...)") },
          minLines = 7,
          maxLines = 16,
          colors =
            OutlinedTextFieldDefaults.colors(
              focusedBorderColor = ElectricCyan,
              unfocusedBorderColor = HairlineBorder,
            ),
          modifier = Modifier.fillMaxWidth().testTag("script_editor_input"),
        )

        OutlinedButton(
          onClick = onApproveAndPlanScenes,
          enabled = project.scriptText.isNotBlank() && !uiStatus.isPlanningScenes,
          border = BorderStroke(1.dp, ElectricCyan),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricCyan),
          contentPadding = PaddingValues(vertical = 14.dp, horizontal = 18.dp),
          modifier = Modifier.fillMaxWidth().testTag("approve_script_button"),
        ) {
          if (uiStatus.isPlanningScenes) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = ElectricCyan, strokeWidth = 2.dp)
            Spacer(Modifier.width(10.dp))
            Text("Planning Documentary Scenes...")
          } else {
            Text("Approve Script & Plan Scenes")
            Spacer(Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
          }
        }
      }
    }

    Spacer(Modifier.height(16.dp))
  }
}
