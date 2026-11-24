package com.example.data.model

import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.ceil
import kotlin.math.roundToInt

// ============================================================================
// 1. SPACED REPETITION (SRS) MISTAKE NOTEBOOK (1d / 3d / 7d / 14d / Mastered)
// ============================================================================

enum class SrsStage(val boxIndex: Int, val intervalDays: Int, val badgeLabel: String) {
    BOX_1D(0, 1, "1d"),
    BOX_3D(1, 3, "3d"),
    BOX_7D(2, 7, "7d"),
    BOX_14D(3, 14, "14d"),
    MASTERED(4, 30, "Mastered");

    fun nextStage(): SrsStage = when (this) {
        BOX_1D -> BOX_3D
        BOX_3D -> BOX_7D
        BOX_7D -> BOX_14D
        BOX_14D -> MASTERED
        MASTERED -> MASTERED
    }

    companion object {
        fun fromBoxIndex(index: Int): SrsStage =
            entries.firstOrNull { it.boxIndex == index } ?: BOX_1D
    }
}

data class SrsCardState(
    val questionId: String,
    val stage: SrsStage = SrsStage.BOX_1D,
    val nextReviewAtMs: Long = 0L,
    val lastReviewedAtMs: Long = 0L,
    val consecutiveCorrect: Int = 0
)

data class SrsNotebookEntry(
    val item: QuestionItem,
    val card: SrsCardState,
    val isDueNow: Boolean
)

data class SrsNotebookSummary(
    val allEntries: List<SrsNotebookEntry> = emptyList(),
    val dueEntries: List<SrsNotebookEntry> = emptyList(),
    val upcomingEntries: List<SrsNotebookEntry> = emptyList(),
    val masteredEntries: List<SrsNotebookEntry> = emptyList()
)

object SpacedRepetitionLogic {
    private const val ONE_DAY_MS = 24L * 60L * 60L * 1000L

    fun recordWrong(
        existing: Map<String, SrsCardState>,
        questionId: String,
        nowMs: Long = System.currentTimeMillis()
    ): Map<String, SrsCardState> {
        if (questionId.isBlank()) return existing
        val updated = existing.toMutableMap()
        // Wrong answers immediately enter Box 0 (1d) and are marked due right away so student can drill them today.
        updated[questionId] = SrsCardState(
            questionId = questionId,
            stage = SrsStage.BOX_1D,
            nextReviewAtMs = nowMs,
            lastReviewedAtMs = nowMs,
            consecutiveCorrect = 0
        )
        return updated
    }

    fun recordRight(
        existing: Map<String, SrsCardState>,
        questionId: String,
        nowMs: Long = System.currentTimeMillis()
    ): Map<String, SrsCardState> {
        val current = existing[questionId] ?: return existing
        val next = current.stage.nextStage()
        val updated = existing.toMutableMap()
        updated[questionId] = current.copy(
            stage = next,
            nextReviewAtMs = nowMs + (next.intervalDays.toLong() * ONE_DAY_MS),
            lastReviewedAtMs = nowMs,
            consecutiveCorrect = current.consecutiveCorrect + 1
        )
        return updated
    }

    fun resetToBox1(
        existing: Map<String, SrsCardState>,
        questionId: String,
        nowMs: Long = System.currentTimeMillis()
    ): Map<String, SrsCardState> = recordWrong(existing, questionId, nowMs)

    fun removeCard(
        existing: Map<String, SrsCardState>,
        questionId: String
    ): Map<String, SrsCardState> {
        if (questionId !in existing) return existing
        val updated = existing.toMutableMap()
        updated.remove(questionId)
        return updated
    }

    fun isDue(card: SrsCardState, nowMs: Long = System.currentTimeMillis()): Boolean {
        return card.stage != SrsStage.MASTERED && card.nextReviewAtMs <= nowMs
    }

