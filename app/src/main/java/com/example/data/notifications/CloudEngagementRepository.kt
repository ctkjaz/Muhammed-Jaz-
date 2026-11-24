package com.example.data.notifications

import android.content.Context
import com.example.R
import com.example.data.cloud.OperationType
import com.example.data.cloud.handleFirestoreError
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
import java.util.UUID

class CloudEngagementRepository(
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
            ?: throw IllegalStateException("Sign in with Google to use live alerts and Doubt Box.")
        if (expectedUserId != null) {
            check(uid == expectedUserId) { "Signed-in account changed; refusing cross-user Firestore request." }
        }
        return uid
    }

    private fun announcementsCol() = db.collection("announcements")
    private fun doubtsCol() = db.collection("doubts")
    private fun directNotificationsCol(userId: String) =
        db.collection("users").document(userId).collection("notifications")

    private fun newFirestoreId(prefix: String): String =
        "${prefix}_${System.currentTimeMillis()}_${UUID.randomUUID().toString().replace("-", "").take(8)}"

    // ------------------------------------------------------------------------
    // REALTIME STREAMS (Auth-gated, ServerTimestampBehavior.ESTIMATE)
    // ------------------------------------------------------------------------

    fun observeAnnouncements(): Flow<List<CloudAnnouncement>> = flow {
        val uid = requireUserId()
        val path = "announcements"
        emitAll(
            announcementsCol()
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        CloudAnnouncement::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAtMs }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val ex = firestoreError ?: (error as? Exception)
                    if (ex != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(ex, OperationType.LIST, path)
                    }
                    emit(emptyList())
                }
        )
    }

    fun observeDirectNotifications(userId: String): Flow<List<CloudDirectNotification>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/notifications"
        emitAll(
            directNotificationsCol(uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(60)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        CloudDirectNotification::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAtMs }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val ex = firestoreError ?: (error as? Exception)
                    if (ex != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(ex, OperationType.LIST, path)
                    }
                    emit(emptyList())
                }
        )
    }

    fun observeStudentDoubts(userId: String): Flow<List<CloudDoubt>> = flow {
        val uid = requireUserId(userId)
        val path = "doubts"
        emitAll(
            doubtsCol()
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        CloudDoubt::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAtMs }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val ex = firestoreError ?: (error as? Exception)
                    if (ex != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(ex, OperationType.LIST, path)
                    }
                    emit(emptyList())
                }
        )
    }

    fun observeAllDoubtsForAdmin(): Flow<List<CloudDoubt>> = flow {
        val uid = requireUserId()
        val path = "doubts"
        emitAll(
            doubtsCol()
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        CloudDoubt::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAtMs }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val ex = firestoreError ?: (error as? Exception)
                    if (ex != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(ex, OperationType.LIST, path)
                    }
                    emit(emptyList())
                }
        )
    }

    // ------------------------------------------------------------------------
    // BACKGROUND ONE-SHOT FETCHERS (For WorkManager Tray Sync)
    // ------------------------------------------------------------------------

    suspend fun fetchRecentAnnouncementsOnce(limit: Long = 20): List<CloudAnnouncement> {
        if (auth.currentUser == null) return emptyList()
        return try {
            val query = announcementsCol()
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
            val snap = try {
                query.get(Source.SERVER).await()
            } catch (_: Exception) {
                query.get().await()
            }
            snap.toObjects(
                CloudAnnouncement::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "announcements")
            emptyList()
        }
    }

    suspend fun fetchUnreadDirectNotificationsOnce(userId: String, limit: Long = 25): List<CloudDirectNotification> {
        if (auth.currentUser?.uid != userId) return emptyList()
        val path = "users/$userId/notifications"
        return try {
            val query = directNotificationsCol(userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
            val snap = try {
                query.get(Source.SERVER).await()
            } catch (_: Exception) {
                query.get().await()
            }
            snap.toObjects(
                CloudDirectNotification::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
                .filter { !it.read }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            emptyList()
        }
    }

    // ------------------------------------------------------------------------
    // ADMIN BROADCAST & INDIVIDUAL PUSH MUTATIONS
    // ------------------------------------------------------------------------

    suspend fun publishAnnouncement(
        title: String,
        body: String,
        category: AnnouncementCategory,
        targetExamId: String,
        priority: AnnouncementPriority,
        actionUrl: String
    ): Result<String> {
        val validationError = EngagementLogic.validateNotificationFields(title, body, actionUrl)
        if (validationError != null) return Result.failure(IllegalArgumentException(validationError))
        val id = newFirestoreId("ann")
        val path = "announcements/$id"
        return try {
            requireUserId()
            val adminEmail = auth.currentUser?.email.orEmpty().take(120)
            val payload = mapOf(
                "announcementId" to id,
                "title" to title.trim().take(EngagementLogic.MAX_TITLE_LENGTH),
                "body" to body.trim().take(EngagementLogic.MAX_BODY_LENGTH),
                "category" to category.dbValue,
                "targetExamId" to targetExamId.trim().ifBlank { "all" }.take(64),
                "priority" to priority.dbValue,
                "actionUrl" to actionUrl.trim().take(EngagementLogic.MAX_URL_LENGTH),
                "createdByEmail" to adminEmail,
                "createdAt" to FieldValue.serverTimestamp()
            )
            announcementsCol().document(id).set(payload).await()
            Result.success(id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteAnnouncement(announcementId: String): Result<Unit> {
        val path = "announcements/$announcementId"
        return try {
            requireUserId()
            announcementsCol().document(announcementId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun sendDirectNotification(
        targetUserId: String,
        title: String,
        body: String,
        type: DirectNotificationType = DirectNotificationType.CUSTOM_ADMIN,
        referenceId: String = "",
        actionUrl: String = ""
    ): Result<String> {
        val cleanUid = targetUserId.trim()
        if (cleanUid.isEmpty()) return Result.failure(IllegalArgumentException("Select a student recipient first."))
        val validationError = EngagementLogic.validateNotificationFields(title, body, actionUrl)
        if (validationError != null) return Result.failure(IllegalArgumentException(validationError))

        val id = newFirestoreId("dn")
        val path = "users/$cleanUid/notifications/$id"
        return try {
            requireUserId()
            val adminEmail = auth.currentUser?.email.orEmpty().take(120)
            val payload = mapOf(
                "notificationId" to id,
                "userId" to cleanUid,
                "title" to title.trim().take(EngagementLogic.MAX_TITLE_LENGTH),
                "body" to body.trim().take(EngagementLogic.MAX_BODY_LENGTH),
                "type" to type.dbValue,
                "referenceId" to referenceId.trim().take(128),
                "actionUrl" to actionUrl.trim().take(EngagementLogic.MAX_URL_LENGTH),
                "read" to false,
                "createdByEmail" to adminEmail,
                "createdAt" to FieldValue.serverTimestamp()
            )
            directNotificationsCol(cleanUid).document(id).set(payload).await()
            Result.success(id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun markDirectNotificationRead(userId: String, notificationId: String): Result<Unit> {
        val path = "users/$userId/notifications/$notificationId"
        return try {
            requireUserId(userId)
            directNotificationsCol(userId).document(notificationId)
                .update(mapOf("read" to true))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun markAllDirectNotificationsRead(userId: String, unreadIds: List<String>): Result<Unit> {
        if (unreadIds.isEmpty()) return Result.success(Unit)
        val path = "users/$userId/notifications"
        return try {
            requireUserId(userId)
            unreadIds.chunked(250).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { id ->
                    batch.update(directNotificationsCol(userId).document(id), mapOf("read" to true))
                }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteDirectNotification(userId: String, notificationId: String): Result<Unit> {
        val path = "users/$userId/notifications/$notificationId"
        return try {
            requireUserId()
            directNotificationsCol(userId).document(notificationId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // ------------------------------------------------------------------------
    // STUDENT DOUBT BOX & ADMIN DOUBT DESK MUTATIONS
    // ------------------------------------------------------------------------

    suspend fun submitDoubt(
        userId: String,
        studentName: String,
        studentEmail: String,
        examId: String,
        subject: String,
        questionId: String,
        questionPreview: String,
        doubtText: String
    ): Result<String> {
        val validationError = EngagementLogic.validateDoubtText(doubtText)
        if (validationError != null) return Result.failure(IllegalArgumentException(validationError))

        val id = newFirestoreId("dbt")
        val path = "doubts/$id"
        return try {
            val uid = requireUserId(userId)
            val payload = mapOf(
                "doubtId" to id,
                "userId" to uid,
                "studentName" to studentName.trim().ifBlank { "ClinEdge Student" }.take(60),
                "studentEmail" to studentEmail.trim().take(120),
                "examId" to examId.trim().ifBlank { "rrb_nursing" }.take(64),
                "subject" to subject.trim().ifBlank { "MSN" }.take(40),
                "questionId" to questionId.trim().take(128),
                "questionPreview" to questionPreview.trim().take(EngagementLogic.MAX_PREVIEW_LENGTH),
                "doubtText" to doubtText.trim().take(EngagementLogic.MAX_DOUBT_LENGTH),
                "status" to DoubtStatus.OPEN.dbValue,
                "adminReply" to "",
                "repliedBy" to "",
                "studentReadReply" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            doubtsCol().document(id).set(payload).await()
            Result.success(id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    /**
     * Admin replies to a student doubt and atomically triggers a personal notification
     * in `/users/{studentUid}/notifications/{notifId}` so the student receives an Android tray alert.
     */
    suspend fun replyToDoubt(
        doubt: CloudDoubt,
        replyText: String,
        repliedByLabel: String = "ClinEdge Faculty"
    ): Result<Unit> {
        val validationError = EngagementLogic.validateAdminReply(replyText)
        if (validationError != null) return Result.failure(IllegalArgumentException(validationError))

        val cleanReply = replyText.trim().take(EngagementLogic.MAX_REPLY_LENGTH)
        val doubtPath = "doubts/${doubt.doubtId}"
        return try {
            requireUserId()
            val adminEmail = auth.currentUser?.email.orEmpty().take(120)
            val notifId = newFirestoreId("dr")
            val batch = db.batch()
            batch.update(
                doubtsCol().document(doubt.doubtId),
                mapOf(
                    "status" to DoubtStatus.ANSWERED.dbValue,
                    "adminReply" to cleanReply,
                    "repliedBy" to repliedByLabel.trim().ifBlank { "ClinEdge Faculty" }.take(80),
                    "repliedAt" to FieldValue.serverTimestamp(),
                    "studentReadReply" to false,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            val notifTitle = "Faculty replied to your ${doubt.subject} doubt"
                .take(EngagementLogic.MAX_TITLE_LENGTH)
            val notifBody = cleanReply.take(EngagementLogic.MAX_BODY_LENGTH)
            batch.set(
                directNotificationsCol(doubt.userId).document(notifId),
                mapOf(
                    "notificationId" to notifId,
                    "userId" to doubt.userId,
                    "title" to notifTitle,
                    "body" to notifBody,
                    "type" to DirectNotificationType.DOUBT_REPLY.dbValue,
                    "referenceId" to doubt.doubtId.take(128),
                    "actionUrl" to "",
                    "read" to false,
                    "createdByEmail" to adminEmail,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, doubtPath)
            Result.failure(e)
        }
    }

    suspend fun markDoubtReplyRead(doubtId: String): Result<Unit> {
        val path = "doubts/$doubtId"
        return try {
            requireUserId()
            doubtsCol().document(doubtId).update(
                mapOf(
                    "studentReadReply" to true,
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

    suspend fun closeDoubt(doubtId: String): Result<Unit> {
        val path = "doubts/$doubtId"
        return try {
            requireUserId()
            doubtsCol().document(doubtId).update(
                mapOf(
                    "status" to DoubtStatus.CLOSED.dbValue,
                    "studentReadReply" to true,
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

    suspend fun deleteDoubt(doubtId: String): Result<Unit> {
        val path = "doubts/$doubtId"
        return try {
            requireUserId()
            doubtsCol().document(doubtId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}
