package com.example.data.model

import java.util.Locale
import kotlin.math.roundToInt

// ============================================================================
// 1. ZERO-DUPLICATION SPECIALTY TRACKS (Cardiology, Critical Care, OBG, Peds)
// ============================================================================

enum class SpecialtyTrack(
    val id: String,
    val title: String,
    val shortTitle: String,
    val subtitle: String,
    val primaryToolHint: String,
    val linkedIbqCategory: IbqCategory
) {
    CARDIOLOGY(
        id = "cardiology",
        title = "Cardiology & ECG",
        shortTitle = "Cardiology",
        subtitle = "Arrhythmias, MI/STEMI, heart failure, ACLS & vasoactive drugs",
        primaryToolHint = "ECG Studio + Cardiac Spotters",
        linkedIbqCategory = IbqCategory.ECG
    ),
    CRITICAL_CARE(
        id = "critical_care",
        title = "Critical Care & ICU",
        shortTitle = "Critical Care",
        subtitle = "ABG acid-base, airway/ETT, shock, sepsis, trauma, burns & thoracic emergencies",
        primaryToolHint = "ABG Lab + ECG Studio + CXR Spotters",
        linkedIbqCategory = IbqCategory.CLINICAL_IMAGING
    ),
    OBSTETRICS(
        id = "obstetrics",
        title = "Obstetrics & Midwifery",
        shortTitle = "Obstetrics",
        subtitle = "Antenatal care, labor, CTG monitoring, WHO partograph, PPH & eclampsia",
        primaryToolHint = "OBG & CTG Visual Spotters",
        linkedIbqCategory = IbqCategory.OBG_CTG
    ),
    PEDIATRICS(
        id = "pediatrics",
        title = "Pediatrics & NICU",
        shortTitle = "Pediatrics",
        subtitle = "Growth milestones, IMNCI, immunization, neonatal care & pediatric dosing",
        primaryToolHint = "Pediatric Dose Calc + IV Spotters",
        linkedIbqCategory = IbqCategory.DEVICES_COLOR
    )
}

data class SpecialtyTrackProjection(
    val track: SpecialtyTrack,
    val questions: List<QuestionItem>,
    val ibqQuestions: List<QuestionItem>,
    val weakQuestions: List<QuestionItem>,
    val unseenQuestions: List<QuestionItem>,
    val attemptedCount: Int,
    val correctCount: Int
) {
    val totalCount: Int get() = questions.size
    val accuracyPercent: Int
        get() = if (attemptedCount == 0) 0 else ((correctCount.toDouble() / attemptedCount) * 100.0).roundToInt()

    /**
     * Builds a 5-minute high-yield rapid revision drill (default 10Q) prioritizing
     * mistakes -> unseen visual IBQs -> unseen text MCQs -> remaining items, with zero duplicates.
     */
    fun buildRapidRevisionDrill(targetCount: Int = 10): List<QuestionItem> {
        val clamped = targetCount.coerceIn(1, 30)
        val priorityOrdered = (
            weakQuestions +
                ibqQuestions.filter { it.attemptsCount == 0 } +
                unseenQuestions +
                questions
            ).distinctBy { it.id }

        return IbqHubLogic.mixIbqsIntoGeneralPractice(
            generalPool = priorityOrdered,
            ibqPool = ibqQuestions,
            targetCount = clamped.coerceAtMost(questions.size),
            ibqSharePercent = 25,
            minIbqCount = if (ibqQuestions.isNotEmpty() && clamped >= 5) 2 else 0,
            maxIbqCount = 3
        )
    }
}

/**
 * Pure-Kotlin zero-duplication Specialty Track lens over the single unified Room question bank.
 */
object SpecialtyTrackLogic {

    private val CARDIO_KEYWORDS = listOf(
        "ecg", "ekg", "stemi", "myocardial", "infarction", "angina", "arrhythmia",
        "ventricular", "atrial fibrillation", "av block", "pacemaker", "defibrillat",
        "cardioversion", "acls", "cpr", "heart failure", "chf", "digoxin", "amiodarone",
        "atropine", "adenosine", "nitroglycerin", "hypertension", "hypertensive", "endocarditis",
        "pericarditis", "tamponade", "cardiac", "coronary", "troponin", "bradycardia",
        "tachycardia", "torsades", "hyperkalemia", "potassium", "warfarin", "heparin", "stroke"
    )

    private val ICU_KEYWORDS = listOf(
        "abg", "acidosis", "alkalosis", "ventilat", "intubat", "endotracheal", "laryngoscope",
        "tracheostom", "pneumothorax", "pleural effusion", "thoracentesis", "chest tube",
        "shock", "sepsis", "septic", "ards", "burn", "parkland", "rule of nines", "glasgow",
        "gcs", "icp", "intracranial", "venturi", "non-rebreather", "oxygen", "hypoxia",
        "hypoxemia", "central venous", "cvp", "cannula", "fluid resuscitation", "triage",
        "emergency", "icu", "critical", "anaphylax", "status epilepticus", "dka", "ketoacidosis"
    )

