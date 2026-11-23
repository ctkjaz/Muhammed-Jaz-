package com.example.data.model

import androidx.compose.ui.graphics.Color

enum class QuestionDifficulty(val label: String) {
    EASY("Easy"),
    MEDIUM("Medium"),
    HARD("Hard");

    companion object {
        fun fromRaw(raw: String?): QuestionDifficulty = when (raw?.trim()?.lowercase()) {
            "easy" -> EASY
            "hard" -> HARD
            else -> MEDIUM
        }
    }
}

enum class DifficultyFilter(val label: String, val difficulty: QuestionDifficulty?) {
    ALL("All", null),
    EASY("Easy", QuestionDifficulty.EASY),
    MEDIUM("Medium", QuestionDifficulty.MEDIUM),
    HARD("Hard", QuestionDifficulty.HARD)
}

enum class QuestionSourceType(val dbValue: String, val label: String) {
    OFFICIAL_PYQ("official_pyq", "Official PYQ"),
    MEMORY_BASED("memory_based", "Memory-Based PYQ"),
    PREDICTED_MOCK("predicted_mock", "Predicted Mock"),
    PRACTICE("practice", "Practice"),
    CURRENT_AFFAIRS("current_affairs", "Current Affairs");

    companion object {
        fun fromDbValue(raw: String?): QuestionSourceType? = when (raw?.trim()?.lowercase()) {
            "official_pyq" -> OFFICIAL_PYQ
            "memory_based", "reconstructed" -> MEMORY_BASED
            "predicted_mock", "mock" -> PREDICTED_MOCK
            "practice" -> PRACTICE
            "current_affairs", "ca" -> CURRENT_AFFAIRS
            else -> null
        }

        fun infer(
            category: String,
            shiftYear: String,
            sourceStatus: String,
            sourceNote: String,
            rawSourceType: String? = null
        ): QuestionSourceType {
            val isVerified = sourceStatus == Question.SOURCE_VERIFIED && sourceNote.isNotBlank()
            val explicit = fromDbValue(rawSourceType)
            if (explicit != null) {
                return if (explicit == OFFICIAL_PYQ && !isVerified) MEMORY_BASED else explicit
            }
            return when {
                category == "mock" -> PREDICTED_MOCK
                category == "pyq" || shiftYear.isNotBlank() ->
                    if (isVerified) OFFICIAL_PYQ else MEMORY_BASED
                category == "ca" -> CURRENT_AFFAIRS
                else -> PRACTICE
            }
        }
    }
}

fun parseShiftYear(shiftYear: String): Pair<String, String> {
    val trimmed = shiftYear.trim()
    if (trimmed.isEmpty()) return "" to ""
    val dash = trimmed.indexOf('-')
    return if (dash > 0) {
        trimmed.substring(0, dash).trim() to trimmed.substring(dash + 1).trim()
    } else {
        trimmed to ""
    }
}

fun composeShiftYear(examYear: String, examShift: String): String {
    val y = examYear.trim()
    val s = examShift.trim()
    return when {
        y.isNotEmpty() && s.isNotEmpty() -> "$y-$s"
        y.isNotEmpty() -> y
        else -> ""
    }
}

data class Profession(
    val id: String,
    val name: String,
    val description: String = "",
    val sortOrder: Int = 0,
    val isActive: Boolean = true
)

data class Exam(
    val id: String,
    val professionId: String,
    val name: String,
    val shortName: String,
    val description: String = "",
    val defaultMockQuestions: Int = 100,
    val defaultMockMinutes: Int = 90,
    val positiveMarks: Double = 1.0,
    val negativeMarks: Double = 0.33,
    val sortOrder: Int = 0,
    val isActive: Boolean = true
)

data class ExamSection(
    val id: String,
    val examId: String,
    val name: String,
    val weightagePercent: Int = 0,
    val sortOrder: Int = 0
)

data class Topic(
    val id: String,
    val subjectCode: String,
    val name: String,
    val sortOrder: Int = 0
)

data class ExamQuestionLink(
    val examId: String,
    val questionId: String,
    val sectionId: String,
    val sortOrder: Int = 0,
    val isPyqForExam: Boolean = false,
    val examYear: String = "",
    val examShift: String = ""
)

enum class ExamCategory(val label: String, val shortLabel: String) {
    CENTRAL("Central & Railways", "Central"),
    AUTONOMOUS("Autonomous Institutes", "Autonomous"),
    STATE_CHO("State PSC, DHS & NHM/CHO", "State & CHO"),
    DEFENCE_PSU("Defence & PSU", "Defence & PSU")
}

object PlatformCatalogDefaults {
    const val EVERGREEN_BANK_LABEL = "10,000+ Qs"

    const val PROFESSION_NURSING = "nursing"

    // Central & Railways (5)
    const val EXAM_RRB_NURSING = "rrb_nursing"
    const val EXAM_AIIMS_NORCET = "aiims_norcet"
    const val EXAM_ESIC_NURSING = "esic_nursing"
    const val EXAM_DSSSB_NURSING = "dsssb_nursing"
    const val EXAM_RML_CENTRAL_NURSING = "rml_central_nursing"

    // Autonomous Institutes (12)
    const val EXAM_PGIMER_NURSING = "pgimer_nursing"
    const val EXAM_JIPMER_NURSING = "jipmer_nursing"
    const val EXAM_NIMHANS_NURSING = "nimhans_nursing"
    const val EXAM_NEIGRIHMS_NURSING = "neigrihms_nursing"
    const val EXAM_GIMS_NOIDA_NURSING = "gims_noida_nursing"
    const val EXAM_SGPGI_LUCKNOW_NURSING = "sgpgi_lucknow_nursing"
    const val EXAM_KGMU_LUCKNOW_NURSING = "kgmu_lucknow_nursing"
    const val EXAM_BHU_NURSING = "bhu_nursing"
    const val EXAM_GMCH_CHANDIGARH_NURSING = "gmch_chandigarh_nursing"
    const val EXAM_SCTIMST_TRIVANDRUM_NURSING = "sctimst_trivandrum_nursing"
    const val EXAM_UPUMS_SAIFAI_NURSING = "upums_saifai_nursing"
    const val EXAM_TMC_ICMR_NURSING = "tmc_icmr_nursing"

    // State PSC, DHS & NHM/CHO (9)
    const val EXAM_NHM_CHO_NURSING = "nhm_cho_nursing"
    const val EXAM_KERALA_PSC_NURSING = "kerala_psc_nursing"
    const val EXAM_RUHS_RAJASTHAN_NURSING = "ruhs_rajasthan_nursing"
    const val EXAM_OSSSC_ODISHA_NURSING = "osssc_odisha_nursing"
    const val EXAM_BTSC_BIHAR_NURSING = "btsc_bihar_nursing"
    const val EXAM_TN_MRB_NURSING = "tn_mrb_nursing"
    const val EXAM_MH_DMER_NURSING = "mh_dmer_nursing"
    const val EXAM_WBHRB_WEST_BENGAL_NURSING = "wbhrb_west_bengal_nursing"
    const val EXAM_HSSC_MP_NHM_NURSING = "hssc_mp_nhm_nursing"

    // Defence & PSU (3)
    const val EXAM_MNS_SSC_NURSING = "mns_ssc_nursing"
    const val EXAM_ISRO_NURSING = "isro_nursing"
    const val EXAM_PSU_NURSING = "psu_nursing"

    const val SECTION_RRB_NURSING_CORE = "rrb_nursing_core"
    const val SECTION_RRB_CA_GK = "rrb_nursing_ca_gk"
    const val SECTION_RRB_NON_TECH = "rrb_nursing_nontech"

