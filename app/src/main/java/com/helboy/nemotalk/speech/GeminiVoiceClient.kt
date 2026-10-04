package com.helboy.nemotalk.speech

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Base64
import android.util.Log
import com.helboy.nemotalk.data.PreferencesManager
import com.helboy.nemotalk.network.ProxySupport
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
    private var client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /** Mirror of [NvidiaApiClient.updateProxySettings] for the TTS endpoint. */
    fun updateProxySettings(prefs: PreferencesManager?) {
        val builder = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
        client = ProxySupport.applyTo(builder, prefs ?: return).build()
    }

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
        model: String = "gemini-2.5-flash-preview-tts"
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("کلید API جمینای وارد نشده است."))
        }

        val cleanText = cleanMarkdownForSpeech(text)
        if (cleanText.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("متن برای تبدیل به صوت خالی است."))
        }

        try {
            // ponytail: 2.5-flash native TTS needs style instructions to be spoken, not read as content.
            // A bare "style" key makes the model read the style aloud; wrapping it as a user instruction
            // keeps it out of the synthesized audio while still steering prosody.
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", "Style: Clear, warm and professional.\n\n$cleanText")
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
                    val apiMessage = try {
                        JSONObject(responseBody).optJSONObject("error")?.optString("message", "") ?: ""
                    } catch (_: Exception) { "" }
                    val errMsg = when (response.code) {
                        400 -> "درخواست نامعتبر به جمینای (کد ۴۰۰)${if (apiMessage.isNotBlank()) "\n$apiMessage" else ""}"
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
