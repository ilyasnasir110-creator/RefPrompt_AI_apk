package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.PromptDetailLevel
import com.example.data.model.PromptMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode {
    DARK, LIGHT, SYSTEM
}

class UserPreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("refprompt_prefs", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(loadThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _defaultMode = MutableStateFlow(loadDefaultMode())
    val defaultMode: StateFlow<PromptMode> = _defaultMode.asStateFlow()

    private val _defaultDetailLevel = MutableStateFlow(loadDefaultDetailLevel())
    val defaultDetailLevel: StateFlow<PromptDetailLevel> = _defaultDetailLevel.asStateFlow()

    private val _customApiKey = MutableStateFlow(loadCustomApiKey())
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    private fun loadThemeMode(): ThemeMode {
        val str = prefs.getString("theme_mode", ThemeMode.DARK.name) ?: ThemeMode.DARK.name
        return try {
            ThemeMode.valueOf(str)
        } catch (_: Exception) {
            ThemeMode.DARK
        }
    }

    private fun loadDefaultMode(): PromptMode {
        val str = prefs.getString("default_mode", PromptMode.UNIVERSAL.name)
        return PromptMode.fromName(str)
    }

    private fun loadDefaultDetailLevel(): PromptDetailLevel {
        val str = prefs.getString("default_detail_level", PromptDetailLevel.ULTRA_DETAILED.name)
        return PromptDetailLevel.fromName(str)
    }

    private fun loadCustomApiKey(): String {
        val saved = prefs.getString("custom_gemini_api_key", "") ?: ""
        if (saved.isNotBlank()) return saved
        val buildKey = try {
            com.example.BuildConfig.GEMINI_API_KEY.trim()
        } catch (_: Exception) {
            ""
        }
        if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY") return buildKey
        return com.example.data.api.GeminiApiClient.DEFAULT_PERMANENT_KEY
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString("theme_mode", mode.name).apply()
        _themeMode.value = mode
    }

    fun setDefaultMode(mode: PromptMode) {
        prefs.edit().putString("default_mode", mode.name).apply()
        _defaultMode.value = mode
    }

    fun setDefaultDetailLevel(level: PromptDetailLevel) {
        prefs.edit().putString("default_detail_level", level.name).apply()
        _defaultDetailLevel.value = level
    }

    fun setCustomApiKey(key: String) {
        prefs.edit().putString("custom_gemini_api_key", key.trim()).apply()
        _customApiKey.value = key.trim()
    }

    fun hasSeenIntro(): Boolean {
        return prefs.getBoolean("has_seen_intro", false)
    }

    fun setHasSeenIntro(seen: Boolean) {
        prefs.edit().putBoolean("has_seen_intro", seen).apply()
    }
}
