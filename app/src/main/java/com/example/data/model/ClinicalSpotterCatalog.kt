package com.example.data.model

import java.util.Locale

/**
 * Typed representation of a clinical image attached to an MCQ.
 *
 * Supported schemes:
 * - `diagram://<key>`   → Built-in vector-sharp medical Canvas renderer (ECG strips, IV cannulas, BMW bins, Venturi mask)
 * - `drawable://<key>`  → Pre-bundled clinical spotter photography in `res/drawable/`
 * - `https://...` or `http://...` → Remote clinical image pushed via Owner Admin OTA Question Packs (rendered via Coil)
 */
sealed class ClinicalImageRef {
    data object None : ClinicalImageRef()
    data class Diagram(val key: String) : ClinicalImageRef()
    data class Drawable(val key: String) : ClinicalImageRef()
    data class RemoteUrl(val url: String) : ClinicalImageRef()

    companion object {
        private val SAFE_KEY_REGEX = Regex("^[a-z0-9_\\-]{2,64}$")

        /**
         * Sanitizes an incoming image URL / URI string. Rejects unsafe schemes (`javascript:`, `file://`, `content://`, `data:`)
         * and returns an empty string if invalid.
         */
        fun sanitize(raw: String?): String {
            val trimmed = raw?.trim().orEmpty()
            if (trimmed.isBlank() || trimmed.length > 512) return ""
            val lower = trimmed.lowercase(Locale.US)
            return when {
                lower.startsWith("diagram://") -> {
                    val key = lower.removePrefix("diagram://").trim()
                    if (SAFE_KEY_REGEX.matches(key)) "diagram://$key" else ""
                }
                lower.startsWith("drawable://") -> {
                    val key = lower.removePrefix("drawable://").trim()
                    if (SAFE_KEY_REGEX.matches(key)) "drawable://$key" else ""
                }
                lower.startsWith("https://") || lower.startsWith("http://") -> {
                    if (trimmed.any { it.isWhitespace() }) "" else trimmed
                }
                else -> ""
            }
        }

        fun parse(raw: String?): ClinicalImageRef {
            val clean = sanitize(raw)
            if (clean.isEmpty()) return None
            val lower = clean.lowercase(Locale.US)
            return when {
                lower.startsWith("diagram://") -> Diagram(lower.removePrefix("diagram://"))
                lower.startsWith("drawable://") -> Drawable(lower.removePrefix("drawable://"))
                lower.startsWith("https://") || lower.startsWith("http://") -> RemoteUrl(clean)
                else -> None
            }
        }
    }
}

/**
 * Built-in high-yield Image-Based Clinical Spotters Pack for AIIMS NORCET (Prelims & Mains),
 * RRB Nursing Superintendent, PGIMER, ESIC, JIPMER & State Nursing Officer exams.
 *
 * Includes:
 * - 6 ECG rhythm & ischemic strips rendered on authentic 1mm/5mm red ECG grid paper
 * - 4 clinical airway & surgical instrument photo spotters
 * - 5 hospital color-coding & respiratory device spotters (IV Cannulas, BMW Bins, Venturi Mask)
 */
object ClinicalSpotterCatalog {

    const val DIAGRAM_ECG_VTACH = "ecg_vtach"
    const val DIAGRAM_ECG_VFIB = "ecg_vfib"
    const val DIAGRAM_ECG_AFIB = "ecg_afib"
    const val DIAGRAM_ECG_STEMI = "ecg_stemi"
    const val DIAGRAM_ECG_AVBLOCK3 = "ecg_avblock3"
    const val DIAGRAM_ECG_HYPERKALEMIA = "ecg_hyperkalemia"

    const val DIAGRAM_IV_CANNULA_18G = "spotter_iv_cannula_18g"
    const val DIAGRAM_IV_CANNULA_22G = "spotter_iv_cannula_22g"
    const val DIAGRAM_BMW_RED = "spotter_bmw_red"
    const val DIAGRAM_BMW_YELLOW = "spotter_bmw_yellow"
    const val DIAGRAM_VENTURI_MASK = "spotter_venturi_mask"

    const val DRAWABLE_LARYNGOSCOPE = "laryngoscope"
    const val DRAWABLE_SURGICAL_TRAY = "surgical_tray"

