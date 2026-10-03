package com.helboy.nemotalk.model

import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val modelUsed: String? = null,
    val latencyMs: Long? = null,
    val isError: Boolean = false
)