    private val OBG_KEYWORDS = listOf(
        "obstetric", "pregnan", "antenatal", "intrapartum", "postpartum", "labor", "labour",
        "partograph", "ctg", "cardiotocography", "fetal heart", "deceleration", "placenta",
        "eclampsia", "preeclampsia", "oxytocin", "magnesium sulfate", "pph", "speculum",
        "cervical", "uterine", "apgar", "lochia", "amniotic", "cord prolapse", "bishop",
        "leopold", "ectopic", "cesarean", "caesarean", "breastfeeding", "gynec"
    )

    private val PEDS_KEYWORDS = listOf(
        "pediatric", "paediatric", "neonate", "neonatal", "newborn", "infant", "toddler",
        "child", "milestone", "imnci", "immunization", "vaccine", "bcg", "pentavalent",
        "measles", "rotavirus", "apgar", "jaundice", "phototherapy", "kernicterus",
        "kwashiorkor", "marasmus", "sam", "ors", "dehydration", "tetralogy", "fallot",
        "croup", "bronchiolitis", "kawasaki", "intussusception", "pyloric", "hirschsprung",
        "nicu", "preterm", "kangaroo mother", "kmc", "fontanelle"
    )

    fun matchesTrack(question: Question, track: SpecialtyTrack): Boolean {
        val subj = question.subject.trim().uppercase(Locale.US)
        val topic = question.topicId.lowercase(Locale.US)
        val text = "${question.questionText} ${question.explanation} ${question.sourceNote}".lowercase(Locale.US)

        return when (track) {
            SpecialtyTrack.OBSTETRICS -> {
                subj == "OBG" ||
                    topic.startsWith("obg_") ||
                    OBG_KEYWORDS.any { text.contains(it) }
            }
            SpecialtyTrack.PEDIATRICS -> {
                subj == "PEDS" ||
                    topic.startsWith("peds_") ||
                    PEDS_KEYWORDS.any { text.contains(it) }
            }
            SpecialtyTrack.CARDIOLOGY -> {
                topic.contains("cardiac") ||
                    question.imageUrl.lowercase(Locale.US).startsWith("diagram://ecg_") ||
                    (subj in setOf("MSN", "PHARM", "ANAT", "FON") && CARDIO_KEYWORDS.any { text.contains(it) })
            }
            SpecialtyTrack.CRITICAL_CARE -> {
                topic.contains("critical") ||
                    topic.contains("airway") ||
                    topic.contains("respiratory") ||
                    topic.contains("thoracic") ||
                    topic.contains("burns") ||
                    topic.contains("electrolyte") ||
                    topic.contains("iv_therapy") ||
                    (subj in setOf("MSN", "FON", "PHARM") && ICU_KEYWORDS.any { text.contains(it) })
            }
        }
    }

    fun projectTrack(
        allQuestions: List<QuestionItem>,
        track: SpecialtyTrack
    ): SpecialtyTrackProjection {
        val distinct = allQuestions.distinctBy { it.id }
        val matched = distinct.filter { matchesTrack(it.question, track) }
        val ibqs = matched.filter { it.question.isImageBased }
        val weak = matched.filter { item ->
            item.lastOutcome == "wrong" || (item.attemptsCount >= 2 && item.accuracyPercent < 60)
        }
        val unseen = matched.filter { it.attemptsCount == 0 }
        val attempted = matched.count { it.attemptsCount > 0 }
        val correct = matched.count { it.lastOutcome == "right" }

        return SpecialtyTrackProjection(
            track = track,
            questions = matched,
            ibqQuestions = ibqs,
            weakQuestions = weak,
            unseenQuestions = unseen,
            attemptedCount = attempted,
            correctCount = correct
        )
    }

    fun projectAllTracks(allQuestions: List<QuestionItem>): List<SpecialtyTrackProjection> {
        val distinct = allQuestions.distinctBy { it.id }
        return SpecialtyTrack.entries.map { track -> projectTrack(distinct, track) }
    }
}

// ============================================================================
// 2. CUSTOM MOCK TEST BUILDER (CBT Exam Simulator)
// ============================================================================

enum class CustomMockSourceFilter(val label: String) {
    ALL_BANK("All Bank"),
    FRESH_UNSEEN("Fresh Unseen"),
    MISTAKES_AND_WEAK("Mistakes & Weak"),
    HARD_CLINICAL("Hard & Clinical")
}

data class CustomMockConfig(
    val questionCount: Int = 25,
    val selectedSubjectCodes: Set<String> = emptySet(), // empty = All Subjects
    val sourceFilter: CustomMockSourceFilter = CustomMockSourceFilter.ALL_BANK,
    val includeIbqs: Boolean = true,
    val timedMode: Boolean = true,
    val durationMinutes: Int = 25,
    val enableNegativeMarking: Boolean = true,
    val negativeMarkPenalty: Double = 0.33
) {
    val effectiveNegativeMarks: Double
        get() = if (enableNegativeMarking) negativeMarkPenalty.coerceIn(0.0, 1.0) else 0.0
}

data class CustomMockValidation(
    val availableCount: Int,
    val effectiveCount: Int,
    val canStart: Boolean,
    val statusMessage: String
)

