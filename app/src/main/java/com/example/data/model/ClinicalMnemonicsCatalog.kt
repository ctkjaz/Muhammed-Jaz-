package com.example.data.model

import java.util.Locale

/**
 * ClinEdge v2.8.0 — Offline High-Yield Clinical Mnemonics & Rapid Revision Catalog.
 *
 * Curated for Indian Nursing Officer recruitment exams (RRB Nursing Superintendent,
 * AIIMS NORCET, PGIMER, ESIC, JIPMER, DSSSB).
 */
enum class MnemonicCategory(val label: String) {
    EMERGENCY("Emergency & ICU"),
    PHARMACOLOGY("Pharmacology"),
    MSN("Med-Surg"),
    OBG_PEDS("OBG & Peds"),
    FUNDAMENTALS("Fundamentals"),
    COMMUNITY_PSYCH("CHN & Psych")
}

data class MnemonicStep(
    val letter: String,
    val meaning: String
)

data class ClinicalMnemonic(
    val id: String,
    val acronym: String,
    val title: String,
    val category: MnemonicCategory,
    val examTags: List<String>,
    val steps: List<MnemonicStep>,
    val examTrap: String
)

object ClinicalMnemonicsCatalog {

    val ALL_MNEMONICS: List<ClinicalMnemonic> = listOf(
        // ==================== EMERGENCY & ICU ====================
        ClinicalMnemonic(
            id = "MN_MONA",
            acronym = "MONA",
            title = "Acute Coronary Syndrome (MI) Immediate Priority",
            category = MnemonicCategory.EMERGENCY,
            examTags = listOf("NORCET", "RRB", "PGIMER"),
            steps = listOf(
                MnemonicStep("M", "Morphine IV (only if chest pain persists after nitrates; reduces preload & anxiety)"),
                MnemonicStep("O", "Oxygen (give ONLY if SpO2 < 90%, dyspnea, or signs of heart failure)"),
                MnemonicStep("N", "Nitroglycerin sublingual (vasodilator; hold if SBP < 90 mmHg or RV infarct)"),
                MnemonicStep("A", "Aspirin 162–325 mg CHEWED immediately (antiplatelet — top survival priority)")
            ),
            examTrap = "NORCET Trap: Remember the bedside order is O-N-A-M or Aspirin + ECG within 10 min first — NEVER administer Nitroglycerin if the patient took Sildenafil within 24–48 hrs or has SBP < 90 mmHg."
        ),
        ClinicalMnemonic(
            id = "MN_BEFAST",
            acronym = "BE-FAST",
            title = "Acute Stroke Recognition & Thrombolysis Window",
            category = MnemonicCategory.EMERGENCY,
            examTags = listOf("NORCET", "RRB", "ESIC"),
            steps = listOf(
                MnemonicStep("B", "Balance — sudden loss of coordination or dizziness"),
                MnemonicStep("E", "Eyes — sudden diplopia or visual field loss"),
                MnemonicStep("F", "Face — unilateral facial droop when smiling"),
                MnemonicStep("A", "Arm — unilateral arm drift or weakness"),
                MnemonicStep("S", "Speech — slurred speech or aphasia"),
                MnemonicStep("T", "Time — note 'Last Known Well' time for IV Alteplase (< 4.5 hrs)")
            ),
            examTrap = "Priority Trap: A Non-Contrast CT Head MUST be completed first to rule out hemorrhagic stroke before starting IV tPA (Alteplase)."
        ),
        ClinicalMnemonic(
            id = "MN_ABCDE",
            acronym = "ABCDE",
            title = "Primary Trauma & Critical Care Survey",
            category = MnemonicCategory.EMERGENCY,
            examTags = listOf("NORCET", "PGIMER", "JIPMER"),
            steps = listOf(
                MnemonicStep("A", "Airway patency WITH cervical spine immobilization"),
                MnemonicStep("B", "Breathing & ventilation (SpO2, tension pneumothorax check)"),
                MnemonicStep("C", "Circulation & hemorrhage control (2 large-bore 16G/18G IV cannulas)"),
                MnemonicStep("D", "Disability — neurological status (GCS, pupillary reflex, blood glucose)"),
                MnemonicStep("E", "Exposure & Environmental control (undress fully, prevent hypothermia)")
            ),
            examTrap = "Airway Trap: In suspected cervical spine injury, ALWAYS open the airway with the Jaw-Thrust maneuver — NEVER Head-Tilt Chin-Lift."
        ),
        ClinicalMnemonic(
            id = "MN_HT",
            acronym = "5 H's & 5 T's",
            title = "Reversible Causes of Cardiac Arrest (ACLS)",
            category = MnemonicCategory.EMERGENCY,
            examTags = listOf("NORCET", "PGIMER"),
            steps = listOf(
                MnemonicStep("H1–H3", "Hypovolemia · Hypoxia · Hydrogen ion (Acidosis)"),
                MnemonicStep("H4–H5", "Hypo-/Hyperkalemia · Hypothermia"),
                MnemonicStep("T1–T2", "Tension pneumothorax · Tamponade (cardiac)"),
                MnemonicStep("T3–T5", "Toxins · Thrombosis (Pulmonary PE) · Thrombosis (Coronary MI)")
            ),
            examTrap = "ACLS Trap: PEA and Asystole are NON-shockable rhythms — deliver high-quality CPR + Epinephrine 1 mg IV q3–5 min and aggressively treat the H's & T's."
        ),
        ClinicalMnemonic(
            id = "MN_RACE",
            acronym = "RACE & PASS",
            title = "Hospital Fire Safety & Extinguisher Protocol",
            category = MnemonicCategory.EMERGENCY,
            examTags = listOf("RRB", "ESIC", "NORCET"),
            steps = listOf(
                MnemonicStep("R", "Rescue patients in immediate danger FIRST"),
                MnemonicStep("A", "Alarm — pull the fire alarm & notify hospital code red"),
                MnemonicStep("C", "Contain — close doors/windows & shut off O2 zone valves"),
                MnemonicStep("E", "Extinguish / Evacuate using PASS (Pull pin, Aim base, Squeeze, Sweep)")
            ),
            examTrap = "Exam Favorite: If asked what the nurse does FIRST when smoke is seen in a patient room, ALWAYS select 'Rescue / move the patient to safety' before pulling the alarm."
        ),

        // ==================== PHARMACOLOGY ====================
        ClinicalMnemonic(
            id = "MN_RIPES",
            acronym = "RIPES",
            title = "First-Line Anti-Tubercular Drugs (NTEP India)",
            category = MnemonicCategory.PHARMACOLOGY,
            examTags = listOf("RRB", "NORCET", "ESIC", "DSSSB"),
            steps = listOf(
                MnemonicStep("R", "Rifampicin — orange-red urine/tears (harmless), potent CYP450 inducer, hepatotoxic"),
                MnemonicStep("I", "Isoniazid (INH) — peripheral neuropathy (give Vitamin B6 / Pyridoxine), hepatotoxic"),
                MnemonicStep("P", "Pyrazinamide — hyperuricemia / gout attacks, hepatotoxic"),
                MnemonicStep("E", "Ethambutol — optic neuritis (red-green color blindness, visual acuity check)"),
                MnemonicStep("S", "Streptomycin — ototoxicity (CN VIII) & nephrotoxicity (contraindicated in pregnancy)")
            ),
            examTrap = "Repeated PYQ: Ethambutol is the ONLY bacteriostatic first-line ATT drug, and Streptomycin is strictly contraindicated in pregnancy due to fetal CN VIII damage."
        ),
        ClinicalMnemonic(
            id = "MN_SLUDGE",
            acronym = "SLUDGE",
            title = "Cholinergic / Organophosphate Poisoning Toxicity",
            category = MnemonicCategory.PHARMACOLOGY,
            examTags = listOf("NORCET", "RRB", "PGIMER"),
            steps = listOf(
                MnemonicStep("S", "Salivation (profuse oral secretions)"),
                MnemonicStep("L", "Lacrimation (tearing)"),
                MnemonicStep("U", "Urination (incontinence)"),
                MnemonicStep("D", "Defecation & Diaphoresis"),
                MnemonicStep("G", "GI cramping & bronchorrhea / bronchospasm (Killer B's)"),
                MnemonicStep("E", "Emesis + pinpoint pupils (miosis)")
            ),
            examTrap = "Antidote Priority: Give IV Atropine FIRST to dry life-threatening bronchorrhea until atropinization (clear lungs), followed by Pralidoxime (2-PAM) to regenerate acetylcholinesterase."
        ),
        ClinicalMnemonic(
            id = "MN_ANTIDOTES",
            acronym = "A-B-C-D-H",
            title = "Must-Know High-Yield Drug Antidotes",
            category = MnemonicCategory.PHARMACOLOGY,
            examTags = listOf("RRB", "NORCET", "ESIC"),
            steps = listOf(
                MnemonicStep("A", "Acetaminophen (Paracetamol) → N-Acetylcysteine (NAC)"),
                MnemonicStep("B", "Benzodiazepines → Flumazenil · Beta-blockers → Glucagon"),
                MnemonicStep("C", "Coumadin (Warfarin) → Vitamin K (Phytonadione) / FFP"),
                MnemonicStep("D", "Digoxin → Digoxin Immune Fab (Digibind)"),
                MnemonicStep("H", "Heparin → Protamine Sulfate · Opioids → Naloxone · MgSO4 → Calcium Gluconate")
            ),
            examTrap = "Monitoring Trap: Heparin efficacy is tracked by aPTT (therapeutic 1.5–2.5× control), whereas Warfarin is tracked by PT/INR (therapeutic 2.0–3.0)."
        ),
        ClinicalMnemonic(
            id = "MN_MUDPILES",
            acronym = "MUDPILES",
            title = "High Anion Gap Metabolic Acidosis (HAGMA)",
            category = MnemonicCategory.PHARMACOLOGY,
            examTags = listOf("NORCET", "PGIMER"),
            steps = listOf(
                MnemonicStep("M-U", "Methanol · Uremia (renal failure)"),
                MnemonicStep("D-P", "Diabetic Ketoacidosis (DKA) · Propylene glycol"),
                MnemonicStep("I-L", "Isoniazid / Iron · Lactic acidosis (sepsis/shock)"),
                MnemonicStep("E-S", "Ethylene glycol · Salicylates (Aspirin toxicity)")
            ),
            examTrap = "ABG Pearl: Normal Anion Gap = Na+ − (Cl− + HCO3−) = 8–12 mEq/L. DKA triggers Kussmaul deep rapid respirations as respiratory compensation."
        ),

        // ==================== MED-SURG NURSING ====================
        ClinicalMnemonic(
            id = "MN_CUSHING",
            acronym = "CUSHING'S",
            title = "Cushing's Triad (Late Sign of Increased ICP)",
            category = MnemonicCategory.MSN,
            examTags = listOf("NORCET", "RRB", "PGIMER"),
            steps = listOf(
                MnemonicStep("1", "Systolic Hypertension with Widening Pulse Pressure (e.g., 180/60 mmHg)"),
                MnemonicStep("2", "Bradycardia (reflex vagal stimulation)"),
                MnemonicStep("3", "Irregular / Cheyne-Stokes Respirations (brainstem compression)")
            ),
            examTrap = "Classic NORCET Trap: Cushing's triad is a LATE sign of imminent brain herniation. The EARLIEST indicator of rising ICP is a change in Level of Consciousness (restlessness, confusion, lethargy)."
        ),
        ClinicalMnemonic(
            id = "MN_BECKS",
            acronym = "BECK'S",
            title = "Beck's Triad (Acute Cardiac Tamponade)",
            category = MnemonicCategory.MSN,
            examTags = listOf("NORCET", "PGIMER", "JIPMER"),
            steps = listOf(
                MnemonicStep("1", "Hypotension with Narrowed Pulse Pressure (impaired ventricular filling)"),
                MnemonicStep("2", "Jugular Venous Distension (JVD — elevated CVP)"),
                MnemonicStep("3", "Muffled / Distant Heart Sounds on auscultation")
            ),
            examTrap = "Associated Sign: Pulsus Paradoxus (systolic BP drop > 10 mmHg during inspiration) strongly confirms cardiac tamponade; emergency treatment is Pericardiocentesis."
        ),
        ClinicalMnemonic(
            id = "MN_CHARCOT",
            acronym = "CHARCOT → REYNOLD",
            title = "Ascending Cholangitis Triad & Pentad",
            category = MnemonicCategory.MSN,
            examTags = listOf("NORCET", "PGIMER"),
            steps = listOf(
                MnemonicStep("1", "Fever with chills/rigors"),
                MnemonicStep("2", "Right Upper Quadrant (RUQ) abdominal pain"),
                MnemonicStep("3", "Jaundice (obstructive bilirubin elevation)"),
                MnemonicStep("+2", "Add Hypotension + Altered Mental Status = Reynold's Pentad (septic shock)")
            ),
            examTrap = "Surgical Pearl: Urgent IV broad-spectrum antibiotics plus biliary decompression (ERCP) is life-saving in Reynold's Pentad."
        ),
        ClinicalMnemonic(
            id = "MN_5PS",
            acronym = "5 P's",
            title = "Acute Compartment Syndrome Warning Signs",
            category = MnemonicCategory.MSN,
            examTags = listOf("RRB", "NORCET", "ESIC"),
            steps = listOf(
                MnemonicStep("P1", "Pain — severe, out of proportion, worsens on passive stretch, unrelieved by opioids"),
                MnemonicStep("P2", "Paresthesia — tingling/numbness (early nerve ischemia)"),
                MnemonicStep("P3", "Pallor & Poikilothermia (cool extremity)"),
                MnemonicStep("P4", "Paralysis (late motor nerve loss)"),
                MnemonicStep("P5", "Pulselessness (LATE emergency — irreversible tissue damage imminent)")
            ),
            examTrap = "Priority Action: NEVER elevate the limb above heart level or apply ice in suspected compartment syndrome — keep limb at heart level, bivalve/split the cast, and notify surgeon for Fasciotomy."
        ),
        ClinicalMnemonic(
            id = "MN_HYPERCA",
            acronym = "BONES & STONES",
            title = "Hypercalcemia Clinical Presentation (> 10.5 mg/dL)",
            category = MnemonicCategory.MSN,
            examTags = listOf("RRB", "NORCET"),
            steps = listOf(
                MnemonicStep("Stones", "Nephrolithiasis (renal calculi) & polyuria"),
                MnemonicStep("Bones", "Bone pain, osteopenia, pathological fractures"),
                MnemonicStep("Groans", "Abdominal pain, constipation, nausea, peptic ulcers"),
                MnemonicStep("Moans", "Psychiatric depression, lethargy, hyporeflexia, shortened QT on ECG")
            ),
            examTrap = "Contrast with Hypocalcemia: Low Ca2+ causes neuromuscular irritability — positive Chvostek's sign (facial twitch), Trousseau's sign (carpopedal spasm), and PROLONGED QT interval."
        ),

        // ==================== OBG & PEDIATRICS ====================
        ClinicalMnemonic(
            id = "MN_VEALCHOP",
            acronym = "VEAL CHOP",
            title = "Fetal Heart Rate (FHR) Decelerations & Causes",
            category = MnemonicCategory.OBG_PEDS,
            examTags = listOf("NORCET", "RRB", "PGIMER"),
            steps = listOf(
                MnemonicStep("V → C", "Variable decelerations → Cord compression (reposition mother, amnioinfusion)"),
                MnemonicStep("E → H", "Early decelerations → Head compression (benign vagal response during contractions)"),
                MnemonicStep("A → O", "Accelerations → Okay / reassuring fetal oxygenation"),
                MnemonicStep("L → P", "Late decelerations → Placental insufficiency (STOP oxytocin, left lateral, O2, IV fluids)")
            ),
            examTrap = "NORCET Must-Know: For Late Decelerations, the immediate nursing bundle is LION — Left lateral position, IV bolus, Oxygen 8–10 L/min via non-rebreather, and Stop Oxytocin infusion."
        ),
        ClinicalMnemonic(
            id = "MN_APGAR",
            acronym = "APGAR",
            title = "Newborn Assessment at 1 Minute & 5 Minutes",
            category = MnemonicCategory.OBG_PEDS,
            examTags = listOf("RRB", "NORCET", "ESIC", "DSSSB"),
            steps = listOf(
                MnemonicStep("A", "Appearance (0 blue/pale · 1 acrocyanosis · 2 completely pink)"),
                MnemonicStep("P", "Pulse / HR (0 absent · 1 < 100 bpm · 2 ≥ 100 bpm)"),
                MnemonicStep("G", "Grimace / Reflex irritability (0 none · 1 grimace · 2 vigorous cry/cough)"),
                MnemonicStep("A", "Activity / Muscle tone (0 limp · 1 some flexion · 2 active movement)"),
                MnemonicStep("R", "Respiration (0 absent · 1 slow/weak cry · 2 strong cry)")
            ),
            examTrap = "Resuscitation Rule: NEVER wait for the 1-minute APGAR score to start neonatal resuscitation — begin drying, warming, and PPV immediately if apneic or HR < 100 bpm."
        ),
        ClinicalMnemonic(
            id = "MN_BUBBLEHE",
            acronym = "BUBBLE-HE",
            title = "Systematic Postpartum Maternal Assessment",
            category = MnemonicCategory.OBG_PEDS,
            examTags = listOf("RRB", "NORCET"),
            steps = listOf(
                MnemonicStep("B-U", "Breasts (engorgement/cracks) · Uterus (firmness, midline, descends 1 cm/day)"),
                MnemonicStep("B-B", "Bladder (distension shifts fundus UP & RIGHT) · Bowels"),
                MnemonicStep("L-E", "Lochia (Rubra 1–4d → Serosa 5–9d → Alba 10–14d) · Episiotomy (REEDA)"),
                MnemonicStep("H-E", "Homan's / calf tenderness (DVT check) · Emotional bonding / baby blues")
            ),
            examTrap = "Top PYQ: If the postpartum fundus is boggy and displaced to the RIGHT of the umbilicus, the FIRST nursing intervention is to assist the mother to empty her bladder (or catheterize)."
        ),
        ClinicalMnemonic(
            id = "MN_4TS_PPH",
            acronym = "4 T's (PPH)",
            title = "Causes of Postpartum Hemorrhage (> 500 mL Vaginal)",
            category = MnemonicCategory.OBG_PEDS,
            examTags = listOf("NORCET", "RRB", "PGIMER"),
            steps = listOf(
                MnemonicStep("Tone", "Uterine Atony (~70%, #1 cause) → Fundal massage + Oxytocin 10 IU IM/IV"),
                MnemonicStep("Trauma", "Cervical, vaginal, or perineal lacerations (firm uterus with bright red trickle)"),
                MnemonicStep("Tissue", "Retained placental fragments or membranes"),
                MnemonicStep("Thrombin", "Coagulopathy (DIC, HELLP, abruptio placentae)")
            ),
            examTrap = "Exam Distinction: Bright red continuous bleeding with a FIRM, well-contracted uterus indicates cervical/vaginal Trauma — NOT uterine atony."
        ),
        ClinicalMnemonic(
            id = "MN_TORCH",
            acronym = "TORCH",
            title = "Congenital Transplacental Perinatal Infections",
            category = MnemonicCategory.OBG_PEDS,
            examTags = listOf("RRB", "NORCET", "ESIC"),
            steps = listOf(
                MnemonicStep("T", "Toxoplasmosis (cat litter — chorioretinitis, hydrocephalus, intracranial calcifications)"),
                MnemonicStep("O", "Others (Syphilis — Hutchinson's triad, Parvovirus B19, Varicella, HIV)"),
                MnemonicStep("R", "Rubella (classic triad: Cataracts, Sensorineural Deafness, PDA)"),
                MnemonicStep("C", "Cytomegalovirus (most common viral cause of congenital SNHL & periventricular calcifications)"),
                MnemonicStep("H", "Herpes Simplex Virus (active genital lesions → Cesarean delivery indicated)")
            ),
            examTrap = "Vaccine Safety: Rubella (MMR) is a live-attenuated vaccine — contraindicated during pregnancy; give postpartum and advise avoiding conception for at least 1 month."
        ),

        // ==================== FUNDAMENTALS ====================
        ClinicalMnemonic(
            id = "MN_ISBAR",
            acronym = "ISBAR",
            title = "WHO Standardized Clinical Handover Tool",
            category = MnemonicCategory.FUNDAMENTALS,
            examTags = listOf("NORCET", "RRB", "ESIC"),
            steps = listOf(
                MnemonicStep("I", "Identify — nurse name/ward + patient name, age, UHID"),
                MnemonicStep("S", "Situation — current acute issue or reason for calling"),
                MnemonicStep("B", "Background — admission diagnosis, relevant history, current meds"),
                MnemonicStep("A", "Assessment — current vitals, SpO2, lab abnormalities, clinical impression"),
                MnemonicStep("R", "Recommendation — exact orders, review, or transfer requested")
            ),
            examTrap = "Quality & Safety: Always finish verbal/telephone orders with a documented 'Read-Back' verification to eliminate medication errors."
        ),
        ClinicalMnemonic(
            id = "MN_SAMPLE",
            acronym = "SAMPLE",
            title = "Rapid Focused Patient History (Triage & ER)",
            category = MnemonicCategory.FUNDAMENTALS,
            examTags = listOf("RRB", "NORCET"),
            steps = listOf(
                MnemonicStep("S", "Signs and Symptoms (onset, severity, radiation)"),
                MnemonicStep("A", "Allergies (drugs, latex, contrast dye, food)"),
                MnemonicStep("M", "Medications (prescription, OTC, anticoagulants, insulin)"),
                MnemonicStep("P", "Past pertinent medical & surgical history"),
                MnemonicStep("L", "Last oral intake (critical for aspiration risk / emergency OT)"),
                MnemonicStep("E", "Events leading up to the present illness or injury")
            ),
            examTrap = "Triage Priority: Check 'L' (Last Oral Intake) before every emergency surgical procedure to assess NPO aspiration risk."
        ),
        ClinicalMnemonic(
            id = "MN_RIGHTS",
            acronym = "5+3 RIGHTS",
            title = "Safe Medication Administration Checks",
            category = MnemonicCategory.FUNDAMENTALS,
            examTags = listOf("RRB", "ESIC", "DSSSB"),
            steps = listOf(
                MnemonicStep("Core 5", "Right Patient · Right Drug · Right Dose · Right Route · Right Time"),
                MnemonicStep("+3", "Right Documentation · Right Reason/Indication · Right Response/Evaluation"),
                MnemonicStep("3 Checks", "Check label: (1) pulling from stock, (2) preparing dose, (3) at bedside before giving")
            ),
            examTrap = "Safety Rule: Always verify TWO patient identifiers (Full Name + Date of Birth or UHID) — NEVER use the patient's bed/room number as an identifier."
        ),

        // ==================== CHN & PSYCHIATRY ====================
        ClinicalMnemonic(
            id = "MN_BMW",
            acronym = "Y-R-W-B",
            title = "Biomedical Waste Segregation (India BMW Rules 2016)",
            category = MnemonicCategory.COMMUNITY_PSYCH,
            examTags = listOf("RRB", "NORCET", "ESIC", "PGIMER"),
            steps = listOf(
                MnemonicStep("Yellow", "Human anatomical waste, soiled cotton/dressings, placenta, expired meds → Incineration"),
                MnemonicStep("Red", "Contaminated recyclable plastics: IV tubing, catheters, syringes WITHOUT needles, gloves → Autoclave"),
                MnemonicStep("White", "Puncture-proof translucent container: metallic sharps, needles, scalpels, blades"),
                MnemonicStep("Blue", "Cardboard box with blue marking: broken/discarded glassware, vials, ampoules, metallic implants")
            ),
            examTrap = "Highest-Frequency Indian Exam PYQ: Used gloves, urine bags, and plastic syringes (without needles) go in RED — NEVER Yellow. Broken glass ampoules go in BLUE — NEVER White."
        ),
        ClinicalMnemonic(
            id = "MN_SIGECAPS",
            acronym = "SIG-E-CAPS",
            title = "Major Depressive Disorder (DSM-5 Criteria)",
            category = MnemonicCategory.COMMUNITY_PSYCH,
            examTags = listOf("NORCET", "PGIMER", "RRB"),
            steps = listOf(
                MnemonicStep("S-I", "Sleep disturbance (insomnia/hypersomnia) · Interest loss (Anhedonia)"),
                MnemonicStep("G-E", "Guilt / worthlessness · Energy loss / fatigue"),
                MnemonicStep("C-A", "Concentration difficulty · Appetite / weight change"),
                MnemonicStep("P-S", "Psychomotor agitation/retardation · Suicidal ideation (≥ 5 symptoms for ≥ 2 weeks)")
            ),
            examTrap = "Psychiatric Nursing Priority: When a severely depressed patient suddenly appears cheerful or calm after starting antidepressants (weeks 2–4), assess IMMEDIATELY for suicide risk — returning energy increases ability to act on a plan."
        )
    )

    fun search(query: String, category: MnemonicCategory? = null): List<ClinicalMnemonic> {
        val q = query.trim().lowercase(Locale.US)
        return ALL_MNEMONICS.filter { item ->
            val categoryMatch = category == null || item.category == category
            val queryMatch = q.isBlank() ||
                item.acronym.lowercase(Locale.US).contains(q) ||
                item.title.lowercase(Locale.US).contains(q) ||
                item.examTrap.lowercase(Locale.US).contains(q) ||
                item.examTags.any { it.lowercase(Locale.US).contains(q) } ||
                item.steps.any {
                    it.letter.lowercase(Locale.US).contains(q) ||
                        it.meaning.lowercase(Locale.US).contains(q)
                }
            categoryMatch && queryMatch
        }
    }
}
