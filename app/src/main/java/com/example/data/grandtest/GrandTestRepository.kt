package com.example.data.grandtest

import android.content.Context
import com.example.R
import com.example.data.cloud.OperationType
import com.example.data.cloud.handleFirestoreError
import com.example.data.subscription.EntitlementEvaluator
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class GrandTestRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUid(expected: String? = null): String {
        val uid = auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Please sign in with Google first.")
        if (expected != null) {
            check(uid == expected) { "Signed-in account changed." }
        }
        return uid
    }

    private fun requireAdminEmail(): String {
        val email = auth.currentUser?.email.orEmpty().trim()
        check(EntitlementEvaluator.isOwnerAdminEmail(email)) {
            "Admin access is restricted to the app owner."
        }
        return email
    }

    private fun testsCollection() = db.collection("grandTests")
    private fun testDoc(testId: String) = testsCollection().document(testId)
    private fun entriesCollection(testId: String) = testDoc(testId).collection("entries")

    /**
     * Real-time observer for students: filters strictly by status in ['live', 'closed']
     * to satisfy Firestore rules-to-query parity (drafts remain admin-only).
     */
    fun observePublicGrandTests(): Flow<List<CloudGrandTest>> = flow {
        requireUid()
        val path = "grandTests"
        emitAll(
            testsCollection()
                .whereIn("status", listOf(GrandTestStatus.LIVE.dbValue, GrandTestStatus.CLOSED.dbValue))
                .limit(30)
                .snapshots()
                .map { snaps ->
                    snaps.toObjects(
                        CloudGrandTest::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt?.toDate()?.time ?: it.startsAtMs }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    if (auth.currentUser != null) {
                        handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
                    }
                    emit(emptyList())
                }
        )
    }

    suspend fun fetchLeaderboardEntries(testId: String): Result<List<CloudGrandTestEntry>> {
        val path = "grandTests/$testId/entries"
        return try {
            requireUid()
            val snaps = entriesCollection(testId)
                .limit(500)
                .get()
                .await()
            val entries = snaps.toObjects(
                CloudGrandTestEntry::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(entries)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun submitGrandTestEntry(
        testId: String,
        userId: String,
        studentName: String,
        examId: String,
        netScore: Double,
        correctCount: Int,
        wrongCount: Int,
        skippedCount: Int,
        totalQuestions: Int,
        accuracyPercent: Int,
        timeTakenSeconds: Int
    ): Result<Unit> {
        val path = "grandTests/$testId/entries/$userId"
        return try {
            val uid = requireUid(userId)
            val safeTotal = totalQuestions.coerceIn(1, 200)
            val safeCorrect = correctCount.coerceIn(0, safeTotal)
            val safeWrong = wrongCount.coerceIn(0, safeTotal - safeCorrect)
            val safeSkipped = (safeTotal - safeCorrect - safeWrong).coerceAtLeast(0)
            val safeAccuracy = accuracyPercent.coerceIn(0, 100)
            val safeNetScore = netScore.coerceIn(-200.0, safeTotal.toDouble())
            val safeTime = timeTakenSeconds.coerceIn(0, 20_000)

            val payload = mapOf<String, Any>(
                "testId" to testId.trim(),
                "userId" to uid,
                "studentName" to studentName.trim().ifBlank { "Nursing Aspirant" }.take(60),
                "examId" to examId.trim().ifBlank { "aiims_norcet" }.take(64),
                "netScore" to safeNetScore,
                "correctCount" to safeCorrect,
                "wrongCount" to safeWrong,
                "skippedCount" to safeSkipped,
                "totalQuestions" to safeTotal,
                "accuracyPercent" to safeAccuracy,
                "timeTakenSeconds" to safeTime,
                "submittedAt" to FieldValue.serverTimestamp()
            )
            entriesCollection(testId).document(uid).set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    // ------------------------------------------------------------------------
    // OWNER ADMIN METHODS
    // ------------------------------------------------------------------------
    suspend fun adminFetchAllGrandTests(): Result<List<CloudGrandTest>> {
        return try {
            requireAdminEmail()
            val snaps = testsCollection()
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .await()
            val tests = snaps.toObjects(
                CloudGrandTest::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(tests)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "grandTests")
            Result.failure(e)
        }
    }

    suspend fun adminCreateGrandTest(
        title: String,
        examId: String,
        weekLabel: String,
        questionCount: Int,
        durationMinutes: Int,
        negativeMarks: Double,
        status: GrandTestStatus
    ): Result<CloudGrandTest> {
        val testId = "gt_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
        val path = "grandTests/$testId"
        return try {
            val adminEmail = requireAdminEmail()
            val cleanTitle = title.trim().take(120)
            require(cleanTitle.length >= 3) { "Grand Test title must be at least 3 characters." }
            val cleanWeek = weekLabel.trim().ifBlank { "Weekly Grand Test" }.take(40)
            val safeCount = questionCount.coerceIn(10, 200)
            val safeDuration = durationMinutes.coerceIn(5, 240)
            val safeNeg = negativeMarks.coerceIn(0.0, 1.0)
            val nowMs = System.currentTimeMillis()
            val endsAtMs = nowMs + 7L * 86_400_000L

            val payload = mapOf<String, Any>(
                "testId" to testId,
                "title" to cleanTitle,
                "examId" to examId.trim().ifBlank { "aiims_norcet" }.take(64),
                "weekLabel" to cleanWeek,
                "questionCount" to safeCount,
                "durationMinutes" to safeDuration,
                "negativeMarks" to safeNeg,
                "status" to status.dbValue,
                "startsAtMs" to nowMs,
                "endsAtMs" to endsAtMs,
                "createdByEmail" to adminEmail,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            testDoc(testId).set(payload).await()
            Result.success(
                CloudGrandTest(
                    id = testId,
                    testId = testId,
                    title = cleanTitle,
                    examId = examId.trim().ifBlank { "aiims_norcet" },
                    weekLabel = cleanWeek,
                    questionCount = safeCount.toLong(),
                    durationMinutes = safeDuration.toLong(),
                    negativeMarks = safeNeg,
                    status = status.dbValue,
                    startsAtMs = nowMs,
                    endsAtMs = endsAtMs,
                    createdByEmail = adminEmail
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun adminUpdateGrandTestStatus(testId: String, status: GrandTestStatus): Result<Unit> {
        val path = "grandTests/$testId"
        return try {
            requireAdminEmail()
            testDoc(testId).update(
                mapOf(
                    "status" to status.dbValue,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun adminDeleteGrandTest(testId: String): Result<Unit> {
        val path = "grandTests/$testId"
        return try {
            requireAdminEmail()
            // Clean up any leaderboard entries first so no orphans remain
            val existingEntries = entriesCollection(testId).limit(500).get().await()
            for (doc in existingEntries.documents) {
                doc.reference.delete().await()
            }
            testDoc(testId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}
