package com.example.data.model

import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

enum class AbgPrimaryDisorder(val label: String) {
    NORMAL("Normal Acid-Base Balance"),
    RESPIRATORY_ACIDOSIS("Respiratory Acidosis"),
    RESPIRATORY_ALKALOSIS("Respiratory Alkalosis"),
    METABOLIC_ACIDOSIS("Metabolic Acidosis"),
    METABOLIC_ALKALOSIS("Metabolic Alkalosis"),
    MIXED_ACIDOSIS("Mixed Respiratory & Metabolic Acidosis"),
    MIXED_ALKALOSIS("Mixed Respiratory & Metabolic Alkalosis"),
    MIXED_OPPOSING("Mixed Acid-Base Disorder (Opposing Processes)")
}

enum class AbgCompensationStatus(val label: String) {
    NORMAL("Normal"),
    UNCOMPENSATED("Uncompensated"),
    PARTIALLY_COMPENSATED("Partially Compensated"),
    FULLY_COMPENSATED("Fully Compensated"),
    MIXED_SUPERIMPOSED("Mixed / Superimposed Disorder")
}

enum class OxygenationStatus(val label: String, val isCritical: Boolean) {
    NORMAL("Normal Oxygenation (PaO2 80–100 mmHg)", false),
    HYPEROXIA("Hyperoxia (Excess Supplemental O2 > 100 mmHg)", false),
    MILD_HYPOXEMIA("Mild Hypoxemia (PaO2 60–79 mmHg)", false),
    MODERATE_HYPOXEMIA("Moderate Hypoxemia (PaO2 40–59 mmHg)", true),
    SEVERE_HYPOXEMIA("Severe Hypoxemia (PaO2 < 40 mmHg · STAT)", true),
    NOT_PROVIDED("PaO2 not entered", false)
}

enum class AbgPracticeTier(val label: String) {
    BASIC("Basic"),
    COMPENSATED("Compensated"),
    MIXED_ICU("Mixed & ICU")
}

data class AbgInput(
    val ph: Double,
    val paco2: Double,
    val hco3: Double,
    val pao2: Double? = null,
    val sao2: Double? = null,
    val sodium: Double? = null,
    val chloride: Double? = null
)

data class AbgAnionGapResult(
    val anionGap: Double,
    val isElevated: Boolean,
    val classification: String,
    val causesHint: String
)

data class AbgInterpretationResult(
    val primaryDisorder: AbgPrimaryDisorder,
    val compensationStatus: AbgCompensationStatus,
    val headline: String,
    val phAssessment: String,
    val compensationMath: String,
    val mixedDisorderNote: String?,
    val oxygenationStatus: OxygenationStatus,
    val oxygenationDetail: String,
    val anionGap: AbgAnionGapResult?,
    val stepByStepSummary: List<String>,
    val commonCauses: List<String>,
    val nursingPriorities: List<String>
)

data class AbgPracticeCase(
    val id: String,
    val tier: AbgPracticeTier,
    val title: String,
    val vignette: String,
    val input: AbgInput,
    val options: List<String>,
    val correctIndex: Int,
    val rationale: String
)

object AbgInterpreterLogic {
    const val PH_MIN = 6.50
    const val PH_MAX = 7.90
    const val PH_LOW = 7.35
    const val PH_HIGH = 7.45
    const val PH_MID = 7.40

    const val PACO2_LOW = 35.0
    const val PACO2_HIGH = 45.0
    const val PACO2_MID = 40.0

    const val HCO3_LOW = 22.0
    const val HCO3_HIGH = 26.0
    const val HCO3_MID = 24.0

    fun validateInput(input: AbgInput): String? {
        if (input.ph.isNaN() || input.ph < PH_MIN || input.ph > PH_MAX) {
            return "Enter a physiological pH between 6.50 and 7.90."
        }
        if (input.paco2.isNaN() || input.paco2 < 5.0 || input.paco2 > 150.0) {
            return "Enter PaCO2 between 5 and 150 mmHg."
        }
        if (input.hco3.isNaN() || input.hco3 < 2.0 || input.hco3 > 65.0) {
            return "Enter HCO3- between 2 and 65 mEq/L."
        }
        if (input.pao2 != null && (input.pao2.isNaN() || input.pao2 < 10.0 || input.pao2 > 650.0)) {
            return "Enter PaO2 between 10 and 650 mmHg."
        }
        if (input.sao2 != null && (input.sao2.isNaN() || input.sao2 < 20.0 || input.sao2 > 100.0)) {
            return "Enter SaO2 between 20% and 100%."
        }
        return null
    }