    const val SECTION_NORCET_NURSING = "norcet_nursing_core"
    const val SECTION_NORCET_GK_APTITUDE = "norcet_gk_aptitude"

    const val SECTION_PGIMER_NURSING_CORE = "pgimer_nursing_core"

    val DEFAULT_PROFESSIONS = listOf(
        Profession(
            id = PROFESSION_NURSING,
            name = "Nursing",
            description = "All-India Central, Autonomous, State PSC/CHO & Defence Nursing Officer exams",
            sortOrder = 0,
            isActive = true
        )
    )

    val DEFAULT_EXAMS = listOf(
        // --- Central & Railways (5) ---
        Exam(
            id = EXAM_RRB_NURSING,
            professionId = PROFESSION_NURSING,
            name = "RRB Nursing Superintendent & Staff Nurse",
            shortName = "RRB Nursing",
            description = "100Q CBT (70% Nursing, 30% Non-Tech & CA/GK) • 90 min • +1 / -0.33",
            defaultMockQuestions = 100,
            defaultMockMinutes = 90,
            positiveMarks = 1.0,
            negativeMarks = 0.33,
            sortOrder = 0,
            isActive = true
        ),
        Exam(
            id = EXAM_AIIMS_NORCET,
            professionId = PROFESSION_NURSING,
            name = "AIIMS NORCET (Prelims & Mains)",
            shortName = "AIIMS NORCET",
            description = "Prelims: 100Q / 90m (5×18m locked sections, 80:20) • Mains: 160Q / 180m (100% Clinical) • +1 / -0.33",
            defaultMockQuestions = 100,
            defaultMockMinutes = 90,
            positiveMarks = 1.0,
            negativeMarks = 0.33,
            sortOrder = 1,
            isActive = true
        ),
        Exam(
            id = EXAM_PGIMER_NURSING,
            professionId = PROFESSION_NURSING,
            name = "PGIMER Chandigarh Nursing Officer",
            shortName = "PGIMER",
            description = "100Q CBT (100% Core Nursing) • 100 min • +1 / -0.25 marking • UR 40% / Reserved 35% cutoff",
            defaultMockQuestions = 100,
            defaultMockMinutes = 100,
            positiveMarks = 1.0,
            negativeMarks = 0.25,
            sortOrder = 2,
            isActive = true
        ),
        Exam(
            id = EXAM_ESIC_NURSING,
            professionId = PROFESSION_NURSING,
            name = "ESIC Nursing Officer (UPSC / ESIC)",
            shortName = "ESIC",
            description = "125Q CBT (80% Core Nursing, 20% Aptitude & GK) • 120 min • +1 / -0.33 marking",
            defaultMockQuestions = 125,
            defaultMockMinutes = 120,
            positiveMarks = 1.0,
            negativeMarks = 0.33,
            sortOrder = 3,
            isActive = true
        ),
        Exam(
            id = EXAM_DSSSB_NURSING,
            professionId = PROFESSION_NURSING,
            name = "DSSSB Nursing Officer (Delhi)",
            shortName = "DSSSB",
            description = "200Q CBT (100 Nursing + 100 General Section) • 120 min • +1 / -0.25 marking",
            defaultMockQuestions = 200,
            defaultMockMinutes = 120,
            positiveMarks = 1.0,
            negativeMarks = 0.25,
            sortOrder = 4,
            isActive = true
        ),
        Exam(
            id = EXAM_RML_CENTRAL_NURSING,
            professionId = PROFESSION_NURSING,
            name = "RML / Safdarjung / Central Govt Hospitals",
            shortName = "RML & Central",
            description = "100Q CBT (80% Nursing Core, 20% GK & Aptitude) • 90 min • +1 / -0.25 marking",
            defaultMockQuestions = 100,
            defaultMockMinutes = 90,
            positiveMarks = 1.0,
            negativeMarks = 0.25,
            sortOrder = 5,
            isActive = true
        ),
        // --- Autonomous Institutes (11 more + PGIMER above = 12) ---
        Exam(
            id = EXAM_JIPMER_NURSING,
            professionId = PROFESSION_NURSING,
            name = "JIPMER Puducherry Nursing Officer",
            shortName = "JIPMER",
            description = "100Q CBT (70% Nursing, 30% Aptitude & GK) • 90 min • +4 / -1 marking",
            defaultMockQuestions = 100,
            defaultMockMinutes = 90,
            positiveMarks = 4.0,
            negativeMarks = 1.0,
            sortOrder = 6,
            isActive = true
        ),
        Exam(
            id = EXAM_NIMHANS_NURSING,
            professionId = PROFESSION_NURSING,
            name = "NIMHANS Bengaluru Nursing Officer",
            shortName = "NIMHANS",
            description = "100Q CBT (100% Core Nursing with Neuro & Psych focus) • 90 min • +1 / -0.25",
            defaultMockQuestions = 100,
            defaultMockMinutes = 90,
            positiveMarks = 1.0,
            negativeMarks = 0.25,
            sortOrder = 7,
            isActive = true
        ),
        Exam(
            id = EXAM_NEIGRIHMS_NURSING,
            professionId = PROFESSION_NURSING,
            name = "NEIGRIHMS Shillong Nursing Officer",
            shortName = "NEIGRIHMS",
            description = "100Q CBT (100% Core Nursing Sciences) • 90 min • +1 / No Negative Marking",
            defaultMockQuestions = 100,
            defaultMockMinutes = 90,
            positiveMarks = 1.0,
            negativeMarks = 0.0,
            sortOrder = 8,
            isActive = true
        ),
        Exam(
            id = EXAM_GIMS_NOIDA_NURSING,
            professionId = PROFESSION_NURSING,
            name = "GIMS Greater Noida Staff Nurse",
            shortName = "GIMS Noida",
            description = "100Q CBT (85% Nursing Core, 15% GK & Aptitude) • 120 min • +1 / -0.25",
            defaultMockQuestions = 100,
            defaultMockMinutes = 120,
            positiveMarks = 1.0,
            negativeMarks = 0.25,
            sortOrder = 9,
            isActive = true
        ),
        Exam(
            id = EXAM_SGPGI_LUCKNOW_NURSING,
            professionId = PROFESSION_NURSING,
            name = "SGPGIMS Lucknow Nursing Officer",
            shortName = "SGPGI Lucknow",
            description = "100Q CBT (60% Nursing, 40% GK, Science & Reasoning) • 120 min • +1 / -0.33",
            defaultMockQuestions = 100,
            defaultMockMinutes = 120,
            positiveMarks = 1.0,
            negativeMarks = 0.33,
            sortOrder = 10,
            isActive = true
        ),
        Exam(
            id = EXAM_KGMU_LUCKNOW_NURSING,
            professionId = PROFESSION_NURSING,
            name = "KGMU Lucknow Nursing Officer",
            shortName = "KGMU Lucknow",
            description = "100Q CBT (60% Nursing, 40% General Aptitude & Science) • 120 min • +1 / -0.33",
            defaultMockQuestions = 100,
            defaultMockMinutes = 120,
            positiveMarks = 1.0,
            negativeMarks = 0.33,
            sortOrder = 11,
            isActive = true
        ),
        Exam(
            id = EXAM_BHU_NURSING,
            professionId = PROFESSION_NURSING,
            name = "BHU IMS Varanasi Nursing Officer",
            shortName = "BHU IMS",
            description = "100Q CBT (100% Core Nursing) • 90 min • +1 / -0.25 marking",
            defaultMockQuestions = 100,
            defaultMockMinutes = 90,
            positiveMarks = 1.0,
            negativeMarks = 0.25,
            sortOrder = 12,
            isActive = true
        ),
        Exam(
            id = EXAM_GMCH_CHANDIGARH_NURSING,
            professionId = PROFESSION_NURSING,
            name = "GMCH Chandigarh Nursing Officer",
            shortName = "GMCH",
            description = "100Q CBT (70% Nursing, 30% Practical & General Aptitude) • 120 min • +1 / -0.25",
            defaultMockQuestions = 100,
            defaultMockMinutes = 120,
            positiveMarks = 1.0,
            negativeMarks = 0.25,
            sortOrder = 13,
            isActive = true
        ),
        Exam(
            id = EXAM_SCTIMST_TRIVANDRUM_NURSING,
            professionId = PROFESSION_NURSING,
            name = "SCTIMST Trivandrum Staff Nurse",
            shortName = "SCTIMST",
            description = "100Q CBT (100% Core Nursing with Cardio-Neuro focus) • 90 min • +1 / No Negative",
            defaultMockQuestions = 100,
            defaultMockMinutes = 90,
            positiveMarks = 1.0,
            negativeMarks = 0.0,
            sortOrder = 14,
            isActive = true
        ),
        Exam(
            id = EXAM_UPUMS_SAIFAI_NURSING,
            professionId = PROFESSION_NURSING,
            name = "UPUMS Saifai Nursing Officer",
            shortName = "UPUMS Saifai",
            description = "200Q CBT (85% Nursing, 15% GK & Aptitude) • 180 min • +3 / No Negative",
            defaultMockQuestions = 200,
            defaultMockMinutes = 180,
            positiveMarks = 3.0,
            negativeMarks = 0.0,
            sortOrder = 15,
            isActive = true
        ),
        Exam(
            id = EXAM_TMC_ICMR_NURSING,
            professionId = PROFESSION_NURSING,
            name = "TMC Mumbai / ACTREC / ICMR Nurse",
            shortName = "TMC & ICMR",
            description = "100Q CBT (70% Nursing & Oncology/Research, 30% Aptitude) • 90 min • +1 / -0.25",
            defaultMockQuestions = 100,
            defaultMockMinutes = 90,
            positiveMarks = 1.0,
            negativeMarks = 0.25,
            sortOrder = 16,
            isActive = true
        ),
        // --- State PSC, DHS & NHM/CHO (9) ---
        Exam(
            id = EXAM_NHM_CHO_NURSING,
            professionId = PROFESSION_NURSING,
            name = "NHM / CHO (Community Health Officer)",
            shortName = "NHM / CHO",
            description = "100Q Exam (80% CHN, OBG & Primary Care, 20% Health Programs) • 90 min • +1 / No Neg",
            defaultMockQuestions = 100,
            defaultMockMinutes = 90,
            positiveMarks = 1.0,
            negativeMarks = 0.0,
            sortOrder = 17,
            isActive = true
        ),
        Exam(
            id = EXAM_KERALA_PSC_NURSING,
            professionId = PROFESSION_NURSING,
            name = "Kerala PSC / DHS Staff Nurse",
            shortName = "Kerala PSC",
            description = "100Q OMR/CBT (80% Nursing Core, 20% GK & Current Affairs) • 75 min • +1 / -0.33",
            defaultMockQuestions = 100,
            defaultMockMinutes = 75,
            positiveMarks = 1.0,
            negativeMarks = 0.33,
            sortOrder = 18,
            isActive = true
        ),
        Exam(
            id = EXAM_RUHS_RAJASTHAN_NURSING,
            professionId = PROFESSION_NURSING,
            name = "RUHS / RSSB Rajasthan Nursing Officer",
            shortName = "RUHS / RSSB",
            description = "100Q Exam (100% Core Nursing Syllabus) • 120 min • +1 / No Negative Marking",
            defaultMockQuestions = 100,
            defaultMockMinutes = 120,
            positiveMarks = 1.0,
            negativeMarks = 0.0,
            sortOrder = 19,
            isActive = true
        ),
        Exam(
            id = EXAM_OSSSC_ODISHA_NURSING,
            professionId = PROFESSION_NURSING,
            name = "OSSSC Odisha Nursing Officer",
            shortName = "OSSSC Odisha",
            description = "100Q Exam (85% Nursing Core, 15% Arithmetic & GK) • 120 min • +1 / -0.25",
            defaultMockQuestions = 100,
            defaultMockMinutes = 120,
            positiveMarks = 1.0,
            negativeMarks = 0.25,
            sortOrder = 20,
            isActive = true
        ),
        Exam(
            id = EXAM_BTSC_BIHAR_NURSING,
            professionId = PROFESSION_NURSING,
            name = "BTSC / SHSB Bihar Staff Nurse",
            shortName = "BTSC Bihar",
            description = "100Q CBT (100% GNM/B.Sc Core Nursing) • 120 min • +1 / -0.25 marking",
            defaultMockQuestions = 100,
            defaultMockMinutes = 120,
            positiveMarks = 1.0,
            negativeMarks = 0.25,
            sortOrder = 21,
            isActive = true
        ),
        Exam(
            id = EXAM_TN_MRB_NURSING,
            professionId = PROFESSION_NURSING,
            name = "TN MRB Tamil Nadu Staff Nurse",
            shortName = "TN MRB",
            description = "200Q CBT (100% Diploma/B.Sc Nursing Core) • 150 min • +1 / No Negative Marking",
            defaultMockQuestions = 200,
            defaultMockMinutes = 150,
            positiveMarks = 1.0,
            negativeMarks = 0.0,
            sortOrder = 22,
            isActive = true
        ),
        Exam(
            id = EXAM_MH_DMER_NURSING,
            professionId = PROFESSION_NURSING,
            name = "Maharashtra DMER / DHS Staff Nurse",
            shortName = "MH DMER",
            description = "100Q CBT (60% Nursing Core, 40% GK & Aptitude) • 90 min • +1 / -0.25",
            defaultMockQuestions = 100,
            defaultMockMinutes = 90,
            positiveMarks = 1.0,
            negativeMarks = 0.25,
            sortOrder = 23,
            isActive = true
        ),
        Exam(
            id = EXAM_WBHRB_WEST_BENGAL_NURSING,
            professionId = PROFESSION_NURSING,
            name = "WBHRB West Bengal Staff Nurse",
            shortName = "WBHRB",
            description = "50Q Screening (80% Nursing Core, 20% GK & Health Drives) • 60 min • +2 / -0.5",
            defaultMockQuestions = 50,
            defaultMockMinutes = 60,
            positiveMarks = 2.0,
            negativeMarks = 0.5,
            sortOrder = 24,
            isActive = true
        ),
        Exam(
            id = EXAM_HSSC_MP_NHM_NURSING,
            professionId = PROFESSION_NURSING,
            name = "HSSC / MPESB / State NHM Staff Nurse",
            shortName = "HSSC & MPESB",
            description = "100Q CBT (75% Nursing Core, 25% GK, Science & Reasoning) • 90 min • +1 / No Neg",
            defaultMockQuestions = 100,
            defaultMockMinutes = 90,
            positiveMarks = 1.0,
            negativeMarks = 0.0,
            sortOrder = 25,
            isActive = true
        ),
        // --- Defence & PSU (3) ---
        Exam(
            id = EXAM_MNS_SSC_NURSING,
            professionId = PROFESSION_NURSING,
            name = "MNS (Military Nursing Service SSC)",
            shortName = "MNS (Army)",
            description = "150Q CBT (65% Nursing, 35% Science, Reasoning & GK) • 120 min • +1 / -0.5",
            defaultMockQuestions = 150,
            defaultMockMinutes = 120,
            positiveMarks = 1.0,
            negativeMarks = 0.5,
            sortOrder = 26,
            isActive = true
        ),
        Exam(
            id = EXAM_ISRO_NURSING,
            professionId = PROFESSION_NURSING,
            name = "ISRO (VSSC / SDSC) Nurse-B",
            shortName = "ISRO Nurse-B",
            description = "80Q CBT (100% Core Clinical & Emergency Nursing) • 120 min • +1 / -0.33",
            defaultMockQuestions = 80,
            defaultMockMinutes = 120,
            positiveMarks = 1.0,
            negativeMarks = 0.33,
            sortOrder = 27,
            isActive = true
        ),
        Exam(
            id = EXAM_PSU_NURSING,
            professionId = PROFESSION_NURSING,
            name = "PSU Staff Nurse (SAIL / Coal India / ONGC / BHEL)",
            shortName = "PSU Nurse",
            description = "100Q CBT (70% Nursing & Occupational Health, 30% Aptitude) • 90 min • +1 / -0.25",
            defaultMockQuestions = 100,
            defaultMockMinutes = 90,
            positiveMarks = 1.0,
            negativeMarks = 0.25,
            sortOrder = 28,
            isActive = true
        )
    )

