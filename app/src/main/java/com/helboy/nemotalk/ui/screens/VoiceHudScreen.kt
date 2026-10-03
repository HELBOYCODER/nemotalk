package com.helboy.nemotalk.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.helboy.nemotalk.ui.components.AudioWaveform
import com.helboy.nemotalk.ui.theme.DarkBackground
import com.helboy.nemotalk.ui.theme.DarkBorder
import com.helboy.nemotalk.ui.theme.NvidiaDarkGreen
import com.helboy.nemotalk.ui.theme.NvidiaGreen
import com.helboy.nemotalk.ui.theme.NvidiaNeon
import com.helboy.nemotalk.ui.theme.TextMuted
import com.helboy.nemotalk.ui.theme.TextPrimary
import com.helboy.nemotalk.ui.theme.TextSecondary

@Composable
fun VoiceHudScreen(
    isListening: Boolean,
    isThinking: Boolean,
    isSpeaking: Boolean,
    rmsLevel: Float,
    spokenText: String,
    aiResponseText: String,
    onMicToggle: () -> Unit,
    onStopSpeaking: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "hud_pulse")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp)
    ) {
        // Top Close Button & Title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "حالت مکالمه صوتی NeMo",
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "مکالمه زنده و بلادرنگ با مدل ان‌ویدیا",
                    color = TextMuted,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E2838))
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "بستن",
                    tint = TextPrimary
                )
            }
        }

        // Center Voice Orb Visualizer
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Glowing Pulsing Outer Ring
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(240.dp)
            ) {
                val activeScale = when {
                    isListening -> (1.0f + rmsLevel * 0.4f)
                    isSpeaking -> pulseScale
                    isThinking -> pulseScale * 1.05f
                    else -> 1.0f
                }

                // Outer Aura
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .scale(activeScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    NvidiaGreen.copy(alpha = if (isListening || isSpeaking || isThinking) glowAlpha * 0.4f else 0.1f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Middle Ring
                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .scale(if (isListening) (1f + rmsLevel * 0.2f) else 1f)
                        .clip(CircleShape)
                        .border(
                            width = 2.dp,
                            color = when {
                                isThinking -> NvidiaNeon.copy(alpha = glowAlpha)
                                isSpeaking -> NvidiaNeon
                                isListening -> NvidiaGreen
                                else -> DarkBorder
                            },
                            shape = CircleShape
                        )
                        .background(Color(0xFF0F1722))
                )

                // Inner Core
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = when {
                                    isThinking -> listOf(NvidiaDarkGreen, Color(0xFF00B0FF))
                                    isSpeaking -> listOf(NvidiaGreen, NvidiaNeon)
                                    isListening -> listOf(Color(0xFF1B5E20), NvidiaGreen)
                                    else -> listOf(Color(0xFF141F2D), Color(0xFF1F2E42))
                                }
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when {
                            isSpeaking -> Icons.Default.Stop
                            isListening -> Icons.Default.Mic
                            else -> Icons.Default.MicOff
                        },
                        contentDescription = "وضعیت صدا",
                        tint = TextPrimary,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // State Title
            val stateText = when {
                isListening -> "در حال شنیدن صدای شما..."
                isThinking -> "نِمو در حال اندیشیدن..."
                isSpeaking -> "نِمو در حال پاسخ دادن..."
                else -> "برای صحبت کردن روی میکروفون ضربه بزنید"
            }

            Text(
                text = stateText,
                color = when {
                    isListening -> NvidiaNeon
                    isThinking -> Color(0xFF40C4FF)
                    isSpeaking -> NvidiaGreen
                    else -> TextSecondary
                },
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Waveform equalizer bars
            AudioWaveform(
                isListening = isListening,
                isSpeaking = isSpeaking,
                rmsLevel = rmsLevel,
                barCount = 15,
                modifier = Modifier.height(44.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Display Live Transcription or AI Speech Text
            val displayText = when {
                spokenText.isNotBlank() && isListening -> "شما: $spokenText"
                aiResponseText.isNotBlank() && isSpeaking -> "نِمو: $aiResponseText"
                else -> ""
            }

            if (displayText.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF141C2B))
                        .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = displayText,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )
                }
            }
        }

        // Bottom Controls (Mic Toggle & Stop Speech)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Stop speaking button if AI is talking
            if (isSpeaking) {
                IconButton(
                    onClick = onStopSpeaking,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF442727))
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "قطع صدای پاسخ",
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Main Mic Toggle Button
            IconButton(
                onClick = onMicToggle,
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(if (isListening) NvidiaGreen else Color(0xFF1E2838))
                    .border(2.dp, if (isListening) NvidiaNeon else DarkBorder, CircleShape)
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                    contentDescription = "کنترل میکروفون",
                    tint = if (isListening) DarkBackground else TextPrimary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}
