package com.helboy.nemotalk.ui.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.helboy.nemotalk.model.ChatMessage
import com.helboy.nemotalk.model.Conversation
import com.helboy.nemotalk.ui.components.MessageBubble
import com.helboy.nemotalk.ui.components.ThinkingSkeleton
import kotlinx.coroutines.launch

/**
 * Minis-grade ChatScreen: orchestrator only (~150 lines).
 * Blocks extracted to ui/chat/: TopBar, MessageList, InputBar, Sheets.
 * State owned by ChatViewModel; this file only wires callbacks.
 */
@Composable
fun ChatScreenV3(
    messages: List<ChatMessage>,
    conversations: List<Conversation>,
    isThinking: Boolean,
    isListening: Boolean,
    rmsLevel: Float,
    hasApiKey: Boolean,
    selectedModelId: String,
    onOpenChatList: () -> Unit,
    onShowModelSheet: () -> Unit,
    onNewChat: () -> Unit,
    onVoiceHudOpen: () -> Unit,
    onOpenSettings: () -> Unit,
    onSend: (String, String?) -> Unit,
    onPromptSelect: (String) -> Unit,
    onMicQuickToggle: () -> Unit,
    onModelSelect: (String) -> Unit,
    onClearCurrentChat: () -> Unit,
    onSpeakText: (ChatMessage) -> Unit,
    onStopSpeak: () -> Unit,
    currentlySpeakingId: String?,
    showModelSheet: Boolean,
    onDismissModelSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    var inputText by remember { mutableStateOf("") }
    var pendingImageUri by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        pendingImageUri = uri?.toString()
    }

    LaunchedEffect(messages.size, isThinking) {
        if (messages.isNotEmpty()) {
            scope.launch { listState.animateScrollToItem(messages.size - 1) }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        ChatTopBar(
            hasApiKey = hasApiKey,
            selectedModelId = selectedModelId,
            onOpenChatList = onOpenChatList,
            onShowModelSheet = onShowModelSheet,
            onNewChat = onNewChat,
            onVoiceHudOpen = onVoiceHudOpen,
            onOpenSettings = onOpenSettings
        )

        MissingApiKeyBanner(
            visible = !hasApiKey,
            onOpenSettings = onOpenSettings
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (messages.isEmpty() && !isThinking) {
                EmptyChatWelcome(onPromptSelect = { prompt ->
                    onSend(prompt, null)
                })
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    items(messages, key = { it.id }) { message ->
                        MessageBubble(
                            message = message,
                            isSpeakingThis = (currentlySpeakingId == message.id),
                            onSpeakClick = { onSpeakText(message) },
                            onStopSpeakClick = onStopSpeak
                        )
                    }

                    if (isThinking) {
                        item { ThinkingSkeleton() }
                    }

                    item { Spacer(modifier = Modifier.height(6.dp)) }
                }
            }
        }

        RecordingWaveStrip(isListening = isListening, rmsLevel = rmsLevel)

        ChatInputBar(
            inputText = inputText,
            onInputChange = { inputText = it },
            pendingImageUri = pendingImageUri,
            isListening = isListening,
            onPickImage = { imagePicker.launch(arrayOf("image/*")) },
            onClearImage = { pendingImageUri = null },
            onSend = {
                onSend(inputText, pendingImageUri)
                inputText = ""
                pendingImageUri = null
            },
            onMicQuickToggle = onMicQuickToggle,
            onVoiceHudOpen = onVoiceHudOpen
        )

        ModelSelectorSheet(
            visible = showModelSheet,
            conversations = conversations,
            selectedModelId = selectedModelId,
            onModelSelect = onModelSelect,
            onClearCurrentChat = onClearCurrentChat,
            onDismiss = onDismissModelSheet
        )
    }
}
