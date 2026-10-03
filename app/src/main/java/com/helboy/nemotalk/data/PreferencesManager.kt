package com.helboy.nemotalk.data

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("nemotalk_prefs", Context.MODE_PRIVATE)

    var apiKey: String
        get() = prefs.getString(KEY_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_API_KEY, value.trim()).apply()

    var selectedModel: String
        get() = prefs.getString(KEY_MODEL, "nvidia/nemotron-4-340b-instruct") ?: "nvidia/nemotron-4-340b-instruct"
        set(value) = prefs.edit().putString(KEY_MODEL, value).apply()

    var systemPrompt: String
        get() = prefs.getString(
            KEY_SYSTEM_PROMPT,
            "You are NeMoTalk, an intelligent, helpful and concise AI assistant powered by NVIDIA NeMo and Nemotron models. When spoken to in Persian, reply naturally and warmly in Persian. When spoken to in English, reply in English. Keep voice responses natural and focused."
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

    companion object {
        private const val KEY_API_KEY = "key_api_key"
        private const val KEY_MODEL = "key_model"
        private const val KEY_SYSTEM_PROMPT = "key_system_prompt"
        private const val KEY_AUTO_SPEAK = "key_auto_speak"
        private const val KEY_SPEECH_LANG = "key_speech_lang"
        private const val KEY_SPEECH_RATE = "key_speech_rate"
        private const val KEY_SPEECH_PITCH = "key_speech_pitch"
    }
}
