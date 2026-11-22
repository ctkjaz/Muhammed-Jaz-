package com.example.ui.viewmodel

import com.example.data.model.PlatformCatalogDefaults
import com.example.data.model.Question
import com.example.data.model.QuestionDifficulty
import com.example.data.model.QuestionSourceType
import com.example.data.subscription.AdminUserDirectoryRow
import com.example.data.subscription.CloudAccessCode
import com.example.data.subscription.CloudDeviceTrialLock
import com.example.data.subscription.CloudQuestionPack

enum class AdminConsoleTab(val label: String) {
    USERS("Users"),
    CODES("Codes"),
    LOCKS("Locks"),
    CONTENT("Content"),
    PRICING("Pricing")
}

data class AdminContentDraftState(
    val title: String = "",
    val examId: String = PlatformCatalogDefaults.EXAM_AIIMS_NORCET,
    val stage: String = "all",
    val subject: String = "MSN",
    val category: String = "pyq",
    val sourceType: QuestionSourceType = QuestionSourceType.MEMORY_BASED,
    val shiftYear: String = "2026-S1",
    val difficulty: QuestionDifficulty = QuestionDifficulty.MEDIUM,
    val isPremiumOnly: Boolean = true,
    val rawText: String = "",
    val extractedFileName: String = "",
    val previewQuestions: List<Question> = emptyList(),
    val isParsingOrExtracting: Boolean = false,
    val isPublishing: Boolean = false
)

data class AdminConsoleUiState(
    val selectedTab: AdminConsoleTab = AdminConsoleTab.USERS,
    val isLoading: Boolean = false,
    val isMutating: Boolean = false,
    val users: List<AdminUserDirectoryRow> = emptyList(),
    val accessCodes: List<CloudAccessCode> = emptyList(),
    val deviceLocks: List<CloudDeviceTrialLock> = emptyList(),
    val questionPacks: List<CloudQuestionPack> = emptyList(),
    val lastGeneratedCode: CloudAccessCode? = null,
    val contentDraft: AdminContentDraftState = AdminContentDraftState()
)
