package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documentary_projects")
data class DocumentaryEntity(
  @PrimaryKey val id: String,
  val title: String,
  val topic: String,
  val categoryName: String,
  val targetDurationName: String,
  val aspectRatioName: String,
  val languageName: String,
  val visualStyleName: String,
  val colorGradeName: String,
  val scriptText: String,
  val isScriptApproved: Boolean,
  val isSampleAiGenerated: Boolean,
  val scenesJson: String,
  val voiceId: String,
  val voiceName: String,
  val voiceSpeed: Float,
  val voicePitch: Float,
  val musicMoodName: String,
  val customBgmPath: String?,
  val customBgmName: String?,
  val bgmVolume: Float,
  val voiceoverVolume: Float,
  val sfxVolume: Float,
  val autoDucking: Boolean,
  val subtitlesEnabled: Boolean,
  val subtitlePositionName: String,
  val subtitleFontName: String,
  val subtitleFontSizeSp: Float,
  val subtitleHighlightKeywords: Boolean,
  val subtitleAnimatedCaptions: Boolean,
  val exportedVideoPath: String?,
  val exportedResolutionName: String,
  val updatedAtMillis: Long,
)
