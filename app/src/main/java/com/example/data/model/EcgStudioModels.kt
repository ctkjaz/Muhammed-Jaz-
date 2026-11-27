package com.example.data.model

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

enum class EcgCategory(val label: String) {
    SINUS_ATRIAL("Sinus & Atrial"),
    VENTRICULAR_ARREST("Ventricular & Arrest"),
    AV_BLOCKS("AV Blocks"),
    ISCHEMIA_ELECTROLYTES("Ischemia & Electrolytes")
}

enum class EcgUrgency(val label: String) {
    NORMAL("Baseline"),
    MONITOR("Monitor"),
    URGENT("Urgent"),
    STAT("STAT · Life-Threatening")
}

enum class EcgWaveformPattern {
    NSR,
    SINUS_BRADYCARDIA,
    SINUS_TACHYCARDIA,
    ATRIAL_FIBRILLATION,
    ATRIAL_FLUTTER,
    SVT,
    VENTRICULAR_TACHYCARDIA,
    VENTRICULAR_FIBRILLATION,
    TORSADES_DE_POINTES,
    FIRST_DEGREE_AV_BLOCK,
    SECOND_DEGREE_MOBITZ_I,
    THIRD_DEGREE_HEART_BLOCK,
    ACUTE_STEMI,
    HYPERKALEMIA
}

data class EcgNinePointBreakdown(
    val rate: String,
    val rhythm: String,
    val pWaves: String,
    val prInterval: String,
    val qrsComplex: String,
    val stSegment: String,
    val tWave: String,
    val qtInterval: String,
    val nursingPriority: String
)

data class EcgCaseItem(
    val id: String,
    val title: String,
    val shortLabel: String,
    val category: EcgCategory,
    val urgency: EcgUrgency,
    val pattern: EcgWaveformPattern,
    val displayBpm: String,
    val leadLabel: String,
    val clinicalCue: String,
    val examPearl: String,
    val breakdown: EcgNinePointBreakdown,
    val distractorTitles: List<String>
)

data class EcgQuizQuestion(
    val caseItem: EcgCaseItem,
    val options: List<String>,
    val correctIndex: Int
)

