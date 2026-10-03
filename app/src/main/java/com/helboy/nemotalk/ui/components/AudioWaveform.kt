package com.helboy.nemotalk.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.helboy.nemotalk.ui.theme.NvidiaGreen
import com.helboy.nemotalk.ui.theme.NvidiaNeon

@Composable
fun AudioWaveform(
    isListening: Boolean,
    isSpeaking: Boolean,
    rmsLevel: Float = 0f,
    modifier: Modifier = Modifier,
    barCount: Int = 9,
    baseColor: Color = NvidiaGreen
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")

    val anim1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "anim1"
    )

    val anim2 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, delayMillis = 100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "anim2"
    )

    val anim3 by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(480, delayMillis = 200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "anim3"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val anims = listOf(anim1, anim2, anim3, anim2, anim1, anim3, anim2, anim1, anim3)

        for (i in 0 until barCount) {
            val scale = when {
                isListening -> (0.2f + rmsLevel * 0.8f * (1f - kotlin.math.abs(i - barCount / 2f) / barCount)).coerceIn(0.15f, 1f)
                isSpeaking -> anims[i % anims.size]
                else -> 0.15f
            }

            val heightDp = (42 * scale).coerceAtLeast(6f).dp
            val color = if (i == barCount / 2) NvidiaNeon else baseColor

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(heightDp)
                    .background(color, RoundedCornerShape(2.dp))
            )
        }
    }
}
