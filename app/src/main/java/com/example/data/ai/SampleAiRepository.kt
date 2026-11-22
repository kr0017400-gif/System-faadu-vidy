package com.example.data.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Shader
import com.example.R
import com.example.data.model.AspectRatioMode
import com.example.data.model.CameraMovement
import com.example.data.model.ContentCategory
import com.example.data.model.DocumentaryScene
import com.example.data.model.GeneratedScriptResult
import com.example.data.model.NarrationLanguage
import com.example.data.model.SceneTransition
import com.example.data.model.SfxType
import com.example.data.model.ShotType
import com.example.data.model.TargetDuration
import com.example.data.model.VisualStyle
import java.io.File
import java.io.FileOutputStream
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Sample / placeholder implementation of [AiRepository] used when Firebase AI Logic is not enabled.
 * Produces structured, topic-aware scripts, scene breakdowns, and stylized local B-roll frames
 * without calling external APIs or embedding API keys.
 */
class SampleAiRepository(private val context: Context) : AiRepository {
  override val isSampleMode: Boolean = true

  override suspend fun generateScript(
    topic: String,
    category: ContentCategory,
    duration: TargetDuration,
    language: NarrationLanguage,
    visualStyle: VisualStyle,
  ): Result<GeneratedScriptResult> =
    withContext(Dispatchers.Default) {
      val cleanTopic = topic.trim().ifBlank { "The Unseen Architecture of Time" }
      delay(350)
      val sceneTarget = duration.defaultSceneCount
      val sections = buildTopicSections(cleanTopic, category, language, sceneTarget)
      val hook = sections.first()
      val script =
        sections.mapIndexed { idx, paragraph ->
          "[SCENE ${idx + 1}]\n$paragraph"
        }.joinToString("\n\n")

      val title =
        when (language) {
          NarrationLanguage.HINDI -> "$cleanTopic: अनकही दास्तान"
          NarrationLanguage.HINGLISH -> "$cleanTopic: The Untold Story"
          NarrationLanguage.ENGLISH -> "$cleanTopic — Documentary"
        }

      Result.success(
        GeneratedScriptResult(
          title = title,
          hook = hook,
          script = script,
          isSampleMode = true,
        )
      )
    }

  override suspend fun planScenes(
    topic: String,
    script: String,
    category: ContentCategory,
    duration: TargetDuration,
    language: NarrationLanguage,
    visualStyle: VisualStyle,
  ): Result<List<DocumentaryScene>> =
    withContext(Dispatchers.Default) {
      delay(250)
      val cleanTopic = topic.trim().ifBlank { "Documentary Subject" }
      val rawBlocks =
        script
          .split(Regex("\\n\\s*\\n|\\[SCENE\\s*\\d+\\]"))
          .map { it.trim() }
          .filter { it.isNotBlank() }

      val blocks =
        if (rawBlocks.isNotEmpty()) rawBlocks
        else buildTopicSections(cleanTopic, category, language, duration.defaultSceneCount)

      val perSceneSec = (duration.seconds.toFloat() / blocks.size).coerceIn(4.5f, 45f)
      val cameras = CameraMovement.entries
      val shots = ShotType.entries
      val transitions = SceneTransition.entries
      val sfxOptions = listOf(SfxType.DEEP_IMPACT, SfxType.CINEMATIC_WHOOSH, SfxType.RISER_TENSION, SfxType.TECH_GLITCH)

      val scenes =
        blocks.mapIndexed { index, narration ->
          val sceneNum = index + 1
          val words = narration.split(Regex("\\s+")).filter { it.isNotBlank() }
          val headlineWords = words.take(4).joinToString(" ").uppercase()
          val onScreen =
            when {
              index == 0 -> cleanTopic.take(28).uppercase()
              index == blocks.lastIndex -> "THE FINAL VERDICT"
              else -> headlineWords.take(26)
            }
          val shot = shots[index % shots.size]
          val cam = cameras[index % cameras.size]
          val sfx =
            when {
              index == 0 -> SfxType.DEEP_IMPACT
              index == blocks.lastIndex -> SfxType.RISER_TENSION
              else -> sfxOptions[index % sfxOptions.size]
            }
          val transition = transitions[index % transitions.size]

          DocumentaryScene(
            sceneNumber = sceneNum,
            durationSec = perSceneSec,
            narration = narration,
            visualPrompt =
              "${visualStyle.promptDirective}. Subject: $cleanTopic — ${shot.label} depicting: ${narration.take(110)}",
            cameraMovement = cam,
            shotType = shot,
            lightingAtmosphere =
              when (category) {
                ContentCategory.FACTS_MYSTERIES -> "Low-key volumetric fog with amber shafts"
                ContentCategory.SCIENCE_TECH -> "Precision cyan rim-lighting and dark lab optics"
                ContentCategory.HISTORY_GEOGRAPHY -> "Golden-hour archival sunbeams and dust motes"
                ContentCategory.GAMING_FREE_FIRE -> "High-contrast neon flare smoke and tactical embers"
                ContentCategory.MOTIVATION_STORY -> "Dramatic sunrise silhouette and warm horizon glow"
                ContentCategory.EDUCATIONAL -> "Clean studio contrast with crisp focal depth"
                ContentCategory.GENERAL_YOUTUBE -> "Modern 35mm documentary color contrast"
              },
            motionGraphicsInstruction =
              if (index == 0) "Cinema letterbox reveal + bold kinetic title lower-third"
              else "Subtle HUD telemetry corner badge + lower-third context card",
            onScreenText = onScreen,
            sfxType = sfx,
            musicCue =
              when {
                index == 0 -> "Drop sub-bass hit & fade into bed"
                index == blocks.lastIndex -> "Swell main theme & resolve"
                else -> "Maintain ducked bed under narration"
              },
            transition = transition,
            isUsingSampleVisual = true,
          )
        }

      Result.success(scenes)
    }