    private val examByIdMap: Map<String, Exam> = DEFAULT_EXAMS.associateBy { it.id }

    fun examById(examId: String?): Exam? = if (examId.isNullOrBlank()) null else examByIdMap[examId]

    fun examCategoryFor(examId: String?): ExamCategory = when (examId) {
        EXAM_RRB_NURSING,
        EXAM_AIIMS_NORCET,
        EXAM_ESIC_NURSING,
        EXAM_DSSSB_NURSING,
        EXAM_RML_CENTRAL_NURSING -> ExamCategory.CENTRAL

        EXAM_PGIMER_NURSING,
        EXAM_JIPMER_NURSING,
        EXAM_NIMHANS_NURSING,
        EXAM_NEIGRIHMS_NURSING,
        EXAM_GIMS_NOIDA_NURSING,
        EXAM_SGPGI_LUCKNOW_NURSING,
        EXAM_KGMU_LUCKNOW_NURSING,
        EXAM_BHU_NURSING,
        EXAM_GMCH_CHANDIGARH_NURSING,
        EXAM_SCTIMST_TRIVANDRUM_NURSING,
        EXAM_UPUMS_SAIFAI_NURSING,
        EXAM_TMC_ICMR_NURSING -> ExamCategory.AUTONOMOUS

        EXAM_NHM_CHO_NURSING,
        EXAM_KERALA_PSC_NURSING,
        EXAM_RUHS_RAJASTHAN_NURSING,
        EXAM_OSSSC_ODISHA_NURSING,
        EXAM_BTSC_BIHAR_NURSING,
        EXAM_TN_MRB_NURSING,
        EXAM_MH_DMER_NURSING,
        EXAM_WBHRB_WEST_BENGAL_NURSING,
        EXAM_HSSC_MP_NHM_NURSING -> ExamCategory.STATE_CHO

        EXAM_MNS_SSC_NURSING,
        EXAM_ISRO_NURSING,
        EXAM_PSU_NURSING -> ExamCategory.DEFENCE_PSU

        else -> ExamCategory.CENTRAL
    }

