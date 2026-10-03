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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.Surface
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

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        val screenHeight = maxHeight
        val orbSize = (screenHeight * 0.26f).coerceIn(120.dp, 200.dp)

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // TOP HEADER (Close button + Title)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "مکالمه صوتی زنده NeMo",
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "گفتگوی دوطرفه بلادرنگ با هوش مصنوعی",
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
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.5f))

            // CENTER GLOWING VOICE ORB
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(orbSize * 1.3f)
            ) {
                val activeScale = when {
                    isListening -> (1.0f + rmsLevel * 0.35f)
                    isSpeaking -> pulseScale
                    isThinking -> pulseScale * 1.05f
                    else -> 1.0f
                }

                // Outer Aura
                Box(
                    modifier = Modifier
                        .size(orbSize * 1.25f)
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
                        .size(orbSize)
                        .scale(if (isListening) (1f + rmsLevel * 0.15f) else 1f)
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
                        .size(orbSize * 0.65f)
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
                        contentDescription = null,
                        tint = TextPrimary,
                        modifier = Modifier.size(orbSize * 0.28f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // State Title
            val stateText = when {
                isListening -> "در حال شنیدن صدای شما..."
                isThinking -> "نِمو در حال اندیشیدن..."
                isSpeaking -> "نِمو در حال پاسخ دادن..."
                else -> "برای صحبت کردن دکمه میکروفون را لمس کنید"
            }

            Text(
                text = stateText,
                color = when {
                    isListening -> NvidiaNeon
                    isThinking -> Color(0xFF40C4FF)
                    isSpeaking -> NvidiaGreen
                    else -> TextSecondary
                },
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Waveform equalizer bars
            AudioWaveform(
                isListening = isListening,
                isSpeaking = isSpeaking,
                rmsLevel = rmsLevel,
                barCount = 11,
                modifier = Modifier.height(30.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Spoken Transcription or AI Answer Card
            val displayText = when {
                spokenText.isNotBlank() && isListening -> "شما: $spokenText"
                aiResponseText.isNotBlank() && isSpeaking -> "نِمو: $aiResponseText"
                else -> ""
            }

            if (displayText.isNotBlank()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF141C2B),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                ) {
                    Text(
                        text = displayText,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(12.dp),
                        maxLines = 3
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // BOTTOM CONTROL ROW (Spacious, easy to tap)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stop Button if AI is speaking
                if (isSpeaking) {
                    IconButton(
                        onClick = onStopSpeaking,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF442727))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "قطع صدای پاسخ",
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(24.dp))
                }

                // Main Mic Button (Spacious 64dp)
                IconButton(
                    onClick = onMicToggle,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(if (isListening) NvidiaGreen else Color(0xFF1E2838))
                        .border(2.dp, if (isListening) NvidiaNeon else DarkBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = "کنترل میکروفون",
                        tint = if (isListening) DarkBackground else TextPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}
