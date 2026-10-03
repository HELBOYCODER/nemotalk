package com.helboy.nemotalk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.helboy.nemotalk.model.ChatMessage
import com.helboy.nemotalk.ui.theme.AiBubble
import com.helboy.nemotalk.ui.theme.DarkBorder
import com.helboy.nemotalk.ui.theme.ErrorRed
import com.helboy.nemotalk.ui.theme.NvidiaGreen
import com.helboy.nemotalk.ui.theme.NvidiaNeon
import com.helboy.nemotalk.ui.theme.TextMuted
import com.helboy.nemotalk.ui.theme.TextPrimary
import com.helboy.nemotalk.ui.theme.TextSecondary
import com.helboy.nemotalk.ui.theme.UserBubble
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MessageBubble(
    message: ChatMessage,
    isSpeakingThis: Boolean,
    onSpeakClick: () -> Unit,
    onStopSpeakClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(message.timestamp))

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!message.isUser) {
            // NeMo AI Avatar
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF142417))
                    .border(1.dp, NvidiaGreen, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = "NeMo AI",
                    tint = NvidiaNeon,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        // Message Content Bubble
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (message.isUser) 16.dp else 4.dp,
                        bottomEnd = if (message.isUser) 4.dp else 16.dp
                    )
                )
                .background(
                    when {
                        message.isError -> Color(0xFF2A1515)
                        message.isUser -> UserBubble
                        else -> AiBubble
                    }
                )
                .border(
                    width = 1.dp,
                    color = when {
                        message.isError -> ErrorRed.copy(alpha = 0.5f)
                        message.isUser -> Color(0xFF2E3E5C)
                        else -> DarkBorder
                    },
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (message.isUser) 16.dp else 4.dp,
                        bottomEnd = if (message.isUser) 4.dp else 16.dp
                    )
                )
                .padding(12.dp)
        ) {
            // Header info for AI messages
            if (!message.isUser && !message.isError) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "NVIDIA NeMo",
                        color = NvidiaGreen,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp
                    )

                    if (message.latencyMs != null) {
                        Text(
                            text = "⚡ ${message.latencyMs}ms",
                            color = TextMuted,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Message text
            Text(
                text = message.content,
                color = if (message.isError) ErrorRed else TextPrimary,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // Bottom bar with timestamp & action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedTime,
                    color = TextMuted,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Copy button
                    IconButton(
                        onClick = { clipboardManager.setText(AnnotatedString(message.content)) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "کپی متن",
                            tint = TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    // TTS Speech button for AI responses
                    if (!message.isUser && !message.isError) {
                        IconButton(
                            onClick = {
                                if (isSpeakingThis) onStopSpeakClick() else onSpeakClick()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (isSpeakingThis) Icons.Default.Stop else Icons.Default.GraphicEq,
                                contentDescription = if (isSpeakingThis) "توقف خواندن" else "خواندن با صدا",
                                tint = if (isSpeakingThis) NvidiaNeon else TextSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }

        if (message.isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            // User Avatar
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E2838))
                    .border(1.dp, Color(0xFF384A68), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "کاربر",
                    tint = Color(0xFF90CAF9),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
