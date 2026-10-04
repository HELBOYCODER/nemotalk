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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.helboy.nemotalk.data.ChatDatabase
import com.helboy.nemotalk.data.PreferencesManager
import com.helboy.nemotalk.network.NvidiaApiClient
import com.helboy.nemotalk.sandbox.SandboxManager
import com.helboy.nemotalk.speech.SpeechRecognitionHelper
import com.helboy.nemotalk.speech.TextToSpeechHelper
import com.helboy.nemotalk.ui.components.ChatListDrawer
import com.helboy.nemotalk.ui.chat.ChatScreenV3
import com.helboy.nemotalk.ui.screens.SandboxScreen
import com.helboy.nemotalk.ui.screens.SettingsScreen
import com.helboy.nemotalk.ui.screens.VoiceHudScreen
import com.helboy.nemotalk.ui.state.ChatViewModel
import com.helboy.nemotalk.ui.theme.DarkBackground
import com.helboy.nemotalk.ui.theme.NeMoTalkTheme
import kotlinx.coroutines.launch

enum class AppScreen {
    CHAT,
    VOICE_HUD,
    SETTINGS,
    SANDBOX
}

/**
 * Minis-grade app root — thin orchestrator, fat domain.
 * State lives in ChatViewModel/SessionManager, send path in AgentLoop.
 * This file only wires: permissions, speech helpers, proxy, navigation.
 */
@Composable
fun NeMoTalkApp() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val prefs = remember { PreferencesManager(context) }
    val apiClient = remember { NvidiaApiClient() }
    val speechHelper = remember { SpeechRecognitionHelper(context) }
    val ttsHelper = remember { TextToSpeechHelper(context) }
    val database = remember { ChatDatabase(context) }
    val sandbox = remember { SandboxManager(context) }
    val vm = remember { ChatViewModel(database, apiClient) }

    // Apply the in-app proxy (ZeroNet/Zray on 127.0.0.1) to every NeMoTalk
    // client the moment preferences load or the proxy settings change.
    LaunchedEffect(prefs.proxyEnabled, prefs.proxyType, prefs.proxyAddress, prefs.proxyPort, prefs.proxyHttpPort) {
        apiClient.updateProxySettings(prefs)
        ttsHelper.geminiClient.updateProxySettings(prefs)
    }

    // Load sessions once, seed active model from prefs
    LaunchedEffect(Unit) {
        vm.load(prefs.selectedModel)
    }

    val conversations by vm.session.conversations.collectAsState()
    val currentConversationId by vm.session.currentId.collectAsState()
    val messages by vm.session.messages.collectAsState()
    val isThinking by vm.isThinking.collectAsState()
    val activeModelId by vm.activeModel.collectAsState()
    val aiResponseVoiceText by vm.aiVoiceText.collectAsState()

    var currentScreen by remember { mutableStateOf(AppScreen.CHAT) }
    var showChatDrawer by remember { mutableStateOf(false) }
    var currentlySpeakingId by remember { mutableStateOf<String?>(null) }

    val isListening by speechHelper.isListening.collectAsState()
    val rmsLevel by speechHelper.rmsDb.collectAsState()
    val partialSpokenText by speechHelper.partialText.collectAsState()
    val isSpeaking by ttsHelper.isSpeaking.collectAsState()

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

    // Send path: ViewModel (SessionManager + AgentLoop), then speak
    fun handleSendMessage(userText: String, isFromVoice: Boolean = false, imageUri: String? = null) {
        if (userText.isBlank() && imageUri == null) return
        coroutineScope.launch {
            val aiMessage = vm.send(
                userText = userText,
                imageUri = imageUri,
                apiKey = prefs.apiKey,
                systemPrompt = prefs.systemPrompt,
                responseLanguage = prefs.responseLanguage,
                context = context
            )
            // Persist fallback model choice
            if (activeModelId.isNotBlank()) prefs.selectedModel = activeModelId

            // Speak out automatically if enabled or in Voice HUD mode
            if (!aiMessage.isError &&
                (prefs.autoSpeak || isFromVoice || currentScreen == AppScreen.VOICE_HUD)
            ) {
                currentlySpeakingId = aiMessage.id
                ttsHelper.speak(
                    text = aiMessage.content,
                    prefs = prefs,
                    forcePersian = (prefs.responseLanguage == "fa" || ttsHelper.containsPersianCharacters(aiMessage.content)),
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
                    var showModelSheet by remember { mutableStateOf(false) }
                    ChatScreenV3(
                        messages = messages,
                        conversations = conversations,
                        isThinking = isThinking,
                        isListening = isListening,
                        rmsLevel = rmsLevel,
                        hasApiKey = prefs.apiKey.isNotBlank(),
                        selectedModelId = activeModelId.ifBlank { prefs.selectedModel },
                        onOpenChatList = { showChatDrawer = true },
                        onShowModelSheet = { showModelSheet = true },
                        onNewChat = {
                            ttsHelper.stop()
                            currentlySpeakingId = null
                            vm.newChat()
                        },
                        onVoiceHudOpen = {
                            currentScreen = AppScreen.VOICE_HUD
                            startListeningWithPermission(fromVoiceHud = true)
                        },
                        onOpenSettings = { currentScreen = AppScreen.SETTINGS },
                        onSend = { text, image -> handleSendMessage(text, isFromVoice = false, imageUri = image) },
                        onPromptSelect = { prompt -> handleSendMessage(prompt, isFromVoice = false) },
                        onMicQuickToggle = {
                            if (isListening) speechHelper.stopListening()
                            else startListeningWithPermission(fromVoiceHud = false)
                        },
                        onModelSelect = { newModelId ->
                            vm.setModel(newModelId)
                            prefs.selectedModel = newModelId
                        },
                        onClearCurrentChat = {
                            ttsHelper.stop()
                            currentlySpeakingId = null
                            vm.clearCurrent()
                        },
                        onSpeakText = { msg ->
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
                        currentlySpeakingId = currentlySpeakingId,
                        showModelSheet = showModelSheet,
                        onDismissModelSheet = { showModelSheet = false }
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
                            vm.setModel(prefs.selectedModel)
                            currentScreen = AppScreen.CHAT
                        },
                        onOpenSandbox = {
                            currentScreen = AppScreen.SANDBOX
                        }
                    )
                }

                AppScreen.SANDBOX -> {
                    SandboxScreen(
                        sandbox = sandbox,
                        database = database,
                        onBack = { currentScreen = AppScreen.SETTINGS }
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
                    vm.switchTo(convId)
                    showChatDrawer = false
                },
                onNewChat = {
                    vm.newChat()
                    showChatDrawer = false
                },
                onDeleteConversation = { convId ->
                    vm.delete(convId)
                },
                onDismiss = { showChatDrawer = false }
            )
        }
    }
}
