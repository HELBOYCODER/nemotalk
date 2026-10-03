package com.helboy.nemotalk.network

import com.helboy.nemotalk.model.ChatMessage
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

class NvidiaApiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val endpointUrl = "https://integrate.api.nvidia.com/v1/chat/completions"

    suspend fun sendMessage(
        apiKey: String,
        model: String,
        messages: List<ChatMessage>,
        systemPrompt: String
    ): Result<Pair<String, Long>> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("لطفاً ابتدا کلید API ان‌ویدیا (nvapi-...) را در بخش تنظیمات وارد کنید.")
            )
        }

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
                    val errorMsg = when (response.code) {
                        401 -> "کلید API ان‌ویدیا معتبر نیست (کد ۴۰۱). لطفاً کلید سالم وارد کنید."
                        403 -> "دسترسی به این مدل ان‌ویدیا مجاز نیست یا کردیت اکانت به اتمام رسیده است (کد ۴۰۳)."
                        429 -> "محدودیت تعداد درخواست ان‌ویدیا (Rate Limit). لطفاً کمی صبر کنید."
                        500, 502, 503 -> "سرور ان‌ویدیا در حال حاضر در دسترس نیست (کد ${response.code})."
                        else -> "خطای سرور ان‌ویدیا (${response.code}): ${parseErrorMessage(responseBody)}"
                    }
                    return@withContext Result.failure(IOException(errorMsg))
                }

                val responseJson = JSONObject(responseBody)
                val choices = responseJson.optJSONArray("choices")
                if (choices != null && choices.length() > 0) {
                    val firstChoice = choices.getJSONObject(0)
                    val messageObj = firstChoice.optJSONObject("message")
                    val content = messageObj?.optString("content") ?: ""
                    return@withContext Result.success(Pair(content.trim(), latency))
                } else {
                    return@withContext Result.failure(IOException("پاسخ دریافتی از سرور ان‌ویدیا خالی بود."))
                }
            }
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            return@withContext Result.failure(
                IOException("خطا در برقراری ارتباط با ان‌ویدیا (${e.localizedMessage ?: "عدم اتصال به اینترنت"})")
            )
        }
    }

    suspend fun testConnection(apiKey: String, model: String = "nvidia/llama-3.1-nemotron-70b-instruct"): Result<Long> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("کلید API نمی‌تواند خالی باشد."))
        }

        val startTime = System.currentTimeMillis()
        try {
            val testPayload = JSONObject().apply {
                put("model", model)
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
                    Result.success(latency)
                } else {
                    Result.failure(IOException("اتصال ناموفق بود (کد ${response.code})"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
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
