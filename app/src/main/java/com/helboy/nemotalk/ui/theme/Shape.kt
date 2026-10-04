package com.helboy.nemotalk.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// ── Minis-grade shape system ────────────────────────────────
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

// Semantic aliases — use these in UI, not raw dp
val ShapeChip = RoundedCornerShape(999.dp)       // pill
val ShapeBubbleUser = RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)
val ShapeBubbleAi = RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)
val ShapeCard = RoundedCornerShape(16.dp)
val ShapeSheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
val ShapeGlassCard = RoundedCornerShape(20.dp)
