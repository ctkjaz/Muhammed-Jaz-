package com.example.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class BedsideVitals(
    val hr: String,
    val bp: String,
    val spo2: String,
    val rr: String,
    val temp: String,
    val gcs: String,
    val deltaChips: List<String> = emptyList()
)

@Immutable
data class ClinicalCaseStage(
    val stageNumber: Int,
    val stageTitle: String,
    val elapsedTimeLabel: String,
    val vitals: BedsideVitals,
    val clinicalUpdate: String,
    val labOrMonitorClue: String,
    val prompt: String,
    val options: List<String>,
    val correctIndex: Int,
    val rationale: String,
    val criticalPearl: String
)

@Immutable
data class UnfoldingClinicalCase(
    val id: String,
    val title: String,
    val specialty: String,
    val subjectCode: String,
    val acuityTag: String,
    val examBadge: String,
    val patientProfile: String,
    val chiefComplaint: String,
    val stages: List<ClinicalCaseStage>,
    val debriefPearls: List<String>
)

object UnfoldingClinicalCasesCatalog {

    const val SPECIALTY_ICU_MSN = "ICU / MSN"
    const val SPECIALTY_OBG = "OBG"
    const val SPECIALTY_PEDS_NICU = "Pediatrics / NICU"
    const val SPECIALTY_EMERGENCY = "Emergency"

    val FILTER_SPECIALTIES: List<String> = listOf(
        "All",
        SPECIALTY_ICU_MSN,
        SPECIALTY_OBG,
        SPECIALTY_PEDS_NICU,
        SPECIALTY_EMERGENCY
    )

