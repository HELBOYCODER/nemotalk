package com.helboy.nemotalk.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class SpeechRecognitionHelper(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _rmsDb = MutableStateFlow(0f)
    val rmsDb: StateFlow<Float> = _rmsDb.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    private var onFinalResultCallback: ((String) -> Unit)? = null
    private var onErrorCallback: ((String) -> Unit)? = null

    fun startListening(
        language: String = "fa-IR",
        onFinalResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("موتور تشخیص گفتار در این دستگاه پشتیبانی نمی‌شود یا فعال نیست.")
            return
        }

        stopListening()

        this.onFinalResultCallback = onFinalResult
        this.onErrorCallback = onError
        _partialText.value = ""

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _isListening.value = true
                    }

                    override fun onBeginningOfSpeech() {
                        _isListening.value = true
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        // Normalize between 0.0 and 1.0 roughly
                        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                        _rmsDb.value = normalized
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _isListening.value = false
                        _rmsDb.value = 0f
                    }

                    override fun onError(error: Int) {
                        _isListening.value = false
                        _rmsDb.value = 0f
                        val message = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "خطای ضبط صدا"
                            SpeechRecognizer.ERROR_CLIENT -> "خطای کلاینت صوتی"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "دسترسی به میکروفون داده نشده است"
                            SpeechRecognizer.ERROR_NETWORK -> "خطای شبکه تشخیص گفتار"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "اتمام مهلت اتصال شبکه صوتی"
                            SpeechRecognizer.ERROR_NO_MATCH -> "صدایی تشخیص داده نشد"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "سرویس صوتی در حال حاضر مشغول است"
                            SpeechRecognizer.ERROR_SERVER -> "خطای سرور تشخیص صدا"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "مهلت سخن گفتن به پایان رسید"
                            else -> "خطای نامشخص صوتی (کد $error)"
                        }
                        // Only report non-empty errors
                        if (error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                            onErrorCallback?.invoke(message)
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        _isListening.value = false
                        _rmsDb.value = 0f
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()?.trim() ?: ""
                        if (text.isNotBlank()) {
                            _partialText.value = text
                            onFinalResultCallback?.invoke(text)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        if (text.isNotBlank()) {
                            _partialText.value = text
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)

                if (language == "auto") {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                } else {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language)
                }
            }

            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            _isListening.value = false
            onError("خطا در راه‌اندازی ضبط صدا: ${e.localizedMessage}")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
        _isListening.value = false
        _rmsDb.value = 0f
    }
}
