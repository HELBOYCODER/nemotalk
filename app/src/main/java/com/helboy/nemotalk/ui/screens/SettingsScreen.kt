package com.helboy.nemotalk.ui.screens

import androidx.compose.animation.AnimatedVisibility
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.VpnKey
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.helboy.nemotalk.data.PreferencesManager
import com.helboy.nemotalk.model.NvidiaModel
import com.helboy.nemotalk.network.NvidiaApiClient
import com.helboy.nemotalk.network.ProxySupport
import com.helboy.nemotalk.speech.TextToSpeechHelper
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
    ttsHelper: TextToSpeechHelper,
    onBack: () -> Unit,
    onOpenSandbox: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var apiKey by remember { mutableStateOf(prefs.apiKey) }
    var isApiKeyVisible by remember { mutableStateOf(false) }

    var selectedModelId by remember { mutableStateOf(prefs.selectedModel) }
    var responseLanguage by remember { mutableStateOf(prefs.responseLanguage) }
    var autoSpeak by remember { mutableStateOf(prefs.autoSpeak) }
    var speechLanguage by remember { mutableStateOf(prefs.speechLanguage) }
    var speechRate by remember { mutableFloatStateOf(prefs.speechRate) }
    var speechPitch by remember { mutableFloatStateOf(prefs.speechPitch) }
    var systemPrompt by remember { mutableStateOf(prefs.systemPrompt) }
    var ttsEngine by remember { mutableStateOf(prefs.ttsEngineType) }
    var geminiApiKey by remember { mutableStateOf(prefs.geminiApiKey) }
    var isGeminiKeyVisible by remember { mutableStateOf(false) }
    var geminiVoice by remember { mutableStateOf(prefs.geminiVoice) }

    // In-app proxy state (ZeroNet / Zray on 127.0.0.1)
    // State stores the preset code ("socks"/"http") exactly as prefs encode it.
    var proxyEnabled by remember { mutableStateOf(prefs.proxyEnabled) }
    var proxyType by remember {
        mutableStateOf(
            if (prefs.proxyType == java.net.Proxy.Type.HTTP)
                PreferencesManager.PROXY_TYPE_HTTP else PreferencesManager.PROXY_TYPE_SOCKS
        )
    }
    var proxyHost by remember { mutableStateOf(prefs.proxyAddress) }
    var proxySocksPort by remember { mutableStateOf(prefs.proxyPort) }
    var proxyHttpPort by remember { mutableStateOf(prefs.proxyHttpPort) }

    val hasPersianVoice by ttsHelper.hasPersianVoice.collectAsState()

    var isTestingConnection by remember { mutableStateOf(false) }
    var testResultText by remember { mutableStateOf<String?>(null) }
    var isTestSuccess by remember { mutableStateOf(false) }

    var isFetchingModels by remember { mutableStateOf(false) }
    var liveModelsList by remember { mutableStateOf<List<String>>(emptyList()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp)
            .verticalScroll(scrollState)
    ) {
        // TOP HEADER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(42.dp)
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
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "پیکربندی هوش مصنوعی، صدای فارسی و کلید اختصاصی",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // SECTION 1: NVIDIA API KEY
        SettingsCard(title = "کلید اختصاصی NVIDIA (رایگان)", icon = Icons.Default.Key) {
            Text(
                text = "کلید API شما برای مکالمه مستقیم با سرورهای هوش مصنوعی ان‌ویدیا استفاده می‌شود.",
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp
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

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Test Connection Button
                Button(
                    onClick = {
                        if (apiKey.isBlank()) {
                            testResultText = "لطفاً ابتدا کلید API را وارد کنید."
                            isTestSuccess = false
                            return@Button
                        }
                        isTestingConnection = true
                        testResultText = null
                        coroutineScope.launch {
                            val result = apiClient.testConnection(apiKey, selectedModelId)
                            isTestingConnection = false
                            result.fold(
                                onSuccess = { (workingModel, latency) ->
                                    isTestSuccess = true
                                    testResultText = "اتصال موفق! مدل فعال: $workingModel (پینگ: ${latency}ms)"
                                    if (workingModel != selectedModelId) {
                                        selectedModelId = workingModel
                                        prefs.selectedModel = workingModel
                                    }
                                },
                                onFailure = { error ->
                                    isTestSuccess = false
                                    testResultText = error.localizedMessage ?: "خطا در اتصال به سرور ان‌ویدیا"
                                }
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NvidiaGreen, contentColor = DarkBackground),
                    enabled = !isTestingConnection
                ) {
                    if (isTestingConnection) {
                        CircularProgressIndicator(
                            color = DarkBackground,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("در حال بررسی اتصال...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(imageVector = Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تست اتصال به سرور ان‌ویدیا", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Get Free Key Button
                OutlinedButton(
                    onClick = {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://build.nvidia.com/"))
                        context.startActivity(browserIntent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NvidiaNeon),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(NvidiaGreen))
                ) {
                    Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("دریافت رایگان کلید API از build.nvidia.com", fontSize = 12.sp)
                }
            }

            // Test Result Banner
            if (testResultText != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isTestSuccess) Color(0xFF132B1A) else Color(0xFF331414))
                        .border(1.dp, if (isTestSuccess) SuccessGreen else ErrorRed, RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isTestSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
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
        }

        Spacer(modifier = Modifier.height(14.dp))

        // SECTION 2: AI OUTPUT LANGUAGE (فارسی / انگلیسی)
        SettingsCard(title = "زبان پاسخ‌دهی هوش مصنوعی", icon = Icons.Default.VolumeUp) {
            Text(
                text = "پاسخ‌ها و مکالمات صوتی به این زبان تولید و خوانده می‌شوند:",
                color = TextSecondary,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val languages = listOf("fa" to "🇮🇷 همیشه فارسی", "auto" to "🌐 هوشمند", "en" to "🇺🇸 انگلیسی")
                languages.forEach { (code, label) ->
                    val isSelected = (responseLanguage == code)
                    Surface(
                        onClick = {
                            responseLanguage = code
                            prefs.responseLanguage = code
                        },
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp)),
                        color = if (isSelected) NvidiaGreen else DarkSurfaceElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) NvidiaNeon else DarkBorder)
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) DarkBackground else TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 9.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // SECTION 3: VOICE & TTS SETTINGS
        SettingsCard(title = "موتور صوتی و خوانش فارسی (Voice & TTS)", icon = Icons.Default.VolumeUp) {
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
                        text = "پاسخ نِمو به محض دریافت به صورت صوتی خوانده شود",
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

            // Persian Voice Status and Google Speech Setup
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (hasPersianVoice) Color(0xFF132B1A) else Color(0xFF261D12))
                    .border(1.dp, if (hasPersianVoice) SuccessGreen.copy(alpha = 0.5f) else Color(0xFFFFB300).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (hasPersianVoice) Icons.Default.CheckCircle else Icons.Default.VolumeUp,
                    contentDescription = null,
                    tint = if (hasPersianVoice) SuccessGreen else Color(0xFFFFB300),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (hasPersianVoice) "صدای فارسی گوگل: فعال و آماده ✅" else "صدای فارسی گوگل: بررسی یا فعال‌سازی",
                        color = if (hasPersianVoice) SuccessGreen else Color(0xFFFFD54F),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (hasPersianVoice) "پاسخ‌ها با صدای طبیعی و بدون درنگ خوانده می‌شوند." else "برای کیفیت استودیویی، صدای فارسی را در تنظیمات گوگل فعال کنید.",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = { ttsHelper.openTtsSettings() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NvidiaNeon),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(NvidiaGreen))
            ) {
                Icon(imageVector = Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("تنظیمات موتور صوتی گوگل (دانلود دیتای فارسی)", fontSize = 12.sp)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = DarkBorder)

            // TTS ENGINE SELECTOR — Gemini / Neural / System
            Text(text = "موتور تولید صدای فارسی:", color = TextSecondary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val engines = listOf(
                    "gemini" to "🧬 جِمینای (طبیعی)",
                    "neural" to "⚡ نِمو آفلاین",
                    "system" to "🔉 سیستم گوشی"
                )
                engines.forEach { (code, label) ->
                    val isSelected = (ttsEngine == code)
                    Surface(
                        onClick = {
                            ttsEngine = code
                            prefs.ttsEngineType = code
                        },
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)),
                        color = if (isSelected) NvidiaGreen else DarkSurfaceElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) NvidiaNeon else DarkBorder)
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) DarkBackground else TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (ttsEngine == "gemini") {
                OutlinedTextField(
                    value = geminiApiKey,
                    onValueChange = {
                        geminiApiKey = it
                        prefs.geminiApiKey = it
                    },
                    label = { Text("کلید API گوگل جِمینای (AIza...)", fontSize = 12.sp) },
                    singleLine = true,
                    visualTransformation = if (isGeminiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isGeminiKeyVisible = !isGeminiKeyVisible }) {
                            Icon(
                                imageVector = if (isGeminiKeyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NvidiaGreen,
                        unfocusedBorderColor = DarkBorder,
                        focusedLabelColor = NvidiaGreen,
                        unfocusedLabelColor = TextSecondary,
                        cursorColor = NvidiaGreen
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "رایگان از Google AI Studio: aistudio.google.com/apikey — مدل‌های Gemini طبیعی‌ترین و حرفه‌ای‌ترین صدای فارسی را با لحن و درنگ طبیعی تولید می‌کنند.",
                    color = TextMuted,
                    fontSize = 10.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Gemini Voice Selector
                Text(text = "صدای جِمینای:", color = TextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val voices = listOf("Aoede" to "Aoede (زنانه)", "Charon" to "Charon (مردانه)", "Fenrir" to "Fenrir", "Kore" to "Kore", "Puck" to "Puck")
                    voices.forEach { (code, label) ->
                        val isSelected = (geminiVoice == code)
                        Surface(
                            onClick = {
                                geminiVoice = code
                                prefs.geminiVoice = code
                            },
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)),
                            color = if (isSelected) NvidiaGreen else DarkSurfaceElevated,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) NvidiaNeon else DarkBorder)
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) DarkBackground else TextPrimary,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 7.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = DarkBorder)

            // Microphone Recognition Language
            Text(text = "زبان تشخیص گفتار میکروفون:", color = TextSecondary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val languages = listOf("fa-IR" to "فارسی", "en-US" to "English", "auto" to "خودکار")
                languages.forEach { (code, label) ->
                    val isSelected = (speechLanguage == code)
                    Surface(
                        onClick = {
                            speechLanguage = code
                            prefs.speechLanguage = code
                        },
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp)),
                        color = if (isSelected) NvidiaGreen else DarkSurfaceElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) NvidiaNeon else DarkBorder)
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) DarkBackground else TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

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

        Spacer(modifier = Modifier.height(14.dp))

        // SECTION 4: MODEL SELECTION
        SettingsCard(title = "مدل‌های هوش مصنوعی ان‌ویدیا", icon = Icons.Default.Speed) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "مدل فعال برای مکالمه صوتی:",
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                // Refresh Models Button
                if (apiKey.isNotBlank()) {
                    IconButton(
                        onClick = {
                            isFetchingModels = true
                            coroutineScope.launch {
                                val result = apiClient.fetchAvailableModels(apiKey)
                                isFetchingModels = false
                                result.onSuccess { liveModelsList = it }
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        if (isFetchingModels) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = NvidiaGreen, strokeWidth = 2.dp)
                        } else {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "بروزرسانی", tint = NvidiaGreen, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            NvidiaModel.ALL_MODELS.forEach { model ->
                val isSelected = (model.id == selectedModelId)
                Surface(
                    onClick = {
                        selectedModelId = model.id
                        prefs.selectedModel = model.id
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    color = if (isSelected) Color(0xFF132218) else DarkSurfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) NvidiaGreen else DarkBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
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
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = model.displayName,
                                    color = if (isSelected) NvidiaNeon else TextPrimary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                                if (model.isRecommended) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(NvidiaGreen.copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("فعال و سریع", color = NvidiaGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
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
        }

        Spacer(modifier = Modifier.height(14.dp))

        // SECTION 5: SYSTEM PROMPT
        SettingsCard(title = "دستور سیستمی (System Prompt)", icon = Icons.Default.Speed) {
            OutlinedTextField(
                value = systemPrompt,
                onValueChange = {
                    systemPrompt = it
                    prefs.systemPrompt = it
                },
                label = { Text("رفتار و شخصیت مدل NeMo") },
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

        Spacer(modifier = Modifier.height(14.dp))

        // SECTION 6: IN-APP PROXY (ZeroNet / Zray)
        SettingsCard(title = "پروکسی داخلی (عبور از فیلترینگ)", icon = Icons.Default.VpnKey) {
            // Master toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "فعال‌سازی پروکسی داخلی",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (proxyEnabled)
                            "ترافیک NeMoTalk از طریق ${ProxySupport.socksEndpoint(prefs)} عبور می‌کند — بقیه سیستم دست‌نخورده می‌ماند"
                        else
                            "فقط ترافیک همین اپ پروکسی می‌شود؛ کل سیستم تونل نمی‌شود",
                        color = TextMuted,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
                Switch(
                    checked = proxyEnabled,
                    onCheckedChange = {
                        proxyEnabled = it
                        prefs.proxyEnabled = it
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = NvidiaNeon,
                        checkedTrackColor = NvidiaGreen.copy(alpha = 0.5f)
                    )
                )
            }

            AnimatedVisibility(visible = proxyEnabled) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Proxy type: SOCKS5 vs HTTP
                    Text(
                        text = "نوع پروکسی",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            PreferencesManager.PROXY_TYPE_SOCKS to "SOCKS5",
                            PreferencesManager.PROXY_TYPE_HTTP to "HTTP"
                        ).forEach { (code, label) ->
                            Surface(
                                onClick = {
                                    proxyType = code
                                    prefs.proxyType = if (code == PreferencesManager.PROXY_TYPE_HTTP)
                                        java.net.Proxy.Type.HTTP else java.net.Proxy.Type.SOCKS
                                },                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp)),
                                color = if (proxyType == code) Color(0xFF132218) else Color(0xFF121620),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (proxyType == code) NvidiaGreen else DarkBorder
                                )
                            ) {
                                Text(
                                    text = label,
                                    color = if (proxyType == code) NvidiaNeon else TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(vertical = 9.dp, horizontal = 12.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = proxyHost,
                        onValueChange = {
                            proxyHost = it
                            prefs.proxyAddress = it
                        },
                        label = { Text("آدرس (خالی = ۱۲۷.۰.۰.۱)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NvidiaGreen,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = if (proxyType == PreferencesManager.PROXY_TYPE_HTTP) proxyHttpPort else proxySocksPort,
                        onValueChange = {
                            if (proxyType == PreferencesManager.PROXY_TYPE_HTTP) {
                                proxyHttpPort = it
                                prefs.proxyHttpPort = it
                            } else {
                                proxySocksPort = it
                                prefs.proxyPort = it
                            }
                        },
                        label = {
                            Text(
                                if (proxyType == PreferencesManager.PROXY_TYPE_HTTP)
                                    "پورت HTTP (پیش‌فرض ۱۰۸۰۹)"
                                else
                                    "پورت SOCKS5 (پیش‌فرض ۱۰۸۰۸)"
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NvidiaGreen,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick connect to a running ZeroNet/Zray instance
                    Button(
                        onClick = {
                            proxyHost = "127.0.0.1"
                            proxySocksPort = "10808"
                            proxyHttpPort = "10809"
                            prefs.proxyAddress = "127.0.0.1"
                            prefs.proxyPort = "10808"
                            prefs.proxyHttpPort = "10809"
                            proxyType = PreferencesManager.PROXY_TYPE_SOCKS
                            prefs.proxyType = java.net.Proxy.Type.SOCKS
                            proxyEnabled = true
                            prefs.proxyEnabled = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NvidiaGreen,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("اتصال سریع زیرونت (۱۲۷.۰.۰.۱:۱۰۸۰۸)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Open the embedded Linux sandbox + long-term memory panel
                    OutlinedButton(
                        onClick = onOpenSandbox,
                        modifier = Modifier.fillMaxWidth(),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NvidiaGreen.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = NvidiaNeon
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("سندباکس لینوکس داخلی + حافظه بلندمدت", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = NvidiaNeon)
                    }
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
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp)),
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
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
                    fontSize = 14.sp
                )
            }
            content()
        }
    }
}
