package com.example.data.model

import java.util.Locale

/**
 * ClinEdge v3.0.0 — Active-Recall Rapid Flashcard Studio & Focused Study Timer Logic.
 *
 * Pure-Kotlin models & deterministic helpers (zero Android dependencies) so JVM unit tests
 * and Android runtime stay 100% consistent.
 */
enum class RapidRecallDeckCategory(val label: String) {
    DRUGS_DOC("Drug of Choice"),
    IMMUNIZATION("NIS & Vaccines"),
    PROCEDURES("Positions & Gauges"),
    CLINICAL_SIGNS("Signs & Triads"),
    OBG_PEDS("OBG & Peds"),
    CA_GK("CA & GK Facts")
}

data class RapidRecallCard(
    val id: String,
    val category: RapidRecallDeckCategory,
    val frontPrompt: String,
    val backAnswer: String,
    val examPearl: String,
    val examTag: String = "NORCET · RRB"
)

object RapidRecallCatalog {

    val BUILTIN_CLINICAL_CARDS: List<RapidRecallCard> = listOf(
        // ==================== DRUG OF CHOICE (6) ====================
        RapidRecallCard(
            id = "RR_DOC_01",
            category = RapidRecallDeckCategory.DRUGS_DOC,
            frontPrompt = "Drug of choice to prevent and control convulsions in Eclampsia?",
            backAnswer = "Magnesium Sulfate (MgSO4) — Pritchard Regimen (4 g IV loading + 10 g deep IM, then 5 g IM q4h).",
            examPearl = "Toxicity triad to monitor before every dose: Patellar reflex present, RR ≥ 16/min, Urine output ≥ 30 mL/hr. Antidote: 10% Calcium Gluconate 10 mL slow IV.",
            examTag = "NORCET · RRB · PGIMER"
        ),
        RapidRecallCard(
            id = "RR_DOC_02",
            category = RapidRecallDeckCategory.DRUGS_DOC,
            frontPrompt = "First-line life-saving drug, dose, and route for Acute Anaphylaxis?",
            backAnswer = "Epinephrine (Adrenaline) 1:1000 — 0.3 to 0.5 mg IM into the anterolateral thigh (vastus lateralis).",
            examPearl = "IM injection into the vastus lateralis achieves peak plasma concentration faster than subcutaneous or deltoid routes; repeat every 5–15 minutes if needed.",
            examTag = "NORCET · RRB · ESIC"
        ),
        RapidRecallCard(
            id = "RR_DOC_03",
            category = RapidRecallDeckCategory.DRUGS_DOC,
            frontPrompt = "Immediate first-line antidote sequence for Organophosphate (OP) poisoning?",
            backAnswer = "IV Atropine first (until atropinization — clear lung secretions), followed by Pralidoxime (2-PAM).",
            examPearl = "Atropine blocks muscarinic bronchorrhea (the immediate killer); 2-PAM reactivates acetylcholinesterase before enzyme 'aging' occurs.",
            examTag = "NORCET · PGIMER"
        ),
        RapidRecallCard(
            id = "RR_DOC_04",
            category = RapidRecallDeckCategory.DRUGS_DOC,
            frontPrompt = "First-line Drug of Choice for Status Epilepticus (seizure > 5 minutes)?",
            backAnswer = "IV Lorazepam 0.1 mg/kg (or IV Diazepam / IM Midazolam if IV access is unavailable).",
            examPearl = "Benzodiazepines stop active seizures rapidly; follow with a long-acting antiepileptic such as IV Fosphenytoin, Levetiracetam, or Valproate to prevent recurrence.",
            examTag = "NORCET · RRB"
        ),
        RapidRecallCard(
            id = "RR_DOC_05",
            category = RapidRecallDeckCategory.DRUGS_DOC,
            frontPrompt = "Drug of choice for Visceral Leishmaniasis (Kala-Azar) under India's NVBDCP?",
            backAnswer = "Single-dose Liposomal Amphotericin B (10 mg/kg IV infusion); oral alternative: Miltefosine.",
            examPearl = "Transmitted by female Phlebotomus argentipes (Sandfly). Miltefosine is teratogenic and strictly contraindicated in pregnancy.",
            examTag = "RRB · CHN · ESIC"
        ),
        RapidRecallCard(
            id = "RR_DOC_06",
            category = RapidRecallDeckCategory.DRUGS_DOC,
            frontPrompt = "Drug of choice for uncomplicated Plasmodium falciparum malaria in India?",
            backAnswer = "ACT (Artemisinin-based Combination Therapy): Artesunate + Sulfadoxine-Pyrimethamine (AS+SP) for 3 days + single-dose Primaquine on Day 2.",
            examPearl = "In North-Eastern Indian states (due to SP resistance), Artemether-Lumefantrine (ACT-AL) is used instead. Primaquine is contraindicated in G6PD deficiency & pregnancy.",
            examTag = "RRB · NORCET · CHN"
        ),

        // ==================== NIS INDIA & VACCINES (5) ====================
        RapidRecallCard(
            id = "RR_NIS_01",
            category = RapidRecallDeckCategory.IMMUNIZATION,
            frontPrompt = "BCG vaccine dose, route, site, and diluent under India's National Immunization Schedule?",
            backAnswer = "0.05 mL (< 1 month) or 0.1 mL (> 1 month), Intradermal (ID), Left Upper Arm, reconstituted ONLY with Normal Saline.",
            examPearl = "Use within 4 hours of reconstitution (NOT eligible for Open Vial Policy). Reconstituting BCG with sterile water causes severe local irritation.",
            examTag = "RRB · ESIC · NORCET"
        ),
        RapidRecallCard(
            id = "RR_NIS_02",
            category = RapidRecallDeckCategory.IMMUNIZATION,
            frontPrompt = "Measles-Rubella (MR) vaccine schedule, route, and injection site?",
            backAnswer = "0.5 mL Subcutaneous (SC) in the Right Upper Arm — Dose 1 at 9–12 months, Dose 2 at 16–24 months.",
            examPearl = "Given alongside Vitamin A 1st dose (1 lakh IU oral) at 9 months. Reconstituted MR vaccine must be discarded after 4 hours to prevent Toxic Shock Syndrome.",
            examTag = "RRB · CHN · DSSSB"
        ),
        RapidRecallCard(
            id = "RR_NIS_03",
            category = RapidRecallDeckCategory.IMMUNIZATION,
            frontPrompt = "Optimal Cold Chain temperature maintained inside an Ice-Lined Refrigerator (ILR) at PHC level?",
            backAnswer = "+2°C to +8°C (checked and logged twice daily).",
            examPearl = "Heat-sensitive vaccines (OPV, Measles, BCG) are kept at the BOTTOM of the ILR; freeze-sensitive vaccines (Pentavalent, Hep B, Td, IPV) are kept in the UPPER baskets.",
            examTag = "RRB · NORCET · CHN"
        ),
        RapidRecallCard(
            id = "RR_NIS_04",
            category = RapidRecallDeckCategory.IMMUNIZATION,
            frontPrompt = "How do you interpret Vaccine Vial Monitor (VVM) Stages 1, 2, 3, and 4?",
            backAnswer = "Stage 1 & 2 (inner square lighter than outer circle) = USE. Stage 3 & 4 (inner square matches or is darker than circle) = DISCARD.",
            examPearl = "VVM measures cumulative heat exposure over time — it does NOT detect freeze damage (use the Shake Test for freeze-sensitive vaccines).",
            examTag = "RRB · NORCET · ESIC"
        ),
        RapidRecallCard(
            id = "RR_NIS_05",
            category = RapidRecallDeckCategory.IMMUNIZATION,
            frontPrompt = "Which vaccines are EXCLUDED from India's 28-Day Open Vial Policy?",
            backAnswer = "Reconstituted live vaccines: BCG, Measles-Rubella (MR), Japanese Encephalitis (JE), and Rotavirus.",
            examPearl = "Liquid vaccines eligible for 28-day reuse (if cold chain & VVM intact): OPV, IPV, Pentavalent, Hepatitis B, PCV, and Td.",
            examTag = "RRB · CHN"
        ),

        // ==================== POSITIONS, GAUGES & PROCEDURES (6) ====================
        RapidRecallCard(
            id = "RR_PROC_01",
            category = RapidRecallDeckCategory.PROCEDURES,
            frontPrompt = "Color-coded IV Cannula gauges from 16G to 24G (ISO Standard)?",
            backAnswer = "16G Grey (trauma/rapid blood) · 18G Green (blood transfusion/OT) · 20G Pink (standard adult) · 22G Blue (elderly/children) · 24G Yellow (neonates).",
            examPearl = "Remember: Smaller gauge number = LARGER internal bore diameter and higher mL/min flow rate. 18G Green is the classic NORCET spotter for adult blood transfusion.",
            examTag = "NORCET · RRB · PGIMER"
        ),
        RapidRecallCard(
            id = "RR_PROC_02",
            category = RapidRecallDeckCategory.PROCEDURES,
            frontPrompt = "Optimal patient position and vertebral interspace for adult Lumbar Puncture (LP)?",
            backAnswer = "Lateral recumbent knee-chest (C-shape / fetal position) at L3–L4 or L4–L5 interspace.",
            examPearl = "The adult spinal cord terminates at L1–L2 (conus medullaris), making L3–L5 safe. Post-LP: keep patient flat/supine and hydrate to prevent post-dural puncture headache.",
            examTag = "NORCET · RRB"
        ),
        RapidRecallCard(
            id = "RR_PROC_03",
            category = RapidRecallDeckCategory.PROCEDURES,
            frontPrompt = "Emergency nursing position for suspected Central Venous Air Embolism?",
            backAnswer = "Left Lateral Trendelenburg position (Durant's maneuver).",
            examPearl = "Traps the air bubble in the right atrium/ventricle apex so it cannot enter the pulmonary outflow tract and cause fatal pulmonary obstruction.",
            examTag = "NORCET · PGIMER"
        ),
        RapidRecallCard(
            id = "RR_PROC_04",
            category = RapidRecallDeckCategory.PROCEDURES,
            frontPrompt = "How is Nasogastric (Ryle's) tube insertion length measured (NEX method) & confirmed?",
            backAnswer = "Tip of Nose → Earlobe → Xiphoid process (NEX). Gold-standard placement confirmation: Chest/Abdominal X-ray (bedside: gastric aspirate pH ≤ 5.0).",
            examPearl = "NEVER rely solely on air auscultation ('whoosh test') for NG placement — X-ray is the only definitive confirmation before starting enteral feeds.",
            examTag = "NORCET · RRB · FON"
        ),
        RapidRecallCard(
            id = "RR_PROC_05",
            category = RapidRecallDeckCategory.PROCEDURES,
            frontPrompt = "Why is the Z-Track technique used for deep IM injections like Iron Dextran?",
            backAnswer = "Pulling skin/subcutaneous tissue laterally by 2.5–3.5 cm seals the needle track inside the muscle, preventing medication leakage and permanent skin staining.",
            examPearl = "Do NOT massage the injection site after a Z-Track IM injection — wait 10 seconds before withdrawing the needle, then release the skin.",
            examTag = "RRB · NORCET · FON"
        ),
        RapidRecallCard(
            id = "RR_PROC_06",
            category = RapidRecallDeckCategory.PROCEDURES,
            frontPrompt = "Correct patient position during Thoracentesis and post-procedure positioning?",
            backAnswer = "During: Sitting upright leaning forward over an overbed cardiac table. After: Lie on the UNAFFECTED side for 1 hour to let the puncture site seal.",
            examPearl = "Remove no more than 1,000–1,500 mL of pleural fluid in one sitting to prevent re-expansion pulmonary edema and mediastinal shift.",
            examTag = "NORCET · PGIMER"
        ),

        // ==================== CLINICAL SIGNS & DIAGNOSTICS (7) ====================
        RapidRecallCard(
            id = "RR_SIGN_01",
            category = RapidRecallDeckCategory.CLINICAL_SIGNS,
            frontPrompt = "What electrolyte imbalance causes positive Chvostek's and Trousseau's signs?",
            backAnswer = "Hypocalcemia (Serum Ca2+ < 8.5 mg/dL) — also seen in Hypomagnesemia and post-thyroidectomy parathyroid injury.",
            examPearl = "Chvostek = facial muscle twitch on tapping CN VII anterior to ear. Trousseau = carpopedal spasm after inflating BP cuff above SBP for 3 minutes (more specific).",
            examTag = "NORCET · RRB · PGIMER"
        ),
        RapidRecallCard(
            id = "RR_SIGN_02",
            category = RapidRecallDeckCategory.CLINICAL_SIGNS,
            frontPrompt = "Differentiate Kernig's sign and Brudzinski's sign in Bacterial Meningitis?",
            backAnswer = "Kernig's: pain/resistance extending the knee with hip flexed at 90°. Brudzinski's: involuntary flexion of hips & knees when the neck is passively flexed.",
            examPearl = "Both indicate meningeal irritation. Implement Droplet Precautions immediately for at least 24 hours after starting effective IV antibiotics.",
            examTag = "RRB · NORCET · ESIC"
        ),
        RapidRecallCard(
            id = "RR_SIGN_03",
            category = RapidRecallDeckCategory.CLINICAL_SIGNS,
            frontPrompt = "What are Cullen's sign and Grey Turner's sign, and what condition do they indicate?",
            backAnswer = "Cullen's = periumbilical ecchymosis (bruising); Grey Turner's = flank ecchymosis. Both signify severe Acute Hemorrhagic Pancreatitis (retroperitoneal bleeding).",
            examPearl = "Priority nursing care in acute pancreatitis: strict NPO (rest pancreas), aggressive IV crystalloids, opioid analgesia, and monitor serum lipase (more specific than amylase).",
            examTag = "NORCET · PGIMER"
        ),
        RapidRecallCard(
            id = "RR_SIGN_04",
            category = RapidRecallDeckCategory.CLINICAL_SIGNS,
            frontPrompt = "What is Murphy's sign and how is it elicited at the bedside?",
            backAnswer = "Sudden inspiratory arrest due to sharp RUQ pain when the examiner palpates deeply under the right costal margin — classic for Acute Cholecystitis.",
            examPearl = "Patients with cholecystitis should follow a low-fat diet to reduce cholecystokinin-triggered gallbladder contractions.",
            examTag = "RRB · NORCET"
        ),
        RapidRecallCard(
            id = "RR_SIGN_05",
            category = RapidRecallDeckCategory.CLINICAL_SIGNS,
            frontPrompt = "What breathing pattern is seen in Diabetic Ketoacidosis (DKA) and why?",
            backAnswer = "Kussmaul respirations — deep, rapid, labored breathing with fruity acetone breath odor.",
            examPearl = "It is a compensatory respiratory mechanism blowing off CO2 to correct severe High Anion Gap Metabolic Acidosis.",
            examTag = "RRB · NORCET · ESIC"
        ),
        RapidRecallCard(
            id = "RR_SIGN_06",
            category = RapidRecallDeckCategory.CLINICAL_SIGNS,
            frontPrompt = "What are Koplik's spots and which disease are they pathognomonic for?",
            backAnswer = "Small bluish-white spots on an erythematous base on the buccal mucosa opposite the lower 2nd molars — pathognomonic for Measles (Rubeola).",
            examPearl = "Koplik's spots appear 1–2 days BEFORE the maculopapular rash (which starts behind the ears and spreads downward). Requires Airborne Precautions.",
            examTag = "RRB · Peds · CHN"
        ),
        RapidRecallCard(
            id = "RR_SIGN_07",
            category = RapidRecallDeckCategory.CLINICAL_SIGNS,
            frontPrompt = "When is a positive Babinski reflex (dorsiflexion of great toe + fanning) normal vs abnormal?",
            backAnswer = "Normal in infants up to 12–18 months (incomplete corticospinal myelination); ABNORMAL in adults, indicating an Upper Motor Neuron (UMN) lesion.",
            examPearl = "Other primitive newborn reflexes: Moro disappears by 3–6 months, Rooting by 3–4 months, Palmar grasp by 3–4 months.",
            examTag = "NORCET · RRB · Peds"
        ),

        // ==================== OBG & PEDIATRICS MILESTONES (6) ====================
        RapidRecallCard(
            id = "RR_OBG_01",
            category = RapidRecallDeckCategory.OBG_PEDS,
            frontPrompt = "How is Estimated Date of Delivery (EDD) calculated using Naegele's Rule?",
            backAnswer = "First day of Last Menstrual Period (LMP) + 9 calendar months + 7 days (or subtract 3 months + 7 days + 1 year).",
            examPearl = "Assumes a regular 28-day menstrual cycle with ovulation on Day 14. First-trimester Crown-Rump Length (CRL) ultrasound is the most accurate dating method.",
            examTag = "RRB · NORCET · OBG"
        ),
        RapidRecallCard(
            id = "RR_OBG_02",
            category = RapidRecallDeckCategory.OBG_PEDS,
            frontPrompt = "Normal baseline Fetal Heart Rate (FHR) and normal variability range?",
            backAnswer = "Baseline FHR: 110–160 bpm · Moderate (reassuring) variability: 6–25 bpm.",
            examPearl = "FHR < 110 bpm for ≥ 10 min = Fetal Bradycardia (hypoxia/cord prolapse); > 160 bpm for ≥ 10 min = Fetal Tachycardia (earliest sign of maternal fever/chorioamnionitis).",
            examTag = "NORCET · RRB · OBG"
        ),
        RapidRecallCard(
            id = "RR_OBG_03",
            category = RapidRecallDeckCategory.OBG_PEDS,
            frontPrompt = "When does a healthy infant's birth weight double, triple, and quadruple?",
            backAnswer = "Doubles by 5–6 months · Triples by 12 months (1 year) · Quadruples by 2 years.",
            examPearl = "Physiological weight loss of up to 7–10% in the first week of life is normal and regained by Day 10–14.",
            examTag = "RRB · Peds · ESIC"
        ),
        RapidRecallCard(
            id = "RR_OBG_04",
            category = RapidRecallDeckCategory.OBG_PEDS,
            frontPrompt = "Normal closure timeline of the Posterior vs Anterior Fontanelle?",
            backAnswer = "Posterior Fontanelle (triangular, lambda): closes at 6–8 weeks · Anterior Fontanelle (diamond, bregma): closes at 12–18 months.",
            examPearl = "Sunken anterior fontanelle = dehydration; bulging tense fontanelle at rest = increased ICP / meningitis / hydrocephalus.",
            examTag = "RRB · NORCET · Peds"
        ),
        RapidRecallCard(
            id = "RR_OBG_05",
            category = RapidRecallDeckCategory.OBG_PEDS,
            frontPrompt = "High-yield Pediatric Milestones: Social smile, Head control, Sit without support, Pincer grasp?",
            backAnswer = "Social smile: 2 months · Head control: 3–4 months · Sits without support: 7–8 months · Mature pincer grasp: 9–10 months · Walks alone: 12–15 months.",
            examPearl = "Red flag: Failure to achieve head control by 4–5 months or no social smile by 3 months requires immediate developmental screening.",
            examTag = "RRB · NORCET · Peds"
        ),
        RapidRecallCard(
            id = "RR_OBG_06",
            category = RapidRecallDeckCategory.OBG_PEDS,
            frontPrompt = "On a WHO Partograph, when does plotting start and how far apart are Alert & Action lines?",
            backAnswer = "Plotting starts in Active Phase at 4 cm cervical dilation (WHO Modified Partograph). Alert & Action lines are 4 hours apart (expected progress ≥ 1 cm/hr).",
            examPearl = "Crossing the Alert line warrants close monitoring/transfer to a FRU; crossing the Action line requires active obstetric intervention (augmentation or C-section).",
            examTag = "NORCET · RRB · OBG"
        )
    )

