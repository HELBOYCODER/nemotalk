package com.helboy.nemotalk.domain

import com.helboy.nemotalk.data.ChatDatabase
import com.helboy.nemotalk.model.ChatMessage
import com.helboy.nemotalk.model.Conversation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Minis-grade SessionManager — each conversation is an isolated session
 * with its own message stream. Mirrors Minis' session/ lifecycle:
 * create / switch / delete / persist. No god-file state in NeMoTalkApp.
 */
class SessionManager(private val db: ChatDatabase) {

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private val _currentId = MutableStateFlow("")
    val currentId: StateFlow<String> = _currentId.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    fun load() {
        val all = db.getAllConversations()
        _conversations.value = all
        if (all.isEmpty()) {
            createNew()
        } else {
            switchTo(all.first().id)
        }
    }

    fun createNew(): String {
        val conv = Conversation(
            id = UUID.randomUUID().toString(),
            title = "گفتگوی جدید",
            createdAt = System.currentTimeMillis()
        )
        db.insertConversation(conv)
        _conversations.value = db.getAllConversations()
        switchTo(conv.id)
        return conv.id
    }

    fun switchTo(id: String) {
        _currentId.value = id
        _messages.value = db.getMessagesForConversation(id)
    }

    fun addMessage(msg: ChatMessage) {
        val id = _currentId.value
        if (id.isBlank()) return
        db.insertMessage(id, msg)
        // update in-memory
        _messages.value = _messages.value + msg
        // bump conversation preview
        val conv = _conversations.value.firstOrNull { it.id == id } ?: return
        val updated = conv.copy(
            lastMessage = msg.content.ifBlank { if (msg.imageUri != null) "🖼️ تصویر" else "" },
            lastMessageTime = System.currentTimeMillis()
        )
        db.updateConversation(updated)
        _conversations.value = db.getAllConversations()
    }

    fun clearCurrent() {
        val id = _currentId.value
        if (id.isBlank()) return
        db.clearConversationMessages(id)
        _messages.value = emptyList()
        _conversations.value = db.getAllConversations()
    }

    fun delete(id: String) {
        db.deleteConversation(id)
        val remaining = db.getAllConversations()
        _conversations.value = remaining
        if (id == _currentId.value) {
            if (remaining.isNotEmpty()) switchTo(remaining.first().id)
            else createNew()
        }
    }

    fun refresh() {
        _conversations.value = db.getAllConversations()
        val id = _currentId.value
        if (id.isNotBlank()) _messages.value = db.getMessagesForConversation(id)
    }
}