    fun buildSummary(
        questions: List<QuestionItem>,
        storedCards: Map<String, SrsCardState>,
        nowMs: Long = System.currentTimeMillis()
    ): SrsNotebookSummary {
        val byId = questions.associateBy { it.id }
        val effectiveCards = LinkedHashMap<String, SrsCardState>()

        // 1. Include all persisted SRS cards that belong to the current pool
        storedCards.forEach { (qId, card) ->
            if (qId in byId) {
                effectiveCards[qId] = card
            }
        }

        // 2. Automatically seed any existing wrong-answered questions not yet tracked in SRS
        questions.forEach { item ->
            if (item.lastOutcome == "wrong" && item.id !in effectiveCards) {
                effectiveCards[item.id] = SrsCardState(
                    questionId = item.id,
                    stage = SrsStage.BOX_1D,
                    nextReviewAtMs = item.progress.updatedAt.takeIf { it > 0L } ?: nowMs,
                    lastReviewedAtMs = item.progress.updatedAt,
                    consecutiveCorrect = 0
                )
            }
        }

        val entries = effectiveCards.values.mapNotNull { card ->
            val item = byId[card.questionId] ?: return@mapNotNull null
            SrsNotebookEntry(
                item = item,
                card = card,
                isDueNow = isDue(card, nowMs)
            )
        }.sortedWith(
            compareByDescending<SrsNotebookEntry> { it.isDueNow }
                .thenBy { it.card.stage.boxIndex }
                .thenBy { it.card.nextReviewAtMs }
        )

        val due = entries.filter { it.isDueNow }
        val mastered = entries.filter { it.card.stage == SrsStage.MASTERED }
        val upcoming = entries.filter { !it.isDueNow && it.card.stage != SrsStage.MASTERED }

        return SrsNotebookSummary(
            allEntries = entries,
            dueEntries = due,
            upcomingEntries = upcoming,
            masteredEntries = mastered
        )
    }

    fun formatNextReviewLabel(card: SrsCardState, nowMs: Long = System.currentTimeMillis()): String {
        if (card.stage == SrsStage.MASTERED) return "Mastered (30d)"
        val diffMs = card.nextReviewAtMs - nowMs
        if (diffMs <= 0L) return "Due Now"
        val days = ceil(diffMs.toDouble() / ONE_DAY_MS.toDouble()).toInt().coerceAtLeast(1)
        return if (days == 1) "Due tomorrow" else "Due in ${days}d"
    }

    fun serializeMap(cards: Map<String, SrsCardState>): String {
        val root = JSONObject()
        cards.values.take(600).forEach { card ->
            root.put(
                card.questionId,
                JSONObject().apply {
                    put("s", card.stage.boxIndex)
                    put("n", card.nextReviewAtMs)
                    put("l", card.lastReviewedAtMs)
                    put("c", card.consecutiveCorrect)
                }
            )
        }
        return root.toString()
    }

    fun deserializeMap(rawJson: String?): Map<String, SrsCardState> {
        if (rawJson.isNullOrBlank()) return emptyMap()
        return runCatching {
            val root = JSONObject(rawJson)
            val result = LinkedHashMap<String, SrsCardState>()
            val keys = root.keys()
            while (keys.hasNext()) {
                val qId = keys.next()
                val obj = root.optJSONObject(qId) ?: continue
                result[qId] = SrsCardState(
                    questionId = qId,
                    stage = SrsStage.fromBoxIndex(obj.optInt("s", 0)),
                    nextReviewAtMs = obj.optLong("n", 0L),
                    lastReviewedAtMs = obj.optLong("l", 0L),
                    consecutiveCorrect = obj.optInt("c", 0).coerceAtLeast(0)
                )
            }
            result
        }.getOrDefault(emptyMap())
    }
}

// ============================================================================
// 2. TARGET EXAM COUNTDOWN & DAILY STUDY ROADMAP
// ============================================================================

data class ExamCountdownConfig(
    val targetDateIso: String = "",
    val customExamLabel: String = ""
)