    fun interpret(input: AbgInput): AbgInterpretationResult {
        val ph = input.ph
        val co2 = input.paco2
        val hco3 = input.hco3

        val phLow = ph < PH_LOW
        val phHigh = ph > PH_HIGH
        val phNormal = !phLow && !phHigh

        val co2High = co2 > PACO2_HIGH
        val co2Low = co2 < PACO2_LOW
        val co2Normal = !co2High && !co2Low

        val hco3Low = hco3 < HCO3_LOW
        val hco3High = hco3 > HCO3_HIGH
        val hco3Normal = !hco3Low && !hco3High

        val phAssessment = when {
            phLow -> "Acidemia (pH ${fmt2(ph)} < 7.35)"
            phHigh -> "Alkalemia (pH ${fmt2(ph)} > 7.45)"
            else -> "Normal pH (${fmt2(ph)} within 7.35–7.45)"
        }

        // Determine Primary Disorder & Baseline Compensation
        val primary: AbgPrimaryDisorder
        var compStatus: AbgCompensationStatus
        var compMath: String
        var mixedNote: String? = null

        when {
            phNormal && co2Normal && hco3Normal -> {
                primary = AbgPrimaryDisorder.NORMAL
                compStatus = AbgCompensationStatus.NORMAL
                compMath = "pH, PaCO2 (35–45 mmHg), and HCO3- (22–26 mEq/L) are all within normal arterial reference ranges."
            }

            // Combined Acidosis: pH low, CO2 high, HCO3 low
            phLow && co2High && hco3Low -> {
                primary = AbgPrimaryDisorder.MIXED_ACIDOSIS
                compStatus = AbgCompensationStatus.MIXED_SUPERIMPOSED
                val wintersCenter = (1.5 * hco3) + 8.0
                compMath = "Both CO2 retention (PaCO2 ${fmt1(co2)}) and bicarbonate loss (HCO3- ${fmt1(hco3)}) drive acidemia. Winter's expected PaCO2 for metabolic acidosis alone is ${fmt1(wintersCenter - 2)}–${fmt1(wintersCenter + 2)} mmHg."
                mixedNote = "Dual primary acidosis detected: respiratory ventilatory failure plus metabolic acid accumulation."
            }

            // Combined Alkalosis: pH high, CO2 low, HCO3 high
            phHigh && co2Low && hco3High -> {
                primary = AbgPrimaryDisorder.MIXED_ALKALOSIS
                compStatus = AbgCompensationStatus.MIXED_SUPERIMPOSED
                compMath = "Both hyperventilation (PaCO2 ${fmt1(co2)}) and bicarbonate excess (HCO3- ${fmt1(hco3)}) elevate arterial pH simultaneously."
                mixedNote = "Dual primary alkalosis detected (e.g., hyperventilation + vomiting/diuretic therapy)."
            }

            // Acidemia driven by Respiratory Acidosis
            phLow && co2High -> {
                primary = AbgPrimaryDisorder.RESPIRATORY_ACIDOSIS
                val deltaCo2 = co2 - PACO2_MID
                val acuteHco3 = HCO3_MID + (0.1 * deltaCo2)
                val chronicHco3 = HCO3_MID + (0.35 * deltaCo2)
                compStatus = if (hco3Normal) AbgCompensationStatus.UNCOMPENSATED else AbgCompensationStatus.PARTIALLY_COMPENSATED
                compMath = "Acute expected HCO3- ≈ ${fmt1(acuteHco3)} ± 2 mEq/L; Chronic expected HCO3- ≈ ${fmt1(chronicHco3)} ± 2 mEq/L. Measured HCO3- is ${fmt1(hco3)} mEq/L."
                if (hco3 > chronicHco3 + 3.0) {
                    compStatus = AbgCompensationStatus.MIXED_SUPERIMPOSED
                    mixedNote = "Measured HCO3- exceeds chronic renal compensation — suspect superimposed Metabolic Alkalosis."
                } else if (abs(hco3 - chronicHco3) <= 2.5 && hco3High) {
                    mixedNote = "Renal HCO3- elevation matches chronic respiratory acidosis (e.g., COPD CO2 retainer)."
                }
            }

            // Acidemia driven by Metabolic Acidosis (apply Winter's formula!)
            phLow && hco3Low -> {
                primary = AbgPrimaryDisorder.METABOLIC_ACIDOSIS
                val wintersCenter = (1.5 * hco3) + 8.0
                val lowBound = wintersCenter - 2.0
                val highBound = wintersCenter + 2.0
                compMath = "Winter's Formula: Expected PaCO2 = (1.5 × ${fmt1(hco3)}) + 8 ± 2 = ${fmt1(lowBound)}–${fmt1(highBound)} mmHg. Measured PaCO2 = ${fmt1(co2)} mmHg."
                when {
                    co2 > highBound -> {
                        compStatus = AbgCompensationStatus.MIXED_SUPERIMPOSED
                        mixedNote = "PaCO2 (${fmt1(co2)}) is higher than Winter's range — concomitant Respiratory Acidosis (inadequate ventilatory compensation / fatigue)."
                    }
                    co2 < lowBound -> {
                        compStatus = AbgCompensationStatus.MIXED_SUPERIMPOSED
                        mixedNote = "PaCO2 (${fmt1(co2)}) is lower than Winter's range — concomitant Respiratory Alkalosis (e.g., salicylate toxicity or sepsis)."
                    }
                    co2Normal -> {
                        compStatus = AbgCompensationStatus.UNCOMPENSATED
                    }
                    else -> {
                        compStatus = AbgCompensationStatus.PARTIALLY_COMPENSATED
                    }
                }
            }

            // Alkalemia driven by Respiratory Alkalosis
            phHigh && co2Low -> {
                primary = AbgPrimaryDisorder.RESPIRATORY_ALKALOSIS
                val deltaCo2 = PACO2_MID - co2
                val acuteHco3 = (HCO3_MID - (0.2 * deltaCo2)).coerceAtLeast(8.0)
                val chronicHco3 = (HCO3_MID - (0.5 * deltaCo2)).coerceAtLeast(8.0)
                compStatus = if (hco3Normal) AbgCompensationStatus.UNCOMPENSATED else AbgCompensationStatus.PARTIALLY_COMPENSATED
                compMath = "Acute expected HCO3- ≈ ${fmt1(acuteHco3)} ± 2 mEq/L; Chronic expected HCO3- ≈ ${fmt1(chronicHco3)} ± 2 mEq/L. Measured HCO3- is ${fmt1(hco3)} mEq/L."
                if (hco3 < chronicHco3 - 3.0) {
                    compStatus = AbgCompensationStatus.MIXED_SUPERIMPOSED
                    mixedNote = "HCO3- is lower than chronic compensation predicts — suspect superimposed Metabolic Acidosis."
                }
            }

            // Alkalemia driven by Metabolic Alkalosis
            phHigh && hco3High -> {
                primary = AbgPrimaryDisorder.METABOLIC_ALKALOSIS
                val expectedCo2 = PACO2_MID + 0.7 * (hco3 - HCO3_MID)
                val lowBound = expectedCo2 - 2.0
                val highBound = expectedCo2 + 2.0
                compMath = "Expected PaCO2 = 40 + 0.7 × (${fmt1(hco3)} − 24) ± 2 = ${fmt1(lowBound)}–${fmt1(highBound)} mmHg. Measured PaCO2 = ${fmt1(co2)} mmHg."
                when {
                    co2Normal -> compStatus = AbgCompensationStatus.UNCOMPENSATED
                    co2 > highBound + 3.0 -> {
                        compStatus = AbgCompensationStatus.MIXED_SUPERIMPOSED
                        mixedNote = "PaCO2 exceeds expected respiratory compensation — suspect superimposed Respiratory Acidosis."
                    }
                    co2 < lowBound - 3.0 -> {
                        compStatus = AbgCompensationStatus.MIXED_SUPERIMPOSED
                        mixedNote = "PaCO2 is lower than expected — suspect superimposed Respiratory Alkalosis."
                    }
                    else -> compStatus = AbgCompensationStatus.PARTIALLY_COMPENSATED
                }
            }

            // Normal pH with BOTH PaCO2 & HCO3 abnormal in the SAME direction -> Fully Compensated (use 7.40 midpoint rule!)
            phNormal && co2High && hco3High -> {
                if (ph < PH_MID) {
                    primary = AbgPrimaryDisorder.RESPIRATORY_ACIDOSIS
                    compStatus = AbgCompensationStatus.FULLY_COMPENSATED
                    val chronicHco3 = HCO3_MID + 0.35 * (co2 - PACO2_MID)
                    compMath = "pH (${fmt2(ph)}) is normal but leans acidic (< 7.40), pointing to Fully Compensated Respiratory Acidosis (chronic expected HCO3- ≈ ${fmt1(chronicHco3)} mEq/L)."
                } else {
                    primary = AbgPrimaryDisorder.METABOLIC_ALKALOSIS
                    compStatus = AbgCompensationStatus.FULLY_COMPENSATED
                    val expectedCo2 = PACO2_MID + 0.7 * (hco3 - HCO3_MID)
                    compMath = "pH (${fmt2(ph)}) is normal but leans alkaline (≥ 7.40), pointing to Fully Compensated Metabolic Alkalosis (expected PaCO2 ≈ ${fmt1(expectedCo2)} mmHg)."
                }
            }

            phNormal && co2Low && hco3Low -> {
                if (ph < PH_MID) {
                    primary = AbgPrimaryDisorder.METABOLIC_ACIDOSIS
                    compStatus = AbgCompensationStatus.FULLY_COMPENSATED
                    val wintersCenter = (1.5 * hco3) + 8.0
                    compMath = "pH (${fmt2(ph)}) is normal but leans acidic (< 7.40), pointing to Fully Compensated Metabolic Acidosis. Winter's expected PaCO2 = ${fmt1(wintersCenter - 2)}–${fmt1(wintersCenter + 2)} mmHg."
                } else {
                    primary = AbgPrimaryDisorder.RESPIRATORY_ALKALOSIS
                    compStatus = AbgCompensationStatus.FULLY_COMPENSATED
                    val chronicHco3 = (HCO3_MID - 0.5 * (PACO2_MID - co2)).coerceAtLeast(8.0)
                    compMath = "pH (${fmt2(ph)}) is normal but leans alkaline (≥ 7.40), pointing to Fully Compensated Respiratory Alkalosis (chronic expected HCO3- ≈ ${fmt1(chronicHco3)} mEq/L)."
                }
            }

            // Edge cases: normal pH with opposing abnormalities or isolated shift -> Mixed opposing
            else -> {
                primary = AbgPrimaryDisorder.MIXED_OPPOSING
                compStatus = AbgCompensationStatus.MIXED_SUPERIMPOSED
                compMath = "pH (${fmt2(ph)}) sits near normal despite discordant PaCO2 (${fmt1(co2)}) and HCO3- (${fmt1(hco3)}) — check clinical history and Anion Gap for opposing mixed disorders."
                mixedNote = "Opposing acid-base processes can normalize pH while hiding serious underlying pathology."
            }
        }

        // Oxygenation evaluation
        val (oxyStatus, oxyDetail) = evaluateOxygenation(input.pao2, input.sao2)

        // Optional Anion Gap
        val agResult = if (input.sodium != null && input.chloride != null) {
            val ag = input.sodium - (input.chloride + hco3)
            val elevated = ag > 12.0
            AbgAnionGapResult(
                anionGap = ag,
                isElevated = elevated,
                classification = if (elevated) {
                    "High Anion Gap (${fmt1(ag)} mEq/L > 12)"
                } else {
                    "Normal Anion Gap (${fmt1(ag)} mEq/L · Normal 8–12)"
                },
                causesHint = if (elevated) {
                    "MUDPILES: Methanol, Uremia, DKA, Propylene glycol, Isoniazid/Iron, Lactic acidosis, Ethylene glycol, Salicylates."
                } else {
                    "HARDUP: Hyperalimentation, Acetazolamide, Renal Tubular Acidosis, Diarrhea, Ureteral diversion, Pancreatic fistula."
                }
            )
        } else {
            null
        }

        val headline = when {
            primary == AbgPrimaryDisorder.NORMAL -> "Normal Arterial Blood Gas"
            compStatus == AbgCompensationStatus.MIXED_SUPERIMPOSED &&
                (primary == AbgPrimaryDisorder.MIXED_ACIDOSIS || primary == AbgPrimaryDisorder.MIXED_ALKALOSIS || primary == AbgPrimaryDisorder.MIXED_OPPOSING) ->
                primary.label
            compStatus == AbgCompensationStatus.MIXED_SUPERIMPOSED ->
                "${primary.label} (with Mixed Component)"
            else -> "${compStatus.label} ${primary.label}"
        }

        val steps = buildList {
            add("1. pH Check: $phAssessment")
            add("2. Primary Driver: ${primary.label} (PaCO2 ${fmt1(co2)} mmHg, HCO3- ${fmt1(hco3)} mEq/L)")
            add("3. Compensation: ${compStatus.label} — $compMath")
            if (mixedNote != null) add("4. Clinical Flag: $mixedNote")
            if (input.pao2 != null) add("5. Oxygenation: $oxyDetail")
            if (agResult != null) add("6. Anion Gap: ${agResult.classification}")
        }

        return AbgInterpretationResult(
            primaryDisorder = primary,
            compensationStatus = compStatus,
            headline = headline,
            phAssessment = phAssessment,
            compensationMath = compMath,
            mixedDisorderNote = mixedNote,
            oxygenationStatus = oxyStatus,
            oxygenationDetail = oxyDetail,
            anionGap = agResult,
            stepByStepSummary = steps,
            commonCauses = causesFor(primary),
            nursingPriorities = nursingPrioritiesFor(primary, oxyStatus)
        )
    }

