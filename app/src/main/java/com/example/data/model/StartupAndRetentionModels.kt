package com.example.data.model

import java.util.Locale
import kotlin.math.abs

/**
 * Phase 0 & Phase 1 Pure-Kotlin Models & Logic:
 * 1. StartupProfiler & StartupNursingPearls (real startup phase telemetry + non-blocking pearls)
 * 2. Question of the Day (14 curated, medically verified Indian Nursing Officer clinical cases
 *    with per-option rationales, nursing priorities, exam pearls, common traps & deeper learning)
 * 3. Hybrid Per-Option Rationale Analyzer (structured offline option cards without hallucination)
 * 4. Custom Daily Goal Validator (presets 10/20/30/50 + custom 5..200)
 */

// ============================================================================
// 1. STARTUP PERFORMANCE INSTRUMENTATION & NON-BLOCKING LOADING PEARLS
// ============================================================================

enum class StartupLaunchMode(val label: String) {
    COLD_SEED("cold_seed"),
    WARM_CACHE("warm_cache")
}

data class StartupPhaseMark(
    val phaseName: String,
    val elapsedMs: Long
)

data class StartupTimingReport(
    val launchMode: StartupLaunchMode,
    val totalToFirstDataMs: Long,
    val seedDurationMs: Long,
    val marks: List<StartupPhaseMark>
) {
    fun formatLogLine(): String {
        val details = marks.joinToString(", ") { "${it.phaseName}=${it.elapsedMs}ms" }
        return "ClinEdgeStartup mode=${launchMode.label} firstData=${totalToFirstDataMs}ms seed=${seedDurationMs}ms [$details]"
    }
}

class StartupProfiler(
    private val clockMs: () -> Long = { System.currentTimeMillis() }
) {
    private val startedAtMs: Long = clockMs()
    private val marks = mutableListOf<StartupPhaseMark>()
    private var launchMode: StartupLaunchMode = StartupLaunchMode.WARM_CACHE
    private var seedStartMs: Long? = null
    private var seedDurationMs: Long = 0L
    private var firstDataMs: Long = 0L

    @Synchronized
    fun mark(phaseName: String): Long {
        val elapsed = (clockMs() - startedAtMs).coerceAtLeast(0L)
        marks.add(StartupPhaseMark(phaseName = phaseName, elapsedMs = elapsed))
        if (phaseName == "seed_start") {
            seedStartMs = elapsed
        } else if (phaseName == "seed_done") {
            seedDurationMs = (elapsed - (seedStartMs ?: 0L)).coerceAtLeast(0L)
        } else if (phaseName == "first_data_ready" && firstDataMs == 0L) {
            firstDataMs = elapsed
        }
        return elapsed
    }

    @Synchronized
    fun setLaunchMode(mode: StartupLaunchMode) {
        launchMode = mode
    }

    @Synchronized
    fun buildReport(): StartupTimingReport = StartupTimingReport(
        launchMode = launchMode,
        totalToFirstDataMs = firstDataMs.takeIf { it > 0L } ?: (clockMs() - startedAtMs).coerceAtLeast(0L),
        seedDurationMs = seedDurationMs,
        marks = marks.toList()
    )
}

data class HighYieldStartupPearl(
    val tag: String,
    val text: String
)

object StartupNursingPearls {
    val ALL: List<HighYieldStartupPearl> = listOf(
        HighYieldStartupPearl(
            tag = "OBG · PRIORITY",
            text = "Before every MgSO4 dose in eclampsia, verify: patellar reflex present, RR ≥ 16/min, and urine output ≥ 30 mL/hr."
        ),
        HighYieldStartupPearl(
            tag = "EMERGENCY",
            text = "First-line anaphylaxis drug: Epinephrine 1:1000 (0.3–0.5 mg) IM into the anterolateral thigh (vastus lateralis)."
        ),
        HighYieldStartupPearl(
            tag = "IMMUNIZATION",
            text = "BCG is reconstituted ONLY with Normal Saline and must be discarded 4 hours after reconstitution."
        ),
        HighYieldStartupPearl(
            tag = "CARDIOLOGY",
            text = "Hyperkalemia ECG progression: Tall peaked T waves → prolonged PR → flattened P → widened QRS → sine wave."
        ),
        HighYieldStartupPearl(
            tag = "CRITICAL CARE",
            text = "In Diabetic Ketoacidosis (DKA), isotonic 0.9% NaCl fluid resuscitation comes BEFORE regular insulin infusion."
        ),
        HighYieldStartupPearl(
            tag = "PHARMACOLOGY",
            text = "Hypokalemia (< 3.5 mEq/L) precipitates Digoxin toxicity; always check apical pulse for 1 full minute and serum K+."
        ),
        HighYieldStartupPearl(
            tag = "PEDIATRICS",
            text = "APGAR score is assessed at 1 and 5 minutes after birth — never delay neonatal resuscitation to wait for the 1-minute score."
        ),
        HighYieldStartupPearl(
            tag = "CHN · COLD CHAIN",
            text = "ILR stays at +2°C to +8°C: heat-sensitive vaccines (OPV, Measles, BCG) at bottom; freeze-sensitive (Penta, Hep B, Td) on top."
        ),
        HighYieldStartupPearl(
            tag = "SURGERY · BURNS",
            text = "Parkland formula: 4 mL × kg × %TBSA of Ringer Lactate — give half in the first 8 hours from time of burn."
        ),
        HighYieldStartupPearl(
            tag = "AIRWAY",
            text = "Limit tracheostomy/ET suctioning to 10–15 seconds per pass and apply suction ONLY during catheter withdrawal."
        )
    )

    fun pearlForIndex(index: Int): HighYieldStartupPearl {
        val safeIdx = ((index % ALL.size) + ALL.size) % ALL.size
        return ALL[safeIdx]
    }
}

// ============================================================================
// 2. DAILY GOAL PRESETS & VALIDATION
// ============================================================================

object DailyGoalValidation {
    val PRESET_TARGETS: List<Int> = listOf(10, 20, 30, 50)
    const val MIN_CUSTOM_TARGET = 5
    const val MAX_CUSTOM_TARGET = 200

    fun parseCustomTarget(raw: String): Int? {
        val clean = raw.trim()
        if (clean.isEmpty()) return null
        val parsed = clean.toIntOrNull() ?: return null
        return if (parsed in MIN_CUSTOM_TARGET..MAX_CUSTOM_TARGET) parsed else null
    }

