package com.helboy.nemotalk.speech

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class GeminiVoiceClient(private val context: Context) {

    private val tag = "NeMoTalkGeminiVoice"
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val scope = CoroutineScope(Dispatchers.Default + Job())

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private var audioTrack: AudioTrack? = null
    private var currentJob: Job? = null

    companion object {
        val VOICES = listOf(
            "Aoede" to "آئوده (Aoede) — ملایم، دلنشین و صمیمی",
            "Puck" to "پوک (Puck) — پرانرژی، جوان و پویا",
            "Charon" to "کارون (Charon) — عمیق، دانشمند و متین",
            "Kore" to "کوره (Kore) — قاطع، شیوا و رسمی",
            "Fenrir" to "فنریر (Fenrir) — پرشور و مقتدر"
        )
    }

    suspend fun synthesizeSpeech(
        apiKey: String,
        text: String,
        voiceName: String = "Aoede",
        model: String = "gemini-2.0-flash"
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("کلید API جمینای وارد نشده است."))
        }

        val cleanText = cleanMarkdownForSpeech(text)
        if (cleanText.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("متن برای تبدیل به صوت خالی است."))
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

            val promptText = "Read the following text aloud with natural, authentic Persian pronunciation, clear enunciation of words, correct short vowels, warm emotional cadence, and expressive human inflection:\n\n$cleanText"

            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", promptText)
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("responseModalities", JSONArray().apply {
                        put("AUDIO")
                    })
                    put("speechConfig", JSONObject().apply {
                        put("voiceConfig", JSONObject().apply {
                            put("prebuiltVoiceConfig", JSONObject().apply {
                                put("voiceName", voiceName)
                            })
                        })
                    })
                })
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .post(requestJson.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    val errMsg = when (response.code) {
                        400 -> "درخواست نامعتبر به جمینای (کد ۴۰۰)"
                        403 -> "کلید جمینای نامعتبر یا محدود شده است (کد ۴۰۳)"
                        429 -> "محدودیت تعداد درخواست جمینای (Rate Limit)"
                        else -> "خطای سرویس جمینای (${response.code})"
                    }
                    Log.e(tag, "Gemini Audio API error: $errMsg: $responseBody")
                    return@withContext Result.failure(IOException(errMsg))
                }

                val json = JSONObject(responseBody)
                val candidates = json.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        for (i in 0 until parts.length()) {
                            val part = parts.getJSONObject(i)
                            val inlineData = part.optJSONObject("inlineData")
                            if (inlineData != null) {
                                val base64Data = inlineData.optString("data")
                                if (base64Data.isNotBlank()) {
                                    val pcmBytes = Base64.decode(base64Data, Base64.DEFAULT)
                                    Log.d(tag, "Received ${pcmBytes.size} bytes of 24kHz audio from Gemini")
                                    return@withContext Result.success(pcmBytes)
                                }
                            }
                        }
                    }
                }

                Result.failure(IOException("دیتای صوتی در پاسخ جمینای یافت نشد."))
            }
        } catch (e: Exception) {
            Log.e(tag, "Exception during Gemini speech synthesis: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun speak(
        apiKey: String,
        text: String,
        voiceName: String = "Aoede",
        onDone: () -> Unit = {}
    ) {
        stop()

        currentJob = scope.launch(Dispatchers.Default) {
            try {
                _isSpeaking.value = true
                val result = synthesizeSpeech(apiKey, text, voiceName)
                result.fold(
                    onSuccess = { pcmBytes ->
                        playPcm16Audio(pcmBytes, sampleRate = 24000, onDone = onDone)
                    },
                    onFailure = { err ->
                        Log.e(tag, "Gemini speak failed: ${err.message}")
                        _isSpeaking.value = false
                        withContext(Dispatchers.Main) {
                            onDone()
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(tag, "Error in speak job: ${e.message}")
                _isSpeaking.value = false
                withContext(Dispatchers.Main) {
                    onDone()
                }
            }
        }
    }

    private suspend fun playPcm16Audio(
        pcmData: ByteArray,
        sampleRate: Int = 24000,
        onDone: () -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build()

            val audioFormat = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .setSampleRate(sampleRate)
                .build()

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(audioAttributes)
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(pcmData.size)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack?.write(pcmData, 0, pcmData.size)
            audioTrack?.play()

            // 16-bit mono = 2 bytes per sample
            val durationMs = (pcmData.size.toDouble() / (sampleRate * 2) * 1000).toLong()
            delay(durationMs + 100)

            try {
                audioTrack?.stop()
                audioTrack?.release()
            } catch (_: Exception) {}
            audioTrack = null

            _isSpeaking.value = false
            withContext(Dispatchers.Main) {
                onDone()
            }
        } catch (e: Exception) {
            Log.e(tag, "AudioTrack error: ${e.message}")
            _isSpeaking.value = false
            withContext(Dispatchers.Main) {
                onDone()
            }
        }
    }

    fun stop() {
        currentJob?.cancel()
        currentJob = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
        _isSpeaking.value = false
    }

    private fun cleanMarkdownForSpeech(text: String): String {
        return text
            .replace(Regex("```[\\s\\S]*?```"), " ")
            .replace(Regex("`[^`]*`"), "")
            .replace(Regex("\\[(.*?)\\]\\(.*?\\)"), "$1")
            .replace(Regex("[*#_~`>]"), "")
            .replace(Regex("[\\p{So}\\p{Cn}]"), " ")
            .replace("،", " ، ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