    fun nursingWeightagePercent(examId: String?): Int = when (examId) {
        EXAM_PGIMER_NURSING,
        EXAM_NIMHANS_NURSING,
        EXAM_NEIGRIHMS_NURSING,
        EXAM_BHU_NURSING,
        EXAM_SCTIMST_TRIVANDRUM_NURSING,
        EXAM_RUHS_RAJASTHAN_NURSING,
        EXAM_BTSC_BIHAR_NURSING,
        EXAM_TN_MRB_NURSING,
        EXAM_ISRO_NURSING -> 100

        EXAM_GIMS_NOIDA_NURSING,
        EXAM_UPUMS_SAIFAI_NURSING,
        EXAM_OSSSC_ODISHA_NURSING -> 85

        EXAM_AIIMS_NORCET,
        EXAM_ESIC_NURSING,
        EXAM_RML_CENTRAL_NURSING,
        EXAM_NHM_CHO_NURSING,
        EXAM_KERALA_PSC_NURSING,
        EXAM_WBHRB_WEST_BENGAL_NURSING -> 80

        EXAM_HSSC_MP_NHM_NURSING -> 75

        EXAM_RRB_NURSING,
        EXAM_JIPMER_NURSING,
        EXAM_GMCH_CHANDIGARH_NURSING,
        EXAM_TMC_ICMR_NURSING,
        EXAM_PSU_NURSING -> 70

        EXAM_MNS_SSC_NURSING -> 65

        EXAM_SGPGI_LUCKNOW_NURSING,
        EXAM_KGMU_LUCKNOW_NURSING,
        EXAM_MH_DMER_NURSING -> 60

        EXAM_DSSSB_NURSING -> 50

        else -> 70
    }

    fun examIsPureNursing(examId: String?): Boolean = nursingWeightagePercent(examId) >= 100

    fun examSupportsCaGk(examId: String?): Boolean = !examIsPureNursing(examId)

    fun examSupportsNonTech(examId: String?): Boolean = !examIsPureNursing(examId)

    fun isSharedSubjectForExam(examId: String?, subjectCode: String): Boolean = when {
        examIsPureNursing(examId) -> SubjectCatalog.isNursingCode(subjectCode)
        examId == EXAM_RRB_NURSING -> true
        else -> subjectCode != "Railways"
    }

    val DEFAULT_SECTIONS: List<ExamSection> = buildList {
        add(ExamSection(SECTION_RRB_NURSING_CORE, EXAM_RRB_NURSING, "Nursing Core (70%)", 70, 0))
        add(ExamSection(SECTION_RRB_CA_GK, EXAM_RRB_NURSING, "Current Affairs & Static GK", 10, 1))
        add(ExamSection(SECTION_RRB_NON_TECH, EXAM_RRB_NURSING, "Science, Arithmetic & Reasoning (30%)", 20, 2))

        add(ExamSection(SECTION_NORCET_NURSING, EXAM_AIIMS_NORCET, "Nursing Core & Clinical Scenarios (80%)", 80, 0))
        add(ExamSection(SECTION_NORCET_GK_APTITUDE, EXAM_AIIMS_NORCET, "General Knowledge & Aptitude (20%)", 20, 1))

        add(ExamSection(SECTION_PGIMER_NURSING_CORE, EXAM_PGIMER_NURSING, "Core Nursing Sciences (100%)", 100, 0))

        DEFAULT_EXAMS
            .filter { it.id != EXAM_RRB_NURSING && it.id != EXAM_AIIMS_NORCET && it.id != EXAM_PGIMER_NURSING }
            .forEach { exam ->
                val nPct = nursingWeightagePercent(exam.id)
                if (nPct >= 100) {
                    add(ExamSection("${exam.id}_nursing", exam.id, "Core Nursing Sciences (100%)", 100, 0))
                } else {
                    add(ExamSection("${exam.id}_nursing", exam.id, "Nursing Core ($nPct%)", nPct, 0))
                    add(ExamSection("${exam.id}_general", exam.id, "GK, Aptitude & Science (${100 - nPct}%)", 100 - nPct, 1))
                }
            }
    }

    fun isNorcetExam(examId: String?): Boolean = examId == EXAM_AIIMS_NORCET

    fun isPgimerExam(examId: String?): Boolean = examId == EXAM_PGIMER_NURSING

    /** Only exams that genuinely have separate Prelims & Mains stages return true. */
    fun supportsStages(examId: String?): Boolean = examId == EXAM_AIIMS_NORCET

