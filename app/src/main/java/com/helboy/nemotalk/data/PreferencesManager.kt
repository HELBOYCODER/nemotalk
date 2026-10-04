package com.helboy.nemotalk.data

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("nemotalk_prefs", Context.MODE_PRIVATE)

    var apiKey: String
        get() = prefs.getString(KEY_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_API_KEY, value.trim()).apply()

    var geminiApiKey: String
        get() = prefs.getString(KEY_GEMINI_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GEMINI_API_KEY, value.trim()).apply()

    var selectedModel: String
        get() = prefs.getString(KEY_MODEL, "nvidia/nemotron-4-340b-instruct") ?: "nvidia/nemotron-4-340b-instruct"
        set(value) = prefs.edit().putString(KEY_MODEL, value).apply()

    var responseLanguage: String
        get() = prefs.getString(KEY_RESPONSE_LANG, "fa") ?: "fa"
        set(value) = prefs.edit().putString(KEY_RESPONSE_LANG, value).apply()

    var ttsEngineType: String
        get() = prefs.getString(KEY_TTS_ENGINE, "gemini") ?: "gemini"
        set(value) = prefs.edit().putString(KEY_TTS_ENGINE, value).apply()

    var geminiVoice: String
        get() = prefs.getString(KEY_GEMINI_VOICE, "Aoede") ?: "Aoede"
        set(value) = prefs.edit().putString(KEY_GEMINI_VOICE, value).apply()

    var systemPrompt: String
        get() = prefs.getString(
            KEY_SYSTEM_PROMPT,
            "شما دستیار صوتی و هوشمند آوانمو (NeMoTalk) مجهز به مدل‌های NVIDIA NeMo هستید. وظیفه شما پاسخگویی دقیق، دلنشین و شیوا به زبان فارسی است. همواره به زبان فارسی روان پاسخ دهید مگر اینکه کاربر صریحاً به زبان دیگری صحبت کند. در مکالمات صوتی، پاسخ‌ها را رسا، خوش‌آهنگ و موجز بیان کنید."
        ) ?: ""
        set(value) = prefs.edit().putString(KEY_SYSTEM_PROMPT, value).apply()

    var autoSpeak: Boolean
        get() = prefs.getBoolean(KEY_AUTO_SPEAK, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_SPEAK, value).apply()

    var speechLanguage: String
        get() = prefs.getString(KEY_SPEECH_LANG, "fa-IR") ?: "fa-IR"
        set(value) = prefs.edit().putString(KEY_SPEECH_LANG, value).apply()

    var speechRate: Float
        get() = prefs.getFloat(KEY_SPEECH_RATE, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_SPEECH_RATE, value).apply()

    var speechPitch: Float
        get() = prefs.getFloat(KEY_SPEECH_PITCH, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_SPEECH_PITCH, value).apply()

    // ---------- In-app proxy (ZeroNet / Zray or any local proxy) ----------

    var proxyEnabled: Boolean
        get() = prefs.getBoolean(KEY_PROXY_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_PROXY_ENABLED, value).apply()

    /** SOCKS5 vs HTTP CONNECT. SOCKS5 is Zray's primary inbound. */
    var proxyType: java.net.Proxy.Type
        get() = when (prefs.getString(KEY_PROXY_TYPE, PROXY_TYPE_SOCKS)) {
            PROXY_TYPE_HTTP -> java.net.Proxy.Type.HTTP
            else -> java.net.Proxy.Type.SOCKS
        }
        set(value) = prefs.edit().putString(
            KEY_PROXY_TYPE,
            if (value == java.net.Proxy.Type.HTTP) PROXY_TYPE_HTTP else PROXY_TYPE_SOCKS
        ).apply()

    /** Blank means 127.0.0.1 — the proxy is expected to run on-device. */
    var proxyAddress: String
        get() = prefs.getString(KEY_PROXY_ADDRESS, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PROXY_ADDRESS, value.trim()).apply()

    var proxyPort: String
        get() = prefs.getString(KEY_PROXY_PORT, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PROXY_PORT, value.trim()).apply()

    var proxyHttpPort: String
        get() = prefs.getString(KEY_PROXY_HTTP_PORT, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PROXY_HTTP_PORT, value.trim()).apply()

    var proxyUsername: String
        get() = prefs.getString(KEY_PROXY_USERNAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PROXY_USERNAME, value).apply()

    var proxyPassword: String
        get() = prefs.getString(KEY_PROXY_PASSWORD, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PROXY_PASSWORD, value).apply()

    companion object {
        private const val KEY_API_KEY = "key_api_key"
        private const val KEY_GEMINI_API_KEY = "key_gemini_api_key"
        private const val KEY_MODEL = "key_model"
        private const val KEY_RESPONSE_LANG = "key_response_lang"
        private const val KEY_TTS_ENGINE = "key_tts_engine"
        private const val KEY_GEMINI_VOICE = "key_gemini_voice"
        private const val KEY_SYSTEM_PROMPT = "key_system_prompt"
        private const val KEY_AUTO_SPEAK = "key_auto_speak"
        private const val KEY_SPEECH_LANG = "key_speech_lang"
        private const val KEY_SPEECH_RATE = "key_speech_rate"
        private const val KEY_SPEECH_PITCH = "key_speech_pitch"

        const val PROXY_TYPE_SOCKS = "socks"
        const val PROXY_TYPE_HTTP = "http"

        private const val KEY_PROXY_ENABLED = "key_proxy_enabled"
        private const val KEY_PROXY_TYPE = "key_proxy_type"
        private const val KEY_PROXY_ADDRESS = "key_proxy_address"
        private const val KEY_PROXY_PORT = "key_proxy_port"
        private const val KEY_PROXY_HTTP_PORT = "key_proxy_http_port"
        private const val KEY_PROXY_USERNAME = "key_proxy_username"
        private const val KEY_PROXY_PASSWORD = "key_proxy_password"
    }
}
