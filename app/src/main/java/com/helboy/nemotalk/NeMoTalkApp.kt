package com.helboy.nemotalk

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.helboy.nemotalk.data.ChatDatabase
import com.helboy.nemotalk.data.PreferencesManager
import com.helboy.nemotalk.model.ChatMessage
import com.helboy.nemotalk.model.Conversation
import com.helboy.nemotalk.network.NvidiaApiClient
import com.helboy.nemotalk.speech.SpeechRecognitionHelper
import com.helboy.nemotalk.speech.TextToSpeechHelper
import com.helboy.nemotalk.ui.components.ChatListDrawer
import com.helboy.nemotalk.ui.screens.ChatScreen
import com.helboy.nemotalk.ui.screens.SettingsScreen
import com.helboy.nemotalk.ui.screens.VoiceHudScreen
import com.helboy.nemotalk.ui.theme.DarkBackground
import com.helboy.nemotalk.ui.theme.NeMoTalkTheme
import kotlinx.coroutines.launch

enum class AppScreen {
    CHAT,
    VOICE_HUD,
    SETTINGS
}

@Composable
fun NeMoTalkApp() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val prefs = remember { PreferencesManager(context) }
    val apiClient = remember { NvidiaApiClient() }
    val speechHelper = remember { SpeechRecognitionHelper(context) }
    val ttsHelper = remember { TextToSpeechHelper(context) }
    val database = remember { ChatDatabase(context) }

    // Apply the in-app proxy (ZeroNet/Zray on 127.0.0.1) to every NeMoTalk
    // client the moment preferences load or the proxy settings change.
    LaunchedEffect(prefs.proxyEnabled, prefs.proxyType, prefs.proxyAddress, prefs.proxyPort, prefs.proxyHttpPort) {
        apiClient.updateProxySettings(prefs)
        ttsHelper.geminiClient.updateProxySettings(prefs)
    }

    var currentScreen by remember { mutableStateOf(AppScreen.CHAT) }
    val messages = remember { mutableStateListOf<ChatMessage>() }
    val conversations = remember { mutableStateListOf<Conversation>() }
    var currentConversationId by remember { mutableStateOf("") }
    var showChatDrawer by remember { mutableStateOf(false) }

    var isThinking by remember { mutableStateOf(false) }
    var currentlySpeakingId by remember { mutableStateOf<String?>(null) }
    var aiResponseVoiceText by remember { mutableStateOf("") }

    val isListening by speechHelper.isListening.collectAsState()
    val rmsLevel by speechHelper.rmsDb.collectAsState()
    val partialSpokenText by speechHelper.partialText.collectAsState()
    val isSpeaking by ttsHelper.isSpeaking.collectAsState()

    var activeModelId by remember { mutableStateOf(prefs.selectedModel) }

    // Permission launcher for Microphone
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "برای گفتگوی صوتی، دسترسی به میکروفون الزامی است.", Toast.LENGTH_LONG).show()
        }
    }

    fun hasRecordPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    // ---------- Conversation Management (chat memory & chat list) ----------
    fun updateConversationLastMessage(convId: String, lastMsg: String) {
        try {
            val conv = conversations.firstOrNull { it.id == convId }
            if (conv != null) {
                val updated = conv.copy(lastMessage = lastMsg, lastMessageTime = System.currentTimeMillis())
                val idx = conversations.indexOf(conv)
                conversations[idx] = updated
                database.updateConversation(updated)
            }
        } catch (_: Exception) {
        }
    }

    fun createNewConversation() {
        val newConv = Conversation(
            id = java.util.UUID.randomUUID().toString(),
            title = "گفتگوی جدید",
            createdAt = System.currentTimeMillis()
        )
        database.insertConversation(newConv)
        conversations.clear()
        conversations.addAll(database.getAllConversations())
        currentConversationId = newConv.id
        messages.clear()
    }

    fun switchToConversation(convId: String) {
        currentConversationId = convId
        messages.clear()
        messages.addAll(database.getMessagesForConversation(convId))
    }

    // Load conversations on startup
    LaunchedEffect(Unit) {
        val allConvs = database.getAllConversations()
        conversations.clear()
        conversations.addAll(allConvs)
        if (allConvs.isEmpty()) {
            createNewConversation()
        } else {
            currentConversationId = allConvs.first().id
            messages.clear()
            messages.addAll(database.getMessagesForConversation(allConvs.first().id))
        }
    }
    // Function to send message to NVIDIA NeMo API with Persian enforcement & auto-fallback
    fun handleSendMessage(userText: String, isFromVoice: Boolean = false, imageUri: String? = null) {
        if (userText.isBlank() && imageUri == null) return

        val userMessage = ChatMessage(content = userText, isUser = true, imageUri = imageUri)
        messages.add(userMessage)
        // Persist user message (chat memory)
        database.insertMessage(currentConversationId, userMessage)
        updateConversationLastMessage(currentConversationId, if (imageUri != null && userText.isBlank()) "🖼️ تصویر" else userText)

        if (prefs.apiKey.isBlank()) {
            val errorMsg = ChatMessage(
                content = "کلید API ان‌ویدیا تنظیم نشده است. لطفاً از طریق آیکون تنظیمات (⚙️) در بالای صفحه، کلید رایگان خود را از build.nvidia.com وارد کنید.",
                isUser = false,
                isError = true
            )
            messages.add(errorMsg)
            return
        }

        isThinking = true
        coroutineScope.launch {
            val result = apiClient.sendMessage(
                apiKey = prefs.apiKey,
                model = activeModelId,
                messages = messages,
                systemPrompt = prefs.systemPrompt,
                responseLanguage = prefs.responseLanguage,
                currentContext = context
            )

            isThinking = false
            result.fold(
                onSuccess = { sendResult ->
                    val aiText = sendResult.content
                    val latency = sendResult.latencyMs
                    val usedModel = sendResult.modelUsed

                    // If auto-fallback triggered, update active model smoothly
                    if (sendResult.wasFallback) {
                        activeModelId = usedModel
                        prefs.selectedModel = usedModel
                    }

                    val aiMessage = ChatMessage(
                        content = aiText,
                        isUser = false,
                        modelUsed = usedModel,
                        latencyMs = latency
                    )
                    messages.add(aiMessage)
                    // Persist to database (chat memory)
                    database.insertMessage(currentConversationId, aiMessage)
                    updateConversationLastMessage(currentConversationId, aiText)
                    aiResponseVoiceText = aiText

                    // Speak out automatically if enabled or in Voice HUD mode
                    if (prefs.autoSpeak || isFromVoice || currentScreen == AppScreen.VOICE_HUD) {
                        currentlySpeakingId = aiMessage.id
                        ttsHelper.speak(
                            text = aiText,
                            prefs = prefs,
                            forcePersian = (prefs.responseLanguage == "fa" || ttsHelper.containsPersianCharacters(aiText)),
                            onDone = {
                                currentlySpeakingId = null
                                // If still in Voice HUD, re-listen for continuous conversation!
                                if (currentScreen == AppScreen.VOICE_HUD && hasRecordPermission()) {
                                    speechHelper.startListening(
                                        language = prefs.speechLanguage,
                                        onFinalResult = { text -> handleSendMessage(text, isFromVoice = true) },
                                        onError = {}
                                    )
                                }
                            }
                        )
                    }
                },
                onFailure = { error ->
                    val errorMessage = ChatMessage(
                        content = "خطا در پاسخ هوش مصنوعی: ${error.localizedMessage}",
                        isUser = false,
                        isError = true
                    )
                    messages.add(errorMessage)
                }
            )
        }
    }

    fun startListeningWithPermission(fromVoiceHud: Boolean = false) {
        if (!hasRecordPermission()) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        ttsHelper.stop()
        currentlySpeakingId = null

        speechHelper.startListening(
            language = prefs.speechLanguage,
            onFinalResult = { recognizedText ->
                handleSendMessage(recognizedText, isFromVoice = fromVoiceHud)
            },
            onError = { errMsg ->
                Toast.makeText(context, errMsg, Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Cleanup resources on disposal
    DisposableEffect(Unit) {
        onDispose {
            speechHelper.stopListening()
            ttsHelper.shutdown()
        }
    }

    NeMoTalkTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = DarkBackground
        ) {
            when (currentScreen) {
                AppScreen.CHAT -> {
                    ChatScreen(
                        messages = messages,
                        isThinking = isThinking,
                        isListening = isListening,
                        rmsLevel = rmsLevel,
                        currentlySpeakingId = currentlySpeakingId,
                        hasApiKey = prefs.apiKey.isNotBlank(),
                        selectedModelId = activeModelId,
                        onModelChange = { newModelId ->
                            activeModelId = newModelId
                            prefs.selectedModel = newModelId
                        },
                        onSendMessage = { text, image -> handleSendMessage(text, isFromVoice = false, imageUri = image) },
                        onVoiceHudOpen = {
                            currentScreen = AppScreen.VOICE_HUD
                            startListeningWithPermission(fromVoiceHud = true)
                        },
                        onMicQuickToggle = {
                            if (isListening) {
                                speechHelper.stopListening()
                            } else {
                                startListeningWithPermission(fromVoiceHud = false)
                            }
                        },
                        onNewChat = {
                            ttsHelper.stop()
                            currentlySpeakingId = null
                            createNewConversation()
                        },
                        onOpenChatList = {
                            showChatDrawer = true
                        },
                        onSpeakMessage = { msg ->
                            ttsHelper.stop()
                            currentlySpeakingId = msg.id
                            ttsHelper.speak(
                                text = msg.content,
                                prefs = prefs,
                                forcePersian = (prefs.responseLanguage == "fa" || ttsHelper.containsPersianCharacters(msg.content)),
                                onDone = { currentlySpeakingId = null }
                            )
                        },
                        onStopSpeak = {
                            ttsHelper.stop()
                            currentlySpeakingId = null
                        },
                        onClearChat = {
                            ttsHelper.stop()
                            currentlySpeakingId = null
                            messages.clear()
                            database.clearConversationMessages(currentConversationId)
                        },
                        onOpenSettings = {
                            currentScreen = AppScreen.SETTINGS
                        }
                    )
                }

                AppScreen.VOICE_HUD -> {
                    VoiceHudScreen(
                        isListening = isListening,
                        isThinking = isThinking,
                        isSpeaking = isSpeaking,
                        rmsLevel = rmsLevel,
                        spokenText = partialSpokenText,
                        aiResponseText = aiResponseVoiceText,
                        onMicToggle = {
                            if (isListening) {
                                speechHelper.stopListening()
                            } else {
                                startListeningWithPermission(fromVoiceHud = true)
                            }
                        },
                        onStopSpeaking = {
                            ttsHelper.stop()
                            currentlySpeakingId = null
                        },
                        onClose = {
                            speechHelper.stopListening()
                            ttsHelper.stop()
                            currentlySpeakingId = null
                            currentScreen = AppScreen.CHAT
                        }
                    )
                }

                AppScreen.SETTINGS -> {
                    SettingsScreen(
                        prefs = prefs,
                        apiClient = apiClient,
                        ttsHelper = ttsHelper,
                        onBack = {
                            activeModelId = prefs.selectedModel
                            currentScreen = AppScreen.CHAT
                        }
                    )
                }
            }
        }

        // Chat List Drawer ( swipe in from left edge )
        if (showChatDrawer) {
            ChatListDrawer(
                conversations = conversations,
                currentConversationId = currentConversationId,
                onSwitchConversation = { convId ->
                    ttsHelper.stop()
                    currentlySpeakingId = null
                    switchToConversation(convId)
                    showChatDrawer = false
                },
                onNewChat = {
                    createNewConversation()
                    showChatDrawer = false
                },
                onDeleteConversation = { convId ->
                    database.deleteConversation(convId)
                    if (convId == currentConversationId) {
                        val remaining = database.getAllConversations()
                        if (remaining.isNotEmpty()) {
                            switchToConversation(remaining.first().id)
                        } else {
                            createNewConversation()
                        }
                    }
                    conversations.clear()
                    conversations.addAll(database.getAllConversations())
                },
                onDismiss = { showChatDrawer = false }
            )
        }
    }
}
