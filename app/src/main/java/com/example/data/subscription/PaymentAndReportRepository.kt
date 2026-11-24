package com.example.data.subscription

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.cloud.CloudMergeLogic
import com.example.data.model.Question
import com.example.data.model.ClinicalImageRef
import com.example.data.model.QuestionDifficulty
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

private const val TAG = "ClinEdgePayReportRepo"

/**
 * Firestore repository for:
 * 1. Student UPI UTR payment requests + atomic 1-tap Admin approval/rejection.
 * 2. Student MCQ error flags + Admin review queue + inline OTA question editing.
 */
class PaymentAndReportRepository(
    context: Context,
    private val subscriptionRepository: SubscriptionRepository
) {
    private val appContext = context.applicationContext
    private val db: FirebaseFirestore by lazy {
        val dbId = appContext.getString(R.string.firestore_database_id)
        FirebaseFirestore.getInstance(dbId)
    }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    private fun paymentRequestsCol() = db.collection("paymentRequests")
    private fun questionReportsCol() = db.collection("questionReports")
    private fun entitlementDoc(uid: String) = db.collection("entitlements").document(uid)
    private fun directNotificationsCol(uid: String) =
        db.collection("users").document(uid).collection("notifications")

    private fun requireAdminEmail(): String {
        val email = auth.currentUser?.email.orEmpty()
        require(EntitlementEvaluator.isOwnerAdminEmail(email)) {
            "Admin access restricted to verified owner account."
        }
        return email
    }

    // ------------------------------------------------------------------------
    // 1. UPI Payment Requests (Student + Admin)
    // ------------------------------------------------------------------------

    fun observeStudentPaymentRequests(uid: String): Flow<List<CloudPaymentRequest>> = callbackFlow {
        if (uid.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val registration = paymentRequestsCol()
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.w(TAG, "observeStudentPaymentRequests error", err)
                    return@addSnapshotListener
                }
                val list = snap?.toObjects(
                    CloudPaymentRequest::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).orEmpty().sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    fun observeAdminPaymentRequests(): Flow<List<CloudPaymentRequest>> = callbackFlow {
        val registration = paymentRequestsCol()
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(120)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.w(TAG, "observeAdminPaymentRequests error", err)
                    return@addSnapshotListener
                }
                val list = snap?.toObjects(
                    CloudPaymentRequest::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).orEmpty()
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    suspend fun submitPaymentRequest(
        uid: String,
        studentName: String,
        studentEmail: String,
        planTier: PlanTier,
        unlockedExamIds: List<String>,
        tenureDays: Int,
        amountInr: Int,
        utrNumber: String,
        payerName: String
    ): Result<String> {
        return try {
            val cleanUtr = UpiPaymentLogic.sanitizeUtr(utrNumber)
            require(UpiPaymentLogic.isValidUtr(cleanUtr)) {
                "Enter a valid 6–32 character UPI UTR / Reference number."
            }
            val safeTier = if (planTier == PlanTier.ALL_EXAM) PlanTier.ALL_EXAM else PlanTier.SINGLE_EXAM
            val safeExams = if (safeTier == PlanTier.ALL_EXAM) {
                listOf("all")
            } else {
                unlockedExamIds.filter { it.isNotBlank() }.distinct().take(5).ifEmpty { listOf("aiims_norcet") }
            }
            val reqId = CloudMergeLogic.sanitizeFirestoreId(
                "pay_${System.currentTimeMillis()}_${cleanUtr.takeLast(6)}",
                "pay"
            )
            val payload = mapOf<String, Any>(
                "requestId" to reqId,
                "userId" to uid,
                "studentName" to studentName.trim().ifBlank { "Nursing Aspirant" }.take(60),
                "studentEmail" to studentEmail.trim().take(120),
                "planTier" to safeTier.dbValue,
                "unlockedExamIds" to safeExams,
                "tenureDays" to tenureDays.coerceIn(1, 365),
                "amountInr" to amountInr.coerceIn(0, 100000),
                "utrNumber" to cleanUtr,
                "payerName" to payerName.trim().take(80),
                "status" to PaymentRequestStatus.PENDING.dbValue,
                "adminNote" to "",
                "reviewedByEmail" to "",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            paymentRequestsCol().document(reqId).set(payload).await()
            Result.success(reqId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "submitPaymentRequest failed", e)
            Result.failure(e)
        }
    }

    /**
     * Atomic 1-tap approval:
     * 1. Marks paymentRequest approved
     * 2. Creates or updates/stacks student entitlement (respecting Single-Exam vs All-Exam & tenure days)
     * 3. Sends an instant DirectNotification to the student's inbox & Android tray
     */
    suspend fun adminApprovePaymentRequest(
        request: CloudPaymentRequest,
        adminNote: String = "UTR verified & plan activated"
    ): Result<Unit> {
        return try {
            val adminEmail = requireAdminEmail()
            val reqId = request.requestId.ifBlank { request.id }
            val studentUid = request.userId
            require(reqId.isNotBlank() && studentUid.isNotBlank()) { "Invalid payment request." }

            val serverNowMs = subscriptionRepository
                .syncServerClock(auth.currentUser?.uid.orEmpty())
                .getOrElse { subscriptionRepository.currentServerNowMs() }
            val entSnap = entitlementDoc(studentUid).get().await()
            val existingEnt = if (entSnap.exists()) {
                entSnap.toObject(CloudUserEntitlement::class.java)
            } else {
                null
            }

            val targetTier = if (request.parsedTier == PlanTier.ALL_EXAM) PlanTier.ALL_EXAM else PlanTier.SINGLE_EXAM
            val resolvedExams = EntitlementEvaluator.resolveUnlockedExamsForRedemption(
                existing = existingEnt,
                codeTier = targetTier,
                codeExamIds = request.unlockedExamIds,
                serverNowMs = serverNowMs
            )
            val newExpiryMs = EntitlementEvaluator.computeNewExpiryMs(
                existing = existingEnt,
                tenureDays = request.tenureDays.toInt().coerceIn(1, 365),
                serverNowMs = serverNowMs
            )
            val expiresTs = Timestamp(Date(newExpiryMs))
            val lastRef = "UPI_${request.utrNumber}".take(64)

            val notifId = CloudMergeLogic.sanitizeFirestoreId(
                "pay_ok_${System.currentTimeMillis()}",
                "notif"
            )
            val notifPayload = mapOf<String, Any>(
                "notificationId" to notifId,
                "userId" to studentUid,
                "title" to "Plan Activated via UPI",
                "body" to "Payment (UTR ${request.utrNumber}) verified! Your ${request.planSummaryLabel} is active until ${EntitlementEvaluator.formatShortDate(newExpiryMs)}.",
                "type" to "account_notice",
                "referenceId" to reqId,
                "actionUrl" to "",
                "read" to false,
                "createdByEmail" to adminEmail,
                "createdAt" to FieldValue.serverTimestamp()
            )

            val batch = db.batch()
            batch.update(
                paymentRequestsCol().document(reqId),
                mapOf(
                    "status" to PaymentRequestStatus.APPROVED.dbValue,
                    "adminNote" to adminNote.trim().take(300),
                    "reviewedByEmail" to adminEmail,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (existingEnt != null) {
                batch.update(
                    entitlementDoc(studentUid),
                    mapOf(
                        "planTier" to targetTier.dbValue,
                        "unlockedExamIds" to resolvedExams,
                        "status" to SubscriptionStatus.ACTIVE.dbValue,
                        "startsAt" to FieldValue.serverTimestamp(),
                        "expiresAt" to expiresTs,
                        "lastCodeId" to lastRef,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.set(
                    entitlementDoc(studentUid),
                    mapOf(
                        "userId" to studentUid,
                        "email" to request.studentEmail.trim().take(120),
                        "displayName" to request.studentName.trim().ifBlank { "Nursing Aspirant" }.take(60),
                        "planTier" to targetTier.dbValue,
                        "unlockedExamIds" to resolvedExams,
                        "status" to SubscriptionStatus.ACTIVE.dbValue,
                        "startsAt" to FieldValue.serverTimestamp(),
                        "expiresAt" to expiresTs,
                        "trialUsed" to false,
                        "trialDeviceHash" to "",
                        "lastCodeId" to lastRef,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.set(directNotificationsCol(studentUid).document(notifId), notifPayload)
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "adminApprovePaymentRequest failed", e)
            Result.failure(e)
        }
    }

    suspend fun adminRejectPaymentRequest(
        request: CloudPaymentRequest,
        reason: String
    ): Result<Unit> {
        return try {
            val adminEmail = requireAdminEmail()
            val reqId = request.requestId.ifBlank { request.id }
            val studentUid = request.userId
            val cleanReason = reason.trim().ifBlank { "UTR could not be matched. Please verify and resubmit or contact WhatsApp support." }.take(300)

            val notifId = CloudMergeLogic.sanitizeFirestoreId(
                "pay_rej_${System.currentTimeMillis()}",
                "notif"
            )
            val notifPayload = mapOf<String, Any>(
                "notificationId" to notifId,
                "userId" to studentUid,
                "title" to "UPI Payment Verification Update",
                "body" to "UTR ${request.utrNumber} could not be approved: $cleanReason",
                "type" to "account_notice",
                "referenceId" to reqId,
                "actionUrl" to "",
                "read" to false,
                "createdByEmail" to adminEmail,
                "createdAt" to FieldValue.serverTimestamp()
            )

            val batch = db.batch()
            batch.update(
                paymentRequestsCol().document(reqId),
                mapOf(
                    "status" to PaymentRequestStatus.REJECTED.dbValue,
                    "adminNote" to cleanReason,
                    "reviewedByEmail" to adminEmail,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(directNotificationsCol(studentUid).document(notifId), notifPayload)
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "adminRejectPaymentRequest failed", e)
            Result.failure(e)
        }
    }

    // ------------------------------------------------------------------------
    // 2. Question Error Flagging + Admin Reports & Inline OTA Editor
    // ------------------------------------------------------------------------

    fun observeAdminQuestionReports(): Flow<List<CloudQuestionReport>> = callbackFlow {
        val registration = questionReportsCol()
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(150)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.w(TAG, "observeAdminQuestionReports error", err)
                    return@addSnapshotListener
                }
                val list = snap?.toObjects(
                    CloudQuestionReport::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).orEmpty()
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    suspend fun submitQuestionReport(
        uid: String,
        studentName: String,
        studentEmail: String,
        examId: String,
        question: Question,
        reason: QuestionReportReason,
        note: String
    ): Result<String> {
        return try {
            val reportId = CloudMergeLogic.sanitizeFirestoreId(
                "qrep_${System.currentTimeMillis()}_${uid.take(6)}",
                "qrep"
            )
            val payload = mapOf<String, Any>(
                "reportId" to reportId,
                "userId" to uid,
                "studentName" to studentName.trim().ifBlank { "Nursing Aspirant" }.take(60),
                "studentEmail" to studentEmail.trim().take(120),
                "examId" to examId.trim().take(64),
                "subject" to question.subject.trim().take(40),
                "questionId" to question.id.trim().take(128),
                "questionPreview" to question.questionText.trim().take(280),
                "reason" to reason.dbValue,
                "note" to note.trim().take(1000),
                "status" to QuestionReportStatus.OPEN.dbValue,
                "adminResolutionNote" to "",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            questionReportsCol().document(reportId).set(payload).await()
            Result.success(reportId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "submitQuestionReport failed", e)
            Result.failure(e)
        }
    }

    suspend fun adminUpdateQuestionReportStatus(
        report: CloudQuestionReport,
        newStatus: QuestionReportStatus,
        resolutionNote: String,
        notifyStudent: Boolean
    ): Result<Unit> {
        return try {
            val adminEmail = requireAdminEmail()
            val reportId = report.reportId.ifBlank { report.id }
            val cleanNote = resolutionNote.trim().take(500)

            val batch = db.batch()
            batch.update(
                questionReportsCol().document(reportId),
                mapOf(
                    "status" to newStatus.dbValue,
                    "adminResolutionNote" to cleanNote,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (notifyStudent && report.userId.isNotBlank()) {
                val notifId = CloudMergeLogic.sanitizeFirestoreId(
                    "qrep_res_${System.currentTimeMillis()}",
                    "notif"
                )
                val title = if (newStatus == QuestionReportStatus.RESOLVED) {
                    "Question Report Resolved — Thank You!"
                } else {
                    "Update on Your Question Report"
                }
                val body = buildString {
                    append("Thanks for flagging ${report.subject} MCQ (${report.questionId}). ")
                    if (cleanNote.isNotBlank()) {
                        append(cleanNote)
                    } else if (newStatus == QuestionReportStatus.RESOLVED) {
                        append("Our faculty reviewed and updated the question bank.")
                    } else {
                        append("Our faculty reviewed the key and confirmed it matches standard guidelines.")
                    }
                }.take(1900)
                batch.set(
                    directNotificationsCol(report.userId).document(notifId),
                    mapOf(
                        "notificationId" to notifId,
                        "userId" to report.userId,
                        "title" to title,
                        "body" to body,
                        "type" to "account_notice",
                        "referenceId" to reportId,
                        "actionUrl" to "",
                        "read" to false,
                        "createdByEmail" to adminEmail,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "adminUpdateQuestionReportStatus failed", e)
            Result.failure(e)
        }
    }

    /**
     * Loads a cloud OTA question from /questionPacks/{packId}/questions/{docId} for inline admin editing.
     */
    suspend fun adminLoadOtaQuestionForEdit(
        localQuestionId: String,
        knownPackIds: Collection<String>,
        reportId: String? = null
    ): Result<OtaQuestionEditDraft> {
        return try {
            requireAdminEmail()
            val coords = UpiPaymentLogic.parseOtaQuestionCoordinates(localQuestionId, knownPackIds)
                ?: return Result.failure(
                    IllegalStateException(
                        "Question '$localQuestionId' is a bundled offline question (not an OTA cloud pack question). " +
                            "Bundled questions update via app releases; you can resolve/note this report now."
                    )
                )
            val (packId, docId) = coords
            val snap = db.collection("questionPacks")
                .document(packId)
                .collection("questions")
                .document(docId)
                .get()
                .await()
            if (!snap.exists()) {
                return Result.failure(IllegalStateException("OTA question $packId/$docId was not found in Firestore."))
            }
            val cloudQ = snap.toObject(CloudPackQuestion::class.java)
                ?: return Result.failure(IllegalStateException("Unable to decode OTA question $packId/$docId."))
            Result.success(
                OtaQuestionEditDraft(
                    packId = packId,
                    docId = docId,
                    localQuestionId = localQuestionId,
                    examId = cloudQ.examId,
                    subject = cloudQ.subject,
                    difficulty = QuestionDifficulty.fromRaw(cloudQ.difficulty),
                    questionText = cloudQ.questionText,
                    options = cloudQ.options,
                    correctIndex = cloudQ.correctIndex.toInt().coerceIn(0, (cloudQ.options.size - 1).coerceAtLeast(0)),
                    explanation = cloudQ.explanation,
                    imageUrl = cloudQ.imageUrl,
                    reportId = reportId
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "adminLoadOtaQuestionForEdit failed", e)
            Result.failure(e)
        }
    }

    /**
     * Updates an OTA question in Firestore and bumps catalogVersion so all students sync the fix automatically.
     */
    suspend fun adminSaveOtaQuestionEdit(
        draft: OtaQuestionEditDraft,
        currentConfig: CloudSubscriptionConfig
    ): Result<Unit> {
        return try {
            requireAdminEmail()
            val cleanOptions = draft.options.map { it.trim() }.filter { it.isNotBlank() }.take(6)
            require(cleanOptions.size >= 2) { "Provide at least 2 non-empty options." }
            require(draft.questionText.trim().isNotBlank()) { "Question text cannot be blank." }
            val safeCorrect = draft.correctIndex.coerceIn(0, cleanOptions.lastIndex)

            val docRef = db.collection("questionPacks")
                .document(draft.packId)
                .collection("questions")
                .document(draft.docId)
            docRef.update(
                mapOf(
                    "questionText" to draft.questionText.trim().take(2000),
                    "options" to cleanOptions,
                    "correctIndex" to safeCorrect,
                    "explanation" to draft.explanation.trim().take(4000),
                    "difficulty" to draft.difficulty.label,
                    "imageUrl" to ClinicalImageRef.sanitize(draft.imageUrl).take(512)
                )
            ).await()

            val nextVersion = (currentConfig.catalogVersion + 1L).coerceAtLeast(1L)
            db.collection("questionPacks")
                .document(draft.packId)
                .update(
                    mapOf(
                        "version" to nextVersion,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            subscriptionRepository.adminSaveSubscriptionConfig(
                currentConfig.copy(catalogVersion = nextVersion)
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "adminSaveOtaQuestionEdit failed", e)
            Result.failure(e)
        }
    }
}