data class ExamRoadmapSummary(
    val isConfigured: Boolean = false,
    val targetDateIso: String = "",
    val formattedDate: String = "",
    val daysRemaining: Int = 0,
    val isExamDayOrPassed: Boolean = false,
    val phaseTitle: String = "Set Exam Date",
    val phaseGuidance: String = "Pick your target exam date to unlock your daily MCQ roadmap.",
    val recommendedDailyQs: Int = 30,
    val unattemptedCount: Int = 0,
    val totalBankCount: Int = 0
)

object ExamCountdownLogic {
    private const val ISO_PATTERN = "yyyy-MM-dd"

    fun todayIso(nowMs: Long = System.currentTimeMillis(), tz: TimeZone = TimeZone.getDefault()): String {
        val sdf = SimpleDateFormat(ISO_PATTERN, Locale.US).apply { timeZone = tz }
        return sdf.format(Date(nowMs))
    }

    fun isoDateAfterDays(
        daysFromToday: Int,
        nowMs: Long = System.currentTimeMillis(),
        tz: TimeZone = TimeZone.getDefault()
    ): String {
        val cal = Calendar.getInstance(tz).apply {
            timeInMillis = nowMs
            add(Calendar.DAY_OF_YEAR, daysFromToday)
        }
        val sdf = SimpleDateFormat(ISO_PATTERN, Locale.US).apply { timeZone = tz }
        return sdf.format(cal.time)
    }

