package com.helboy.nemotalk.network

import android.content.Context
import com.helboy.nemotalk.data.PreferencesManager
import com.helboy.nemotalk.model.ChatMessage
import com.helboy.nemotalk.model.visionFallbackFor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class SendMessageResult(
    val content: String,
    val latencyMs: Long,
    val modelUsed: String,
    val wasFallback: Boolean = false
)

class NvidiaApiClient {

    private var client: OkHttpClient = buildClient(null)
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val endpointUrl = "https://integrate.api.nvidia.com/v1/chat/completions"
    private val modelsUrl = "https://integrate.api.nvidia.com/v1/models"

    /**
     * Rebuild the OkHttp client against [prefs] so a proxy toggle in Settings
     * takes effect for every subsequent request. Kept cheap: OkHttp reuses
     * connection pools across builders with the same address set.
     */
    fun updateProxySettings(prefs: PreferencesManager?) {
        client = buildClient(prefs)
    }

    private fun buildClient(prefs: PreferencesManager?): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
        return ProxySupport.applyTo(builder, prefs ?: return builder.build()).build()
    }

    // High-availability fallback chain for NVIDIA API catalog
    private val fallbackModels = listOf(
        "nvidia/nemotron-4-340b-instruct",
        "nvidia/nemotron-3-super-120b-a12b",
        "meta/llama-3.3-70b-instruct",
        "meta/llama-3.1-8b-instruct",
        "meta/llama-3.2-11b-vision-instruct",
        "openai/gpt-oss-20b"
    )

    // Vision-capable fallback chain (NVIDIA NIM multimodal models) — ordered by quality
    private val visionModels = listOf(
        "meta/llama-3.2-90b-vision-instruct",
        "meta/llama-3.2-11b-vision-instruct",
        "nvidia/neva-22b"
    )

    private val MAX_HISTORY_MESSAGES = 12

    suspend fun sendMessage(
        apiKey: String,
        model: String,
        messages: List<ChatMessage>,
        systemPrompt: String,
        responseLanguage: String = "fa",
        currentContext: Context? = null
    ): Result<SendMessageResult> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("لطفاً ابتدا کلید API ان‌ویدیا (nvapi-...) را در بخش تنظیمات وارد کنید.")
            )
        }

        val effectivePrompt = when (responseLanguage) {
            "fa" -> if (systemPrompt.isNotBlank()) {
                "$systemPrompt\n\n[دستور قطعی: پاسخ شما باید حتماً و ۱۰۰٪ به زبان فارسی روان، سلیس و دلنشین باشد. به هیچ عنوان انگلیسی پاسخ ندهید.]"
            } else {
                "شما دستیار صوتی و هوشمند آوانمو هستید. تمام پاسخ‌های شما باید ۱۰۰٪ به زبان فارسی شیوا و روان باشد."
            }
            "en" -> "$systemPrompt\n\n[Instruction: Reply strictly in English.]"
            else -> systemPrompt
        }

        // Detect any image attached to the latest user message → vision mode
        val lastUserImage = messages.lastOrNull { it.isUser && it.imageUri != null }?.imageUri
        val isVisionRequest = lastUserImage != null && currentContext != null

        val visionModel = if (isVisionRequest) visionFallbackFor(model) else model

        // 1. Try (vision model if image present, else requested model) first
        val firstAttempt = if (isVisionRequest && currentContext != null) {
            executeVisionRequest(apiKey, visionModel, messages, effectivePrompt, currentContext, lastUserImage!!)
        } else {
            executeRequest(apiKey, model, messages, effectivePrompt)
        }
        if (firstAttempt.isSuccess) {
            val (content, latency) = firstAttempt.getOrThrow()
            return@withContext Result.success(
                SendMessageResult(
                    content = content,
                    latencyMs = latency,
                    modelUsed = visionModel,
                    wasFallback = false
                )
            )
        }

        val firstError = firstAttempt.exceptionOrNull()
        val errorMsg = firstError?.message ?: ""

        // Check if error is 404 (Function not found) or deprecated model
        val isNotFoundOrDeprecated = errorMsg.contains("404") ||
                errorMsg.contains("Function") ||
                errorMsg.contains("not found", ignoreCase = true) ||
                errorMsg.contains("inactive", ignoreCase = true)

        if (!isNotFoundOrDeprecated) {
            // Not a model ID issue, return original error (e.g. 401, 429, no internet)
            return@withContext Result.failure(firstError ?: IOException("خطای نامشخص"))
        }

        // 2. Auto-fallback chain: Try fallback models until one works!
        val chain = if (isVisionRequest) visionModels else fallbackModels
        for (fallbackModel in chain) {
            if (fallbackModel == visionModel) continue

            val fallbackAttempt = if (isVisionRequest && currentContext != null) {
                executeVisionRequest(apiKey, fallbackModel, messages, effectivePrompt, currentContext, lastUserImage!!)
            } else {
                executeRequest(apiKey, fallbackModel, messages, effectivePrompt)
            }
            if (fallbackAttempt.isSuccess) {
                val (content, latency) = fallbackAttempt.getOrThrow()
                return@withContext Result.success(
                    SendMessageResult(
                        content = content,
                        latencyMs = latency,
                        modelUsed = fallbackModel,
                        wasFallback = true
                    )
                )
            }
        }

        // If all fallbacks failed, report clear error
        Result.failure(
            IOException("مدل '$visionModel' در اکانت ان‌ویدیا شما در دسترس نیست و مدل‌های جایگزین نیز پاسخ ندادند. لطفاً در تنظیمات کلید را تست یا مدل دیگری انتخاب کنید.")
        )
    }

    private fun executeVisionRequest(
        apiKey: String,
        model: String,
        messages: List<ChatMessage>,
        systemPrompt: String,
        context: Context,
        imageUri: String
    ): Result<Pair<String, Long>> {
        return try {
            val startTime = System.currentTimeMillis()

            // Read & base64-encode the image from the device
            val base64Image = readImageAsBase64(context, imageUri)
            if (base64Image.isBlank()) {
                return Result.failure(IOException("امکان خواندن تصویر انتخاب شده وجود ندارد."))
            }

            val requestJson = buildVisionRequestJson(messages, systemPrompt, base64Image, model)

            val request = Request.Builder()
                .url(endpointUrl)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .post(requestJson.toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    val apiMessage = JSONObject(responseBody).optJSONObject("error")
                        ?.optString("detail") ?: ""
                    return Result.failure(
                        IOException("خطای سرور ان‌ویدیا (${response.code}): $apiMessage")
                    )
                }

                val (content, finishReason) = parseChatCompletion(JSONObject(responseBody))
                if (content.isBlank()) {
                    return Result.failure(IOException("پاسخ خالی از مدل تصویری دریافت شد ($finishReason)"))
                }
                Result.success(Pair(content, System.currentTimeMillis() - startTime))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun readImageAsBase64(context: Context, imageUri: String): String {
        return try {
            val uri = android.net.Uri.parse(imageUri)
            context.contentResolver.openInputStream(uri)?.use { input ->
                val bytes = input.readBytes()
                android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
            } ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    private fun buildVisionRequestJson(
        messages: List<ChatMessage>,
        systemPrompt: String,
        base64Image: String,
        model: String
    ): String {
        val jsonArray = JSONArray()

        // System message
        jsonArray.put(JSONObject().apply {
            put("role", "system")
            put("content", systemPrompt)
        })

        // Conversation history (text only, images are not re-sent for previous turns)
        messages.takeLast(MAX_HISTORY_MESSAGES).forEach { msg ->
            jsonArray.put(JSONObject().apply {
                put("role", if (msg.isUser) "user" else "assistant")
                if (msg.isUser && msg.imageUri != null) {
                    // Multimodal content: image + text
                    put("content", JSONArray().apply {
                        put(JSONObject().apply {
                            put("type", "image_url")
                            put("image_url", JSONObject().apply {
                                put("url", "data:image/jpeg;base64,$base64Image")
                            })
                        })
                        if (msg.content.isNotBlank()) {
                            put(JSONObject().apply {
                                put("type", "text")
                                put("text", msg.content)
                            })
                        }
                    })
                } else {
                    put("content", msg.content)
                }
            })
        }

        val root = JSONObject().apply {
            put("model", model)
            put("messages", jsonArray)
            put("temperature", 0.6)
            put("top_p", 0.9)
            put("max_tokens", 2048)
            put("stream", false)
        }
        return root.toString()
    }

    private fun parseChatCompletion(json: JSONObject): Pair<String, String> {
        val choices = json.optJSONArray("choices")
        if (choices != null && choices.length() > 0) {
            val firstChoice = choices.getJSONObject(0)
            val messageObj = firstChoice.optJSONObject("message")
            val content = messageObj?.optString("content") ?: ""
            val finishReason = firstChoice.optString("finish_reason", "")
            return Pair(content.trim(), finishReason)
        }
        return Pair("", "")
    }

    private fun executeRequest(
        apiKey: String,
        model: String,
        messages: List<ChatMessage>,
        systemPrompt: String
    ): Result<Pair<String, Long>> {
        val startTime = System.currentTimeMillis()
        try {
            val rootJson = JSONObject().apply {
                put("model", model)
                put("temperature", 0.6)
                put("max_tokens", 1024)
                put("stream", false)

                val msgsArray = JSONArray()

                // Add System Prompt
                if (systemPrompt.isNotBlank()) {
                    msgsArray.put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemPrompt)
                    })
                }

                // Add conversation history (last 10 messages for context)
                val recentMessages = messages.takeLast(10)
                for (msg in recentMessages) {
                    if (msg.isError) continue
                    msgsArray.put(JSONObject().apply {
                        put("role", if (msg.isUser) "user" else "assistant")
                        put("content", msg.content)
                    })
                }

                put("messages", msgsArray)
            }

            val requestBody = rootJson.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(endpointUrl)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Accept", "application/json")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val latency = System.currentTimeMillis() - startTime
                val responseBody = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    val message = when (response.code) {
                        401 -> "کلید API ان‌ویدیا معتبر نیست (کد ۴۰۱). لطفاً کلید سالم وارد کنید."
                        404 -> "خطای سرور ان‌ویدیا (404): مدل یا تابع در این اکانت یافت نشد."
                        403 -> "دسترسی به این مدل ان‌ویدیا مجاز نیست یا کردیت اکانت به اتمام رسیده است (کد ۴۰۳)."
                        429 -> "محدودیت تعداد درخواست ان‌ویدیا (Rate Limit). لطفاً کمی صبر کنید."
                        500, 502, 503 -> "سرور ان‌ویدیا در حال حاضر در دسترس نیست (کد ${response.code})."
                        else -> "خطای سرور ان‌ویدیا (${response.code}): ${parseErrorMessage(responseBody)}"
                    }
                    return Result.failure(IOException(message))
                }

                val responseJson = JSONObject(responseBody)
                val choices = responseJson.optJSONArray("choices")
                if (choices != null && choices.length() > 0) {
                    val firstChoice = choices.getJSONObject(0)
                    val messageObj = firstChoice.optJSONObject("message")
                    val content = messageObj?.optString("content") ?: ""
                    return Result.success(Pair(content.trim(), latency))
                } else {
                    return Result.failure(IOException("پاسخ دریافتی از سرور ان‌ویدیا خالی بود."))
                }
            }
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            return Result.failure(
                IOException("خطا در برقراری ارتباط با ان‌ویدیا (${e.localizedMessage ?: "عدم اتصال به اینترنت"})")
            )
        }
    }

    suspend fun fetchAvailableModels(apiKey: String): Result<List<String>> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("کلید API وارد نشده است."))
        }

        try {
            val request = Request.Builder()
                .url(modelsUrl)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Accept", "application/json")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("دریافت مدل‌ها ناموفق بود (${response.code})"))
                }

                val json = JSONObject(body)
                val data = json.optJSONArray("data") ?: JSONArray()
                val list = mutableListOf<String>()
                for (i in 0 until data.length()) {
                    val obj = data.getJSONObject(i)
                    val id = obj.optString("id")
                    // Filter chat / instruct models
                    if (id.isNotBlank() && (id.contains("instruct") || id.contains("chat") || id.contains("nemotron") || id.contains("llama") || id.contains("gemma"))) {
                        list.add(id)
                    }
                }
                list.sort()
                Result.success(list)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun testConnection(apiKey: String, model: String = "nvidia/nemotron-4-340b-instruct"): Result<Pair<String, Long>> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("کلید API نمی‌تواند خالی باشد."))
        }

        // Test with current model, or fallback
        val modelsToTry = listOf(model) + fallbackModels.filter { it != model }
        for (m in modelsToTry) {
            val startTime = System.currentTimeMillis()
            try {
                val testPayload = JSONObject().apply {
                    put("model", m)
                    put("max_tokens", 5)
                    put("messages", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", "ping")
                        })
                    })
                }

                val request = Request.Builder()
                    .url(endpointUrl)
                    .addHeader("Authorization", "Bearer $apiKey")
                    .post(testPayload.toString().toRequestBody(jsonMediaType))
                    .build()

                client.newCall(request).execute().use { response ->
                    val latency = System.currentTimeMillis() - startTime
                    if (response.isSuccessful) {
                        return@withContext Result.success(Pair(m, latency))
                    }
                }
            } catch (_: Exception) {
                continue
            }
        }

        Result.failure(IOException("اتصال با هیچ‌یک از مدل‌ها برقرار نشد. لطفاً صحت کلید API را بررسی کنید."))
    }

    private fun parseErrorMessage(jsonStr: String): String {
        return try {
            val obj = JSONObject(jsonStr)
            val err = obj.optJSONObject("error")
            err?.optString("message") ?: jsonStr.take(150)
        } catch (_: Exception) {
            jsonStr.take(150)
        }
    }
}
