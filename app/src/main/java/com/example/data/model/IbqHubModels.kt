package com.example.data.model

import java.util.Locale
import kotlin.math.roundToInt

/**
 * High-yield clinical categories for the dedicated Image-Based Questions (IBQ) Hub.
 * Supports built-in offline visual diagrams, bundled spotter photography, and future OTA remote IBQs.
 */
enum class IbqCategory(
    val label: String,
    val shortLabel: String,
    val subtitle: String
) {
    ALL(
        label = "All IBQs",
        shortLabel = "All",
        subtitle = "Complete visual spotter bank"
    ),
    ECG(
        label = "ECG Strips",
        shortLabel = "ECG",
        subtitle = "Arrhythmias, blocks, STEMI & electrolytes"
    ),
    AIRWAY_OT(
        label = "Airway & OT",
        shortLabel = "Airway & OT",
        subtitle = "Laryngoscopes, ETT & surgical trays"
    ),
    OBG_CTG(
        label = "OBG & CTG",
        shortLabel = "OBG & CTG",
        subtitle = "Fetal monitoring, partograph & specula"
    ),
    DEVICES_COLOR(
        label = "Devices & Codes",
        shortLabel = "Devices",
        subtitle = "IV cannulas, BMW bins & O₂ masks"
    ),
    CLINICAL_IMAGING(
        label = "Imaging & Visuals",
        shortLabel = "Imaging",
        subtitle = "Chest X-rays, burns TBSA & clinical charts"
    )
}

enum class IbqStatusFilter(val label: String) {
    ALL("All"),
    UNATTEMPTED("Unseen"),
    MISTAKES("Mistakes"),
    MASTERED("Mastered")
}

data class IbqCategoryStat(
    val category: IbqCategory,
    val totalCount: Int,
    val attemptedCount: Int,
    val correctCount: Int
) {
    val accuracyPercent: Int
        get() = if (attemptedCount == 0) 0 else ((correctCount.toDouble() / attemptedCount) * 100.0).roundToInt()
}

data class IbqHubSummary(
    val totalCount: Int,
    val attemptedCount: Int,
    val correctCount: Int,
    val unseenCount: Int,
    val mistakeCount: Int,
    val categoryStats: List<IbqCategoryStat>
) {
    val accuracyPercent: Int
        get() = if (attemptedCount == 0) 0 else ((correctCount.toDouble() / attemptedCount) * 100.0).roundToInt()
}

/**
 * Pure-Kotlin IBQ categorization, filtering, analytics, and general-practice mixing engine.
 * Operates strictly on the unified Room QuestionItem bank (zero question duplication).
 */
object IbqHubLogic {

    /**
     * Classifies any image-based question (built-in diagram, bundled drawable, or remote OTA image)
     * into a specific non-ALL IbqCategory.
     */
    fun categorize(question: Question): IbqCategory {
        val ref = ClinicalImageRef.parse(question.imageUrl)
        val key = when (ref) {
            is ClinicalImageRef.Diagram -> ref.key.lowercase(Locale.US)
            is ClinicalImageRef.Drawable -> ref.key.lowercase(Locale.US)
            is ClinicalImageRef.RemoteUrl -> ref.url.lowercase(Locale.US)
            ClinicalImageRef.None -> ""
        }
        val topic = question.topicId.lowercase(Locale.US)
        val subject = question.subject.uppercase(Locale.US)
        val text = question.questionText.lowercase(Locale.US)

        return when {
            key.startsWith("ecg_") || key.contains("ecg") || topic.contains("cardiac") ||
                text.contains("ecg") || text.contains("electrocardiogram") -> IbqCategory.ECG

            key.startsWith("spotter_ctg_") || key.startsWith("spotter_partograph") ||
                subject == "OBG" || topic.startsWith("obg_") ||
                text.contains("cardiotocography") || text.contains("partograph") ||
                text.contains("speculum") -> IbqCategory.OBG_CTG

            key == ClinicalSpotterCatalog.DRAWABLE_LARYNGOSCOPE ||
                key == ClinicalSpotterCatalog.DRAWABLE_SURGICAL_TRAY ||
                topic.contains("airway") || topic.contains("instrument") ||
                text.contains("laryngoscope") || text.contains("forceps") ||
                text.contains("endotracheal tube") -> IbqCategory.AIRWAY_OT

            key.startsWith("spotter_iv_") || key.startsWith("spotter_bmw_") ||
                key.startsWith("spotter_venturi") || key.startsWith("spotter_nrb") ||
                topic.contains("iv_therapy") || topic.contains("biomedical_waste") ||
                text.contains("cannula") || text.contains("bio-medical waste") ||
                text.contains("venturi") || text.contains("non-rebreather") -> IbqCategory.DEVICES_COLOR

            key.startsWith("spotter_cxr_") || key.startsWith("spotter_rule_of_nines") ||
                text.contains("radiograph") || text.contains("x-ray") ||
                text.contains("rule of nines") || text.contains("pneumothorax") ||
                text.contains("pleural effusion") -> IbqCategory.CLINICAL_IMAGING

            else -> IbqCategory.CLINICAL_IMAGING
        }
    }