    fun buildUnifiedDeck(caFlashcards: List<Flashcard>): List<RapidRecallCard> {
        val caCards = caFlashcards.mapIndexed { idx, card ->
            RapidRecallCard(
                id = "RR_CA_${idx + 1}_${card.title.hashCode()}",
                category = RapidRecallDeckCategory.CA_GK,
                frontPrompt = card.title,
                backAnswer = card.body,
                examPearl = "Category: ${card.tag.ifBlank { "Current Affairs & GK" }} — Quick revision fact for RRB & Non-Nursing 20–30% sections.",
                examTag = card.tag.ifBlank { "CA · GK" }
            )
        }
        return BUILTIN_CLINICAL_CARDS + caCards
    }

    fun filterDeck(
        deck: List<RapidRecallCard>,
        category: RapidRecallDeckCategory? = null,
        masteredIds: Set<String> = emptySet(),
        unmasteredOnly: Boolean = false,
        query: String = ""
    ): List<RapidRecallCard> {
        val q = query.trim().lowercase(Locale.US)
        return deck.filter { card ->
            val catMatch = category == null || card.category == category
            val masteryMatch = !unmasteredOnly || card.id !in masteredIds
            val queryMatch = q.isBlank() ||
                card.frontPrompt.lowercase(Locale.US).contains(q) ||
                card.backAnswer.lowercase(Locale.US).contains(q) ||
                card.examPearl.lowercase(Locale.US).contains(q) ||
                card.examTag.lowercase(Locale.US).contains(q)
            catMatch && masteryMatch && queryMatch
        }
    }