  override suspend fun generateSceneVisual(
    scene: DocumentaryScene,
    aspectRatio: AspectRatioMode,
    visualStyle: VisualStyle,
    category: ContentCategory,
    outputDir: File,
    variationSeed: Int,
  ): Result<String> =
    withContext(Dispatchers.IO) {
      runCatching {
        outputDir.mkdirs()
        val width = if (aspectRatio == AspectRatioMode.LANDSCAPE_16_9) 1280 else 720
        val height = if (aspectRatio == AspectRatioMode.LANDSCAPE_16_9) 720 else 1280

        val drawableId =
          when (category) {
            ContentCategory.SCIENCE_TECH, ContentCategory.EDUCATIONAL ->
              R.drawable.img_broll_science_1795268417038
            ContentCategory.FACTS_MYSTERIES, ContentCategory.HISTORY_GEOGRAPHY ->
              R.drawable.img_broll_mystery_1795268416816
            ContentCategory.GAMING_FREE_FIRE ->
              R.drawable.img_broll_gaming_1795268417185
            ContentCategory.MOTIVATION_STORY, ContentCategory.GENERAL_YOUTUBE ->
              if (scene.sceneNumber % 2 == 0) R.drawable.img_studio_hero_1795268398221
              else R.drawable.img_broll_mystery_1795268416816
          }

        val sourceBitmap =
          BitmapFactory.decodeResource(context.resources, drawableId)
            ?: throw IllegalStateException("Bundled documentary backdrop missing")

        val outputBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outputBitmap)
        val seed = scene.sceneNumber * 131 + variationSeed * 37
        val rng = Random(seed)

        // Crop & reframe differently per scene so each scene gets a unique composition
        val baseScale = maxOf(width.toFloat() / sourceBitmap.width, height.toFloat() / sourceBitmap.height)
        val zoomJitter = 1.08f + (scene.sceneNumber % 4) * 0.08f + (variationSeed % 3) * 0.06f
        val scale = baseScale * zoomJitter
        val scaledW = sourceBitmap.width * scale
        val scaledH = sourceBitmap.height * scale
        val maxOffsetX = (scaledW - width).coerceAtLeast(1f)
        val maxOffsetY = (scaledH - height).coerceAtLeast(1f)
        val offsetX = -rng.nextFloat() * maxOffsetX
        val offsetY = -rng.nextFloat() * maxOffsetY

        val matrix = Matrix().apply {
          postScale(scale, scale)
          postTranslate(offsetX, offsetY)
        }
        val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(sourceBitmap, matrix, bitmapPaint)
        sourceBitmap.recycle()

        // Atmospheric color tint wash tailored to visualStyle
        val (accentA, accentB) =
          when (visualStyle) {
            VisualStyle.CINEMATIC_DOC -> Color.argb(70, 12, 34, 52) to Color.argb(60, 245, 158, 11)
            VisualStyle.NOIR_MYSTERY -> Color.argb(120, 8, 10, 18) to Color.argb(55, 56, 189, 248)
            VisualStyle.SCI_FI_TECH -> Color.argb(85, 6, 30, 48) to Color.argb(75, 34, 211, 238)
            VisualStyle.ARCHIVAL_HISTORY -> Color.argb(80, 44, 28, 12) to Color.argb(65, 251, 191, 36)
            VisualStyle.ESPORTS_GAMING -> Color.argb(85, 30, 10, 45) to Color.argb(75, 239, 68, 68)
            VisualStyle.EPIC_MOTIVATION -> Color.argb(75, 24, 18, 8) to Color.argb(70, 249, 115, 22)
          }

        val gradPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          shader =
            LinearGradient(
              0f,
              0f,
              width.toFloat(),
              height.toFloat(),
              accentA,
              accentB,
              Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), gradPaint)