    fun filterQuestions(
        items: List<QuestionItem>,
        category: IbqCategory = IbqCategory.ALL,
        statusFilter: IbqStatusFilter = IbqStatusFilter.ALL,
        searchQuery: String = ""
    ): List<QuestionItem> {
        val cleanQuery = searchQuery.trim().lowercase(Locale.US)
        return items.asSequence()
            .filter { it.question.isImageBased }
            .filter { item ->
                category == IbqCategory.ALL || categorize(item.question) == category
            }
            .filter { item ->
                when (statusFilter) {
                    IbqStatusFilter.ALL -> true
                    IbqStatusFilter.UNATTEMPTED -> item.attemptsCount == 0
                    IbqStatusFilter.MISTAKES -> item.lastOutcome == "wrong"
                    IbqStatusFilter.MASTERED -> item.attemptsCount > 0 && item.lastOutcome == "right"
                }
            }
            .filter { item ->
                if (cleanQuery.isEmpty()) {
                    true
                } else {
                    val q = item.question
                    val badge = ClinicalSpotterCatalog.badgeLabelFor(ClinicalImageRef.parse(q.imageUrl))
                    q.questionText.lowercase(Locale.US).contains(cleanQuery) ||
                        q.subject.lowercase(Locale.US).contains(cleanQuery) ||
                        q.explanation.lowercase(Locale.US).contains(cleanQuery) ||
                        q.sourceNote.lowercase(Locale.US).contains(cleanQuery) ||
                        badge.lowercase(Locale.US).contains(cleanQuery) ||
                        categorize(q).label.lowercase(Locale.US).contains(cleanQuery)
                }
            }
            .toList()
    }

    fun buildSummary(allIbqItems: List<QuestionItem>): IbqHubSummary {
        val ibqs = allIbqItems.filter { it.question.isImageBased }.distinctBy { it.id }
        val attempted = ibqs.count { it.attemptsCount > 0 }
        val correct = ibqs.count { it.lastOutcome == "right" }
        val unseen = ibqs.count { it.attemptsCount == 0 }
        val mistakes = ibqs.count { it.lastOutcome == "wrong" }

        val stats = IbqCategory.entries.map { cat ->
            val catItems = if (cat == IbqCategory.ALL) {
                ibqs
            } else {
                ibqs.filter { categorize(it.question) == cat }
            }
            IbqCategoryStat(
                category = cat,
                totalCount = catItems.size,
                attemptedCount = catItems.count { it.attemptsCount > 0 },
                correctCount = catItems.count { it.lastOutcome == "right" }
            )
        }

        return IbqHubSummary(
            totalCount = ibqs.size,
            attemptedCount = attempted,
            correctCount = correct,
            unseenCount = unseen,
            mistakeCount = mistakes,
            categoryStats = stats
        )
    }

