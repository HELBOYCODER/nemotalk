package com.helboy.nemotalk.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.helboy.nemotalk.ui.components.AudioWaveform
import com.helboy.nemotalk.ui.theme.DarkBackground
import com.helboy.nemotalk.ui.theme.DarkBorder
import com.helboy.nemotalk.ui.theme.DarkSurfaceElevated
import com.helboy.nemotalk.ui.theme.NvidiaGreen
import com.helboy.nemotalk.ui.theme.NvidiaNeon
import com.helboy.nemotalk.ui.theme.TextMuted
import com.helboy.nemotalk.ui.theme.TextPrimary
import com.helboy.nemotalk.ui.theme.TextSecondary

/**
 * Extracted from ChatScreen (lines 359-507): live wave strip + input bar.
 * State hoisted: caller owns inputText/pendingImageUri via callbacks.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecordingWaveStrip(
    isListening: Boolean,
    rmsLevel: Float,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(visible = isListening, modifier = modifier) {
        Surface(
            color = Color(0xFF0F2215),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = NvidiaNeon,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "در حال شنیدن صدای شما...",
                        color = NvidiaNeon,
                        fontSize = 12.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                    )
                }

                AudioWaveform(
                    isListening = true,
                    isSpeaking = false,
                    rmsLevel = rmsLevel,
                    barCount = 7,
                    modifier = Modifier.height(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatInputBar(
    inputText: String,
    onInputChange: (String) -> Unit,
    pendingImageUri: String?,
    isListening: Boolean,
    onPickImage: () -> Unit,
    onClearImage: () -> Unit,
    onSend: () -> Unit,
    onMicQuickToggle: () -> Unit,
    onVoiceHudOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(DarkSurfaceElevated)
                .border(1.dp, DarkBorder, RoundedCornerShape(28.dp))
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1B2A20))
                    .border(1.dp, DarkBorder, CircleShape)
                    .combinedClickable(
                        onClick = onPickImage,
                        onLongClick = onClearImage
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (pendingImageUri != null) Icons.Default.CheckCircle else Icons.Default.AddPhotoAlternate,
                    contentDescription = if (pendingImageUri != null) "تصویر انتخاب شد (لمس طولانی برای حذف)" else "افزودن تصویر",
                    tint = if (pendingImageUri != null) NvidiaNeon else TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(2.dp))

            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChange,
                placeholder = {
                    Text(
                        text = if (pendingImageUri != null) "توضیح تصویر را بنویسید (اختیاری)..." else "پیام خود را بنویسید...",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = NvidiaGreen
                ),
                maxLines = 4,
                singleLine = false
            )

            if (inputText.isNotBlank() || pendingImageUri != null) {
                IconButton(
                    onClick = onSend,
                    modifier = Modifier
                        .size(46.dp)
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
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(if (isListening) NvidiaNeon else Color(0xFF1E2838))
                        .border(1.5.dp, if (isListening) NvidiaGreen else DarkBorder, CircleShape)
                        .combinedClickable(
                            onClick = onMicQuickToggle,
                            onLongClick = onVoiceHudOpen
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "میکروفون (لمس: گفتار، لمس طولانی: مکالمه صوتی)",
                        tint = if (isListening) DarkBackground else NvidiaGreen,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
