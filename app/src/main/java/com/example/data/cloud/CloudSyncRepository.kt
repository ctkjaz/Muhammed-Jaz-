package com.example.data.cloud

import android.content.Context
import com.example.R
import com.example.data.local.MockHistoryEntity
import com.example.data.local.QuestionProgressEntity
import com.example.data.local.SettingsPreferences
import com.example.data.model.AccountSessionLogic
import com.example.data.model.GamificationLogic
import com.example.data.model.GamificationState
import com.example.data.model.PlatformCatalogDefaults
import com.example.data.model.Question
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class CloudSyncRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(expectedUserId: String? = null): String {
        val uid = auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        if (expectedUserId != null) {
            check(uid == expectedUserId) { "Signed-in user changed; refusing cross-user Firestore operation." }
        }
        return uid
    }

    private fun userDoc(uid: String) = db.collection("users").document(uid)
    private fun progressCol(uid: String) = userDoc(uid).collection("progress")
    private fun mockHistoryCol(uid: String) = userDoc(uid).collection("mockHistory")
    private fun customQuestionsCol(uid: String) = userDoc(uid).collection("customQuestions")

    // ------------------------------------------------------------------------
    // REALTIME SNAPSHOT FLOWS (Auth-gated, ServerTimestampBehavior.ESTIMATE)
    // ------------------------------------------------------------------------
    fun observeProfile(userId: String): Flow<CloudUserProfile?> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid"
        emitAll(
            userDoc(uid)
                .snapshots()
                .map { snap ->
                    if (!snap.exists()) null
                    else snap.toObject(
                        CloudUserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    fun observeProgress(userId: String): Flow<List<CloudQuestionProgress>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/progress"
        emitAll(
            progressCol(uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        CloudQuestionProgress::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeMockHistory(userId: String): Flow<List<CloudMockHistory>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/mockHistory"
        emitAll(
            mockHistoryCol(uid)
                .orderBy("completedAtMs", Query.Direction.DESCENDING)
                .limit(25)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        CloudMockHistory::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeCustomQuestions(userId: String): Flow<List<CloudCustomQuestion>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/customQuestions"
        emitAll(
            customQuestionsCol(uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        CloudCustomQuestion::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    // ------------------------------------------------------------------------
    // ONE-SHOT FULL SNAPSHOT (For initial merge & manual Sync Now)
    // ------------------------------------------------------------------------
    suspend fun fetchAccountSnapshot(userId: String): Result<CloudAccountSnapshot> {
        var currentPath = "users/$userId"
        return try {
            val uid = requireUserId(userId)
            currentPath = "users/$uid"
            val profileSnap = userDoc(uid).get().await()
            val profile = if (profileSnap.exists()) {
                profileSnap.toObject(
                    CloudUserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            } else {
                null
            }

            currentPath = "users/$uid/progress"
            val progressList = progressCol(uid).get().await()
                .toObjects(CloudQuestionProgress::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)

            currentPath = "users/$uid/mockHistory"
            val mockList = mockHistoryCol(uid)
                .orderBy("completedAtMs", Query.Direction.DESCENDING)
                .limit(25)
                .get()
                .await()
                .toObjects(CloudMockHistory::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)

            currentPath = "users/$uid/customQuestions"
            val customList = customQuestionsCol(uid).get().await()
                .toObjects(CloudCustomQuestion::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)

            Result.success(
                CloudAccountSnapshot(
                    profile = profile,
                    progress = progressList,
                    mockHistory = mockList,
                    customQuestions = customList
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, currentPath)
            Result.failure(e)
        }
    }

    /**
     * Reads /users/{uid} directly from the Firestore server (bypassing stale local cache)
     * so single-device session validation detects logins from another phone immediately on resume/launch.
     */
    suspend fun fetchProfileFromServer(userId: String): Result<CloudUserProfile?> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId(userId)
            val snap = userDoc(uid).get(Source.SERVER).await()
            val profile = if (snap.exists()) {
                snap.toObject(
                    CloudUserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            } else {
                null
            }
            Result.success(profile)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docPath)
            Result.failure(e)
        }
    }

    // ------------------------------------------------------------------------
    // CRASH-SAFE MUTATIONS (Validated against firestore.rules, Result<Unit>)
    // ------------------------------------------------------------------------
    suspend fun claimActiveSession(
        userId: String,
        sessionId: String,
        deviceName: String,
        displayName: String,
        role: String,
        targetYear: String,
        focusSubject: String,
        phoneNumber: String,
        activeExamId: String,
        themePreference: String,
        negativeMarking: Boolean,
        useTimeLimit: Boolean,
        secondsPerQuestion: Int,
        keepScreenOn: Boolean,
        onboardingCompleted: Boolean,
        dailyGoalQuestions: Int,
        currentStreakDays: Int,
        longestStreakDays: Int,
        lastGoalMetDate: String,
        todayStudyDate: String,
        todayAnsweredCount: Int,
        email: String = "",
        lastDeviceHash: String = "",
        gamificationState: GamificationState? = null
    ): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId(userId)
            val safeSessionId = sessionId.trim().take(AccountSessionLogic.MAX_SESSION_ID_LENGTH)
            val safeDevice = deviceName.trim().ifBlank { "Android Phone" }.take(AccountSessionLogic.MAX_DEVICE_NAME_LENGTH)
            val docRef = userDoc(uid)
            val exists = docRef.get().await().exists()
            if (exists) {
                val claimPayload = mapOf<String, Any>(
                    "activeSessionId" to safeSessionId,
                    "activeDeviceName" to safeDevice,
                    "sessionUpdatedAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(claimPayload).await()
                Result.success(Unit)
            } else {
                upsertProfile(
                    userId = uid,
                    displayName = displayName,
                    role = role,
                    targetYear = targetYear,
                    focusSubject = focusSubject,
                    phoneNumber = phoneNumber,
                    activeExamId = activeExamId,
                    themePreference = themePreference,
                    negativeMarking = negativeMarking,
                    useTimeLimit = useTimeLimit,
                    secondsPerQuestion = secondsPerQuestion,
                    keepScreenOn = keepScreenOn,
                    onboardingCompleted = onboardingCompleted,
                    dailyGoalQuestions = dailyGoalQuestions,
                    currentStreakDays = currentStreakDays,
                    longestStreakDays = longestStreakDays,
                    lastGoalMetDate = lastGoalMetDate,
                    todayStudyDate = todayStudyDate,
                    todayAnsweredCount = todayAnsweredCount,
                    initialActiveSessionId = safeSessionId,
                    initialActiveDeviceName = safeDevice,
                    email = email,
                    lastDeviceHash = lastDeviceHash,
                    gamificationState = gamificationState
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun upsertProfile(
        userId: String,
        displayName: String,
        role: String,
        targetYear: String,
        focusSubject: String,
        phoneNumber: String,
        activeExamId: String,
        themePreference: String,
        negativeMarking: Boolean,
        useTimeLimit: Boolean,
        secondsPerQuestion: Int,
        keepScreenOn: Boolean,
        onboardingCompleted: Boolean,
        dailyGoalQuestions: Int,
        currentStreakDays: Int,
        longestStreakDays: Int,
        lastGoalMetDate: String,
        todayStudyDate: String,
        todayAnsweredCount: Int,
        initialActiveSessionId: String = "",
        initialActiveDeviceName: String = "",
        email: String = "",
        lastDeviceHash: String = "",
        gamificationState: GamificationState? = null
    ): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId(userId)
            val safeEmail = email.ifBlank { auth.currentUser?.email.orEmpty() }.trim().take(120)
            val safeDeviceHash = lastDeviceHash.trim().take(64)
            val safeName = displayName.take(SettingsPreferences.MAX_PROFILE_NAME_LENGTH)
            val safeRole = role.take(SettingsPreferences.MAX_PROFILE_ROLE_LENGTH)
            val safeYear = targetYear.trim().take(SettingsPreferences.MAX_TARGET_YEAR_LENGTH)
            val safeFocus = focusSubject.trim().take(32)
            val safePhone = AccountSessionLogic.sanitizePhoneNumber(phoneNumber)
            val safeExam = activeExamId.trim().ifBlank { PlatformCatalogDefaults.EXAM_RRB_NURSING }.take(64)
            val safeTheme = CloudMergeLogic.parseThemeSafe(themePreference).name
            val safeSec = secondsPerQuestion.coerceIn(
                SettingsPreferences.MIN_SECONDS_PER_QUESTION,
                SettingsPreferences.MAX_SECONDS_PER_QUESTION
            )
            val safeDailyGoal = dailyGoalQuestions.coerceIn(
                com.example.data.model.StudyGoalLogic.MIN_DAILY_GOAL,
                com.example.data.model.StudyGoalLogic.MAX_DAILY_GOAL
            )
            val safeCurrentStreak = currentStreakDays.coerceIn(0, com.example.data.model.StudyGoalLogic.MAX_COUNT_BOUND)
            val safeLongestStreak = longestStreakDays.coerceIn(0, com.example.data.model.StudyGoalLogic.MAX_COUNT_BOUND)
            val safeLastMetDate = com.example.data.model.StudyGoalLogic.sanitizeDateKey(lastGoalMetDate)
            val safeTodayDate = com.example.data.model.StudyGoalLogic.sanitizeDateKey(todayStudyDate)
            val safeTodayCount = todayAnsweredCount.coerceIn(0, com.example.data.model.StudyGoalLogic.MAX_COUNT_BOUND)

            val gamificationPayload: Map<String, Any> = gamificationState?.let { g ->
                mapOf(
                    "totalXp" to g.totalXp.coerceIn(0, GamificationLogic.MAX_TOTAL_XP),
                    "weeklyXp" to g.weeklyXp.coerceIn(0, GamificationLogic.MAX_WEEKLY_XP),
                    "weeklyKey" to GamificationLogic.sanitizeWeekKey(g.weeklyKey),
                    "weeklyQuestions" to g.weeklyQuestions.coerceIn(0, GamificationLogic.MAX_WEEKLY_QUESTIONS),
                    "weeklyCorrect" to g.weeklyCorrect.coerceIn(0, GamificationLogic.MAX_WEEKLY_QUESTIONS),
                    "weeklySessions" to g.weeklySessions.coerceIn(0, GamificationLogic.MAX_WEEKLY_SESSIONS),
                    "weeklyClaimedIds" to g.weeklyClaimedIds
                        .filter { it in GamificationLogic.VALID_CHALLENGE_IDS }
                        .distinct()
                        .take(GamificationLogic.MAX_WEEKLY_CLAIMED_COUNT),
                    "unlockedBadgeIds" to g.unlockedBadgeIds
                        .filter { it in GamificationLogic.VALID_BADGE_IDS }
                        .distinct()
                        .take(GamificationLogic.MAX_BADGES_COUNT),
                    "streakShields" to g.streakShields.coerceIn(0, GamificationLogic.MAX_STREAK_SHIELDS),
                    "lastShieldEarnedStreak" to g.lastShieldEarnedStreak.coerceIn(0, com.example.data.model.StudyGoalLogic.MAX_COUNT_BOUND),
                    "lastStreakRecoveryDate" to com.example.data.model.StudyGoalLogic.sanitizeDateKey(g.lastStreakRecoveryDate),
                    "qotdAnsweredCount" to g.qotdAnsweredCount.coerceIn(0, GamificationLogic.MAX_COUNTER_BOUND),
                    "completedSessionsCount" to g.completedSessionsCount.coerceIn(0, GamificationLogic.MAX_COUNTER_BOUND),
                    "focusSprintsCount" to g.focusSprintsCount.coerceIn(0, GamificationLogic.MAX_COUNTER_BOUND),
                    "srsMasteredCount" to g.srsMasteredCount.coerceIn(0, GamificationLogic.MAX_COUNTER_BOUND),
                    "bestSessionAccuracy" to g.bestSessionAccuracy.coerceIn(0, 100)
                )
            } ?: emptyMap()

            val docRef = userDoc(uid)
            val exists = docRef.get().await().exists()
            if (exists) {
                val updatePayload = mutableMapOf<String, Any>(
                    "displayName" to safeName,
                    "role" to safeRole,
                    "targetYear" to safeYear,
                    "focusSubject" to safeFocus,
                    "phoneNumber" to safePhone,
                    "activeExamId" to safeExam,
                    "themePreference" to safeTheme,
                    "negativeMarking" to negativeMarking,
                    "useTimeLimit" to useTimeLimit,
                    "secondsPerQuestion" to safeSec,
                    "keepScreenOn" to keepScreenOn,
                    "onboardingCompleted" to onboardingCompleted,
                    "dailyGoalQuestions" to safeDailyGoal,
                    "currentStreakDays" to safeCurrentStreak,
                    "longestStreakDays" to safeLongestStreak,
                    "lastGoalMetDate" to safeLastMetDate,
                    "todayStudyDate" to safeTodayDate,
                    "todayAnsweredCount" to safeTodayCount,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                updatePayload.putAll(gamificationPayload)
                if (safeEmail.isNotBlank()) updatePayload["email"] = safeEmail
                if (safeDeviceHash.isNotBlank()) updatePayload["lastDeviceHash"] = safeDeviceHash
                docRef.update(updatePayload).await()
            } else {
                val createPayload = mutableMapOf<String, Any>(
                    "userId" to uid,
                    "displayName" to safeName,
                    "role" to safeRole,
                    "targetYear" to safeYear,
                    "focusSubject" to safeFocus,
                    "phoneNumber" to safePhone,
                    "activeExamId" to safeExam,
                    "themePreference" to safeTheme,
                    "negativeMarking" to negativeMarking,
                    "useTimeLimit" to useTimeLimit,
                    "secondsPerQuestion" to safeSec,
                    "keepScreenOn" to keepScreenOn,
                    "onboardingCompleted" to onboardingCompleted,
                    "dailyGoalQuestions" to safeDailyGoal,
                    "currentStreakDays" to safeCurrentStreak,
                    "longestStreakDays" to safeLongestStreak,
                    "lastGoalMetDate" to safeLastMetDate,
                    "todayStudyDate" to safeTodayDate,
                    "todayAnsweredCount" to safeTodayCount,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                createPayload.putAll(gamificationPayload)
                if (safeEmail.isNotBlank()) createPayload["email"] = safeEmail
                if (safeDeviceHash.isNotBlank()) createPayload["lastDeviceHash"] = safeDeviceHash
                val cleanSessionId = initialActiveSessionId.trim().take(AccountSessionLogic.MAX_SESSION_ID_LENGTH)
                if (cleanSessionId.isNotBlank()) {
                    createPayload["activeSessionId"] = cleanSessionId
                    createPayload["activeDeviceName"] = initialActiveDeviceName.trim()
                        .ifBlank { "Android Phone" }
                        .take(AccountSessionLogic.MAX_DEVICE_NAME_LENGTH)
                    createPayload["sessionUpdatedAt"] = FieldValue.serverTimestamp()
                }
                docRef.set(createPayload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun upsertProgress(
        userId: String,
        entity: QuestionProgressEntity
    ): Result<Unit> {
        val safeQuestionId = CloudMergeLogic.sanitizeFirestoreId(entity.questionId, "q")
        val docPath = "users/$userId/progress/$safeQuestionId"
        return try {
            val uid = requireUserId(userId)
            val docRef = progressCol(uid).document(safeQuestionId)
            val exists = docRef.get().await().exists()
            val cleanOutcome = entity.lastOutcome?.takeIf { it == "right" || it == "wrong" }

            if (exists) {
                val updatePayload = mutableMapOf<String, Any>(
                    "bookmarked" to entity.isBookmarked,
                    "attemptsCount" to entity.attemptsCount.coerceIn(0, 100000),
                    "correctCount" to entity.correctCount.coerceIn(0, 100000),
                    "wrongCount" to entity.wrongCount.coerceIn(0, 100000),
                    "clientUpdatedAtMs" to entity.updatedAt.coerceAtLeast(0L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (cleanOutcome != null) {
                    updatePayload["lastOutcome"] = cleanOutcome
                } else {
                    updatePayload["lastOutcome"] = FieldValue.delete()
                }
                docRef.update(updatePayload).await()
            } else {
                val rawCreate = mutableMapOf<String, Any?>(
                    "userId" to uid,
                    "questionId" to safeQuestionId,
                    "bookmarked" to entity.isBookmarked,
                    "attemptsCount" to entity.attemptsCount.coerceIn(0, 100000),
                    "correctCount" to entity.correctCount.coerceIn(0, 100000),
                    "wrongCount" to entity.wrongCount.coerceIn(0, 100000),
                    "lastOutcome" to cleanOutcome,
                    "clientUpdatedAtMs" to entity.updatedAt.coerceAtLeast(0L),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                @Suppress("UNCHECKED_CAST")
                val createPayload = rawCreate.filterValues { it != null } as Map<String, Any>
                docRef.set(createPayload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun upsertProgressBatch(
        userId: String,
        entities: List<QuestionProgressEntity>
    ): Result<Unit> {
        if (entities.isEmpty()) return Result.success(Unit)
        val colPath = "users/$userId/progress"
        return try {
            val uid = requireUserId(userId)
            val existingIds = progressCol(uid).get().await().documents.map { it.id }.toSet()
            // Firestore batches allow up to 500 writes; chunk safely at 400
            entities.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { entity ->
                    val safeQId = CloudMergeLogic.sanitizeFirestoreId(entity.questionId, "q")
                    val docRef = progressCol(uid).document(safeQId)
                    val cleanOutcome = entity.lastOutcome?.takeIf { it == "right" || it == "wrong" }
                    if (safeQId in existingIds) {
                        val updatePayload = mutableMapOf<String, Any>(
                            "bookmarked" to entity.isBookmarked,
                            "attemptsCount" to entity.attemptsCount.coerceIn(0, 100000),
                            "correctCount" to entity.correctCount.coerceIn(0, 100000),
                            "wrongCount" to entity.wrongCount.coerceIn(0, 100000),
                            "clientUpdatedAtMs" to entity.updatedAt.coerceAtLeast(0L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                        updatePayload["lastOutcome"] = cleanOutcome ?: FieldValue.delete()
                        batch.update(docRef, updatePayload)
                    } else {
                        val rawCreate = mapOf<String, Any?>(
                            "userId" to uid,
                            "questionId" to safeQId,
                            "bookmarked" to entity.isBookmarked,
                            "attemptsCount" to entity.attemptsCount.coerceIn(0, 100000),
                            "correctCount" to entity.correctCount.coerceIn(0, 100000),
                            "wrongCount" to entity.wrongCount.coerceIn(0, 100000),
                            "lastOutcome" to cleanOutcome,
                            "clientUpdatedAtMs" to entity.updatedAt.coerceAtLeast(0L),
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                        @Suppress("UNCHECKED_CAST")
                        val createPayload = rawCreate.filterValues { it != null } as Map<String, Any>
                        batch.set(docRef, createPayload)
                    }
                }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, colPath)
            Result.failure(e)
        }
    }

    suspend fun upsertMockHistory(
        userId: String,
        mock: MockHistoryEntity
    ): Result<Unit> {
        val docId = CloudMergeLogic.deterministicMockDocId(mock.timestamp, mock.examId)
        val docPath = "users/$userId/mockHistory/$docId"
        return try {
            val uid = requireUserId(userId)
            val docRef = mockHistoryCol(uid).document(docId)
            val exists = docRef.get().await().exists()
            val safeTitle = mock.title.trim().ifBlank { "CBT Mock" }.take(120)
            val safeExam = mock.examId.trim().ifBlank { PlatformCatalogDefaults.EXAM_RRB_NURSING }.take(64)
            val safeTotal = mock.totalQuestions.coerceIn(1, 500)
            val safeCorrect = mock.correctCount.coerceIn(0, 500)
            val safeWrong = mock.wrongCount.coerceIn(0, 500)
            val safeSkipped = mock.skippedCount.coerceIn(0, 500)
            val safeNet = mock.netScore.coerceIn(-500.0, 500.0)
            val safeAcc = mock.accuracyPercent.coerceIn(0, 100)
            val safeTs = mock.timestamp.coerceAtLeast(0L)

            if (exists) {
                val updatePayload = mapOf<String, Any>(
                    "title" to safeTitle,
                    "examId" to safeExam,
                    "totalQuestions" to safeTotal,
                    "correctCount" to safeCorrect,
                    "wrongCount" to safeWrong,
                    "skippedCount" to safeSkipped,
                    "netScore" to safeNet,
                    "accuracyPercent" to safeAcc,
                    "completedAtMs" to safeTs,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updatePayload).await()
            } else {
                val createPayload = mapOf<String, Any>(
                    "userId" to uid,
                    "title" to safeTitle,
                    "examId" to safeExam,
                    "totalQuestions" to safeTotal,
                    "correctCount" to safeCorrect,
                    "wrongCount" to safeWrong,
                    "skippedCount" to safeSkipped,
                    "netScore" to safeNet,
                    "accuracyPercent" to safeAcc,
                    "completedAtMs" to safeTs,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(createPayload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun upsertCustomQuestions(
        userId: String,
        questions: List<Question>,
        examIdsByQuestion: Map<String, List<String>>
    ): Result<Unit> {
        if (questions.isEmpty()) return Result.success(Unit)
        val colPath = "users/$userId/customQuestions"
        return try {
            val uid = requireUserId(userId)
            val existingIds = customQuestionsCol(uid).get().await().documents.map { it.id }.toSet()
            questions.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { q ->
                    val safeQId = CloudMergeLogic.sanitizeFirestoreId(q.id, "C")
                    val docRef = customQuestionsCol(uid).document(safeQId)
                    val safeOptions = q.options.take(6).let { opts ->
                        if (opts.size >= 2) opts else listOf("Option A", "Option B")
                    }
                    val safeCorrect = q.correctIndex.coerceIn(0, safeOptions.lastIndex)
                    val safeExamIds = examIdsByQuestion[q.id]
                        .orEmpty()
                        .filter { it.isNotBlank() }
                        .distinct()
                        .take(10)
                        .ifEmpty { listOf(PlatformCatalogDefaults.EXAM_RRB_NURSING) }
                    val safeStatus = if (q.sourceStatus == Question.SOURCE_VERIFIED && q.sourceNote.isNotBlank()) {
                        Question.SOURCE_VERIFIED
                    } else {
                        Question.SOURCE_UNVERIFIED
                    }
                    val baseFields = mutableMapOf<String, Any>(
                        "category" to q.category.ifBlank { "nur" }.take(32),
                        "shiftYear" to q.shiftYear.take(40),
                        "subject" to q.subject.ifBlank { "MSN" }.take(40),
                        "difficulty" to q.difficulty.label,
                        "questionText" to q.questionText.trim().ifBlank { "Untitled question" }.take(2000),
                        "options" to safeOptions,
                        "correctIndex" to safeCorrect,
                        "explanation" to q.explanation.take(4000),
                        "sourceStatus" to safeStatus,
                        "sourceNote" to (if (safeStatus == Question.SOURCE_VERIFIED) q.sourceNote.take(200) else ""),
                        "topicId" to q.topicId.take(80),
                        "sourceType" to q.sourceType.dbValue.ifBlank { "practice" }.take(40),
                        "examYear" to q.examYear.take(20),
                        "examShift" to q.examShift.take(32),
                        "examIds" to safeExamIds,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    if (safeQId in existingIds) {
                        batch.update(docRef, baseFields)
                    } else {
                        baseFields["userId"] = uid
                        baseFields["createdAt"] = FieldValue.serverTimestamp()
                        batch.set(docRef, baseFields)
                    }
                }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, colPath)
            Result.failure(e)
        }
    }

    suspend fun deleteAllCustomQuestions(userId: String): Result<Unit> {
        val colPath = "users/$userId/customQuestions"
        return try {
            val uid = requireUserId(userId)
            val docs = customQuestionsCol(uid).get().await().documents
            docs.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { batch.delete(it.reference) }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, colPath)
            Result.failure(e)
        }
    }
}