    fun serializeMasteredIds(ids: Set<String>): String {
        return ids.asSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
            .joinToString("\n")
    }

    fun deserializeMasteredIds(raw: String?): Set<String> {
        if (raw.isNullOrBlank()) return emptySet()
        return raw.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()
    }
}

// ============================================================================
// FOCUSED STUDY TIMER (POMODORO & EXAM STAMINA SPRINT)
// ============================================================================

enum class FocusTimerPreset(
    val label: String,
    val durationMinutes: Int,
    val subtitle: String,
    val isBreak: Boolean = false
) {
    POMODORO_25("25m Pomodoro", 25, "Classic focused revision sprint", isBreak = false),
    DEEP_STUDY_50("50m Deep Study", 50, "Subject mastery & PYQ block", isBreak = false),
    CBT_STAMINA_90("90m CBT Stamina", 90, "Full RRB / NORCET endurance", isBreak = false),
    SHORT_BREAK_5("5m Break", 5, "Hydration & eye-strain reset", isBreak = true);

    val totalSeconds: Int get() = durationMinutes * 60
}

data class DailyFocusProgress(
    val dateKey: String = "",
    val completedMinutes: Int = 0,
    val completedSessions: Int = 0
) {
    fun effectiveMinutes(todayKey: String = StudyGoalLogic.todayDateKey()): Int =
        if (dateKey == todayKey) completedMinutes.coerceAtLeast(0) else 0

    fun effectiveSessions(todayKey: String = StudyGoalLogic.todayDateKey()): Int =
        if (dateKey == todayKey) completedSessions.coerceAtLeast(0) else 0
}

