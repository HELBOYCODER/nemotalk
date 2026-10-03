package com.helboy.nemotalk.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
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

    private var onDoneCallback: (() -> Unit)? = null

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
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

    fun speak(
        text: String,
        speechRate: Float = 1.0f,
        speechPitch: Float = 1.0f,
        onDone: () -> Unit = {}
    ) {
        if (!isInitialized || tts == null) return

        this.onDoneCallback = onDone
        val cleanText = cleanMarkdownForSpeech(text)

        // Detect if text is mostly Persian/Arabic
        val isPersian = containsPersianCharacters(cleanText)
        if (isPersian) {
            val faLocale = Locale("fa", "IR")
            val result = tts?.setLanguage(faLocale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Fallback to default or Arabic if Persian TTS package is not installed on device
                tts?.language = Locale.getDefault()
            }
        } else {
            tts?.language = Locale.US
        }

        tts?.setSpeechRate(speechRate.coerceIn(0.5f, 2.0f))
        tts?.setPitch(speechPitch.coerceIn(0.5f, 2.0f))

        val utteranceId = UUID.randomUUID().toString()
        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
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

    private fun containsPersianCharacters(text: String): Boolean {
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
            .replace(Regex("```[\\s\\S]*?```"), " قطعه کد ")
            .replace(Regex("`[^`]*`"), "")
            .replace(Regex("\\[(.*?)\\]\\(.*?\\)"), "$1")
            .replace(Regex("[*#_~`>]"), "")
            .trim()
    }
}
