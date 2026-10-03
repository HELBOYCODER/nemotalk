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
import com.helboy.nemotalk.data.PreferencesManager
import com.helboy.nemotalk.model.ChatMessage
import com.helboy.nemotalk.network.NvidiaApiClient
import com.helboy.nemotalk.speech.SpeechRecognitionHelper
import com.helboy.nemotalk.speech.TextToSpeechHelper
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

    var currentScreen by remember { mutableStateOf(AppScreen.CHAT) }
    val messages = remember { mutableStateListOf<ChatMessage>() }

    var isThinking by remember { mutableStateOf(false) }
    var currentlySpeakingId by remember { mutableStateOf<String?>(null) }
    var aiResponseVoiceText by remember { mutableStateOf("") }

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

    // Function to send message to NVIDIA NeMo API
    fun handleSendMessage(userText: String, isFromVoice: Boolean = false) {
        if (userText.isBlank()) return

        val userMessage = ChatMessage(content = userText, isUser = true)
        messages.add(userMessage)

        if (prefs.apiKey.isBlank()) {
            val errorMsg = ChatMessage(
                content = "کلید API ان‌ویدیا تنظیم نشده است. لطفاً از طریق آیکون تنظیمات در بالای صفحه، کلید رایگان خود را از build.nvidia.com وارد کنید.",
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
                model = prefs.selectedModel,
                messages = messages,
                systemPrompt = prefs.systemPrompt
            )

            isThinking = false
            result.fold(
                onSuccess = { (aiText, latency) ->
                    val aiMessage = ChatMessage(
                        content = aiText,
                        isUser = false,
                        modelUsed = prefs.selectedModel,
                        latencyMs = latency
                    )
                    messages.add(aiMessage)
                    aiResponseVoiceText = aiText

                    // Speak out automatically if enabled or in Voice HUD mode
                    if (prefs.autoSpeak || isFromVoice || currentScreen == AppScreen.VOICE_HUD) {
                        currentlySpeakingId = aiMessage.id
                        ttsHelper.speak(
                            text = aiText,
                            speechRate = prefs.speechRate,
                            speechPitch = prefs.speechPitch,
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
                        selectedModelId = prefs.selectedModel,
                        onSendMessage = { text -> handleSendMessage(text, isFromVoice = false) },
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
                        onSpeakMessage = { msg ->
                            ttsHelper.stop()
                            currentlySpeakingId = msg.id
                            ttsHelper.speak(
                                text = msg.content,
                                speechRate = prefs.speechRate,
                                speechPitch = prefs.speechPitch,
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
                        onBack = { currentScreen = AppScreen.CHAT }
                    )
                }
            }
        }
    }
}
