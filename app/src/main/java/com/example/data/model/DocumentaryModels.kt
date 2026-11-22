package com.example.data.model

import java.util.UUID

enum class ContentCategory(val label: String, val tag: String) {
  FACTS_MYSTERIES("Facts & Mysteries", "MYSTERY"),
  SCIENCE_TECH("Science & Technology", "SCIENCE"),
  HISTORY_GEOGRAPHY("History & Geography", "HISTORY"),
  GAMING_FREE_FIRE("Gaming & Free Fire", "GAMING"),
  MOTIVATION_STORY("Motivation & Storytelling", "STORY"),
  EDUCATIONAL("Educational Videos", "EDU"),
  GENERAL_YOUTUBE("General YouTube", "YOUTUBE"),
}

enum class TargetDuration(val label: String, val seconds: Int, val defaultSceneCount: Int) {
  SEC_30("30s", 30, 4),
  SEC_60("60s", 60, 6),
  MIN_3("3 min", 180, 9),
  MIN_5("5 min", 300, 12),
  MIN_10("10 min", 600, 16),
}

enum class AspectRatioMode(
  val label: String,
  val width1080: Int,
  val height1080: Int,
  val width720: Int,
  val height720: Int,
) {
  LANDSCAPE_16_9("16:9", 1920, 1080, 1280, 720),
  PORTRAIT_9_16("9:16", 1080, 1920, 720, 1280),
}

enum class NarrationLanguage(val label: String, val localeTag: String) {
  ENGLISH("English", "en-US"),
  HINDI("Hindi", "hi-IN"),
  HINGLISH("Hinglish", "en-IN"),
}

enum class VisualStyle(val label: String, val promptDirective: String) {
  CINEMATIC_DOC("Cinematic Doc", "35mm anamorphic documentary film, shallow depth of field, atmospheric haze"),
  NOIR_MYSTERY("Noir Mystery", "Chiaroscuro low-key shadows, rain-slicked reflections, suspenseful lighting"),
  SCI_FI_TECH("Futuristic Tech", "Clean laboratory optics, volumetric cyan rim light, precision macro detail"),
  ARCHIVAL_HISTORY("Archival Heritage", "Golden hour monumental ruins, natural grain, museum-grade historical realism"),
  ESPORTS_GAMING("Gaming Cinematics", "Unreal-style tactical skyline, flare smoke, high-contrast esports energy"),
  EPIC_MOTIVATION("Motivational Epic", "Dramatic sunrise silhouettes, bold lens flare, inspiring aerial scale"),
}

enum class CameraMovement(val label: String) {
  SLOW_ZOOM_IN("Slow Zoom In"),
  SLOW_ZOOM_OUT("Slow Zoom Out"),
  PAN_LEFT("Pan Left"),
  PAN_RIGHT("Pan Right"),
  TRACKING_DRIFT("Tracking Drift"),
}

enum class ShotType(val label: String) {
  WIDE_ESTABLISHING("Wide Establishing"),
  MEDIUM_SUBJECT("Medium Shot"),
  EXTREME_CLOSE_UP("Macro Close-Up"),
  AERIAL_DRONE("Aerial Drone"),
  LOW_ANGLE_HERO("Low-Angle Hero"),
}

enum class SceneTransition(val label: String) {
  CROSSFADE("Crossfade"),
  DIP_TO_BLACK("Dip to Black"),
  SLIDE_LEFT("Whip Slide"),
  WHIP_ZOOM("Zoom Cut"),
}

enum class ColorGrade(val label: String) {
  TEAL_AND_ORANGE("Teal & Orange"),
  NOIR_CONTRAST("Noir Contrast"),
  WARM_ARCHIVAL("Warm Archival"),
  CYBER_NEON("Cyber Cyan"),
  NATURAL_CINEMA("Natural Cinema"),
}

enum class MusicMood(val label: String, val description: String) {
  CINEMATIC_EPIC("Cinematic Pulse", "Deep sub-bass & sweeping documentary synth pad"),
  SUSPENSE_PULSE("Suspense Tension", "Dark rhythmic pulse for mysteries & investigation"),
  EMOTIONAL_STRINGS("Emotional Warmth", "Soft harmonic drone for storytelling & motivation"),
  DOC_AMBIENCE("Doc Ambience", "Clean atmospheric bed for educational narration"),
  CUSTOM_USER_AUDIO("Custom Audio", "User-imported background music file"),
}

enum class SfxType(val label: String) {
  NONE("None"),
  DEEP_IMPACT("Deep Impact"),
  CINEMATIC_WHOOSH("Whoosh"),
  RISER_TENSION("Tension Riser"),
  TECH_GLITCH("Tech Pulse"),
}

enum class SubtitlePosition(val label: String) {
  BOTTOM("Bottom"),
  CENTER("Center"),
  TOP("Top"),
}

