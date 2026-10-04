package com.helboy.nemotalk.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically

// ── Minis-grade motion tokens ───────────────────────────────
object Motion {
    val EasingStandard = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val EasingEmphasized = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val EasingDecelerate = CubicBezierEasing(0.0f, 0.0f, 0.0f, 1.0f)

    const val DurationShort = 150
    const val DurationMedium = 300
    const val DurationLong = 450
    const val DurationExtraLong = 700

    // Reusable enter/exit for chat bubbles
    val bubbleEnter = fadeIn(tween(DurationMedium, easing = EasingStandard)) +
        slideInVertically(tween(DurationMedium, easing = EasingStandard)) { it / 6 }
    val bubbleExit = fadeOut(tween(DurationShort))

    val sheetEnter = fadeIn(tween(DurationMedium)) +
        slideInVertically(tween(DurationMedium, easing = EasingDecelerate)) { it }
    val sheetExit = fadeOut(tween(DurationShort)) +
        slideOutVertically(tween(DurationShort)) { it / 2 }

    val shimmerDuration = 1200
    val shimmerDelay = 300
}
