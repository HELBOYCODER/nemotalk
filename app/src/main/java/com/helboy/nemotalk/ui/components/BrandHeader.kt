package com.helboy.nemotalk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.helboy.nemotalk.ui.theme.Dimens
import com.helboy.nemotalk.ui.theme.NvidiaGreen
import com.helboy.nemotalk.ui.theme.NvidiaNeon

/**
 * Minis-grade brand header — avatar + title + status dot.
 * Extracted from ChatScreen's inline top bar so HUD/drawer reuse it.
 */
@Composable
fun BrandHeader(
    icon: ImageVector,
    title: String,
    hasApiKey: Boolean,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(Dimens.avatarSize)
                .clip(CircleShape)
                .background(androidx.compose.ui.graphics.Color(0xFF142417))
                .border(1.5.dp, NvidiaGreen, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = NvidiaNeon,
                modifier = Modifier.size(Dimens.iconSm)
            )
        }
        androidx.compose.foundation.layout.Spacer(
            modifier = Modifier.padding(Dimens.xs)
        )
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleMedium
        )
        androidx.compose.foundation.layout.Spacer(
            modifier = Modifier.padding(Dimens.xs)
        )
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(
                    if (hasApiKey) NvidiaNeon
                    else androidx.compose.ui.graphics.Color(0xFFFFB300)
                )
        )
    }
}