object EcgStudioCatalog {
    val ALL_CASES: List<EcgCaseItem> = listOf(
        EcgCaseItem(
            id = "ecg_nsr",
            title = "Normal Sinus Rhythm (NSR)",
            shortLabel = "NSR",
            category = EcgCategory.SINUS_ATRIAL,
            urgency = EcgUrgency.NORMAL,
            pattern = EcgWaveformPattern.NSR,
            displayBpm = "75 bpm",
            leadLabel = "Lead II · 25 mm/s · 10 mm/mV",
            clinicalCue = "32-year-old healthy adult during routine pre-employment cardiac screening. BP 118/76 mmHg, asymptomatic.",
            examPearl = "Every upright P wave in Lead II is followed by a narrow QRS (< 0.12 s / 3 small boxes) with constant PR 0.12–0.20 s.",
            breakdown = EcgNinePointBreakdown(
                rate = "75 bpm (Normal 60–100 bpm)",
                rhythm = "Regular R–R and P–P intervals (4 large boxes)",
                pWaves = "Upright, rounded, uniform before every QRS (1:1)",
                prInterval = "0.16 s (4 small boxes · Normal 0.12–0.20 s)",
                qrsComplex = "0.08 s (2 small boxes · Narrow < 0.12 s)",
                stSegment = "Isoelectric (flat on baseline)",
                tWave = "Upright, smooth, asymmetric in Lead II",
                qtInterval = "0.38 s (Normal < 0.44 s)",
                nursingPriority = "Document baseline rhythm; no intervention required."
            ),
            distractorTitles = listOf("Sinus Bradycardia", "First-Degree AV Block", "Atrial Fibrillation (AFib)")
        ),
        EcgCaseItem(
            id = "ecg_sbrady",
            title = "Sinus Bradycardia",
            shortLabel = "Sinus Brady",
            category = EcgCategory.SINUS_ATRIAL,
            urgency = EcgUrgency.MONITOR,
            pattern = EcgWaveformPattern.SINUS_BRADYCARDIA,
            displayBpm = "46 bpm",
            leadLabel = "Lead II · 25 mm/s · 10 mm/mV",
            clinicalCue = "68-year-old on beta-blockers complains of dizziness and cool extremities. BP 84/54 mmHg, HR 46/min.",
            examPearl = "Only treat SYMPTOMATIC bradycardia (hypotension, altered mental status, chest pain): First-line drug is IV Atropine 1 mg (ACLS).",
            breakdown = EcgNinePointBreakdown(
                rate = "46 bpm (< 60 bpm)",
                rhythm = "Regular R–R interval (~6.5 large boxes)",
                pWaves = "Normal upright P wave before every QRS",
                prInterval = "0.16 s (Constant & normal)",
                qrsComplex = "0.08 s (Narrow)",
                stSegment = "Isoelectric",
                tWave = "Normal upright",
                qtInterval = "0.42 s (Within normal for slow rate)",
                nursingPriority = "Assess hemodynamic stability! If symptomatic: IV Atropine 1 mg bolus, hold beta-blockers/digoxin, prepare transcutaneous pacing."
            ),
            distractorTitles = listOf("Third-Degree (Complete) Heart Block", "First-Degree AV Block", "Normal Sinus Rhythm (NSR)")
        ),
        EcgCaseItem(
            id = "ecg_stach",
            title = "Sinus Tachycardia",
            shortLabel = "Sinus Tach",
            category = EcgCategory.SINUS_ATRIAL,
            urgency = EcgUrgency.MONITOR,
            pattern = EcgWaveformPattern.SINUS_TACHYCARDIA,
            displayBpm = "128 bpm",
            leadLabel = "Lead II · 25 mm/s · 10 mm/mV",
            clinicalCue = "29-year-old post-appendectomy patient with fever 39.2°C and acute pain. BP 114/70 mmHg, SpO2 98%.",
            examPearl = "Sinus tachycardia is a physiologic response — NEVER shock it; treat the root cause (fever, hypovolemia, pain, hypoxemia, anemia).",
            breakdown = EcgNinePointBreakdown(
                rate = "128 bpm (100–150 bpm)",
                rhythm = "Regular R–R interval (~2.3 large boxes)",
                pWaves = "Distinct upright P wave precedes every QRS",
                prInterval = "0.14 s (Normal & constant)",
                qrsComplex = "0.08 s (Narrow)",
                stSegment = "Isoelectric",
                tWave = "Upright",
                qtInterval = "0.32 s (Shortens physiologically with faster rate)",
                nursingPriority = "Identify and treat underlying cause (antipyretics, IV fluids for hypovolemia, analgesia, oxygen)."
            ),
            distractorTitles = listOf("Supraventricular Tachycardia (SVT)", "Atrial Flutter (4:1)", "Ventricular Tachycardia (Monomorphic VT)")
        ),
        EcgCaseItem(
            id = "ecg_afib",
            title = "Atrial Fibrillation (AFib)",
            shortLabel = "AFib",
            category = EcgCategory.SINUS_ATRIAL,
            urgency = EcgUrgency.URGENT,
            pattern = EcgWaveformPattern.ATRIAL_FIBRILLATION,
            displayBpm = "~112 bpm",
            leadLabel = "Lead II · 25 mm/s · 10 mm/mV",
            clinicalCue = "71-year-old with rheumatic mitral stenosis reports palpitations and irregularly irregular radial pulse.",
            examPearl = "Hallmark triad: Irregularly irregular R–R intervals + absent P waves + undulating fibrillatory baseline. High thromboembolic stroke risk!",
            breakdown = EcgNinePointBreakdown(
                rate = "Ventricular ~112 bpm (Variable); Atrial > 350 bpm",
                rhythm = "Irregularly irregular R–R intervals",
                pWaves = "Absent — replaced by chaotic low-amplitude f-waves",
                prInterval = "Not measurable",
                qrsComplex = "0.08 s (Narrow)",
                stSegment = "Isoelectric (obscured slightly by f-waves)",
                tWave = "Upright where visible",
                qtInterval = "Variable with R–R",
                nursingPriority = "Check hemodynamic stability. Rate control (beta-blocker/diltiazem), anticoagulation (CHA2DS2-VASc) to prevent stroke; synchronized cardioversion if unstable."
            ),
            distractorTitles = listOf("Atrial Flutter (4:1)", "Supraventricular Tachycardia (SVT)", "Second-Degree AV Block (Mobitz I / Wenckebach)")
        ),
        EcgCaseItem(
            id = "ecg_aflutter",
            title = "Atrial Flutter (4:1)",
            shortLabel = "A-Flutter",
            category = EcgCategory.SINUS_ATRIAL,
            urgency = EcgUrgency.URGENT,
            pattern = EcgWaveformPattern.ATRIAL_FLUTTER,
            displayBpm = "75 bpm (A:300)",
            leadLabel = "Lead II · 25 mm/s · 10 mm/mV",
            clinicalCue = "64-year-old with COPD and ischemic heart disease displays a classic regular sawtooth baseline on telemetry.",
            examPearl = "Classic 'sawtooth' flutter (F) waves at 250–350/min in inferior leads (II, III, aVF). Like AFib, requires stroke anticoagulation assessment.",
            breakdown = EcgNinePointBreakdown(
                rate = "Atrial 300 bpm · Ventricular 75 bpm (4:1 conduction)",
                rhythm = "Regular ventricular rhythm (fixed 4:1 block)",
                pWaves = "Absent — replaced by uniform sawtooth F-waves",
                prInterval = "Not measurable",
                qrsComplex = "0.08 s (Narrow)",
                stSegment = "Masked by continuous flutter waves",
                tWave = "Partially obscured by F-waves",
                qtInterval = "Difficult to isolate due to F-waves",
                nursingPriority = "Assess BP & symptoms; anticipate rate control (diltiazem/beta-blocker), anticoagulation evaluation, or synchronized cardioversion if unstable."
            ),
            distractorTitles = listOf("Atrial Fibrillation (AFib)", "Normal Sinus Rhythm (NSR)", "Supraventricular Tachycardia (SVT)")
        ),
        EcgCaseItem(
            id = "ecg_svt",
            title = "Supraventricular Tachycardia (SVT)",
            shortLabel = "SVT",
            category = EcgCategory.SINUS_ATRIAL,
            urgency = EcgUrgency.URGENT,
            pattern = EcgWaveformPattern.SVT,
            displayBpm = "185 bpm",
            leadLabel = "Lead II · 25 mm/s · 10 mm/mV",
            clinicalCue = "26-year-old develops sudden-onset pounding chest palpitations while studying. Alert, BP 112/74 mmHg, HR 185/min.",
            examPearl = "Stable regular narrow-complex SVT (> 150 bpm): 1st Vagal maneuvers (modified Valsalva), 2nd Rapid IV Adenosine 6 mg (then 12 mg) via large proximal vein + 20 mL saline flush!",
            breakdown = EcgNinePointBreakdown(
                rate = "185 bpm (Typically 150–250 bpm)",
                rhythm = "Strictly regular R–R intervals (~1.6 large boxes)",
                pWaves = "Buried in preceding T wave or absent",
                prInterval = "Not measurable",
                qrsComplex = "0.08 s (Narrow < 0.12 s)",
                stSegment = "Isoelectric or mild rate-related depression",
                tWave = "Upright, crowded close to next QRS",
                qtInterval = "Shortened due to extreme rate",
                nursingPriority = "If stable: vagal maneuver -> rapid IV Adenosine 6 mg push + saline flush (warn patient of brief chest tightness/asystole pause). If unstable: synchronized cardioversion."
            ),
            distractorTitles = listOf("Sinus Tachycardia", "Ventricular Tachycardia (Monomorphic VT)", "Atrial Fibrillation (AFib)")
        ),
        EcgCaseItem(
            id = "ecg_vtach",
            title = "Ventricular Tachycardia (Monomorphic VT)",
            shortLabel = "V-Tach",
            category = EcgCategory.VENTRICULAR_ARREST,
            urgency = EcgUrgency.STAT,
            pattern = EcgWaveformPattern.VENTRICULAR_TACHYCARDIA,
            displayBpm = "175 bpm",
            leadLabel = "Lead II · 25 mm/s · 10 mm/mV",
            clinicalCue = "66-year-old post-anterior MI suddenly develops wide rapid complexes on monitor. FIRST step: check patient responsiveness and carotid pulse!",
            examPearl = "Always check the pulse first! Pulseless VT = Immediate unsynchronized DEFIBRILLATION + CPR. Unstable VT with pulse = Synchronized cardioversion. Stable monomorphic VT = IV Amiodarone 150 mg.",
            breakdown = EcgNinePointBreakdown(
                rate = "175 bpm (150–250 bpm)",
                rhythm = "Regular wide-complex tachycardia",
                pWaves = "Absent or dissociated from QRS",
                prInterval = "None",
                qrsComplex = "0.16 s (Wide, bizarre > 0.12 s, uniform monomorphic)",
                stSegment = "Discordant (opposite polarity to QRS)",
                tWave = "Inverted / opposite to QRS deflection",
                qtInterval = "Prolonged / merged with wide complex",
                nursingPriority = "STAT check pulse & responsiveness! Pulseless VT -> Immediate CPR + Unsynchronized Defibrillation. Pulse present + unstable -> Synchronized Cardioversion."
            ),
            distractorTitles = listOf("Supraventricular Tachycardia (SVT)", "Ventricular Fibrillation (VFib)", "Torsades de Pointes")
        ),
        EcgCaseItem(
            id = "ecg_vfib",
            title = "Ventricular Fibrillation (VFib)",
            shortLabel = "V-Fib",
            category = EcgCategory.VENTRICULAR_ARREST,
            urgency = EcgUrgency.STAT,
            pattern = EcgWaveformPattern.VENTRICULAR_FIBRILLATION,
            displayBpm = "0 (No Pulse)",
            leadLabel = "Lead II · 25 mm/s · 10 mm/mV",
            clinicalCue = "59-year-old collapses in CCU — unresponsive, apneic, no palpable carotid pulse. Chaotic jagged waves on monitor.",
            examPearl = "VFib = Zero cardiac output! Immediate high-quality CPR + Unsynchronized Defibrillation (biphasic 120–200 J) -> Epinephrine 1 mg q3–5m -> Amiodarone 300 mg.",
            breakdown = EcgNinePointBreakdown(
                rate = "Indeterminate (chaotic > 300/min quivering)",
                rhythm = "Completely chaotic and irregular",
                pWaves = "Absent",
                prInterval = "None",
                qrsComplex = "Absent — no identifiable QRS complexes",
                stSegment = "Absent",
                tWave = "Absent",
                qtInterval = "Not measurable",
                nursingPriority = "CODE BLUE: Immediate high-quality chest compressions and unsynchronized defibrillation ASAP. Never delay shock to check leads if patient is pulseless!"
            ),
            distractorTitles = listOf("Torsades de Pointes", "Ventricular Tachycardia (Monomorphic VT)", "Atrial Fibrillation (AFib)")
        ),
        EcgCaseItem(
            id = "ecg_torsades",
            title = "Torsades de Pointes",
            shortLabel = "Torsades",
            category = EcgCategory.VENTRICULAR_ARREST,
            urgency = EcgUrgency.STAT,
            pattern = EcgWaveformPattern.TORSADES_DE_POINTES,
            displayBpm = "210 bpm",
            leadLabel = "Lead II · 25 mm/s · 10 mm/mV",
            clinicalCue = "54-year-old with hypomagnesemia and QT-prolonging IV Haloperidol suddenly has polymorphic twisting wide complexes.",
            examPearl = "Polymorphic VT 'twisting of the points' associated with prolonged QT. Drug of choice (NORCET classic): IV Magnesium Sulfate 1–2 g! Defibrillate immediately if pulseless.",
            breakdown = EcgNinePointBreakdown(
                rate = "200–250 bpm",
                rhythm = "Irregular polymorphic spindle-shaped waxing/waning amplitude",
                pWaves = "Absent",
                prInterval = "None",
                qrsComplex = "Wide (> 0.12 s), changing axis twisting around baseline",
                stSegment = "Indistinguishable",
                tWave = "Indistinguishable",
                qtInterval = "Underlying baseline QT is prolonged (> 0.46 s)",
                nursingPriority = "STAT check pulse: Defibrillate if pulseless/unstable. Administer IV Magnesium Sulfate 2 g, correct K+/Mg2+, stop QT-prolonging drugs."
            ),
            distractorTitles = listOf("Ventricular Fibrillation (VFib)", "Ventricular Tachycardia (Monomorphic VT)", "Atrial Fibrillation (AFib)")
        ),
        EcgCaseItem(
            id = "ecg_av1",
            title = "First-Degree AV Block",
            shortLabel = "1st° AVB",
            category = EcgCategory.AV_BLOCKS,
            urgency = EcgUrgency.MONITOR,
            pattern = EcgWaveformPattern.FIRST_DEGREE_AV_BLOCK,
            displayBpm = "70 bpm",
            leadLabel = "Lead II · 25 mm/s · 10 mm/mV",
            clinicalCue = "60-year-old taking Digoxin & Verapamil has asymptomatic delayed AV nodal conduction on routine telemetry.",
            examPearl = "Memory rhyme: 'If the R is far from P (> 0.20 s / 5 small boxes), then you have a First Degree!' Every P wave still conducts a QRS.",
            breakdown = EcgNinePointBreakdown(
                rate = "70 bpm (Normal)",
                rhythm = "Regular R–R intervals",
                pWaves = "Normal upright P wave before every QRS (1:1)",
                prInterval = "0.28 s (7 small boxes · Prolonged > 0.20 s, constant)",
                qrsComplex = "0.08 s (Narrow)",
                stSegment = "Isoelectric",
                tWave = "Normal upright",
                qtInterval = "0.38 s (Normal)",
                nursingPriority = "Usually benign/asymptomatic. Review AV-blocking medications (digoxin, beta-blockers, CCBs) and monitor for progression to higher-grade block."
            ),
            distractorTitles = listOf("Second-Degree AV Block (Mobitz I / Wenckebach)", "Normal Sinus Rhythm (NSR)", "Third-Degree (Complete) Heart Block")
        ),
        EcgCaseItem(
            id = "ecg_av2_m1",
            title = "Second-Degree AV Block (Mobitz I / Wenckebach)",
            shortLabel = "Mobitz I",
            category = EcgCategory.AV_BLOCKS,
            urgency = EcgUrgency.MONITOR,
            pattern = EcgWaveformPattern.SECOND_DEGREE_MOBITZ_I,
            displayBpm = "~62 bpm",
            leadLabel = "Lead II · 25 mm/s · 10 mm/mV",
            clinicalCue = "65-year-old with inferior wall MI shows progressive PR lengthening across 3 beats followed by a lonely P wave with no QRS.",
            examPearl = "Memory rhyme: 'Longer, longer, longer, DROP — then you have a Wenckebach!' Usually AV nodal and transient; rarely needs pacing unless symptomatic.",
            breakdown = EcgNinePointBreakdown(
                rate = "Atrial 78 bpm · Ventricular ~62 bpm (more P than QRS)",
                rhythm = "Regularly irregular (grouped beating pattern)",
                pWaves = "Normal upright P waves; 4th P wave fails to conduct",
                prInterval = "Progressively lengthens (0.18 s -> 0.26 s -> 0.34 s -> dropped QRS)",
                qrsComplex = "0.08 s (Narrow)",
                stSegment = "Isoelectric",
                tWave = "Upright",
                qtInterval = "Normal",
                nursingPriority = "Monitor closely and review AV nodal blocking drugs. If symptomatic bradycardia occurs: IV Atropine 1 mg."
            ),
            distractorTitles = listOf("First-Degree AV Block", "Third-Degree (Complete) Heart Block", "Atrial Fibrillation (AFib)")
        ),
        EcgCaseItem(
            id = "ecg_av3",
            title = "Third-Degree (Complete) Heart Block",
            shortLabel = "3rd° CHB",
            category = EcgCategory.AV_BLOCKS,
            urgency = EcgUrgency.STAT,
            pattern = EcgWaveformPattern.THIRD_DEGREE_HEART_BLOCK,
            displayBpm = "36 bpm (A:80)",
            leadLabel = "Lead II · 25 mm/s · 10 mm/mV",
            clinicalCue = "74-year-old presents with syncope (Stokes-Adams attack), BP 78/48 mmHg, and profound bradycardia at 36 bpm.",
            examPearl = "Memory rhyme: 'If Ps and Qs don't agree, then you have a Third Degree!' Complete AV dissociation — Atropine often fails; prepare IMMEDIATE Transcutaneous Pacing!",
            breakdown = EcgNinePointBreakdown(
                rate = "Atrial ~80 bpm · Ventricular escape ~36 bpm (independent)",
                rhythm = "Regular P–P and Regular R–R, but completely dissociated",
                pWaves = "Regular upright P waves marching right through QRS/T waves",
                prInterval = "Constantly varying with NO relationship to QRS",
                qrsComplex = "0.14 s (Wide ventricular escape rhythm)",
                stSegment = "Discordant to escape QRS",
                tWave = "Broad/discordant",
                qtInterval = "Variable",
                nursingPriority = "STAT: Apply transcutaneous pacing (TCP) pads immediately, establish IV access, notify cardiology for temporary/permanent pacemaker."
            ),
            distractorTitles = listOf("Sinus Bradycardia", "Second-Degree AV Block (Mobitz I / Wenckebach)", "First-Degree AV Block")
        ),
        EcgCaseItem(
            id = "ecg_stemi",
            title = "Acute STEMI (ST-Elevation MI)",
            shortLabel = "STEMI",
            category = EcgCategory.ISCHEMIA_ELECTROLYTES,
            urgency = EcgUrgency.STAT,
            pattern = EcgWaveformPattern.ACUTE_STEMI,
            displayBpm = "88 bpm",
            leadLabel = "Lead V3 · 25 mm/s · 10 mm/mV",
            clinicalCue = "58-year-old diabetic male with crushing retrosternal chest pain radiating to left arm, diaphoresis, and nausea for 40 minutes.",
            examPearl = "Convex 'tombstone' ST elevation ≥ 2 mm in precordial leads (≥ 1 mm in limb leads). Door-to-Balloon PCI goal < 90 minutes! Give chewable Aspirin 325 mg STAT.",
            breakdown = EcgNinePointBreakdown(
                rate = "88 bpm",
                rhythm = "Regular sinus rhythm",
                pWaves = "Normal upright P wave before every QRS",
                prInterval = "0.16 s (Normal)",
                qrsComplex = "0.09 s (Pathologic Q wave may develop later)",
                stSegment = "Marked convex ST elevation (+3.5 mm above J-point)",
                tWave = "Tall hyperacute T wave fusing with elevated ST",
                qtInterval = "0.40 s",
                nursingPriority = "STAT Cath Lab activation (PCI < 90 min), chewable Aspirin 325 mg, continuous ECG/defibrillator at bedside, O2 only if SpO2 < 90%, avoid nitrates if RV MI/hypotension."
            ),
            distractorTitles = listOf("Severe Hyperkalemia (K+ 7.2 mEq/L)", "Normal Sinus Rhythm (NSR)", "Ventricular Tachycardia (Monomorphic VT)")
        ),
        EcgCaseItem(
            id = "ecg_hyperk",
            title = "Severe Hyperkalemia (K+ 7.2 mEq/L)",
            shortLabel = "Hyperkalemia",
            category = EcgCategory.ISCHEMIA_ELECTROLYTES,
            urgency = EcgUrgency.STAT,
            pattern = EcgWaveformPattern.HYPERKALEMIA,
            displayBpm = "64 bpm",
            leadLabel = "Lead V4 · 25 mm/s · 10 mm/mV",
            clinicalCue = "61-year-old CKD patient missed 2 hemodialysis sessions; serum K+ returns 7.2 mEq/L with muscle weakness.",
            examPearl = "Progression: Tall peaked/tented T waves -> Flattened P & prolonged PR -> Widened QRS -> Sine wave -> Arrest. FIRST-LINE cardiac stabilizer: IV 10% Calcium Gluconate!",
            breakdown = EcgNinePointBreakdown(
                rate = "64 bpm",
                rhythm = "Regular (can deteriorate abruptly to VF/asystole)",
                pWaves = "Flattened / diminished amplitude",
                prInterval = "0.24 s (Prolonged)",
                qrsComplex = "0.14 s (Widening intraventricular delay)",
                stSegment = "Shortened, sloping sharply into peaked T",
                tWave = "Tall, narrow-based, symmetrical tented peaked T waves (+6 mm)",
                qtInterval = "Shortened initially",
                nursingPriority = "1st Protect myocardium: IV 10% Calcium Gluconate (does NOT lower K+). 2nd Shift K+ intracellularly: Regular Insulin + Dextrose, nebulized Albuterol. 3rd Eliminate K+: Dialysis/binders."
            ),
            distractorTitles = listOf("Acute STEMI (ST-Elevation MI)", "First-Degree AV Block", "Normal Sinus Rhythm (NSR)")
        )
    )

