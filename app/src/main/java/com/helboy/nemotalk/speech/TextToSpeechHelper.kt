package com.helboy.nemotalk.speech

import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

class TextToSpeechHelper(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _hasPersianVoice = MutableStateFlow(false)
    val hasPersianVoice: StateFlow<Boolean> = _hasPersianVoice.asStateFlow()

    private var onDoneCallback: (() -> Unit)? = null

    init {
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
        val engine = tts ?: return
        try {
            val voices = engine.voices
            val hasFa = voices?.any { isVoicePersian(it) } == true
            if (hasFa) {
                _hasPersianVoice.value = true
                return
            }
        } catch (_: Exception) {}

        val faLocales = listOf(
            Locale.forLanguageTag("fa-IR"),
            Locale("fa", "IR"),
            Locale("fa"),
            Locale("fas")
        )
        val isAvail = faLocales.any { engine.isLanguageAvailable(it) >= TextToSpeech.LANG_AVAILABLE }
        _hasPersianVoice.value = isAvail
    }

    private fun isVoicePersian(voice: Voice): Boolean {
        val tag = voice.locale.toLanguageTag().lowercase()
        val lang = voice.locale.language.lowercase()
        return tag.startsWith("fa") || lang == "fa" || lang == "fas" || tag.contains("persian")
    }

    private fun configureVoiceForPersian(engine: TextToSpeech): Boolean {
        // 1. Search through all available voice packs (supports Google Speech Services neural Persian voice)
        try {
            val voices = engine.voices
            if (!voices.isNullOrEmpty()) {
                val persianVoices = voices.filter { isVoicePersian(it) }
                if (persianVoices.isNotEmpty()) {
                    // Prefer local installed voice, or network high-quality voice
                    val chosen = persianVoices.find { !it.isNetworkConnectionRequired }
                        ?: persianVoices.first()
                    engine.voice = chosen
                    return true
                }
            }
        } catch (_: Exception) {}

        // 2. Try setting by locale
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

        // 3. Fallback: Force set Locale("fa") — NEVER switch to English for Persian text!
        engine.language = Locale("fa")
        return false
    }

    fun speak(
        text: String,
        speechRate: Float = 1.0f,
        speechPitch: Float = 1.0f,
        forcePersian: Boolean = false,
        onDone: () -> Unit = {}
    ) {
        if (!isInitialized || tts == null) return

        this.onDoneCallback = onDone
        val cleanText = cleanMarkdownForSpeech(text)
        val engine = tts ?: return

        val isPersian = forcePersian || containsPersianCharacters(cleanText)

        if (isPersian) {
            configureVoiceForPersian(engine)
        } else {
            engine.language = Locale.US
        }

        engine.setSpeechRate(speechRate.coerceIn(0.5f, 2.0f))
        engine.setPitch(speechPitch.coerceIn(0.5f, 2.0f))

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

    fun stop() {
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
            .replace(Regex("```[\\s\\S]*?```"), " قطعه کد ")
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
