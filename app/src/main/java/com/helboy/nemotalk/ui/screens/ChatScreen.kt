package com.helboy.nemotalk.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.helboy.nemotalk.model.ChatMessage
import com.helboy.nemotalk.model.NvidiaModel
import com.helboy.nemotalk.ui.components.AudioWaveform
import com.helboy.nemotalk.ui.components.MessageBubble
import com.helboy.nemotalk.ui.theme.DarkBackground
import com.helboy.nemotalk.ui.theme.DarkBorder
import com.helboy.nemotalk.ui.theme.DarkSurface
import com.helboy.nemotalk.ui.theme.DarkSurfaceElevated
import com.helboy.nemotalk.ui.theme.NvidiaGreen
import com.helboy.nemotalk.ui.theme.NvidiaNeon
import com.helboy.nemotalk.ui.theme.TextMuted
import com.helboy.nemotalk.ui.theme.TextPrimary
import com.helboy.nemotalk.ui.theme.TextSecondary

@Composable
fun ChatScreen(
    messages: List<ChatMessage>,
    isThinking: Boolean,
    isListening: Boolean,
    rmsLevel: Float,
    currentlySpeakingId: String?,
    hasApiKey: Boolean,
    selectedModelId: String,
    onSendMessage: (String) -> Unit,
    onVoiceHudOpen: () -> Unit,
    onMicQuickToggle: () -> Unit,
    onSpeakMessage: (ChatMessage) -> Unit,
    onStopSpeak: () -> Unit,
    onClearChat: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll on new message
    LaunchedEffect(messages.size, isThinking) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val currentModel = remember(selectedModelId) { NvidiaModel.findById(selectedModelId) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .imePadding()
    ) {
        // TOP APP BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Branding & Status Dot
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF142417))
                        .border(1.dp, NvidiaGreen, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = "NeMo",
                        tint = NvidiaNeon,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "NeMoTalk",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        // Status indicator dot
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (hasApiKey) NvidiaNeon else Color(0xFFFFB300))
                        )
                    }

                    Text(
                        text = currentModel.displayName,
                        color = NvidiaGreen,
                        fontSize = 11.sp
                    )
                }
            }

            // Top Action Buttons
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Fullscreen Voice Mode Button
                IconButton(
                    onClick = onVoiceHudOpen,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF15291C))
                        .border(1.dp, NvidiaGreen.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.RecordVoiceOver,
                        contentDescription = "مکالمه صوتی زنده",
                        tint = NvidiaNeon,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Clear Chat Button
                if (messages.isNotEmpty()) {
                    IconButton(
                        onClick = onClearChat,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "پاکسازی گفتگو",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Settings Button
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "تنظیمات",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // MISSING API KEY WARNING BANNER
        AnimatedVisibility(visible = !hasApiKey) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2C1F0E))
                    .clickable { onOpenSettings() }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "هشدار",
                    tint = Color(0xFFFFB300),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "کلید رایگان ان‌ویدیا تنظیم نشده است. لمس برای ثبت در تنظیمات",
                    color = Color(0xFFFFE082),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // CHAT MESSAGES LIST
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (messages.isEmpty()) {
                // Empty State / Welcome Hero Card
                EmptyChatWelcome(
                    onPromptSelect = { prompt ->
                        onSendMessage(prompt)
                    },
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        MessageBubble(
                            message = message,
                            isSpeakingThis = (currentlySpeakingId == message.id),
                            onSpeakClick = { onSpeakMessage(message) },
                            onStopSpeakClick = onStopSpeak
                        )
                    }

                    // Thinking Indicator
                    if (isThinking) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF142417)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = NvidiaNeon,
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "نِمو در حال پردازش...",
                                    color = TextMuted,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // LIVE SPEECH BAR (WHEN MIC IS RECORDING)
        AnimatedVisibility(visible = isListening) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF101C14))
                    .border(1.dp, NvidiaGreen.copy(alpha = 0.4f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "ضبط صدا",
                        tint = NvidiaNeon,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "در حال شنیدن صدای شما...",
                        color = NvidiaNeon,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                AudioWaveform(
                    isListening = true,
                    isSpeaking = false,
                    rmsLevel = rmsLevel,
                    barCount = 7,
                    modifier = Modifier.height(20.dp)
                )
            }
        }

        // BOTTOM INPUT BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Text Input
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("پیام خود را بنویسید...", color = TextMuted, fontSize = 14.sp) },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkSurfaceElevated,
                    unfocusedContainerColor = DarkSurfaceElevated,
                    focusedBorderColor = NvidiaGreen,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                maxLines = 4,
                singleLine = false
            )

            // Send / Voice Button
            if (inputText.isNotBlank()) {
                IconButton(
                    onClick = {
                        val text = inputText.trim()
                        inputText = ""
                        onSendMessage(text)
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(NvidiaGreen)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "ارسال",
                        tint = DarkBackground,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                IconButton(
                    onClick = onMicQuickToggle,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isListening) NvidiaNeon else Color(0xFF1E2838))
                        .border(1.dp, if (isListening) NvidiaGreen else DarkBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "میکروفون صوتی",
                        tint = if (isListening) DarkBackground else NvidiaGreen,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyChatWelcome(
    onPromptSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color(0xFF122316))
                .border(2.dp, NvidiaGreen, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = NvidiaNeon,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "به آوانمو (NeMoTalk) خوش آمدید",
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "دستیار صوتی و متنی مجهز به هوش مصنوعی ان‌ویدیا\nبرای شروع می‌توانید صحبت کنید یا یکی از گزینه‌های زیر را انتخاب نمایید:",
            color = TextSecondary,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Quick Suggestion Chips
        val suggestions = listOf(
            "نِمو، خودت رو معرفی کن و بگو چطور کار می‌کنی؟",
            "یک داستان کوتاه و هیجان‌انگیز برام بگو",
            "معماری مدل‌های صوتی NVIDIA NeMo چگونه است؟",
            "چند ترفند عالی برای برنامه‌نویسی پایتون بگو"
        )

        suggestions.forEach { prompt ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurfaceElevated)
                    .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
                    .clickable { onPromptSelect(prompt) }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = prompt,
                    color = TextPrimary,
                    fontSize = 12.sp
                )
            }
        }
    }
}