    /**
     * Strictly partitions AIIMS NORCET questions into disjoint Stage I Prelims vs Stage II Mains pools.
     * - Stage II Mains: 100% Core Clinical Nursing (dedicated [Mains ...] clinical cases + Hard clinical nursing practice).
     * - Stage I Prelims: Prelims PYQ recall papers (NORCET-5..10), 80% Prelims Nursing & 20% GK/Aptitude.
     * Non-nursing questions NEVER enter Stage II Mains, and [Mains] clinical case items NEVER mix into Prelims.
     */
    fun stageForNorcetQuestion(question: Question): ExamStage {
        if (!SubjectCatalog.isNursing(question)) return ExamStage.PRELIMS
        if (question.questionText.contains("[Mains", ignoreCase = true)) return ExamStage.MAINS
        val isNorcetRecallPyq = question.id.startsWith("N_") &&
            (question.category == "pyq" || question.effectiveShiftYear.startsWith("NORCET", ignoreCase = true))
        if (isNorcetRecallPyq) return ExamStage.PRELIMS
        if ((question.category == "nur" || question.isPredictedMock) && question.difficulty == QuestionDifficulty.HARD) {
            return ExamStage.MAINS
        }
        return ExamStage.PRELIMS
    }

    fun filterQuestionsForExamStage(
        examId: String?,
        stage: ExamStage,
        questions: List<QuestionItem>
    ): List<QuestionItem> {
        if (!supportsStages(examId)) return questions
        return questions.filter { stageForNorcetQuestion(it.question) == stage }
    }

    fun positiveMarksForExam(examId: String?): Double =
        examById(examId)?.positiveMarks ?: 1.0

    fun negativeMarksForExam(examId: String?): Double =
        examById(examId)?.negativeMarks ?: 0.33

    fun formatMarkingLabel(exam: Exam): String {
        val pos = if (exam.positiveMarks % 1.0 == 0.0) "+${exam.positiveMarks.toInt()}" else "+${exam.positiveMarks}"
        val neg = if (exam.negativeMarks <= 0.0) "No Neg" else {
            val n = if (exam.negativeMarks % 1.0 == 0.0) exam.negativeMarks.toInt().toString() else exam.negativeMarks.toString()
            "-$n"
        }
        return "$pos / $neg"
    }

    fun isNorcetSharedSubject(subjectCode: String): Boolean = subjectCode != "Railways"

    fun isPgimerSharedSubject(subjectCode: String): Boolean = SubjectCatalog.isNursingCode(subjectCode)

    fun defaultTopicIdFor(subjectCode: String): String {
        val clean = subjectCode.trim().lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
        return if (clean.isEmpty()) "general" else "${clean}_general"
    }

    fun sectionIdForRrbSubject(subjectCode: String): String = when {
        SubjectCatalog.isNursingCode(subjectCode) -> SECTION_RRB_NURSING_CORE
        SubjectCatalog.isCaCode(subjectCode) -> SECTION_RRB_CA_GK
        else -> SECTION_RRB_NON_TECH
    }

    fun sectionIdForNorcetSubject(subjectCode: String): String =
        if (SubjectCatalog.isNursingCode(subjectCode)) SECTION_NORCET_NURSING else SECTION_NORCET_GK_APTITUDE

    fun sectionIdForPgimerSubject(subjectCode: String): String = SECTION_PGIMER_NURSING_CORE

    fun sectionIdForExam(examId: String, subjectCode: String): String = when (examId) {
        EXAM_RRB_NURSING -> sectionIdForRrbSubject(subjectCode)
        EXAM_AIIMS_NORCET -> sectionIdForNorcetSubject(subjectCode)
        EXAM_PGIMER_NURSING -> sectionIdForPgimerSubject(subjectCode)
        else -> if (examIsPureNursing(examId) || SubjectCatalog.isNursingCode(subjectCode)) {
            "${examId}_nursing"
        } else {
            "${examId}_general"
        }
    }
}

enum class ExamStage(val id: String, val label: String, val shortLabel: String) {
    PRELIMS("prelims", "Stage I · Prelims", "Prelims"),
    MAINS("mains", "Stage II · Mains", "Mains")
}

enum class PracticePathway(val label: String) {
    BY_EXAM("By Exam"),
    BY_SUBJECT("By Subject")
}

object SubjectPracticeLogic {
    fun normalizeStem(text: String): String {
        val len = text.length
        if (len == 0) return ""
        val sb = StringBuilder(len)
        var inSpace = true
        for (i in 0 until len) {
            val ch = text[i]
            if (ch.isWhitespace()) {
                if (!inSpace) {
                    sb.append(' ')
                    inSpace = true
                }
            } else {
                sb.append(ch.lowercaseChar())
                inSpace = false
            }
        }
        if (sb.isNotEmpty() && sb[sb.length - 1] == ' ') {
            sb.setLength(sb.length - 1)
        }
        return sb.toString()
    }

    /** Deduplicates questions by ID and normalized stem, preserving higher progress/bookmark state. */
    fun deduplicateQuestions(items: List<QuestionItem>): List<QuestionItem> {
        val byId = LinkedHashMap<String, QuestionItem>(items.size * 2 + 1)
        for (item in items) {
            val existing = byId[item.id]
            if (existing == null || item.attemptsCount > existing.attemptsCount || (!existing.isBookmarked && item.isBookmarked)) {
                byId[item.id] = item
            }
        }
        val byStem = LinkedHashMap<String, QuestionItem>(byId.size * 2 + 1)
        for (item in byId.values) {
            val stemKey = "${item.question.subject}|${normalizeStem(item.question.questionText)}"
            val existing = byStem[stemKey]
            if (existing == null || item.attemptsCount > existing.attemptsCount || (!existing.isBookmarked && item.isBookmarked)) {
                byStem[stemKey] = item
            }
        }
        return byStem.values.toList()
    }

    private fun matchesExamScope(
        item: QuestionItem,
        allowedIds: Set<String>?,
        examIdFilter: String?,
        includeVirtualShared: Boolean
    ): Boolean {
        if (allowedIds == null) return true
        if (item.id in allowedIds) return true
        if (!includeVirtualShared || examIdFilter.isNullOrBlank()) return false
        val q = item.question
        return !q.isPyqTagged && !q.isCustom && PlatformCatalogDefaults.isSharedSubjectForExam(examIdFilter, q.subject)
    }

    fun collectQuestionsForSubject(
        allQuestions: List<QuestionItem>,
        allLinks: List<ExamQuestionLink>,
        subjectCode: String,
        examIdFilter: String? = null,
        difficultyFilter: DifficultyFilter = DifficultyFilter.ALL,
        excludePyqAndMock: Boolean = false,
        alreadyDeduplicated: Boolean = false,
        includeVirtualShared: Boolean = false
    ): List<QuestionItem> {
        val allowedIds = if (!examIdFilter.isNullOrBlank()) {
            val set = HashSet<String>()
            for (link in allLinks) {
                if (link.examId == examIdFilter) set.add(link.questionId)
            }
            set
        } else {
            null
        }
        val targetDiff = difficultyFilter.difficulty
        val matched = ArrayList<QuestionItem>()
        for (item in allQuestions) {
            val q = item.question
            if (q.subject == subjectCode &&
                matchesExamScope(item, allowedIds, examIdFilter, includeVirtualShared) &&
                (targetDiff == null || q.difficulty == targetDiff) &&
                (!excludePyqAndMock || (!q.isPyqTagged && !q.isPredictedMock))
            ) {
                matched.add(item)
            }
        }
        return if (alreadyDeduplicated) matched else deduplicateQuestions(matched)
    }