    /**
     * Prioritizes mistakes -> unseen -> previously mastered so IBQ drills maximize retention.
     */
    fun prioritizeForDrill(items: List<QuestionItem>): List<QuestionItem> {
        val mistakes = items.filter { it.lastOutcome == "wrong" }
        val unseen = items.filter { it.attemptsCount == 0 }
        val rest = items.filter { it.attemptsCount > 0 && it.lastOutcome != "wrong" }
        return (mistakes + unseen + rest).distinctBy { it.id }
    }

    /**
     * Seamlessly mixes Image-Based Questions (IBQs) into general practice sessions (e.g., Daily Goal Drill,
     * 25Q Mixed Drill, Smart Weak-Topic Drill) at an exam-realistic ratio (~15-20% IBQ) without duplicates.
     *
     * Rules:
     * - Preserves total count <= targetCount.
     * - Guarantees at least `minIbqCount` IBQs when `targetCount >= 5` and matching IBQs exist in `ibqPool`.
     * - Caps IBQ share at `maxIbqCount` so general practice remains balanced between text MCQs and visual IBQs.
     * - Interleaves IBQs evenly across the session so students don't see all visuals clumped at the start/end.
     */
    fun mixIbqsIntoGeneralPractice(
        generalPool: List<QuestionItem>,
        ibqPool: List<QuestionItem>,
        targetCount: Int,
        ibqSharePercent: Int = 18,
        minIbqCount: Int = 2,
        maxIbqCount: Int = 5
    ): List<QuestionItem> {
        if (targetCount <= 0) return emptyList()
        val distinctGeneral = generalPool.distinctBy { it.id }
        val distinctIbqs = ibqPool
            .filter { it.question.isImageBased }
            .distinctBy { it.id }

        if (distinctGeneral.isEmpty() && distinctIbqs.isEmpty()) return emptyList()
        if (distinctIbqs.isEmpty()) return distinctGeneral.take(targetCount)

        val nonIbqGeneral = distinctGeneral.filterNot { it.question.isImageBased }
        if (nonIbqGeneral.isEmpty()) return prioritizeForDrill(distinctIbqs).take(targetCount)

        val computedDesired = ((targetCount * ibqSharePercent) / 100.0).roundToInt()
        val effectiveMin = if (targetCount >= 5) minIbqCount else 1
        val desiredIbqs = computedDesired
            .coerceAtLeast(effectiveMin)
            .coerceAtMost(maxIbqCount)
            .coerceAtMost(distinctIbqs.size)
            .coerceAtMost(targetCount)

        // Prioritize unseen/mistake IBQs first
        val prioritizedIbqs = prioritizeForDrill(distinctIbqs).take(desiredIbqs)
        val pickedIbqIds = prioritizedIbqs.map { it.id }.toSet()

        val textNeeded = (targetCount - prioritizedIbqs.size).coerceAtLeast(0)
        val pickedText = nonIbqGeneral
            .filterNot { it.id in pickedIbqIds }
            .take(textNeeded)

        // If text pool is smaller than needed, backfill with remaining IBQs
        val backfill = if (pickedText.size + prioritizedIbqs.size < targetCount) {
            val usedIds = (pickedText.map { it.id } + prioritizedIbqs.map { it.id }).toSet()
            prioritizeForDrill(distinctIbqs)
                .filterNot { it.id in usedIds }
                .take(targetCount - pickedText.size - prioritizedIbqs.size)
        } else {
            emptyList()
        }

        val allSelectedIbqs = prioritizedIbqs + backfill
        if (allSelectedIbqs.isEmpty()) return pickedText.take(targetCount)
        if (pickedText.isEmpty()) return allSelectedIbqs.take(targetCount)

        // Interleave IBQs evenly through the text MCQs
        val interval = (pickedText.size / (allSelectedIbqs.size + 1)).coerceAtLeast(1)
        val result = ArrayList<QuestionItem>(pickedText.size + allSelectedIbqs.size)
        var ibqIdx = 0
        var sinceLastIbq = 0

        for (textItem in pickedText) {
            result.add(textItem)
            sinceLastIbq++
            if (sinceLastIbq >= interval && ibqIdx < allSelectedIbqs.size) {
                result.add(allSelectedIbqs[ibqIdx])
                ibqIdx++
                sinceLastIbq = 0
            }
        }
        while (ibqIdx < allSelectedIbqs.size) {
            result.add(allSelectedIbqs[ibqIdx])
            ibqIdx++
        }
        return result.distinctBy { it.id }.take(targetCount)
    }