object FocusTimerLogic {

    fun formatClock(remainingSeconds: Int): String {
        val safe = remainingSeconds.coerceAtLeast(0)
        val hours = safe / 3600
        val mins = (safe % 3600) / 60
        val secs = safe % 60
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, mins, secs)
        } else {
            String.format(Locale.US, "%02d:%02d", mins, secs)
        }
    }

    fun recordCompletedSession(
        current: DailyFocusProgress,
        preset: FocusTimerPreset,
        todayKey: String = StudyGoalLogic.todayDateKey()
    ): DailyFocusProgress {
        val baseMinutes = current.effectiveMinutes(todayKey)
        val baseSessions = current.effectiveSessions(todayKey)
        if (preset.isBreak) {
            return DailyFocusProgress(
                dateKey = todayKey,
                completedMinutes = baseMinutes,
                completedSessions = baseSessions
            )
        }
        return DailyFocusProgress(
            dateKey = todayKey,
            completedMinutes = (baseMinutes + preset.durationMinutes).coerceAtMost(1440),
            completedSessions = (baseSessions + 1).coerceAtMost(99)
        )
    }

    fun serializeProgress(progress: DailyFocusProgress): String {
        return "${progress.dateKey}|${progress.completedMinutes.coerceAtLeast(0)}|${progress.completedSessions.coerceAtLeast(0)}"
    }

    fun deserializeProgress(raw: String?): DailyFocusProgress {
        if (raw.isNullOrBlank()) return DailyFocusProgress()
        val parts = raw.split('|')
        if (parts.size < 3) return DailyFocusProgress()
        val date = StudyGoalLogic.sanitizeDateKey(parts[0])
        val mins = parts[1].toIntOrNull()?.coerceIn(0, 1440) ?: 0
        val sessions = parts[2].toIntOrNull()?.coerceIn(0, 99) ?: 0
        return DailyFocusProgress(
            dateKey = date,
            completedMinutes = mins,
            completedSessions = sessions
        )
    }
}