enum class SubtitleFontStyle(val label: String) {
  SPACE_GROTESK("Space Grotesk"),
  PLAYFAIR_SERIF("Playfair Serif"),
  JETBRAINS_MONO("JetBrains Mono"),
  PLUS_JAKARTA("Jakarta Sans"),
}

enum class ExportResolution(val label: String) {
  FHD_1080P("1080p Full HD"),
  HD_720P("720p Fast HD"),
}

enum class ProductionStep(val index: Int, val label: String) {
  CREATE(0, "Brief & Script"),
  SCENES(1, "Scenes & Assets"),
  EDITOR(2, "Timeline & Mix"),
  EXPORT(3, "Render & History"),
}

data class VoiceOption(
  val id: String,
  val displayName: String,
  val localeTag: String,
  val isSystemDefault: Boolean = false,
)

data class SubtitleStyleConfig(
  val enabled: Boolean = true,
  val position: SubtitlePosition = SubtitlePosition.BOTTOM,
  val fontStyle: SubtitleFontStyle = SubtitleFontStyle.SPACE_GROTESK,
  val fontSizeSp: Float = 26f,
  val highlightKeywords: Boolean = true,
  val animatedCaptions: Boolean = true,
)

data class AudioMixConfig(
  val musicMood: MusicMood = MusicMood.CINEMATIC_EPIC,
  val customBgmPath: String? = null,
  val customBgmName: String? = null,
  val bgmVolume: Float = 0.35f,
  val voiceoverVolume: Float = 1.0f,
  val sfxVolume: Float = 0.65f,
  val autoDucking: Boolean = true,
)

data class VoiceSettings(
  val voiceId: String = "default",
  val voiceName: String = "System Narrator",
  val speed: Float = 1.0f,
  val pitch: Float = 1.0f,
)

data class SubtitleSegment(
  val text: String,
  val startMs: Long,
  val endMs: Long,
  val highlightWord: String? = null,
)

data class DocumentaryScene(
  val id: String = UUID.randomUUID().toString(),
  val sceneNumber: Int,
  val durationSec: Float,
  val narration: String,
  val visualPrompt: String,
  val cameraMovement: CameraMovement = CameraMovement.SLOW_ZOOM_IN,
  val shotType: ShotType = ShotType.WIDE_ESTABLISHING,
  val lightingAtmosphere: String = "Volumetric rim light with soft atmospheric haze",
  val motionGraphicsInstruction: String = "Animated lower-third kinetic callout & subtle letterbox",
  val onScreenText: String = "",
  val sfxType: SfxType = SfxType.CINEMATIC_WHOOSH,
  val musicCue: String = "Hold bed & duck under narration",
  val transition: SceneTransition = SceneTransition.CROSSFADE,
  val visualPath: String? = null,
  val voiceoverPath: String? = null,
  val voiceoverDurationMs: Long = 0L,
  val visualError: String? = null,
  val voiceError: String? = null,
  val isGeneratingVisual: Boolean = false,
  val isGeneratingVoice: Boolean = false,
  val isUsingSampleVisual: Boolean = true,
) {
  val effectiveDurationMs: Long
    get() = maxOf((durationSec * 1000f).toLong(), voiceoverDurationMs + 500L).coerceAtLeast(2500L)
}

data class DocumentaryProject(
  val id: String = UUID.randomUUID().toString(),
  val title: String = "Untitled Documentary",
  val topic: String = "",
  val category: ContentCategory = ContentCategory.FACTS_MYSTERIES,
  val targetDuration: TargetDuration = TargetDuration.SEC_60,
  val aspectRatio: AspectRatioMode = AspectRatioMode.LANDSCAPE_16_9,
  val language: NarrationLanguage = NarrationLanguage.ENGLISH,
  val visualStyle: VisualStyle = VisualStyle.CINEMATIC_DOC,
  val colorGrade: ColorGrade = ColorGrade.TEAL_AND_ORANGE,
  val scriptText: String = "",
  val isScriptApproved: Boolean = false,
  val isSampleAiGenerated: Boolean = true,
  val scenes: List<DocumentaryScene> = emptyList(),
  val voiceSettings: VoiceSettings = VoiceSettings(),
  val audioMix: AudioMixConfig = AudioMixConfig(),
  val subtitleStyle: SubtitleStyleConfig = SubtitleStyleConfig(),
  val exportedVideoPath: String? = null,
  val exportedResolution: ExportResolution = ExportResolution.FHD_1080P,
  val updatedAtMillis: Long = System.currentTimeMillis(),
) {
  val totalDurationMs: Long
    get() = scenes.sumOf { it.effectiveDurationMs }
}

data class GeneratedScriptResult(
  val title: String,
  val hook: String,
  val script: String,
  val isSampleMode: Boolean,
)
