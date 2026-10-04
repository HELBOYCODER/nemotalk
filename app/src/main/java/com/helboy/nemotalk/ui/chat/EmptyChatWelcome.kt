package com.helboy.nemotalk.ui.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.helboy.nemotalk.ui.theme.DarkBorder
import com.helboy.nemotalk.ui.theme.DarkSurfaceElevated
import com.helboy.nemotalk.ui.theme.TextPrimary
import com.helboy.nemotalk.ui.theme.TextSecondary

/**
 * Extracted from ChatScreen (lines 643-749): responsive empty-state welcome.
 */
@Composable
fun EmptyChatWelcome(
    onPromptSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(80.dp))

        Text(
            text = "سلام! من نِمو هستم 👋",
            color = TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "دستیار فارسی‌زبان شما با قدرت NVIDIA Nemotron.\nمتن بنویسید، صحبت کنید یا تصویر بفرستید.",
            color = TextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        val suggestions = listOf(
            "نِمو، خودت رو معرفی کن و بگو چطور کار می‌کنی؟",
            "یک داستان کوتاه صوتی برای من تعریف کن",
            "معماری مدل‌های هوش مصنوعی ان‌ویدیا چگونه است؟"
        )

        suggestions.forEach { prompt ->
            Surface(
                onClick = { onPromptSelect(prompt) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 3.dp)
                    .clip(RoundedCornerShape(10.dp)),
                color = DarkSurfaceElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Text(
                    text = prompt,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                )
            }
        }
    }
}