    fun casesForCategory(category: EcgCategory?): List<EcgCaseItem> =
        if (category == null) ALL_CASES else ALL_CASES.filter { it.category == category }

    fun buildPracticeDeck(): List<EcgQuizQuestion> {
        return ALL_CASES.mapIndexed { idx, item ->
            // Deterministic rotation of correct option slot (0..3) so answer position varies naturally
            val correctSlot = (idx * 3 + 1) % 4
            val distractors = item.distractorTitles.take(3)
            val opts = MutableList(4) { "" }
            opts[correctSlot] = item.title
            var dIdx = 0
            for (slot in 0 until 4) {
                if (slot != correctSlot) {
                    opts[slot] = distractors.getOrElse(dIdx++) { "Normal Sinus Rhythm (NSR)" }
                }
            }
            EcgQuizQuestion(
                caseItem = item,
                options = opts,
                correctIndex = correctSlot
            )
        }
    }
}

/**
 * Pure-Kotlin mathematical ECG signal synthesizer calibrated to standard clinical paper:
 * - Sweep duration default: 6.0 seconds (30 large boxes at 25 mm/s)
 * - Amplitude unit: millivolts (mV), where 1.0 mV = 10 mm (2 large boxes)
 */
object EcgWaveformEngine {
    const val DEFAULT_DURATION_SEC = 6.0
    const val DEFAULT_SAMPLE_COUNT = 720 // 120 samples/sec for crisp R-wave peaks on high-DPI displays

