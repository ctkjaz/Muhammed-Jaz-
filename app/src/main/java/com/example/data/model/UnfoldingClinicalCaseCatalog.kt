package com.example.data.model

import androidx.compose.runtime.Immutable

enum class VitalDeltaDirection {
    STABLE,
    UP,
    DOWN,
    CRITICAL
}

@Immutable
data class VitalReading(
    val label: String,
    val value: String,
    val unit: String = "",
    val delta: String? = null,
    val direction: VitalDeltaDirection = VitalDeltaDirection.STABLE,
    val isCritical: Boolean = false
)

@Immutable
data class BedsideVitals(
    val hr: VitalReading,
    val bp: VitalReading,
    val spo2: VitalReading,
    val rr: VitalReading,
    val temp: VitalReading,
    val gcs: VitalReading,
    val keyClue: String
) {
    fun asTileList(): List<VitalReading> = listOf(hr, bp, spo2, rr, temp, gcs)
}

@Immutable
data class ClinicalCaseStage(
    val stageNumber: Int,
    val stageTitle: String,
    val elapsedTimeLabel: String,
    val clinicalUpdate: String,
    val vitals: BedsideVitals,
    val questionPrompt: String,
    val options: List<String>,
    val correctIndex: Int,
    val rationale: String,
    val nursingPearl: String
)

@Immutable
data class UnfoldingClinicalCase(
    val id: String,
    val title: String,
    val specialty: String,
    val examTag: String,
    val acuityBadge: String,
    val patientSummary: String,
    val estimatedMinutes: Int,
    val stages: List<ClinicalCaseStage>,
    val debriefTakeaways: List<String>
)

@Immutable
data class ClinicalCaseCompletionResult(
    val caseId: String,
    val correctStages: Int,
    val totalStages: Int,
    val earnedXp: Int,
    val isFirstTimeMastery: Boolean
)

/**
 * Pure-Kotlin catalog and scoring engine for 10 multi-step Unfolding NORCET Mains
 * bedside clinical cases (36 sequential decision stages total, 100% offline).
 */
object UnfoldingClinicalCaseCatalog {

    const val SPECIALTY_ALL = "All"

    val specialties: List<String> = listOf(
        SPECIALTY_ALL,
        "ICU & Sepsis",
        "Cardiology",
        "OBG",
        "Pediatrics",
        "Emergency"
    )

    fun calculateCompletionXp(
        correctStages: Int,
        totalStages: Int,
        alreadyCompleted: Boolean
    ): Int {
        if (totalStages <= 0 || correctStages <= 0) return 0
        if (alreadyCompleted) return 0 // Anti-farming: replays give zero duplicate XP
        val basePerCorrect = 15
        val perfectBonus = if (correctStages == totalStages) 25 else 0
        return (correctStages * basePerCorrect) + perfectBonus
    }

    fun filterBySpecialty(specialty: String): List<UnfoldingClinicalCase> {
        if (specialty == SPECIALTY_ALL || specialty.isBlank()) return allCases
        return allCases.filter { it.specialty.equals(specialty, ignoreCase = true) }
    }

    fun findById(caseId: String?): UnfoldingClinicalCase? =
        if (caseId.isNullOrBlank()) null else allCases.firstOrNull { it.id == caseId }