    /** Single-pass O(N) grouping of cross-exam subject practice questions across all 16 subjects. */
    fun groupQuestionsBySubjectForPractice(
        allQuestions: List<QuestionItem>,
        allLinks: List<ExamQuestionLink>,
        examIdFilter: String? = null,
        difficultyFilter: DifficultyFilter = DifficultyFilter.ALL,
        excludePyqAndMock: Boolean = true,
        alreadyDeduplicated: Boolean = false,
        includeVirtualShared: Boolean = false
    ): List<Pair<SubjectMeta, List<QuestionItem>>> {
        val allowedIds = if (!examIdFilter.isNullOrBlank()) {
            val set = HashSet<String>()
            for (link in allLinks) {
                if (link.examId == examIdFilter) set.add(link.questionId)
            }
            set
        } else {
            null
        }
        val source = if (alreadyDeduplicated) allQuestions else deduplicateQuestions(allQuestions)
        val targetDiff = difficultyFilter.difficulty
        val bySubject = HashMap<String, ArrayList<QuestionItem>>(SubjectCatalog.ALL_SUBJS.size * 2)
        for (item in source) {
            val q = item.question
            if (matchesExamScope(item, allowedIds, examIdFilter, includeVirtualShared) &&
                (targetDiff == null || q.difficulty == targetDiff) &&
                (!excludePyqAndMock || (!q.isPyqTagged && !q.isPredictedMock))
            ) {
                bySubject.getOrPut(q.subject) { ArrayList() }.add(item)
            }
        }
        return SubjectCatalog.ALL_SUBJS.mapNotNull { meta ->
            val list = bySubject[meta.code]
            if (list.isNullOrEmpty()) null else meta to list
        }
    }

    fun computeCrossExamSubjectStats(
        allQuestions: List<QuestionItem>,
        allLinks: List<ExamQuestionLink>,
        examIdFilter: String? = null,
        alreadyDeduplicated: Boolean = false,
        includeVirtualShared: Boolean = false
    ): List<SubjectStat> {
        val allowedIds = if (!examIdFilter.isNullOrBlank()) {
            val set = HashSet<String>()
            for (link in allLinks) {
                if (link.examId == examIdFilter) set.add(link.questionId)
            }
            set
        } else {
            null
        }
        val source = if (alreadyDeduplicated) {
            allQuestions
        } else {
            deduplicateQuestions(allQuestions)
        }
        val grouped = HashMap<String, ArrayList<QuestionItem>>(SubjectCatalog.ALL_SUBJS.size * 2)
        for (item in source) {
            if (matchesExamScope(item, allowedIds, examIdFilter, includeVirtualShared)) {
                grouped.getOrPut(item.question.subject) { ArrayList() }.add(item)
            }
        }
        return SubjectCatalog.ALL_SUBJS.mapNotNull { meta ->
            val subItems = grouped[meta.code]
            if (subItems.isNullOrEmpty()) return@mapNotNull null
            var attempted = 0
            var totalAttempts = 0
            var correct = 0
            var wrong = 0
            var mastered = 0
            for (item in subItems) {
                val p = item.progress
                if (p.attemptsCount > 0) attempted++
                totalAttempts += p.attemptsCount
                correct += p.correctCount
                wrong += p.wrongCount
                if (p.lastOutcome == "right") mastered++
            }
            SubjectStat(
                code = meta.code,
                name = meta.name,
                group = meta.group,
                totalQuestions = subItems.size,
                attemptedQuestions = attempted,
                totalAttempts = totalAttempts,
                correctAttempts = correct,
                wrongAttempts = wrong,
                masteredQuestions = mastered,
                accentColor = meta.accentColor
            )
        }
    }
}

object MockSelectionLogic {
    /**
     * Selects a standard CBT mock (RRB 70:30, NORCET Prelims 80:20, PGIMER 100% nursing)
     * strictly EXCLUDING PYQs and prioritizing the 500 High-Probability Predicted Mock pool first.
     */
    fun selectStandardMock(
        pool: List<QuestionItem>,
        examId: String?,
        totalQuestions: Int,
        shuffle: Boolean = true
    ): List<QuestionItem> {
        val mockEligible = pool.filter { !it.question.isPyqTagged }
        if (mockEligible.isEmpty()) return emptyList()

        val nursingPct = PlatformCatalogDefaults.nursingWeightagePercent(examId)
        val nursingCount = (totalQuestions * nursingPct) / 100
        val nonNursingCount = totalQuestions - nursingCount

        fun ordered(items: List<QuestionItem>): List<QuestionItem> {
            val predicted = items.filter { it.question.isPredictedMock }
            val practice = items.filter { !it.question.isPredictedMock }
            return if (shuffle) {
                predicted.shuffled() + practice.shuffled()
            } else {
                predicted + practice
            }
        }

        val nurPool = ordered(mockEligible.filter { SubjectCatalog.isNursing(it.question) })
        val nonPool = ordered(mockEligible.filter { !SubjectCatalog.isNursing(it.question) })
        val selected = (nurPool.take(nursingCount) + nonPool.take(nonNursingCount)).toMutableList()

        if (selected.size < totalQuestions) {
            val usedIds = selected.map { it.id }.toHashSet()
            selected += ordered(mockEligible.filter { it.id !in usedIds }).take(totalQuestions - selected.size)
        }
        return selected
    }

    /**
     * Selects a Stage II AIIMS NORCET Mains clinical CBT mock strictly EXCLUDING PYQs,
     * prioritizing High-Probability Predicted Mock Mains clinical vignettes first.
     */
    fun selectNorcetMainsMock(
        allQuestions: List<QuestionItem>,
        totalQuestions: Int,
        shuffle: Boolean = true
    ): List<QuestionItem> {
        val mainsPool = PlatformCatalogDefaults.filterQuestionsForExamStage(
            examId = PlatformCatalogDefaults.EXAM_AIIMS_NORCET,
            stage = ExamStage.MAINS,
            questions = allQuestions
        ).filter { !it.question.isPyqTagged }
        if (mainsPool.isEmpty()) return emptyList()

        val predictedMains = mainsPool.filter { it.question.isPredictedMock }
        val dedicatedOtherMains = mainsPool.filter {
            !it.question.isPredictedMock && it.question.questionText.contains("[Mains", ignoreCase = true)
        }
        val remainingClinical = mainsPool.filter {
            !it.question.isPredictedMock && !it.question.questionText.contains("[Mains", ignoreCase = true)
        }
        val ordered = if (shuffle) {
            predictedMains.shuffled() + dedicatedOtherMains.shuffled() + remainingClinical.shuffled()
        } else {
            predictedMains + dedicatedOtherMains + remainingClinical
        }
        return ordered.take(totalQuestions)
    }
}

