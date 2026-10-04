package com.helboy.nemotalk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.helboy.nemotalk.ui.theme.BorderGlass
import com.helboy.nemotalk.ui.theme.Dimens
import com.helboy.nemotalk.ui.theme.ShapeGlassCard
import com.helboy.nemotalk.ui.theme.SurfaceGlass

/**
 * Minis-grade glass card — frosted surface + luminous border.
 * ponytail: no blur lib (extra dep); translucency + border reads as
 * glass on dark obsidian, upgrade to real blur later if needed.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(ShapeGlassCard)
            .background(SurfaceGlass)
            .border(1.dp, BorderGlass, ShapeGlassCard)
            .padding(Dimens.cardPadding),
        content = content
    )
}