    val allCases: List<UnfoldingClinicalCase> = listOf(
        // CASE 1: Cardiology — Inferior Wall STEMI -> RV Infarction -> Complete Heart Block
        UnfoldingClinicalCase(
            id = "case_stemi_rv_chb",
            title = "Inferior STEMI & Right Ventricular Shock",
            specialty = "Cardiology",
            examTag = "NORCET Mains · High-Yield",
            acuityBadge = "CODE CRITICAL",
            patientSummary = "58M presenting with crushing retrosternal chest pain radiating to jaw × 45 min, diaphoresis, and nausea.",
            estimatedMinutes = 5,
            stages = listOf(
                ClinicalCaseStage(
                    stageNumber = 1,
                    stageTitle = "Triage & Initial ECG Recognition",
                    elapsedTimeLabel = "T+00 min",
                    clinicalUpdate = "12-lead ECG reveals 3 mm ST-segment elevation in leads II, III, and aVF with clear lung fields bilaterally and distended neck veins (JVD).",
                    vitals = BedsideVitals(
                        hr = VitalReading("HR", "54", "bpm", isCritical = true, direction = VitalDeltaDirection.DOWN),
                        bp = VitalReading("BP", "88/54", "mmHg", isCritical = true, direction = VitalDeltaDirection.CRITICAL),
                        spo2 = VitalReading("SpO2", "96", "%"),
                        rr = VitalReading("RR", "22", "/min"),
                        temp = VitalReading("Temp", "36.7", "°C"),
                        gcs = VitalReading("GCS", "15", "/15"),
                        keyClue = "ECG: ST↑ II, III, aVF · Clear lungs + JVD (suspect RV infarction)"
                    ),
                    questionPrompt = "The resident writes orders for chewable Aspirin 300 mg, sublingual Nitroglycerin 0.4 mg, and right-sided V4R ECG. Which order MUST the nurse QUESTION immediately?",
                    options = listOf(
                        "Chewable Aspirin 300 mg stat",
                        "Sublingual Nitroglycerin 0.4 mg for chest pain",
                        "Recording right-sided precordial lead V4R",
                        "Establishing two large-bore peripheral IV cannulas"
                    ),
                    correctIndex = 1,
                    rationale = "Inferior STEMI with hypotension, clear lungs, and JVD is classic for Right Ventricular (RV) infarction. RV output is strictly preload-dependent; venodilators like Nitroglycerin or Morphine can crash preload and trigger fatal cardiogenic shock.",
                    nursingPearl = "Always record V4R in inferior STEMI and HOLD nitrates/diuretics if systolic BP < 90 mmHg or RV infarct is suspected."
                ),
                ClinicalCaseStage(
                    stageNumber = 2,
                    stageTitle = "Hemodynamic Stabilization",
                    elapsedTimeLabel = "T+10 min",
                    clinicalUpdate = "Lead V4R confirms 2 mm ST elevation (acute RV infarction). Systolic BP drops further to 80/48 mmHg while lung fields remain completely clear on auscultation.",
                    vitals = BedsideVitals(
                        hr = VitalReading("HR", "50", "bpm", delta = "-4", direction = VitalDeltaDirection.DOWN, isCritical = true),
                        bp = VitalReading("BP", "80/48", "mmHg", delta = "-8", direction = VitalDeltaDirection.CRITICAL, isCritical = true),
                        spo2 = VitalReading("SpO2", "95", "%", delta = "-1"),
                        rr = VitalReading("RR", "24", "/min", delta = "+2", direction = VitalDeltaDirection.UP),
                        temp = VitalReading("Temp", "36.6", "°C"),
                        gcs = VitalReading("GCS", "14", "/15", delta = "-1", direction = VitalDeltaDirection.DOWN),
                        keyClue = "V4R ST↑ 2 mm · Lungs clear · Preload-dependent RV failure"
                    ),
                    questionPrompt = "While activating the Cath Lab for primary PCI, what is the FIRST-LINE bedside hemodynamic intervention?",
                    options = listOf(
                        "Administer IV Furosemide 40 mg to relieve JVD",
                        "Infuse cautious 250–500 mL bolus of 0.9% Normal Saline and reassess lung sounds & BP",
                        "Administer IV Metoprolol 5 mg slowly",
                        "Start IV Nitroprusside infusion"
                    ),
                    correctIndex = 1,
                    rationale = "Despite elevated JVD, a failing ischemic right ventricle requires adequate intravascular preload (cautious isotonic saline boluses) to pump blood forward to the left ventricle. Loop diuretics and beta-blockers worsen shock.",
                    nursingPearl = "RV infarction is the rare cardiac shock with JVD where cautious IV fluid boluses—not diuretics—save lives."
                ),
                ClinicalCaseStage(
                    stageNumber = 3,
                    stageTitle = "Acute Rhythm Deterioration",
                    elapsedTimeLabel = "T+22 min",
                    clinicalUpdate = "After 500 mL NS, BP rises briefly to 92/58 mmHg, then the cardiac monitor alarms. P waves march regularly at 90/min, wide QRS complexes march independently at 32/min, and the patient becomes dizzy.",
                    vitals = BedsideVitals(
                        hr = VitalReading("HR", "32", "bpm", delta = "-18", direction = VitalDeltaDirection.CRITICAL, isCritical = true),
                        bp = VitalReading("BP", "72/44", "mmHg", delta = "-20", direction = VitalDeltaDirection.CRITICAL, isCritical = true),
                        spo2 = VitalReading("SpO2", "93", "%", delta = "-2", direction = VitalDeltaDirection.DOWN),
                        rr = VitalReading("RR", "26", "/min", delta = "+2", direction = VitalDeltaDirection.UP),
                        temp = VitalReading("Temp", "36.5", "°C"),
                        gcs = VitalReading("GCS", "12", "/15", delta = "-2", direction = VitalDeltaDirection.CRITICAL, isCritical = true),
                        keyClue = "Monitor: Complete AV Dissociation (3rd-Degree Heart Block) at 32 bpm"
                    ),
                    questionPrompt = "What is the PRIORITY nursing intervention for this symptomatic third-degree (complete) heart block?",
                    options = listOf(
                        "Apply transcutaneous pacing (TCP) pads immediately while preparing IV Atropine 1 mg as a bridge",
                        "Deliver unsynchronized defibrillation at 200 Joules",
                        "Administer IV Adenosine 6 mg rapid push",
                        "Perform carotid sinus massage"
                    ),
                    correctIndex = 0,
                    rationale = "Complete AV block with hypotension and altered mental status requires immediate Transcutaneous Pacing (TCP) per ACLS bradycardia guidelines (IV Atropine 1 mg may be tried briefly without delaying pacing).",
                    nursingPearl = "Defibrillation is for pulseless VF/pVT; symptomatic 3rd-degree block needs immediate TCP + confirmation of mechanical pulse capture."
                ),
                ClinicalCaseStage(
                    stageNumber = 4,
                    stageTitle = "Post-Pacing & Pre-PCI Verification",
                    elapsedTimeLabel = "T+32 min",
                    clinicalUpdate = "Transcutaneous pacing is set at 70 bpm with electrical spikes followed by wide QRS complexes. You palpate the femoral artery to verify perfusion.",
                    vitals = BedsideVitals(
                        hr = VitalReading("HR", "70", "paced", delta = "+38 ↑", direction = VitalDeltaDirection.UP),
                        bp = VitalReading("BP", "104/66", "mmHg", delta = "+32 ↑", direction = VitalDeltaDirection.UP),
                        spo2 = VitalReading("SpO2", "98", "%", delta = "+5 ↑", direction = VitalDeltaDirection.UP),
                        rr = VitalReading("RR", "18", "/min", delta = "-8 ↓", direction = VitalDeltaDirection.DOWN),
                        temp = VitalReading("Temp", "36.6", "°C"),
                        gcs = VitalReading("GCS", "15", "/15", delta = "+3 ↑", direction = VitalDeltaDirection.UP),
                        keyClue = "TCP active at 70 bpm · Femoral pulse 70/min matches paced spikes"
                    ),
                    questionPrompt = "Which bedside finding confirms TRUE mechanical capture of the transcutaneous pacemaker before wheeling to the Cath Lab?",
                    options = listOf(
                        "Visible pacer spikes on the ECG monitor alone",
                        "A palpable radial/femoral pulse matching the paced rate of 70/min with improved BP and mentation",
                        "Skeletal muscle twitching of the chest wall",
                        "Absence of chest pain on patient report"
                    ),
                    correctIndex = 1,
                    rationale = "Electrical capture (spike + wide QRS) must ALWAYS be confirmed with mechanical capture—palpating a pulse (femoral or right radial, away from chest wall artifact) matching the paced rate alongside improved perfusion.",
                    nursingPearl = "Verify electrical capture on screen AND mechanical capture by palpating the pulse; give IV analgesia/sedation for TCP discomfort."
                )
            ),
            debriefTakeaways = listOf(
                "Inferior STEMI (II, III, aVF) + hypotension + clear lungs + JVD = Right Ventricular Infarction -> obtain V4R and HOLD Nitroglycerin.",
                "First-line preload support in RV MI is cautious isotonic saline boluses, not diuretics.",
                "Symptomatic 3rd-degree AV block requires immediate Transcutaneous Pacing (TCP) with verified mechanical pulse capture."
            )
        ),

        // CASE 2: OBG — Severe Preeclampsia -> Eclampsia -> MgSO4 Toxicity
        UnfoldingClinicalCase(
            id = "case_severe_preeclampsia_mgso4",
            title = "Severe Preeclampsia & MgSO4 Toxicity Rescue",
            specialty = "OBG",
            examTag = "NORCET Mains · OBG Core",
            acuityBadge = "MATERNAL ALERT",
            patientSummary = "26F G1P0 at 37 weeks gestation admitted with severe frontal headache, scotomata (blurred vision), and epigastric pain.",
            estimatedMinutes = 5,
            stages = listOf(
                ClinicalCaseStage(
                    stageNumber = 1,
                    stageTitle = "Imminent Eclampsia Recognition",
                    elapsedTimeLabel = "T+00 min",
                    clinicalUpdate = "Urine dipstick shows 3+ proteinuria, deep tendon reflexes are 4+ with ankle clonus, and FHR baseline is 146 bpm.",
                    vitals = BedsideVitals(
                        hr = VitalReading("HR", "102", "bpm"),
                        bp = VitalReading("BP", "174/114", "mmHg", isCritical = true, direction = VitalDeltaDirection.CRITICAL),
                        spo2 = VitalReading("SpO2", "98", "%"),
                        rr = VitalReading("RR", "20", "/min"),
                        temp = VitalReading("Temp", "36.8", "°C"),
                        gcs = VitalReading("GCS", "15", "/15"),
                        keyClue = "DTR 4+ with clonus · Urine protein 3+ · Epigastric pain (RUQ)"
                    ),
                    questionPrompt = "What is the nurse's PRIORITY pharmacological preparation to prevent eclamptic seizures?",
                    options = listOf(
                        "IV Phenytoin loading dose 1,000 mg",
                        "IV Magnesium Sulfate loading dose (4 g IV slowly over 15–20 min per Pritchard/Zuspan protocol)",
                        "IV Diazepam 10 mg bolus",
                        "Oral Methyldopa 250 mg alone"
                    ),
                    correctIndex = 1,
                    rationale = "Magnesium Sulfate is the unequivocal drug of choice to prevent and control eclamptic seizures (superior to phenytoin and diazepam). Antihypertensives like IV Labetalol/Hydralazine lower BP >=160/110 concurrently.",
                    nursingPearl = "Always place 10% Calcium Gluconate at the bedside BEFORE starting a Magnesium Sulfate infusion."
                ),
                ClinicalCaseStage(
                    stageNumber = 2,
                    stageTitle = "Acute Tonic-Clonic Seizure",
                    elapsedTimeLabel = "T+08 min",
                    clinicalUpdate = "Before the IV loading dose finishes, the mother's eyes roll upward and she develops a generalized tonic-clonic seizure lasting 45 seconds.",
                    vitals = BedsideVitals(
                        hr = VitalReading("HR", "124", "bpm", delta = "+22 ↑", direction = VitalDeltaDirection.UP, isCritical = true),
                        bp = VitalReading("BP", "182/118", "mmHg", delta = "+8 ↑", direction = VitalDeltaDirection.CRITICAL, isCritical = true),
                        spo2 = VitalReading("SpO2", "88", "%", delta = "-10 ↓", direction = VitalDeltaDirection.CRITICAL, isCritical = true),
                        rr = VitalReading("RR", "8", "/min", delta = "-12 ↓", direction = VitalDeltaDirection.CRITICAL, isCritical = true),
                        temp = VitalReading("Temp", "37.0", "°C"),
                        gcs = VitalReading("GCS", "9", "/15", delta = "-6 ↓", direction = VitalDeltaDirection.CRITICAL, isCritical = true),
                        keyClue = "Active eclamptic fit · Transient fetal bradycardia 100 bpm"
                    ),
                    questionPrompt = "During the active eclamptic seizure, what is the nurse's IMMEDIATE bedside physical action?",
                    options = listOf(
                        "Force a padded tongue blade between her clenched teeth",
                        "Turn her onto her left lateral side, protect from injury, maintain airway/suction, and apply high-flow O2",
                        "Restrain her arms and legs firmly to stop tonic-clonic movements",
                        "Rush immediately for emergency cesarean section before stabilizing mother"
                    ),
                    correctIndex = 1,
                    rationale = "Left lateral positioning prevents aortocaval compression and aspiration; never force anything into the mouth or physically restrain limbs. Maternal stabilization (airway, O2, MgSO4) ALWAYS precedes delivery.",
                    nursingPearl = "Stabilize the mother first—post-seizure fetal bradycardia usually recovers once maternal oxygenation and perfusion restore."
                ),
                ClinicalCaseStage(
                    stageNumber = 3,
                    stageTitle = "Detecting Magnesium Sulfate Toxicity",
                    elapsedTimeLabel = "T+03 hr",
                    clinicalUpdate = "She is stable on maintenance MgSO4 2 g/hr IV. On hourly bedside surveillance, patellar reflexes are ABSENT, urine output over the past hour is 18 mL, and she is somnolent.",
                    vitals = BedsideVitals(
                        hr = VitalReading("HR", "78", "bpm", delta = "-46 ↓", direction = VitalDeltaDirection.DOWN),
                        bp = VitalReading("BP", "138/88", "mmHg", delta = "-44 ↓", direction = VitalDeltaDirection.DOWN),
                        spo2 = VitalReading("SpO2", "91", "%", delta = "-5 ↓", direction = VitalDeltaDirection.DOWN, isCritical = true),
                        rr = VitalReading("RR", "10", "/min", delta = "CRITICAL", direction = VitalDeltaDirection.CRITICAL, isCritical = true),
                        temp = VitalReading("Temp", "36.7", "°C"),
                        gcs = VitalReading("GCS", "11", "/15", isCritical = true, direction = VitalDeltaDirection.DOWN),
                        keyClue = "Absent patellar DTR · Urine output 18 mL/hr (<30) · RR 10/min (<12)"
                    ),
                    questionPrompt = "What are the nurse's IMMEDIATE priority actions?",
                    options = listOf(
                        "Reduce MgSO4 rate by half and recheck in 1 hour",
                        "STOP the MgSO4 infusion immediately, notify the obstetrician, and administer 10% Calcium Gluconate 1 g (10 mL) IV slowly over 3–5 min",
                        "Administer IV Furosemide 40 mg and continue MgSO4",
                        "Administer IV Naloxone 0.4 mg stat"
                    ),
                    correctIndex = 1,
                    rationale = "MgSO4 is excreted exclusively by the kidneys; oliguria (<30 mL/hr) causes rapid toxicity: loss of patellar reflex (earliest sign) -> respiratory depression (<12/min) -> cardiac arrest. Stop MgSO4 immediately and give 10% Calcium Gluconate IV slowly.",
                    nursingPearl = "MgSO4 safety triad before every dose: Patellar reflex PRESENT, RR >= 12/min, Urine output >= 30 mL/hr (100 mL/4 hr)."
                ),
                ClinicalCaseStage(
                    stageNumber = 4,
                    stageTitle = "Post-Antidote Recovery & Surveillance",
                    elapsedTimeLabel = "T+03 hr 20 min",
                    clinicalUpdate = "Ten minutes after IV 10% Calcium Gluconate and stopping MgSO4, respiratory effort strengthens and serum magnesium returns at 9.2 mg/dL (therapeutic 4.8–8.4 mg/dL).",
                    vitals = BedsideVitals(
                        hr = VitalReading("HR", "84", "bpm", delta = "+6 ↑", direction = VitalDeltaDirection.UP),
                        bp = VitalReading("BP", "142/90", "mmHg"),
                        spo2 = VitalReading("SpO2", "98", "%", delta = "+7 ↑", direction = VitalDeltaDirection.UP),
                        rr = VitalReading("RR", "16", "/min", delta = "+6 ↑", direction = VitalDeltaDirection.UP),
                        temp = VitalReading("Temp", "36.7", "°C"),
                        gcs = VitalReading("GCS", "15", "/15", delta = "+4 ↑", direction = VitalDeltaDirection.UP),
                        keyClue = "Serum Mg2+ 9.2 mg/dL · Patellar reflexes returning (1+) · FHR 140 bpm"
                    ),
                    questionPrompt = "Which laboratory abnormality should the nurse also scrutinize closely for HELLP syndrome complication in this patient?",
                    options = listOf(
                        "Elevated serum amylase and lipase",
                        "Hemolysis, elevated liver enzymes (AST/ALT), and platelets < 100,000/µL",
                        "Isolated hypercalcemia and polycythemia",
                        "Elevated thyroid-stimulating hormone (TSH)"
                    ),
                    correctIndex = 1,
                    rationale = "Epigastric/RUQ pain in severe preeclampsia is a hallmark warning of hepatic capsule stretching in HELLP syndrome (Hemolysis, Elevated Liver enzymes, Low Platelets <100,000/µL).",
                    nursingPearl = "Epigastric pain + severe preeclampsia = rule out HELLP syndrome immediately (CBC/platelets, LFTs, LDH)."
                )
            ),
            debriefTakeaways = listOf(
                "MgSO4 is the first-line anticonvulsant for eclampsia; keep 10% Calcium Gluconate at bedside.",
                "Monitor the MgSO4 safety triad hourly: Reflexes present, RR >= 12/min, Urine output >= 30 mL/hr.",
                "Loss of patellar reflex is the earliest clinical sign of Magnesium toxicity."
            )
        ),

        // CASE 3: ICU & Sepsis — Urosepsis -> Septic Shock -> Norepinephrine Extravasation
        UnfoldingClinicalCase(
            id = "case_septic_shock_bundle",
            title = "Septic Shock Hour-1 Bundle & Vasopressor Safety",
            specialty = "ICU & Sepsis",
            examTag = "NORCET Mains · ICU Protocol",
            acuityBadge = "SEPSIS HOUR-1",
            patientSummary = "71F with indwelling Foley catheter admitted from ward with rigors, confusion, cloudy foul-smelling urine, and flushed warm skin.",
            estimatedMinutes = 5,
            stages = listOf(
                ClinicalCaseStage(
                    stageNumber = 1,
                    stageTitle = "Surviving Sepsis Hour-1 Sequence",
                    elapsedTimeLabel = "T+00 min",
                    clinicalUpdate = "Serum lactate returns at 4.6 mmol/L, WBC 21,400/µL. Physician orders blood cultures × 2 sets, IV Piperacillin-Tazobactam, and 30 mL/kg crystalloid bolus.",
                    vitals = BedsideVitals(
                        hr = VitalReading("HR", "126", "bpm", isCritical = true, direction = VitalDeltaDirection.UP),
                        bp = VitalReading("BP", "82/46", "mmHg", isCritical = true, direction = VitalDeltaDirection.CRITICAL),
                        spo2 = VitalReading("SpO2", "94", "%"),
                        rr = VitalReading("RR", "28", "/min", isCritical = true, direction = VitalDeltaDirection.UP),
                        temp = VitalReading("Temp", "39.4", "°C", isCritical = true, direction = VitalDeltaDirection.UP),
                        gcs = VitalReading("GCS", "13", "/15", isCritical = true, direction = VitalDeltaDirection.DOWN),
                        keyClue = "MAP 58 mmHg · Serum Lactate 4.6 mmol/L · Warm vasodilatory shock"
                    ),
                    questionPrompt = "What is the CORRECT nursing sequence for executing the Surviving Sepsis Hour-1 Bundle?",
                    options = listOf(
                        "Infuse broad-spectrum IV antibiotics first, then draw blood cultures 2 hours later",
                        "Draw blood cultures (2 sets) FIRST without delaying therapy, then immediately administer IV broad-spectrum antibiotics and 30 mL/kg crystalloid",
                        "Wait for final urine culture sensitivity (48 hours) before giving antibiotics",
                        "Give oral paracetamol first and observe BP for 1 hour"
                    ),
                    correctIndex = 1,
                    rationale = "Hour-1 Sepsis Bundle: Measure lactate, obtain blood cultures BEFORE antimicrobials (without causing >45 min delay), administer broad-spectrum IV antibiotics immediately, and infuse 30 mL/kg isotonic crystalloid for hypotension or lactate >=4 mmol/L.",
                    nursingPearl = "Cultures BEFORE antibiotics, fluids 30 mL/kg for MAP <65 or Lactate >=4, and remeasure lactate if initially >2 mmol/L."
                ),
                ClinicalCaseStage(
                    stageNumber = 2,
                    stageTitle = "Fluid-Refractory Shock & Vasopressor Choice",
                    elapsedTimeLabel = "T+45 min",
                    clinicalUpdate = "After 1,800 mL (30 mL/kg) Ringer's Lactate, BP remains 78/44 mmHg (MAP 55 mmHg) and urine output is only 12 mL/hr.",
                    vitals = BedsideVitals(
                        hr = VitalReading("HR", "130", "bpm", delta = "+4 ↑", direction = VitalDeltaDirection.UP, isCritical = true),
                        bp = VitalReading("BP", "78/44", "mmHg", delta = "MAP 55", direction = VitalDeltaDirection.CRITICAL, isCritical = true),
                        spo2 = VitalReading("SpO2", "95", "%", delta = "+1"),
                        rr = VitalReading("RR", "26", "/min", delta = "-2"),
                        temp = VitalReading("Temp", "39.1", "°C", delta = "-0.3"),
                        gcs = VitalReading("GCS", "13", "/15"),
                        keyClue = "Refractory hypotension post-30 mL/kg bolus · MAP 55 (<65 mmHg)"
                    ),
                    questionPrompt = "Which FIRST-LINE vasopressor should the nurse initiate and what is the target Mean Arterial Pressure (MAP)?",
                    options = listOf(
                        "Dopamine targeting MAP >= 85 mmHg",
                        "Norepinephrine (Noradrenaline) targeting MAP >= 65 mmHg",
                        "Phenylephrine targeting MAP >= 50 mmHg",
                        "Dobutamine alone targeting HR > 140 bpm"
                    ),
                    correctIndex = 1,
                    rationale = "Norepinephrine is the undisputed first-line vasopressor in septic shock per Surviving Sepsis Campaign guidelines, titrated to maintain MAP >= 65 mmHg to preserve renal and cerebral perfusion.",
                    nursingPearl = "Septic shock first-line pressor = Norepinephrine; initial hemodynamic target = MAP >= 65 mmHg."
                ),
                ClinicalCaseStage(
                    stageNumber = 3,
                    stageTitle = "Peripheral Vasopressor Extravasation Emergency",
                    elapsedTimeLabel = "T+75 min",
                    clinicalUpdate = "While awaiting central line insertion, Norepinephrine infuses through a proximal 18G forearm cannula. On inspection, the skin around the IV site is blanched, pale, cool, and indurated.",
                    vitals = BedsideVitals(
                        hr = VitalReading("HR", "108", "bpm", delta = "-22 ↓", direction = VitalDeltaDirection.DOWN),
                        bp = VitalReading("BP", "102/60", "mmHg", delta = "MAP 74 ↑", direction = VitalDeltaDirection.UP),
                        spo2 = VitalReading("SpO2", "97", "%", delta = "+2 ↑", direction = VitalDeltaDirection.UP),
                        rr = VitalReading("RR", "22", "/min", delta = "-4 ↓", direction = VitalDeltaDirection.DOWN),
                        temp = VitalReading("Temp", "38.5", "°C", delta = "-0.6 ↓", direction = VitalDeltaDirection.DOWN),
                        gcs = VitalReading("GCS", "14", "/15", delta = "+1 ↑", direction = VitalDeltaDirection.UP),
                        keyClue = "Forearm IV site: Blanched, pale, cold, swollen (vasopressor extravasation)"
                    ),
                    questionPrompt = "After switching Norepinephrine to an alternate secure IV line and stopping the infiltrated line, which local antidote prevents tissue necrosis?",
                    options = listOf(
                        "Apply ice packs continuously and flush the infiltrated catheter with 20 mL NS",
                        "Aspirate residual drug, apply warm compresses, and infiltrate subcutaneous Phentolamine (5–10 mg in 10–15 mL NS) around the site",
                        "Infiltrate Protamine sulfate subcutaneously",
                        "Apply a tight elastic pressure bandage over the blanched area"
                    ),
                    correctIndex = 1,
                    rationale = "Phentolamine (an alpha-adrenergic antagonist) directly reverses alpha-1 vasoconstriction caused by norepinephrine extravasation. Never flush an extravasated vasopressor line or apply cold packs (cold worsens vasoconstriction/necrosis).",
                    nursingPearl = "Norepinephrine/Dopamine extravasation rescue: STOP infusion, aspirate, WARM compress, and local subcutaneous Phentolamine."
                ),
                ClinicalCaseStage(
                    stageNumber = 4,
                    stageTitle = "Resuscitation Response Evaluation",
                    elapsedTimeLabel = "T+03 hr",
                    clinicalUpdate = "Central line is placed, Norepinephrine maintains MAP 70 mmHg, and repeat labs return.",
                    vitals = BedsideVitals(
                        hr = VitalReading("HR", "94", "bpm", delta = "-14 ↓", direction = VitalDeltaDirection.DOWN),
                        bp = VitalReading("BP", "108/64", "mmHg", delta = "MAP 79 ↑", direction = VitalDeltaDirection.UP),
                        spo2 = VitalReading("SpO2", "98", "%", delta = "+1 ↑", direction = VitalDeltaDirection.UP),
                        rr = VitalReading("RR", "18", "/min", delta = "-4 ↓", direction = VitalDeltaDirection.DOWN),
                        temp = VitalReading("Temp", "37.8", "°C", delta = "-0.7 ↓", direction = VitalDeltaDirection.DOWN),
                        gcs = VitalReading("GCS", "15", "/15", delta = "+1 ↑", direction = VitalDeltaDirection.UP),
                        keyClue = "Repeat Lactate 1.9 mmol/L (down from 4.6) · Urine output 48 mL/hr"
                    ),
                    questionPrompt = "Which combined trend BEST confirms effective tissue perfusion recovery from septic shock?",
                    options = listOf(
                        "Rising serum lactate and decreasing urine output",
                        "Lactate clearance (4.6 → 1.9 mmol/L), urine output >= 0.5 mL/kg/hr, MAP >= 65 mmHg, and alert GCS 15",
                        "Isolated drop in temperature with persistent MAP 54 mmHg",
                        "Central venous pressure of 22 mmHg with bilateral crackles"
                    ),
                    correctIndex = 1,
                    rationale = "End-organ resuscitation goals include clearing serum lactate (<2 mmol/L), adequate renal perfusion (urine output >=0.5 mL/kg/hr), MAP >=65 mmHg, and improving mentation/capillary refill.",
                    nursingPearl = "Track lactate clearance + urine output >=0.5 mL/kg/hr + mentation to verify true microcirculatory recovery."
                )
            ),
            debriefTakeaways = listOf(
                "Hour-1 Sepsis Bundle: Lactate -> Blood Cultures -> IV Broad-Spectrum Antibiotics -> 30 mL/kg Crystalloids.",
                "Norepinephrine is the #1 vasopressor for septic shock (target MAP >= 65 mmHg).",
                "Vasopressor extravasation antidote = local subcutaneous Phentolamine + warm compresses (never flush or ice)."
            )
        ),

        // CASE 4: Pediatrics — DKA -> Potassium Safety -> Cerebral Edema
        UnfoldingClinicalCase(
            id = "case_peds_dka_edema",
            title = "Pediatric DKA & Cerebral Edema Crisis",
            specialty = "Pediatrics",
            examTag = "NORCET Mains · PICU",
            acuityBadge = "PICU URGENT",
            patientSummary = "9-year-old boy (30 kg) brought to ER with 3 days of polyuria, vomiting, fruity acetone breath, and deep rapid Kussmaul breathing.",
            estimatedMinutes = 5,
            stages = listOf(
                ClinicalCaseStage(
                    stageNumber = 1,
                    stageTitle = "Initial Resuscitation & Potassium Check",
                    elapsedTimeLabel = "T+00 min",
                    clinicalUpdate = "Capillary glucose is 540 mg/dL, venous pH 7.12, HCO3- 9 mEq/L, ketones 4+, and serum K+ returns at 3.0 mEq/L. Urine output is confirmed.",
                    vitals = BedsideVitals(
                        hr = VitalReading("HR", "138", "bpm", isCritical = true, direction = VitalDeltaDirection.UP),
                        bp = VitalReading("BP", "92/56", "mmHg"),
                        spo2 = VitalReading("SpO2", "99", "%"),
                        rr = VitalReading("RR", "34", "Kussmaul", isCritical = true, direction = VitalDeltaDirection.UP),
                        temp = VitalReading("Temp", "37.1", "°C"),
                        gcs = VitalReading("GCS", "14", "/15"),
                        keyClue = "BG 540 mg/dL · pH 7.12 · Serum K+ 3.0 mEq/L (HYPOKALEMIA!)"
                    ),
                    questionPrompt = "After starting initial isotonic 0.9% NS hydration, what MUST the nurse verify BEFORE initiating IV Regular Insulin infusion?",
                    options = listOf(
                        "Give an immediate IV insulin bolus of 0.5 units/kg",
                        "HOLD insulin until IV potassium replacement brings serum K+ >= 3.3–3.5 mEq/L",
                        "Administer IV Sodium Bicarbonate 2 mEq/kg push immediately",
                        "Switch immediately to 0.45% hypotonic saline"
                    ),
                    correctIndex = 1,
                    rationale = "Insulin shifts potassium rapidly into cells; starting insulin when serum K+ <3.3 mEq/L can trigger lethal hypokalemic arrhythmias/cardiac arrest. Also avoid IV insulin boluses and routine bicarbonate in pediatric DKA (both increase cerebral edema risk).",
                    nursingPearl = "In DKA: Fluids first -> check K+ (replace if <3.3 before insulin) -> continuous low-dose IV Regular Insulin (0.05–0.1 U/kg/hr, NO bolus)."
                ),
                ClinicalCaseStage(
                    stageNumber = 2,
                    stageTitle = "Glucose Drop & Dextrose Transition",
                    elapsedTimeLabel = "T+04 hr",
                    clinicalUpdate = "After K+ replacement and continuous IV Regular Insulin (0.1 U/kg/hr), capillary blood glucose falls to 240 mg/dL, while venous pH is still 7.21 and anion gap remains elevated at 20.",
                    vitals = BedsideVitals(
                        hr = VitalReading("HR", "114", "bpm", delta = "-24 ↓", direction = VitalDeltaDirection.DOWN),
                        bp = VitalReading("BP", "100/62", "mmHg", delta = "+8 ↑", direction = VitalDeltaDirection.UP),
                        spo2 = VitalReading("SpO2", "99", "%"),
                        rr = VitalReading("RR", "24", "/min", delta = "-10 ↓", direction = VitalDeltaDirection.DOWN),
                        temp = VitalReading("Temp", "36.9", "°C"),
                        gcs = VitalReading("GCS", "15", "/15", delta = "+1 ↑", direction = VitalDeltaDirection.UP),
                        keyClue = "BG 240 mg/dL (<=250) · pH still 7.21 · Anion gap still 20 (ketoacidosis NOT closed)"
                    ),
                    questionPrompt = "What is the APPROPRIATE IV fluid and insulin adjustment now that blood glucose has reached 240 mg/dL?",
                    options = listOf(
                        "Stop the IV Regular Insulin infusion immediately to prevent hypoglycemia",
                        "Add 5% Dextrose to the IV fluid (e.g., D5 + 0.45% NS with KCl) and CONTINUE the IV insulin infusion until the anion gap closes",
                        "Double the IV insulin rate to clear ketones faster",
                        "Give oral juice and stop all IV fluids"
                    ),
                    correctIndex = 1,
                    rationale = "Blood glucose normalizes hours before ketosis resolves. Stopping insulin prematurely causes rebound ketoacidosis; instead, add Dextrose (D5) when BG <=250–300 mg/dL while continuing insulin until pH >7.30 and anion gap closes.",
                    nursingPearl = "Add Dextrose at BG <=250 mg/dL—NEVER stop IV insulin until the anion gap and ketoacidosis have resolved!"
                ),
                ClinicalCaseStage(
                    stageNumber = 3,
                    stageTitle = "Acute Cerebral Edema Warning",
                    elapsedTimeLabel = "T+06 hr",
                    clinicalUpdate = "Suddenly the child complains of severe bursting headache, vomits twice, becomes irritable then lethargic, and heart rate drops from 112 to 64 bpm while BP climbs to 132/84 mmHg.",
                    vitals = BedsideVitals(
                        hr = VitalReading("HR", "64", "bpm", delta = "-50 ↓", direction = VitalDeltaDirection.CRITICAL, isCritical = true),
                        bp = VitalReading("BP", "132/84", "mmHg", delta = "+32 ↑", direction = VitalDeltaDirection.CRITICAL, isCritical = true),
                        spo2 = VitalReading("SpO2", "96", "%", delta = "-3 ↓", direction = VitalDeltaDirection.DOWN),
                        rr = VitalReading("RR", "14", "irreg", delta = "-10 ↓", direction = VitalDeltaDirection.CRITICAL, isCritical = true),
                        temp = VitalReading("Temp", "36.8", "°C"),
                        gcs = VitalReading("GCS", "10", "/15", delta = "-5 ↓", direction = VitalDeltaDirection.CRITICAL, isCritical = true),
                        keyClue = "Cushing's Triad (Bradycardia + HTN + Irregular RR) + Sudden Headache & GCS drop"
                    ),
                    questionPrompt = "Which life-saving bedside emergency treatment MUST be initiated IMMEDIATELY for suspected DKA cerebral edema?",
                    options = listOf(
                        "Delay treatment for 45 minutes until an MRI brain is completed",
                        "Elevate head of bed 30°, reduce IV fluid rate by one-third, and administer IV Mannitol (0.5–1 g/kg) or 3% Hypertonic Saline immediately",
                        "Administer a rapid 40 mL/kg normal saline bolus",
                        "Place the child flat in Trendelenburg position"
                    ),
                    correctIndex = 1,
                    rationale = "Cerebral edema is the leading cause of mortality in pediatric DKA. Clinical signs (headache, vomiting, GCS decline, Cushing's triad) demand IMMEDIATE osmotherapy (IV Mannitol or 3% NaCl), HOB 30° midline, and fluid rate reduction—never delay treatment for CT/MRI.",
                    nursingPearl = "Pediatric DKA + sudden headache + bradycardia/HTN = Cerebral Edema -> treat immediately with IV Mannitol or 3% NaCl + HOB 30°."
                ),
                ClinicalCaseStage(
                    stageNumber = 4,
                    stageTitle = "Safe Subcutaneous Transition",
                    elapsedTimeLabel = "T+18 hr",
                    clinicalUpdate = "After osmotherapy, GCS fully recovers to 15/15. Venous pH is 7.37, HCO3- is 20 mEq/L, anion gap is 10 (closed), and the child is hungry and tolerating oral fluids.",
                    vitals = BedsideVitals(
                        hr = VitalReading("HR", "92", "bpm", delta = "+28 ↑", direction = VitalDeltaDirection.UP),
                        bp = VitalReading("BP", "104/64", "mmHg", delta = "-28 ↓", direction = VitalDeltaDirection.DOWN),
                        spo2 = VitalReading("SpO2", "99", "%", delta = "+3 ↑", direction = VitalDeltaDirection.UP),
                        rr = VitalReading("RR", "20", "/min"),
                        temp = VitalReading("Temp", "36.8", "°C"),
                        gcs = VitalReading("GCS", "15", "/15", delta = "+5 ↑", direction = VitalDeltaDirection.UP),
                        keyClue = "pH 7.37 · HCO3- 20 · Anion gap 10 (closed) · Tolerating oral diet"
                    ),
                    questionPrompt = "When transitioning from IV Regular Insulin infusion to Subcutaneous Basal-Bolus Insulin, what timing rule prevents rebound DKA?",
                    options = listOf(
                        "Stop the IV insulin 2 hours BEFORE giving the first subcutaneous dose",
                        "Administer the subcutaneous insulin dose 1–2 hours BEFORE discontinuing the IV insulin infusion (overlap period)",
                        "Stop IV insulin and wait 6 hours to observe fasting glucose",
                        "Switch directly to oral Metformin"
                    ),
                    correctIndex = 1,
                    rationale = "IV Regular Insulin has a half-life of only ~5–7 minutes. Giving subcutaneous insulin 1–2 hours BEFORE stopping the IV drip ensures therapeutic overlap so the patient never has an insulin-free gap that triggers rebound ketoacidosis.",
                    nursingPearl = "Always overlap subcutaneous insulin by 1–2 hours BEFORE turning off an IV insulin infusion!"
                )
            ),
            debriefTakeaways = listOf(
                "Hold insulin in DKA if serum K+ < 3.3 mEq/L; replace K+ first once urine output is confirmed.",
                "Add D5 to IV fluids at BG <= 250 mg/dL and keep insulin running until the anion gap closes.",
                "Treat DKA cerebral edema (headache, GCS drop, Cushing's triad) immediately with Mannitol/3% NaCl—never wait for imaging."
            )
        ),

        // CASE 5: OBG — Postpartum Hemorrhage (Uterine Atony + Asthma Contraindication)
        UnfoldingClinicalCase(
            id = "case_pph_uterotonics",
            title = "Atonic PPH & Uterotonic Safety Trap",
            specialty = "OBG",
            examTag = "NORCET Mains · Labor Room",
            acuityBadge = "OBG CODE RED",
            patientSummary = "31F G3P3 with known bronchial asthma and chronic hypertension (BP 148/94 baseline) 20 minutes after vaginal delivery of a 4.1 kg macrosomic baby.",
            estimatedMinutes = 4,
            stages = listOf(
                ClinicalCaseStage(
                    stageNumber = 1,
                    stageTitle = "Immediate Bedside PPH Recognition",
                    elapsedTimeLabel = "T+00 min",
                    clinicalUpdate = "Suddenly a gush of 650 mL bright-red blood soaks the linen. On abdominal palpation, the uterine fundus is soft, boggy, 2 cm above the umbilicus, and deviated to the right.",
                    vitals = BedsideVitals(
                        hr = VitalReading("HR", "118", "bpm", isCritical = true, direction = VitalDeltaDirection.UP),
                        bp = VitalReading("BP", "96/58", "mmHg", isCritical = true, direction = VitalDeltaDirection.DOWN),
                        spo2 = VitalReading("SpO2", "97", "%"),
                        rr = VitalReading("RR", "22", "/min"),
                        temp = VitalReading("Temp", "36.7", "°C"),
                        gcs = VitalReading("GCS", "15", "/15"),
                        keyClue = "Boggy fundus deviated RIGHT · Estimated blood loss 650 mL · History: Asthma & HTN"
                    ),
                    questionPrompt = "What is the nurse's FIRST immediate bedside action?",
                    options = listOf(
                        "Perform firm fundal massage, call for help, and catheterize to empty the distended urinary bladder",
                        "Pack the vagina tightly with sterile dry gauze",
                        "Wait 15 minutes to see if bleeding slows spontaneously",
                        "Place an ice pack on the perineum only"
                    ),
                    correctIndex = 0,
                    rationale = "Uterine atony causes ~70–80% of primary PPH; a fundus displaced to the right signals a full bladder mechanically preventing uterine contraction. Firm fundal massage + bladder emptying + IV Oxytocin are immediate first-line actions.",
                    nursingPearl = "Boggy fundus + deviated to the right = EMPTY THE BLADDER while massaging the fundus!"
                ),
                ClinicalCaseStage(
                    stageNumber = 2,
                    stageTitle = "Second-Line Uterotonic Contraindication Trap",
                    elapsedTimeLabel = "T+10 min",
                    clinicalUpdate = "Despite bladder emptying, vigorous fundal massage, IV Oxytocin 20 units in 1 L NS, and IV Tranexamic acid 1 g, the uterus remains intermittently boggy with ongoing trickle (EBL 1,100 mL).",
                    vitals = BedsideVitals(
                        hr = VitalReading("HR", "132", "bpm", delta = "+14 ↑", direction = VitalDeltaDirection.UP, isCritical = true),
                        bp = VitalReading("BP", "84/50", "mmHg", delta = "-12 ↓", direction = VitalDeltaDirection.CRITICAL, isCritical = true),
                        spo2 = VitalReading("SpO2", "95", "%", delta = "-2 ↓", direction = VitalDeltaDirection.DOWN),
                        rr = VitalReading("RR", "26", "/min", delta = "+4 ↑", direction = VitalDeltaDirection.UP),
                        temp = VitalReading("Temp", "36.4", "°C"),
                        gcs = VitalReading("GCS", "14", "/15", delta = "-1"),
                        keyClue = "Ongoing atony · EBL 1,100 mL · PMH: Bronchial Asthma + Chronic Hypertension"
                    ),
                    questionPrompt = "Which second-line uterotonic is SAFEST for THIS specific patient (who has BOTH asthma AND hypertension)?",
                    options = listOf(
                        "IM Methylergonovine (Methergine) 0.2 mg",
                        "IM Carboprost (15-methyl PGF2α / Hemabate) 250 mcg",
                        "Sublingual/Rectal Misoprostol (PGE1) 800 mcg",
                        "IV Magnesium Sulfate 4 g bolus"
                    ),
                    correctIndex = 2,
                    rationale = "Classic NORCET Mains safety trap! Methergine causes severe vasospasm and is CONTRAINDICATED in hypertension/preeclampsia. Carboprost (PGF2α) causes severe bronchospasm and is CONTRAINDICATED in bronchial asthma. Misoprostol (PGE1) is safe in BOTH!",
                    nursingPearl = "PPH Drug Traps: NO Methergine in HTN/Preeclampsia · NO Carboprost in Asthma · Misoprostol (PGE1) is safe in both!"
                ),
                ClinicalCaseStage(
                    stageNumber = 3,
                    stageTitle = "Refractory Bleeding & Tamponade Stabilization",
                    elapsedTimeLabel = "T+22 min",
                    clinicalUpdate = "Two 16G IV lines are running warmed crystalloid and cross-matched packed RBCs are infusing. The obstetrician inserts an intrauterine Bakri balloon tamponade.",
                    vitals = BedsideVitals(
                        hr = VitalReading("HR", "106", "bpm", delta = "-26 ↓", direction = VitalDeltaDirection.DOWN),
                        bp = VitalReading("BP", "104/66", "mmHg", delta = "+20 ↑", direction = VitalDeltaDirection.UP),
                        spo2 = VitalReading("SpO2", "98", "%", delta = "+3 ↑", direction = VitalDeltaDirection.UP),
                        rr = VitalReading("RR", "20", "/min", delta = "-6 ↓", direction = VitalDeltaDirection.DOWN),
                        temp = VitalReading("Temp", "36.6", "°C"),
                        gcs = VitalReading("GCS", "15", "/15", delta = "+1 ↑", direction = VitalDeltaDirection.UP),
                        keyClue = "Bakri balloon inflated with 350 mL sterile saline · Bleeding controlled"
                    ),
                    questionPrompt = "Which nursing measure is ESSENTIAL while an intrauterine Bakri balloon tamponade is in place?",
                    options = listOf(
                        "Inflate the balloon with 500 mL of room air",
                        "Maintain an indwelling Foley catheter to monitor urine output (>=30 mL/hr) and measure blood drainage from the Bakri drainage port",
                        "Remove the balloon after 15 minutes regardless of tone",
                        "Stop monitoring vital signs once the balloon is inflated"
                    ),
                    correctIndex = 1,
                    rationale = "Bakri balloons are ALWAYS inflated with sterile saline/water (never air—embolism risk), paired with a Foley catheter to prevent bladder distension and track renal perfusion, and monitored closely for ongoing drainage through the central lumen.",
                    nursingPearl = "With Bakri balloon: inflate with sterile saline (never air), keep Foley in place, track central drainage + shock index (HR/SBP < 0.9)."
                )
            ),
            debriefTakeaways = listOf(
                "4 T's of PPH: Tone (atony #1), Trauma, Tissue (retained placenta), Thrombin (coagulopathy).",
                "Remember absolute uterotonic contraindications: Methergine = HTN; Carboprost = Asthma.",
                "Give IV Tranexamic Acid 1 g within 3 hours of birth in PPH alongside uterotonics and fluid/blood resuscitation."
            )
        ),

        // CASE 6: ICU — Acute COPD Exacerbation -> CO2 Narcosis -> NIV Management
        UnfoldingClinicalCase(
            id = "case_copd_co2_narcosis",
            title = "COPD Hypercapnic Failure & NIV Titration",
            specialty = "ICU & Sepsis",
            examTag = "NORCET Mains · Respiratory",
            acuityBadge = "AIRWAY WATCH",
            patientSummary = "66M with GOLD Stage IV COPD (40 pack-year smoking history) admitted with purulent sputum and worsening dyspnea.",
            estimatedMinutes = 4,
            stages = listOf(
                ClinicalCaseStage(
                    stageNumber = 1,
                    stageTitle = "Oxygen Toxicity / CO2 Narcosis Prevention",
                    elapsedTimeLabel = "T+00 min",
                    clinicalUpdate = "An attendant increased his oxygen from 2 L/min nasal cannula to 8 L/min via simple face mask because SpO2 was 90%. Thirty minutes later, he is somnolent with asterixis (flapping tremor).",
                    vitals = BedsideVitals(
                        hr = VitalReading("HR", "108", "bpm"),
                        bp = VitalReading("BP", "146/86", "mmHg"),
                        spo2 = VitalReading("SpO2", "99", "%", isCritical = true, direction = VitalDeltaDirection.UP),
                        rr = VitalReading("RR", "10", "/min", isCritical = true, direction = VitalDeltaDirection.CRITICAL),
                        temp = VitalReading("Temp", "37.6", "°C"),
                        gcs = VitalReading("GCS", "11", "/15", isCritical = true, direction = VitalDeltaDirection.DOWN),
                        keyClue = "ABG: pH 7.22 · PaCO2 74 mmHg · HCO3- 30 mEq/L · SpO2 99% on 8 L/min"
                    ),
                    questionPrompt = "What is the nurse's PRIORITY action for this acute-on-chronic hypercapnic respiratory acidosis?",
                    options = listOf(
                        "Turn off oxygen completely to zero",
                        "Titrate oxygen via controlled Venturi mask (24–28%) to target SpO2 88–92% and immediately prepare BiPAP (non-invasive ventilation)",
                        "Increase oxygen to 15 L/min non-rebreather mask",
                        "Administer IV Morphine 4 mg to ease breathing"
                    ),
                    correctIndex = 1,
                    rationale = "Excessive O2 in chronic CO2 retainers causes V/Q mismatch, Haldane effect, and blunted hypoxic drive -> CO2 narcosis. Never abruptly stop O2 (fatal rebound hypoxemia); titrate strictly to 88–92% using a Venturi mask and initiate BiPAP for pH <7.35 with PaCO2 >45.",
                    nursingPearl = "COPD target SpO2 = 88–92%. Venturi mask delivers exact FiO2; BiPAP (IPAP/EPAP) blows off retained CO2."
                ),
                ClinicalCaseStage(
                    stageNumber = 2,
                    stageTitle = "BiPAP Initiation & Contraindication Check",
                    elapsedTimeLabel = "T+15 min",
                    clinicalUpdate = "BiPAP is initiated (IPAP 14 cmH2O / EPAP 5 cmH2O) with FiO2 titrated to keep SpO2 at 90%. Repeat ABG at 1 hour shows pH 7.31, PaCO2 60 mmHg.",
                    vitals = BedsideVitals(
                        hr = VitalReading("HR", "96", "bpm", delta = "-12 ↓", direction = VitalDeltaDirection.DOWN),
                        bp = VitalReading("BP", "134/80", "mmHg", delta = "-12 ↓", direction = VitalDeltaDirection.DOWN),
                        spo2 = VitalReading("SpO2", "90", "%", delta = "Target", direction = VitalDeltaDirection.STABLE),
                        rr = VitalReading("RR", "18", "/min", delta = "+8 ↑", direction = VitalDeltaDirection.UP),
                        temp = VitalReading("Temp", "37.4", "°C"),
                        gcs = VitalReading("GCS", "14", "/15", delta = "+3 ↑", direction = VitalDeltaDirection.UP),
                        keyClue = "Repeat ABG: pH 7.31 (improving from 7.22) · PaCO2 60 (down from 74)"
                    ),
                    questionPrompt = "Which bedside change during BiPAP therapy would be an ABSOLUTE indication to stop NIV and prepare for immediate endotracheal intubation?",
                    options = listOf(
                        "Mild nasal bridge redness under the mask",
                        "Worsening obtundation (GCS <= 8), inability to protect airway, or persistent vomiting",
                        "SpO2 reading 89% while alert and conversing",
                        "Patient asking for a sip of water"
                    ),
                    correctIndex = 1,
                    rationale = "NIV (BiPAP) requires an intact airway and cooperative patient. Coma/GCS <=8, copious unmanageable secretions, hemodynamic instability, or active vomiting carry extreme aspiration risk and mandate immediate endotracheal intubation.",
                    nursingPearl = "If GCS drops <= 8 or patient vomits on BiPAP -> stop NIV and intubate immediately to protect the airway."
                ),
                ClinicalCaseStage(
                    stageNumber = 3,
                    stageTitle = "Discharge Inhaler & Breathing Teaching",
                    elapsedTimeLabel = "T+48 hr",
                    clinicalUpdate = "Patient weans successfully off BiPAP to 1.5 L/min nasal cannula with baseline ABG restored (pH 7.38, PaCO2 48, HCO3- 29).",
                    vitals = BedsideVitals(
                        hr = VitalReading("HR", "82", "bpm", delta = "-14 ↓", direction = VitalDeltaDirection.DOWN),
                        bp = VitalReading("BP", "126/78", "mmHg"),
                        spo2 = VitalReading("SpO2", "91", "%"),
                        rr = VitalReading("RR", "18", "/min"),
                        temp = VitalReading("Temp", "36.8", "°C"),
                        gcs = VitalReading("GCS", "15", "/15", delta = "+1 ↑", direction = VitalDeltaDirection.UP),
                        keyClue = "Compensated baseline ABG · Prescribed Salbutamol + Budesonide inhalers"
                    ),
                    questionPrompt = "When teaching the patient to use both a bronchodilator (Salbutamol) and inhaled corticosteroid (Budesonide) MDI, what instruction is MOST accurate?",
                    options = listOf(
                        "Take the steroid inhaler first, then Salbutamol immediately after",
                        "Take the bronchodilator (Salbutamol) FIRST, wait 5 minutes to dilate airways, then take Budesonide and rinse/spit mouth to prevent oral thrush",
                        "Never rinse the mouth after inhaled corticosteroids",
                        "Exhale rapidly for 1 second during pursed-lip breathing"
                    ),
                    correctIndex = 1,
                    rationale = "Bronchodilator opens airways first so the corticosteroid penetrates deeper; rinsing and spitting after ICS prevents Candida albicans oral thrush. Pursed-lip breathing prolongs exhalation (1:2 ratio) to prevent small-airway collapse.",
                    nursingPearl = "MDI Order: Bronchodilator FIRST -> wait 5 min -> Steroid SECOND -> Rinse & Spit!"
                )
            ),
            debriefTakeaways = listOf(
                "Target SpO2 88–92% in COPD using a Venturi mask; never abruptly remove O2 completely.",
                "BiPAP is first-line for acute hypercapnic COPD acidosis (pH 7.25–7.35), but GCS <=8 or vomiting mandates intubation.",
                "Always teach bronchodilator first, corticosteroid second, then rinse mouth."
            )
        ),

        // CASE 7: Emergency — Tension Pneumothorax & Chest Tube (ICD) Emergency
        UnfoldingClinicalCase(
            id = "case_tension_pneumothorax_icd",
            title = "Tension Pneumothorax & Chest Tube Troubleshooting",
            specialty = "Emergency",
            examTag = "NORCET Mains · Trauma ER",
            acuityBadge = "TRAUMA STAT",
            patientSummary = "29M brought after high-speed motorcycle collision with right-sided blunt chest wall bruising and rapidly worsening respiratory distress.",
            estimatedMinutes = 5,
            stages = listOf(
                ClinicalCaseStage(
                    stageNumber = 1,
                    stageTitle = "Clinical Diagnosis Without Delay",
                    elapsedTimeLabel = "T+00 min",
                    clinicalUpdate = "Right hemithorax has ABSENT breath sounds, hyperresonance on percussion, distended neck veins, and trachea visibly deviated to the LEFT.",
                    vitals = BedsideVitals(
                        hr = VitalReading("HR", "142", "bpm", isCritical = true, direction = VitalDeltaDirection.CRITICAL),
                        bp = VitalReading("BP", "74/46", "mmHg", isCritical = true, direction = VitalDeltaDirection.CRITICAL),
                        spo2 = VitalReading("SpO2", "81", "%", isCritical = true, direction = VitalDeltaDirection.CRITICAL),
                        rr = VitalReading("RR", "36", "/min", isCritical = true, direction = VitalDeltaDirection.UP),
                        temp = VitalReading("Temp", "36.5", "°C"),
                        gcs = VitalReading("GCS", "12", "/15", isCritical = true, direction = VitalDeltaDirection.DOWN),
                        keyClue = "Absent right breath sounds + Hyperresonance + JVD + Left tracheal shift"
                    ),
                    questionPrompt = "What is the IMMEDIATE life-saving emergency intervention for this patient?",
                    options = listOf(
                        "Send the patient to Radiology for a PA Chest X-ray to confirm pneumothorax first",
                        "Prepare for IMMEDIATE needle thoracostomy (decompression) with a 14G angiocatheter at the 4th–5th ICS anterior/mid-axillary line (or 2nd ICS MCL) followed by chest tube (ICD)",
                        "Insert a nasogastric tube first",
                        "Administer IV Furosemide for JVD"
                    ),
                    correctIndex = 1,
                    rationale = "Tension pneumothorax is a CLINICAL diagnosis causing obstructive shock—NEVER delay decompression for a Chest X-ray! Immediate needle decompression converts tension into simple pneumothorax, followed by definitive intercostal chest tube (ICD).",
                    nursingPearl = "Tension Pneumothorax = NEVER wait for CXR! ATLS 10th ed adult needle decompression site: 4th–5th ICS just anterior to mid-axillary line (or 2nd ICS MCL)."
                ),
                ClinicalCaseStage(
                    stageNumber = 2,
                    stageTitle = "Water-Seal Chamber Assessment",
                    elapsedTimeLabel = "T+25 min",
                    clinicalUpdate = "An audible hiss of air escaped during decompression, BP rebounded to 112/70 mmHg, and a 28 Fr chest tube connected to a 3-chamber underwater-seal drainage system is secured.",
                    vitals = BedsideVitals(
                        hr = VitalReading("HR", "104", "bpm", delta = "-38 ↓", direction = VitalDeltaDirection.DOWN),
                        bp = VitalReading("BP", "112/70", "mmHg", delta = "+38 ↑", direction = VitalDeltaDirection.UP),
                        spo2 = VitalReading("SpO2", "97", "%", delta = "+16 ↑", direction = VitalDeltaDirection.UP),
                        rr = VitalReading("RR", "22", "/min", delta = "-14 ↓", direction = VitalDeltaDirection.DOWN),
                        temp = VitalReading("Temp", "36.6", "°C"),
                        gcs = VitalReading("GCS", "15", "/15", delta = "+3 ↑", direction = VitalDeltaDirection.UP),
                        keyClue = "Water-seal chamber shows gentle rise/fall (tidaling) + 2 cm water column"
                    ),
                    questionPrompt = "Two hours later, you observe CONTINUOUS vigorous bubbling in the water-seal chamber (not just during coughing). What does this indicate?",
                    options = listOf(
                        "Normal expected lung re-expansion",
                        "An air leak in the pleural space, insertion site, or tubing connections",
                        "Complete occlusion of the chest tube",
                        "Proper negative suction calibration"
                    ),
                    correctIndex = 1,
                    rationale = "Gentle tidaling and intermittent bubbling on expiration/coughing are normal. Continuous bubbling in the WATER-SEAL chamber signifies an air leak—assess the insertion dressing and tubing connections systematically.",
                    nursingPearl = "Water-seal chamber: Tidaling = normal patency; Continuous bubbling = AIR LEAK! Suction control chamber: gentle continuous bubbling = normal."
                ),
                ClinicalCaseStage(
                    stageNumber = 3,
                    stageTitle = "Accidental Tube Dislodgement Crisis",
                    elapsedTimeLabel = "T+05 hr",
                    clinicalUpdate = "While turning in bed, the patient accidentally pulls the chest tube COMPLETELY OUT of the chest wall, leaving an open sucking wound on the right thorax.",
                    vitals = BedsideVitals(
                        hr = VitalReading("HR", "124", "bpm", delta = "+20 ↑", direction = VitalDeltaDirection.UP, isCritical = true),
                        bp = VitalReading("BP", "102/64", "mmHg", delta = "-10 ↓", direction = VitalDeltaDirection.DOWN),
                        spo2 = VitalReading("SpO2", "90", "%", delta = "-7 ↓", direction = VitalDeltaDirection.CRITICAL, isCritical = true),
                        rr = VitalReading("RR", "28", "/min", delta = "+6 ↑", direction = VitalDeltaDirection.UP),
                        temp = VitalReading("Temp", "36.6", "°C"),
                        gcs = VitalReading("GCS", "14", "/15"),
                        keyClue = "Chest tube completely dislodged from thorax · Sucking wound visible"
                    ),
                    questionPrompt = "What is the nurse's IMMEDIATE bedside action?",
                    options = listOf(
                        "Reinsert the pulled-out chest tube back into the incision immediately",
                        "Apply a sterile petroleum/occlusive dressing taped on THREE sides (flutter valve) and notify the physician immediately",
                        "Tape an occlusive dressing tightly on all FOUR sides",
                        "Submerge the dislodged tube tip in sterile water while leaving the chest wound uncovered"
                    ),
                    correctIndex = 1,
                    rationale = "If the chest tube comes OUT of the chest wall, cover the wound with sterile occlusive gauze taped on THREE sides (allows trapped air to escape on expiration while blocking air entry on inspiration—preventing tension pneumothorax!). Note: submerging tip in sterile water is for tubing DISCONNECTION from the drainage box, NOT tube pull-out from the chest!",
                    nursingPearl = "Tube OUT of chest = 3-sided occlusive dressing · Tubing DISCONNECTED from box = dip end 2 cm into sterile water!"
                ),
                ClinicalCaseStage(
                    stageNumber = 4,
                    stageTitle = "Safe Chest Tube Removal Technique",
                    elapsedTimeLabel = "Day 4",
                    clinicalUpdate = "CXR confirms full right lung re-expansion with no air leak for 24 hours and drainage < 50 mL/24 hr. Physician prepares to pull the ICD at bedside.",
                    vitals = BedsideVitals(
                        hr = VitalReading("HR", "78", "bpm", delta = "-46 ↓", direction = VitalDeltaDirection.DOWN),
                        bp = VitalReading("BP", "118/74", "mmHg", delta = "+16 ↑", direction = VitalDeltaDirection.UP),
                        spo2 = VitalReading("SpO2", "99", "%", delta = "+9 ↑", direction = VitalDeltaDirection.UP),
                        rr = VitalReading("RR", "16", "/min", delta = "-12 ↓", direction = VitalDeltaDirection.DOWN),
                        temp = VitalReading("Temp", "36.7", "°C"),
                        gcs = VitalReading("GCS", "15", "/15", delta = "+1 ↑", direction = VitalDeltaDirection.UP),
                        keyClue = "Lung fully expanded · Ready for bedside ICD removal"
                    ),
                    questionPrompt = "Which breathing maneuver should the nurse instruct the patient to perform at the exact moment the chest tube is pulled out?",
                    options = listOf(
                        "Take a deep breath IN while the tube is being pulled",
                        "Take a deep breath, hold it, and bear down (Valsalva maneuver) to maintain positive intrathoracic pressure",
                        "Pant rapidly through an open mouth",
                        "Cough vigorously as the tube exits"
                    ),
                    correctIndex = 1,
                    rationale = "Holding breath / Valsalva maneuver raises intrathoracic pressure above atmospheric pressure so room air cannot be sucked back into the pleural space during withdrawal, followed immediately by an airtight petroleum gauze dressing.",
                    nursingPearl = "Pull chest tube on Valsalva (breath hold) + airtight petroleum dressing -> obtain post-removal Chest X-ray."
                )
            ),
            debriefTakeaways = listOf(
                "Tension pneumothorax is a clinical diagnosis—decompress immediately before any Chest X-ray.",
                "Continuous water-seal bubbling = air leak; never routinely clamp a chest tube.",
                "Tube pulled out of chest = 3-sided taped occlusive dressing; tube disconnected from box = submerge end in sterile water."
            )
        ),

        // CASE 8: ICU & Sepsis — Life-Threatening Hyperkalemia in AKI
        UnfoldingClinicalCase(
            id = "case_hyperkalemia_aki",
            title = "Severe AKI Hyperkalemia (K+ 7.1 mEq/L)",
            specialty = "ICU & Sepsis",
            examTag = "NORCET Mains · Renal ICU",
            acuityBadge = "CARDIAC DANGER",
            patientSummary = "54M with acute kidney injury (oliguria × 36 hr) reporting generalized muscle weakness and palpitations.",
            estimatedMinutes = 4,
            stages = listOf(
                ClinicalCaseStage(
                    stageNumber = 1,
                    stageTitle = "Emergency Cardiac Membrane Stabilization",
                    elapsedTimeLabel = "T+00 min",
                    clinicalUpdate = "STAT serum K+ returns at 7.1 mEq/L (hemolysis ruled out). Telemetry displays tall peaked T waves, flattened P waves, and widening QRS complexes (0.14 sec).",
                    vitals = BedsideVitals(
                        hr = VitalReading("HR", "52", "bpm", isCritical = true, direction = VitalDeltaDirection.DOWN),
                        bp = VitalReading("BP", "108/66", "mmHg"),
                        spo2 = VitalReading("SpO2", "97", "%"),
                        rr = VitalReading("RR", "22", "/min"),
                        temp = VitalReading("Temp", "36.8", "°C"),
                        gcs = VitalReading("GCS", "15", "/15"),
                        keyClue = "K+ 7.1 mEq/L · ECG: Tall tented T waves + Widening QRS (imminent VF/asystole)"
                    ),
                    questionPrompt = "Which IV medication MUST be administered FIRST (within 1–3 minutes) to prevent sudden cardiac arrest?",
                    options = listOf(
                        "Oral Sodium Polystyrene Sulfonate (Kayexalate)",
                        "IV 10% Calcium Gluconate 10 mL (1 g) slowly over 2–5 minutes under ECG monitoring",
                        "Oral Furosemide 40 mg",
                        "IV Spironolactone"
                    ),
                    correctIndex = 1,
                    rationale = "ECG changes with K+ >=6.5 demand IMMEDIATE cardiac membrane stabilization with IV 10% Calcium Gluconate (works within 1–3 minutes for 30–60 min). Note: Calcium Gluconate protects the myocardium but does NOT lower serum K+!",
                    nursingPearl = "Hyperkalemia + ECG changes -> #1 FIRST drug is IV 10% Calcium Gluconate (stabilizes heart membrane in 1–3 min)."
                ),
                ClinicalCaseStage(
                    stageNumber = 2,
                    stageTitle = "Rapid Intracellular Potassium Shift",
                    elapsedTimeLabel = "T+08 min",
                    clinicalUpdate = "QRS narrows back to 0.09 sec after Calcium Gluconate, but serum K+ is still 7.1 mEq/L. Physician orders IV Regular Insulin 10 units + 50% Dextrose 50 mL (25 g) and nebulized Salbutamol.",
                    vitals = BedsideVitals(
                        hr = VitalReading("HR", "72", "bpm", delta = "+20 ↑", direction = VitalDeltaDirection.UP),
                        bp = VitalReading("BP", "114/70", "mmHg", delta = "+6 ↑", direction = VitalDeltaDirection.UP),
                        spo2 = VitalReading("SpO2", "98", "%"),
                        rr = VitalReading("RR", "20", "/min"),
                        temp = VitalReading("Temp", "36.8", "°C"),
                        gcs = VitalReading("GCS", "15", "/15"),
                        keyClue = "QRS narrowed · Giving IV Regular Insulin 10 U + D50W to shift K+ into cells"
                    ),
                    questionPrompt = "Which bedside monitoring check is MANDATORY for the next 4–6 hours after giving IV Insulin + Dextrose in an AKI patient?",
                    options = listOf(
                        "Hourly capillary blood glucose checks because impaired renal insulin clearance creates high risk of delayed hypoglycemia",
                        "No monitoring needed once Dextrose is given",
                        "Restricting all oral/IV glucose completely",
                        "Checking serum amylase every 30 minutes"
                    ),
                    correctIndex = 0,
                    rationale = "Kidneys clear ~30–40% of endogenous and exogenous insulin. In AKI/CKD, IV Regular Insulin lingers far longer than the D50W bolus, causing dangerous delayed hypoglycemia 1–4 hours later—mandatory hourly CBG monitoring!",
                    nursingPearl = "Insulin + Dextrose shifts K+ intracellularly within 15–30 min, but ALWAYS monitor hourly CBG for delayed hypoglycemia in AKI!"
                ),
                ClinicalCaseStage(
                    stageNumber = 3,
                    stageTitle = "Definitive Total-Body Potassium Removal",
                    elapsedTimeLabel = "T+90 min",
                    clinicalUpdate = "Repeat K+ drops to 5.8 mEq/L after Insulin/Dextrose and Salbutamol, but patient remains completely anuric (urine output 0 mL/hr) with serum creatinine 6.4 mg/dL.",
                    vitals = BedsideVitals(
                        hr = VitalReading("HR", "76", "bpm"),
                        bp = VitalReading("BP", "124/76", "mmHg"),
                        spo2 = VitalReading("SpO2", "98", "%"),
                        rr = VitalReading("RR", "18", "/min"),
                        temp = VitalReading("Temp", "36.7", "°C"),
                        gcs = VitalReading("GCS", "15", "/15"),
                        keyClue = "Anuric AKI · K+ 5.8 (temporary shift only) · Creatinine 6.4 mg/dL"
                    ),
                    questionPrompt = "Which intervention provides the MOST rapid and definitive total-body potassium elimination in an anuric AKI patient?",
                    options = listOf(
                        "Repeating IV Calcium Gluconate every hour",
                        "Urgent Hemodialysis",
                        "Infusing Ringer's Lactate 2 Liters",
                        "Administering Potassium-sparing diuretics"
                    ),
                    correctIndex = 1,
                    rationale = "Calcium only stabilizes the heart and Insulin/Salbutamol only temporarily shift K+ into cells. In anuric renal failure, urgent Hemodialysis is the fastest, most reliable definitive method to remove potassium from the body (and avoid K+-containing Ringer's Lactate!).",
                    nursingPearl = "Hyperkalemia 3-step ladder: 1. Stabilize (Ca Gluconate) -> 2. Shift (Insulin+Dextrose, Salbutamol) -> 3. Eliminate (Diuretics / K-binders / Hemodialysis)."
                )
            ),
            debriefTakeaways = listOf(
                "IV 10% Calcium Gluconate is #1 first drug when ECG changes appear—protects heart without lowering K+.",
                "Monitor hourly CBG after Insulin + D50W in AKI/CKD due to delayed hypoglycemia risk.",
                "Urgent Hemodialysis is definitive potassium removal in anuric AKI; avoid Ringer's Lactate."
            )
        ),

        // CASE 9: Emergency — Anaphylactic Shock -> Biphasic Airway Edema
        UnfoldingClinicalCase(
            id = "case_anaphylaxis_rescue",
            title = "IV Antibiotic Anaphylaxis & Biphasic Reaction",
            specialty = "Emergency",
            examTag = "NORCET Mains · Ward Rapid Response",
            acuityBadge = "ANAPHYLAXIS",
            patientSummary = "34F admitted for community-acquired pneumonia; 3 minutes after starting IV Ceftriaxone she develops generalized urticaria, lip swelling, and hoarse stridor.",
            estimatedMinutes = 4,
            stages = listOf(
                ClinicalCaseStage(
                    stageNumber = 1,
                    stageTitle = "First-Line Anaphylaxis Drug, Dose & Route",
                    elapsedTimeLabel = "T+00 min",
                    clinicalUpdate = "You immediately STOP the IV Ceftriaxone infusion and call for emergency help. She is wheezing audibly with swollen tongue and lightheadedness.",
                    vitals = BedsideVitals(
                        hr = VitalReading("HR", "136", "bpm", isCritical = true, direction = VitalDeltaDirection.UP),
                        bp = VitalReading("BP", "76/44", "mmHg", isCritical = true, direction = VitalDeltaDirection.CRITICAL),
                        spo2 = VitalReading("SpO2", "87", "%", isCritical = true, direction = VitalDeltaDirection.CRITICAL),
                        rr = VitalReading("RR", "30", "stridor", isCritical = true, direction = VitalDeltaDirection.UP),
                        temp = VitalReading("Temp", "37.2", "°C"),
                        gcs = VitalReading("GCS", "14", "/15"),
                        keyClue = "Sudden urticaria + laryngeal stridor + BP 76/44 after IV Ceftriaxone"
                    ),
                    questionPrompt = "What is the FIRST-LINE emergency medication, concentration, dose, and route?",
                    options = listOf(
                        "IV Diphenhydramine (antihistamine) 50 mg first, then observe 15 minutes",
                        "IM Epinephrine (Adrenaline) 1:1,000 (1 mg/mL) — 0.3 to 0.5 mg into the anterolateral mid-thigh (vastus lateralis)",
                        "IV Hydrocortisone 200 mg alone as sole first-line rescue",
                        "Subcutaneous Epinephrine 1:10,000 into the deltoid"
                    ),
                    correctIndex = 1,
                    rationale = "IM Epinephrine 1:1,000 (0.3–0.5 mg adult; 0.01 mg/kg pediatric) in the mid-anterolateral thigh achieves the fastest, safest peak plasma levels to reverse bronchospasm, laryngeal edema, and distributive shock. Antihistamines and steroids are secondary adjuncts—NEVER delay Epinephrine for them!",
                    nursingPearl = "Anaphylaxis #1 drug = IM Epinephrine 1:1,000 (0.3–0.5 mg) in anterolateral thigh (repeat every 5–15 min PRN)."
                ),
                ClinicalCaseStage(
                    stageNumber = 2,
                    stageTitle = "Positioning & Fluid Resuscitation",
                    elapsedTimeLabel = "T+06 min",
                    clinicalUpdate = "IM Epinephrine is administered and high-flow O2 12 L/min via non-rebreather mask is applied. BP is still 82/48 mmHg due to massive capillary leak.",
                    vitals = BedsideVitals(
                        hr = VitalReading("HR", "124", "bpm", delta = "-12 ↓", direction = VitalDeltaDirection.DOWN),
                        bp = VitalReading("BP", "82/48", "mmHg", delta = "+6 ↑", direction = VitalDeltaDirection.CRITICAL, isCritical = true),
                        spo2 = VitalReading("SpO2", "94", "%", delta = "+7 ↑", direction = VitalDeltaDirection.UP),
                        rr = VitalReading("RR", "24", "/min", delta = "-6 ↓", direction = VitalDeltaDirection.DOWN),
                        temp = VitalReading("Temp", "37.1", "°C"),
                        gcs = VitalReading("GCS", "14", "/15"),
                        keyClue = "Stridor easing · Still hypotensive (BP 82/48) from capillary vasodilation"
                    ),
                    questionPrompt = "What is the SAFEST body positioning and fluid intervention for this hypotensive anaphylaxis patient?",
                    options = listOf(
                        "Make her sit upright suddenly or stand up to ease breathing",
                        "Keep her supine with legs elevated (modified Trendelenburg) as airway permits and infuse rapid 0.9% Normal Saline bolus (1–2 L)",
                        "Restrict IV fluids to prevent pulmonary edema",
                        "Place her prone"
                    ),
                    correctIndex = 1,
                    rationale = "Abrupt upright sitting or standing during anaphylactic shock can trigger fatal 'empty ventricle syndrome' (sudden cardiac arrest from zero venous return). Keep supine with legs elevated (or semi-recumbent ONLY if severe airway distress) + rapid isotonic crystalloid resuscitation.",
                    nursingPearl = "Never sit a hypotensive anaphylaxis patient bolt upright—risk of sudden 'empty ventricle' cardiac arrest!"
                ),
                ClinicalCaseStage(
                    stageNumber = 3,
                    stageTitle = "Observation Window for Biphasic Reaction",
                    elapsedTimeLabel = "T+90 min",
                    clinicalUpdate = "After two IM Epinephrine doses, 1.5 L NS, IV Diphenhydramine, and IV Hydrocortisone, urticaria and stridor resolve completely. The patient asks to sign out and go home immediately.",
                    vitals = BedsideVitals(
                        hr = VitalReading("HR", "88", "bpm", delta = "-36 ↓", direction = VitalDeltaDirection.DOWN),
                        bp = VitalReading("BP", "118/74", "mmHg", delta = "+36 ↑", direction = VitalDeltaDirection.UP),
                        spo2 = VitalReading("SpO2", "99", "%", delta = "+5 ↑", direction = VitalDeltaDirection.UP),
                        rr = VitalReading("RR", "16", "/min", delta = "-8 ↓", direction = VitalDeltaDirection.DOWN),
                        temp = VitalReading("Temp", "36.9", "°C"),
                        gcs = VitalReading("GCS", "15", "/15", delta = "+1 ↑", direction = VitalDeltaDirection.UP),
                        keyClue = "Complete symptom resolution · Required 2 doses of Epinephrine"
                    ),
                    questionPrompt = "Why MUST the nurse strongly advise continued monitored observation (minimum 4–6 hours, up to 24 hr for severe cases)?",
                    options = listOf(
                        "Because Epinephrine causes permanent hypertension",
                        "Because up to 20% of patients experience a potentially fatal BIPHASIC anaphylactic rebound 4–12 hours later without re-exposure",
                        "Only to finish the Ceftriaxone vial slowly",
                        "To wait for routine chest X-ray clearance"
                    ),
                    correctIndex = 1,
                    rationale = "Biphasic anaphylaxis causes recurrence of airway edema and shock hours after initial resolution without any new allergen exposure—highest risk in patients who required >1 dose of Epinephrine or had severe hypotension.",
                    nursingPearl = "Always observe severe anaphylaxis for 4–12+ hours for biphasic rebound, flag allergy bracelet/chart, and teach Epinephrine auto-injector use."
                )
            ),
            debriefTakeaways = listOf(
                "Stop offending agent + IM Epinephrine 1:1,000 (0.3–0.5 mg) in anterolateral thigh is ALWAYS #1.",
                "Keep hypotensive patients supine with legs elevated to avoid empty-ventricle cardiac arrest.",
                "Monitor 4–12+ hours for biphasic anaphylaxis recurrence."
            )
        ),

        // CASE 10: Surgery/MSN — Post-Thyroidectomy Hematoma & Hypocalcemic Tetany
        UnfoldingClinicalCase(
            id = "case_post_thyroidectomy_crisis",
            title = "Post-Thyroidectomy Airway & Hypocalcemic Tetany",
            specialty = "Emergency",
            examTag = "NORCET Mains · Post-Op ICU",
            acuityBadge = "AIRWAY CRITICAL",
            patientSummary = "42F 5 hours post-total thyroidectomy for multinodular goiter admitted to surgical recovery unit.",
            estimatedMinutes = 4,
            stages = listOf(
                ClinicalCaseStage(
                    stageNumber = 1,
                    stageTitle = "Early Hemorrhage vs Laryngeal Edema Detection",
                    elapsedTimeLabel = "T+00 min",
                    clinicalUpdate = "Patient reports a sudden sensation of tight pressure in the neck and difficulty swallowing. The anterior neck dressing looks mostly clean, so you slide your gloved hand behind her neck.",
                    vitals = BedsideVitals(
                        hr = VitalReading("HR", "112", "bpm", isCritical = true, direction = VitalDeltaDirection.UP),
                        bp = VitalReading("BP", "134/82", "mmHg"),
                        spo2 = VitalReading("SpO2", "94", "%", isCritical = true, direction = VitalDeltaDirection.DOWN),
                        rr = VitalReading("RR", "24", "stridor", isCritical = true, direction = VitalDeltaDirection.UP),
                        temp = VitalReading("Temp", "37.0", "°C"),
                        gcs = VitalReading("GCS", "15", "/15"),
                        keyClue = "Warm wet blood pooling at back of neck · Tight neck fullness · Mild inspiratory stridor"
                    ),
                    questionPrompt = "Why did the nurse check BEHIND the patient's neck, and what is the IMMEDIATE priority action?",
                    options = listOf(
                        "Gravity pulls post-thyroidectomy arterial bleeding posteriorly; immediately elevate HOB (semi-Fowler's), alert the surgeon STAT, and prepare bedside suture-removal/tracheostomy tray",
                        "Tighten the neck bandage to tamponade the bleeding",
                        "Place the patient flat supine and recheck in 30 minutes",
                        "Give oral lozenges for post-intubation sore throat"
                    ),
                    correctIndex = 0,
                    rationale = "Post-thyroidectomy arterial hemorrhage tracks posteriorly by gravity and rapidly compresses the trachea inside tight fascial planes. Elevate HOB (semi-Fowler's with head/neck neutral—no flexion/hyperextension), call surgeon immediately, and have bedside suture cutter & tracheostomy set ready to evacuate hematoma.",
                    nursingPearl = "Post-thyroidectomy check: ALWAYS palpate behind the neck for pooling blood + keep emergency tracheostomy tray & IV Calcium Gluconate at bedside!"
                ),
                ClinicalCaseStage(
                    stageNumber = 2,
                    stageTitle = "Acute Hypocalcemic Tetany Recognition",
                    elapsedTimeLabel = "T+18 hr",
                    clinicalUpdate = "Hematoma was evacuated cleanly in OR. On post-op day 1, the patient reports tingling/numbness around her mouth and fingertips. When the BP cuff inflates on her arm, her hand curls into painful carpal spasm.",
                    vitals = BedsideVitals(
                        hr = VitalReading("HR", "98", "bpm"),
                        bp = VitalReading("BP", "122/76", "mmHg"),
                        spo2 = VitalReading("SpO2", "97", "%"),
                        rr = VitalReading("RR", "22", "/min"),
                        temp = VitalReading("Temp", "36.9", "°C"),
                        gcs = VitalReading("GCS", "15", "/15"),
                        keyClue = "Positive Trousseau's sign (carpal spasm) + Perioral paresthesia · Serum Ca2+ 6.8 mg/dL"
                    ),
                    questionPrompt = "What caused this clinical finding and which emergency medication MUST be administered?",
                    options = listOf(
                        "Thyroid storm -> give IV Propylthiouracil",
                        "Inadvertent parathyroid injury/hypocalcemia -> administer IV 10% Calcium Gluconate slowly under continuous ECG monitoring",
                        "Hyperkalemia -> give IV Regular Insulin",
                        "Recurrent laryngeal nerve injury -> give IV Atropine"
                    ),
                    correctIndex = 1,
                    rationale = "Circumoral numbness + positive Trousseau's sign (carpal spasm on BP cuff inflation) + Chvostek's sign indicate acute hypocalcemia from parathyroid gland trauma/ischemia, risking fatal laryngospasm and QT prolongation. Treat immediately with slow IV 10% Calcium Gluconate.",
                    nursingPearl = "Trousseau's sign (BP cuff carpal spasm) is more specific than Chvostek's sign for acute hypocalcemia -> give slow IV 10% Calcium Gluconate."
                ),
                ClinicalCaseStage(
                    stageNumber = 3,
                    stageTitle = "Safe Voice & Post-Op Discharge Assessment",
                    elapsedTimeLabel = "T+36 hr",
                    clinicalUpdate = "Serum calcium stabilizes at 8.9 mg/dL on oral Calcium + Calcitriol (Vitamin D). The nurse asks the patient to speak aloud softly every few hours.",
                    vitals = BedsideVitals(
                        hr = VitalReading("HR", "78", "bpm", delta = "-20 ↓", direction = VitalDeltaDirection.DOWN),
                        bp = VitalReading("BP", "118/74", "mmHg"),
                        spo2 = VitalReading("SpO2", "99", "%", delta = "+2 ↑", direction = VitalDeltaDirection.UP),
                        rr = VitalReading("RR", "16", "/min", delta = "-6 ↓", direction = VitalDeltaDirection.DOWN),
                        temp = VitalReading("Temp", "36.8", "°C"),
                        gcs = VitalReading("GCS", "15", "/15"),
                        keyClue = "Serum Ca2+ 8.9 mg/dL · Clear strong voice · No stridor"
                    ),
                    questionPrompt = "Which voice finding would specifically alert the nurse to possible RECURRENT LARYNGEAL NERVE injury rather than routine mild intubation soreness?",
                    options = listOf(
                        "Mild throat scratchiness that improves with sips of cool water",
                        "Persistent hoarseness, weak breathy voice, or inability to speak with stridor/aspiration",
                        "Slight dryness when waking up",
                        "Normal volume speech when answering questions"
                    ),
                    correctIndex = 1,
                    rationale = "Unilateral recurrent laryngeal nerve injury causes persistent hoarseness/breathy voice; bilateral injury causes vocal cord adduction with severe stridor and airway obstruction. Mild hoarseness for 24–48 hr can occur from intubation, so track trend closely.",
                    nursingPearl = "Post-thyroidectomy 3 big complications: 1. Neck hematoma (stridor/back bleeding) · 2. Hypocalcemia (Trousseau/tetany) · 3. Recurrent laryngeal nerve injury (hoarseness/stridor)."
                )
            ),
            debriefTakeaways = listOf(
                "Always check BEHIND the neck post-thyroidectomy for pooling arterial hemorrhage.",
                "Keep emergency tracheostomy kit and IV 10% Calcium Gluconate at the bedside for 48 hours.",
                "Perioral tingling + positive Trousseau's sign = acute hypocalcemia risking laryngospasm -> treat with slow IV Calcium Gluconate."
            )
        )
    )
}