    private fun evaluateOxygenation(pao2: Double?, sao2: Double?): Pair<OxygenationStatus, String> {
        if (pao2 == null) {
            return if (sao2 != null) {
                val status = if (sao2 < 90.0) OxygenationStatus.MODERATE_HYPOXEMIA else OxygenationStatus.NORMAL
                status to "SaO2 ${sao2.roundToInt()}% (Normal 95–100%; enter PaO2 for full hypoxemia grading)."
            } else {
                OxygenationStatus.NOT_PROVIDED to "Enter PaO2 (80–100 mmHg) to evaluate arterial oxygenation."
            }
        }
        val satSuffix = sao2?.let { " · SaO2 ${it.roundToInt()}%" } ?: ""
        return when {
            pao2 < 40.0 -> OxygenationStatus.SEVERE_HYPOXEMIA to "PaO2 ${fmt1(pao2)} mmHg$satSuffix — Severe life-threatening hypoxemia; prepare immediate high-flow O2 / airway support."
            pao2 < 60.0 -> OxygenationStatus.MODERATE_HYPOXEMIA to "PaO2 ${fmt1(pao2)} mmHg$satSuffix — Moderate hypoxemia; escalate oxygen therapy and assess work of breathing."
            pao2 < 80.0 -> OxygenationStatus.MILD_HYPOXEMIA to "PaO2 ${fmt1(pao2)} mmHg$satSuffix — Mild hypoxemia; monitor SpO2 and titrate supplemental O2."
            pao2 <= 100.0 -> OxygenationStatus.NORMAL to "PaO2 ${fmt1(pao2)} mmHg$satSuffix — Normal arterial oxygenation."
            else -> OxygenationStatus.HYPEROXIA to "PaO2 ${fmt1(pao2)} mmHg$satSuffix — Hyperoxia; consider weaning FiO2 per ICU protocol to avoid oxygen toxicity."
        }
    }

