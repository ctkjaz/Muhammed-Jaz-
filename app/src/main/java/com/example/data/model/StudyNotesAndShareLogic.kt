package com.example.data.model

import java.util.Locale

/**
 * ClinEdge v2.9.0 — Per-Question Personal Margin Notes & Shareable Score Cards.
 *
 * Pure-Kotlin serialization & formatting (zero Android framework stubs) so both
 * JVM unit tests and Android runtime share identical deterministic behavior.
 */
data class QuestionNote(
    val questionId: String,
    val text: String,
    val updatedAtMs: Long = System.currentTimeMillis()
)

object QuestionNotesLogic {
    const val MAX_NOTE_LENGTH = 500

    fun sanitizeNote(raw: String): String {
        return raw
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
            .take(MAX_NOTE_LENGTH)
    }

    fun upsertNote(
        existing: Map<String, QuestionNote>,
        questionId: String,
        rawText: String,
        nowMs: Long = System.currentTimeMillis()
    ): Map<String, QuestionNote> {
        val cleanId = questionId.trim()
        if (cleanId.isBlank()) return existing
        val cleanText = sanitizeNote(rawText)
        val updated = LinkedHashMap(existing)
        if (cleanText.isBlank()) {
            updated.remove(cleanId)
        } else {
            updated[cleanId] = QuestionNote(
                questionId = cleanId,
                text = cleanText,
                updatedAtMs = nowMs
            )
        }
        return updated
    }

    fun removeNote(
        existing: Map<String, QuestionNote>,
        questionId: String
    ): Map<String, QuestionNote> {
        if (questionId !in existing) return existing
        val updated = LinkedHashMap(existing)
        updated.remove(questionId)
        return updated
    }

    fun searchNotedQuestions(
        questions: List<QuestionItem>,
        notes: Map<String, QuestionNote>,
        query: String = ""
    ): List<Pair<QuestionItem, QuestionNote>> {
        if (notes.isEmpty()) return emptyList()
        val q = query.trim().lowercase(Locale.US)
        val byId = questions.associateBy { it.id }
        return notes.values
            .sortedByDescending { it.updatedAtMs }
            .mapNotNull { note ->
                val item = byId[note.questionId] ?: return@mapNotNull null
                val matches = q.isBlank() ||
                    note.text.lowercase(Locale.US).contains(q) ||
                    item.question.questionText.lowercase(Locale.US).contains(q) ||
                    item.question.subject.lowercase(Locale.US).contains(q)
                if (matches) item to note else null
            }
    }

    /**
     * Compact, dependency-free, line-oriented escaped encoding:
     * `<escapedId>\t<updatedAtMs>\t<escapedText>\n`
     * Safe for quotes, newlines, tabs, Unicode, and Devanagari/medical symbols.
     */
    fun serializeMap(notes: Map<String, QuestionNote>): String {
        if (notes.isEmpty()) return ""
        val sb = StringBuilder()
        notes.values.forEach { note ->
            val cleanText = sanitizeNote(note.text)
            if (note.questionId.isNotBlank() && cleanText.isNotBlank()) {
                sb.append(escapeField(note.questionId.trim()))
                sb.append('\t')
                sb.append(note.updatedAtMs)
                sb.append('\t')
                sb.append(escapeField(cleanText))
                sb.append('\n')
            }
        }
        return sb.toString()
    }

    fun deserializeMap(raw: String?): Map<String, QuestionNote> {
        if (raw.isNullOrBlank()) return emptyMap()
        val result = LinkedHashMap<String, QuestionNote>()
        raw.lineSequence().forEach { line ->
            if (line.isBlank()) return@forEach
            val firstTab = line.indexOf('\t')
            if (firstTab <= 0) return@forEach
            val secondTab = line.indexOf('\t', firstTab + 1)
            if (secondTab <= firstTab) return@forEach
            val id = unescapeField(line.substring(0, firstTab)).trim()
            val ts = line.substring(firstTab + 1, secondTab).toLongOrNull() ?: 0L
            val text = sanitizeNote(unescapeField(line.substring(secondTab + 1)))
            if (id.isNotBlank() && text.isNotBlank()) {
                result[id] = QuestionNote(
                    questionId = id,
                    text = text,
                    updatedAtMs = ts
                )
            }
        }
        return result
    }

    private fun escapeField(value: String): String {
        val sb = StringBuilder(value.length + 8)
        for (ch in value) {
            when (ch) {
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\t' -> sb.append("\\t")
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    private fun unescapeField(value: String): String {
        val sb = StringBuilder(value.length)
        var i = 0
        while (i < value.length) {
            val ch = value[i]
            if (ch == '\\' && i + 1 < value.length) {
                when (value[i + 1]) {
                    '\\' -> { sb.append('\\'); i += 2 }
                    'n' -> { sb.append('\n'); i += 2 }
                    't' -> { sb.append('\t'); i += 2 }
                    else -> { sb.append(ch); i++ }
                }
            } else {
                sb.append(ch)
                i++
            }
        }
        return sb.toString()
    }
}

object ResultShareFormatter {

    fun formatCbtResultShare(
        title: String,
        netScore: Double,
        totalQuestions: Int,
        right: Int,
        wrong: Int,
        skipped: Int,
        accuracyPercent: Int,
        negativeMarks: Double,
        topSubjectLine: String? = null
    ): String {
        val scoreStr = String.format(Locale.US, "%.2f", netScore)
        val negStr = String.format(Locale.US, "%.2f", negativeMarks)
        return buildString {
            appendLine("ClinEdge CBT Score Card")
            appendLine("Test: ${title.trim().ifBlank { "Nursing CBT Mock" }}")
            appendLine("Net Score: $scoreStr / $totalQuestions (-$negStr marking)")
            appendLine("Accuracy: ${accuracyPercent.coerceIn(0, 100)}% | Correct: $right | Wrong: $wrong | Skipped: $skipped")
            if (!topSubjectLine.isNullOrBlank()) {
                appendLine("Strongest Subject: ${topSubjectLine.trim()}")
            }
            append("Prepared on ClinEdge — Indian Nursing Officer Exam Prep")
        }
    }

    fun formatReadinessShare(
        examName: String,
        summary: ExamReadinessSummary,
        streakDays: Int
    ): String {
        return buildString {
            appendLine("ClinEdge Exam Readiness Report")
            appendLine("Target Exam: ${examName.trim().ifBlank { "RRB Nursing" }}")
            appendLine("Readiness Index: ${summary.overallScore}% (${summary.band.label})")
            appendLine("Projected Band: ${summary.estimatedRankTier}")
            if (streakDays > 0) {
                appendLine("Active Study Streak: $streakDays day${if (streakDays == 1) "" else "s"}")
            }
            summary.pillars.forEach { p ->
                appendLine("- ${p.label}: ${p.scorePercent}%")
            }
            append("Next Focus: ${summary.nextBestAction}")
        }
    }
}