    fun validationHint(raw: String): String? {
        val clean = raw.trim()
        if (clean.isEmpty()) return null
        val parsed = clean.toIntOrNull() ?: return "Enter a whole number between $MIN_CUSTOM_TARGET and $MAX_CUSTOM_TARGET."
        return when {
            parsed < MIN_CUSTOM_TARGET -> "Minimum daily goal is $MIN_CUSTOM_TARGET questions."
            parsed > MAX_CUSTOM_TARGET -> "Maximum daily goal is $MAX_CUSTOM_TARGET questions."
            else -> null
        }
    }
}

// ============================================================================
// 3. QUESTION OF THE DAY (QOTD) — CURATED, MEDICALLY VERIFIED BANK
// ============================================================================

data class OptionRationaleDetail(
    val optionIndex: Int,
    val label: String,
    val optionText: String,
    val isCorrect: Boolean,
    val isUserSelected: Boolean = false,
    val explanation: String,
    val whenCorrectElsewhere: String = ""
)

data class QotdOptionRationale(
    val optionIndex: Int,
    val whyCorrectOrWrong: String,
    val whenAppliesElsewhere: String = ""
)

data class DailyFeaturedQuestion(
    val id: String,
    val topicTag: String,
    val subjectCode: String,
    val difficulty: QuestionDifficulty,
    val examTag: String,
    val questionText: String,
    val options: List<String>,
    val correctIndex: Int,
    val conciseSummary: String,
    val optionRationales: List<QotdOptionRationale>,
    val nursingPriority: String,
    val examPearl: String,
    val commonTrap: String,
    val deeperLearning: String
)

data class QotdCompletionState(
    val dateKey: String = "",
    val questionId: String = "",
    val selectedIndex: Int = -1,
    val isCorrect: Boolean = false
) {
    fun isAnsweredFor(todayKey: String, expectedQuestionId: String): Boolean =
        dateKey == todayKey && questionId == expectedQuestionId && selectedIndex >= 0
}

object QuestionOfTheDayCatalog {