    private fun causesFor(disorder: AbgPrimaryDisorder): List<String> = when (disorder) {
        AbgPrimaryDisorder.NORMAL -> listOf("Balanced ventilation and renal bicarbonate regulation.")
        AbgPrimaryDisorder.RESPIRATORY_ACIDOSIS -> listOf(
            "Hypoventilation: Opioid/sedative overdose, brainstem injury",
            "Airway/Lung disease: COPD exacerbation, severe asthma fatigue, pneumonia, ARDS",
            "Neuromuscular: Guillain-Barré syndrome, myasthenia gravis crisis"
        )
        AbgPrimaryDisorder.RESPIRATORY_ALKALOSIS -> listOf(
            "Hyperventilation: Acute anxiety/panic, severe pain, fever, sepsis",
            "Hypoxemic drive: Early pulmonary embolism, high altitude, pneumonia",
            "Iatrogenic: Excessive mechanical ventilator minute ventilation"
        )
        AbgPrimaryDisorder.METABOLIC_ACIDOSIS -> listOf(
            "High Anion Gap (MUDPILES): DKA, lactic acidosis (sepsis/shock), renal failure, salicylates",
            "Normal Anion Gap (HARDUP): Severe diarrhea, renal tubular acidosis, saline excess"
        )
        AbgPrimaryDisorder.METABOLIC_ALKALOSIS -> listOf(
            "GI acid loss: Persistent vomiting, continuous nasogastric (NG) suction",
            "Renal acid/K+ loss: Loop/thiazide diuretics, hyperaldosteronism",
            "Excess base intake: Over-administration of IV sodium bicarbonate or antacids"
        )
        AbgPrimaryDisorder.MIXED_ACIDOSIS -> listOf(
            "Cardiopulmonary arrest (hypoventilation + lactic acidosis)",
            "Severe COPD exacerbation complicated by sepsis or shock",
            "Respiratory muscle fatigue during severe DKA or toxin ingestion"
        )
        AbgPrimaryDisorder.MIXED_ALKALOSIS -> listOf(
            "Critically ill patient with NG suctioning + ventilator hyperventilation",
            "Cirrhosis/hepatic failure (hyperventilation) + loop diuretic therapy"
        )
        AbgPrimaryDisorder.MIXED_OPPOSING -> listOf(
            "Salicylate overdose (metabolic acidosis + central respiratory alkalosis)",
            "DKA with severe vomiting (metabolic acidosis + metabolic alkalosis)"
        )
    }