    /**
     * Produces a crisp single-line visual title for IBQ gallery cards.
     */
    fun shortSpotterTitle(question: Question): String {
        val ref = ClinicalImageRef.parse(question.imageUrl)
        return when (ref) {
            is ClinicalImageRef.Diagram -> when (ref.key) {
                ClinicalSpotterCatalog.DIAGRAM_ECG_VTACH -> "Monomorphic Ventricular Tachycardia"
                ClinicalSpotterCatalog.DIAGRAM_ECG_VFIB -> "Coarse Ventricular Fibrillation"
                ClinicalSpotterCatalog.DIAGRAM_ECG_AFIB -> "Atrial Fibrillation (Irregularly Irregular)"
                ClinicalSpotterCatalog.DIAGRAM_ECG_STEMI -> "Acute Anterior STEMI (V2–V4)"
                ClinicalSpotterCatalog.DIAGRAM_ECG_AVBLOCK3 -> "Complete 3rd-Degree AV Block"
                ClinicalSpotterCatalog.DIAGRAM_ECG_HYPERKALEMIA -> "Hyperkalemia Peaked T-Waves"
                ClinicalSpotterCatalog.DIAGRAM_ECG_TORSADES -> "Torsades de Pointes (Polymorphic VT)"
                ClinicalSpotterCatalog.DIAGRAM_IV_CANNULA_18G -> "18G Green IV Cannula (ISO)"
                ClinicalSpotterCatalog.DIAGRAM_IV_CANNULA_20G -> "20G Pink IV Cannula (ISO)"
                ClinicalSpotterCatalog.DIAGRAM_IV_CANNULA_22G -> "22G Blue IV Cannula (ISO)"
                ClinicalSpotterCatalog.DIAGRAM_BMW_RED -> "BMW Red Bin (Recyclable Plastics)"
                ClinicalSpotterCatalog.DIAGRAM_BMW_YELLOW -> "BMW Yellow Bin (Anatomical/Soiled)"
                ClinicalSpotterCatalog.DIAGRAM_BMW_WHITE -> "BMW White Container (Metal Sharps)"
                ClinicalSpotterCatalog.DIAGRAM_VENTURI_MASK -> "Venturi High-Flow O₂ Mask"
                ClinicalSpotterCatalog.DIAGRAM_NRB_MASK -> "Non-Rebreather (NRB) O₂ Mask"
                ClinicalSpotterCatalog.DIAGRAM_CTG_LATE_DECEL -> "CTG · Late Decelerations"
                ClinicalSpotterCatalog.DIAGRAM_CTG_VARIABLE_DECEL -> "CTG · Variable Decelerations"
                ClinicalSpotterCatalog.DIAGRAM_PARTOGRAPH_ACTION -> "WHO Partograph · Alert/Action Line"
                ClinicalSpotterCatalog.DIAGRAM_CXR_TENSION_PNEUMO -> "CXR · Right Tension Pneumothorax"
                ClinicalSpotterCatalog.DIAGRAM_CXR_PLEURAL_EFFUSION -> "CXR · Pleural Effusion (Meniscus)"
                ClinicalSpotterCatalog.DIAGRAM_RULE_OF_NINES -> "Wallace Rule of Nines (36% TBSA)"
                else -> ClinicalSpotterCatalog.badgeLabelFor(ref)
            }
            is ClinicalImageRef.Drawable -> when (question.id) {
                "IBQ_007" -> "Macintosh Curved Laryngoscope"
                "IBQ_008" -> "Cuffed ETT & Murphy's Eye"
                "IBQ_009" -> "Cheatle Sterile Transfer Forceps"
                "IBQ_010" -> "Cusco's Bivalve Speculum"
                else -> ClinicalSpotterCatalog.badgeLabelFor(ref)
            }
            is ClinicalImageRef.RemoteUrl -> "${question.subject} Visual Case"
            ClinicalImageRef.None -> question.subject
        }
    }
}