data class Question(
    val id: String,
    val category: String,      // Legacy: "pyq" | "nur" | "ca" | "gen"
    val shiftYear: String,     // Legacy combined tag: e.g. "2025-S1", "2015", ""
    val subject: String,       // Matches subjects.code: e.g. "MSN", "Pharm", "Science"
    val difficulty: QuestionDifficulty = QuestionDifficulty.MEDIUM,
    val questionText: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
    val isCustom: Boolean = false,
    val sourceStatus: String = SOURCE_UNVERIFIED, // "unverified" | "verified"
    val sourceNote: String = "",
    val topicId: String = PlatformCatalogDefaults.defaultTopicIdFor(subject),
    val sourceType: QuestionSourceType = QuestionSourceType.infer(category, shiftYear, sourceStatus, sourceNote),
    val examYear: String = parseShiftYear(shiftYear).first,
    val examShift: String = parseShiftYear(shiftYear).second
) {
    val isPyqTagged: Boolean
        get() = sourceType == QuestionSourceType.OFFICIAL_PYQ ||
            sourceType == QuestionSourceType.MEMORY_BASED ||
            category == "pyq" ||
            shiftYear.isNotBlank() ||
            examYear.isNotBlank()

    val isPredictedMock: Boolean
        get() = sourceType == QuestionSourceType.PREDICTED_MOCK || category == "mock"

    val isVerifiedSource: Boolean
        get() = sourceStatus == SOURCE_VERIFIED && sourceNote.isNotBlank()

    val effectiveShiftYear: String
        get() = shiftYear.ifBlank { composeShiftYear(examYear, examShift) }

    fun pyqBadgeText(): String? {
        if (!isPyqTagged) return null
        val sy = effectiveShiftYear
        return if (isVerifiedSource) {
            if (sy.isNotBlank()) "Verified PYQ • $sy" else "Verified PYQ"
        } else {
            "Unverified PYQ tag"
        }
    }

    fun sourceTypeBadgeText(): String {
        val sy = effectiveShiftYear
        return when (sourceType) {
            QuestionSourceType.OFFICIAL_PYQ ->
                if (isVerifiedSource && sy.isNotBlank()) "Official PYQ • $sy"
                else if (isVerifiedSource) "Official PYQ"
                else "Memory-Based PYQ"
            QuestionSourceType.MEMORY_BASED -> "Memory-Based PYQ"
            QuestionSourceType.PREDICTED_MOCK -> "Predicted Mock"
            QuestionSourceType.CURRENT_AFFAIRS -> "Current Affairs"
            QuestionSourceType.PRACTICE -> "Practice"
        }
    }

    companion object {
        const val SOURCE_UNVERIFIED = "unverified"
        const val SOURCE_VERIFIED = "verified"
    }
}

data class QuestionProgress(
    val questionId: String,
    val isBookmarked: Boolean = false,
    val attemptsCount: Int = 0,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    val lastOutcome: String? = null, // "right" | "wrong" | null
    val updatedAt: Long = 0L
) {
    val accuracyPercent: Int
        get() = if (attemptsCount > 0) ((correctCount.toFloat() / attemptsCount) * 100f).toInt() else 0
}

data class QuestionItem(
    val question: Question,
    val progress: QuestionProgress
) {
    val id: String get() = question.id
    val isBookmarked: Boolean get() = progress.isBookmarked
    val lastOutcome: String? get() = progress.lastOutcome
    val attemptsCount: Int get() = progress.attemptsCount
    val correctCount: Int get() = progress.correctCount
    val wrongCount: Int get() = progress.wrongCount
    val accuracyPercent: Int get() = progress.accuracyPercent
}

data class Flashcard(
    val id: Int,
    val title: String,
    val tag: String,
    val body: String
)

data class MockHistory(
    val id: Long = 0,
    val title: String,
    val totalQuestions: Int,
    val correctCount: Int,
    val wrongCount: Int,
    val skippedCount: Int,
    val netScore: Double,
    val accuracyPercent: Int,
    val timestamp: Long,
    val examId: String = PlatformCatalogDefaults.EXAM_RRB_NURSING
)

data class SubjectMeta(
    val code: String,
    val name: String,
    val accentColor: Color,
    val group: SubjectGroup,
    val accentHex: String = "#726D70"
)

enum class SubjectGroup(val label: String) {
    NURSING("Nursing (70%)"),
    CA_GK("Current Affairs & GK"),
    NON_TECH("Sci / Math / Reasoning (30%)")
}

data class SubjectStat(
    val code: String,
    val name: String,
    val group: SubjectGroup,
    val totalQuestions: Int,
    val attemptedQuestions: Int,
    val totalAttempts: Int,
    val correctAttempts: Int,
    val wrongAttempts: Int,
    val masteredQuestions: Int,
    val accentColor: Color
) {
    val accuracyPercent: Int
        get() = if (totalAttempts > 0) ((correctAttempts.toFloat() / totalAttempts) * 100f).toInt() else 0
}

data class CategoryStat(
    val key: String,
    val label: String,
    val description: String,
    val totalQuestions: Int,
    val attemptedQuestions: Int,
    val totalAttempts: Int,
    val correctAttempts: Int,
    val wrongAttempts: Int,
    val accentColor: Color
) {
    val accuracyPercent: Int
        get() = if (totalAttempts > 0) ((correctAttempts.toFloat() / totalAttempts) * 100f).toInt() else 0
}

object SubjectCatalog {
    val NURSING_SUBJS = listOf(
        SubjectMeta("MSN", "Medical-Surgical", Color(0xFF262626), SubjectGroup.NURSING, "#262626"),
        SubjectMeta("Pharm", "Pharmacology", Color(0xFF404040), SubjectGroup.NURSING, "#404040"),
        SubjectMeta("OBG", "OBG & Midwifery", Color(0xFF525252), SubjectGroup.NURSING, "#525252"),
        SubjectMeta("Peds", "Child Health (Peds)", Color(0xFF3F3F46), SubjectGroup.NURSING, "#3F3F46"),
        SubjectMeta("CHN", "Community Health", Color(0xFF27272A), SubjectGroup.NURSING, "#27272A"),
        SubjectMeta("Psych", "Mental Health", Color(0xFF52525B), SubjectGroup.NURSING, "#52525B"),
        SubjectMeta("FON", "Fundamentals (FON)", Color(0xFF333333), SubjectGroup.NURSING, "#333333"),
        SubjectMeta("Anat", "Anatomy & Physio", Color(0xFF474747), SubjectGroup.NURSING, "#474747"),
        SubjectMeta("Admin", "Admin & Research", Color(0xFF5C5C5C), SubjectGroup.NURSING, "#5C5C5C")
    )

    val CA_SUBJS = listOf(
        SubjectMeta("CA-2026", "2025–26 National & Global", Color(0xFF333333), SubjectGroup.CA_GK, "#333333"),
        SubjectMeta("Railways", "Indian Railways & Budget", Color(0xFF4A4A4A), SubjectGroup.CA_GK, "#4A4A4A"),
        SubjectMeta("Health-Prog", "Health Drives & UIP", Color(0xFF3F3F46), SubjectGroup.CA_GK, "#3F3F46"),
        SubjectMeta("Static-GK", "Static GK & Polity", Color(0xFF525252), SubjectGroup.CA_GK, "#525252")
    )

    val GEN_SUBJS = listOf(
        SubjectMeta("Science", "General Science (NCERT)", Color(0xFF2E2E2E), SubjectGroup.NON_TECH, "#2E2E2E"),
        SubjectMeta("Math", "Arithmetic & Dosage", Color(0xFF444444), SubjectGroup.NON_TECH, "#444444"),
        SubjectMeta("Reasoning", "Logical Reasoning", Color(0xFF555555), SubjectGroup.NON_TECH, "#555555")
    )

    val ALL_SUBJS = NURSING_SUBJS + CA_SUBJS + GEN_SUBJS

    private val nursingCodes = NURSING_SUBJS.map { it.code }.toSet()
    private val caCodes = CA_SUBJS.map { it.code }.toSet()
    private val genCodes = GEN_SUBJS.map { it.code }.toSet()

    fun isNursingCode(code: String): Boolean = code in nursingCodes
    fun isCaCode(code: String): Boolean = code in caCodes
    fun isGenCode(code: String): Boolean = code in genCodes

    fun isNursing(q: Question): Boolean = q.subject in nursingCodes
    fun isCa(q: Question): Boolean = q.subject in caCodes
    fun isGen(q: Question): Boolean = q.subject in genCodes

    fun displayNameFor(code: String): String =
        ALL_SUBJS.firstOrNull { it.code == code }?.name ?: code