        // Volumetric lens flare / spotlight unique per scene
        val flareX = width * (0.2f + rng.nextFloat() * 0.6f)
        val flareY = height * (0.18f + rng.nextFloat() * 0.45f)
        val flareRadius = width * 0.55f
        val flarePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
          shader =
            RadialGradient(
              flareX,
              flareY,
              flareRadius,
              intArrayOf(Color.argb(95, 255, 220, 160), Color.argb(30, 34, 211, 238), Color.TRANSPARENT),
              floatArrayOf(0f, 0.45f, 1f),
              Shader.TileMode.CLAMP,
            )
        }
        canvas.drawCircle(flareX, flareY, flareRadius, flarePaint)

        // Subtle documentary grid / optical framing reticles
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = Color.argb(36, 255, 255, 255)
          strokeWidth = 1.5f
          style = Paint.Style.STROKE
        }
        val margin = width * 0.06f
        val bracket = width * 0.04f
        canvas.drawLine(margin, margin, margin + bracket, margin, linePaint)
        canvas.drawLine(margin, margin, margin, margin + bracket, linePaint)
        canvas.drawLine(width - margin, height - margin, width - margin - bracket, height - margin, linePaint)
        canvas.drawLine(width - margin, height - margin, width - margin, height - margin - bracket, linePaint)

        // Floating dust / bokeh particles
        val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        for (p in 0 until 24) {
          val px = rng.nextFloat() * width
          val py = rng.nextFloat() * height
          val pr = 1.5f + rng.nextFloat() * 4.5f
          particlePaint.color = Color.argb(30 + rng.nextInt(55), 255, 240, 210)
          canvas.drawCircle(px, py, pr, particlePaint)
        }

        // Deep edge vignette
        val vignettePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          shader =
            RadialGradient(
              width / 2f,
              height / 2f,
              maxOf(width, height) * 0.68f,
              intArrayOf(Color.TRANSPARENT, Color.argb(170, 5, 7, 12)),
              floatArrayOf(0.45f, 1f),
              Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), vignettePaint)

        val outFile = File(outputDir, "scene_${scene.sceneNumber}_v$variationSeed.jpg")
        FileOutputStream(outFile).use { fos ->
          outputBitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
        }
        outputBitmap.recycle()
        outFile.absolutePath
      }
    }

  private fun buildTopicSections(
    topic: String,
    category: ContentCategory,
    language: NarrationLanguage,
    sceneCount: Int,
  ): List<String> {
    val base =
      when (language) {
        NarrationLanguage.HINDI ->
          when (category) {
            ContentCategory.GAMING_FREE_FIRE ->
              listOf(
                "क्या आपने कभी सोचा है कि $topic ने करोड़ों गेमर्स की दुनिया कैसे बदल दी? अगले कुछ मिनटों में हम इसका असली सच जानेंगे।",
                "शुरुआत में यह सिर्फ एक साधारण कॉन्सेप्ट था, लेकिन तेज़ गेमप्ले और कम स्टोरेज ने इसे हर मोबाइल खिलाड़ी की पहली पसंद बना दिया।",
                "रैंक पुश, क्लच मोमेंट्स और स्मार्ट स्ट्रेटजी ने इसे सिर्फ एक गेम नहीं, बल्कि एक ग्लोबल ई-स्पोर्ट्स क्रांति बना दिया।",
                "लेकिन पर्दे के पीछे की असली कहानी डेटा, टीमवर्क और मिलीसेकंड के फैसलों में छिपी है जो एक विजेता को अलग बनाती है।",
                "हर मैच एक नई सीख देता है — धैर्य, सटीक निशाने और सही समय पर लिया गया फैसला ही आपको बूयाह तक ले जाता है।",
                "तो अगली बार जब आप मैदान में उतरें, याद रखें: $topic की असली ताकत आपकी रणनीति में है।",
              )
            ContentCategory.SCIENCE_TECH ->
              listOf(
                "सिर्फ एक सेकंड के लिए कल्पना कीजिए: $topic के बिना हमारी आधुनिक दुनिया कैसी दिखती?",
                "वैज्ञानिकों ने दशकों तक इस रहस्य पर काम किया, और जो खोज सामने आई उसने विज्ञान की किताबें बदल दीं।",
                "गहराई से देखने पर पता चलता है कि ऊर्जा, डेटा और सटीक इंजीनियरिंग मिलकर इस चमत्कार को संभव बनाते हैं।",
                "आज की लैब्स और सुपरकंप्यूटर्स इस तकनीक को पहले से दस गुना तेज़ और ज़्यादा सुरक्षित बना रहे हैं।",
                "आने वाले दशक में इसका असर हर इंसान के जीवन, स्वास्थ्य और भविष्य की दिशा तय करेगा।",
                "यही कारण है कि $topic को इक्कीसवीं सदी की सबसे बड़ी क्रांति माना जा रहा है।",
              )
            else ->
              listOf(
                "यह कहानी $topic के उस अनकहे पहलू से शुरू होती है, जिसे आज तक ज़्यादातर लोगों ने अनदेखा किया।",
                "जब हम समय के पन्ने पलटते हैं, तो हर संकेत एक गहरे रहस्य की तरफ इशारा करता है।",
                "विशेषज्ञों और ऐतिहासिक सबूतों ने जब कड़ियों को जोड़ा, तो एक चौंकाने वाली सच्चाई सामने आई।",
                "हर मोड़ पर नई चुनौतियाँ थीं, लेकिन जिज्ञासा और सच्चाई की खोज ने इस सफर को आगे बढ़ाया।",
                "आज भी $topic से जुड़े तथ्य हमें सोचने पर मजबूर करते हैं कि हम दुनिया को कितना कम जानते हैं।",
                "अंत में यही सवाल बाकी रहता है — क्या आप इस रहस्य को नए नज़रिए से देखने के लिए तैयार हैं?",
              )
          }

        NarrationLanguage.HINGLISH ->
          when (category) {
            ContentCategory.GAMING_FREE_FIRE ->
              listOf(
                "Kya aapne kabhi socha hai ki $topic ne gaming world mein itna bada dhamaka kaise kiya? Next 5 seconds mein asli game-changer secret dekho!",
                "Jab sab high-end devices ke peeche bhaag rahe the, tab fast-paced survival aur smart mechanics ne har mobile player ko spotlight mein la diya.",
                "Har match ke peeche hoti hai ek silent strategy — zone rotation, gloo wall timing, aur split-second squad calls jo champion banati hain.",
                "Esports tournaments ne dikhaya ki sirf fast fingers kaafi nahi, mind games aur team synergy hi real Booyah dilati hai.",
                "Aaj $topic ek global community ban chuka hai jahan har player apni legacy khud likhta hai.",
                "Toh ready ho jao — kyunki asli grind abhi shuru hui hai, aur agla clutch moment aapka ho sakta hai!",
              )
            else ->
              listOf(
                "Imagine karo ek aisi duniya jahan $topic ke peeche chhupa sach sab kuch badal de — aaj hum wahi mystery unlock karne wale hain!",
                "Upper surface par jo simple dikhta hai, uske neeche decades ki research aur astonishing facts chhupe hue hain.",
                "Jab experts ne timeline aur real evidence ko connect kiya, tab ek aisa pattern nikla jisne sabko shock kar diya.",
                "Har clue ek naye twist ki taraf le jaata hai, aur yahi curiosity is documentary ko itna gripping banati hai.",
                "Aaj ke digital era mein $topic sirf ek topic nahi, balki future ko samajhne ka ek naya lens ban gaya hai.",
                "Story yahin khatam nahi hoti — ab baari hai aapki is puzzle ko naye perspective se dekhne ki!",
              )
          }

        NarrationLanguage.ENGLISH ->
          when (category) {
            ContentCategory.GAMING_FREE_FIRE ->
              listOf(
                "In less than five seconds, $topic transformed millions of mobile screens into high-stakes arenas — here is how it happened.",
                "While legacy titles demanded expensive hardware, agile mechanics and ten-minute survival matches rewrote the rules of competitive play.",
                "Behind every clutch highlight lies cold tactical math: zone predictions, cover timing, and split-second squad communication.",
                "Regional tournaments soon exploded into packed stadiums, turning everyday players into household esports legends.",
                "Every patch and meta shift forces competitors to adapt faster, proving mastery is built on discipline rather than luck.",
                "The next time the drop begins, remember: in $topic, victory belongs to the sharpest strategist.",
              )
            ContentCategory.SCIENCE_TECH ->
              listOf(
                "Stop for five seconds and look closer — $topic is quietly rewriting the laws of what humanity thought possible.",
                "For decades, researchers believed fundamental physics and computing limits locked this door forever.",
                "Then a series of breakthroughs in precision optics, algorithms, and material science cracked the barrier wide open.",
                "Inside modern laboratories, teams are compressing years of experimentation into real-time simulations.",
                "The ripple effects will reshape energy, medicine, and how billions of people interact with intelligence itself.",
                "We are no longer watching the future arrive — with $topic, we are living inside the turning point.",
              )
            ContentCategory.HISTORY_GEOGRAPHY ->
              listOf(
                "Buried beneath centuries of silence lies the forgotten story of $topic — and a clue that changed history forever.",
                "Long before modern borders existed, trade routes and shifting climates forged an extraordinary civilization here.",
                "Archival records and satellite scans recently revealed patterns invisible from the ground.",
                "At the height of its power, ambition collided with nature, leaving behind monuments carved in stone and memory.",
                "Every artifact uncovered today pieces together the human resilience behind the legend.",
                "Standing before these coordinates today, one truth remains clear: $topic still echoes into our modern world.",
              )
            ContentCategory.MOTIVATION_STORY ->
              listOf(
                "Most people quit right before the breakthrough — and the story of $topic proves why resilience changes everything.",
                "It started with zero guarantees, quiet late nights, and doubts louder than applause.",
                "Instead of waiting for perfect conditions, small daily habits compounded into unstoppable momentum.",
                "When setbacks struck hardest, every failure became raw data for a sharper comeback.",
                "History rarely remembers comfort; it remembers the courage to stay in the arena one round longer.",
                "Whatever chapter you are writing today, let $topic remind you: consistency builds what talent alone cannot.",
              )
            else ->
              listOf(
                "What if everything you assumed about $topic was only half the story? Look closely at the next five seconds.",
                "At first glance, the surface appears familiar — yet beneath it lies a chain of hidden facts few ever connect.",
                "Tracing the timeline reveals surprising turning points driven by curiosity, human ambition, and unexpected evidence.",
                "When researchers and analysts examined the data side by side, a compelling pattern emerged.",
                "Understanding how those pieces fit together unlocks a clearer view of the forces shaping our world today.",
                "That is why $topic remains one of the most fascinating stories worth telling — and the question now is what comes next.",
              )
          }
      }

    // Scale to requested sceneCount cleanly
    val result = mutableListOf<String>()
    for (i in 0 until sceneCount) {
      val template = base[i % base.size]
      result +=
        if (i < base.size) template
        else "$template (Chapter ${i + 1}: deeper perspective on $topic)"
    }
    return result
  }
}