    val ALL_QUESTIONS: List<DailyFeaturedQuestion> = listOf(
        DailyFeaturedQuestion(
            id = "QOTD_01",
            topicTag = "Eclampsia · Pharmacology",
            subjectCode = "OBG",
            difficulty = QuestionDifficulty.MEDIUM,
            examTag = "AIIMS NORCET · RRB",
            questionText = "A primigravida at 36 weeks with severe preeclampsia is receiving IV Magnesium Sulfate (MgSO4). On hourly bedside assessment, her respiratory rate is 11/min and patellar reflexes are absent. What is the nurse's IMMEDIATE priority action?",
            options = listOf(
                "Reduce the MgSO4 infusion rate by half and recheck in 30 minutes",
                "Stop the MgSO4 infusion immediately and prepare IV 10% Calcium Gluconate",
                "Administer IV Hydralazine to stabilize maternal blood pressure",
                "Place the patient in Trendelenburg position and start a 500 mL fluid bolus"
            ),
            correctIndex = 1,
            conciseSummary = "Absent deep tendon reflexes and respiratory depression (< 12–16/min) are hallmark signs of Magnesium Sulfate toxicity requiring immediate cessation of the infusion and antidote administration.",
            optionRationales = listOf(
                QotdOptionRationale(
                    0,
                    "Continuing MgSO4 even at half rate worsens neuromuscular blockade and risks respiratory or cardiac arrest.",
                    "Dose reduction is only considered with physician orders when serum magnesium is mildly elevated without respiratory compromise."
                ),
                QotdOptionRationale(
                    1,
                    "Stopping the offending infusion halts further toxicity immediately, while 10% Calcium Gluconate (10 mL slow IV over 3–10 min) physiologically antagonizes magnesium at the neuromuscular junction."
                ),
                QotdOptionRationale(
                    2,
                    "Hydralazine lowers severe hypertension but does nothing to reverse magnesium-induced respiratory depression.",
                    "Appropriate when severe hypertension (BP ≥ 160/110 mmHg) is the primary emergency."
                ),
                QotdOptionRationale(
                    3,
                    "Trendelenburg compromises diaphragmatic excursion and excess IV fluid risks acute pulmonary edema in preeclampsia.",
                    "Left lateral tilt with head slightly elevated is preferred for uteroplacental perfusion and airway safety."
                )
            ),
            nursingPriority = "Stop MgSO4 immediately → maintain airway & give O2 → administer 10% Calcium Gluconate 1 g (10 mL) slow IV → notify obstetrician.",
            examPearl = "Toxicity progression by serum Mg2+ level: Loss of patellar reflex (8–10 mEq/L) → Respiratory depression (10–12 mEq/L) → Cardiac arrest (> 15 mEq/L).",
            commonTrap = "Do not wait for a serum magnesium lab report before stopping the infusion when respiratory rate drops below 12/min.",
            deeperLearning = "Under the Pritchard regimen, therapeutic serum magnesium is 4–7 mEq/L (4.8–8.4 mg/dL). Because MgSO4 is excreted almost exclusively by the kidneys, oliguria (< 30 mL/hr or < 100 mL over 4 hours) is an early warning sign of impending accumulation."
        ),
        DailyFeaturedQuestion(
            id = "QOTD_02",
            topicTag = "Hyperkalemia · ECG",
            subjectCode = "MSN",
            difficulty = QuestionDifficulty.HARD,
            examTag = "AIIMS NORCET · PGIMER",
            questionText = "A chronic kidney disease patient presents with serum potassium of 7.1 mEq/L and a 12-lead ECG showing tall, peaked T waves with widened QRS complexes. Which medication is administered FIRST to protect the myocardium?",
            options = listOf(
                "IV 10% Calcium Gluconate",
                "Oral Sodium Polystyrene Sulfonate (Kayexalate)",
                "Nebulized Salbutamol (Albuterol)",
                "IV Furosemide"
            ),
            correctIndex = 0,
            conciseSummary = "When severe hyperkalemia causes ECG changes (widened QRS / peaked T waves), IV Calcium Gluconate is given FIRST because it stabilizes the cardiac cell membrane within 1–3 minutes.",
            optionRationales = listOf(
                QotdOptionRationale(
                    0,
                    "IV 10% Calcium Gluconate (10 mL over 2–5 min with ECG monitoring) raises the myocardial threshold potential immediately to prevent ventricular fibrillation or asystole."
                ),
                QotdOptionRationale(
                    1,
                    "Cation-exchange resins take hours to eliminate potassium via the GI tract and cannot prevent imminent cardiac arrest.",
                    "Used for non-emergent, subacute total-body potassium elimination."
                ),
                QotdOptionRationale(
                    2,
                    "Beta-2 agonists shift K+ intracellularly within 15–30 minutes—useful as second-line therapy alongside Regular Insulin + Dextrose, not as the fastest membrane stabilizer.",
                    "Adjunct therapy after myocardial stabilization to lower serum K+."
                ),
                QotdOptionRationale(
                    3,
                    "Loop diuretics require residual renal function and take 30–60 minutes; unreliable in oliguric CKD.",
                    "Useful for volume-overloaded hyperkalemic patients with preserved urine output."
                )
            ),
            nursingPriority = "Place on continuous cardiac telemetry → give IV Calcium Gluconate first → follow immediately with IV Regular Insulin + D50W/D25W to shift K+ intracellularly.",
            examPearl = "Remember C-BIG-K-Drop: Calcium gluconate (stabilize), Bicarb/Beta-agonist, Insulin + Glucose (shift), Kayexalate/Diuretics/Dialysis (remove).",
            commonTrap = "Calcium Gluconate does NOT lower serum potassium concentration—it only buys 30–60 minutes of cardiac protection while K+-lowering therapies work.",
            deeperLearning = "After giving IV Regular Insulin (10 units) with Dextrose (25–50 g), monitor capillary blood glucose hourly for at least 4–6 hours because CKD patients have reduced renal insulin clearance and high risk of delayed hypoglycemia."
        ),
        DailyFeaturedQuestion(
            id = "QOTD_03",
            topicTag = "Anaphylaxis · Emergency",
            subjectCode = "FON",
            difficulty = QuestionDifficulty.EASY,
            examTag = "RRB · ESIC · NORCET",
            questionText = "Five minutes after starting IV Ceftriaxone, a ward patient develops inspiratory stridor, generalized urticaria, and BP 78/46 mmHg. After stopping the antibiotic infusion, what is the FIRST-LINE medication and route?",
            options = listOf(
                "IV Hydrocortisone 100 mg slow push",
                "IM Epinephrine (1:1000) 0.3–0.5 mg in the anterolateral thigh",
                "Subcutaneous Epinephrine (1:1000) 0.5 mg in the upper arm",
                "IV Chlorpheniramine (Avil) 10 mg bolus"
            ),
            correctIndex = 1,
            conciseSummary = "Intramuscular Epinephrine 1:1000 into the vastus lateralis (anterolateral thigh) is the single life-saving first-line drug in anaphylaxis.",
            optionRationales = listOf(
                QotdOptionRationale(
                    0,
                    "Corticosteroids take 4–6 hours to act and cannot reverse acute bronchospasm or distributive shock.",
                    "Given as adjunct therapy to help reduce risk of biphasic anaphylactic reactions."
                ),
                QotdOptionRationale(
                    1,
                    "IM vastus lateralis injection produces rapid, reliable peak plasma epinephrine levels (within ~8 min), reversing bronchospasm (beta-2) and hypotension (alpha-1)."
                ),
                QotdOptionRationale(
                    2,
                    "Subcutaneous absorption is erratic and delayed during shock-induced peripheral vasoconstriction.",
                    "Obsolete route for anaphylaxis; IM thigh is strictly preferred."
                ),
                QotdOptionRationale(
                    3,
                    "H1 antihistamines relieve itching and hives only; they do not treat airway edema or circulatory collapse.",
                    "Second-line adjunct for cutaneous symptoms after epinephrine."
                )
            ),
            nursingPriority = "Stop offending infusion → Give IM Epinephrine in outer mid-thigh → High-flow O2 → Supine position with legs elevated (if airway allows) → Rapid isotonic IV crystalloid bolus.",
            examPearl = "Concentration check: IM anaphylaxis = 1:1000 (1 mg/mL); IV cardiac arrest = 1:10,000 (0.1 mg/mL). Never push 1:1000 undiluted IV.",
            commonTrap = "Never sit a hypotensive anaphylaxis patient upright abruptly—sudden venous pooling can trigger 'empty ventricle' cardiac arrest.",
            deeperLearning = "Repeat IM epinephrine every 5–15 minutes if stridor or hypotension persists. Keep the patient under monitored observation for at least 4–8 hours due to the risk of biphasic anaphylaxis."
        ),
        DailyFeaturedQuestion(
            id = "QOTD_04",
            topicTag = "DKA · Critical Care",
            subjectCode = "MSN",
            difficulty = QuestionDifficulty.MEDIUM,
            examTag = "AIIMS NORCET · JIPMER",
            questionText = "A young adult with Type 1 Diabetes arrives in the Emergency Department with Kussmaul respirations, fruity breath, blood glucose 510 mg/dL, pH 7.14, and BP 88/54 mmHg. Which IV intervention has the HIGHEST initial priority?",
            options = listOf(
                "IV Regular Insulin bolus followed by infusion",
                "IV 0.9% Normal Saline rapid fluid resuscitation",
                "IV Sodium Bicarbonate 100 mEq bolus",
                "IV 5% Dextrose in Water (D5W) infusion"
            ),
            correctIndex = 1,
            conciseSummary = "Restoring intravascular volume with isotonic 0.9% Normal Saline is always Step 1 in DKA—improving tissue perfusion, lowering counter-regulatory hormones, and clarifying true serum potassium before insulin starts.",
            optionRationales = listOf(
                QotdOptionRationale(
                    0,
                    "Starting insulin before fluid resuscitation and checking serum K+ can precipitate severe hypokalemia and worsen hypotension.",
                    "Started after initial saline bolus once serum K+ is confirmed ≥ 3.3 mEq/L."
                ),
                QotdOptionRationale(
                    1,
                    "DKA patients have a 4–6 L water deficit; 0.9% NaCl restores blood pressure, renal perfusion, and immediately starts lowering glucose via dilution and glycosuria."
                ),
                QotdOptionRationale(
                    2,
                    "Routine bicarbonate in DKA with pH > 6.9–7.0 increases risk of paradoxical CNS acidosis, hypokalemia, and cerebral edema.",
                    "Reserved strictly for life-threatening acidemia (pH < 6.9)."
                ),
                QotdOptionRationale(
                    3,
                    "Adding dextrose at glucose 510 mg/dL worsens hyperosmolarity.",
                    "Switched to D5 with 0.45% NaCl once blood glucose falls to ~200–250 mg/dL while continuing insulin to clear ketones."
                )
            ),
            nursingPriority = "Airway & 2 large-bore IV lines → 0.9% NaCl bolus (1 L/hr initially) → check serum K+ BEFORE insulin → hourly glucose & neurologic checks.",
            examPearl = "Hold IV insulin and replace potassium FIRST if initial serum K+ is < 3.3 mEq/L—insulin drives K+ into cells and can trigger fatal arrhythmias.",
            commonTrap = "Never stop the IV insulin drip just because blood glucose reaches 250 mg/dL; add dextrose and continue insulin until anion gap closes and acidosis resolves.",
            deeperLearning = "Target glucose drop is 50–75 mg/dL per hour. Overly rapid osmolarity drops increase the risk of cerebral edema, especially in pediatric and adolescent DKA."
        ),
        DailyFeaturedQuestion(
            id = "QOTD_05",
            topicTag = "NIS India · Cold Chain",
            subjectCode = "CHN",
            difficulty = QuestionDifficulty.EASY,
            examTag = "RRB · ESIC · DSSSB",
            questionText = "At a Primary Health Centre (PHC), a nurse is organizing vaccines inside an Ice-Lined Refrigerator (ILR) maintained at +2°C to +8°C. Which group of vaccines MUST be placed in the UPPER baskets away from the cold bottom?",
            options = listOf(
                "OPV, Measles-Rubella (MR), and BCG",
                "Pentavalent, Hepatitis B, Td, and IPV",
                "BCG, Yellow Fever, and OPV",
                "Measles-Rubella (MR) and Rotavirus only"
            ),
            correctIndex = 1,
            conciseSummary = "Freeze-sensitive T-series and adsorbed vaccines (Pentavalent, Hep B, Td, IPV, PCV) are destroyed by freezing and must be stored in the upper baskets of an ILR.",
            optionRationales = listOf(
                QotdOptionRationale(
                    0,
                    "OPV, MR, and BCG are heat-sensitive live vaccines that tolerate lower temperatures and belong at the bottom of the ILR.",
                    "Correct choice if the question asks which vaccines go at the BOTTOM of the ILR."
                ),
                QotdOptionRationale(
                    1,
                    "Aluminium-adsorbed and inactivated vaccines (Pentavalent, Hep B, Td, IPV, PCV) lose potency permanently if frozen, so they must stay in the warmer upper baskets."
                ),
                QotdOptionRationale(
                    2,
                    "These are heat-sensitive live attenuated vaccines kept in the bottom compartment.",
                    "Stored at the bottom of the ILR."
                ),
                QotdOptionRationale(
                    3,
                    "MR is heat-sensitive (kept at bottom), not freeze-sensitive.",
                    "MR belongs with OPV and BCG near the bottom."
                )
            ),
            nursingPriority = "Log ILR temperature twice daily (+2°C to +8°C), keep diluents chilled 24 hours before sessions, and perform the Shake Test if freezing is suspected.",
            examPearl = "ILR Bottom = Heat-sensitive (OPV, Measles/MR, BCG). ILR Top = Freeze-sensitive (Pentavalent, Hep B, DPT, Td, IPV, PCV).",
            commonTrap = "Remember: Vaccine Vial Monitor (VVM) detects HEAT exposure only—it will NOT turn dark if a freeze-sensitive vaccine was frozen!",
            deeperLearning = "In the WHO Shake Test, compare a suspect vial against a deliberately frozen-and-thawed control vial of the same batch; if the suspect vial sediments at the same fast rate as the frozen control, discard it."
        ),
        DailyFeaturedQuestion(
            id = "QOTD_06",
            topicTag = "PPH · Obstetric Emergency",
            subjectCode = "OBG",
            difficulty = QuestionDifficulty.MEDIUM,
            examTag = "AIIMS NORCET · RRB",
            questionText = "Thirty minutes after a normal vaginal delivery, a mother saturates two perineal pads with bright red bleeding. On palpation, the uterine fundus is soft, boggy, and displaced to the right of the umbilicus. What is the nurse's FIRST action?",
            options = listOf(
                "Perform firm fundal massage and assist the mother to empty her bladder",
                "Administer IV Methylergometrine (Methergine) immediately without checking BP",
                "Pack the vagina tightly with sterile gauze rolls",
                "Prepare the patient immediately for emergency hysterectomy"
            ),
            correctIndex = 0,
            conciseSummary = "A boggy uterus shifted to the right signals uterine atony compounded by a distended urinary bladder; immediate fundal massage plus bladder emptying is first-line.",
            optionRationales = listOf(
                QotdOptionRationale(
                    0,
                    "Firm bimanual/fundal massage stimulates myometrial contraction to clamp spiral arteries, and emptying a full bladder allows the uterus to contract centrally."
                ),
                QotdOptionRationale(
                    1,
                    "Methergine causes severe vasoconstriction and is strictly contraindicated in hypertension/preeclampsia—BP must always be checked first.",
                    "Used as a second-line uterotonic only after confirming normotension."
                ),
                QotdOptionRationale(
                    2,
                    "Vaginal packing conceals ongoing uterine bleeding and increases infection risk without correcting uterine atony.",
                    "Intrauterine balloon tamponade (Bakri/condom catheter) is used for refractory atony, not simple vaginal gauze packing."
                ),
                QotdOptionRationale(
                    3,
                    "Hysterectomy is a last-resort surgical intervention only after uterotonics, tranexamic acid, and tamponade fail.",
                    "Considered in intractable PPH or placenta accreta spectrum."
                )
            ),
            nursingPriority = "Massage boggy fundus → empty bladder (straight catheter if unable to void) → administer IV Oxytocin & Tranexamic Acid (within 3 hrs) → quantify blood loss & monitor vitals.",
            examPearl = "4 Ts of Postpartum Hemorrhage: Tone (70% — most common), Trauma (20%), Tissue (10%), Thrombin (<1%).",
            commonTrap = "Always support the lower uterine segment with your non-dominant hand above the symphysis pubis during fundal massage to prevent uterine inversion.",
            deeperLearning = "WHO recommends Active Management of the Third Stage of Labor (AMTSL) with 10 IU IM/IV Oxytocin immediately after birth of the baby as the single most effective measure to prevent atonic PPH."
        ),
        DailyFeaturedQuestion(
            id = "QOTD_07",
            topicTag = "Digoxin Toxicity · Pharmacology",
            subjectCode = "PHARM",
            difficulty = QuestionDifficulty.MEDIUM,
            examTag = "RRB · PGIMER · NORCET",
            questionText = "A 68-year-old heart failure patient on Digoxin and Furosemide reports nausea, anorexia, and yellow-green halos around ward lights. Apical pulse is 52 bpm. Which electrolyte abnormality MOST strongly predisposes this patient to Digoxin toxicity?",
            options = listOf(
                "Hypernatremia (Na+ 150 mEq/L)",
                "Hypokalemia (K+ 2.9 mEq/L)",
                "Hypermagnesemia (Mg2+ 3.2 mEq/L)",
                "Hypocalcemia (Ca2+ 7.6 mg/dL)"
            ),
            correctIndex = 1,
            conciseSummary = "Hypokalemia dramatically increases Digoxin binding to the myocardial Na+/K+-ATPase pump, precipitating toxicity even at normal therapeutic serum Digoxin levels.",
            optionRationales = listOf(
                QotdOptionRationale(
                    0,
                    "Serum sodium does not directly compete with digoxin at the Na+/K+-ATPase binding site.",
                    "Relevant in dehydration or diabetes insipidus."
                ),
                QotdOptionRationale(
                    1,
                    "Potassium and Digoxin compete for the same binding site on Na+/K+-ATPase; loop diuretic-induced hypokalemia leaves more sites open for Digoxin, causing toxicity."
                ),
                QotdOptionRationale(
                    2,
                    "Hypomagnesemia (not hypermagnesemia) increases digoxin toxicity risk.",
                    "Hypermagnesemia causes hyporeflexia and respiratory depression."
                ),
                QotdOptionRationale(
                    3,
                    "Hypercalcemia (not hypocalcemia) potentiates digoxin toxicity by increasing intracellular calcium load.",
                    "Hypocalcemia causes Chvostek's and Trousseau's signs."
                )
            ),
            nursingPriority = "Hold Digoxin dose (adult apical pulse < 60 bpm) → notify physician → check serum Digoxin (therapeutic 0.5–2.0 ng/mL) and K+/Mg2+ → prepare Digoxin Immune Fab (Digibind) if life-threatening.",
            examPearl = "Early signs of Digoxin toxicity are GI (anorexia, nausea, vomiting) before visual halos (xanthopsia) and bradyarrhythmias.",
            commonTrap = "Apical pulse hold thresholds: Adults < 60 bpm, Children < 70 bpm, Infants < 90–100 bpm.",
            deeperLearning = "Hypokalemia, hypomagnesemia, and hypercalcemia are the classic electrolyte triad that worsens Digoxin toxicity. Always pair potassium-wasting diuretics (Furosemide) with regular K+ monitoring."
        ),
        DailyFeaturedQuestion(
            id = "QOTD_08",
            topicTag = "OP Poisoning · Toxicology",
            subjectCode = "MSN",
            difficulty = QuestionDifficulty.MEDIUM,
            examTag = "AIIMS NORCET · ESIC",
            questionText = "A farmer is brought to Casualty after ingesting an organophosphate pesticide with pinpoint pupils, profuse salivation, frothy lung crackles, and HR 44/min. Which clinical sign best indicates adequate 'atropinization' during IV Atropine therapy?",
            options = listOf(
                "Pupils dilated to 8 mm with dry flushed skin",
                "Clear lung fields on auscultation with drying of bronchial secretions",
                "Blood pressure rising above 160/100 mmHg",
                "Complete cessation of all muscle fasciculations"
            ),
            correctIndex = 1,
            conciseSummary = "Drying of tracheobronchial secretions and clear lung auscultation—NOT pupil dilation alone—is the primary clinical endpoint of adequate atropinization.",
            optionRationales = listOf(
                QotdOptionRationale(
                    0,
                    "Pupillary dilation can occur early or be delayed by local eye exposure and is an unreliable solitary endpoint; excessive dryness with delirium signals atropine toxicity."
                ),
                QotdOptionRationale(
                    1,
                    "Bronchorrhea and bronchospasm cause respiratory failure (the immediate cause of death in OP poisoning), so clear lungs, HR > 80/min, and SBP > 80 mmHg are the true targets."
                ),
                QotdOptionRationale(
                    2,
                    "Severe hypertension is not an endpoint of atropinization.",
                    "Goal is merely maintaining adequate perfusion (SBP > 80–90 mmHg)."
                ),
                QotdOptionRationale(
                    3,
                    "Atropine blocks muscarinic receptors only; nicotinic muscle fasciculations require Pralidoxime (2-PAM) to reactivate acetylcholinesterase.",
                    "Resolved by Pralidoxime (2-PAM), not Atropine."
                )
            ),
            nursingPriority = "Protect staff with PPE & decontaminate skin/clothes → suction airway & give O2 → aggressive IV Atropine doubling doses until chest clears → IV Pralidoxime (2-PAM).",
            examPearl = "Muscarinic mnemonic DUMBELS (Diarrhea, Urination, Miosis, Bronchorrhea/Bradycardia, Emesis, Lacrimation, Salivation) is reversed by Atropine.",
            commonTrap = "Do not withhold Atropine due to mild tachycardia if the patient still has profuse wet lung secretions—hypoxia from bronchorrhea causes tachycardia too!",
            deeperLearning = "After initial atropinization, start a continuous Atropine maintenance infusion (10–20% of the total loading dose per hour) and monitor closely for 'Intermediate Syndrome' (proximal limb and respiratory muscle weakness 24–96 hours post-ingestion)."
        ),
        DailyFeaturedQuestion(
            id = "QOTD_09",
            topicTag = "Neonatal Care · Pediatrics",
            subjectCode = "PEDS",
            difficulty = QuestionDifficulty.EASY,
            examTag = "RRB · NORCET · JIPMER",
            questionText = "At 1 minute after birth, a term newborn has HR 118/min, weak irregular cry, some flexion of extremities, grimace on suctioning, and a pink body with blue hands and feet. What is the APGAR score?",
            options = listOf(
                "5",
                "6",
                "7",
                "8"
            ),
            correctIndex = 1,
            conciseSummary = "Scoring each parameter: Appearance (acrocyanosis) = 1, Pulse (> 100) = 2, Grimace (grimace only) = 1, Activity (some flexion) = 1, Respiration (weak cry) = 1 → Total = 6.",
            optionRationales = listOf(
                QotdOptionRationale(0, "Under-scores by 1 point—heart rate > 100 bpm awards 2 full points."),
                QotdOptionRationale(1, "Correct calculation: A(1) + P(2) + G(1) + A(1) + R(1) = 6/10, indicating mild-to-moderate transition distress needing tactile stimulation and reassessment."),
                QotdOptionRationale(2, "Over-scores by 1 point—either vigorous cry, active motion, or sneeze/cough would be needed to reach 7."),
                QotdOptionRationale(3, "An APGAR of 8 requires at least three parameters scoring 2.")
            ),
            nursingPriority = "Dry, keep warm under radiant warmer, clear secretions gently if obstructing airway, provide tactile stimulation, and reassess APGAR at 5 minutes.",
            examPearl = "APGAR 7–10 = Normal routine care; 4–6 = Moderate depression (stimulation/O2/PPV if needed); 0–3 = Severe depression (immediate resuscitation).",
            commonTrap = "Acrocyanosis (pink body, blue hands/feet) is normal in the first 24 hours and scores 1—never 0 (0 is completely pale or centrally cyanotic).",
            deeperLearning = "Under NRP guidelines, never delay initial resuscitation steps (warm, dry, stimulate, clear airway, start PPV if apneic or HR < 100 within the 'Golden Minute') just to calculate the 1-minute APGAR."
        ),
        DailyFeaturedQuestion(
            id = "QOTD_10",
            topicTag = "Parkland Formula · Burns",
            subjectCode = "MSN",
            difficulty = QuestionDifficulty.MEDIUM,
            examTag = "AIIMS NORCET · RRB",
            questionText = "A 70 kg adult sustains 40% TBSA partial- and full-thickness thermal burns at 10:00 AM and arrives in the burn ICU at 12:00 PM. Using the Parkland formula (4 mL/kg/%TBSA), how much Ringer Lactate should be infused over the FIRST 8 hours, and by what clock time must it finish?",
            options = listOf(
                "5,600 mL completed by 8:00 PM",
                "5,600 mL completed by 6:00 PM",
                "11,200 mL completed by 6:00 PM",
                "2,800 mL completed by 8:00 PM"
            ),
            correctIndex = 1,
            conciseSummary = "Total 24-hour fluid = 4 × 70 × 40 = 11,200 mL. Half (5,600 mL) is given in the first 8 hours calculated FROM THE TIME OF BURN (10:00 AM → 6:00 PM), leaving 6 hours from arrival.",
            optionRationales = listOf(
                QotdOptionRationale(
                    0,
                    "8:00 PM mistakenly counts 8 hours from hospital arrival (12:00 PM) instead of from the time of burn injury (10:00 AM)."
                ),
                QotdOptionRationale(
                    1,
                    "Half of 11,200 mL is 5,600 mL, and the 8-hour window starts at injury time (10:00 AM + 8 hrs = 6:00 PM), so the remaining 6 hours run at ~933 mL/hr."
                ),
                QotdOptionRationale(
                    2,
                    "11,200 mL is the FULL 24-hour volume, not the first 8-hour half—giving all in 8 hours causes severe fluid overload and compartment syndrome."
                ),
                QotdOptionRationale(
                    3,
                    "2,800 mL is only 25% of the total volume (half of the required first-8-hour volume) and uses the wrong finish time."
                )
            ),
            nursingPriority = "Titrate actual IV rate to hourly urine output (adult target 0.5 mL/kg/hr = 30–50 mL/hr; electrical burns 75–100 mL/hr) rather than following the formula blindly.",
            examPearl = "Parkland split: 50% in first 8 hours FROM BURN TIME, then 50% evenly over the next 16 hours. Preferred fluid: Ringer Lactate (RL).",
            commonTrap = "NORCET loves adding a 2-hour pre-hospital delay—always subtract elapsed pre-hospital hours from the initial 8-hour clock!",
            deeperLearning = "Urine output is the most reliable bedside indicator of adequate burn resuscitation. Also watch for hyperkalemia in the emergent phase (first 24–48 hrs) due to massive cellular lysis, followed by hypokalemia during the diuretic phase."
        ),
        DailyFeaturedQuestion(
            id = "QOTD_11",
            topicTag = "Tracheostomy & Suctioning",
            subjectCode = "FON",
            difficulty = QuestionDifficulty.EASY,
            examTag = "RRB · DSSSB · NORCET",
            questionText = "Before performing open endotracheal suctioning on a mechanically ventilated ICU patient with thick secretions, which nursing action is MOST important to prevent suction-induced hypoxemia and cardiac arrhythmias?",
            options = listOf(
                "Instill 10 mL of sterile Normal Saline down the ET tube before every pass",
                "Hyperoxygenate with 100% FiO2 for 30–60 seconds and limit suction to 10–15 seconds on withdrawal only",
                "Apply continuous wall suction while inserting the catheter until resistance is felt",
                "Set wall vacuum suction pressure to 220 mmHg for rapid clearance"
            ),
            correctIndex = 1,
            conciseSummary = "Pre-oxygenation with 100% O2, gentle insertion without suction, and limiting suction to 10–15 seconds strictly during withdrawal prevents hypoxia and vagal bradycardia.",
            optionRationales = listOf(
                QotdOptionRationale(
                    0,
                    "Routine normal saline instillation is evidence-discouraged because it dislodges biofilm, increases VAP risk, and lowers SpO2."
                ),
                QotdOptionRationale(
                    1,
                    "Hyperoxygenation builds an oxygen reserve, while limiting suction to 10–15 seconds at 80–120 mmHg on withdrawal prevents mucosal trauma, atelectasis, and vagal bradycardia."
                ),
                QotdOptionRationale(
                    2,
                    "Applying suction during insertion strips oxygen from the airway and causes severe tracheal mucosal trauma."
                ),
                QotdOptionRationale(
                    3,
                    "Excessive vacuum pressure (> 150 mmHg in adults) causes tracheal bleeding and alveolar collapse.",
                    "Safe adult suction pressure: 80–120 mmHg (max 150 mmHg); infants 60–80 mmHg."
                )
            ),
            nursingPriority = "Assess SpO2 & ECG continuously; stop suctioning immediately and re-oxygenate if heart rate drops > 20 bpm, arrhythmias appear, or SpO2 falls below 90%.",
            examPearl = "Catheter outer diameter must NEVER exceed 50% of the internal diameter of the ET/tracheostomy tube.",
            commonTrap = "Remember: Never suction routinely on a fixed schedule—suction only when clinically indicated (coarse rhonchi, visible secretions, rising peak airway pressure).",
            deeperLearning = "Allow at least 20–30 seconds of recovery/re-oxygenation between passes, and limit total passes to 2–3 per suctioning episode whenever possible."
        ),
        DailyFeaturedQuestion(
            id = "QOTD_12",
            topicTag = "Antepartum Hemorrhage · OBG",
            subjectCode = "OBG",
            difficulty = QuestionDifficulty.MEDIUM,
            examTag = "AIIMS NORCET · ESIC",
            questionText = "A multipara at 33 weeks gestation presents with sudden, PAINLESS, bright red vaginal bleeding. Her abdomen is soft, non-tender, and fetal heart tones are 144/min. Which nursing action is STRICTLY CONTRAINDICATED?",
            options = listOf(
                "Initiating continuous external fetal heart rate monitoring",
                "Establishing two large-bore IV cannulas and sending blood for crossmatch",
                "Performing a digital per-vaginal (PV) examination to check cervical dilation",
                "Preparing for transabdominal ultrasound to confirm placental localization"
            ),
            correctIndex = 2,
            conciseSummary = "Painless, bright-red third-trimester bleeding with a soft uterus is classic Placenta Previa—a digital PV exam can tear the low-lying placenta and cause catastrophic hemorrhage.",
            optionRationales = listOf(
                QotdOptionRationale(0, "Continuous external FHR monitoring is safe and essential to detect fetal hypoxia early."),
                QotdOptionRationale(1, "Two 16G–18G IV lines and blood grouping/crossmatch are immediate safety priorities in any antepartum hemorrhage."),
                QotdOptionRationale(
                    2,
                    "Digital vaginal examination is strictly forbidden until Placenta Previa is ruled out by ultrasound because touching the placental edge can trigger torrential hemorrhage."
                ),
                QotdOptionRationale(3, "Transabdominal ultrasound is the gold-standard non-invasive test to confirm placental position.")
            ),
            nursingPriority = "Post a 'NO PV EXAM' alert at bedside → left lateral rest → large-bore IV access & pad count → ultrasound localization → corticosteroids if < 34 weeks and stable.",
            examPearl = "Placenta Previa = Painless, bright red, soft uterus. Abruptio Placentae = Painful, dark red/concealed, rigid board-like tender uterus.",
            commonTrap = "Remember the bedside rhyme: 'Previa = Painless, Bright, Soft' vs 'Abruptio = Agony, Dark, Board-like'.",
            deeperLearning = "If expectant management is chosen for a preterm mother with Placenta Previa (Macafee & Johnson regimen), monitor hemoglobin, maintain pelvic rest, and administer Rh(D) immune globulin if Rh-negative."
        ),
        DailyFeaturedQuestion(
            id = "QOTD_13",
            topicTag = "Biomedical Waste · CHN",
            subjectCode = "CHN",
            difficulty = QuestionDifficulty.EASY,
            examTag = "RRB · ESIC · NORCET",
            questionText = "Under India's Biomedical Waste Management Rules (2016, amended), into which color-coded container should a nurse discard used IV tubing, a blood-free Foley catheter, and a plastic syringe (without needle)?",
            options = listOf(
                "Yellow non-chlorinated plastic bag",
                "Red non-chlorinated plastic bag",
                "White translucent puncture-proof container",
                "Blue cardboard box with blue marking"
            ),
            correctIndex = 1,
            conciseSummary = "Contaminated recyclable plastic waste (IV sets, catheters, vacutainers, syringes without needles, gloves) goes into the RED bag for autoclaving/shredding.",
            optionRationales = listOf(
                QotdOptionRationale(
                    0,
                    "Yellow bag is for human anatomical waste, soiled cotton/dressings, expired medicines, and blood bags—items destined for incineration.",
                    "Correct for blood-soaked gauze, placenta, or discarded cytotoxic drugs."
                ),
                QotdOptionRationale(
                    1,
                    "Red bag collects contaminated recyclable plastics (tubing, bottles, IV sets, catheters, syringes without needles, gloves) which are autoclaved and shredded—never incinerated."
                ),
                QotdOptionRationale(
                    2,
                    "White puncture-proof container is exclusively for metallic sharps (needles, scalpels, blades, fixed-needle syringes).",
                    "Correct for scalpel blades and detached needles."
                ),
                QotdOptionRationale(
                    3,
                    "Blue puncture-proof box is strictly for broken/discarded glassware and metallic body implants.",
                    "Correct for empty glass vials, ampoules, and orthopedic implants."
                )
            ),
            nursingPriority = "Segregate strictly at the point of generation—never recap used needles and never mix PVC plastics into the yellow incineration stream (prevents toxic dioxin release).",
            examPearl = "Quick BMW Map: Yellow = Anatomy/Cotton/Blood bags; Red = Recyclable Plastics/Rubber; White = Metal Sharps; Blue = Glassware & Metallic Implants.",
            commonTrap = "A syringe WITH a fixed needle (like an insulin syringe) goes into WHITE; a syringe WITHOUT needle goes into RED!",
            deeperLearning = "Pre-treatment of microbiology lab waste and blood bags must be done on-site using autoclaving or chemical disinfection (non-chlorinated) as per WHO/NACO guidelines before final disposal."
        ),
        DailyFeaturedQuestion(
            id = "QOTD_14",
            topicTag = "Increased ICP · Neuro Nursing",
            subjectCode = "MSN",
            difficulty = QuestionDifficulty.HARD,
            examTag = "AIIMS NORCET · PGIMER",
            questionText = "A head-injury patient in Neuro ICU develops systolic BP rising from 124 to 176 mmHg (diastolic 72 mmHg), heart rate falling from 84 to 50/min, and irregular Cheyne-Stokes respirations. Which complication does this triad signify, and what is the nurse's immediate positioning priority?",
            options = listOf(
                "Hypovolemic shock — place flat with legs elevated",
                "Cushing's triad of impending brain herniation — elevate HOB 30° with head strictly midline",
                "Autonomic dysreflexia — lay patient supine and check blood sugar",
                "Neurogenic shock — give IV Atropine and lower HOB to 0°"
            ),
            correctIndex = 1,
            conciseSummary = "Widening pulse pressure (rising SBP), bradycardia, and irregular respirations form Cushing's triad—a late, life-threatening sign of severely elevated ICP and brainstem herniation.",
            optionRationales = listOf(
                QotdOptionRationale(
                    0,
                    "Hypovolemic shock presents with hypotension and tachycardia—the exact opposite of Cushing's triad.",
                    "Classic pattern in acute hemorrhage."
                ),
                QotdOptionRationale(
                    1,
                    "Elevating the head of bed 30° with neutral midline neck alignment maximizes jugular venous outflow to reduce ICP while preparing osmotic therapy (IV Mannitol or 3% Hypertonic Saline)."
                ),
                QotdOptionRationale(
                    2,
                    "Autonomic dysreflexia occurs in spinal cord injury at/above T6 with severe hypertension and bradycardia, but requires sitting upright (90°) and checking for bladder/bowel distension, not lying supine.",
                    "Triggered by full bladder or fecal impaction in T6+ SCI."
                ),
                QotdOptionRationale(
                    3,
                    "Neurogenic shock causes hypotension + bradycardia (loss of sympathetic tone), not severe systolic hypertension.",
                    "Seen in acute upper thoracic/cervical spinal cord injury."
                )
            ),
            nursingPriority = "Keep HOB 30° & neck neutral midline → notify neurosurgeon STAT → ensure airway/prevent hypercapnia (PaCO2 35–40 mmHg) → prepare IV Mannitol 20% (use filtered tubing) or 3% NaCl.",
            examPearl = "Earliest sign of rising ICP = Change in Level of Consciousness (restlessness, confusion). Late sign = Cushing's triad.",
            commonTrap = "Avoid hip flexion > 90°, neck twisting, coughing, straining, or clustering painful nursing care—all spike intracranial pressure immediately.",
            deeperLearning = "Cerebral Perfusion Pressure (CPP) = MAP − ICP (normal target 60–70 mmHg). Never abruptly drop blood pressure in Cushing's reflex without neurosurgical guidance, as high MAP may be maintaining cerebral perfusion against elevated ICP."
        )
    )