    private fun nursingPrioritiesFor(
        disorder: AbgPrimaryDisorder,
        oxy: OxygenationStatus
    ): List<String> = buildList {
        if (oxy.isCritical) {
            add("STAT Airway & Oxygenation: Apply high-flow O2, position semi-Fowler's, and notify physician for NIV/intubation readiness.")
        }
        when (disorder) {
            AbgPrimaryDisorder.RESPIRATORY_ACIDOSIS -> {
                add("Assess respiratory rate, depth, SpO2, and mental status (CO2 narcosis signs).")
                add("Improve ventilation: bronchodilators, suctioning, Naloxone if opioid-induced; titrate O2 carefully in COPD (target SpO2 88–92%).")
            }
            AbgPrimaryDisorder.RESPIRATORY_ALKALOSIS -> {
                add("Treat underlying trigger (oxygen for PE/hypoxemia, analgesics for pain, calm coached breathing for anxiety).")
                add("Monitor for hypocalcemia symptoms (circumoral numbness, carpopedal spasm) from reduced ionized Ca2+.")
            }
            AbgPrimaryDisorder.METABOLIC_ACIDOSIS -> {
                add("Monitor Kussmaul respirations, serum potassium (hyperkalemia shift), and continuous ECG.")
                add("Treat root cause: IV fluids + regular insulin for DKA; hemodynamic resuscitation for sepsis/lactic acidosis.")
            }
            AbgPrimaryDisorder.METABOLIC_ALKALOSIS -> {
                add("Check serum potassium and chloride; replace KCl and isotonic 0.9% NaCl as ordered.")
                add("Monitor for hypoventilation and neuromuscular irritability (Chvostek/Trousseau signs).")
            }
            AbgPrimaryDisorder.MIXED_ACIDOSIS -> {
                add("High-acuity ICU escalation: secure airway/ventilation AND rapidly correct shock/metabolic cause.")
                add("Repeat ABG in 15–30 minutes after intervention; monitor serum K+ and continuous cardiac rhythm.")
            }
            AbgPrimaryDisorder.MIXED_ALKALOSIS -> {
                add("Severe alkalemia (pH > 7.55) risks seizures and arrhythmias — review ventilator settings and stop unnecessary diuretics/suction.")
            }
            AbgPrimaryDisorder.MIXED_OPPOSING -> {
                add("Always calculate Anion Gap and correlate with history (toxins, vomiting, sepsis) — normal pH does not rule out life-threatening acidosis.")
            }
            AbgPrimaryDisorder.NORMAL -> {
                add("Continue routine respiratory and hemodynamic surveillance; correlate with bedside clinical status.")
            }
        }
    }

