package com.helboy.nemotalk.speech

import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import com.helboy.nemotalk.data.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

class TextToSpeechHelper(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    // 1. Gemini Studio Neural Voice Engine (Hyper-realistic & expressive Persian TTS)
    val geminiClient = GeminiVoiceClient(context)

    // 2. Embedded Neural Persian Speech Engine (Piper VITS Amir INT8 — 100% offline)
    val embeddedEngine = EmbeddedPersianTtsEngine(context)

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _hasPersianVoice = MutableStateFlow(true)
    val hasPersianVoice: StateFlow<Boolean> = _hasPersianVoice.asStateFlow()

    private var onDoneCallback: (() -> Unit)? = null

    init {
        // Collect embedded and gemini speaking states
        scope.launch {
            embeddedEngine.isSpeaking.collect { speaking ->
                if (speaking) _isSpeaking.value = true
            }
        }
        scope.launch {
            geminiClient.isSpeaking.collect { speaking ->
                if (speaking) _isSpeaking.value = true
            }
        }

        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            checkPersianVoiceAvailability()

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                    onDoneCallback?.invoke()
                    onDoneCallback = null
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                    onDoneCallback?.invoke()
                    onDoneCallback = null
                }
            })
        }
    }

    private fun checkPersianVoiceAvailability() {
        _hasPersianVoice.value = true
    }

    private fun isVoicePersian(voice: Voice): Boolean {
        val tag = voice.locale.toLanguageTag().lowercase()
        val lang = voice.locale.language.lowercase()
        return tag.startsWith("fa") || lang == "fa" || lang == "fas" || tag.contains("persian")
    }

    private fun configureVoiceForPersian(engine: TextToSpeech): Boolean {
        try {
            val voices = engine.voices
            if (!voices.isNullOrEmpty()) {
                val persianVoices = voices.filter { isVoicePersian(it) }
                if (persianVoices.isNotEmpty()) {
                    val chosen = persianVoices.find { !it.isNetworkConnectionRequired }
                        ?: persianVoices.first()
                    engine.voice = chosen
                    return true
                }
            }
        } catch (_: Exception) {}

        val locales = listOf(
            Locale.forLanguageTag("fa-IR"),
            Locale("fa", "IR"),
            Locale("fa"),
            Locale("fas")
        )
        for (loc in locales) {
            val res = engine.setLanguage(loc)
            if (res != TextToSpeech.LANG_MISSING_DATA && res != TextToSpeech.LANG_NOT_SUPPORTED) {
                return true
            }
        }

        engine.language = Locale("fa")
        return false
    }

    fun speak(
        text: String,
        prefs: PreferencesManager,
        forcePersian: Boolean = false,
        onDone: () -> Unit = {}
    ) {
        val cleanText = cleanMarkdownForSpeech(text)
        val isPersian = forcePersian || containsPersianCharacters(cleanText)

        // 1. If Gemini Voice Engine selected and API key is present: use Google Gemini's hyper-realistic audio!
        if (prefs.ttsEngineType == "gemini" && prefs.geminiApiKey.isNotBlank()) {
            _isSpeaking.value = true
            geminiClient.speak(
                apiKey = prefs.geminiApiKey,
                text = cleanText,
                voiceName = prefs.geminiVoice,
                onDone = {
                    _isSpeaking.value = false
                    onDone()
                }
            )
            return
        }

        // 2. If Persian and Embedded Neural Engine is ready (or Gemini fallback): use embedded Piper Amir INT8
        if (isPersian && embeddedEngine.isReady.value) {
            _isSpeaking.value = true
            embeddedEngine.speak(
                text = cleanText,
                speechRate = prefs.speechRate,
                onDone = {
                    _isSpeaking.value = false
                    onDone()
                }
            )
            return
        }

        // 3. System TTS Fallback (for English or when system engine is explicitly chosen)
        if (!isInitialized || tts == null) {
            onDone()
            return
        }

        this.onDoneCallback = onDone
        val engine = tts ?: return

        if (isPersian) {
            configureVoiceForPersian(engine)
        } else {
            engine.language = Locale.US
        }

        engine.setSpeechRate(prefs.speechRate.coerceIn(0.5f, 2.0f))
        engine.setPitch(prefs.speechPitch.coerceIn(0.5f, 2.0f))

        val utteranceId = UUID.randomUUID().toString()
        engine.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun openTtsSettings() {
        try {
            val intent = Intent("com.android.settings.TTS_SETTINGS").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val intent = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
    }

    /**
     * Apply speech rate/pitch to system TTS engine (for system engine mode).
     */
    fun applySpeechParameters(rate: Float, pitch: Float) {
        try {
            tts?.setSpeechRate(rate.coerceIn(0.6f, 1.6f))
            tts?.setPitch(pitch.coerceIn(0.7f, 1.5f))
        } catch (_: Exception) {
        }
    }

    fun stop() {
        geminiClient.stop()
        embeddedEngine.stop()
        tts?.stop()
        _isSpeaking.value = false
        onDoneCallback = null
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }

    fun containsPersianCharacters(text: String): Boolean {
        for (char in text) {
            val code = char.code
            if (code in 0x0600..0x06FF || code in 0xFB50..0xFDFF || code in 0xFE70..0xFEFF) {
                return true
            }
        }
        return false
    }

    private fun cleanMarkdownForSpeech(text: String): String {
        return text
            // Strip code blocks and inline code
            .replace(Regex("```[\\s\\S]*?```"), " ")
            .replace(Regex("`[^`]*`"), "")
            // Strip links, keep anchor text
            .replace(Regex("\\[(.*?)\\]\\(.*?\\)"), "$1")
            // Strip markdown header/bold/italic tags
            .replace(Regex("[*#_~`>]"), "")
            // Strip emojis so TTS doesn't stumble
            .replace(Regex("[\\p{So}\\p{Cn}]"), " ")
            // Standardize Persian pause commas
            .replace("،", " ، ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