    fun questionForDate(dateKey: String = StudyGoalLogic.todayDateKey()): DailyFeaturedQuestion {
        val seed = dateKey.trim().ifBlank { "2026-01-01" }
        var hash = 0
        for (ch in seed) {
            hash = (hash * 31) + ch.code
        }
        val idx = abs(hash) % ALL_QUESTIONS.size
        return ALL_QUESTIONS[idx]
    }

    fun serializeCompletion(state: QotdCompletionState): String {
        if (state.dateKey.isBlank() || state.questionId.isBlank() || state.selectedIndex < 0) return ""
        return "${state.dateKey}|${state.questionId}|${state.selectedIndex}|${if (state.isCorrect) 1 else 0}"
    }

    fun deserializeCompletion(raw: String?): QotdCompletionState {
        if (raw.isNullOrBlank()) return QotdCompletionState()
        val parts = raw.split("|")
        if (parts.size < 4) return QotdCompletionState()
        return QotdCompletionState(
            dateKey = parts[0].trim(),
            questionId = parts[1].trim(),
            selectedIndex = parts[2].toIntOrNull() ?: -1,
            isCorrect = parts[3] == "1"
        )
    }
}

// ============================================================================
// 4. HYBRID PER-OPTION RATIONALE ANALYZER (OFFLINE STRUCTURED CARDS)
// ============================================================================