    val ALL_CASES: List<UnfoldingClinicalCase> = listOf(
        // 1. Acute Anterior STEMI -> Cardiogenic Shock (4 stages)
        UnfoldingClinicalCase(
            id = "case_stemi_shock",
            title = "Acute Anterior STEMI to Cardiogenic Shock",
            specialty = SPECIALTY_ICU_MSN,
            subjectCode = "MSN",
            acuityTag = "CRITICAL",
            examBadge = "NORCET Mains",
            patientProfile = "58M · Smokers · T2DM · ER Bay 2",
            chiefComplaint = "Crushing substernal chest pain radiating to left jaw for 45 min with diaphoresis",
            stages = listOf(
                ClinicalCaseStage(
                    stageNumber = 1,
                    stageTitle = "Door-to-ECG & Immediate Stabilization",
                    elapsedTimeLabel = "T+00 min",
                    vitals = BedsideVitals(
                        hr = "108 bpm",
                        bp = "146/92",
                        spo2 = "95% RA",
                        rr = "22/min",
                        temp = "36.8°C",
                        gcs = "15/15",
                        deltaChips = listOf("Pain 9/10", "Diaphoretic")
                    ),
                    clinicalUpdate = "Patient arrives clutching his chest, pale and diaphoretic. Lungs are clear to auscultation bilaterally.",
                    labOrMonitorClue = "12-Lead ECG (within 6 min): 4 mm ST-segment elevation in leads V1–V4 with reciprocal ST depression in II, III, aVF.",
                    prompt = "What is the nurse's HIGHEST priority set of immediate interventions per AHA/AIIMS STEMI protocol?",
                    options = listOf(
                        "Give chewed Aspirin 300–325 mg, obtain IV access, activate Cath Lab for Primary PCI, and avoid routine O2 since SpO2 is 95%",
                        "Apply 15 L/min non-rebreather oxygen immediately and wait for Troponin-I results before calling cardiology",
                        "Administer oral Atenolol 50 mg and keep patient flat supine for 1 hour",
                        "Give Sublingual Nitroglycerin 3 doses simultaneously and send for Chest X-ray first"
                    ),
                    correctIndex = 0,
                    rationale = "ST elevation in V1–V4 confirms Acute Anterior Wall STEMI (LAD occlusion). Never delay Cath Lab activation waiting for Troponin. Chewed aspirin is immediate, and AHA guidelines withhold routine O2 when SpO2 >= 90% to avoid coronary vasoconstriction.",
                    criticalPearl = "Door-to-ECG <= 10 min; Door-to-Balloon (Primary PCI) <= 90 min; routine O2 only if SpO2 < 90%."
                ),
                ClinicalCaseStage(
                    stageNumber = 2,
                    stageTitle = "Reperfusion Safety Check",
                    elapsedTimeLabel = "T+12 min",
                    vitals = BedsideVitals(
                        hr = "114 bpm",
                        bp = "124/80",
                        spo2 = "94% RA",
                        rr = "24/min",
                        temp = "36.8°C",
                        gcs = "15/15",
                        deltaChips = listOf("BP 124/80 ↓", "HR 114 ↑")
                    ),
                    clinicalUpdate = "Primary PCI lab is temporarily busy; the physician considers IV Tenecteplase (fibrinolysis) while reviewing the patient's history.",
                    labOrMonitorClue = "History reveals an ischemic stroke 2 months ago and prior peptic ulcer bleed. Troponin-I is elevated at 3.8 ng/mL.",
                    prompt = "Which history finding represents an ABSOLUTE contraindication to fibrinolytic therapy that the nurse must flag immediately?",
                    options = listOf(
                        "History of Type 2 Diabetes Mellitus for 8 years",
                        "Ischemic stroke within the last 3 months",
                        "Current smoking history of 20 pack-years",
                        "Heart rate of 114 bpm on admission"
                    ),
                    correctIndex = 1,
                    rationale = "Any ischemic stroke within 3 months (except acute ischemic stroke within 4.5 hours) is an absolute contraindication to fibrinolysis due to catastrophic intracranial hemorrhage risk; Primary PCI must be prioritized.",
                    criticalPearl = "Absolute fibrinolytic contraindications: any prior ICH, ischemic stroke < 3 mo, active internal bleeding, suspected aortic dissection."
                ),
                ClinicalCaseStage(
                    stageNumber = 3,
                    stageTitle = "Acute Cardiogenic Shock Onset",
                    elapsedTimeLabel = "T+28 min",
                    vitals = BedsideVitals(
                        hr = "128 bpm",
                        bp = "78/48",
                        spo2 = "87% RA",
                        rr = "30/min",
                        temp = "36.4°C",
                        gcs = "13/15",
                        deltaChips = listOf("BP 78/48 ↓↓", "SpO2 87% ↓", "Bibasilar Crackles")
                    ),
                    clinicalUpdate = "Patient becomes confused with cool, mottled extremities, jugular venous distension, and new bilateral basilar crackles. Urine output has dropped to 10 mL over 30 min.",
                    labOrMonitorClue = "Arterial lactate 4.6 mmol/L; Bedside echo shows LVEF 24% with anterior akinesia; Cardiac Index 1.7 L/min/m².",
                    prompt = "Which medication order should the nurse QUESTION or HOLD immediately in this hemodynamic state?",
                    options = listOf(
                        "IV Norepinephrine infusion titrated to MAP >= 65 mmHg",
                        "IV Nitroglycerin infusion and IV Metoprolol 5 mg push",
                        "Supplemental oxygen to maintain SpO2 >= 92%",
                        "Preparation for emergency coronary angiography and IABP support"
                    ),
                    correctIndex = 1,
                    rationale = "In cardiogenic shock (BP 78/48, low cardiac index, pulmonary edema), vasodilators (Nitroglycerin) and negative inotropes (Metoprolol) will precipitate fatal cardiovascular collapse. Norepinephrine + urgent revascularization are indicated.",
                    criticalPearl = "Hold beta-blockers and nitrates whenever SBP < 90 mmHg or signs of cardiogenic shock/pulmonary edema appear."
                ),
                ClinicalCaseStage(
                    stageNumber = 4,
                    stageTitle = "Post-PCI Reperfusion Arrhythmia",
                    elapsedTimeLabel = "T+95 min",
                    vitals = BedsideVitals(
                        hr = "172 bpm",
                        bp = "68/40",
                        spo2 = "92% O2",
                        rr = "26/min",
                        temp = "36.6°C",
                        gcs = "9/15",
                        deltaChips = listOf("HR 172 ↑↑", "Wide-QRS VT", "Pulse Present")
                    ),
                    clinicalUpdate = "After successful LAD stenting, the patient becomes acutely dizzy and lethargic. A weak carotid pulse is still palpable.",
                    labOrMonitorClue = "Cardiac monitor shows regular monomorphic wide-complex tachycardia at 172 bpm (Unstable Ventricular Tachycardia with pulse).",
                    prompt = "What is the nurse's IMMEDIATE life-saving priority action?",
                    options = listOf(
                        "Prepare for immediate synchronized cardioversion at 100 J biphasic",
                        "Administer IV Adenosine 6 mg rapid push",
                        "Deliver unsynchronized defibrillation at 200 J without checking sync mode",
                        "Start oral Amiodarone 200 mg and recheck ECG in 15 minutes"
                    ),
                    correctIndex = 0,
                    rationale = "Unstable monomorphic VT WITH a pulse (hypotension, altered mental status) requires immediate synchronized cardioversion (100 J biphasic). Unsynchronized defibrillation is reserved for pulseless VT/VF or polymorphic VT.",
                    criticalPearl = "Unstable tachycardia WITH pulse = Synchronized Cardioversion; Pulseless VT / V-Fib = Unsynchronized Defibrillation + CPR."
                )
            ),
            debriefPearls = listOf(
                "Anterior STEMI (V1–V4, LAD) carries the highest risk of LV pump failure and cardiogenic shock.",
                "Withhold routine supplemental O2 in uncomplicated MI if SpO2 >= 90%, and hold nitrates/beta-blockers if SBP < 90 mmHg.",
                "Always differentiate unstable VT WITH pulse (synchronized cardioversion) from pulseless VT (immediate CPR + unsynchronized shock)."
            )
        ),

        // 2. Septic Shock & 1-Hour Bundle (4 stages)
        UnfoldingClinicalCase(
            id = "case_septic_shock",
            title = "Urosepsis to Septic Shock & 1-Hour Bundle",
            specialty = SPECIALTY_ICU_MSN,
            subjectCode = "MSN",
            acuityTag = "CRITICAL",
            examBadge = "NORCET Mains",
            patientProfile = "71F · Indwelling Foley · Medical ICU",
            chiefComplaint = "High-grade fever with rigors, flank pain, and progressive drowsiness over 6 hours",
            stages = listOf(
                ClinicalCaseStage(
                    stageNumber = 1,
                    stageTitle = "Surviving Sepsis 1-Hour Sequence",
                    elapsedTimeLabel = "T+00 min",
                    vitals = BedsideVitals(
                        hr = "122 bpm",
                        bp = "86/52",
                        spo2 = "93% RA",
                        rr = "26/min",
                        temp = "39.4°C",
                        gcs = "14/15",
                        deltaChips = listOf("MAP 63 ↓", "Temp 39.4°C ↑")
                    ),
                    clinicalUpdate = "Foley tubing shows cloudy, foul-smelling urine. Two 18G peripheral IV lines are inserted. Orders include blood cultures ×2, IV Piperacillin-Tazobactam, serum lactate, and 0.9% NS bolus.",
                    labOrMonitorClue = "WBC 21,400/µL (bandemia 14%), Serum Lactate 4.2 mmol/L, Procalcitonin 12 ng/mL.",
                    prompt = "In what EXACT sequence must the nurse execute the diagnostic and antimicrobial orders?",
                    options = listOf(
                        "Infuse IV Piperacillin-Tazobactam first, then draw blood cultures 2 hours later",
                        "Draw 2 sets of blood cultures (aerobic + anaerobic) FIRST, then administer IV Piperacillin-Tazobactam immediately without delaying >45 min",
                        "Wait for final urine culture sensitivity report before starting broad-spectrum antibiotics",
                        "Give oral Paracetamol and observe fever curve for 1 hour before starting IV fluids"
                    ),
                    correctIndex = 1,
                    rationale = "Surviving Sepsis Campaign 1-hour bundle mandates drawing blood cultures BEFORE broad-spectrum antibiotics so pathogen growth is not sterilized, while still administering IV antimicrobials within 1 hour of recognition.",
                    criticalPearl = "Sepsis 1-Hr Bundle: Measure Lactate -> Blood Cultures -> Broad-Spectrum IV Antibiotics -> 30 mL/kg Crystalloid -> Vasopressors for MAP >= 65."
                ),
                ClinicalCaseStage(
                    stageNumber = 2,
                    stageTitle = "Crystalloid Resuscitation Target",
                    elapsedTimeLabel = "T+20 min",
                    vitals = BedsideVitals(
                        hr = "118 bpm",
                        bp = "84/50",
                        spo2 = "95% 2L O2",
                        rr = "24/min",
                        temp = "39.0°C",
                        gcs = "14/15",
                        deltaChips = listOf("MAP 61 ↓", "Weight 60 kg")
                    ),
                    clinicalUpdate = "Blood cultures are drawn and Piperacillin-Tazobactam is infusing. Patient weighs 60 kg and remains hypotensive with lactate 4.2 mmol/L.",
                    labOrMonitorClue = "Baseline lung sounds clear; IVC collapsible >50% on bedside ultrasound (fluid responsive).",
                    prompt = "What initial IV crystalloid volume is recommended by the Surviving Sepsis Campaign for this 60 kg patient?",
                    options = listOf(
                        "500 mL of 5% Dextrose over 6 hours",
                        "1,800 mL (30 mL/kg) of balanced crystalloid (Ringer's Lactate / Plasma-Lyte) within the first 3 hours",
                        "3,600 mL of 0.45% hypotonic saline as rapid bolus",
                        "250 mL of 5% Albumin only, avoiding all crystalloids"
                    ),
                    correctIndex = 1,
                    rationale = "Standard sepsis resuscitation is 30 mL/kg of isotonic balanced crystalloid within 3 hours (30 × 60 kg = 1,800 mL), with frequent reassessment of lung sounds and perfusion.",
                    criticalPearl = "30 mL/kg isotonic crystalloid is the benchmark initial volume in sepsis-induced hypoperfusion."
                ),
                ClinicalCaseStage(
                    stageNumber = 3,
                    stageTitle = "Refractory Hypotension -> Vasopressor Choice",
                    elapsedTimeLabel = "T+65 min",
                    vitals = BedsideVitals(
                        hr = "126 bpm",
                        bp = "76/44",
                        spo2 = "96% 2L O2",
                        rr = "26/min",
                        temp = "38.6°C",
                        gcs = "13/15",
                        deltaChips = listOf("MAP 55 ↓↓", "Post-1800mL RL")
                    ),
                    clinicalUpdate = "Despite completing the 1,800 mL crystalloid bolus, MAP remains 55 mmHg and urine output is 12 mL/hr. Central venous line placement is underway.",
                    labOrMonitorClue = "Repeat Lactate 4.5 mmol/L; Diagnosis upgraded to Septic Shock.",
                    prompt = "Which vasopressor is the FIRST-LINE agent of choice to restore MAP >= 65 mmHg in septic shock?",
                    options = listOf(
                        "IV Dopamine infusion",
                        "IV Norepinephrine (Noradrenaline) infusion",
                        "IV Phenylephrine bolus only",
                        "IV Isoproterenol infusion"
                    ),
                    correctIndex = 1,
                    rationale = "Norepinephrine is the undisputed first-line vasopressor in septic shock due to potent alpha-1 vasoconstriction with modest beta-1 inotropy and lower arrhythmia risk than Dopamine.",
                    criticalPearl = "Septic shock vasopressor ladder: 1st = Norepinephrine; 2nd add-on = Vasopressin (0.03 units/min); Target MAP >= 65 mmHg."
                ),
                ClinicalCaseStage(
                    stageNumber = 4,
                    stageTitle = "Vasopressor Extravasation Emergency",
                    elapsedTimeLabel = "T+80 min",
                    vitals = BedsideVitals(
                        hr = "104 bpm",
                        bp = "102/64",
                        spo2 = "97% 2L O2",
                        rr = "20/min",
                        temp = "38.2°C",
                        gcs = "15/15",
                        deltaChips = listOf("MAP 77 ↑", "IV Site Blanched")
                    ),
                    clinicalUpdate = "While Norepinephrine was temporarily running through a proximal peripheral IV before central access finished, the nurse detects cool, pale, blanched, firm swelling around the catheter tip.",
                    labOrMonitorClue = "Capillary refill delayed over dorsal forearm; severe alpha-1 local vasoconstriction risk of tissue necrosis.",
                    prompt = "After immediately stopping the peripheral infusion (leaving the cannula in place briefly to aspirate) and shifting Norepinephrine to the central line, which specific local antidote should be prepared?",
                    options = listOf(
                        "Subcutaneous infiltration of Phentolamine (5–10 mg diluted in 10–15 mL NS)",
                        "Local application of ice packs and IV Protamine sulfate",
                        "Subcutaneous injection of 10% Calcium gluconate",
                        "Local infiltration of Atropine 1 mg"
                    ),
                    correctIndex = 0,
                    rationale = "Phentolamine (alpha-adrenergic antagonist) infiltrated locally into the blanched tissue reverses Norepinephrine-induced vasospasm and prevents dermal necrosis. Apply warm (not cold) compresses after vasopressor extravasation.",
                    criticalPearl = "Vasopressor extravasation antidote = Phentolamine (Regitine) infiltrated locally + warm compress."
                )
            ),
            debriefPearls = listOf(
                "Always draw blood cultures BEFORE the first antibiotic dose, never delaying antibiotics beyond 1 hour.",
                "Norepinephrine targeting MAP >= 65 mmHg is first-line for fluid-refractory septic shock.",
                "Monitor peripheral vasopressor sites q15–30min; keep Phentolamine ready for extravasation blanching."
            )
        ),

        // 3. DKA & Hypokalemia Transition (4 stages)
        UnfoldingClinicalCase(
            id = "case_dka_transition",
            title = "Diabetic Ketoacidosis (DKA) & Potassium Trap",
            specialty = SPECIALTY_ICU_MSN,
            subjectCode = "MSN",
            acuityTag = "HIGH-ALERT",
            examBadge = "NORCET Mains",
            patientProfile = "22F · Type 1 DM · Missed Insulin 48 hrs",
            chiefComplaint = "Fruity breath, deep rapid breathing, abdominal pain, and persistent vomiting",
            stages = listOf(
                ClinicalCaseStage(
                    stageNumber = 1,
                    stageTitle = "Initial Resuscitation Priority",
                    elapsedTimeLabel = "T+00 min",
                    vitals = BedsideVitals(
                        hr = "124 bpm",
                        bp = "92/58",
                        spo2 = "98% RA",
                        rr = "32/min",
                        temp = "37.1°C",
                        gcs = "14/15",
                        deltaChips = listOf("Kussmaul RR 32 ↑", "CBG 560 mg/dL")
                    ),
                    clinicalUpdate = "Mucous membranes are parched, skin turgor is poor, and patient demonstrates deep, rapid Kussmaul respirations with acetone odor.",
                    labOrMonitorClue = "ABG: pH 7.16, PaCO2 22 mmHg, HCO3- 8 mEq/L, Anion Gap 24; Serum K+ pending; Urine ketones 4+.",
                    prompt = "What is the VERY FIRST IV therapy the nurse must initiate in DKA?",
                    options = listOf(
                        "IV 0.9% Normal Saline (isotonic crystalloid) bolus 1 L over the first hour",
                        "IV Regular Insulin 0.1 unit/kg/hr before any IV fluids",
                        "IV Sodium Bicarbonate 100 mEq push immediately",
                        "IV 5% Dextrose in Water at 200 mL/hr"
                    ),
                    correctIndex = 0,
                    rationale = "Intravascular volume restoration with 0.9% Normal Saline ALWAYS precedes insulin in DKA to restore renal perfusion, lower counter-regulatory hormones, and prevent vascular collapse.",
                    criticalPearl = "DKA Sequence: 1) Isotonic Fluids (0.9% NS) -> 2) Check Serum K+ -> 3) IV Regular Insulin -> 4) Add Dextrose at CBG <= 250."
                ),
                ClinicalCaseStage(
                    stageNumber = 2,
                    stageTitle = "The Fatal Hypokalemia Trap",
                    elapsedTimeLabel = "T+30 min",
                    vitals = BedsideVitals(
                        hr = "116 bpm",
                        bp = "104/66",
                        spo2 = "99% RA",
                        rr = "28/min",
                        temp = "37.0°C",
                        gcs = "15/15",
                        deltaChips = listOf("BP 104/66 ↑", "Serum K+ 3.0 ↓↓")
                    ),
                    clinicalUpdate = "After 1 L of 0.9% NS, blood pressure improved and urine output is 45 mL/hr. The STAT serum electrolyte panel returns.",
                    labOrMonitorClue = "Serum K+ 3.0 mEq/L, Na+ 131 mEq/L, CBG 510 mg/dL; ECG shows flattened T waves and prominent U waves.",
                    prompt = "The physician's standing order includes an IV Regular Insulin drip. What is the nurse's CRITICAL safety action?",
                    options = listOf(
                        "Start IV Regular Insulin immediately to bring blood glucose below 300 mg/dL",
                        "HOLD IV Regular Insulin, alert the physician, and initiate IV Potassium Chloride replacement (20–30 mEq/hr) until K+ >= 3.3 mEq/L",
                        "Administer Potassium Chloride 40 mEq IV push undiluted over 2 minutes",
                        "Give Subcutaneous NPH insulin instead of IV Regular Insulin"
                    ),
                    correctIndex = 1,
                    rationale = "Insulin drives K+ rapidly into cells. Starting insulin when serum K+ is < 3.3 mEq/L can trigger lethal hypokalemic ventricular arrhythmias and respiratory arrest. Never give KCl as IV push.",
                    criticalPearl = "NEVER start insulin in DKA if K+ < 3.3 mEq/L! Replace K+ first (confirm urine output >= 30 mL/hr; max peripheral rate 10 mEq/hr)."
                ),
                ClinicalCaseStage(
                    stageNumber = 3,
                    stageTitle = "Preventing Cerebral Edema & Hypoglycemia",
                    elapsedTimeLabel = "T+05 hrs",
                    vitals = BedsideVitals(
                        hr = "92 bpm",
                        bp = "114/72",
                        spo2 = "99% RA",
                        rr = "20/min",
                        temp = "36.9°C",
                        gcs = "15/15",
                        deltaChips = listOf("CBG 235 ↓", "pH 7.26 ↑", "K+ 4.1 ✓")
                    ),
                    clinicalUpdate = "Serum K+ is stabilized at 4.1 mEq/L and IV Regular Insulin has lowered blood glucose steadily from 510 to 235 mg/dL, but ketoacidosis has NOT yet resolved.",
                    labOrMonitorClue = "Venous pH 7.26, HCO3- 13 mEq/L, Anion Gap 18 (still open).",
                    prompt = "How should the nurse expect the IV fluid and insulin orders to be adjusted now that CBG reached 235 mg/dL?",
                    options = listOf(
                        "Stop the IV Regular Insulin infusion immediately to prevent hypoglycemia",
                        "Switch IV fluids to 5% Dextrose in 0.45% Saline (D5½NS) AND continue IV Regular Insulin at reduced rate until anion gap closes",
                        "Double the IV Regular Insulin rate to normalize pH faster",
                        "Stop all IV fluids and allow oral fruit juice"
                    ),
                    correctIndex = 1,
                    rationale = "Ketoacidosis clears slower than hyperglycemia. When CBG falls to 200–250 mg/dL, add Dextrose (D5½NS) while continuing IV Regular Insulin so ketogenesis remains suppressed until pH > 7.30, HCO3- >= 15–18, and Anion Gap <= 12.",
                    criticalPearl = "At CBG 200–250 mg/dL in DKA: ADD D5 to IV fluid — DO NOT stop IV insulin until the anion gap closes!"
                ),
                ClinicalCaseStage(
                    stageNumber = 4,
                    stageTitle = "Safe Transition to Subcutaneous Insulin",
                    elapsedTimeLabel = "T+11 hrs",
                    vitals = BedsideVitals(
                        hr = "80 bpm",
                        bp = "118/74",
                        spo2 = "99% RA",
                        rr = "16/min",
                        temp = "36.8°C",
                        gcs = "15/15",
                        deltaChips = listOf("AG 10 ✓", "pH 7.36 ✓", "Tolerating PO")
                    ),
                    clinicalUpdate = "DKA has resolved (pH 7.36, HCO3- 19 mEq/L, Anion Gap 10, CBG 172 mg/dL). Patient is hungry and ready to transition to subcutaneous basal-bolus insulin.",
                    labOrMonitorClue = "Subcutaneous Glargine (long-acting) + Lispro ordered.",
                    prompt = "To prevent rebound DKA during transition, when must the nurse discontinue the IV Regular Insulin infusion?",
                    options = listOf(
                        "Turn off IV Regular Insulin 2 hours BEFORE giving subcutaneous basal insulin",
                        "Administer subcutaneous basal insulin FIRST, then continue IV Regular Insulin overlap for 1–2 hours before stopping the IV drip",
                        "Stop IV Regular Insulin immediately and wait until next morning to give basal insulin",
                        "Mix Glargine and Regular Insulin in the same syringe and stop the IV drip immediately"
                    ),
                    correctIndex = 1,
                    rationale = "IV Regular Insulin has a very short half-life (~5–10 min), while subcutaneous basal insulin takes 1–2 hours to reach therapeutic levels. Overlapping for 1–2 hours prevents rapid rebound ketoacidosis.",
                    criticalPearl = "Always overlap subcutaneous basal insulin with the IV Regular Insulin infusion for 1–2 hours before stopping the drip."
                )
            ),
            debriefPearls = listOf(
                "Fluid resuscitation (0.9% NS) always comes first; verify K+ >= 3.3 mEq/L before starting IV Regular Insulin.",
                "Add D5½NS when CBG reaches 200–250 mg/dL so IV insulin can continue until anion gap <= 12.",
                "Overlap subcutaneous basal insulin by 1–2 hours before turning off the IV insulin infusion to avoid rebound DKA."
            )
        ),

        // 4. Severe Preeclampsia -> Eclampsia & MgSO4 Toxicity (4 stages)
        UnfoldingClinicalCase(
            id = "case_preeclampsia_mgso4",
            title = "Severe Preeclampsia, Eclampsia & MgSO4 Toxicity",
            specialty = SPECIALTY_OBG,
            subjectCode = "OBG",
            acuityTag = "CRITICAL",
            examBadge = "NORCET Mains",
            patientProfile = "26F · Primigravida · 37+4 Wks · Labor Ward",
            chiefComplaint = "Severe frontal headache, flashing visual scotomata, and right upper quadrant pain",
            stages = listOf(
                ClinicalCaseStage(
                    stageNumber = 1,
                    stageTitle = "Imminent Eclampsia & Pritchard Regimen",
                    elapsedTimeLabel = "T+00 min",
                    vitals = BedsideVitals(
                        hr = "98 bpm",
                        bp = "172/114",
                        spo2 = "98% RA",
                        rr = "20/min",
                        temp = "36.9°C",
                        gcs = "15/15",
                        deltaChips = listOf("BP 172/114 ↑↑", "DTR 3+ Clonus")
                    ),
                    clinicalUpdate = "Urine dipstick shows 3+ proteinuria, patellar reflexes are 3+ with ankle clonus, and FHR baseline is 142 bpm with moderate variability.",
                    labOrMonitorClue = "Platelets 96,000/µL, AST 94 U/L (evolving HELLP features). Pritchard regimen Magnesium Sulfate ordered.",
                    prompt = "What is the standard Pritchard regimen LOADING DOSE of Magnesium Sulfate for seizure prophylaxis?",
                    options = listOf(
                        "4 g IV (20% solution) slowly over 3–5 min PLUS 10 g deep IM (5 g of 50% solution in each buttock)",
                        "10 g IV rapid push over 1 minute followed by oral MgSO4",
                        "1 g IV slowly once daily only",
                        "20 g IV over 10 minutes without IM injection"
                    ),
                    correctIndex = 0,
                    rationale = "The classic Pritchard loading regimen is 14 g total: 4 g IV (20% solution) given slowly over 3–5 min plus 10 g deep IM (5 g 50% in each gluteal quadrant), followed by 5 g IM q4h in alternate buttocks.",
                    criticalPearl = "Pritchard Loading = 4 g IV + 10 g IM (total 14 g). Maintenance = 5 g IM every 4 hours ONLY if toxicity checks pass."
                ),
                ClinicalCaseStage(
                    stageNumber = 2,
                    stageTitle = "Active Eclamptic Seizure Management",
                    elapsedTimeLabel = "T+08 min",
                    vitals = BedsideVitals(
                        hr = "134 bpm",
                        bp = "180/118",
                        spo2 = "88% RA",
                        rr = "Irregular",
                        temp = "37.0°C",
                        gcs = "8/15",
                        deltaChips = listOf("Tonic-Clonic Seizure", "SpO2 88% ↓")
                    ),
                    clinicalUpdate = "Before the IM loading dose is completed, the mother suddenly develops generalized tonic-clonic convulsions lasting 40 seconds.",
                    labOrMonitorClue = "FHR shows transient late deceleration to 96 bpm during seizure hypoxia.",
                    prompt = "What is the nurse's IMMEDIATE bedside priority during the active eclamptic convulsion?",
                    options = listOf(
                        "Force a wooden tongue depressor between her teeth to prevent tongue biting",
                        "Turn her onto her left lateral side, protect from injury, maintain airway/suction, apply O2 8–10 L/min, and administer IV MgSO4 loading dose",
                        "Rush her directly to the operating room for immediate crash C-section while actively seizing",
                        "Restrain all four limbs tightly in supine position"
                    ),
                    correctIndex = 1,
                    rationale = "Never insert objects into the mouth or restrain limbs during a seizure. Left lateral position prevents aortocaval compression and aspiration; oxygen + IV MgSO4 abort seizures. Stabilize mother BEFORE considering delivery.",
                    criticalPearl = "Eclampsia priority: Left lateral position + Airway/O2 + IV MgSO4 -> Stabilize mother first (fetal bradycardia usually recovers once maternal hypoxia resolves)."
                ),
                ClinicalCaseStage(
                    stageNumber = 3,
                    stageTitle = "Detecting Magnesium Sulfate Toxicity",
                    elapsedTimeLabel = "T+06 hrs",
                    vitals = BedsideVitals(
                        hr = "76 bpm",
                        bp = "144/90",
                        spo2 = "92% O2",
                        rr = "10/min",
                        temp = "36.8°C",
                        gcs = "12/15",
                        deltaChips = listOf("RR 10/min ↓↓", "DTR Absent", "UO 18 mL/hr ↓")
                    ),
                    clinicalUpdate = "The mother is seizure-free on MgSO4 maintenance, but during hourly assessment she is markedly drowsy with slurred speech, absent patellar reflexes, RR 10/min, and urine output 18 mL/hr.",
                    labOrMonitorClue = "STAT serum magnesium returns at 9.8 mg/dL (Therapeutic range: 4–7 mEq/L or 4.8–8.4 mg/dL).",
                    prompt = "Which clinical sign is the EARLIEST bedside indicator of Magnesium Sulfate toxicity, and what must the nurse do immediately?",
                    options = listOf(
                        "Cardiac arrest is the earliest sign; continue MgSO4 at half rate",
                        "Loss of deep tendon (patellar) reflexes is the earliest sign; STOP MgSO4 immediately and give 10% Calcium Gluconate 10 mL (1 g) IV slowly over 3–5 min",
                        "Flushing and warmth are toxicity signs; administer IV Atropine 1 mg",
                        "Elevated BP is the earliest sign; increase MgSO4 infusion"
                    ),
                    correctIndex = 1,
                    rationale = "MgSO4 is renally excreted; oliguria (<30 mL/hr) precipitates toxicity. Earliest clinical sign = loss of patellar reflex (8–10 mEq/L), followed by respiratory depression (<12/min), then cardiac arrest. Stop infusion and give 10% Calcium Gluconate 1 g IV slowly.",
                    criticalPearl = "Before EVERY MgSO4 dose verify 'R-U-P': RR >= 12–16/min, Urine output >= 30 mL/hr (100 mL/4h), Patellar reflex PRESENT."
                ),
                ClinicalCaseStage(
                    stageNumber = 4,
                    stageTitle = "Safe Antihypertensive Selection",
                    elapsedTimeLabel = "T+07 hrs",
                    vitals = BedsideVitals(
                        hr = "84 bpm",
                        bp = "166/112",
                        spo2 = "98% O2",
                        rr = "16/min",
                        temp = "36.8°C",
                        gcs = "15/15",
                        deltaChips = listOf("RR 16 ✓", "BP 166/112 ↑")
                    ),
                    clinicalUpdate = "After Calcium Gluconate, respiratory rate and reflexes normalize, but severe hypertension persists at 166/112 mmHg.",
                    labOrMonitorClue = "Mother has no history of bronchial asthma; FHR 138 bpm reactive.",
                    prompt = "Which antihypertensive drug is FIRST-LINE for acute severe hypertension in pregnancy, and which class is STRICTLY CONTRAINDICATED?",
                    options = listOf(
                        "First-line: IV Labetalol (or Oral Nifedipine / IV Hydralazine); Strictly contraindicated: ACE inhibitors / ARBs (e.g., Enalapril, Losartan)",
                        "First-line: IV Enalaprilat; Contraindicated: Labetalol",
                        "First-line: Oral Losartan; Contraindicated: Methyldopa",
                        "First-line: IV Sodium Nitroprusside for 48 hours; Contraindicated: Nifedipine"
                    ),
                    correctIndex = 0,
                    rationale = "IV Labetalol, IV Hydralazine, and immediate-release oral Nifedipine are first-line for acute hypertensive crisis in pregnancy. ACE inhibitors and ARBs are teratogenic (fetal renal dysplasia, oligohydramnios).",
                    criticalPearl = "Pregnancy HTN Safe Trio: Labetalol, Nifedipine, Methyldopa/Hydralazine. NEVER give ACEi/ARBs."
                )
            ),
            debriefPearls = listOf(
                "Magnesium Sulfate is the drug of choice for seizure prevention/control in eclampsia (NOT an antihypertensive).",
                "Check R-U-P (Respirations >= 12, Urine >= 30 mL/hr, Patellar reflex present) before every maintenance dose.",
                "Always keep 10% Calcium Gluconate (10 mL = 1 g IV slow) immediately available at the bedside."
            )
        ),

        // 5. Primary Postpartum Hemorrhage (Uterine Atony + HTN) (3 stages)
        UnfoldingClinicalCase(
            id = "case_pph_atony",
            title = "Primary PPH: Uterine Atony in Hypertensive Mother",
            specialty = SPECIALTY_OBG,
            subjectCode = "OBG",
            acuityTag = "CRITICAL",
            examBadge = "NORCET Mains",
            patientProfile = "31F · G4P4 · Post-Vaginal Delivery 15 min",
            chiefComplaint = "Sudden gush of 750 mL bright-red vaginal blood with clots after placental delivery",
            stages = listOf(
                ClinicalCaseStage(
                    stageNumber = 1,
                    stageTitle = "First-Line Mechanical & Uterotonic Response",
                    elapsedTimeLabel = "T+00 min",
                    vitals = BedsideVitals(
                        hr = "116 bpm",
                        bp = "158/102",
                        spo2 = "97% RA",
                        rr = "22/min",
                        temp = "36.7°C",
                        gcs = "15/15",
                        deltaChips = listOf("EBL 750 mL ↑", "Boggy Fundus Deviated R")
                    ),
                    clinicalUpdate = "Placenta delivered intact. On abdominal palpation, the uterine fundus is soft, boggy, 2 cm above the umbilicus, and shifted to the right.",
                    labOrMonitorClue = "Known gestational hypertension (BP 158/102 mmHg); Shock Index (HR/SBP) = 0.73.",
                    prompt = "What is the nurse's IMMEDIATE first-line action sequence?",
                    options = listOf(
                        "Perform firm bimanual/fundal massage, catheterize to empty the distended urinary bladder, and start IV Oxytocin infusion",
                        "Pack the vagina tightly with sterile gauze and wait 30 minutes",
                        "Give IV Methylergometrine 0.2 mg push immediately",
                        "Place an ice bag over the lower abdomen without touching the fundus"
                    ),
                    correctIndex = 0,
                    rationale = "Uterine atony causes ~70–80% of primary PPH. A right-deviated boggy fundus indicates a full bladder preventing uterine contraction. Immediate fundal massage, emptying the bladder, and IV Oxytocin are first-line.",
                    criticalPearl = "PPH 4 T's: Tone (most common), Trauma, Tissue, Thrombin. Always check & empty a full bladder when fundus shifts right!"
                ),
                ClinicalCaseStage(
                    stageNumber = 2,
                    stageTitle = "Second-Line Uterotonic Contraindication Trap",
                    elapsedTimeLabel = "T+12 min",
                    vitals = BedsideVitals(
                        hr = "128 bpm",
                        bp = "164/106",
                        spo2 = "95% O2",
                        rr = "26/min",
                        temp = "36.6°C",
                        gcs = "14/15",
                        deltaChips = listOf("EBL 1150 mL ↑", "BP 164/106 ↑")
                    ),
                    clinicalUpdate = "Bladder was emptied (500 mL urine) and IV Oxytocin 20 units in 1 L RL is running, yet uterine atony and heavy trickling persist. Patient has NO history of asthma.",
                    labOrMonitorClue = "Coagulation screen and cross-match sent; BP remains 164/106 mmHg.",
                    prompt = "Which second-line uterotonic must the nurse STRICTLY AVOID in this patient, and which adjunct is safe to prepare?",
                    options = listOf(
                        "Strictly avoid Oxytocin; prepare Methylergometrine",
                        "Strictly avoid Methylergometrine (Methergine) due to severe hypertension; prepare IV Tranexamic Acid 1 g + Misoprostol 800 mcg / IM Carboprost",
                        "Strictly avoid Tranexamic Acid; give high-dose Methylergometrine",
                        "Strictly avoid Misoprostol; give oral Labetalol only"
                    ),
                    correctIndex = 1,
                    rationale = "Methylergometrine (ergot alkaloid) induces intense systemic vasoconstriction and risks hypertensive stroke/eclampsia when BP >= 140/90. IV Tranexamic Acid (1 g within 3 hrs of birth), Misoprostol, or IM Carboprost (since no asthma) are safe.",
                    criticalPearl = "Contraindication Pair (NORCET favorite): Methylergometrine -> Contraindicated in HTN/Preeclampsia; Carboprost (PGF2α) -> Contraindicated in Asthma."
                ),
                ClinicalCaseStage(
                    stageNumber = 3,
                    stageTitle = "Shock Index & Escalation Monitoring",
                    elapsedTimeLabel = "T+25 min",
                    vitals = BedsideVitals(
                        hr = "136 bpm",
                        bp = "88/54",
                        spo2 = "94% O2",
                        rr = "28/min",
                        temp = "36.2°C",
                        gcs = "13/15",
                        deltaChips = listOf("Shock Index 1.54 ↑↑", "BP 88/54 ↓↓")
                    ),
                    clinicalUpdate = "Bleed slows after Bakri intrauterine balloon tamponade and TXA, but maternal BP suddenly drops to 88/54 mmHg with HR 136 bpm and cool clammy skin.",
                    labOrMonitorClue = "Obstetric Shock Index (HR/SBP) = 136 / 88 = 1.54 (Normal obstetric SI < 0.9); Hb 6.8 g/dL.",
                    prompt = "What does an Obstetric Shock Index >= 1.1 indicate, and what is the priority nursing preparation?",
                    options = listOf(
                        "Normal physiological postpartum bradycardia; routine observation",
                        "Decompensated hemorrhagic shock requiring immediate massive transfusion protocol (PRBC:FFP:Platelets) and warmed IV crystalloids",
                        "Isolated anxiety reaction; administer oral Diazepam",
                        "Fluid overload; restrict all IV fluids"
                    ),
                    correctIndex = 1,
                    rationale = "In pregnancy, blood volume expansion masks early blood loss until >1,500 mL is lost. An Obstetric Shock Index >= 0.9 signals severe hypovolemia and >= 1.1 mandates urgent blood product transfusion.",
                    criticalPearl = "Obstetric Shock Index = HR / SBP. Normal < 0.9; >= 0.9 warrants urgent resuscitation; >= 1.1 predicts massive transfusion."
                )
            ),
            debriefPearls = listOf(
                "Primary PPH (>500 mL vaginal / >1000 mL C-section within 24 hrs): Tone (atony) is #1 — massage fundus, empty bladder, give IV Oxytocin.",
                "Memorize contraindications: Methergine = NO in Hypertension; Carboprost = NO in Asthma.",
                "IV Tranexamic Acid 1 g given within 3 hours of delivery significantly reduces PPH mortality (WOMAN trial)."
            )
        ),

        // 6. Blunt Chest Trauma -> Tension Pneumothorax & Chest Tube Care (4 stages)
        UnfoldingClinicalCase(
            id = "case_tension_pneumo",
            title = "Tension Pneumothorax & Chest Tube (ICD) Emergencies",
            specialty = SPECIALTY_EMERGENCY,
            subjectCode = "MSN",
            acuityTag = "CRITICAL",
            examBadge = "NORCET Mains",
            patientProfile = "29M · RTA Steering Wheel Impact · Trauma Bay",
            chiefComplaint = "Severe right chest pain, extreme air hunger, and rapidly dropping blood pressure",
            stages = listOf(
                ClinicalCaseStage(
                    stageNumber = 1,
                    stageTitle = "Clinical Recognition & Immediate Decompression",
                    elapsedTimeLabel = "T+00 min",
                    vitals = BedsideVitals(
                        hr = "138 bpm",
                        bp = "76/46",
                        spo2 = "82% O2",
                        rr = "34/min",
                        temp = "36.6°C",
                        gcs = "13/15",
                        deltaChips = listOf("SpO2 82% ↓↓", "BP 76/46 ↓↓", "Trachea Shifted L")
                    ),
                    clinicalUpdate = "Right hemithorax has absent breath sounds and hyperresonance on percussion; neck veins are distended (JVD) and trachea is deviated to the LEFT.",
                    labOrMonitorClue = "Obstructive shock from mediastinal shift compressing the vena cava.",
                    prompt = "What is the IMMEDIATE life-saving priority action BEFORE obtaining a Chest X-ray?",
                    options = listOf(
                        "Send patient urgently for a Portable Chest X-ray to confirm pneumothorax first",
                        "Assist with immediate needle thoracostomy decompression (large-bore 14G cannula at 4th–5th ICS anterior/mid-axillary line or 2nd ICS mid-clavicular line) followed by chest tube insertion",
                        "Intubate and apply high PEEP immediately without chest decompression",
                        "Administer IV Furosemide 40 mg for JVD"
                    ),
                    correctIndex = 1,
                    rationale = "Tension pneumothorax is a CLINICAL diagnosis — NEVER delay decompression for a Chest X-ray! Immediate needle decompression converts tension into a simple pneumothorax and restores venous return.",
                    criticalPearl = "Tension Pneumothorax Triad: Absent breath sounds + Hyperresonance + Hypotension/JVD + Tracheal deviation AWAY from affected side."
                ),
                ClinicalCaseStage(
                    stageNumber = 2,
                    stageTitle = "Water-Seal Chamber Assessment",
                    elapsedTimeLabel = "T+35 min",
                    vitals = BedsideVitals(
                        hr = "98 bpm",
                        bp = "116/72",
                        spo2 = "96% 4L O2",
                        rr = "20/min",
                        temp = "36.7°C",
                        gcs = "15/15",
                        deltaChips = listOf("BP 116/72 ↑↑", "SpO2 96% ↑↑")
                    ),
                    clinicalUpdate = "A 28-Fr right chest tube connected to a 3-chamber water-seal drainage system is secured. Hemodynamics normalize dramatically.",
                    labOrMonitorClue = "Water-seal chamber shows fluid rising on inspiration and falling on expiration (tidaling), with NO continuous bubbling.",
                    prompt = "How should the nurse interpret gentle tidaling in the water-seal chamber?",
                    options = listOf(
                        "It indicates an active air leak requiring immediate chest tube clamping",
                        "It is a NORMAL expected finding showing patent tubing transmitting intrapleural pressure changes",
                        "It proves the suction regulator is broken",
                        "It indicates the lung has completely collapsed again"
                    ),
                    correctIndex = 1,
                    rationale = "Gentle tidaling (oscillation with respirations) confirms tube patency and normal intrapleural pressure transmission. Continuous vigorous bubbling in the water-seal chamber (NOT suction control chamber) signals an air leak.",
                    criticalPearl = "Water-Seal Chamber: Tidaling = NORMAL; Continuous bubbling = AIR LEAK. Suction Control Chamber: Gentle continuous bubbling = NORMAL."
                ),
                ClinicalCaseStage(
                    stageNumber = 3,
                    stageTitle = "Water-Seal Tubing Disconnection",
                    elapsedTimeLabel = "T+04 hrs",
                    vitals = BedsideVitals(
                        hr = "112 bpm",
                        bp = "110/68",
                        spo2 = "93% 4L O2",
                        rr = "24/min",
                        temp = "36.8°C",
                        gcs = "15/15",
                        deltaChips = listOf("Tubing Disconnected", "SpO2 93% ↓")
                    ),
                    clinicalUpdate = "While turning in bed, the chest tube accidentally disconnects from the drainage unit tubing (the tube itself remains inside the chest wall).",
                    labOrMonitorClue = "Open tube end exposed to atmospheric air; bedside bottle of sterile water is immediately available.",
                    prompt = "What is the nurse's IMMEDIATE first action?",
                    options = listOf(
                        "Clamp the chest tube tightly for 30 minutes until the surgeon arrives",
                        "Immediately submerge the open distal end of the chest tube 1–2 inches (2–4 cm) into a bottle of sterile water to re-establish a temporary water seal",
                        "Pull the chest tube completely out of the chest",
                        "Tape the open tube end tightly with non-porous adhesive tape"
                    ),
                    correctIndex = 1,
                    rationale = "Submerging the disconnected tubing 1–2 inches into sterile water immediately restores a one-way water seal (air can escape, cannot enter) without risking tension pneumothorax from prolonged clamping.",
                    criticalPearl = "Disconnected tubing = Submerge 1–2 inches in sterile water immediately (never leave clamped long-term)."
                ),
                ClinicalCaseStage(
                    stageNumber = 4,
                    stageTitle = "Accidental Complete Chest Tube Dislodgement",
                    elapsedTimeLabel = "T+08 hrs",
                    vitals = BedsideVitals(
                        hr = "120 bpm",
                        bp = "102/64",
                        spo2 = "90% 4L O2",
                        rr = "28/min",
                        temp = "36.8°C",
                        gcs = "15/15",
                        deltaChips = listOf("Tube Out of Chest", "Sucking Sound")
                    ),
                    clinicalUpdate = "Later that evening, the agitated patient pulls the entire chest tube completely out of his chest wall, leaving an open sucking chest wound.",
                    labOrMonitorClue = "Audible air movement through the chest wall insertion site on inspiration.",
                    prompt = "What dressing technique must the nurse apply IMMEDIATELY to prevent tension pneumothorax?",
                    options = listOf(
                        "Apply a sterile petrolatum/occlusive gauze taped securely on ALL FOUR sides",
                        "Apply a sterile occlusive dressing taped on THREE sides only (creating a flutter valve) and notify the physician immediately",
                        "Leave the wound open to air so air can circulate freely",
                        "Attempt to re-insert the pulled-out chest tube back into the incision"
                    ),
                    correctIndex = 1,
                    rationale = "Taping an occlusive dressing on THREE sides creates a one-way flutter valve: it seals shut on inspiration (preventing air entry) and opens on expiration (allowing trapped pleural air to escape, preventing tension pneumothorax).",
                    criticalPearl = "Tube pulled OUT of chest = Sterile occlusive dressing taped on 3 SIDES only (never re-insert; never seal 4 sides tightly)."
                )
            ),
            debriefPearls = listOf(
                "Never wait for a Chest X-ray in tension pneumothorax — decompress immediately.",
                "Water-seal tidaling is normal; continuous bubbling in water-seal = air leak.",
                "Tube disconnected from drainage -> submerge in sterile water; Tube pulled out of chest -> 3-sided occlusive dressing."
            )
        ),

        // 7. Pediatric Severe Dehydration & WHO Plan C (3 stages)
        UnfoldingClinicalCase(
            id = "case_peds_dehydration",
            title = "Pediatric Severe Dehydration & WHO Plan C",
            specialty = SPECIALTY_PEDS_NICU,
            subjectCode = "Peds",
            acuityTag = "CRITICAL",
            examBadge = "NORCET Mains",
            patientProfile = "18-Month-Old · Weight 10 kg · Pediatric ER",
            chiefComplaint = "14 episodes of watery rice-water stools and vomiting over 18 hours",
            stages = listOf(
                ClinicalCaseStage(
                    stageNumber = 1,
                    stageTitle = "IMNCI Classification & Plan C Bolus",
                    elapsedTimeLabel = "T+00 min",
                    vitals = BedsideVitals(
                        hr = "168 bpm",
                        bp = "68/42",
                        spo2 = "96% RA",
                        rr = "42/min",
                        temp = "37.4°C",
                        gcs = "11/15",
                        deltaChips = listOf("Lethargic", "Skin Pinch >2s", "Cap Refill 4s")
                    ),
                    clinicalUpdate = "Child is lethargic, unable to drink, eyes are deeply sunken, radial pulse is thready, and abdominal skin pinch goes back very slowly (>2 seconds).",
                    labOrMonitorClue = "IMNCI Classification: Severe Dehydration -> WHO Plan C IV Rehydration (Age >= 12 months, Weight 10 kg).",
                    prompt = "Per WHO/IMNCI Plan C for a child AGED >= 12 MONTHS weighing 10 kg, what is the EXACT IV Ringer's Lactate regimen?",
                    options = listOf(
                        "30 mL/kg (300 mL) over 30 minutes, THEN 70 mL/kg (700 mL) over 2.5 hours (Total 100 mL/kg over 3 hours)",
                        "30 mL/kg (300 mL) over 1 hour, THEN 70 mL/kg (700 mL) over 5 hours (Total 6 hours)",
                        "5% Dextrose 500 mL over 1 hour",
                        "Oral ORS 50 mL/kg over 4 hours only"
                    ),
                    correctIndex = 0,
                    rationale = "WHO Plan C total is 100 mL/kg RL: Infants (<12 mo) get 30 mL/kg in 1 hr + 70 mL/kg in 5 hrs (6 hrs total). Older children (>=12 mo, like this 18-mo child) get 30 mL/kg in 30 min + 70 mL/kg in 2.5 hrs (3 hrs total).",
                    criticalPearl = "WHO Plan C (100 mL/kg RL): <12 mo = 1 hr + 5 hr (6 hrs); >=12 mo = 30 min + 2.5 hr (3 hrs)."
                ),
                ClinicalCaseStage(
                    stageNumber = 2,
                    stageTitle = "Transition to Low-Osmolarity ORS & Zinc",
                    elapsedTimeLabel = "T+03 hrs",
                    vitals = BedsideVitals(
                        hr = "122 bpm",
                        bp = "90/56",
                        spo2 = "99% RA",
                        rr = "28/min",
                        temp = "37.0°C",
                        gcs = "15/15",
                        deltaChips = listOf("Alert ✓", "Passed Urine ✓", "Drinking Eagerly")
                    ),
                    clinicalUpdate = "After 1,000 mL IV Ringer's Lactate, perfusion normalizes, child is alert, passed 40 mL urine, and drinks ORS eagerly.",
                    labOrMonitorClue = "WHO Low-Osmolarity ORS composition & IAP Zinc supplementation protocol ordered.",
                    prompt = "What is the exact total osmolarity of WHO Low-Osmolarity ORS and the recommended Zinc dose for this 18-month-old child?",
                    options = listOf(
                        "311 mOsm/L ORS + Zinc 5 mg daily for 3 days",
                        "245 mOsm/L ORS (Na+ 75, Glucose 75, K+ 20, Cl- 65, Citrate 10 mmol/L) + Zinc 20 mg daily for 14 days",
                        "180 mOsm/L ORS + Zinc 50 mg single dose",
                        "245 mOsm/L ORS + Zinc 10 mg daily for 5 days"
                    ),
                    correctIndex = 1,
                    rationale = "WHO Reduced-Osmolarity ORS has a total osmolarity of 245 mOsm/L (Na+ 75, Glucose 75). Zinc dose: 10 mg/day for infants <6 months and 20 mg/day for children >=6 months, given for 14 days.",
                    criticalPearl = "WHO Low-Osmolarity ORS = 245 mOsm/L (Na+ 75, Glucose 75). Zinc = 10 mg (<6 mo) / 20 mg (>=6 mo) × 14 days."
                ),
                ClinicalCaseStage(
                    stageNumber = 3,
                    stageTitle = "Home Care & Rehydration Safety Teaching",
                    elapsedTimeLabel = "T+08 hrs",
                    vitals = BedsideVitals(
                        hr = "112 bpm",
                        bp = "94/60",
                        spo2 = "99% RA",
                        rr = "26/min",
                        temp = "36.9°C",
                        gcs = "15/15",
                        deltaChips = listOf("Hydrated ✓", "Discharge Prep")
                    ),
                    clinicalUpdate = "The mother asks how long reconstituted ORS can be safely kept at home and whether she should stop breastfeeding during diarrhea.",
                    labOrMonitorClue = "Discharge counseling under National Diarrhoea Control Programme.",
                    prompt = "Which discharge instruction by the nurse is MOST accurate?",
                    options = listOf(
                        "Stop breastfeeding for 48 hours and boil reconstituted ORS before each feed",
                        "Continue frequent breastfeeding/feeding, give 100–200 mL ORS after each loose stool, and discard unused prepared ORS after 24 hours",
                        "Keep reconstituted ORS in the fridge for up to 72 hours",
                        "Mix 1 ORS packet in 200 mL water for stronger effect"
                    ),
                    correctIndex = 1,
                    rationale = "Never stop breastfeeding during diarrhea. Reconstituted ORS (1 standard 20.5 g packet in 1 liter of clean drinking water) must be discarded after 24 hours and never boiled after mixing.",
                    criticalPearl = "Prepared ORS shelf life = 24 hours; never boil after mixing; never stop breastfeeding."
                )
            ),
            debriefPearls = listOf(
                "WHO Plan C: <12 months = 30 mL/kg in 1 hr + 70 mL/kg in 5 hrs; >=12 months = 30 mL/kg in 30 min + 70 mL/kg in 2.5 hrs.",
                "WHO Low-Osmolarity ORS = 245 mOsm/L (Sodium 75 mmol/L, Glucose 75 mmol/L).",
                "Zinc supplementation (20 mg/day >=6 mo for 14 days) reduces diarrhea duration and recurrence."
            )
        ),

        // 8. Acute COPD Exacerbation & Hypercapnic Respiratory Failure (3 stages)
        UnfoldingClinicalCase(
            id = "case_copd_hypercapnia",
            title = "COPD Exacerbation & Hypercapnic CO2 Narcosis",
            specialty = SPECIALTY_ICU_MSN,
            subjectCode = "MSN",
            acuityTag = "HIGH-ALERT",
            examBadge = "NORCET Mains",
            patientProfile = "66M · GOLD Stage IV COPD · Pulmonology Ward",
            chiefComplaint = "Worsening purulent sputum, wheezing, and acute drowsiness after high-flow O2",
            stages = listOf(
                ClinicalCaseStage(
                    stageNumber = 1,
                    stageTitle = "Oxygen Titration & Venturi Precision",
                    elapsedTimeLabel = "T+00 min",
                    vitals = BedsideVitals(
                        hr = "110 bpm",
                        bp = "142/86",
                        spo2 = "97% 6L Mask",
                        rr = "11/min",
                        temp = "37.8°C",
                        gcs = "12/15",
                        deltaChips = listOf("SpO2 97% (Too High)", "RR 11/min ↓", "Drowsy")
                    ),
                    clinicalUpdate = "A student placed a simple face mask at 6 L/min. Over 20 minutes the patient became sluggish with asterixis (flapping tremor) and shallow respirations.",
                    labOrMonitorClue = "ABG: pH 7.25, PaCO2 68 mmHg, PaO2 104 mmHg, HCO3- 29 mEq/L (Acute-on-chronic respiratory acidosis).",
                    prompt = "What is the nurse's PRIORITY oxygen management action?",
                    options = listOf(
                        "Remove oxygen completely until the patient wakes up",
                        "Switch to a controlled low-flow Venturi mask (24%–28% FiO2) targeting SpO2 88%–92% and alert the physician for NIV",
                        "Increase oxygen to 12 L/min via non-rebreather mask",
                        "Administer IV Morphine 4 mg for air hunger"
                    ),
                    correctIndex = 1,
                    rationale = "Excessive O2 in chronic CO2 retainers worsens V/Q mismatch (Haldane effect) and blunts hypoxic drive, causing CO2 narcosis. Never abruptly stop O2 (risks rebound severe hypoxemia); switch to a Venturi mask targeting 88%–92%.",
                    criticalPearl = "COPD target SpO2 = 88%–92% using a Venturi mask (most accurate fixed-FiO2 delivery device)."
                ),
                ClinicalCaseStage(
                    stageNumber = 2,
                    stageTitle = "BiPAP (NIV) Initiation & Bronchodilators",
                    elapsedTimeLabel = "T+25 min",
                    vitals = BedsideVitals(
                        hr = "106 bpm",
                        bp = "138/84",
                        spo2 = "90% Venturi",
                        rr = "26/min",
                        temp = "37.7°C",
                        gcs = "13/15",
                        deltaChips = listOf("SpO2 90% ✓", "pH 7.26 Acidotic")
                    ),
                    clinicalUpdate = "Patient is rousable, protecting his airway, and cooperative, but respiratory acidosis (pH 7.26, PaCO2 66 mmHg) and accessory muscle use persist.",
                    labOrMonitorClue = "Indication met for Non-Invasive Positive Pressure Ventilation (BiPAP) + nebulized Salbutamol/Ipratropium + IV Hydrocortisone.",
                    prompt = "Why is BiPAP preferred over CPAP in hypercapnic COPD respiratory failure?",
                    options = listOf(
                        "BiPAP provides two pressures (higher IPAP to augment tidal volume and blow off CO2 + lower EPAP to splint airways), whereas CPAP gives only one constant pressure",
                        "BiPAP requires endotracheal intubation before starting",
                        "CPAP delivers 100% oxygen automatically",
                        "BiPAP eliminates the need for bronchodilators"
                    ),
                    correctIndex = 0,
                    rationale = "BiPAP delivers Inspiratory Positive Airway Pressure (IPAP) to reduce work of breathing and enhance CO2 clearance, plus Expiratory Positive Airway Pressure (EPAP) to prevent alveolar collapse — making it gold standard for COPD pH 7.25–7.35.",
                    criticalPearl = "Hypercapnic COPD (pH 7.25–7.35, alert airway) -> BiPAP (IPAP clears CO2, EPAP maintains oxygenation)."
                ),
                ClinicalCaseStage(
                    stageNumber = 3,
                    stageTitle = "Monitoring for NIV Failure",
                    elapsedTimeLabel = "T+90 min",
                    vitals = BedsideVitals(
                        hr = "90 bpm",
                        bp = "128/78",
                        spo2 = "91% BiPAP",
                        rr = "18/min",
                        temp = "37.4°C",
                        gcs = "15/15",
                        deltaChips = listOf("pH 7.34 ↑", "PaCO2 52 ↓", "Alert ✓")
                    ),
                    clinicalUpdate = "Repeat ABG after 1 hour of BiPAP shows marked improvement (pH 7.34, PaCO2 52 mmHg) and asterixis has resolved.",
                    labOrMonitorClue = "Nurse prepares bedside NIV safety checklist for the night shift.",
                    prompt = "Which finding during BiPAP therapy would be an ABSOLUTE indication to stop NIV and prepare for immediate endotracheal intubation?",
                    options = listOf(
                        "SpO2 reading 90% on 28% FiO2",
                        "Worsening obtundation (GCS <= 8), inability to protect airway/clear copious secretions, or persistent vomiting",
                        "Mild nasal bridge redness from the mask",
                        "Decrease in PaCO2 from 68 to 54 mmHg"
                    ),
                    correctIndex = 1,
                    rationale = "NIV requires an intact gag/cough reflex and airway protection. Coma (GCS <= 8), refractory vomiting (massive aspiration risk), hemodynamic instability, or worsening pH after 1–2 hrs mandate intubation.",
                    criticalPearl = "Always recheck ABG 1–2 hours after starting BiPAP; obtundation or vomiting = immediate NIV stop & intubation."
                )
            ),
            debriefPearls = listOf(
                "Target SpO2 88%–92% in COPD via Venturi mask; never abruptly disconnect O2.",
                "BiPAP (IPAP + EPAP) is first-line ventilatory support for hypercapnic COPD acidosis (pH 7.25–7.35).",
                "Protect nasal bridge from pressure injury and reassess ABG & mental status within 1–2 hours of NIV start."
            )
        ),

        // 9. Acute Ischemic Stroke Thrombolysis & Raised ICP (4 stages)
        UnfoldingClinicalCase(
            id = "case_stroke_icp",
            title = "Acute Stroke Thrombolysis Window & ICP Crisis",
            specialty = SPECIALTY_EMERGENCY,
            subjectCode = "MSN",
            acuityTag = "CRITICAL",
            examBadge = "NORCET Mains",
            patientProfile = "62M · HTN · Atrial Fibrillation · Stroke ER",
            chiefComplaint = "Sudden right-sided facial droop, arm weakness, and slurred speech 80 minutes ago",
            stages = listOf(
                ClinicalCaseStage(
                    stageNumber = 1,
                    stageTitle = "Code Stroke Diagnostic Priority",
                    elapsedTimeLabel = "T+00 min",
                    vitals = BedsideVitals(
                        hr = "96 irreg",
                        bp = "194/106",
                        spo2 = "97% RA",
                        rr = "18/min",
                        temp = "36.9°C",
                        gcs = "14/15",
                        deltaChips = listOf("Onset 80 min", "NIHSS 14", "BP 194/106 ↑")
                    ),
                    clinicalUpdate = "Last known well was exactly 80 minutes ago. Point-of-care capillary blood glucose is 118 mg/dL (ruling out hypoglycemia stroke mimic).",
                    labOrMonitorClue = "Within 4.5-hour IV Alteplase (tPA) thrombolysis window.",
                    prompt = "What is the SINGLE MOST CRITICAL diagnostic test required BEFORE any Aspirin or IV Alteplase can be given?",
                    options = listOf(
                        "STAT Non-Contrast CT Head (within 20–25 min) to differentiate ischemic stroke from intracranial hemorrhage",
                        "Carotid Doppler ultrasound first",
                        "Lumbar puncture to inspect CSF",
                        "Give Aspirin 300 mg immediately while waiting for MRI next morning"
                    ),
                    correctIndex = 0,
                    rationale = "Non-contrast CT Head is the fastest, gold-standard test to rule out hemorrhagic stroke. Giving tPA or aspirin before ruling out hemorrhage can be fatal.",
                    criticalPearl = "Code Stroke Milestones: Check CBG immediately -> Non-Contrast CT <= 20 min -> IV Alteplase within 4.5 hrs (Door-to-Needle <= 60 min)."
                ),
                ClinicalCaseStage(
                    stageNumber = 2,
                    stageTitle = "Pre-Thrombolysis Blood Pressure Threshold",
                    elapsedTimeLabel = "T+22 min",
                    vitals = BedsideVitals(
                        hr = "92 irreg",
                        bp = "196/112",
                        spo2 = "98% RA",
                        rr = "18/min",
                        temp = "36.9°C",
                        gcs = "14/15",
                        deltaChips = listOf("CT: No Bleed ✓", "BP 196/112 ↑↑")
                    ),
                    clinicalUpdate = "Non-contrast CT Head shows NO hemorrhage and no large hypodensity. Neurologist clears patient for IV Alteplase (0.9 mg/kg).",
                    labOrMonitorClue = "BP remains 196/112 mmHg on two consecutive cuff checks.",
                    prompt = "Before starting IV Alteplase, what BP threshold MUST be achieved (typically with IV Labetalol or Nicardipine)?",
                    options = listOf(
                        "Lower BP aggressively to < 110/70 mmHg immediately",
                        "Lower BP safely to < 185/110 mmHg before bolus, and maintain < 180/105 mmHg for 24 hours post-tPA",
                        "Do not treat BP unless systolic exceeds 240 mmHg",
                        "Give Sublingual Nifedipine 20 mg immediately"
                    ),
                    correctIndex = 1,
                    rationale = "AHA Stroke Guidelines strictly mandate BP < 185/110 mmHg BEFORE initiating IV Alteplase and < 180/105 mmHg during/after infusion to prevent symptomatic hemorrhagic transformation (never give sublingual Nifedipine due to precipitous drops).",
                    criticalPearl = "tPA BP Rule: < 185/110 BEFORE tPA; < 180/105 AFTER tPA. (If NOT receiving tPA, permissive HTN up to 220/120)."
                ),
                ClinicalCaseStage(
                    stageNumber = 3,
                    stageTitle = "Hemorrhagic Transformation & Cushing's Triad",
                    elapsedTimeLabel = "T+55 min",
                    vitals = BedsideVitals(
                        hr = "52 bpm",
                        bp = "208/94",
                        spo2 = "95% RA",
                        rr = "10 irreg",
                        temp = "37.0°C",
                        gcs = "9/15",
                        deltaChips = listOf("HR 52 ↓↓", "Pulse Pressure 114 ↑↑", "GCS 9 ↓")
                    ),
                    clinicalUpdate = "Forty minutes into the Alteplase infusion, the patient cries out with sudden severe headache, vomits once, becomes lethargic, and develops a dilated sluggish left pupil.",
                    labOrMonitorClue = "Classic Cushing's Triad (Widening pulse pressure + Bradycardia + Irregular respirations) indicating acute raised ICP / intracranial bleed.",
                    prompt = "What is the nurse's IMMEDIATE first action?",
                    options = listOf(
                        "Slow the Alteplase infusion to half speed and recheck in 15 minutes",
                        "STOP the Alteplase infusion IMMEDIATELY, notify neurologist, elevate HOB 30° with head midline, and prepare for STAT repeat CT Head + Cryoprecipitate",
                        "Place the patient flat in Trendelenburg position",
                        "Give IV Heparin bolus to prevent re-occlusion"
                    ),
                    correctIndex = 1,
                    rationale = "Sudden neurological deterioration, severe headache, vomiting, or Cushing's triad during tPA indicates symptomatic intracranial hemorrhage. Immediately STOP tPA, maintain HOB 30° neutral neck alignment, obtain STAT CT, and prepare reversal (Cryoprecipitate/TXA).",
                    criticalPearl = "Cushing's Triad (Late sign of raised ICP/herniation): Widening Pulse Pressure (High SBP) + Bradycardia + Irregular Respirations."
                ),
                ClinicalCaseStage(
                    stageNumber = 4,
                    stageTitle = "ICP Nursing Care & Dysphagia Safety",
                    elapsedTimeLabel = "T+06 hrs",
                    vitals = BedsideVitals(
                        hr = "68 bpm",
                        bp = "156/86",
                        spo2 = "98% O2",
                        rr = "16/min",
                        temp = "37.1°C",
                        gcs = "13/15",
                        deltaChips = listOf("ICP Stabilized ✓", "HOB 30° Midline")
                    ),
                    clinicalUpdate = "Small petechial hemorrhage is managed conservatively in Neuro ICU with IV Mannitol and BP control. Patient wakes up and asks for a sip of water.",
                    labOrMonitorClue = "Bedside swallow screen has NOT yet been performed.",
                    prompt = "Which nursing action is ESSENTIAL before giving any oral water, food, or oral medications to ANY acute stroke patient?",
                    options = listOf(
                        "Give a small glass of thin water through a straw to test coughing",
                        "Keep patient strictly NPO until a formal bedside dysphagia / swallow screening is passed",
                        "Give thickened juice immediately without screening",
                        "Place patient supine while feeding"
                    ),
                    correctIndex = 1,
                    rationale = "Aspiration pneumonia is a leading cause of death after stroke. All acute stroke patients MUST remain strictly NPO (even for oral pills/water) until a validated dysphagia swallow screen is completed and passed.",
                    criticalPearl = "Stroke Safety Rule: Strictly NPO until bedside swallow screening is passed; keep HOB 30° and neck neutral to promote venous drainage."
                )
            ),
            debriefPearls = listOf(
                "Always check fingerstick glucose first (hypoglycemia mimics stroke) and obtain Non-Contrast CT Head before any antithrombotic.",
                "tPA BP target: < 185/110 pre-infusion, < 180/105 post-infusion; stop tPA immediately for acute headache, vomiting, or GCS drop.",
                "Cushing's triad = Widening pulse pressure + Bradycardia + Irregular respirations; keep stroke patients NPO until swallow screen passes."
            )
        ),

        // 10. Preterm Neonatal RDS -> Phototherapy Escalation (3 stages)
        UnfoldingClinicalCase(
            id = "case_neonatal_rds_jaundice",
            title = "Preterm Neonatal RDS to Phototherapy Escalation",
            specialty = SPECIALTY_PEDS_NICU,
            subjectCode = "Peds",
            acuityTag = "HIGH-ALERT",
            examBadge = "NORCET Mains",
            patientProfile = "Preterm Neonate · 31 Wks · Birth Wt 1.45 kg · NICU",
            chiefComplaint = "Expiratory grunting, nasal flaring, and subcostal retractions 20 minutes after birth",
            stages = listOf(
                ClinicalCaseStage(
                    stageNumber = 1,
                    stageTitle = "Hyaline Membrane Disease & Early CPAP",
                    elapsedTimeLabel = "Hour 01",
                    vitals = BedsideVitals(
                        hr = "158 bpm",
                        bp = "46/28",
                        spo2 = "86% RA",
                        rr = "68/min",
                        temp = "36.2°C",
                        gcs = "Active",
                        deltaChips = listOf("RR 68 ↑", "Grunting", "Silverman 6/10")
                    ),
                    clinicalUpdate = "Preterm newborn (mother received no antenatal steroids) exhibits marked expiratory grunting, xiphoid retractions, and acrocyanosis.",
                    labOrMonitorClue = "Chest X-ray shows diffuse reticulogranular 'ground-glass' appearance with air bronchograms (Neonatal RDS / Surfactant deficiency).",
                    prompt = "What is the nurse's PRIORITY respiratory and thermoregulatory support?",
                    options = listOf(
                        "Place under radiant warmer (target skin temp 36.5–37.5°C), initiate Bubble Nasal CPAP (5–6 cm H2O) targeting SpO2 90%–95%, and prepare for intratracheal Surfactant",
                        "Give 100% FiO2 via hood targeting SpO2 100% to maximize oxygen",
                        "Bathe the newborn immediately with warm water to stimulate crying",
                        "Keep newborn flat in an open crib without respiratory support"
                    ),
                    correctIndex = 0,
                    rationale = "Preterm RDS (surfactant deficiency) causes alveolar collapse; early Nasal CPAP keeps alveoli open, exogenous surfactant replaces dipalmitoylphosphatidylcholine, and targeting SpO2 90%–95% prevents Retinopathy of Prematurity (ROP). Avoid cold stress (increases O2 demand).",
                    criticalPearl = "Preterm RDS X-ray = 'Ground-glass' + air bronchograms. Target SpO2 90%–95% (avoid hyperoxia -> ROP!)."
                ),
                ClinicalCaseStage(
                    stageNumber = 2,
                    stageTitle = "Intensive Phototherapy Nursing Care",
                    elapsedTimeLabel = "Day 03",
                    vitals = BedsideVitals(
                        hr = "144 bpm",
                        bp = "52/32",
                        spo2 = "93% CPAP",
                        rr = "46/min",
                        temp = "36.9°C",
                        gcs = "Active",
                        deltaChips = listOf("TSB 16.4 mg/dL ↑", "Blue LED Light")
                    ),
                    clinicalUpdate = "Respiratory status stabilizes on low CPAP, but on Day 3 the infant develops jaundice extending to the palms and soles.",
                    labOrMonitorClue = "Total Serum Bilirubin (TSB) = 16.4 mg/dL (unconjugated); Intensive Blue LED Phototherapy (460–490 nm) initiated.",
                    prompt = "Which nursing measure is ESSENTIAL during intensive neonatal phototherapy?",
                    options = listOf(
                        "Apply baby oil/lotion generously to the infant's skin under the lights",
                        "Shield BOTH eyes with opaque patches and cover genitalia, maximize skin exposure, monitor temperature/hydration closely, and remove eye patches briefly during feeds to inspect eyes",
                        "Turn off phototherapy for 4 hours every shift to rest the skin",
                        "Measure serum bilirubin with the phototherapy lights shining directly onto the blood collection tube"
                    ),
                    correctIndex = 1,
                    rationale = "Blue light (460–490 nm) converts unconjugated bilirubin into water-soluble photoisomers (lumirubin). Eye patches prevent retinal damage, no oils/lotions (cause skin burns), turn OFF lights when drawing blood (light degrades bilirubin in tube giving falsely low values).",
                    criticalPearl = "Phototherapy Rules: Eye & gonad shields ON, NO oils/lotions, turn lights OFF while drawing TSB blood sample, monitor loose greenish stools & hydration."
                ),
                ClinicalCaseStage(
                    stageNumber = 3,
                    stageTitle = "Recognizing Acute Bilirubin Encephalopathy (Kernicterus)",
                    elapsedTimeLabel = "Day 04",
                    vitals = BedsideVitals(
                        hr = "140 bpm",
                        bp = "54/34",
                        spo2 = "94% RA",
                        rr = "42/min",
                        temp = "37.0°C",
                        gcs = "Alert ✓",
                        deltaChips = listOf("TSB 11.2 ↓", "Weaned off CPAP")
                    ),
                    clinicalUpdate = "Bilirubin trends down smoothly to 11.2 mg/dL with intensive phototherapy. The NICU nurse reviews escalation red flags with the junior staff nurse.",
                    labOrMonitorClue = "Exchange transfusion threshold watch checklist.",
                    prompt = "Which neurological cluster represents classic Acute Bilirubin Encephalopathy (Kernicterus) requiring emergent Double-Volume Exchange Transfusion?",
                    options = listOf(
                        "Pink skin, strong rooting reflex, and sleeping 3 hours after feeding",
                        "High-pitched shrill cry, lethargy/poor sucking, hypertonia with opisthotonus (arched back/retrocollis), and sunsetting eyes",
                        "Mild physiological weight loss of 5% on Day 3",
                        "Soft green transitional stools"
                    ),
                    correctIndex = 1,
                    rationale = "Unconjugated bilirubin crosses the immature blood-brain barrier and deposits in the basal ganglia, causing lethargy, shrill high-pitched cry, opisthotonus (backward arching), fever, and seizures (BIND score).",
                    criticalPearl = "Kernicterus warning triad: High-pitched cry + Opisthotonus (hypertonia) + Lethargy/poor feeding -> Emergent Double-Volume Exchange Transfusion."
                )
            ),
            debriefPearls = listOf(
                "Preterm RDS: Ground-glass X-ray, early Nasal CPAP + Surfactant, target SpO2 90%–95% to prevent ROP.",
                "Phototherapy (460–490 nm): Protect eyes/genitalia, avoid skin oils, and switch OFF lights during TSB blood draw.",
                "Watch for Kernicterus red flags (high-pitched cry, opisthotonus, poor feeding) -> urgent exchange transfusion."
            )
        )
    )

    fun computeCaseScorePercent(correctStages: Int, totalStages: Int): Int {
        if (totalStages <= 0) return 0
        return ((correctStages.coerceIn(0, totalStages) * 100.0) / totalStages.toDouble()).toInt()
    }

    /**
     * Anti-farming XP formula:
     * - First completion of a clinical case awards 40 base XP + 10 XP per correct stage.
     * - Replays award 0 XP (full practice allowed without XP inflation).
     */
    fun xpForCaseCompletion(
        correctStages: Int,
        totalStages: Int,
        alreadyCompleted: Boolean
    ): Int {
        if (alreadyCompleted || totalStages <= 0) return 0
        val clamped = correctStages.coerceIn(0, totalStages)
        return 40 + (clamped * 10)
    }

    fun serializeCompletedCaseIds(ids: Set<String>): String =
        ids.filter { it.isNotBlank() }.sorted().joinToString(",")

    fun deserializeCompletedCaseIds(raw: String?): Set<String> {
        if (raw.isNullOrBlank()) return emptySet()
        return raw.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
    }
}
