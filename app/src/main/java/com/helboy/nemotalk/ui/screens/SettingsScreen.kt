package com.helboy.nemotalk.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.helboy.nemotalk.data.PreferencesManager
import com.helboy.nemotalk.model.NvidiaModel
import com.helboy.nemotalk.network.NvidiaApiClient
import com.helboy.nemotalk.ui.theme.DarkBackground
import com.helboy.nemotalk.ui.theme.DarkBorder
import com.helboy.nemotalk.ui.theme.DarkSurface
import com.helboy.nemotalk.ui.theme.DarkSurfaceElevated
import com.helboy.nemotalk.ui.theme.ErrorRed
import com.helboy.nemotalk.ui.theme.NvidiaDarkGreen
import com.helboy.nemotalk.ui.theme.NvidiaGreen
import com.helboy.nemotalk.ui.theme.NvidiaNeon
import com.helboy.nemotalk.ui.theme.SuccessGreen
import com.helboy.nemotalk.ui.theme.TextMuted
import com.helboy.nemotalk.ui.theme.TextPrimary
import com.helboy.nemotalk.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    prefs: PreferencesManager,
    apiClient: NvidiaApiClient,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var apiKey by remember { mutableStateOf(prefs.apiKey) }
    var isApiKeyVisible by remember { mutableStateOf(false) }

    var selectedModelId by remember { mutableStateOf(prefs.selectedModel) }
    var autoSpeak by remember { mutableStateOf(prefs.autoSpeak) }
    var speechLanguage by remember { mutableStateOf(prefs.speechLanguage) }
    var speechRate by remember { mutableFloatStateOf(prefs.speechRate) }
    var speechPitch by remember { mutableFloatStateOf(prefs.speechPitch) }
    var systemPrompt by remember { mutableStateOf(prefs.systemPrompt) }

    var isTestingConnection by remember { mutableStateOf(false) }
    var testResultText by remember { mutableStateOf<String?>(null) }
    var isTestSuccess by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceElevated)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "بازگشت",
                    tint = TextPrimary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "تنظیمات NeMoTalk",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "پیکربندی کلید، مدل‌ها و موتور صوتی",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION 1: NVIDIA API KEY
        SettingsCard(title = "کلید اختصاصی NVIDIA (رایگان)", icon = Icons.Default.Key) {
            Text(
                text = "کلید API ان‌ویدیا به شما امکان مکالمه مستقیم و پرسرعت با مدل‌های NeMo و Nemotron را می‌دهد.",
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = apiKey,
                onValueChange = {
                    apiKey = it
                    prefs.apiKey = it
                },
                label = { Text("کلید API ان‌ویدیا (nvapi-...)") },
                placeholder = { Text("nvapi-xxxxxxxxxxxxxxxx") },
                visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                        Icon(
                            imageVector = if (isApiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "نمایش کلید",
                            tint = TextSecondary
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NvidiaGreen,
                    unfocusedBorderColor = DarkBorder,
                    focusedLabelColor = NvidiaGreen,
                    unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons: Get Key & Test Key
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://build.nvidia.com/"))
                        context.startActivity(browserIntent)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NvidiaNeon),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(NvidiaGreen))
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = "لینک",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("دریافت رایگان کلید", fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        if (apiKey.isBlank()) {
                            testResultText = "لطفاً ابتدا کلید را وارد کنید."
                            isTestSuccess = false
                            return@Button
                        }
                        isTestingConnection = true
                        testResultText = null
                        coroutineScope.launch {
                            val result = apiClient.testConnection(apiKey, selectedModelId)
                            isTestingConnection = false
                            result.fold(
                                onSuccess = { latency ->
                                    isTestSuccess = true
                                    testResultText = "اتصال موفق بود! پینگ: ${latency}ms"
                                },
                                onFailure = { error ->
                                    isTestSuccess = false
                                    testResultText = "خطا در اتصال: ${error.localizedMessage}"
                                }
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = NvidiaGreen, contentColor = DarkBackground),
                    enabled = !isTestingConnection
                ) {
                    if (isTestingConnection) {
                        CircularProgressIndicator(
                            color = DarkBackground,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "تست",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تست اتصال", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Test Result Banner
            if (testResultText != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isTestSuccess) Color(0xFF132B1A) else Color(0xFF331414))
                        .border(1.dp, if (isTestSuccess) SuccessGreen else ErrorRed, RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isTestSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = "نتیجه",
                        tint = if (isTestSuccess) SuccessGreen else ErrorRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = testResultText ?: "",
                        color = if (isTestSuccess) SuccessGreen else ErrorRed,
                        fontSize = 12.sp
                    )
                }
            }

            // Guide box
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F1724))
                    .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Column {
                    Text(
                        text = "💡 راهنمای دریافت کلید رایگان:",
                        color = NvidiaGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "۱. دکمه دریافت کلید را بزنید و در سایت build.nvidia.com ثبت‌نام کنید.\n۲. روی دکمه Get API Key کلیک کنید.\n۳. کلید nvapi-... صادر شده را کپی کرده و اینجا قرار دهید (۱۰۰۰ کردیت رایگان اعطا می‌شود).",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION 2: MODEL SELECTION
        SettingsCard(title = "انتخاب مدل هوش مصنوعی (NVIDIA Model)", icon = Icons.Default.Speed) {
            NvidiaModel.ALL_MODELS.forEach { model ->
                val isSelected = (model.id == selectedModelId)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color(0xFF132218) else DarkSurface)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) NvidiaGreen else DarkBorder,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            selectedModelId = model.id
                            prefs.selectedModel = model.id
                        }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = {
                            selectedModelId = model.id
                            prefs.selectedModel = model.id
                        },
                        colors = RadioButtonDefaults.colors(selectedColor = NvidiaGreen, unselectedColor = TextSecondary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
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
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION 3: VOICE & SPEECH SETTINGS
        SettingsCard(title = "تنظیمات صوت و گفتار (Voice & TTS)", icon = Icons.Default.VolumeUp) {
            // Auto Speak Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "پخش خودکار صوتی پاسخ‌ها",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "پاسخ مدل به محض دریافت به صورت صوتی خوانده شود",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked = autoSpeak,
                    onCheckedChange = {
                        autoSpeak = it
                        prefs.autoSpeak = it
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = NvidiaNeon, checkedTrackColor = NvidiaDarkGreen)
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = DarkBorder)

            // Language Selector
            Text(text = "زبان تشخیص گفتار میکروفون:", color = TextSecondary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val languages = listOf("fa-IR" to "فارسی (ایران)", "en-US" to "English (US)", "auto" to "خودکار")
                languages.forEach { (code, label) ->
                    val isSelected = (speechLanguage == code)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) NvidiaGreen else DarkSurfaceElevated)
                            .clickable {
                                speechLanguage = code
                                prefs.speechLanguage = code
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) DarkBackground else TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Speech Rate
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "سرعت خوانش صوتی:", color = TextSecondary, fontSize = 12.sp)
                Text(text = "${String.format("%.1f", speechRate)}x", color = NvidiaGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = speechRate,
                onValueChange = {
                    speechRate = it
                    prefs.speechRate = it
                },
                valueRange = 0.6f..1.6f,
                colors = SliderDefaults.colors(thumbColor = NvidiaNeon, activeTrackColor = NvidiaGreen)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION 4: SYSTEM PROMPT
        SettingsCard(title = "دستور سیستمی (System Prompt)", icon = Icons.Default.Speed) {
            OutlinedTextField(
                value = systemPrompt,
                onValueChange = {
                    systemPrompt = it
                    prefs.systemPrompt = it
                },
                label = { Text("شخصیت و رفتار مدل هوش مصنوعی") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NvidiaGreen,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION 5: ABOUT
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSurface)
                .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "NeMoTalk — v1.0.0",
                    color = NvidiaNeon,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "توسعه‌یافته توسط Helboy Coder بر پایه NVIDIA NeMo Speech",
                    color = TextMuted,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/HELBOYCODER/nemotalk"))
                        context.startActivity(browserIntent)
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder))
                ) {
                    Text("مشاهده مخزن در گیت‌هاب", fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun SettingsCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NvidiaGreen,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
        content()
    }
}