object OptionRationaleAnalyzer {

    /**
     * Builds structured, readable per-option rationale items for any MCQ without hallucinating
     * unsupported medical facts.
     * - If curated per-option rationales are provided (e.g., QOTD), renders each option's exact
     *   clinical rationale and "When correct elsewhere" note.
     * - For existing bundled MCQs storing a single explanation block, cleanly pairs the
     *   authoritative rationale with the correct key and generates transparent distractor analysis
     *   cards (plus a 1-tap AI deep breakdown trigger in UI).
     */
    fun buildForQuestion(
        question: Question,
        selectedIndex: Int?,
        curatedRationales: List<QotdOptionRationale> = emptyList()
    ): List<OptionRationaleDetail> {
        val letters = listOf("A", "B", "C", "D", "E", "F")
        val curatedByIdx = curatedRationales.associateBy { it.optionIndex }
        val cleanExp = question.explanation.trim()
        val correctText = question.options.getOrNull(question.correctIndex).orEmpty()

        return question.options.mapIndexed { idx, optText ->
            val letter = letters.getOrElse(idx) { "${idx + 1}" }
            val isCorrect = idx == question.correctIndex
            val isPicked = selectedIndex != null && idx == selectedIndex
            val curated = curatedByIdx[idx]

            val rationaleText = when {
                curated != null -> curated.whyCorrectOrWrong
                isCorrect -> cleanExp.ifBlank {
                    "Option $letter ($optText) is the verified correct answer for this clinical scenario."
                }
                else -> {
                    val extracted = extractOptionMention(cleanExp, letter, optText)
                    when {
                        extracted != null -> extracted
                        isPicked -> "Not the priority answer here. Compare this choice against Option ${letters.getOrElse(question.correctIndex) { "?" }} ($correctText), or tap 'AI Option Breakdown' below for a detailed distractor comparison."
                        else -> "Incorrect distractor for this stem. The expected clinical choice is Option ${letters.getOrElse(question.correctIndex) { "?" }} ($correctText)."
                    }
                }
            }

            OptionRationaleDetail(
                optionIndex = idx,
                label = letter,
                optionText = optText,
                isCorrect = isCorrect,
                isUserSelected = isPicked,
                explanation = rationaleText,
                whenCorrectElsewhere = curated?.whenAppliesElsewhere.orEmpty()
            )
        }
    }

