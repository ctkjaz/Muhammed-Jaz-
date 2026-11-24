package com.example.data.model

import java.util.Locale

data class MnemonicLetterRow(
    val letter: String,
    val meaning: String
)

data class ClinicalMnemonic(
    val id: String,
    val acronym: String,
    val title: String,
    val subject: String, // MSN, OBG, Peds, Pharm, Psych, FON, CHN
    val summary: String,
    val rows: List<MnemonicLetterRow>,
    val examTip: String
)

/**
 * 24 high-yield offline clinical nursing mnemonics curated for AIIMS NORCET (Prelims & Mains),
 * RRB Nursing Superintendent, PGIMER, ESIC, JIPMER, and State Nursing Officer exams.
 */
object ClinicalMnemonicsCatalog {

    val SUBJECT_FILTERS: List<String> = listOf("All", "MSN", "OBG", "Peds", "Pharm", "Psych", "FON", "CHN")

    val ALL_MNEMONICS: List<ClinicalMnemonic> = listOf(
        ClinicalMnemonic(
            id = "mn_veal_chop",
            acronym = "VEAL CHOP",
            title = "Fetal Heart Rate (CTG) Decelerations",
            subject = "OBG",
            summary = "Instant 1-to-1 match between CTG deceleration pattern and physiological cause.",
            rows = listOf(
                MnemonicLetterRow("V → C", "Variable decelerations → Cord compression (reposition mother first!)"),
                MnemonicLetterRow("E → H", "Early decelerations → Head compression (benign, normal labor)"),
                MnemonicLetterRow("A → O", "Accelerations → Okay / reassuring fetal oxygenation"),
                MnemonicLetterRow("L → P", "Late decelerations → Placental insufficiency (STOP Oxytocin, left lateral, O₂, IV bolus)")
            ),
            examTip = "Priority action for Late decelerations: turn mother to LEFT LATERAL position & stop Pitocin/Oxytocin infusion immediately."
        ),
        ClinicalMnemonic(
            id = "mn_mona",
            acronym = "MONA",
            title = "Acute Myocardial Infarction (ACS) Emergency",
            subject = "MSN",
            summary = "Immediate bedside interventions for suspected acute STEMI / chest pain.",
            rows = listOf(
                MnemonicLetterRow("M", "Morphine IV (only if pain unrelieved by nitrates — reduces preload & O₂ demand)"),
                MnemonicLetterRow("O", "Oxygen (if SpO₂ < 90% or respiratory distress)"),
                MnemonicLetterRow("N", "Nitroglycerin sublingual (check BP > 90 systolic & no sildenafil in 24–48h)"),
                MnemonicLetterRow("A", "Aspirin 300–325 mg CHEWED first (antiplatelet priority)")
            ),
            examTip = "Remember clinical execution order is O-N-A-M (or Aspirin + ECG within 10 min; Morphine last)."
        ),
        ClinicalMnemonic(
            id = "mn_mudpiles",
            acronym = "MUDPILES",
            title = "High Anion Gap Metabolic Acidosis (HAGMA)",
            subject = "MSN",
            summary = "Classic NORCET differential for metabolic acidosis with anion gap > 12 mEq/L.",
            rows = listOf(
                MnemonicLetterRow("M", "Methanol poisoning"),
                MnemonicLetterRow("U", "Uremia (chronic/acute renal failure)"),
                MnemonicLetterRow("D", "Diabetic Ketoacidosis (DKA — Kussmaul respirations)"),
                MnemonicLetterRow("P", "Paraldehyde / Propylene glycol"),
                MnemonicLetterRow("I", "Isoniazid (INH) / Iron toxicity"),
                MnemonicLetterRow("L", "Lactic acidosis (sepsis, shock, hypoxia)"),
                MnemonicLetterRow("E", "Ethylene glycol"),
                MnemonicLetterRow("S", "Salicylates (Aspirin overdose)")
            ),
            examTip = "Normal Anion Gap = Na⁺ − (Cl⁻ + HCO₃⁻) = 8 to 12 mEq/L."
        ),
        ClinicalMnemonic(
            id = "mn_sludge",
            acronym = "SLUDGE",
            title = "Organophosphate / Cholinergic Crisis Signs",
            subject = "Pharm",
            summary = "Muscarinic 'wet' excess symptoms in organophosphate poisoning.",
            rows = listOf(
                MnemonicLetterRow("S", "Salivation (profuse drooling)"),
                MnemonicLetterRow("L", "Lacrimation (tearing)"),
                MnemonicLetterRow("U", "Urination (incontinence)"),
                MnemonicLetterRow("D", "Defecation / Diarrhea"),
                MnemonicLetterRow("G", "GI cramping & bronchorrhea"),
                MnemonicLetterRow("E", "Emesis (vomiting) + pinpoint miosis")
            ),
            examTip = "Antidote of choice: IV Atropine (until secretions dry — 'atropinization') + Pralidoxime (2-PAM)."
        ),
        ClinicalMnemonic(
            id = "mn_cushing",
            acronym = "CUSHING'S TRIAD",
            title = "Late Sign of Increased Intracranial Pressure (ICP)",
            subject = "MSN",
            summary = "Life-threatening brainstem herniation warning triad.",
            rows = listOf(
                MnemonicLetterRow("1", "Hypertension with WIDENING pulse pressure (rising SBP, falling DBP)"),
                MnemonicLetterRow("2", "Bradycardia (slow, bounding pulse)"),
                MnemonicLetterRow("3", "Irregular respirations (Cheyne-Stokes / Biot's breathing)")
            ),
            examTip = "Earliest sign of raised ICP is altered Level of Consciousness (LOC/restlessness); Cushing's Triad is a LATE emergency sign!"
        ),
        ClinicalMnemonic(
            id = "mn_4ts_pph",
            acronym = "4 T's of PPH",
            title = "Causes of Postpartum Hemorrhage (>500 mL NVD / >1000 mL CS)",
            subject = "OBG",
            summary = "Ranked from most common (~70%) to least common cause of PPH.",
            rows = listOf(
                MnemonicLetterRow("T1", "Tone — Uterine atony (MOST COMMON ~70%; boggy fundus → massage + Oxytocin)"),
                MnemonicLetterRow("T2", "Trauma — Cervical/vaginal lacerations, uterine rupture, hematoma"),
                MnemonicLetterRow("T3", "Tissue — Retained placenta or cotyledons / membranes"),
                MnemonicLetterRow("T4", "Thrombin — Coagulopathy (DIC, HELLP, von Willebrand disease)")
            ),
            examTip = "First nursing action for a boggy displaced uterus: fundal massage + empty the bladder!"
        ),
        ClinicalMnemonic(
            id = "mn_bubble_he",
            acronym = "BUBBLE-HE",
            title = "Systematic Postpartum Nursing Assessment",
            subject = "OBG",
            summary = "Head-to-toe postnatal ward checklist during the puerperium.",
            rows = listOf(
                MnemonicLetterRow("B", "Breasts (soft, filling, engorged, cracked nipples)"),
                MnemonicLetterRow("U", "Uterus (firm, midline, descends ~1 cm/day below umbilicus)"),
                MnemonicLetterRow("B", "Bladder (voiding within 6–8h; full bladder displaces fundus to right)"),
                MnemonicLetterRow("B", "Bowels (bowel sounds, flatus, constipation prevention)"),
                MnemonicLetterRow("L", "Lochia (Rubra d1–3 → Serosa d4–10 → Alba d11–21)"),
                MnemonicLetterRow("E", "Episiotomy / Perineum (assess with REEDA)"),
                MnemonicLetterRow("H", "Homan's sign / DVT leg check (calf warmth, redness, unilateral edema)"),
                MnemonicLetterRow("E", "Emotional bonding (Taking-in → Taking-hold → Letting-go)")
            ),
            examTip = "Foul-smelling lochia or return to bright red bleeding after day 4 signals infection or subinvolution."
        ),
        ClinicalMnemonic(
            id = "mn_reeda",
            acronym = "REEDA",
            title = "Episiotomy & Perineal Wound Healing Scale",
            subject = "OBG",
            summary = "5 parameters scored 0–3 each (max 15) to detect perineal infection early.",
            rows = listOf(
                MnemonicLetterRow("R", "Redness (erythema around suture line)"),
                MnemonicLetterRow("E", "Edema (perineal swelling)"),
                MnemonicLetterRow("E", "Ecchymosis (bruising / hematoma)"),
                MnemonicLetterRow("D", "Discharge (serous vs. purulent)"),
                MnemonicLetterRow("A", "Approximation (wound edges closed vs. gaped)")
            ),
            examTip = "Ice pack for first 24 hours to reduce edema; warm Sitz bath AFTER 24 hours to boost healing."
        ),
        ClinicalMnemonic(
            id = "mn_tpal",
            acronym = "GTPAL",
            title = "Obstetric History Notation",
            subject = "OBG",
            summary = "Standardized five-digit obstetric parity summary.",
            rows = listOf(
                MnemonicLetterRow("G", "Gravida — Total number of pregnancies (including current)"),
                MnemonicLetterRow("T", "Term births (≥ 37 completed weeks)"),
                MnemonicLetterRow("P", "Preterm births (20 to 36+6 weeks)"),
                MnemonicLetterRow("A", "Abortions / Miscarriages (< 20 weeks)"),
                MnemonicLetterRow("L", "Living children currently alive")
            ),
            examTip = "Twins count as ONE pregnancy (G+1) and ONE delivery event (T or P +1), but TWO living children (L+2)."
        ),
        ClinicalMnemonic(
            id = "mn_apgar",
            acronym = "APGAR",
            title = "Newborn Transition Scoring (1 min & 5 min)",
            subject = "Peds",
            summary = "Rapid 0–10 neonatal assessment (7–10 normal, 4–6 moderate, 0–3 severe distress).",
            rows = listOf(
                MnemonicLetterRow("A", "Appearance (skin color: 0 pale/blue, 1 acrocyanosis, 2 completely pink)"),
                MnemonicLetterRow("P", "Pulse (0 absent, 1 <100 bpm, 2 ≥100 bpm)"),
                MnemonicLetterRow("G", "Grimace / Reflex irritability (0 none, 1 grimace, 2 vigorous cry)"),
                MnemonicLetterRow("A", "Activity / Muscle tone (0 limp, 1 some flexion, 2 active motion)"),
                MnemonicLetterRow("R", "Respiration (0 absent, 1 slow/irregular, 2 strong cry)")
            ),
            examTip = "Never delay newborn resuscitation waiting for the 1-minute APGAR score!"
        ),
        ClinicalMnemonic(
            id = "mn_torch",
            acronym = "TORCH",
            title = "Congenital Teratogenic Perinatal Infections",
            subject = "Peds",
            summary = "Key maternal infections crossing the placenta causing congenital anomalies.",
            rows = listOf(
                MnemonicLetterRow("T", "Toxoplasmosis (cat litter — chorioretinitis, hydrocephalus, calcifications)"),
                MnemonicLetterRow("O", "Others (Syphilis, Varicella, Parvovirus B19, Zika, HIV, Hep B)"),
                MnemonicLetterRow("R", "Rubella (triad: cataracts, sensorineural deafness, PDA)"),
                MnemonicLetterRow("C", "Cytomegalovirus (CMV — most common congenital viral infection)"),
                MnemonicLetterRow("H", "Herpes Simplex Virus (HSV-2 — C-section if active genital lesions)")
            ),
            examTip = "Rubella vaccine (MMR) is LIVE-attenuated — contraindicated in pregnancy; avoid conception for 1 month post-dose."
        ),
        ClinicalMnemonic(
            id = "mn_5ps",
            acronym = "6 P's",
            title = "Acute Compartment Syndrome / Arterial Occlusion",
            subject = "MSN",
            summary = "Orthopedic & vascular neurovascular check following casts or fractures.",
            rows = listOf(
                MnemonicLetterRow("P1", "Pain — Severe, out of proportion, unrelieved by opioids & worse on passive stretch (EARLIEST!)"),
                MnemonicLetterRow("P2", "Paresthesia — Tingling / numbness (early nerve ischemia)"),
                MnemonicLetterRow("P3", "Pallor — Pale, dusky distal skin"),
                MnemonicLetterRow("P4", "Poikilothermia — Cool extremity"),
                MnemonicLetterRow("P5", "Paralysis — Loss of active movement (LATE)"),
                MnemonicLetterRow("P6", "Pulselessness — Absent distal pulse (LATE emergency)")
            ),
            examTip = "Keep limb at HEART level (do NOT elevate above heart) and prepare for bivalving the cast or emergency fasciotomy."
        ),
        ClinicalMnemonic(
            id = "mn_fast",
            acronym = "BE-FAST",
            title = "Acute Stroke Recognition & tPA Window",
            subject = "MSN",
            summary = "Rapid neurological triage for acute ischemic vs. hemorrhagic stroke.",
            rows = listOf(
                MnemonicLetterRow("B", "Balance — Sudden ataxia or loss of coordination"),
                MnemonicLetterRow("E", "Eyes — Sudden diplopia or visual field loss"),
                MnemonicLetterRow("F", "Face — Unilateral facial droop"),
                MnemonicLetterRow("A", "Arms — Unilateral arm drift/weakness"),
                MnemonicLetterRow("S", "Speech — Slurred dysarthria or aphasia"),
                MnemonicLetterRow("T", "Time — Note 'Last Known Well'; Non-contrast CT head FIRST to rule out bleed!")
            ),
            examTip = "IV Alteplase (tPA) window for ischemic stroke is within 4.5 hours of Last Known Well AFTER CT excludes hemorrhage."
        ),
        ClinicalMnemonic(
            id = "mn_rule_of_9s",
            acronym = "RULE OF 9s",
            title = "Adult Total Body Surface Area (TBSA) Burn Estimate",
            subject = "MSN",
            summary = "Wallace Rule of Nines used directly inside the Parkland fluid formula.",
            rows = listOf(
                MnemonicLetterRow("9%", "Head & Neck (4.5% front + 4.5% back)"),
                MnemonicLetterRow("18%", "Anterior Trunk (chest 9% + abdomen 9%)"),
                MnemonicLetterRow("18%", "Posterior Trunk (upper back 9% + lower back 9%)"),
                MnemonicLetterRow("9% × 2", "Each Upper Limb (4.5% anterior + 4.5% posterior)"),
                MnemonicLetterRow("18% × 2", "Each Lower Limb (9% anterior + 9% posterior)"),
                MnemonicLetterRow("1%", "Perineum / Genitalia")
            ),
            examTip = "Parkland Formula = 4 mL × kg × %TBSA (Ringer's Lactate): give HALF in first 8h from TIME OF BURN, half over next 16h."
        ),
        ClinicalMnemonic(
            id = "mn_digoxin",
            acronym = "DIG-TOX",
            title = "Digoxin Toxicity Recognition & Safety Rule",
            subject = "Pharm",
            summary = "Therapeutic window is ultra-narrow (0.5–2.0 ng/mL).",
            rows = listOf(
                MnemonicLetterRow("D", "Diarrhea, anorexia, nausea/vomiting (EARLIEST symptoms)"),
                MnemonicLetterRow("I", "Increased risk with HYPOKALEMIA (K⁺ < 3.5 mEq/L — loop/thiazide diuretics!)"),
                MnemonicLetterRow("G", "Green-yellow visual halos (xanthopsia) & blurred vision"),
                MnemonicLetterRow("T", "Target apical pulse ≥ 60 bpm in adults before giving dose (hold if < 60!)"),
                MnemonicLetterRow("O", "Order Antidote: Digoxin Immune Fab (Digibind) for life-threatening toxicity"),
                MnemonicLetterRow("X", "eXtra ECG arrhythmias: PVCs, bradycardia, AV block")
            ),
            examTip = "Always auscultate the APICAL pulse for 1 full minute before administering Digoxin (hold if <60 adult, <90–110 infant)."
        ),
        ClinicalMnemonic(
            id = "mn_chads",
            acronym = "CHA₂DS₂-VASc",
            title = "Stroke Risk Anticoagulation Score in Atrial Fibrillation",
            subject = "MSN",
            summary = "Determines oral anticoagulant therapy in non-valvular A-Fib.",
            rows = listOf(
                MnemonicLetterRow("C", "Congestive Heart Failure (+1)"),
                MnemonicLetterRow("H", "Hypertension (+1)"),
                MnemonicLetterRow("A₂", "Age ≥ 75 years (+2)"),
                MnemonicLetterRow("D", "Diabetes Mellitus (+1)"),
                MnemonicLetterRow("S₂", "Prior Stroke / TIA / Thromboembolism (+2)"),
                MnemonicLetterRow("V", "Vascular disease (+1)"),
                MnemonicLetterRow("A", "Age 65–74 years (+1)"),
                MnemonicLetterRow("Sc", "Sex category female (+1)")
            ),
            examTip = "Score ≥ 2 in men (or ≥ 3 in women) indicates oral anticoagulation (DOACs or Warfarin with target INR 2.0–3.0)."
        ),
        ClinicalMnemonic(
            id = "mn_sigecaps",
            acronym = "SIG-E-CAPS",
            title = "Major Depressive Disorder (DSM-5 Criteria ≥ 2 Weeks)",
            subject = "Psych",
            summary = "Requires ≥ 5 symptoms for ≥ 2 weeks including depressed mood or anhedonia.",
            rows = listOf(
                MnemonicLetterRow("S", "Sleep disturbance (insomnia or hypersomnia)"),
                MnemonicLetterRow("I", "Interest loss (anhedonia)"),
                MnemonicLetterRow("G", "Guilt or feelings of worthlessness"),
                MnemonicLetterRow("E", "Energy loss / fatigue"),
                MnemonicLetterRow("C", "Concentration difficulty / indecisiveness"),
                MnemonicLetterRow("A", "Appetite or weight change"),
                MnemonicLetterRow("P", "Psychomotor agitation or retardation"),
                MnemonicLetterRow("S", "Suicidal ideation (PRIORITY nursing safety assessment!)")
            ),
            examTip = "Suicide risk often RISES 1–2 weeks after starting SSRIs when energy returns before mood fully lifts."
        ),
        ClinicalMnemonic(
            id = "mn_sbar",
            acronym = "SBAR",
            title = "Structured Clinical Shift Handover & Doctor Escalation",
            subject = "FON",
            summary = "WHO/NABH standardized nursing communication protocol.",
            rows = listOf(
                MnemonicLetterRow("S", "Situation — Patient name, bed, current acute problem/vitals"),
                MnemonicLetterRow("B", "Background — Admission diagnosis, relevant history, active meds/allergies"),
                MnemonicLetterRow("A", "Assessment — Current physical findings, lab/ECG changes, clinical impression"),
                MnemonicLetterRow("R", "Recommendation — Specific action requested (order, review, transfer)")
            ),
            examTip = "SBAR minimizes communication errors during ICU handoffs and emergency physician calls."
        ),
        ClinicalMnemonic(
            id = "mn_race_pass",
            acronym = "RACE & PASS",
            title = "Hospital Fire Emergency Protocol (NABH Safety)",
            subject = "FON",
            summary = "Mandatory hospital fire response & extinguisher operation order.",
            rows = listOf(
                MnemonicLetterRow("R", "Rescue patients in immediate danger FIRST"),
                MnemonicLetterRow("A", "Alarm — Activate fire pull station & alert switchboard"),
                MnemonicLetterRow("C", "Contain — Close doors/windows & shut off O₂ valves if safe"),
                MnemonicLetterRow("E", "Extinguish / Evacuate"),
                MnemonicLetterRow("PASS", "Pull pin → Aim at BASE of fire → Squeeze handle → Sweep side to side")
            ),
            examTip = "In NORCET situational questions, RESCUING the patient in the burning room always precedes pulling the alarm."
        ),
        ClinicalMnemonic(
            id = "mn_10_rights",
            acronym = "10 RIGHTS",
            title = "Safe Medication Administration Principles",
            subject = "FON",
            summary = "Core nursing safety checks to prevent medication errors.",
            rows = listOf(
                MnemonicLetterRow("1–5", "Right Patient (2 identifiers), Right Drug, Right Dose, Right Route, Right Time"),
                MnemonicLetterRow("6–10", "Right Documentation, Right Reason, Right Assessment, Right Education, Right to Refuse")
            ),
            examTip = "Always verify TWO patient identifiers (Full Name + UHID/DOB) — NEVER use bed number alone!"
        ),
        ClinicalMnemonic(
            id = "mn_pqrst",
            acronym = "PQRST",
            title = "Comprehensive Nursing Pain Assessment",
            subject = "FON",
            summary = "Structured subjective pain history for triage & bedside charting.",
            rows = listOf(
                MnemonicLetterRow("P", "Provoking / Palliating factors (what triggers or relieves it)"),
                MnemonicLetterRow("Q", "Quality (crushing, sharp, burning, colicky)"),
                MnemonicLetterRow("R", "Region & Radiation (substernal radiating to left arm/jaw)"),
                MnemonicLetterRow("S", "Severity (0–10 NRS, Wong-Baker FACES, FLACC in infants)"),
                MnemonicLetterRow("T", "Timing (onset, duration, constant vs. intermittent)")
            ),
            examTip = "Pain is whatever the experiencing person says it is — patient self-report is the gold standard!"
        ),
        ClinicalMnemonic(
            id = "mn_abcde_trauma",
            acronym = "ABCDE",
            title = "ATLS Primary Trauma Survey",
            subject = "MSN",
            summary = "Strict life-saving sequence for polytrauma & emergency triage.",
            rows = listOf(
                MnemonicLetterRow("A", "Airway patency WITH Cervical-Spine immobilization (jaw-thrust, not head-tilt!)"),
                MnemonicLetterRow("B", "Breathing & ventilation (rule out tension pneumothorax, flail chest)"),
                MnemonicLetterRow("C", "Circulation & hemorrhage control (2 large-bore 16G/18G IV lines)"),
                MnemonicLetterRow("D", "Disability (GCS score, pupil reactivity, blood glucose)"),
                MnemonicLetterRow("E", "Exposure & Environment (undress to inspect + prevent hypothermia)")
            ),
            examTip = "For suspected C-spine injury in trauma, use the JAW-THRUST maneuver instead of head-tilt chin-lift."
        ),
        ClinicalMnemonic(
            id = "mn_cold_chain",
            acronym = "FREEZE vs HEAT",
            title = "India UIP Vaccine Cold Chain Sensitivity (2–8 °C)",
            subject = "CHN",
            summary = "Which Universal Immunization Programme vaccines are damaged by freezing vs. heat.",
            rows = listOf(
                MnemonicLetterRow("HEAT (Most)", "OPV (most heat-sensitive, store −20 °C long-term) → Measles/MR → BCG"),
                MnemonicLetterRow("FREEZE (T-Series)", "Hep B (most freeze-sensitive!), Pentavalent, DPT, Td, PCV, IPV — NEVER freeze!"),
                MnemonicLetterRow("SHAKE TEST", "Checks freeze damage in adsorbed T-series vaccines (sediments rapidly = damaged)"),
                MnemonicLetterRow("VVM", "Vaccine Vial Monitor: Discard at Stage 3 (square matches circle) or Stage 4 (darker)")
            ),
            examTip = "In an ILR (Ice-Lined Refrigerator), keep freeze-sensitive vaccines (Hep B, DPT, Td, Penta) in the UPPER basket and OPV/Measles at the BOTTOM!"
        ),
        ClinicalMnemonic(
            id = "mn_sample",
            acronym = "SAMPLE",
            title = "Focused Emergency Secondary History",
            subject = "FON",
            summary = "Rapid patient history gathered during emergency triage.",
            rows = listOf(
                MnemonicLetterRow("S", "Signs & Symptoms"),
                MnemonicLetterRow("A", "Allergies (drugs, latex, contrast dye, food)"),
                MnemonicLetterRow("M", "Medications (including OTC, herbal, anticoagulants)"),
                MnemonicLetterRow("P", "Past medical/surgical history"),
                MnemonicLetterRow("L", "Last oral intake (NPO timing for emergency intubation/OT)"),
                MnemonicLetterRow("E", "Events leading up to current illness or injury")
            ),
            examTip = "Always ask about contrast/shellfish/latex allergies and last meal before emergency procedures."
        )
    )

    fun filter(query: String, subject: String): List<ClinicalMnemonic> {
        val q = query.trim().lowercase(Locale.US)
        val subj = subject.trim()
        return ALL_MNEMONICS.filter { item ->
            val matchesSubj = subj.isBlank() || subj.equals("All", ignoreCase = true) ||
                item.subject.equals(subj, ignoreCase = true)
            if (!matchesSubj) return@filter false
            if (q.isEmpty()) return@filter true
            item.acronym.lowercase(Locale.US).contains(q) ||
                item.title.lowercase(Locale.US).contains(q) ||
                item.summary.lowercase(Locale.US).contains(q) ||
                item.examTip.lowercase(Locale.US).contains(q) ||
                item.rows.any {
                    it.letter.lowercase(Locale.US).contains(q) ||
                        it.meaning.lowercase(Locale.US).contains(q)
                }
        }
    }
}
