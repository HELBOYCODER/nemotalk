package com.helboy.nemotalk.ui.state

import android.content.Context
import androidx.lifecycle.ViewModel
import com.helboy.nemotalk.data.ChatDatabase
import com.helboy.nemotalk.domain.AgentLoop
import com.helboy.nemotalk.domain.SessionManager
import com.helboy.nemotalk.model.ChatMessage
import com.helboy.nemotalk.network.NvidiaApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Minis-grade ChatViewModel — thin UI state, fat domain.
 * NeMoTalkApp.kt (422-line god-file) delegates here:
 * conversations/messages live in SessionManager,
 * send path lives in AgentLoop (recall->think->act).
 * This ViewModel only orchestrates + exposes thinking/error state.
 */
class ChatViewModel(
    db: ChatDatabase,
    api: NvidiaApiClient
) : ViewModel() {

    val session = SessionManager(db)
    private val agent = AgentLoop(db, api)

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()

    private val _activeModel = MutableStateFlow("")
    val activeModel: StateFlow<String> = _activeModel.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val _aiVoiceText = MutableStateFlow("")
    val aiVoiceText: StateFlow<String> = _aiVoiceText.asStateFlow()

    fun load(initialModel: String) {
        session.load()
        _activeModel.value = initialModel
    }

    fun setModel(modelId: String) {
        _activeModel.value = modelId
    }

    fun clearError() {
        _lastError.value = null
    }

    /**
     * Full send path. Returns the AI message (or error message) so the
     * caller can speak/render it. Persists both sides via SessionManager.
     */
    suspend fun send(
        userText: String,
        imageUri: String?,
        apiKey: String,
        systemPrompt: String,
        responseLanguage: String,
        context: Context
    ): ChatMessage {
        val userMessage = ChatMessage(content = userText, isUser = true, imageUri = imageUri)
        session.addMessage(userMessage)

        if (apiKey.isBlank()) {
            val err = ChatMessage(
                content = "کلید API ان‌ویدیا تنظیم نشده است. لطفاً از تنظیمات (⚙️) کلید رایگان build.nvidia.com را وارد کنید.",
                isUser = false,
                isError = true
            )
            session.addMessage(err)
            _lastError.value = err.content
            return err
        }

        _isThinking.value = true
        _lastError.value = null
        try {
            val history = session.messages.value
            val result = agent.run(
                conversationId = session.currentId.value,
                apiKey = apiKey,
                model = _activeModel.value,
                history = history,
                systemPrompt = systemPrompt,
                responseLanguage = responseLanguage,
                context = context,
                onModelFallback = { fb -> _activeModel.value = fb }
            )
            return result.fold(
                onSuccess = { r ->
                    val ai = ChatMessage(
                        content = r.text,
                        isUser = false,
                        modelUsed = r.modelUsed,
                        latencyMs = r.latencyMs
                    )
                    session.addMessage(ai)
                    _aiVoiceText.value = r.text
                    ai
                },
                onFailure = { e ->
                    val err = ChatMessage(
                        content = "خطا در پاسخ هوش مصنوعی: ${e.localizedMessage}",
                        isUser = false,
                        isError = true
                    )
                    session.addMessage(err)
                    _lastError.value = err.content
                    err
                }
            )
        } finally {
            _isThinking.value = false
        }
    }

    fun newChat() = session.createNew()
    fun switchTo(id: String) = session.switchTo(id)
    fun delete(id: String) = session.delete(id)
    fun clearCurrent() = session.clearCurrent()
}