    val SHIFT_NAMES = mapOf(
        // RRB Nursing
        "2026-S1" to "RRB Nursing 2026 — Shift 1",
        "2026-S2" to "RRB Nursing 2026 — Shift 2",
        "2026-S3" to "RRB Nursing 2026 — Shift 3",
        "2025-S1" to "RRB Nursing Supt. 2025 — Shift 1",
        "2025-S2" to "RRB Nursing Supt. 2025 — Shift 2",
        "2025-S3" to "RRB Nursing Supt. 2025 — Shift 3",
        "2019-S1" to "RRB Staff Nurse 2019 — Shift 1",
        "2019-S2" to "RRB Staff Nurse 2019 — Shift 2",
        "2019-S3" to "RRB Staff Nurse 2019 — Shift 3",
        "2015" to "RRB Staff Nurse 2015 — Paper 1",
        // AIIMS NORCET
        "NORCET-10" to "AIIMS NORCET-10 — Stage I Prelims",
        "NORCET-9" to "AIIMS NORCET-9 (2025) — Stage I Prelims",
        "NORCET-8" to "AIIMS NORCET-8 (2025) — Stage I Prelims",
        "NORCET-7" to "AIIMS NORCET-7 (2024) — Stage I Prelims",
        "NORCET-6" to "AIIMS NORCET-6 (2024) — Stage I Prelims",
        "NORCET-5" to "AIIMS NORCET-5 (2023) — Stage I Prelims",
        // PGIMER Chandigarh
        "PGIMER-2025" to "PGIMER Chandigarh 2025 — Paper 1",
        "PGIMER-2024" to "PGIMER Chandigarh 2024 — Paper 1",
        "PGIMER-2023" to "PGIMER Chandigarh 2023 — Paper 1",
        "PGIMER-2022" to "PGIMER Chandigarh 2022 — Paper 1",
        "PGIMER-2020" to "PGIMER Chandigarh 2020 — Paper 1",
        // Central & Railways (ESIC, DSSSB, RML)
        "ESIC-2024-P1" to "ESIC Nursing Officer 2024 (UPSC) — Paper 1",
        "ESIC-2019-S1" to "ESIC Staff Nurse 2019 — Shift 1",
        "DSSSB-2024-S1" to "DSSSB Nursing Officer 2024 — Shift 1",
        "DSSSB-2019-S1" to "DSSSB Nursing Officer 2019 — Shift 1",
        "RML-2024-S1" to "RML & Central Hospitals 2024 — Paper 1",
        "RML-2019-S1" to "RML & Safdarjung 2019 — Paper 1",
        // Autonomous Institutes
        "JIPMER-2024-S1" to "JIPMER Puducherry 2024 — Shift 1",
        "JIPMER-2022-S1" to "JIPMER Puducherry 2022 — Shift 1",
        "NIMHANS-2024-S1" to "NIMHANS Bengaluru 2024 — Paper 1",
        "NIMHANS-2021-S1" to "NIMHANS Bengaluru 2021 — Paper 1",
        "NEIGRIHMS-2024-S1" to "NEIGRIHMS Shillong 2024 — Paper 1",
        "NEIGRIHMS-2022-S1" to "NEIGRIHMS Shillong 2022 — Paper 1",
        "GIMS-2024-S1" to "GIMS Greater Noida 2024 — Paper 1",
        "GIMS-2023-S1" to "GIMS Greater Noida 2023 — Paper 1",
        "SGPGI-2024-S1" to "SGPGIMS Lucknow 2024 — Paper 1",
        "SGPGI-2023-S1" to "SGPGIMS Lucknow 2023 — Paper 1",
        "KGMU-2023-S1" to "KGMU Lucknow 2023 — Paper 1",
        "KGMU-2021-S1" to "KGMU Lucknow 2021 — Paper 1",
        "BHU-2024-S1" to "BHU IMS Varanasi 2024 — Paper 1",
        "BHU-2019-S1" to "BHU IMS Varanasi 2019 — Paper 1",
        "GMCH-2024-S1" to "GMCH Chandigarh 2024 — Paper 1",
        "GMCH-2022-S1" to "GMCH Chandigarh 2022 — Paper 1",
        "SCTIMST-2024-S1" to "SCTIMST Trivandrum 2024 — Paper 1",
        "SCTIMST-2021-S1" to "SCTIMST Trivandrum 2021 — Paper 1",
        "UPUMS-2024-S1" to "UPUMS Saifai 2024 — Paper 1",
        "UPUMS-2023-S1" to "UPUMS Saifai 2023 — Paper 1",
        "TMC-2024-S1" to "TMC / ACTREC / ICMR 2024 — Paper 1",
        "TMC-2022-S1" to "TMC / ACTREC / ICMR 2022 — Paper 1",
        // State PSC, DHS & NHM/CHO
        "CHO-2024-S1" to "NHM / CHO All-India 2024 — Paper 1",
        "CHO-2022-S1" to "NHM / CHO All-India 2022 — Paper 1",
        "KPSC-2024-S1" to "Kerala PSC Staff Nurse 2024 — Paper 1",
        "KPSC-2020-S1" to "Kerala PSC Staff Nurse 2020 — Paper 1",
        "RUHS-2024-S1" to "RUHS / RSSB Rajasthan 2024 — Paper 1",
        "RUHS-2020-S1" to "RUHS / RSSB Rajasthan 2020 — Paper 1",
        "OSSSC-2024-S1" to "OSSSC Odisha Nursing Officer 2024 — Paper 1",
        "OSSSC-2021-S1" to "OSSSC Odisha Nursing Officer 2021 — Paper 1",
        "BTSC-2024-S1" to "BTSC / SHSB Bihar 2024 — Paper 1",
        "BTSC-2020-S1" to "BTSC / SHSB Bihar 2020 — Paper 1",
        "TNMRB-2024-S1" to "TN MRB Staff Nurse 2024 — Paper 1",
        "TNMRB-2019-S1" to "TN MRB Staff Nurse 2019 — Paper 1",
        "MHDMER-2024-S1" to "Maharashtra DMER 2024 — Paper 1",
        "MHDMER-2023-S1" to "Maharashtra DMER 2023 — Paper 1",
        "WBHRB-2024-S1" to "WBHRB West Bengal 2024 — Paper 1",
        "WBHRB-2021-S1" to "WBHRB West Bengal 2021 — Paper 1",
        "HSSC-2024-S1" to "HSSC Haryana Staff Nurse 2024 — Paper 1",
        "MPNHM-2022-S1" to "MPESB / MP NHM Staff Nurse 2022 — Paper 1",
        // Defence & PSU
        "MNS-2024-S1" to "MNS Army SSC 2024 — Paper 1",
        "MNS-2022-S1" to "MNS Army SSC 2022 — Paper 1",
        "ISRO-2024-S1" to "ISRO Nurse-B 2024 — Paper 1",
        "ISRO-2021-S1" to "ISRO Nurse-B 2021 — Paper 1",
        "PSU-2024-S1" to "PSU Staff Nurse (SAIL/ONGC) 2024 — Paper 1",
        "PSU-2021-S1" to "PSU Staff Nurse (CIL/BHEL) 2021 — Paper 1"
    )

    fun formatPaperTitle(shiftCode: String): String {
        val clean = shiftCode.trim()
        if (clean.isEmpty()) return "Previous Year Paper"
        SHIFT_NAMES[clean]?.let { return it }
        val parts = clean.split('-').filter { it.isNotBlank() }
        return when {
            parts.size >= 3 -> {
                val shiftLabel = when {
                    parts[2].startsWith("S", ignoreCase = true) -> "Shift ${parts[2].drop(1)}"
                    parts[2].startsWith("P", ignoreCase = true) -> "Paper ${parts[2].drop(1)}"
                    else -> parts[2]
                }
                "${parts[0]} ${parts[1]} — $shiftLabel"
            }
            parts.size == 2 -> "${parts[0]} — ${parts[1]}"
            else -> "$clean — Paper"
        }
    }
}