    private fun gauss(t: Double, center: Double, width: Double, amp: Double): Double {
        val d = (t - center) / width
        return amp * exp(-0.5 * d * d)
    }

    /**
     * Synthesizes a single cardiac cycle P-QRS-T complex centered at beatStartSec.
     */
    private fun pqrstBeat(
        localT: Double,
        includeP: Boolean = true,
        pAmp: Double = 0.14,
        prSec: Double = 0.16,
        qrsWidthScale: Double = 1.0,
        rAmp: Double = 1.15,
        sAmp: Double = -0.25,
        stElevationMv: Double = 0.0,
        tAmp: Double = 0.30,
        tWidth: Double = 0.048
    ): Double {
        var v = 0.0
        val pCenter = 0.06
        if (includeP) {
            v += gauss(localT, pCenter, 0.022, pAmp)
        }
        val qrsStart = pCenter + prSec
        val w = qrsWidthScale
        v += gauss(localT, qrsStart + 0.012 * w, 0.008 * w, -0.10) // Q
        v += gauss(localT, qrsStart + 0.032 * w, 0.011 * w, rAmp) // R
        v += gauss(localT, qrsStart + 0.054 * w, 0.010 * w, sAmp) // S

        // Optional ST segment elevation plateau (acute STEMI)
        if (stElevationMv != 0.0) {
            v += gauss(localT, qrsStart + 0.125, 0.055, stElevationMv)
        }

        // T wave
        val tCenter = qrsStart + 0.06 * w + 0.175
        v += gauss(localT, tCenter, tWidth, tAmp)
        return v
    }

