package com.helboy.nemotalk.domain

import com.helboy.nemotalk.data.ChatDatabase
import com.helboy.nemotalk.network.NvidiaApiClient

/**
 * Agent loop (SoL-Pi lite): recall → think → act → speak.
 * PonyTail: one linear path, no branches, no framework.
 *
 * - recall: pull FTS memories keyed off the latest user line
 * - think: ask Nemotron with history + memories
 * - act: persist + return the AI text for the caller to speak/render
 */
class AgentLoop(
    private val db: ChatDatabase,
    private val api: NvidiaApiClient
) {
    data class Result(
        val text: String,
        val modelUsed: String,
        val latencyMs: Long,
        val wasFallback: Boolean
    )

    suspend fun run(
        conversationId: String,
        apiKey: String,
        model: String,
        history: List<com.helboy.nemotalk.model.ChatMessage>,
        systemPrompt: String,
        responseLanguage: String,
        context: android.content.Context,
        onModelFallback: (String) -> Unit = {}
    ): kotlin.Result<Result> {
        val recalled: List<String> = try {
            db.recall(history.lastOrNull { it.isUser }?.content.orEmpty())
        } catch (_: Exception) {
            emptyList()
        }

        val send = api.sendMessage(
            apiKey = apiKey,
            model = model,
            messages = history,
            systemPrompt = systemPrompt,
            responseLanguage = responseLanguage,
            currentContext = context,
            memories = recalled
        )
        return send.fold(
            onSuccess = { r ->
                if (r.wasFallback) onModelFallback(r.modelUsed)
                kotlin.Result.success(
                    Result(
                        text = r.content,
                        modelUsed = r.modelUsed,
                        latencyMs = r.latencyMs,
                        wasFallback = r.wasFallback
                    )
                )
            },
            onFailure = { e -> kotlin.Result.failure(e) }
        )
    }
}