    private fun fmt1(v: Double): String = String.format(Locale.US, "%.1f", v)
    private fun fmt2(v: Double): String = String.format(Locale.US, "%.2f", v)
}

data class AbgPreset(
    val id: String,
    val label: String,
    val input: AbgInput
)

object AbgPracticeCatalog {
    val QUICK_PRESETS: List<AbgPreset> = listOf(
        AbgPreset("dka", "DKA (Winter's)", AbgInput(ph = 7.22, paco2 = 24.0, hco3 = 10.0, pao2 = 94.0, sodium = 138.0, chloride = 102.0)),
        AbgPreset("copd", "COPD Retainer", AbgInput(ph = 7.36, paco2 = 62.0, hco3 = 34.0, pao2 = 64.0)),
        AbgPreset("opioid", "Opioid Overdose", AbgInput(ph = 7.21, paco2 = 64.0, hco3 = 25.0, pao2 = 56.0)),
        AbgPreset("vomiting", "NG Suction", AbgInput(ph = 7.52, paco2 = 48.0, hco3 = 38.0, pao2 = 92.0)),
        AbgPreset("mixed_arrest", "Mixed Acidosis", AbgInput(ph = 7.12, paco2 = 58.0, hco3 = 16.0, pao2 = 48.0, sodium = 140.0, chloride = 100.0))
    )