    fun sampleStrip(
        pattern: EcgWaveformPattern,
        durationSec: Double = DEFAULT_DURATION_SEC,
        sampleCount: Int = DEFAULT_SAMPLE_COUNT
    ): FloatArray {
        val safeSamples = sampleCount.coerceIn(120, 2400)
        val out = FloatArray(safeSamples)
        val dt = durationSec / (safeSamples - 1).coerceAtLeast(1)

        // Precompute irregular R-R beat starts for AFib
        val afibBeats = doubleArrayOf(0.20, 0.72, 1.44, 1.88, 2.52, 3.04, 3.82, 4.30, 4.86, 5.56)

        for (i in 0 until safeSamples) {
            val t = i * dt
            val mv = when (pattern) {
                EcgWaveformPattern.NSR -> {
                    val period = 0.80 // 75 bpm
                    val phase = t % period
                    pqrstBeat(phase)
                }

                EcgWaveformPattern.SINUS_BRADYCARDIA -> {
                    val period = 1.30 // ~46 bpm
                    val phase = t % period
                    pqrstBeat(phase)
                }

                EcgWaveformPattern.SINUS_TACHYCARDIA -> {
                    val period = 0.468 // ~128 bpm
                    val phase = t % period
                    pqrstBeat(phase, prSec = 0.13, tWidth = 0.038)
                }

                EcgWaveformPattern.ATRIAL_FIBRILLATION -> {
                    // Chaotic low-amplitude fibrillatory baseline (f-waves)
                    var v = 0.05 * sin(2.0 * PI * 6.4 * t) +
                        0.035 * sin(2.0 * PI * 9.3 * t + 1.1) +
                        0.025 * sin(2.0 * PI * 13.7 * t + 2.4)
                    for (beatStart in afibBeats) {
                        val localT = t - beatStart
                        if (localT in -0.05..0.48) {
                            v += pqrstBeat(localT + 0.16, includeP = false, prSec = 0.12, rAmp = 1.10)
                        }
                    }
                    v
                }

                EcgWaveformPattern.ATRIAL_FLUTTER -> {
                    // Classic 300 bpm sawtooth F-waves (period 0.20s = 1 large box)
                    val fPeriod = 0.20
                    val fPhase = (t % fPeriod) / fPeriod
                    val sawtooth = -0.18 + 0.34 * (1.0 - fPhase) - 0.06 * sin(2.0 * PI * fPhase)
                    // 4:1 ventricular conduction (75 bpm, period 0.80s)
                    val vPhase = (t + 0.12) % 0.80
                    val qrs = pqrstBeat(vPhase, includeP = false, prSec = 0.10, rAmp = 1.12, tAmp = 0.10)
                    sawtooth + qrs
                }

                EcgWaveformPattern.SVT -> {
                    val period = 0.324 // 185 bpm
                    val phase = t % period
                    pqrstBeat(phase, includeP = false, prSec = 0.06, rAmp = 1.08, tAmp = 0.22, tWidth = 0.032)
                }

                EcgWaveformPattern.VENTRICULAR_TACHYCARDIA -> {
                    val period = 0.343 // 175 bpm
                    val phase = t % period
                    // Broad monomorphic bizarre QRS with discordant T
                    gauss(phase, 0.09, 0.034, 1.35) +
                        gauss(phase, 0.18, 0.036, -0.78) +
                        gauss(phase, 0.26, 0.035, -0.22)
                }

                EcgWaveformPattern.VENTRICULAR_FIBRILLATION -> {
                    // Chaotic multi-frequency coarse ventricular fibrillation
                    val env = 0.65 + 0.35 * sin(2.0 * PI * 0.42 * t + 0.5)
                    env * (
                        0.42 * sin(2.0 * PI * 4.3 * t) +
                            0.31 * sin(2.0 * PI * 6.7 * t + 1.3) +
                            0.22 * sin(2.0 * PI * 9.8 * t + 2.7) +
                            0.14 * sin(2.0 * PI * 2.9 * t + 0.8)
                        )
                }

                EcgWaveformPattern.TORSADES_DE_POINTES -> {
                    // Polymorphic spindle-modulated twisting QRS around isoelectric line
                    val carrier = sin(2.0 * PI * 3.55 * t) + 0.35 * sin(2.0 * PI * 7.1 * t + 0.6)
                    val envelope = 0.18 + 0.92 * kotlin.math.abs(sin(2.0 * PI * 0.38 * t + 0.25))
                    val drift = 0.18 * sin(2.0 * PI * 0.76 * t)
                    (carrier * envelope * 0.85) + drift
                }

                EcgWaveformPattern.FIRST_DEGREE_AV_BLOCK -> {
                    val period = 0.86 // 70 bpm
                    val phase = t % period
                    pqrstBeat(phase, prSec = 0.28)
                }

                EcgWaveformPattern.SECOND_DEGREE_MOBITZ_I -> {
                    // 4-beat Wenckebach cycle: 3 conducted beats with progressive PR (0.18 -> 0.26 -> 0.34), 4th P dropped!
                    val pPeriod = 0.76
                    val beatIdx = (t / pPeriod).toInt()
                    val phase = t - beatIdx * pPeriod
                    when (beatIdx % 4) {
                        0 -> pqrstBeat(phase, prSec = 0.18)
                        1 -> pqrstBeat(phase, prSec = 0.26)
                        2 -> pqrstBeat(phase, prSec = 0.34)
                        else -> gauss(phase, 0.06, 0.022, 0.15) // Lonely dropped P wave!
                    }
                }

                EcgWaveformPattern.THIRD_DEGREE_HEART_BLOCK -> {
                    // Independent Atrial P waves at 80 bpm (period 0.75s)
                    val pPhase = t % 0.75
                    val pWave = gauss(pPhase, 0.12, 0.022, 0.16)
                    // Independent slow wide Ventricular escape rhythm at ~36 bpm (period 1.66s)
                    val vPhase = (t + 0.35) % 1.66
                    val escapeQrs = gauss(vPhase, 0.22, 0.028, 1.05) +
                        gauss(vPhase, 0.29, 0.026, -0.45) +
                        gauss(vPhase, 0.48, 0.055, -0.32)
                    pWave + escapeQrs
                }

                EcgWaveformPattern.ACUTE_STEMI -> {
                    val period = 0.68 // 88 bpm
                    val phase = t % period
                    // Deep convex tombstone ST elevation (+0.38 mV = 3.8 mm)
                    pqrstBeat(
                        localT = phase,
                        prSec = 0.16,
                        rAmp = 1.05,
                        sAmp = -0.04,
                        stElevationMv = 0.36,
                        tAmp = 0.42,
                        tWidth = 0.055
                    )
                }

                EcgWaveformPattern.HYPERKALEMIA -> {
                    val period = 0.94 // 64 bpm
                    val phase = t % period
                    // Flattened P, prolonged PR 0.24s, wide QRS, tall narrow-based peaked tented T wave (+0.68 mV!)
                    pqrstBeat(
                        localT = phase,
                        pAmp = 0.05,
                        prSec = 0.24,
                        qrsWidthScale = 1.65,
                        rAmp = 0.95,
                        sAmp = -0.32,
                        tAmp = 0.72,
                        tWidth = 0.028
                    )
                }
            }
            out[i] = mv.toFloat().coerceIn(-1.6f, 1.8f)
        }
        return out
    }
}