    fun daysUntil(
        targetDateIso: String,
        nowMs: Long = System.currentTimeMillis(),
        tz: TimeZone = TimeZone.getDefault()
    ): Int? {
        val clean = targetDateIso.trim()
        if (!Regex("""^\d{4}-\d{2}-\d{2}$""").matches(clean)) return null
        val sdf = SimpleDateFormat(ISO_PATTERN, Locale.US).apply {
            timeZone = tz
            isLenient = false
        }
        val targetDate = runCatching { sdf.parse(clean) }.getOrNull() ?: return null
        val startCal = Calendar.getInstance(tz).apply {
            timeInMillis = nowMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val targetCal = Calendar.getInstance(tz).apply {
            time = targetDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val diffMs = targetCal.timeInMillis - startCal.timeInMillis
        return (diffMs / (24L * 60L * 60L * 1000L)).toInt()
    }

    fun formatFriendlyDate(isoDate: String, tz: TimeZone = TimeZone.getDefault()): String {
        val clean = isoDate.trim()
        val parser = SimpleDateFormat(ISO_PATTERN, Locale.US).apply {
            timeZone = tz
            isLenient = false
        }
        val parsed = runCatching { parser.parse(clean) }.getOrNull() ?: return clean
        val out = SimpleDateFormat("dd MMM yyyy", Locale.US).apply { timeZone = tz }
        return out.format(parsed)
    }

    fun computeRoadmap(
        targetDateIso: String,
        totalBankCount: Int,
        attemptedCount: Int,
        currentDailyGoal: Int,
        nowMs: Long = System.currentTimeMillis(),
        tz: TimeZone = TimeZone.getDefault()
    ): ExamRoadmapSummary {
        val days = daysUntil(targetDateIso, nowMs, tz)
            ?: return ExamRoadmapSummary(
                isConfigured = false,
                unattemptedCount = (totalBankCount - attemptedCount).coerceAtLeast(0),
                totalBankCount = totalBankCount,
                recommendedDailyQs = currentDailyGoal.coerceIn(20, 150)
            )
        val unattempted = (totalBankCount - attemptedCount).coerceAtLeast(0)
        val effectiveDays = days.coerceAtLeast(1)
        val idealPace = if (unattempted > 0) {
            ceil(unattempted.toDouble() / effectiveDays.toDouble()).toInt().coerceIn(25, 150)
        } else {
            currentDailyGoal.coerceIn(25, 100)
        }

        val (phaseTitle, guidance) = when {
            days <= 0 -> "Exam Day Ready" to "Stay calm, trust your clinical instincts, and avoid negative marking guesses."
            days <= 14 -> "Final Sprint (Last 2 Weeks)" to "Prioritize SRS Mistake Notebook, PYQ shift papers, and 1 full timed CBT mock every 2 days."
            days <= 45 -> "High-Yield Consolidation" to "Focus on weak clinical subjects, daily $idealPace MCQs, and 2 full CBT mocks per week."
            else -> "Foundation & Coverage" to "Build rock-solid concepts across MSN, OBG, Pharm & CHN at $idealPace MCQs/day."
        }

        return ExamRoadmapSummary(
            isConfigured = true,
            targetDateIso = targetDateIso.trim(),
            formattedDate = formatFriendlyDate(targetDateIso, tz),
            daysRemaining = days.coerceAtLeast(0),
            isExamDayOrPassed = days <= 0,
            phaseTitle = phaseTitle,
            phaseGuidance = guidance,
            recommendedDailyQs = idealPace,
            unattemptedCount = unattempted,
            totalBankCount = totalBankCount
        )
    }
}

// ============================================================================
// 3. NURSING CLINICAL CALCULATORS & NORMAL LAB VALUES GUIDE (OFFLINE)
// ============================================================================

data class IvDripResult(
    val mlPerHour: Double,
    val dropsPerMinute: Int
)

data class PediatricDoseResult(
    val totalDailyMg: Double,
    val singleDoseMg: Double,
    val clarksRuleMg: Double?,
    val youngsRuleMg: Double?
)

data class ParklandBurnResult(
    val total24hMl: Double,
    val first8hMl: Double,
    val next16hMl: Double,
    val first8hMlPerHr: Double,
    val next16hMlPerHr: Double
)

data class GcsEvaluation(
    val total: Int,
    val severity: String,
    val airwayAlert: Boolean
)

data class ApgarEvaluation(
    val total: Int,
    val interpretation: String
)

data class BishopEvaluation(
    val total: Int,
    val readiness: String
)

object ClinicalCalculatorLogic {

    fun calculateIvDrip(
        volumeMl: Double,
        durationHours: Double,
        dropFactorGttPerMl: Int
    ): IvDripResult? {
        if (volumeMl <= 0.0 || durationHours <= 0.0 || dropFactorGttPerMl <= 0) return null
        val mlPerHr = volumeMl / durationHours
        val durationMins = durationHours * 60.0
        val gttPerMin = ((volumeMl * dropFactorGttPerMl) / durationMins).roundToInt()
        return IvDripResult(mlPerHour = mlPerHr, dropsPerMinute = gttPerMin)
    }

    fun calculatePediatricDose(
        weightKg: Double,
        mgPerKgPerDay: Double,
        dosesPerDay: Int,
        adultDoseMg: Double? = null,
        childAgeYears: Double? = null
    ): PediatricDoseResult? {
        if (weightKg <= 0.0 || mgPerKgPerDay <= 0.0 || dosesPerDay <= 0) return null
        val daily = weightKg * mgPerKgPerDay
        val single = daily / dosesPerDay.toDouble()
        val weightLbs = weightKg * 2.20462
        val clarks = adultDoseMg?.takeIf { it > 0.0 }?.let { (weightLbs / 150.0) * it }
        val youngs = if (adultDoseMg != null && adultDoseMg > 0.0 && childAgeYears != null && childAgeYears > 0.0) {
            (childAgeYears / (childAgeYears + 12.0)) * adultDoseMg
        } else {
            null
        }
        return PediatricDoseResult(
            totalDailyMg = daily,
            singleDoseMg = single,
            clarksRuleMg = clarks,
            youngsRuleMg = youngs
        )
    }

    fun calculateParklandBurns(
        weightKg: Double,
        tbsaPercent: Double
    ): ParklandBurnResult? {
        if (weightKg <= 0.0 || tbsaPercent <= 0.0 || tbsaPercent > 100.0) return null
        val total = 4.0 * weightKg * tbsaPercent
        val half = total / 2.0
        return ParklandBurnResult(
            total24hMl = total,
            first8hMl = half,
            next16hMl = half,
            first8hMlPerHr = half / 8.0,
            next16hMlPerHr = half / 16.0
        )
    }

    fun evaluateGcs(eye: Int, verbal: Int, motor: Int): GcsEvaluation {
        val e = eye.coerceIn(1, 4)
        val v = verbal.coerceIn(1, 5)
        val m = motor.coerceIn(1, 6)
        val total = e + v + m
        val severity = when {
            total >= 13 -> "Mild Brain Injury (13–15)"
            total >= 9 -> "Moderate Brain Injury (9–12)"
            else -> "Severe Coma (3–8) — Intubate ('GCS 8, Intubate')"
        }
        return GcsEvaluation(total = total, severity = severity, airwayAlert = total <= 8)
    }

    fun evaluateApgar(
        appearance: Int,
        pulse: Int,
        grimace: Int,
        activity: Int,
        respiration: Int
    ): ApgarEvaluation {
        val total = appearance.coerceIn(0, 2) +
            pulse.coerceIn(0, 2) +
            grimace.coerceIn(0, 2) +
            activity.coerceIn(0, 2) +
            respiration.coerceIn(0, 2)
        val interp = when {
            total >= 7 -> "Normal Newborn — Routine care & warm skin-to-skin"
            total >= 4 -> "Moderately Depressed — Suction, tactile stimulation, O₂ as needed"
            else -> "Severely Depressed — Immediate neonatal resuscitation (PPV / CPR)"
        }
        return ApgarEvaluation(total = total, interpretation = interp)
    }

    fun evaluateBishop(
        dilation: Int,
        effacement: Int,
        station: Int,
        consistency: Int,
        position: Int
    ): BishopEvaluation {
        val total = dilation.coerceIn(0, 3) +
            effacement.coerceIn(0, 3) +
            station.coerceIn(0, 3) +
            consistency.coerceIn(0, 2) +
            position.coerceIn(0, 2)
        val readiness = when {
            total >= 8 -> "Favorable Cervix (≥ 8) — High likelihood of successful vaginal induction"
            total >= 6 -> "Moderately Favorable (6–7) — Consider oxytocin / amniotomy per protocol"
            else -> "Unfavorable Cervix (≤ 5) — Cervical ripening (PGE₂ / Foley) needed first"
        }
        return BishopEvaluation(total = total, readiness = readiness)
    }
}

enum class LabValueCategory(val label: String) {
    ABG_ELECTROLYTES("ABG & Electrolytes"),
    HEMATOLOGY("Hematology & Coag"),
    RENAL_HEPATIC("Renal & Hepatic"),
    CARDIAC_ENDOCRINE("Cardiac & Endocrine"),
    MATERNAL_PEDS("Maternal & Peds")
}

data class NormalLabValue(
    val name: String,
    val normalRange: String,
    val criticalPanic: String,
    val examPearl: String,
    val category: LabValueCategory
)

object NormalLabValuesCatalog {
    val ALL_VALUES: List<NormalLabValue> = listOf(
        // ABG & Electrolytes
        NormalLabValue("Arterial Blood pH", "7.35 – 7.45", "< 7.20 or > 7.60", "ROME: Respiratory Opposite, Metabolic Equal", LabValueCategory.ABG_ELECTROLYTES),
        NormalLabValue("PaCO₂ (Arterial)", "35 – 45 mmHg", "< 20 or > 60 mmHg", "Primary respiratory acid-base indicator", LabValueCategory.ABG_ELECTROLYTES),
        NormalLabValue("HCO₃⁻ (Bicarbonate)", "22 – 26 mEq/L", "< 10 or > 40 mEq/L", "Primary metabolic buffer regulated by kidneys", LabValueCategory.ABG_ELECTROLYTES),
        NormalLabValue("PaO₂ / SpO₂", "80 – 100 mmHg / ≥ 95%", "< 60 mmHg / < 90%", "COPD target SpO₂ is 88–92% to protect hypoxic drive", LabValueCategory.ABG_ELECTROLYTES),
        NormalLabValue("Serum Sodium (Na⁺)", "135 – 145 mEq/L", "< 120 or > 160 mEq/L", "Rapid correction risks osmotic demyelination", LabValueCategory.ABG_ELECTROLYTES),
        NormalLabValue("Serum Potassium (K⁺)", "3.5 – 5.0 mEq/L", "< 2.5 or > 6.5 mEq/L", "Tall peaked T-waves in hyperkalemia; give Ca gluconate 1st", LabValueCategory.ABG_ELECTROLYTES),
        NormalLabValue("Serum Calcium (Total)", "8.5 – 10.5 mg/dL", "< 6.0 or > 13.0 mg/dL", "Hypocalcemia: positive Chvostek & Trousseau signs", LabValueCategory.ABG_ELECTROLYTES),
        NormalLabValue("Serum Magnesium (Mg²⁺)", "1.5 – 2.5 mEq/L", "< 1.0 or > 4.9 mEq/L", "Eclampsia therapeutic MgSO₄: 4–7 mEq/L; antidote Ca gluconate", LabValueCategory.ABG_ELECTROLYTES),
        NormalLabValue("Serum Chloride (Cl⁻)", "96 – 106 mEq/L", "< 80 or > 115 mEq/L", "Inverse relationship with bicarbonate", LabValueCategory.ABG_ELECTROLYTES),

        // Hematology & Coagulation
        NormalLabValue("Hemoglobin (Hb)", "M: 13.5–17.5 | F: 12–15.5 g/dL", "< 7.0 g/dL", "Pregnancy anemia cutoff (WHO/INC): < 11.0 g/dL", LabValueCategory.HEMATOLOGY),
        NormalLabValue("WBC (Total Leukocyte)", "4,000 – 11,000 /µL", "< 2,000 or > 30,000 /µL", "ANC < 500 requires neutropenic reverse isolation", LabValueCategory.HEMATOLOGY),
        NormalLabValue("Platelet Count", "1.5 – 4.5 lakh /µL", "< 50,000 /µL (< 20k bleeding)", "Monitor closely in Dengue, HELLP, and HIT", LabValueCategory.HEMATOLOGY),
        NormalLabValue("PT / INR", "PT 11–13.5s | INR 0.8–1.2", "INR > 5.0", "Warfarin target INR 2–3 (mechanical valve 2.5–3.5); antidote Vit K", LabValueCategory.HEMATOLOGY),
        NormalLabValue("aPTT", "25 – 35 seconds", "> 90 seconds", "Monitors unfractionated Heparin (1.5–2.5× control); antidote Protamine", LabValueCategory.HEMATOLOGY),
        NormalLabValue("ESR", "M: 0–15 | F: 0–20 mm/hr", "> 100 mm/hr", "Non-specific inflammation marker (TB, RA, temporal arteritis)", LabValueCategory.HEMATOLOGY),

        // Renal & Hepatic
        NormalLabValue("Serum Creatinine", "0.6 – 1.2 mg/dL", "> 4.0 mg/dL", "Most reliable clinical indicator of GFR / renal function", LabValueCategory.RENAL_HEPATIC),
        NormalLabValue("Blood Urea Nitrogen (BUN)", "7 – 20 mg/dL", "> 80 mg/dL", "BUN:Cr > 20:1 points to pre-renal dehydration or GI bleed", LabValueCategory.RENAL_HEPATIC),
        NormalLabValue("Urine Specific Gravity", "1.005 – 1.030", "< 1.003 (DI) or > 1.035", "Low in Diabetes Insipidus; high in SIADH / dehydration", LabValueCategory.RENAL_HEPATIC),
        NormalLabValue("Total Bilirubin (Adult)", "0.2 – 1.2 mg/dL", "> 12.0 mg/dL", "Clinical jaundice visible when bilirubin > 2.5–3.0 mg/dL", LabValueCategory.RENAL_HEPATIC),
        NormalLabValue("Serum Albumin", "3.5 – 5.0 g/dL", "< 2.0 g/dL", "Maintains oncotic pressure; low in nephrotic syndrome & cirrhosis", LabValueCategory.RENAL_HEPATIC),
        NormalLabValue("AST / ALT (SGOT / SGPT)", "10 – 40 U/L", "> 1,000 U/L", "ALT is more liver-specific; AST:ALT > 2 suggests alcoholic liver disease", LabValueCategory.RENAL_HEPATIC),
        NormalLabValue("Serum Ammonia", "15 – 45 µg/dL", "> 100 µg/dL", "Elevated in hepatic encephalopathy; treat with Lactulose", LabValueCategory.RENAL_HEPATIC),

        // Cardiac & Endocrine
        NormalLabValue("Cardiac Troponin I", "< 0.04 ng/mL", "> 0.40 ng/mL", "Gold-standard MI marker; rises in 3–4h, stays elevated 7–10 days", LabValueCategory.CARDIAC_ENDOCRINE),
        NormalLabValue("Fasting Blood Glucose", "70 – 100 mg/dL", "< 50 or > 400 mg/dL", "Diabetes diagnostic threshold: FBS ≥ 126 mg/dL or HbA1c ≥ 6.5%", LabValueCategory.CARDIAC_ENDOCRINE),
        NormalLabValue("HbA1c", "< 5.7% (DM target < 7%)", "> 10.0%", "Reflects average blood glucose over past 2–3 months", LabValueCategory.CARDIAC_ENDOCRINE),
        NormalLabValue("Serum TSH", "0.4 – 4.0 mIU/L", "< 0.1 or > 20 mIU/L", "High TSH + low T3/T4 = primary hypothyroidism", LabValueCategory.CARDIAC_ENDOCRINE),
        NormalLabValue("Digoxin Therapeutic Level", "0.5 – 2.0 ng/mL", "> 2.0 ng/mL", "Hold if apical pulse < 60 bpm; hypokalemia precipitates toxicity", LabValueCategory.CARDIAC_ENDOCRINE),
        NormalLabValue("Serum Lithium Level", "0.6 – 1.2 mEq/L", "> 1.5 mEq/L (> 2.0 severe)", "Low sodium increases lithium reabsorption & toxicity", LabValueCategory.CARDIAC_ENDOCRINE),
        NormalLabValue("Central Venous Pressure", "2 – 8 mmHg (5–10 cmH₂O)", "> 15 mmHg (fluid overload)", "Measured at phlebostatic axis (4th ICS, mid-axillary line)", LabValueCategory.CARDIAC_ENDOCRINE),
        NormalLabValue("Intracranial Pressure (ICP)", "5 – 15 mmHg", "> 20 mmHg sustained", "Cushing triad: HTN (wide pulse pressure), bradycardia, irregular resp", LabValueCategory.CARDIAC_ENDOCRINE),

        // Maternal & Pediatric
        NormalLabValue("Fetal Heart Rate (FHR)", "110 – 160 bpm", "< 110 or > 160 bpm (>10m)", "Late decelerations = uteroplacental insufficiency (turn left lateral!)", LabValueCategory.MATERNAL_PEDS),
        NormalLabValue("Newborn Heart Rate", "120 – 160 bpm", "< 100 bpm (start PPV)", "Apical pulse counted for 1 full minute in infants", LabValueCategory.MATERNAL_PEDS),
        NormalLabValue("Newborn Respiratory Rate", "30 – 60 breaths/min", "> 60 or apnea > 20 sec", "Irregular abdominal breathing is normal in term newborns", LabValueCategory.MATERNAL_PEDS),
        NormalLabValue("Newborn Blood Glucose", "45 – 100 mg/dL", "< 40 mg/dL", "Screen infants of diabetic mothers (IDM) & SGA early", LabValueCategory.MATERNAL_PEDS),
        NormalLabValue("Amniotic Fluid Index (AFI)", "5 – 25 cm", "< 5 cm (Oligo) | > 25 cm (Poly)", "Oligohydramnios linked with Potter sequence / renal agenesis", LabValueCategory.MATERNAL_PEDS),
        NormalLabValue("Normal Newborn Birth Weight", "2.5 – 3.5 kg", "< 2.5 kg (LBW) | < 1.5 kg (VLBW)", "Doubles by 5–6 months; triples by 1 year", LabValueCategory.MATERNAL_PEDS)
    )
}
