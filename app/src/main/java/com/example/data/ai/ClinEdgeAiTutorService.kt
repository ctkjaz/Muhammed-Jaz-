package com.example.data.ai

import android.util.Log
import com.example.data.model.Question
import com.example.data.notifications.CloudDoubt
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.appcheck.FirebaseAppCheck
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import java.util.Collections

private const val TAG = "ClinEdgeAiTutor"

// Ultra-fast Gemini 3.x Flash-Lite for snappy 1-3s MCQ tutoring & admin draft replies,
// with automatic fallback to Gemini 3.8 Flash if lite is temporarily unavailable.
private const val PRIMARY_MODEL_NAME = "gemini-3.5-flash-lite"
private const val FALLBACK_MODEL_NAME = "gemini-3.8-flash"

private const val APP_CHECK_PREFLIGHT_TIMEOUT_MS = 12_000L
private const val FIRST_TOKEN_WATCHDOG_MS = 14_000L
private const val STREAM_TOTAL_TIMEOUT_MS = 35_000L
private const val UNARY_FALLBACK_TIMEOUT_MS = 22_000L
private const val MAX_CACHE_ENTRIES = 40

sealed interface AiTutorUiState {
    data object Idle : AiTutorUiState
    data class Loading(val promptLabel: String = "Analyzing clinical concept...") : AiTutorUiState
    data class Streaming(val text: String, val promptLabel: String = "") : AiTutorUiState
    data class Success(val text: String, val promptLabel: String = "") : AiTutorUiState
    data class Error(val message: String) : AiTutorUiState
}

sealed interface AiStreamEvent {
    data class Status(val message: String) : AiStreamEvent
    data class Partial(val accumulatedText: String) : AiStreamEvent
    data class Completed(val finalText: String) : AiStreamEvent
}

/**
 * Firebase AI Logic clinical tutoring & admin faculty draft service.
 * Strictly uses Firebase.ai with lazy model initialization and App Check — zero API keys.
 *
 * Latency & reliability optimizations:
 * 1. Uses ultra-fast gemini-3.5-flash-lite as primary model (with gemini-3.8-flash fallback).
 * 2. Verifies/uses pre-warmed App Check token with an explicit 12s preflight timeout so
 *    reCAPTCHA network stalls fail fast with actionable diagnostics instead of hanging for 60s.
 * 3. Runs a 14s first-token watchdog on SSE streams; if a mobile carrier proxy buffers SSE,
 *    automatically falls back to a direct unary generateContent() call.
 * 4. Caches initial MCQ explanations in memory (LRU, 40 entries) for instant 0ms re-opens.
 */
class ClinEdgeAiTutorService {

    private val explanationCache: MutableMap<String, String> = Collections.synchronizedMap(
        object : LinkedHashMap<String, String>(MAX_CACHE_ENTRIES, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean =
                size > MAX_CACHE_ENTRIES
        }
    )

    private val tutorSystemInstruction by lazy {
        content {
            text(
                "You are ClinEdge AI Clinical Tutor, an Indian Nursing Officer exam faculty " +
                    "(AIIMS NORCET, RRB Nursing, ESIC, PGIMER, JIPMER, DSSSB, State CHO). " +
                    "Align with Brunner & Suddarth, Potter & Perry, Dutta OBG, Ghai Pediatrics, Park PSM, and INC guidelines. " +
                    "Be crisp, high-yield, and concise so students can read fast on mobile. Never fabricate doses."
            )
        }
    }

    private val adminSystemInstruction by lazy {
        content {
            text(
                "Draft an official faculty reply for the ClinEdge Nursing Prep Owner Admin to review before sending to a student. " +
                    "Warm, authoritative, textbook-aligned tone (4-7 concise sentences or short bullets). " +
                    "Answer the doubt directly with the key exam takeaway. No bracket placeholders."
            )
        }
    }

    private val primaryTutorModel: GenerativeModel by lazy {
        buildModel(PRIMARY_MODEL_NAME, maxTokens = 550, temp = 0.3f, system = tutorSystemInstruction)
    }

    private val fallbackTutorModel: GenerativeModel by lazy {
        buildModel(FALLBACK_MODEL_NAME, maxTokens = 550, temp = 0.3f, system = tutorSystemInstruction)
    }

    private val primaryAdminDraftModel: GenerativeModel by lazy {
        buildModel(PRIMARY_MODEL_NAME, maxTokens = 450, temp = 0.25f, system = adminSystemInstruction)
    }

    private val fallbackAdminDraftModel: GenerativeModel by lazy {
        buildModel(FALLBACK_MODEL_NAME, maxTokens = 450, temp = 0.25f, system = adminSystemInstruction)
    }

    private fun buildModel(
        modelName: String,
        maxTokens: Int,
        temp: Float,
        system: com.google.firebase.ai.type.Content
    ): GenerativeModel = Firebase.ai.generativeModel(
        modelName = modelName,
        generationConfig = generationConfig {
            temperature = temp
            maxOutputTokens = maxTokens
        },
        systemInstruction = system
    )

