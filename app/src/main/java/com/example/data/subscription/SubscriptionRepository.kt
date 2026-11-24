package com.example.data.subscription

import android.content.Context
import android.os.SystemClock
import com.example.R
import com.example.data.cloud.CloudMergeLogic
import com.example.data.cloud.CloudUserProfile
import com.example.data.cloud.OperationType
import com.example.data.cloud.handleFirestoreError
import com.example.data.model.Question
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
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
import java.util.Date

class SubscriptionRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    @Volatile
    private var serverClockBaseMs: Long? = null

    @Volatile
    private var serverClockElapsedRealtimeMs: Long = 0L

    fun currentServerNowMs(): Long {
        val base = serverClockBaseMs ?: return System.currentTimeMillis()
        val delta = (SystemClock.elapsedRealtime() - serverClockElapsedRealtimeMs).coerceAtLeast(0L)
        return base + delta
    }

    private fun requireUid(expected: String? = null): String {
        val uid = auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Please sign in with Google first.")
        if (expected != null) {
            check(uid == expected) { "Signed-in account changed." }
        }
        return uid
    }

    private fun requireAdminEmail(): String {
        val user = auth.currentUser
            ?: throw IllegalStateException("Owner admin must be signed in.")
        val email = user.email.orEmpty().trim()
        check(EntitlementEvaluator.isOwnerAdminEmail(email)) {
            "Admin access is restricted to the app owner."
        }
        return email
    }

    private fun configDoc() =
        db.collection("appConfig").document(CloudSubscriptionConfig.DEFAULT_CONFIG_DOC_ID)

    private fun entitlementDoc(uid: String) = db.collection("entitlements").document(uid)
    private fun accessCodeDoc(code: String) = db.collection("accessCodes").document(code)
    private fun deviceLockDoc(hash: String) = db.collection("deviceTrialLocks").document(hash)
    private fun packDoc(packId: String) = db.collection("questionPacks").document(packId)
    private fun clockSyncDoc(uid: String) = db.collection("clockSync").document(uid)

    // ------------------------------------------------------------------------
    // SERVER CLOCK ANCHORING (Anti local-clock rollback)
    // ------------------------------------------------------------------------
    suspend fun syncServerClock(userId: String): Result<Long> {
        val path = "clockSync/$userId"
        return try {
            val uid = requireUid(userId)
            val ref = clockSyncDoc(uid)
            ref.set(
                mapOf(
                    "userId" to uid,
                    "serverTime" to FieldValue.serverTimestamp()
                )
            ).await()
            val snap = ref.get(Source.SERVER).await()
            val serverTs = snap.getTimestamp("serverTime")?.toDate()?.time
                ?: System.currentTimeMillis()
            serverClockBaseMs = serverTs
            serverClockElapsedRealtimeMs = SystemClock.elapsedRealtime()
            Result.success(serverTs)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // ------------------------------------------------------------------------
    // REALTIME OBSERVERS (Config + User Entitlement)
    // ------------------------------------------------------------------------
    fun observeSubscriptionConfig(): Flow<CloudSubscriptionConfig> = flow {
        requireUid()
        val path = "appConfig/${CloudSubscriptionConfig.DEFAULT_CONFIG_DOC_ID}"
        emitAll(
            configDoc()
                .snapshots()
                .map { snap ->
                    if (!snap.exists()) CloudSubscriptionConfig()
                    else snap.toObject(
                        CloudSubscriptionConfig::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ) ?: CloudSubscriptionConfig()
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    if (auth.currentUser != null) {
                        handleFirestoreError(firestoreError ?: error, OperationType.GET, path)
                    }
                    emit(CloudSubscriptionConfig())
                }
        )
    }

    fun observeEntitlement(userId: String): Flow<CloudUserEntitlement?> = flow {
        val uid = requireUid(userId)
        val path = "entitlements/$uid"
        emitAll(
            entitlementDoc(uid)
                .snapshots()
                .map { snap ->
                    if (!snap.exists()) null
                    else snap.toObject(
                        CloudUserEntitlement::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(firestoreError ?: error, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun fetchEntitlementFromServer(userId: String): Result<CloudUserEntitlement?> {
        val path = "entitlements/$userId"
        return try {
            val uid = requireUid(userId)
            val snap = entitlementDoc(uid).get(Source.SERVER).await()
            val ent = if (snap.exists()) {
                snap.toObject(
                    CloudUserEntitlement::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            } else null
            Result.success(ent)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    suspend fun ensureFreeEntitlement(
        userId: String,
        email: String,
        displayName: String
    ): Result<Unit> {
        val path = "entitlements/$userId"
        return try {
            val uid = requireUid(userId)
            val ref = entitlementDoc(uid)
            val snap = ref.get().await()
            if (!snap.exists()) {
                val payload = mapOf<String, Any>(
                    "userId" to uid,
                    "email" to email.trim().take(120),
                    "displayName" to displayName.trim().take(60),
                    "planTier" to PlanTier.FREE.dbValue,
                    "unlockedExamIds" to emptyList<String>(),
                    "status" to SubscriptionStatus.ACTIVE.dbValue,
                    "trialUsed" to false,
                    "trialDeviceHash" to "",
                    "lastCodeId" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                ref.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    // ------------------------------------------------------------------------
    // ATOMIC 3-DAY DEVICE-LOCKED FREE TRIAL CLAIM
    // ------------------------------------------------------------------------
    suspend fun claimThreeDayFreeTrial(
        userId: String,
        email: String,
        displayName: String,
        device: DeviceIdentity,
        trialDays: Int = 3
    ): TrialClaimResult {
        val path = "deviceTrialLocks/${device.deviceHash}"
        return try {
            val uid = requireUid(userId)
            syncServerClock(uid)
            val serverNow = currentServerNowMs()
            val safeTrialDays = trialDays.coerceIn(1, 3)
            val expiresAtMs = serverNow + (safeTrialDays.toLong() * EntitlementEvaluator.MILLIS_PER_DAY) - 60_000L

            val lockRef = deviceLockDoc(device.deviceHash)
            val entRef = entitlementDoc(uid)

            db.runTransaction { tx ->
                val existingLock = tx.get(lockRef)
                if (existingLock.exists()) {
                    val lockObj = existingLock.toObject(CloudDeviceTrialLock::class.java)
                    return@runTransaction TrialClaimResult.DeviceAlreadyLocked(
                        shortDeviceCode = lockObj?.shortDeviceCode?.ifBlank { device.shortDeviceCode } ?: device.shortDeviceCode,
                        maskedEmail = lockObj?.maskedEmail?.ifBlank { "***" } ?: "***",
                        deviceName = lockObj?.deviceName?.ifBlank { device.deviceName } ?: device.deviceName
                    )
                }

                val existingEntSnap = tx.get(entRef)
                val existingEnt = if (existingEntSnap.exists()) {
                    existingEntSnap.toObject(CloudUserEntitlement::class.java)
                } else null

                if (existingEnt?.trialUsed == true) {
                    return@runTransaction TrialClaimResult.TrialAlreadyUsedByAccount
                }

                val lockPayload = mapOf<String, Any>(
                    "deviceHash" to device.deviceHash,
                    "shortDeviceCode" to device.shortDeviceCode,
                    "ownerUid" to uid,
                    "maskedEmail" to EntitlementEvaluator.maskEmail(email),
                    "deviceName" to device.deviceName.take(64),
                    "claimedAt" to FieldValue.serverTimestamp()
                )
                tx.set(lockRef, lockPayload)

                val startsTs = Timestamp(Date(serverNow))
                val expiresTs = Timestamp(Date(expiresAtMs))

                if (existingEntSnap.exists()) {
                    tx.update(
                        entRef,
                        mapOf<String, Any>(
                            "email" to email.trim().take(120),
                            "displayName" to displayName.trim().take(60),
                            "planTier" to PlanTier.TRIAL.dbValue,
                            "unlockedExamIds" to listOf("all"),
                            "status" to SubscriptionStatus.ACTIVE.dbValue,
                            "startsAt" to startsTs,
                            "expiresAt" to expiresTs,
                            "trialUsed" to true,
                            "trialDeviceHash" to device.deviceHash,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    tx.set(
                        entRef,
                        mapOf<String, Any>(
                            "userId" to uid,
                            "email" to email.trim().take(120),
                            "displayName" to displayName.trim().take(60),
                            "planTier" to PlanTier.TRIAL.dbValue,
                            "unlockedExamIds" to listOf("all"),
                            "status" to SubscriptionStatus.ACTIVE.dbValue,
                            "startsAt" to startsTs,
                            "expiresAt" to expiresTs,
                            "trialUsed" to true,
                            "trialDeviceHash" to device.deviceHash,
                            "lastCodeId" to "",
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                TrialClaimResult.Success(expiresAtMs = expiresAtMs)
            }.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            TrialClaimResult.Error(e.message ?: "Could not activate free trial. Please check your connection.")
        }
    }

    // ------------------------------------------------------------------------
    // ATOMIC POST-PAYMENT ACCESS CODE REDEMPTION
    // ------------------------------------------------------------------------
    suspend fun redeemAccessCode(
        userId: String,
        email: String,
        displayName: String,
        rawCode: String
    ): CodeRedeemResult {
        val codeId = EntitlementEvaluator.normalizeCodeInput(rawCode)
        if (codeId.length < 6 || !CloudMergeLogic.isValidFirestoreId(codeId)) {
            return CodeRedeemResult.InvalidOrUsed("Please enter a valid access code (e.g., CE-ALL-3M-XXXX-XXXX).")
        }
        val path = "accessCodes/$codeId"
        return try {
            val uid = requireUid(userId)
            syncServerClock(uid)
            val serverNow = currentServerNowMs()

            val codeRef = accessCodeDoc(codeId)
            val entRef = entitlementDoc(uid)

            db.runTransaction { tx ->
                val codeSnap = tx.get(codeRef)
                if (!codeSnap.exists()) {
                    return@runTransaction CodeRedeemResult.InvalidOrUsed("Access code not found. Double-check for typos.")
                }
                val codeObj = codeSnap.toObject(CloudAccessCode::class.java)
                    ?: return@runTransaction CodeRedeemResult.InvalidOrUsed("Access code could not be read.")

                if (codeObj.status != "active") {
                    val reason = if (codeObj.status == "redeemed") {
                        "This access code has already been redeemed."
                    } else {
                        "This access code has been revoked."
                    }
                    return@runTransaction CodeRedeemResult.InvalidOrUsed(reason)
                }

                val existingEntSnap = tx.get(entRef)
                val existingEnt = if (existingEntSnap.exists()) {
                    existingEntSnap.toObject(CloudUserEntitlement::class.java)
                } else null

                val codeTier = PlanTier.fromDb(codeObj.planTier)
                val tenureDays = codeObj.tenureDays.toInt().coerceIn(30, 365)
                val newExpiryMs = EntitlementEvaluator.computeNewExpiryMs(existingEnt, tenureDays, serverNow)
                val resolvedExams = EntitlementEvaluator.resolveUnlockedExamsForRedemption(
                    existing = existingEnt,
                    codeTier = codeTier,
                    codeExamIds = codeObj.unlockedExamIds,
                    serverNowMs = serverNow
                )

                tx.update(
                    codeRef,
                    mapOf<String, Any>(
                        "status" to "redeemed",
                        "redeemedByUid" to uid,
                        "redeemedByEmail" to email.trim().take(120),
                        "redeemedAt" to FieldValue.serverTimestamp()
                    )
                )

                val startsTs = Timestamp(Date(serverNow))
                val expiresTs = Timestamp(Date(newExpiryMs))

                if (existingEntSnap.exists()) {
                    tx.update(
                        entRef,
                        mapOf<String, Any>(
                            "email" to email.trim().take(120),
                            "displayName" to displayName.trim().take(60),
                            "planTier" to codeTier.dbValue,
                            "unlockedExamIds" to resolvedExams,
                            "status" to SubscriptionStatus.ACTIVE.dbValue,
                            "startsAt" to startsTs,
                            "expiresAt" to expiresTs,
                            "lastCodeId" to codeId,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    tx.set(
                        entRef,
                        mapOf<String, Any>(
                            "userId" to uid,
                            "email" to email.trim().take(120),
                            "displayName" to displayName.trim().take(60),
                            "planTier" to codeTier.dbValue,
                            "unlockedExamIds" to resolvedExams,
                            "status" to SubscriptionStatus.ACTIVE.dbValue,
                            "startsAt" to startsTs,
                            "expiresAt" to expiresTs,
                            "trialUsed" to false,
                            "trialDeviceHash" to "",
                            "lastCodeId" to codeId,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                val planLabel = if (codeTier == PlanTier.ALL_EXAM) {
                    "All-Exam Nursing Prep (${PlanTenure.fromDays(tenureDays).label})"
                } else {
                    val examLabel = resolvedExams.joinToString(" + ") { EntitlementEvaluator.shortExamLabel(it) }
                    "$examLabel Pro (${PlanTenure.fromDays(tenureDays).label})"
                }
                CodeRedeemResult.Success(planLabel = planLabel, expiresAtMs = newExpiryMs)
            }.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            CodeRedeemResult.Error(e.message ?: "Failed to redeem access code. Check your internet connection.")
        }
    }

    // ------------------------------------------------------------------------
    // STUDENT OTA QUESTION PACK SYNC (Filtered by published status to match rules)
    // ------------------------------------------------------------------------
    suspend fun fetchPublishedQuestionPacks(): Result<List<CloudQuestionPack>> {
        val path = "questionPacks"
        return try {
            requireUid()
            val snaps = db.collection("questionPacks")
                .whereEqualTo("status", "published")
                .get()
                .await()
            val packs = snaps.toObjects(
                CloudQuestionPack::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(packs)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun fetchPublishedQuestionsForPack(packId: String): Result<List<CloudPackQuestion>> {
        val path = "questionPacks/$packId/questions"
        return try {
            requireUid()
            val snaps = packDoc(packId)
                .collection("questions")
                .whereEqualTo("packStatus", "published")
                .get()
                .await()
            val questions = snaps.toObjects(
                CloudPackQuestion::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ).sortedBy { it.sortOrder }
            Result.success(questions)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    // ------------------------------------------------------------------------
    // OWNER ADMIN CONSOLE OPERATIONS (Strict verified owner email RBAC)
    // ------------------------------------------------------------------------
    suspend fun adminFetchUserDirectory(): Result<List<AdminUserDirectoryRow>> {
        return try {
            requireAdminEmail()
            val usersSnap = db.collection("users").get().await()
            val profiles = usersSnap.toObjects(
                CloudUserProfile::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            val entSnap = db.collection("entitlements").get().await()
            val entitlements = entSnap.toObjects(
                CloudUserEntitlement::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )

            val profileMap = profiles.associateBy { it.userId.ifBlank { it.id } }
            val entMap = entitlements.associateBy { it.userId.ifBlank { it.id } }
            val allUids = (profileMap.keys + entMap.keys).distinct()

            val rows = allUids.map { uid ->
                val prof = profileMap[uid]
                val ent = entMap[uid]
                AdminUserDirectoryRow(
                    uid = uid,
                    displayName = prof?.displayName?.ifBlank { ent?.displayName.orEmpty() }
                        ?: ent?.displayName?.ifBlank { "ClinEdge Learner" }
                        ?: "ClinEdge Learner",
                    email = ent?.email?.ifBlank { prof?.email.orEmpty() }
                        ?: prof?.email.orEmpty(),
                    phoneNumber = prof?.phoneNumber.orEmpty(),
                    activeExamId = prof?.activeExamId.orEmpty(),
                    activeDeviceName = prof?.activeDeviceName.orEmpty(),
                    lastDeviceHash = prof?.lastDeviceHash?.ifBlank { ent?.trialDeviceHash.orEmpty() }
                        ?: ent?.trialDeviceHash.orEmpty(),
                    entitlement = ent,
                    profile = prof
                )
            }.sortedByDescending {
                it.entitlement?.updatedAt?.toDate()?.time
                    ?: it.profile?.updatedAt?.toDate()?.time
                    ?: 0L
            }
            Result.success(rows)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "users+entitlements")
            Result.failure(e)
        }
    }

    suspend fun adminUpdateUserEntitlement(
        targetUid: String,
        targetEmail: String,
        targetDisplayName: String,
        planTier: PlanTier,
        unlockedExamIds: List<String>,
        status: SubscriptionStatus,
        tenureDays: Int?,
        extendFromCurrentExpiry: Boolean = true
    ): Result<Unit> {
        val path = "entitlements/$targetUid"
        return try {
            requireAdminEmail()
            val ref = entitlementDoc(targetUid)
            val snap = ref.get().await()
            val existing = if (snap.exists()) snap.toObject(CloudUserEntitlement::class.java) else null
            val nowMs = currentServerNowMs()

            val resolvedExams = when (planTier) {
                PlanTier.ALL_EXAM, PlanTier.TRIAL -> listOf("all")
                PlanTier.SINGLE_EXAM -> unlockedExamIds.filter { it.isNotBlank() }.distinct().take(5)
                PlanTier.FREE -> emptyList()
            }

            val expiresAtTs: Timestamp? = when {
                planTier == PlanTier.FREE -> null
                tenureDays != null && tenureDays > 0 -> {
                    val ms = if (extendFromCurrentExpiry) {
                        EntitlementEvaluator.computeNewExpiryMs(existing, tenureDays, nowMs)
                    } else {
                        nowMs + tenureDays.toLong() * EntitlementEvaluator.MILLIS_PER_DAY - 60_000L
                    }
                    Timestamp(Date(ms))
                }
                else -> existing?.expiresAt
            }

            if (snap.exists()) {
                val payload = mutableMapOf<String, Any>(
                    "email" to targetEmail.trim().take(120),
                    "displayName" to targetDisplayName.trim().take(60),
                    "planTier" to planTier.dbValue,
                    "unlockedExamIds" to resolvedExams,
                    "status" to status.dbValue,
                    "startsAt" to Timestamp(Date(nowMs)),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (expiresAtTs != null) {
                    payload["expiresAt"] = expiresAtTs
                } else {
                    payload["expiresAt"] = FieldValue.delete()
                }
                ref.update(payload).await()
            } else {
                val raw = mutableMapOf<String, Any?>(
                    "userId" to targetUid,
                    "email" to targetEmail.trim().take(120),
                    "displayName" to targetDisplayName.trim().take(60),
                    "planTier" to planTier.dbValue,
                    "unlockedExamIds" to resolvedExams,
                    "status" to status.dbValue,
                    "startsAt" to Timestamp(Date(nowMs)),
                    "expiresAt" to expiresAtTs,
                    "trialUsed" to (planTier == PlanTier.TRIAL),
                    "trialDeviceHash" to "",
                    "lastCodeId" to "ADMIN_GRANT",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                @Suppress("UNCHECKED_CAST")
                ref.set(raw.filterValues { it != null } as Map<String, Any>).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun adminGenerateAccessCode(
        planTier: PlanTier,
        examId: String?,
        tenure: PlanTenure,
        buyerNote: String
    ): Result<CloudAccessCode> {
        return try {
            val adminEmail = requireAdminEmail()
            val safeTier = if (planTier == PlanTier.SINGLE_EXAM) PlanTier.SINGLE_EXAM else PlanTier.ALL_EXAM
            val exams = if (safeTier == PlanTier.ALL_EXAM) {
                listOf("all")
            } else {
                listOf(examId?.takeIf { it.isNotBlank() } ?: "aiims_norcet")
            }
            val code = EntitlementEvaluator.generateAccessCode(safeTier, exams.firstOrNull(), tenure)
            val payload = mapOf<String, Any>(
                "code" to code,
                "planTier" to safeTier.dbValue,
                "unlockedExamIds" to exams,
                "tenureDays" to tenure.days,
                "status" to "active",
                "buyerNote" to buyerNote.trim().take(120),
                "createdByEmail" to adminEmail,
                "createdAt" to FieldValue.serverTimestamp()
            )
            accessCodeDoc(code).set(payload).await()
            Result.success(
                CloudAccessCode(
                    id = code,
                    code = code,
                    planTier = safeTier.dbValue,
                    unlockedExamIds = exams,
                    tenureDays = tenure.days.toLong(),
                    status = "active",
                    buyerNote = buyerNote.trim().take(120),
                    createdByEmail = adminEmail,
                    createdAt = Timestamp.now()
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "accessCodes")
            Result.failure(e)
        }
    }

    suspend fun adminFetchAccessCodes(): Result<List<CloudAccessCode>> {
        return try {
            requireAdminEmail()
            val snaps = db.collection("accessCodes")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(100)
                .get()
                .await()
            val codes = snaps.toObjects(
                CloudAccessCode::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(codes)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "accessCodes")
            Result.failure(e)
        }
    }

    suspend fun adminRevokeAccessCode(codeId: String): Result<Unit> {
        return try {
            requireAdminEmail()
            accessCodeDoc(codeId).update(mapOf("status" to "revoked")).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "accessCodes/$codeId")
            Result.failure(e)
        }
    }

    suspend fun adminFetchDeviceLocks(): Result<List<CloudDeviceTrialLock>> {
        return try {
            requireAdminEmail()
            val snaps = db.collection("deviceTrialLocks")
                .orderBy("claimedAt", Query.Direction.DESCENDING)
                .limit(100)
                .get()
                .await()
            Result.success(
                snaps.toObjects(
                    CloudDeviceTrialLock::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "deviceTrialLocks")
            Result.failure(e)
        }
    }

    suspend fun adminDeleteDeviceLock(deviceHash: String): Result<Unit> {
        return try {
            requireAdminEmail()
            deviceLockDoc(deviceHash).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "deviceTrialLocks/$deviceHash")
            Result.failure(e)
        }
    }

    suspend fun adminSaveSubscriptionConfig(config: CloudSubscriptionConfig): Result<Unit> {
        val path = "appConfig/${CloudSubscriptionConfig.DEFAULT_CONFIG_DOC_ID}"
        return try {
            requireAdminEmail()
            val payload = mapOf<String, Any>(
                "singleExam1M" to config.singleExam1M.coerceIn(0L, 100000L),
                "singleExam3M" to config.singleExam3M.coerceIn(0L, 100000L),
                "singleExam6M" to config.singleExam6M.coerceIn(0L, 100000L),
                "singleExam1Y" to config.singleExam1Y.coerceIn(0L, 100000L),
                "allExam1M" to config.allExam1M.coerceIn(0L, 100000L),
                "allExam3M" to config.allExam3M.coerceIn(0L, 100000L),
                "allExam6M" to config.allExam6M.coerceIn(0L, 100000L),
                "allExam1Y" to config.allExam1Y.coerceIn(0L, 100000L),
                "freeDailyQuestionLimit" to config.freeDailyQuestionLimit.coerceIn(1L, 200L),
                "trialDays" to config.trialDays.coerceIn(1L, 30L),
                "supportWhatsAppNumber" to config.supportWhatsAppNumber.trim().take(24),
                "supportEmail" to config.supportEmail.trim().take(120),
                "upiId" to config.upiId.trim().take(80),
                "upiPayeeName" to config.upiPayeeName.trim().ifBlank { "ClinEdge Nursing Prep" }.take(80),
                "catalogVersion" to config.catalogVersion.coerceIn(0L, 1000000L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            configDoc().set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun adminFetchAllQuestionPacks(): Result<List<CloudQuestionPack>> {
        return try {
            requireAdminEmail()
            val snaps = db.collection("questionPacks")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .limit(100)
                .get()
                .await()
            Result.success(
                snaps.toObjects(
                    CloudQuestionPack::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "questionPacks")
            Result.failure(e)
        }
    }

    suspend fun adminPublishQuestionPack(
        title: String,
        examId: String,
        stage: String,
        subject: String,
        isPremiumOnly: Boolean,
        questions: List<Question>,
        currentConfig: CloudSubscriptionConfig
    ): Result<CloudQuestionPack> {
        return try {
            requireAdminEmail()
            require(questions.isNotEmpty()) { "Add at least one parsed question before publishing." }

            val nextVersion = (currentConfig.catalogVersion + 1L).coerceAtLeast(1L)
            val slugTitle = title.trim().lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_').take(28)
            val packId = CloudMergeLogic.sanitizeFirestoreId(
                "pack_${slugTitle.ifBlank { "set" }}_${System.currentTimeMillis() / 1000}",
                "pack"
            )
            val safeStage = if (stage in listOf("all", "prelims", "mains")) stage else "all"
            val packMetaPayload = mapOf<String, Any>(
                "packId" to packId,
                "title" to title.trim().ifBlank { "ClinEdge Question Pack" }.take(120),
                "examId" to examId.trim().ifBlank { "aiims_norcet" }.take(64),
                "stage" to safeStage,
                "subject" to subject.trim().take(40),
                "questionCount" to questions.size,
                "isPremiumOnly" to isPremiumOnly,
                "status" to "published",
                "version" to nextVersion,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val packReference = packDoc(packId)
            packReference.set(packMetaPayload).await()

            val qCol = packReference.collection("questions")
            questions.chunked(350).forEachIndexed { chunkIdx, chunk ->
                val batch = db.batch()
                chunk.forEachIndexed { idx, q ->
                    val globalIndex = chunkIdx * 350 + idx
                    val qDocId = "Q_${globalIndex + 1}"
                    val safeOptions = q.options.take(6).let { opts ->
                        if (opts.size >= 2) opts else listOf("Option A", "Option B")
                    }
                    val safeCorrect = q.correctIndex.coerceIn(0, safeOptions.lastIndex)
                    val stageShiftTag = when (safeStage) {
                        "mains" -> q.examShift.ifBlank { "Mains" }
                        "prelims" -> q.examShift.ifBlank { "Prelims" }
                        else -> q.examShift
                    }
                    val shiftYearLabel = q.shiftYear.ifBlank {
                        if (safeStage == "mains") "$title (Mains)" else title
                    }
                    val qPayload = mapOf<String, Any>(
                        "packId" to packId,
                        "packStatus" to "published",
                        "examId" to examId.trim().ifBlank { "aiims_norcet" }.take(64),
                        "category" to q.category.ifBlank { "nur" }.take(32),
                        "shiftYear" to shiftYearLabel.take(40),
                        "subject" to (q.subject.ifBlank { subject.ifBlank { "MSN" } }).take(40),
                        "difficulty" to q.difficulty.label,
                        "questionText" to q.questionText.trim().ifBlank { "Untitled question" }.take(2000),
                        "options" to safeOptions,
                        "correctIndex" to safeCorrect,
                        "explanation" to q.explanation.take(4000),
                        "sourceStatus" to Question.SOURCE_VERIFIED,
                        "sourceNote" to (q.sourceNote.ifBlank { "ClinEdge Pack: $title" }).take(200),
                        "topicId" to q.topicId.take(80),
                        "sourceType" to q.sourceType.dbValue.ifBlank { "practice" }.take(40),
                        "examYear" to q.examYear.take(20),
                        "examShift" to stageShiftTag.take(32),
                        "sortOrder" to globalIndex,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                    batch.set(qCol.document(qDocId), qPayload)
                }
                batch.commit().await()
            }

            // Bump catalogVersion so all students auto-sync on next launch
            adminSaveSubscriptionConfig(currentConfig.copy(catalogVersion = nextVersion))

            Result.success(
                CloudQuestionPack(
                    id = packId,
                    packId = packId,
                    title = title.trim().take(120),
                    examId = examId,
                    stage = safeStage,
                    subject = subject,
                    questionCount = questions.size.toLong(),
                    isPremiumOnly = isPremiumOnly,
                    status = "published",
                    version = nextVersion
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "questionPacks")
            Result.failure(e)
        }
    }

    suspend fun adminDeleteQuestionPack(packId: String): Result<Unit> {
        return try {
            requireAdminEmail()
            val packRef = packDoc(packId)
            val qSnaps = packRef.collection("questions").get().await().documents
            qSnaps.chunked(350).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { batch.delete(it.reference) }
                batch.commit().await()
            }
            packRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "questionPacks/$packId")
            Result.failure(e)
        }
    }
}