    /**
     * Checks if the question's existing explanation already contains a specific sentence explaining
     * a distractor (e.g., matching a distinctive keyword or explicit "Option B:" pattern).
     */
    internal fun extractOptionMention(explanation: String, letter: String, optionText: String): String? {
        if (explanation.isBlank()) return null
        val explicitPattern = Regex(
            "(?:Option\\s+$letter|$letter\\))\\s*[:\\-–]\\s*([^\\n]+)",
            RegexOption.IGNORE_CASE
        )
        explicitPattern.find(explanation)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.length >= 12 }?.let {
            return it
        }

        // Check sentence-level match for distinctive clinical terms (>5 chars) in the distractor
        val keyTerms = optionText
            .split(Regex("[^A-Za-z0-9]+"))
            .map { it.trim() }
            .filter { it.length >= 6 && it.lowercase(Locale.US) !in STOP_WORDS }
            .take(3)
        if (keyTerms.isEmpty()) return null

        val sentences = explanation.split(Regex("(?<=[.!?])\\s+"))
        for (sentence in sentences) {
            val lower = sentence.lowercase(Locale.US)
            if (keyTerms.any { lower.contains(it.lowercase(Locale.US)) } &&
                (lower.contains("not ") || lower.contains("unlike") || lower.contains("whereas") ||
                    lower.contains("instead") || lower.contains("wrong") || lower.contains("contraindicated") ||
                    lower.contains("used for") || lower.contains("seen in"))
            ) {
                return sentence.trim()
            }
        }
        return null
    }

    private val STOP_WORDS = setOf(
        "patient", "nursing", "clinical", "normal", "should", "during", "before", "after",
        "within", "option", "answer", "correct", "always", "usually", "common", "disease"
    )
}
