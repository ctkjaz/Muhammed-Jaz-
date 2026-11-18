package com.example.data.local

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemePreference {
    SYSTEM, LIGHT, DARK
}

class SettingsPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "rrb_nursing_settings",
        Context.MODE_PRIVATE
    )

    private val _themePreference = MutableStateFlow(readTheme())
    val themePreference: StateFlow<ThemePreference> = _themePreference.asStateFlow()

    private val _negativeMarking = MutableStateFlow(prefs.getBoolean(KEY_NEG_MARKING, true))
    val negativeMarking: StateFlow<Boolean> = _negativeMarking.asStateFlow()

    private fun readTheme(): ThemePreference {
        val raw = prefs.getString(KEY_THEME, ThemePreference.SYSTEM.name)
        return runCatching { ThemePreference.valueOf(raw ?: "") }.getOrDefault(ThemePreference.SYSTEM)
    }

    fun cycleTheme(currentlyDark: Boolean) {
        val next = if (currentlyDark) ThemePreference.LIGHT else ThemePreference.DARK
        prefs.edit().putString(KEY_THEME, next.name).apply()
        _themePreference.value = next
    }

    fun setNegativeMarking(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NEG_MARKING, enabled).apply()
        _negativeMarking.value = enabled
    }

    fun getSeededVersion(): Int = prefs.getInt(KEY_SEEDED_VERSION, 0)

    fun setSeededVersion(version: Int) {
        prefs.edit().putInt(KEY_SEEDED_VERSION, version).apply()
    }

    companion object {
        private const val KEY_THEME = "theme_mode"
        private const val KEY_NEG_MARKING = "negative_marking"
        private const val KEY_SEEDED_VERSION = "builtin_seed_version"
    }
}