object CustomMockBuilderLogic {
    val COUNT_PRESETS = listOf(15, 25, 50, 75, 100)
    const val MIN_CUSTOM_QUESTIONS = 5
    const val MAX_CUSTOM_QUESTIONS = 160

    fun suggestedMinutesFor(questionCount: Int, secondsPerQuestion: Int = 54): Int {
        val safeCount = questionCount.coerceIn(MIN_CUSTOM_QUESTIONS, MAX_CUSTOM_QUESTIONS)
        val safeSec = secondsPerQuestion.coerceIn(30, 120)
        return ((safeCount * safeSec) / 60.0).roundToInt().coerceIn(5, 240)
    }

    fun filterAvailablePool(
        allQuestions: List<QuestionItem>,
        config: CustomMockConfig
    ): List<QuestionItem> {
        val distinct = allQuestions.distinctBy { it.id }
        return distinct.asSequence()
            .filter { item -> !item.question.isPyqTagged }
            .filter { item ->
                config.includeIbqs || !item.question.isImageBased
            }
            .filter { item ->
                config.selectedSubjectCodes.isEmpty() || item.question.subject in config.selectedSubjectCodes
            }
            .filter { item ->
                when (config.sourceFilter) {
                    CustomMockSourceFilter.ALL_BANK -> true
                    CustomMockSourceFilter.FRESH_UNSEEN -> item.attemptsCount == 0
                    CustomMockSourceFilter.MISTAKES_AND_WEAK -> {
                        item.lastOutcome == "wrong" || (item.attemptsCount >= 2 && item.accuracyPercent < 65)
                    }
                    CustomMockSourceFilter.HARD_CLINICAL -> {
                        item.question.difficulty == QuestionDifficulty.HARD ||
                            item.question.isImageBased ||
                            item.question.questionText.contains("[Mains", ignoreCase = true)
                    }
                }
            }
            .toList()
    }

    fun validate(
        allQuestions: List<QuestionItem>,
        config: CustomMockConfig
    ): CustomMockValidation {
        val available = filterAvailablePool(allQuestions, config)
        val desired = config.questionCount.coerceIn(MIN_CUSTOM_QUESTIONS, MAX_CUSTOM_QUESTIONS)
        val effective = desired.coerceAtMost(available.size)

        return when {
            available.isEmpty() -> CustomMockValidation(
                availableCount = 0,
                effectiveCount = 0,
                canStart = false,
                statusMessage = "No questions match these filters. Try selecting more subjects or switching source to All Bank."
            )
            available.size < MIN_CUSTOM_QUESTIONS -> CustomMockValidation(
                availableCount = available.size,
                effectiveCount = available.size,
                canStart = true,
                statusMessage = "Only ${available.size} matching questions found — will launch a ${available.size}Q mini-mock."
            )
            available.size < desired -> CustomMockValidation(
                availableCount = available.size,
                effectiveCount = available.size,
                canStart = true,
                statusMessage = "${available.size} matching questions available (requested $desired). Launching ${available.size}Q custom mock."
            )
            else -> CustomMockValidation(
                availableCount = available.size,
                effectiveCount = effective,
                canStart = true,
                statusMessage = "${available.size} matching questions ready · Launching ${effective}Q custom CBT mock."
            )
        }
    }

    fun buildMockSessionQuestions(
        allQuestions: List<QuestionItem>,
        config: CustomMockConfig,
        shuffle: Boolean = true
    ): List<QuestionItem> {
        val pool = filterAvailablePool(allQuestions, config)
        if (pool.isEmpty()) return emptyList()
        val desired = config.questionCount.coerceIn(MIN_CUSTOM_QUESTIONS, MAX_CUSTOM_QUESTIONS)
        val effectiveTarget = desired.coerceAtMost(pool.size)

        val orderedPool = if (shuffle) pool.shuffled() else pool
        return if (config.includeIbqs) {
            val ibqs = orderedPool.filter { it.question.isImageBased }
            if (ibqs.isNotEmpty()) {
                IbqHubLogic.mixIbqsIntoGeneralPractice(
                    generalPool = orderedPool,
                    ibqPool = ibqs,
                    targetCount = effectiveTarget,
                    ibqSharePercent = 16,
                    minIbqCount = if (effectiveTarget >= 10) 2 else 1,
                    maxIbqCount = (effectiveTarget / 5).coerceIn(2, 10)
                )
            } else {
                orderedPool.take(effectiveTarget)
            }
        } else {
            orderedPool.take(effectiveTarget)
        }
    }

    fun buildTitle(config: CustomMockConfig, actualCount: Int): String {
        val subjPart = when {
            config.selectedSubjectCodes.isEmpty() -> "All Subjects"
            config.selectedSubjectCodes.size <= 2 -> config.selectedSubjectCodes.sorted().joinToString(", ")
            else -> "${config.selectedSubjectCodes.size} Subjects"
        }
        val negTag = if (config.enableNegativeMarking) "" else " · No -ve"
        val timeTag = if (config.timedMode) " · ${config.durationMinutes.coerceIn(5, 240)}m" else " · Untimed"
        return "Custom ${actualCount}Q CBT ($subjPart$timeTag$negTag)"
    }
}