    fun badgeLabelFor(ref: ClinicalImageRef): String = when (ref) {
        is ClinicalImageRef.Diagram -> when {
            ref.key.startsWith("ecg_") -> "ECG STRIP · 25 mm/s"
            ref.key.startsWith("spotter_iv_") -> "IV CANNULA SPOTTER"
            ref.key.startsWith("spotter_bmw_") -> "BMW SEGREGATION"
            ref.key.startsWith("spotter_venturi") -> "OXYGEN DEVICE"
            else -> "CLINICAL DIAGRAM"
        }
        is ClinicalImageRef.Drawable -> when (ref.key) {
            DRAWABLE_LARYNGOSCOPE -> "AIRWAY SPOTTER"
            DRAWABLE_SURGICAL_TRAY -> "OT INSTRUMENT SPOTTER"
            else -> "CLINICAL PHOTO"
        }
        is ClinicalImageRef.RemoteUrl -> "IMAGE-BASED MCQ"
        ClinicalImageRef.None -> ""
    }

    fun builtinSpotterQuestions(): List<Question> = listOf(
        // 1. Monomorphic Ventricular Tachycardia (ECG)
        Question(
            id = "IBQ_001",
            category = "nur",
            shiftYear = "",
            subject = "MSN",
            difficulty = QuestionDifficulty.HARD,
            questionText = "A 62-year-old post-MI patient in CCU suddenly develops the cardiac rhythm shown on the monitor (rate ~175 bpm, regular wide bizarre QRS complexes >0.12 s, no visible P waves). The patient is conscious with BP 108/68 mmHg and palpable radial pulse. What is the priority nursing/ACLS intervention?",
            options = listOf(
                "Immediate unsynchronized defibrillation at 200 J biphasic",
                "Administer IV Amiodarone 150 mg over 10 minutes (or prepare for synchronized cardioversion if unstable)",
                "Administer IV Atropine 1 mg rapid push",
                "Perform carotid sinus massage immediately"
            ),
            correctIndex = 1,
            explanation = "The ECG strip shows regular monomorphic Ventricular Tachycardia (wide QRS >0.12 s, rate 150–250/min). Because the patient has a pulse and is hemodynamically STABLE (conscious, BP 108/68), ACLS recommends IV Amiodarone 150 mg over 10 min (first-line antiarrhythmic). Synchronized cardioversion (100 J) is used for unstable VT with a pulse; unsynchronized defibrillation is ONLY for pulseless VT/VF.",
            isCustom = false,
            sourceStatus = Question.SOURCE_VERIFIED,
            sourceNote = "ClinEdge Clinical Spotters • AIIMS NORCET High-Yield ECG",
            topicId = "msn_cardiac_emergencies",
            sourceType = QuestionSourceType.PRACTICE,
            imageUrl = "diagram://$DIAGRAM_ECG_VTACH"
        ),
        // 2. Ventricular Fibrillation (ECG)
        Question(
            id = "IBQ_002",
            category = "nur",
            shiftYear = "",
            subject = "MSN",
            difficulty = QuestionDifficulty.MEDIUM,
            questionText = "Identify the chaotic, irregular, undulating baseline rhythm shown on the ECG strip. The patient is unresponsive with no carotid pulse and no breathing. What is the FIRST priority life-saving action once the defibrillator arrives?",
            options = listOf(
                "Deliver an immediate unsynchronized defibrillation shock (120–200 J biphasic) and resume CPR immediately",
                "Deliver a synchronized cardioversion shock at 50 J",
                "Administer IV Adenosine 6 mg rapid push first",
                "Check arterial blood gas (ABG) and serum potassium before shocking"
            ),
            correctIndex = 0,
            explanation = "The strip demonstrates coarse Ventricular Fibrillation (V-Fib) — completely disorganized ventricular electrical activity with zero effective cardiac output. High-quality CPR and immediate UNSYNCHRONIZED defibrillation (120–200 J biphasic or 360 J monophasic) followed immediately by 2 minutes of CPR is the definitive ACLS priority.",
            isCustom = false,
            sourceStatus = Question.SOURCE_VERIFIED,
            sourceNote = "ClinEdge Clinical Spotters • AIIMS NORCET High-Yield ECG",
            topicId = "msn_cardiac_emergencies",
            sourceType = QuestionSourceType.PRACTICE,
            imageUrl = "diagram://$DIAGRAM_ECG_VFIB"
        ),
        // 3. Atrial Fibrillation (ECG)
        Question(
            id = "IBQ_003",
            category = "nur",
            shiftYear = "",
            subject = "MSN",
            difficulty = QuestionDifficulty.MEDIUM,
            questionText = "Examine the rhythm strip shown above featuring an 'irregularly irregular' R–R interval, narrow QRS complexes, and fibrillatory baseline waves with absent discrete P waves. Which major complication must the nurse monitor this patient for?",
            options = listOf(
                "Cardioembolic ischemic stroke due to left atrial appendage thrombus",
                "Hypercalcemic tetany",
                "Pulmonary tension pneumothorax",
                "Acute angle-closure glaucoma"
            ),
            correctIndex = 0,
            explanation = "Atrial Fibrillation (A-Fib) is characterized by absent P waves, fibrillatory 'f' waves, and irregularly irregular R–R intervals. Loss of effective atrial contraction ('atrial kick') causes blood stasis in the left atrial appendage, creating mural thrombi that can embolize to the brain (cardioembolic stroke). Rate control + CHA₂DS₂-VASc anticoagulation are cornerstones of care.",
            isCustom = false,
            sourceStatus = Question.SOURCE_VERIFIED,
            sourceNote = "ClinEdge Clinical Spotters • AIIMS NORCET High-Yield ECG",
            topicId = "msn_cardiac_emergencies",
            sourceType = QuestionSourceType.PRACTICE,
            imageUrl = "diagram://$DIAGRAM_ECG_AFIB"
        ),
        // 4. Acute Anterior STEMI (ECG)
        Question(
            id = "IBQ_004",
            category = "nur",
            shiftYear = "",
            subject = "MSN",
            difficulty = QuestionDifficulty.MEDIUM,
            questionText = "A 55-year-old diabetic male presents to Emergency with crushing substernal chest pain radiating to the left jaw for 45 minutes. Lead V2–V4 shows the convex 'tombstone' ST-segment elevation pictured above. What is the target door-to-balloon time for Primary PCI?",
            options = listOf(
                "Within 90 minutes (and door-to-needle ≤ 30 minutes if fibrinolysis is used)",
                "Within 6 hours of symptom onset",
                "Within 12 hours after Troponin-I confirmation",
                "Within 24 hours after echocardiogram"
            ),
            correctIndex = 0,
            explanation = "Convex ST-segment elevation ≥2 mm in precordial leads V2–V4 indicates Acute Anterior Wall STEMI (usually LAD occlusion). AHA/ACC guidelines mandate door-to-ECG ≤10 min, door-to-balloon (Primary PCI) ≤90 min, and door-to-needle (thrombolysis if PCI unavailable within 120 min) ≤30 min. Never wait for Troponin before activating the cath lab!",
            isCustom = false,
            sourceStatus = Question.SOURCE_VERIFIED,
            sourceNote = "ClinEdge Clinical Spotters • AIIMS NORCET High-Yield ECG",
            topicId = "msn_cardiac_emergencies",
            sourceType = QuestionSourceType.PRACTICE,
            imageUrl = "diagram://$DIAGRAM_ECG_STEMI"
        ),
        // 5. Complete Third-Degree AV Block (ECG)
        Question(
            id = "IBQ_005",
            category = "nur",
            shiftYear = "",
            subject = "MSN",
            difficulty = QuestionDifficulty.HARD,
            questionText = "Analyze the ECG strip above showing regular P waves marching independently of slow regular QRS complexes (complete AV dissociation, ventricular escape rate ~36 bpm) in a dizzy patient with BP 78/50 mmHg. What is the most definitive emergency nursing preparation?",
            options = listOf(
                "Prepare immediately for Transcutaneous Pacing (TCP) while administering IV Atropine bridge",
                "Administer IV Metoprolol 5 mg slow push",
                "Administer IV Digoxin loading dose",
                "Place the patient in high-Fowler's position and observe for 1 hour"
            ),
            correctIndex = 0,
            explanation = "Complete (Third-Degree) Heart Block exhibits total AV dissociation: P waves march regularly (P–P constant) and QRS complexes march independently at a slow escape rate (R–R constant), with variable PR intervals. Symptomatic 3rd-degree AV block rarely responds sustainably to Atropine alone; immediate Transcutaneous Pacing (followed by transvenous/permanent pacemaker) is the definitive emergency treatment.",
            isCustom = false,
            sourceStatus = Question.SOURCE_VERIFIED,
            sourceNote = "ClinEdge Clinical Spotters • AIIMS NORCET High-Yield ECG",
            topicId = "msn_cardiac_emergencies",
            sourceType = QuestionSourceType.PRACTICE,
            imageUrl = "diagram://$DIAGRAM_ECG_AVBLOCK3"
        ),
        // 6. Severe Hyperkalemia Tall Peaked T-Waves (ECG)
        Question(
            id = "IBQ_006",
            category = "nur",
            shiftYear = "",
            subject = "Pharm",
            difficulty = QuestionDifficulty.HARD,
            questionText = "A patient with End-Stage Renal Disease (missed 2 dialysis sessions) has serum K⁺ of 7.2 mEq/L and shows tall, narrow, tented ('peaked') T waves with flattened P waves on ECG as illustrated above. Which IV medication is given FIRST to immediately stabilize the myocardial membrane?",
            options = listOf(
                "IV 10% Calcium Gluconate (10 mL over 2–5 min with cardiac monitoring)",
                "Oral Sodium Polystyrene Sulfonate (Kayexalate)",
                "IV Potassium Chloride in Normal Saline",
                "IV Furosemide 20 mg only"
            ),
            correctIndex = 0,
            explanation = "Tall, symmetrical, tented T waves are the earliest classic ECG hallmark of severe Hyperkalemia (K⁺ >6.5 mEq/L). IV 10% Calcium Gluconate acts within 1–3 minutes to antagonize cardiac membrane excitability and prevent fatal V-Fib/asystole (it does NOT lower serum K⁺). Follow immediately with IV Regular Insulin + 50% Dextrose and nebulized Salbutamol to shift K⁺ intracellularly, plus dialysis/diuretics to eliminate K⁺.",
            isCustom = false,
            sourceStatus = Question.SOURCE_VERIFIED,
            sourceNote = "ClinEdge Clinical Spotters • AIIMS NORCET High-Yield ECG",
            topicId = "pharm_electrolyte_emergencies",
            sourceType = QuestionSourceType.PRACTICE,
            imageUrl = "diagram://$DIAGRAM_ECG_HYPERKALEMIA"
        ),
        // 7. Macintosh Curved-Blade Laryngoscope (Photo Spotter)
        Question(
            id = "IBQ_007",
            category = "nur",
            shiftYear = "",
            subject = "FON",
            difficulty = QuestionDifficulty.MEDIUM,
            questionText = "Identify the curved laryngoscope blade shown on the sterile intubation tray above. During adult endotracheal intubation, where is the tip of this curved Macintosh blade properly positioned?",
            options = listOf(
                "In the vallecula (space between the base of the tongue and the anterior surface of the epiglottis)",
                "Directly posterior to the epiglottis to lift it directly (Miller technique)",
                "Inside the pyriform fossa lateral to the vocal cords",
                "Directly inside the upper esophageal sphincter"
            ),
            correctIndex = 0,
            explanation = "The pictured instrument is a Macintosh (curved) laryngoscope blade. Its tip is advanced into the vallecula to indirectly lift the epiglottis via the hyoepiglottic ligament and expose the glottic opening. By contrast, a straight Miller blade is placed posterior to the epiglottis to lift it directly (commonly preferred in neonates/infants).",
            isCustom = false,
            sourceStatus = Question.SOURCE_VERIFIED,
            sourceNote = "ClinEdge Clinical Spotters • NORCET Airway & OT Instruments",
            topicId = "fon_airway_management",
            sourceType = QuestionSourceType.PRACTICE,
            imageUrl = "drawable://$DRAWABLE_LARYNGOSCOPE"
        ),
        // 8. Endotracheal Tube Cuff Pressure & Murphy's Eye (Photo Spotter)
        Question(
            id = "IBQ_008",
            category = "nur",
            shiftYear = "",
            subject = "MSN",
            difficulty = QuestionDifficulty.MEDIUM,
            questionText = "Observe the cuffed Endotracheal Tube (7.5 mm ID) with its blue pilot balloon and distal side-port (Murphy's eye) shown above. What is the safe target ETT cuff pressure that prevents aspiration while avoiding tracheal mucosal necrosis?",
            options = listOf(
                "20–30 cm H₂O (approx. 15–22 mmHg)",
                "5–10 cm H₂O",
                "45–60 cm H₂O",
                "75–90 cm H₂O"
            ),
            correctIndex = 0,
            explanation = "Maintaining ETT cuff pressure between 20–30 cm H₂O (15–22 mmHg) seals the airway against micro-aspiration (reducing VAP) without exceeding tracheal capillary perfusion pressure (~30–35 cm H₂O), thus preventing tracheal ischemia, ulceration, or stenosis. The distal side opening (Murphy's eye) provides an alternate ventilation pathway if the main bevel becomes occluded against the tracheal wall.",
            isCustom = false,
            sourceStatus = Question.SOURCE_VERIFIED,
            sourceNote = "ClinEdge Clinical Spotters • NORCET Airway & ICU Care",
            topicId = "msn_critical_care",
            sourceType = QuestionSourceType.PRACTICE,
            imageUrl = "drawable://$DRAWABLE_LARYNGOSCOPE"
        ),
        // 9. Cheatle Transfer Forceps Sterile Technique (Photo Spotter)
        Question(
            id = "IBQ_009",
            category = "nur",
            shiftYear = "",
            subject = "FON",
            difficulty = QuestionDifficulty.EASY,
            questionText = "Look at the long angled Cheatle transfer forceps displayed on the surgical tray above. Which aseptic nursing principle is MANDATORY when holding Cheatle forceps to transfer sterile instruments from an autoclave drum?",
            options = listOf(
                "Always keep the tips pointing DOWNWARD below the waist/wrist level so disinfectant never flows from the unsterile handle onto the sterile tips",
                "Keep the tips pointing UPWARD toward the ceiling at all times",
                "Touch the inner rim of the sterilizer container to steady the blades",
                "Use Cheatle forceps directly to grasp bleeding vessels in an open wound"
            ),
            correctIndex = 0,
            explanation = "Cheatle forceps are used strictly to lift sterile dressings, gauze, and instruments without touching them by hand. The sterile tips must ALWAYS be held pointing downward so liquid/solution cannot run down onto the unsterile handle and back onto the sterile tips by gravity. Never touch the container edges.",
            isCustom = false,
            sourceStatus = Question.SOURCE_VERIFIED,
            sourceNote = "ClinEdge Clinical Spotters • NORCET Surgical Instruments",
            topicId = "fon_infection_control",
            sourceType = QuestionSourceType.PRACTICE,
            imageUrl = "drawable://$DRAWABLE_SURGICAL_TRAY"
        ),
        // 10. Cusco's Bivalve Speculum & Artery Forceps (Photo Spotter)
        Question(
            id = "IBQ_010",
            category = "nur",
            shiftYear = "",
            subject = "OBG",
            difficulty = QuestionDifficulty.EASY,
            questionText = "Identify the self-retaining bivalve vaginal speculum shown on the lower right of the sterile tray above. Which clinical procedure is this Cusco's speculum primarily used for?",
            options = listOf(
                "Direct visualization of the cervix and obtaining a Pap smear / VIA screening without needing an assistant to hold it",
                "Performing a Dilatation & Curettage (D&C) requiring wide posterior wall retraction",
                "Delivering the fetal head during obstructed second-stage labor",
                "Clamping the umbilical cord immediately after delivery"
            ),
            correctIndex = 0,
            explanation = "Cusco's self-retaining bivalve speculum has a thumb screw lock that holds both blades open hands-free, making it ideal for outpatient cervical inspection, Pap smear collection, IUCD (Copper-T) insertion/removal, and colposcopy. (Sims' double-bladed speculum is preferred for D&C and vaginal wall fistula inspection because Cusco's blades obscure the anterior/posterior vaginal walls.)",
            isCustom = false,
            sourceStatus = Question.SOURCE_VERIFIED,
            sourceNote = "ClinEdge Clinical Spotters • NORCET OBG & OT Instruments",
            topicId = "obg_instruments",
            sourceType = QuestionSourceType.PRACTICE,
            imageUrl = "drawable://$DRAWABLE_SURGICAL_TRAY"
        ),
        // 11. 18G Green IV Cannula Spotter
        Question(
            id = "IBQ_011",
            category = "nur",
            shiftYear = "",
            subject = "FON",
            difficulty = QuestionDifficulty.EASY,
            questionText = "Identify the ISO color-coded peripheral IV cannula shown above (GREEN hub, outer diameter 1.3 mm, flow rate ~90 mL/min). What is its gauge size and primary clinical indication?",
            options = listOf(
                "18 Gauge — Rapid crystalloid resuscitation, major surgery, and packed RBC blood transfusion",
                "20 Gauge — Routine adult maintenance IV fluids only",
                "22 Gauge — Fragile elderly and pediatric veins",
                "24 Gauge — Neonatal ICU infusions"
            ),
            correctIndex = 0,
            explanation = "ISO IV Cannula color-coding (high-yield NORCET/RRB classic): 14G Orange (trauma), 16G Grey (massive transfusion), 18G GREEN (~90 mL/min — blood transfusion, surgery, rapid fluids), 20G Pink (~60 mL/min — general adult IV), 22G Blue (~36 mL/min — children/fragile veins), 24G Yellow (~20 mL/min — neonates/infants), 26G Violet (NICU).",
            isCustom = false,
            sourceStatus = Question.SOURCE_VERIFIED,
            sourceNote = "ClinEdge Clinical Spotters • ISO IV Cannula Color Code",
            topicId = "fon_iv_therapy",
            sourceType = QuestionSourceType.PRACTICE,
            imageUrl = "diagram://$DIAGRAM_IV_CANNULA_18G"
        ),
        // 12. 22G Blue IV Cannula Spotter
        Question(
            id = "IBQ_012",
            category = "nur",
            shiftYear = "",
            subject = "Peds",
            difficulty = QuestionDifficulty.EASY,
            questionText = "Look at the BLUE color-coded peripheral IV cannula illustrated above (outer diameter 0.9 mm, flow rate ~36 mL/min). Which patient group is this 22-Gauge cannula most appropriately selected for?",
            options = listOf(
                "Pediatric patients, older adults with fragile veins, and routine non-emergent IV medications",
                "Massive hemorrhagic shock requiring 2 units of rapid PRBC transfusion",
                "Central venous pressure (CVP) monitoring in the internal jugular vein",
                "Intraosseous emergency access in cardiac arrest"
            ),
            correctIndex = 0,
            explanation = "The 22G BLUE cannula (0.9 mm OD, ~36 mL/min) is the standard choice for children, elderly patients with small/fragile veins, and routine antibiotics or maintenance fluids. Smaller gauge number = larger bore lumen (18G Green > 20G Pink > 22G Blue > 24G Yellow).",
            isCustom = false,
            sourceStatus = Question.SOURCE_VERIFIED,
            sourceNote = "ClinEdge Clinical Spotters • ISO IV Cannula Color Code",
            topicId = "peds_procedures",
            sourceType = QuestionSourceType.PRACTICE,
            imageUrl = "diagram://$DIAGRAM_IV_CANNULA_22G"
        ),
        // 13. Biomedical Waste RED Bin Spotter
        Question(
            id = "IBQ_013",
            category = "nur",
            shiftYear = "",
            subject = "CHN",
            difficulty = QuestionDifficulty.EASY,
            questionText = "According to India's Bio-Medical Waste Management Rules (2016 & Amendments), which waste items belong strictly in the highlighted RED non-chlorinated plastic bin shown above?",
            options = listOf(
                "Contaminated recyclable plastic waste — IV tubing, Foley catheters, urine bags, Vacutainers, and syringes WITHOUT fixed needles",
                "Blood-soaked cotton swabs, soiled bandages, placenta, and human anatomical waste",
                "Used metallic hypodermic needles, scalpel blades, and lancets",
                "Broken glass ampoules, metallic body implants, and glass medicine vials"
            ),
            correctIndex = 0,
            explanation = "BMW Rules 2016 Color Codes: RED = Contaminated recyclable plastics (tubing, catheters, IV bottles, syringes without needles, gloves) → Autoclaving/microwaving + shredding; YELLOW = Anatomical, soiled cotton/dressings, expired/cytotoxic drugs → Incineration; WHITE (translucent puncture-proof) = Metal sharps/needles; BLUE = Broken glassware & metallic implants.",
            isCustom = false,
            sourceStatus = Question.SOURCE_VERIFIED,
            sourceNote = "ClinEdge Clinical Spotters • India BMW Rules 2016",
            topicId = "chn_biomedical_waste",
            sourceType = QuestionSourceType.PRACTICE,
            imageUrl = "diagram://$DIAGRAM_BMW_RED"
        ),
        // 14. Biomedical Waste YELLOW Bin Spotter
        Question(
            id = "IBQ_014",
            category = "nur",
            shiftYear = "",
            subject = "CHN",
            difficulty = QuestionDifficulty.MEDIUM,
            questionText = "After assisting a normal vaginal delivery and dressing a surgical wound, where must the nurse discard the placenta, blood-soaked gauze pads, and an expired vial of cytotoxic chemotherapy drug as highlighted in the BMW segregation chart above?",
            options = listOf(
                "YELLOW non-chlorinated plastic bag (sent for incineration / plasma pyrolysis / deep burial)",
                "RED non-chlorinated plastic bag (sent for autoclaving and recycling)",
                "BLUE cardboard box with blue colored marking",
                "WHITE puncture-proof leak-proof translucent container"
            ),
            correctIndex = 0,
            explanation = "The YELLOW category covers Human Anatomical Waste (placenta, amputated tissue), Soiled Waste (blood/body-fluid-soaked cotton, dressings, plaster casts), Expired/Discarded Medicines & Cytotoxic Drugs (with cytotoxic label), and microbiology blood bags. All yellow waste is incinerated or deep-buried — NEVER recycled.",
            isCustom = false,
            sourceStatus = Question.SOURCE_VERIFIED,
            sourceNote = "ClinEdge Clinical Spotters • India BMW Rules 2016",
            topicId = "chn_biomedical_waste",
            sourceType = QuestionSourceType.PRACTICE,
            imageUrl = "diagram://$DIAGRAM_BMW_YELLOW"
        ),
        // 15. Venturi Oxygen Mask Spotter
        Question(
            id = "IBQ_015",
            category = "nur",
            shiftYear = "",
            subject = "MSN",
            difficulty = QuestionDifficulty.MEDIUM,
            questionText = "Identify the high-flow oxygen delivery device illustrated above featuring interchangeable color-coded jet-mixing adapters (Blue 24%, White 28%, Yellow 35%, Red 40%, Green 60%). For which clinical condition is this device the gold standard?",
            options = listOf(
                "Acute exacerbation of COPD requiring precise, fixed low-concentration FiO₂ (24%–28%) to avoid blunting hypoxic respiratory drive",
                "Carbon monoxide poisoning requiring ~95–100% FiO₂ via reservoir bag",
                "Invasive mechanical ventilation in ARDS",
                "Home long-term oxygen therapy during eating and speaking"
            ),
            correctIndex = 0,
            explanation = "The Venturi (Air-Entrainment) Mask is a high-flow device that delivers the MOST ACCURATE, fixed FiO₂ (24% to 60%) regardless of the patient's breathing pattern or minute ventilation. It is the gold standard in COPD patients with chronic CO₂ retention to titrate SpO₂ safely to 88–92%. (Non-rebreather mask is used for CO poisoning/severe hypoxia requiring ~90–95% FiO₂.)",
            isCustom = false,
            sourceStatus = Question.SOURCE_VERIFIED,
            sourceNote = "ClinEdge Clinical Spotters • Respiratory & Critical Care",
            topicId = "msn_respiratory_care",
            sourceType = QuestionSourceType.PRACTICE,
            imageUrl = "diagram://$DIAGRAM_VENTURI_MASK"
        )
    )
}
