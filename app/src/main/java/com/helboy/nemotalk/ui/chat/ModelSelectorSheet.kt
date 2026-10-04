package com.helboy.nemotalk.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.helboy.nemotalk.model.Conversation
import com.helboy.nemotalk.model.NvidiaModel
import com.helboy.nemotalk.ui.theme.DarkBorder
import com.helboy.nemotalk.ui.theme.DarkSurface
import com.helboy.nemotalk.ui.theme.NvidiaGreen
import com.helboy.nemotalk.ui.theme.NvidiaNeon
import com.helboy.nemotalk.ui.theme.ErrorRed
import com.helboy.nemotalk.ui.theme.TextMuted
import com.helboy.nemotalk.ui.theme.TextPrimary

/**
 * Extracted from ChatScreen (lines 508-641): model selector + conversation
 * clear action. Callbacks hoisted; visibility owned by caller.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSelectorSheet(
    visible: Boolean,
    conversations: List<Conversation>,
    selectedModelId: String,
    onModelSelect: (String) -> Unit,
    onClearCurrentChat: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!visible) return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSurface,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                text = "انتخاب مدل NVIDIA",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Text(
                text = "${NvidiaModel.ALL_MODELS.size} مدل فعال (قابل استفاده با کلید شما)",
                color = TextMuted,
                fontSize = 11.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyColumn(modifier = Modifier.height(380.dp)) {
                items(NvidiaModel.ALL_MODELS) { model ->
                    val isSelected = model.id == selectedModelId
                    Surface(
                        onClick = { onModelSelect(model.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        color = if (isSelected) Color(0xFF132218) else DarkSurface,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) NvidiaGreen else DarkBorder
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = model.displayName,
                                        color = if (isSelected) NvidiaNeon else TextPrimary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                    if (model.isRecommended) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(NvidiaGreen.copy(alpha = 0.2f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("پیشنهادی", color = NvidiaGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Text(
                                    text = model.description,
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    maxLines = 2,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = NvidiaNeon,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                onClick = onClearCurrentChat,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
                color = Color(0xFF2A0E0E),
                border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = null,
                        tint = ErrorRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "پاک‌سازی تاریخچه گفتگوی فعلی",
                        color = ErrorRed,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "powered by NVIDIA NIM",
                color = TextMuted,
                fontSize = 10.sp,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 24.dp)
            )
        }
    }
}
