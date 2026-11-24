package com.example.data.ai

import android.util.Log
import com.example.data.model.Question
import com.example.data.notifications.CloudDoubt
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

private const val TAG = "ClinEdgeAiTutor"
private const val MODEL_NAME = "gemini-3.8-flash"
private const val AI_TIMEOUT_MS = 60_000L

sealed interface AiTutorUiState {
    data object Idle : AiTutorUiState
    data class Loading(val promptLabel: String = "Analyzing clinical concept...") : AiTutorUiState
    data class Streaming(val text: String, val promptLabel: String = "") : AiTutorUiState
    data class Success(val text: String, val promptLabel: String = "") : AiTutorUiState
    data class Error(val message: String) : AiTutorUiState
}

/**
 * Firebase AI Logic clinical tutoring & admin faculty draft service.
 * Strictly uses Firebase.ai with lazy model initialization and App Check — zero API keys.
 */
class ClinEdgeAiTutorService {

    private val tutorModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                temperature = 0.35f
                maxOutputTokens = 1024
            },
            systemInstruction = content {
                text(
                    "You are ClinEdge AI Clinical Tutor, an expert Indian Nursing Officer exam faculty " +
                        "specializing in AIIMS NORCET, RRB Nursing Superintendent, ESIC, PGIMER, JIPMER, DSSSB, and State CHO exams. " +
                        "Always base clinical reasoning on standard nursing textbooks (Brunner & Suddarth, Potter & Perry, " +
                        "Dutta OBG, Ghai Pediatrics, Park PSM, INC guidelines). " +
                        "Keep explanations structured, crisp, accurate, and high-yield for exam preparation. " +
                        "Never fabricate drug doses or guidelines; if something is exam-specific, clearly state the priority nursing action."
                )
            }
        )
    }

    private val adminDraftModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                temperature = 0.3f
                maxOutputTokens = 768
            },
            systemInstruction = content {
                text(
                    "You are drafting an official faculty reply for the Owner Admin of ClinEdge Nursing Prep " +
                        "to review and edit before sending to an Indian Nursing Officer aspirant. " +
                        "Write in a warm, authoritative, textbook-aligned tone (Brunner & Suddarth / Dutta / Park / INC). " +
                        "Keep the reply concise (4-8 sentences or short bullet points), directly answer the student's doubt, " +
                        "highlight the key exam takeaway, and do not include placeholder brackets like [Name]."
                )
            }
        )
    }

    fun explainMcqStream(
        question: Question,
        selectedOptionIndex: Int?,
        activeExamName: String,
        followUpPrompt: String? = null,
        previousExplanation: String? = null
    ): Flow<String> = flow {
        val optionLetters = listOf("A", "B", "C", "D", "E", "F")
        val optionsBlock = question.options.mapIndexed { idx, opt ->
            val letter = optionLetters.getOrElse(idx) { "${idx + 1}" }
            val marker = buildString {
                if (idx == question.correctIndex) append(" [CORRECT KEY]")
                if (selectedOptionIndex != null && idx == selectedOptionIndex) append(" [STUDENT PICKED]")
            }
            "$letter) $opt$marker"
        }.joinToString("\n")

        val prompt = if (followUpPrompt.isNullOrBlank()) {
            buildString {
                appendLine("Target Exam: $activeExamName")
                appendLine("Subject: ${question.subject} | Difficulty: ${question.difficulty.label}")
                appendLine("Question: ${question.questionText}")
                appendLine("Options:")
                appendLine(optionsBlock)
                if (question.explanation.isNotBlank()) {
                    appendLine("Existing Short Rationale: ${question.explanation}")
                }
                appendLine()
                appendLine("Provide a crisp, structured exam explanation with these exact 3 headings:")
                appendLine("1. Core Clinical Concept & Priority")
                appendLine("2. Option-by-Option Elimination")
                appendLine("3. High-Yield Bedside Mnemonic / Exam Trap")
            }
        } else {
            buildString {
                appendLine("Context Question ($activeExamName • ${question.subject}): ${question.questionText}")
                appendLine("Options:")
                appendLine(optionsBlock)
                if (!previousExplanation.isNullOrBlank()) {
                    appendLine("Previous Tutor Summary: ${previousExplanation.take(1200)}")
                }
                appendLine()
                appendLine("Student Follow-up Request: ${followUpPrompt.trim()}")
                appendLine("Answer clearly and concisely with direct nursing exam relevance.")
            }
        }

        val accumulated = StringBuilder()
        try {
            withTimeout(AI_TIMEOUT_MS) {
                tutorModel.generateContentStream(prompt).collect { chunk ->
                    val delta = chunk.text.orEmpty()
                    if (delta.isNotEmpty()) {
                        accumulated.append(delta)
                        emit(accumulated.toString())
                    }
                }
            }
            if (accumulated.isBlank()) {
                throw IllegalStateException("AI Tutor returned an empty response. Please try again.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI Tutor request timed out", e)
            throw IllegalStateException("AI Tutor request timed out after 60s. Check your connection and retry.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI Tutor request failed", e)
            throw e
        }
    }

    fun draftAdminDoubtReplyStream(
        doubt: CloudDoubt
    ): Flow<String> = flow {
        val prompt = buildString {
            appendLine("Student Name: ${doubt.studentName.ifBlank { "Aspirant" }}")
            appendLine("Exam Track: ${doubt.examId.ifBlank { "Nursing Officer" }} | Subject: ${doubt.subject}")
            if (doubt.questionPreview.isNotBlank()) {
                appendLine("Linked MCQ: ${doubt.questionPreview}")
            }
            appendLine("Student's Doubt: ${doubt.doubtText}")
            appendLine()
            appendLine("Draft a clear, accurate faculty response for the student:")
        }

        val accumulated = StringBuilder()
        try {
            withTimeout(AI_TIMEOUT_MS) {
                adminDraftModel.generateContentStream(prompt).collect { chunk ->
                    val delta = chunk.text.orEmpty()
                    if (delta.isNotEmpty()) {
                        accumulated.append(delta)
                        emit(accumulated.toString())
                    }
                }
            }
            if (accumulated.isBlank()) {
                throw IllegalStateException("AI Draft Helper returned an empty response. Please try again.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Admin AI doubt draft timed out", e)
            throw IllegalStateException("AI Draft request timed out after 60s. Please retry.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Admin AI doubt draft failed", e)
            throw e
        }
    }
}