    fun explainMcqEvents(
        question: Question,
        selectedOptionIndex: Int?,
        activeExamName: String,
        followUpPrompt: String? = null,
        previousExplanation: String? = null
    ): Flow<AiStreamEvent> = flow {
        val isInitialRequest = followUpPrompt.isNullOrBlank()
        val cacheKey = "${question.id}|${selectedOptionIndex ?: -1}|$activeExamName"

        if (isInitialRequest) {
            val cached = explanationCache[cacheKey]
            if (!cached.isNullOrBlank()) {
                emit(AiStreamEvent.Completed(cached))
                return@flow
            }
        }

        val optionLetters = listOf("A", "B", "C", "D", "E", "F")
        val optionsBlock = question.options.mapIndexed { idx, opt ->
            val letter = optionLetters.getOrElse(idx) { "${idx + 1}" }
            val marker = buildString {
                if (idx == question.correctIndex) append(" [KEY]")
                if (selectedOptionIndex != null && idx == selectedOptionIndex) append(" [PICKED]")
            }
            "$letter) $opt$marker"
        }.joinToString("\n")

        val prompt = if (isInitialRequest) {
            buildString {
                appendLine("Exam: $activeExamName | Subject: ${question.subject}")
                appendLine("Q: ${question.questionText}")
                appendLine(optionsBlock)
                if (question.explanation.isNotBlank()) {
                    appendLine("Note: ${question.explanation.take(300)}")
                }
                appendLine()
                appendLine("Give a concise, mobile-scannable breakdown under these 3 short headings (max 180 words total):")
                appendLine("1. Core Concept & Priority")
                appendLine("2. Quick Option Elimination (1 short line per option)")
                appendLine("3. Bedside Mnemonic / Exam Trap")
            }
        } else {
            buildString {
                appendLine("MCQ ($activeExamName • ${question.subject}): ${question.questionText}")
                appendLine(optionsBlock)
                if (!previousExplanation.isNullOrBlank()) {
                    appendLine("Prior summary: ${previousExplanation.take(700)}")
                }
                appendLine()
                appendLine("Follow-up: ${followUpPrompt!!.trim()}")
                appendLine("Reply concisely in 3-5 crisp bullets with direct nursing exam relevance.")
            }
        }

        val finalText = executeFastStreamWithFallback(
            prompt = prompt,
            primaryModel = primaryTutorModel,
            fallbackModel = fallbackTutorModel,
            onStatus = { status -> emit(AiStreamEvent.Status(status)) },
            onPartial = { partial -> emit(AiStreamEvent.Partial(partial)) }
        )

        if (isInitialRequest && finalText.isNotBlank()) {
            explanationCache[cacheKey] = finalText
        }
        emit(AiStreamEvent.Completed(finalText))
    }.flowOn(Dispatchers.IO)

    fun draftAdminDoubtReplyEvents(
        doubt: CloudDoubt
    ): Flow<AiStreamEvent> = flow {
        val prompt = buildString {
            appendLine("Student: ${doubt.studentName.ifBlank { "Aspirant" }} | Exam: ${doubt.examId.ifBlank { "Nursing Officer" }} | Subject: ${doubt.subject}")
            if (doubt.questionPreview.isNotBlank()) {
                appendLine("Linked MCQ: ${doubt.questionPreview.take(300)}")
            }
            appendLine("Doubt: ${doubt.doubtText}")
            appendLine()
            appendLine("Draft a concise, accurate faculty reply:")
        }

        val finalText = executeFastStreamWithFallback(
            prompt = prompt,
            primaryModel = primaryAdminDraftModel,
            fallbackModel = fallbackAdminDraftModel,
            onStatus = { status -> emit(AiStreamEvent.Status(status)) },
            onPartial = { partial -> emit(AiStreamEvent.Partial(partial)) }
        )
        emit(AiStreamEvent.Completed(finalText))
    }.flowOn(Dispatchers.IO)

