package com.example.data.local

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.data.model.PlatformCatalogDefaults
import kotlin.math.ceil

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

    private val _profileName = MutableStateFlow(
        (prefs.getString(KEY_PROFILE_NAME, "") ?: "").take(MAX_PROFILE_NAME_LENGTH)
    )
    val profileName: StateFlow<String> = _profileName.asStateFlow()

    private val _useTimeLimit = MutableStateFlow(prefs.getBoolean(KEY_USE_TIME_LIMIT, true))
    val useTimeLimit: StateFlow<Boolean> = _useTimeLimit.asStateFlow()

    private val _secondsPerQuestion = MutableStateFlow(
        prefs.getInt(KEY_SECONDS_PER_QUESTION, DEFAULT_SECONDS_PER_QUESTION)
            .coerceIn(MIN_SECONDS_PER_QUESTION, MAX_SECONDS_PER_QUESTION)
    )
    val secondsPerQuestion: StateFlow<Int> = _secondsPerQuestion.asStateFlow()

    private val _keepScreenOn = MutableStateFlow(prefs.getBoolean(KEY_KEEP_SCREEN_ON, false))
    val keepScreenOn: StateFlow<Boolean> = _keepScreenOn.asStateFlow()

    private val _activeExamId = MutableStateFlow(
        prefs.getString(KEY_ACTIVE_EXAM_ID, PlatformCatalogDefaults.EXAM_RRB_NURSING)
            ?.takeIf { it.isNotBlank() }
            ?: PlatformCatalogDefaults.EXAM_RRB_NURSING
    )
    val activeExamId: StateFlow<String> = _activeExamId.asStateFlow()

    private fun readTheme(): ThemePreference {
        val raw = prefs.getString(KEY_THEME, ThemePreference.SYSTEM.name)
        return runCatching { ThemePreference.valueOf(raw ?: "") }.getOrDefault(ThemePreference.SYSTEM)
    }

    fun setThemePreference(preference: ThemePreference) {
        prefs.edit().putString(KEY_THEME, preference.name).apply()
        _themePreference.value = preference
    }

    fun cycleTheme(currentlyDark: Boolean) {
        val next = if (currentlyDark) ThemePreference.LIGHT else ThemePreference.DARK
        setThemePreference(next)
    }

    fun setNegativeMarking(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NEG_MARKING, enabled).apply()
        _negativeMarking.value = enabled
    }

    fun setProfileName(name: String) {
        val clamped = name.take(MAX_PROFILE_NAME_LENGTH)
        prefs.edit().putString(KEY_PROFILE_NAME, clamped).apply()
        _profileName.value = clamped
    }

    fun setUseTimeLimit(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_USE_TIME_LIMIT, enabled).apply()
        _useTimeLimit.value = enabled
    }

    fun setSecondsPerQuestion(seconds: Int) {
        val clamped = seconds.coerceIn(MIN_SECONDS_PER_QUESTION, MAX_SECONDS_PER_QUESTION)
        prefs.edit().putInt(KEY_SECONDS_PER_QUESTION, clamped).apply()
        _secondsPerQuestion.value = clamped
    }

    fun setKeepScreenOn(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_KEEP_SCREEN_ON, enabled).apply()
        _keepScreenOn.value = enabled
    }

    fun setActiveExamId(examId: String) {
        val clean = examId.trim().ifBlank { PlatformCatalogDefaults.EXAM_RRB_NURSING }
        prefs.edit().putString(KEY_ACTIVE_EXAM_ID, clean).apply()
        _activeExamId.value = clean
    }

    fun getSeededVersion(): Int = prefs.getInt(KEY_SEEDED_VERSION, 0)

    fun setSeededVersion(version: Int) {
        prefs.edit().putInt(KEY_SEEDED_VERSION, version).apply()
    }

    companion object {
        const val MAX_PROFILE_NAME_LENGTH = 40
        const val MIN_SECONDS_PER_QUESTION = 20
        const val MAX_SECONDS_PER_QUESTION = 120
        const val SECONDS_PER_QUESTION_STEP = 5
        const val DEFAULT_SECONDS_PER_QUESTION = 54

        private const val KEY_THEME = "theme_mode"
        private const val KEY_NEG_MARKING = "negative_marking"
        private const val KEY_SEEDED_VERSION = "builtin_seed_version"
        private const val KEY_PROFILE_NAME = "profile_name"
        private const val KEY_USE_TIME_LIMIT = "use_time_limit"
        private const val KEY_SECONDS_PER_QUESTION = "seconds_per_question"
        private const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
        private const val KEY_ACTIVE_EXAM_ID = "active_exam_id"

        fun calculateSuggestedMinutes(questionCount: Int, secondsPerQuestion: Int): Int {
            if (questionCount <= 0 || secondsPerQuestion <= 0) return 0
            return ceil((questionCount.toDouble() * secondsPerQuestion.toDouble()) / 60.0).toInt()
        }
    }
}
