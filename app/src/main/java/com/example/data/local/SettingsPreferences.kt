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

    private val _profileRole = MutableStateFlow(
        (prefs.getString(KEY_PROFILE_ROLE, "") ?: "").take(MAX_PROFILE_ROLE_LENGTH)
    )
    val profileRole: StateFlow<String> = _profileRole.asStateFlow()

    private val _targetYear = MutableStateFlow(
        (prefs.getString(KEY_TARGET_YEAR, DEFAULT_TARGET_YEAR) ?: DEFAULT_TARGET_YEAR)
            .take(MAX_TARGET_YEAR_LENGTH)
    )
    val targetYear: StateFlow<String> = _targetYear.asStateFlow()

    private val _focusSubject = MutableStateFlow(
        prefs.getString(KEY_FOCUS_SUBJECT, "") ?: ""
    )
    val focusSubject: StateFlow<String> = _focusSubject.asStateFlow()

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
        val raw = prefs.getString(KEY_THEME, ThemePreference.LIGHT.name)
        return runCatching { ThemePreference.valueOf(raw ?: "") }.getOrDefault(ThemePreference.LIGHT)
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

    fun setProfileRole(role: String) {
        val clamped = role.take(MAX_PROFILE_ROLE_LENGTH)
        prefs.edit().putString(KEY_PROFILE_ROLE, clamped).apply()
        _profileRole.value = clamped
    }

    fun setTargetYear(year: String) {
        val clamped = year.trim().take(MAX_TARGET_YEAR_LENGTH)
        prefs.edit().putString(KEY_TARGET_YEAR, clamped).apply()
        _targetYear.value = clamped
    }

    fun setFocusSubject(subjectCode: String) {
        val clean = subjectCode.trim()
        prefs.edit().putString(KEY_FOCUS_SUBJECT, clean).apply()
        _focusSubject.value = clean
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

    fun getLastSignedInUid(): String = prefs.getString(KEY_LAST_SIGNED_IN_UID, "") ?: ""

    fun setLastSignedInUid(uid: String) {
        prefs.edit().putString(KEY_LAST_SIGNED_IN_UID, uid).apply()
    }

    fun applySyncedProfile(
        displayName: String,
        role: String,
        targetYear: String,
        focusSubject: String,
        activeExamId: String,
        themePreference: ThemePreference,
        negativeMarking: Boolean,
        useTimeLimit: Boolean,
        secondsPerQuestion: Int,
        keepScreenOn: Boolean
    ) {
        val cleanName = displayName.take(MAX_PROFILE_NAME_LENGTH)
        val cleanRole = role.take(MAX_PROFILE_ROLE_LENGTH)
        val cleanYear = targetYear.trim().take(MAX_TARGET_YEAR_LENGTH)
        val cleanFocus = focusSubject.trim()
        val cleanExam = activeExamId.trim().ifBlank { PlatformCatalogDefaults.EXAM_RRB_NURSING }
        val cleanSec = secondsPerQuestion.coerceIn(MIN_SECONDS_PER_QUESTION, MAX_SECONDS_PER_QUESTION)

        prefs.edit()
            .putString(KEY_PROFILE_NAME, cleanName)
            .putString(KEY_PROFILE_ROLE, cleanRole)
            .putString(KEY_TARGET_YEAR, cleanYear)
            .putString(KEY_FOCUS_SUBJECT, cleanFocus)
            .putString(KEY_ACTIVE_EXAM_ID, cleanExam)
            .putString(KEY_THEME, themePreference.name)
            .putBoolean(KEY_NEG_MARKING, negativeMarking)
            .putBoolean(KEY_USE_TIME_LIMIT, useTimeLimit)
            .putInt(KEY_SECONDS_PER_QUESTION, cleanSec)
            .putBoolean(KEY_KEEP_SCREEN_ON, keepScreenOn)
            .apply()

        _profileName.value = cleanName
        _profileRole.value = cleanRole
        _targetYear.value = cleanYear
        _focusSubject.value = cleanFocus
        _activeExamId.value = cleanExam
        _themePreference.value = themePreference
        _negativeMarking.value = negativeMarking
        _useTimeLimit.value = useTimeLimit
        _secondsPerQuestion.value = cleanSec
        _keepScreenOn.value = keepScreenOn
    }

    companion object {
        const val MAX_PROFILE_NAME_LENGTH = 40
        const val MAX_PROFILE_ROLE_LENGTH = 40
        const val MAX_TARGET_YEAR_LENGTH = 10
        const val DEFAULT_TARGET_YEAR = "2026"
        const val MIN_SECONDS_PER_QUESTION = 20
        const val MAX_SECONDS_PER_QUESTION = 120
        const val SECONDS_PER_QUESTION_STEP = 5
        const val DEFAULT_SECONDS_PER_QUESTION = 54

        private const val KEY_THEME = "theme_mode"
        private const val KEY_NEG_MARKING = "negative_marking"
        private const val KEY_SEEDED_VERSION = "builtin_seed_version"
        private const val KEY_PROFILE_NAME = "profile_name"
        private const val KEY_PROFILE_ROLE = "profile_role"
        private const val KEY_TARGET_YEAR = "target_year"
        private const val KEY_FOCUS_SUBJECT = "focus_subject"
        private const val KEY_USE_TIME_LIMIT = "use_time_limit"
        private const val KEY_SECONDS_PER_QUESTION = "seconds_per_question"
        private const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
        private const val KEY_ACTIVE_EXAM_ID = "active_exam_id"
        private const val KEY_LAST_SIGNED_IN_UID = "last_signed_in_uid"

        fun calculateSuggestedMinutes(questionCount: Int, secondsPerQuestion: Int): Int {
            if (questionCount <= 0 || secondsPerQuestion <= 0) return 0
            return ceil((questionCount.toDouble() * secondsPerQuestion.toDouble()) / 60.0).toInt()
        }
    }
}