    /**
     * Ensures App Check token is available with an explicit 12s cap, then streams from the
     * fast primary model with a 14s first-token watchdog and direct unary fallback.
     */
    private suspend fun executeFastStreamWithFallback(
        prompt: String,
        primaryModel: GenerativeModel,
        fallbackModel: GenerativeModel,
        onStatus: suspend (String) -> Unit,
        onPartial: suspend (String) -> Unit
    ): String {
        // Step 1: Fast App Check preflight (0ms if pre-warmed; capped at 12s if cold)
        if (!isAppCheckTokenWarmedUp) {
            onStatus("Verifying secure session (first-time check)...")
        }
        try {
            withTimeout(APP_CHECK_PREFLIGHT_TIMEOUT_MS) {
                FirebaseAppCheck.getInstance().getAppCheckToken(false).await()
            }
            markAppCheckWarmedUp()
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "App Check attestation timed out after ${APP_CHECK_PREFLIGHT_TIMEOUT_MS}ms", e)
            throw IllegalStateException(
                "Security verification timed out on this network. Disable any VPN/ad-blocker or switch network and tap Retry.",
                e
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Log and continue — Firebase.ai will either succeed or return its exact backend reason
            Log.w(TAG, "App Check preflight returned warning: ${e.message}")
        }

        // Step 2: Stream from ultra-fast primary model with first-token watchdog
        onStatus("Generating clinical breakdown...")
        return try {
            streamWithFirstTokenWatchdog(
                model = primaryModel,
                prompt = prompt,
                onPartial = onPartial
            )
        } catch (e: CancellationException) {
            throw e
        } catch (primaryErr: FirstTokenStalledException) {
            // Mobile carrier/proxy stalled SSE — switch to direct unary call (no SSE buffering)
            Log.w(TAG, "Primary SSE stalled before first token; trying direct unary request", primaryErr)
            onStatus("Switching to fast direct response...")
            runUnaryWithFallback(prompt, primaryModel, fallbackModel)
        } catch (primaryErr: Exception) {
            val msg = primaryErr.message.orEmpty()
            val shouldTryFallbackModel = msg.contains("not found", ignoreCase = true) ||
                msg.contains("not supported", ignoreCase = true) ||
                msg.contains("404", ignoreCase = true) ||
                msg.contains("overloaded", ignoreCase = true) ||
                msg.contains("503", ignoreCase = true) ||
                msg.contains("unavailable", ignoreCase = true)
            if (!shouldTryFallbackModel) {
                Log.e(TAG, "Primary AI request failed", primaryErr)
                throw primaryErr
            }
            Log.w(TAG, "Primary model unavailable ($msg); falling back to $FALLBACK_MODEL_NAME")
            onStatus("Connecting to backup AI model...")
            runUnaryWithFallback(prompt, fallbackModel, null)
        }
    }

    private suspend fun streamWithFirstTokenWatchdog(
        model: GenerativeModel,
        prompt: String,
        onPartial: suspend (String) -> Unit
    ): String = coroutineScope {
        val accumulated = StringBuilder()
        val chunkChannel = Channel<String>(capacity = Channel.UNLIMITED)
        var streamError: Throwable? = null

        val producer = launch {
            try {
                withTimeout(STREAM_TOTAL_TIMEOUT_MS) {
                    model.generateContentStream(prompt).collect { chunk ->
                        val delta = chunk.text.orEmpty()
                        if (delta.isNotEmpty()) {
                            chunkChannel.trySend(delta)
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                streamError = e
            } finally {
                chunkChannel.close()
            }
        }

        // Wait up to FIRST_TOKEN_WATCHDOG_MS for the very first visible chunk
        val firstDelta = try {
            withTimeout(FIRST_TOKEN_WATCHDOG_MS) {
                chunkChannel.receiveCatching().getOrNull()
            }
        } catch (e: TimeoutCancellationException) {
            producer.cancel()
            throw FirstTokenStalledException("No tokens received within ${FIRST_TOKEN_WATCHDOG_MS / 1000}s")
        }

        if (firstDelta == null) {
            producer.join()
            streamError?.let { throw it }
            throw IllegalStateException("AI Tutor returned an empty response. Please retry.")
        }

        accumulated.append(firstDelta)
        onPartial(accumulated.toString())

        // Drain all remaining streamed chunks
        for (delta in chunkChannel) {
            accumulated.append(delta)
            onPartial(accumulated.toString())
        }
        producer.join()

        if (accumulated.isBlank()) {
            streamError?.let { throw it }
            throw IllegalStateException("AI Tutor returned an empty response. Please retry.")
        }
        accumulated.toString()
    }

    private suspend fun runUnaryWithFallback(
        prompt: String,
        firstModel: GenerativeModel,
        secondModel: GenerativeModel?
    ): String {
        try {
            val response = withTimeout(UNARY_FALLBACK_TIMEOUT_MS) {
                firstModel.generateContent(prompt)
            }
            val text = response.text?.trim().orEmpty()
            if (text.isNotBlank()) return text
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (secondModel == null) {
                throw mapFriendlyError(e)
            }
            Log.w(TAG, "Direct unary request failed on primary; trying backup model: ${e.message}")
        }

        if (secondModel != null) {
            try {
                val response = withTimeout(UNARY_FALLBACK_TIMEOUT_MS) {
                    secondModel.generateContent(prompt)
                }
                val text = response.text?.trim().orEmpty()
                if (text.isNotBlank()) return text
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                throw mapFriendlyError(e)
            }
        }
        throw IllegalStateException("AI Tutor returned an empty response. Please tap Retry.")
    }

    private fun mapFriendlyError(e: Exception): Exception {
        if (e is TimeoutCancellationException) {
            return IllegalStateException(
                "AI response timed out on your connection. Please tap Retry.",
                e
            )
        }
        val raw = e.message.orEmpty()
        return when {
            raw.contains("App Check", ignoreCase = true) || raw.contains("403") ->
                IllegalStateException(
                    "App verification failed (App Check). Ensure Google Play Services is updated and disable VPN/ad-blockers, then tap Retry.",
                    e
                )
            raw.contains("Unable to resolve host", ignoreCase = true) ||
                raw.contains("Failed to connect", ignoreCase = true) ->
                IllegalStateException(
                    "No stable internet connection. Check mobile data/Wi-Fi and tap Retry.",
                    e
                )
            else -> e
        }
    }

    private class FirstTokenStalledException(message: String) : Exception(message)
}