    val ALL_CASES: List<AbgPracticeCase> = listOf(
        // BASIC (4 cases)
        AbgPracticeCase(
            id = "abg_b1",
            tier = AbgPracticeTier.BASIC,
            title = "Post-Op Opioid Sedation",
            vignette = "A 52-year-old post-operative patient receives IV morphine and develops RR 6/min with pinpoint pupils. ABG: pH 7.24, PaCO2 60 mmHg, HCO3- 25 mEq/L, PaO2 62 mmHg.",
            input = AbgInput(ph = 7.24, paco2 = 60.0, hco3 = 25.0, pao2 = 62.0),
            options = listOf(
                "Uncompensated Respiratory Acidosis with Mild Hypoxemia",
                "Partially Compensated Respiratory Acidosis",
                "Uncompensated Metabolic Acidosis",
                "Mixed Respiratory and Metabolic Acidosis"
            ),
            correctIndex = 0,
            rationale = "pH < 7.35 (acidemia) with elevated PaCO2 (60 mmHg) and normal HCO3- (25 mEq/L) indicates Acute Uncompensated Respiratory Acidosis. PaO2 62 mmHg indicates mild hypoxemia. Priority: airway support + IV Naloxone."
        ),
        AbgPracticeCase(
            id = "abg_b2",
            tier = AbgPracticeTier.BASIC,
            title = "Acute Anxiety Hyperventilation",
            vignette = "A 24-year-old nursing student presents to ER with panic, RR 34/min, and tingling around the lips. ABG: pH 7.53, PaCO2 24 mmHg, HCO3- 23 mEq/L, PaO2 99 mmHg.",
            input = AbgInput(ph = 7.53, paco2 = 24.0, hco3 = 23.0, pao2 = 99.0),
            options = listOf(
                "Uncompensated Metabolic Alkalosis",
                "Uncompensated Respiratory Alkalosis",
                "Fully Compensated Respiratory Alkalosis",
                "Partially Compensated Metabolic Acidosis"
            ),
            correctIndex = 1,
            rationale = "pH > 7.45 (alkalemia) driven by low PaCO2 (24 mmHg) with normal HCO3- (23 mEq/L) is classic Acute Uncompensated Respiratory Alkalosis. Circumoral paresthesia results from decreased ionized calcium."
        ),
        AbgPracticeCase(
            id = "abg_b3",
            tier = AbgPracticeTier.BASIC,
            title = "Early Severe Diarrhea",
            vignette = "A 38-year-old patient with acute cholera-like watery diarrhea arrives lethargic. ABG: pH 7.26, PaCO2 38 mmHg, HCO3- 16 mEq/L, PaO2 94 mmHg.",
            input = AbgInput(ph = 7.26, paco2 = 38.0, hco3 = 16.0, pao2 = 94.0),
            options = listOf(
                "Uncompensated Respiratory Acidosis",
                "Uncompensated Metabolic Acidosis",
                "Fully Compensated Metabolic Acidosis",
                "Uncompensated Metabolic Alkalosis"
            ),
            correctIndex = 1,
            rationale = "Low pH (7.26) with low HCO3- (16 mEq/L) and normal PaCO2 (38 mmHg) indicates Uncompensated Metabolic Acidosis caused by lower GI bicarbonate loss."
        ),
        AbgPracticeCase(
            id = "abg_b4",
            tier = AbgPracticeTier.BASIC,
            title = "Continuous NG Suctioning",
            vignette = "A patient with bowel obstruction has continuous nasogastric suction for 48 hours. ABG: pH 7.51, PaCO2 43 mmHg, HCO3- 34 mEq/L, PaO2 91 mmHg.",
            input = AbgInput(ph = 7.51, paco2 = 43.0, hco3 = 34.0, pao2 = 91.0),
            options = listOf(
                "Uncompensated Metabolic Alkalosis",
                "Partially Compensated Respiratory Alkalosis",
                "Fully Compensated Metabolic Alkalosis",
                "Mixed Alkalosis"
            ),
            correctIndex = 0,
            rationale = "High pH (7.51) with elevated HCO3- (34 mEq/L) and normal PaCO2 (43 mmHg) represents Uncompensated Metabolic Alkalosis from gastric HCl loss."
        ),

        // COMPENSATED (4 cases)
        AbgPracticeCase(
            id = "abg_c1",
            tier = AbgPracticeTier.COMPENSATED,
            title = "DKA with Kussmaul Respirations",
            vignette = "A 19-year-old with Type 1 DM has fruity breath and deep rapid breathing. ABG: pH 7.22, PaCO2 23 mmHg, HCO3- 10 mEq/L, PaO2 96 mmHg.",
            input = AbgInput(ph = 7.22, paco2 = 23.0, hco3 = 10.0, pao2 = 96.0),
            options = listOf(
                "Uncompensated Metabolic Acidosis",
                "Partially Compensated Metabolic Acidosis (Appropriate Winter's Response)",
                "Fully Compensated Metabolic Acidosis",
                "Mixed Metabolic Acidosis and Respiratory Acidosis"
            ),
            correctIndex = 1,
            rationale = "pH is still < 7.35, HCO3- is 10, and PaCO2 dropped to 23 mmHg. Winter's Formula: (1.5 × 10) + 8 ± 2 = 21–25 mmHg. Measured PaCO2 (23) fits Winter's range exactly -> Partially Compensated Metabolic Acidosis."
        ),
        AbgPracticeCase(
            id = "abg_c2",
            tier = AbgPracticeTier.COMPENSATED,
            title = "Chronic COPD CO2 Retainer",
            vignette = "A 68-year-old with severe stable COPD attends clinic. ABG: pH 7.37, PaCO2 60 mmHg, HCO3- 34 mEq/L, PaO2 66 mmHg.",
            input = AbgInput(ph = 7.37, paco2 = 60.0, hco3 = 34.0, pao2 = 66.0),
            options = listOf(
                "Fully Compensated Metabolic Alkalosis",
                "Partially Compensated Respiratory Acidosis",
                "Fully Compensated Respiratory Acidosis with Mild Hypoxemia",
                "Normal Arterial Blood Gas"
            ),
            correctIndex = 2,
            rationale = "pH 7.37 is within 7.35–7.45 (Fully Compensated) and leans acidic (< 7.40), identifying Respiratory Acidosis (PaCO2 60) as the primary disorder compensated by renal HCO3- retention (34). Target SpO2 in COPD: 88–92%."
        ),
        AbgPracticeCase(
            id = "abg_c3",
            tier = AbgPracticeTier.COMPENSATED,
            title = "Loop Diuretic Overuse",
            vignette = "A 62-year-old heart failure patient taking high-dose IV Furosemide has muscle cramps. ABG: pH 7.49, PaCO2 50 mmHg, HCO3- 37 mEq/L, PaO2 84 mmHg.",
            input = AbgInput(ph = 7.49, paco2 = 50.0, hco3 = 37.0, pao2 = 84.0),
            options = listOf(
                "Uncompensated Metabolic Alkalosis",
                "Partially Compensated Metabolic Alkalosis",
                "Fully Compensated Respiratory Acidosis",
                "Uncompensated Respiratory Acidosis"
            ),
            correctIndex = 1,
            rationale = "pH 7.49 (alkalemia) with high HCO3- (37) and compensatory hypoventilation (PaCO2 50; expected 40 + 0.7×13 = 49.1 ± 2) represents Partially Compensated Metabolic Alkalosis."
        ),
        AbgPracticeCase(
            id = "abg_c4",
            tier = AbgPracticeTier.COMPENSATED,
            title = "Chronic High-Altitude Acclimatization",
            vignette = "A trekker living at 4,200m altitude for 2 weeks undergoes ABG testing: pH 7.43, PaCO2 28 mmHg, HCO3- 18 mEq/L, PaO2 72 mmHg.",
            input = AbgInput(ph = 7.43, paco2 = 28.0, hco3 = 18.0, pao2 = 72.0),
            options = listOf(
                "Fully Compensated Respiratory Alkalosis",
                "Fully Compensated Metabolic Acidosis",
                "Uncompensated Respiratory Alkalosis",
                "Mixed Acidosis"
            ),
            correctIndex = 0,
            rationale = "pH 7.43 is normal (Fully Compensated) and leans alkaline (> 7.40), confirming primary Respiratory Alkalosis (PaCO2 28) compensated by chronic renal HCO3- excretion (expected 24 − 0.5×12 = 18 mEq/L)."
        ),

        // MIXED & ICU (4 cases)
        AbgPracticeCase(
            id = "abg_m1",
            tier = AbgPracticeTier.MIXED_ICU,
            title = "Post-Cardiac Arrest ROSC",
            vignette = "An ICU patient 10 minutes post-ROSC has weak respirations and lactate 8.2 mmol/L. ABG: pH 7.10, PaCO2 58 mmHg, HCO3- 16 mEq/L, PaO2 52 mmHg.",
            input = AbgInput(ph = 7.10, paco2 = 58.0, hco3 = 16.0, pao2 = 52.0),
            options = listOf(
                "Partially Compensated Metabolic Acidosis",
                "Uncompensated Respiratory Acidosis",
                "Mixed Respiratory and Metabolic Acidosis with Moderate Hypoxemia",
                "Fully Compensated Respiratory Acidosis"
            ),
            correctIndex = 2,
            rationale = "Both PaCO2 (58, high) and HCO3- (16, low) are moving in acid-producing directions, producing severe Mixed Respiratory & Metabolic Acidosis with Moderate Hypoxemia (PaO2 52 mmHg)."
        ),
        AbgPracticeCase(
            id = "abg_m2",
            tier = AbgPracticeTier.MIXED_ICU,
            title = "Aspirin (Salicylate) Toxicity",
            vignette = "A 30-year-old presents with tinnitus, tachypnea, and nausea after ingesting 40 aspirin tablets. ABG: pH 7.31, PaCO2 18 mmHg, HCO3- 12 mEq/L, Na+ 140, Cl- 104.",
            input = AbgInput(ph = 7.31, paco2 = 18.0, hco3 = 12.0, pao2 = 98.0, sodium = 140.0, chloride = 104.0),
            options = listOf(
                "Uncompensated Metabolic Acidosis",
                "High Anion Gap Metabolic Acidosis with Superimposed Respiratory Alkalosis",
                "Fully Compensated Respiratory Alkalosis",
                "Normal Anion Gap Metabolic Acidosis"
            ),
            correctIndex = 1,
            rationale = "Winter's expected PaCO2 for HCO3- 12 is (1.5×12)+8 ± 2 = 24–28 mmHg. Actual PaCO2 is 18 mmHg (lower than Winter's), revealing superimposed primary Respiratory Alkalosis! Anion Gap = 140 − (104+12) = 24 (HAGMA)."
        ),
        AbgPracticeCase(
            id = "abg_m3",
            tier = AbgPracticeTier.MIXED_ICU,
            title = "Ventilator Over-Ventilation + NG Drain",
            vignette = "A mechanically ventilated ICU patient with continuous NG suction has ABG: pH 7.58, PaCO2 29 mmHg, HCO3- 32 mEq/L, PaO2 118 mmHg.",
            input = AbgInput(ph = 7.58, paco2 = 29.0, hco3 = 32.0, pao2 = 118.0),
            options = listOf(
                "Uncompensated Metabolic Alkalosis",
                "Mixed Respiratory and Metabolic Alkalosis with Hyperoxia",
                "Partially Compensated Respiratory Alkalosis",
                "Fully Compensated Metabolic Alkalosis"
            ),
            correctIndex = 1,
            rationale = "Both low PaCO2 (29) and high HCO3- (32) drive severe alkalemia (pH 7.58 -> Mixed Alkalosis), while PaO2 118 mmHg indicates Hyperoxia — lower ventilator minute ventilation and FiO2 per orders."
        ),
        AbgPracticeCase(
            id = "abg_m4",
            tier = AbgPracticeTier.MIXED_ICU,
            title = "Impending Respiratory Fatigue in DKA",
            vignette = "A drowsy DKA patient's breathing slows from RR 32 to RR 10. ABG: pH 7.16, PaCO2 34 mmHg, HCO3- 12 mEq/L, PaO2 74 mmHg.",
            input = AbgInput(ph = 7.16, paco2 = 34.0, hco3 = 12.0, pao2 = 74.0),
            options = listOf(
                "Partially Compensated Metabolic Acidosis",
                "Metabolic Acidosis with Superimposed Respiratory Acidosis (Winter's Failure)",
                "Uncompensated Respiratory Acidosis",
                "Normal Acid-Base Balance"
            ),
            correctIndex = 1,
            rationale = "Even though PaCO2 34 looks 'low', Winter's Formula for HCO3- 12 demands PaCO2 24–28 mmHg! PaCO2 34 is dangerously higher than expected, signaling respiratory muscle fatigue (superimposed Respiratory Acidosis) requiring urgent airway readiness."
        )
    )

    fun casesForTier(tier: AbgPracticeTier?): List<AbgPracticeCase> =
        if (tier == null) ALL_CASES else ALL_CASES.filter { it.tier == tier }
}
