package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionDao {
    // ------------------------------------------------------------------------
    // PLATFORM CATALOGUE (PROFESSIONS, EXAMS, SECTIONS, SUBJECTS, TOPICS)
    // ------------------------------------------------------------------------
    @Query("SELECT * FROM professions WHERE isActive = 1 ORDER BY sortOrder ASC, name ASC")
    fun observeProfessions(): Flow<List<ProfessionEntity>>

    @Query("SELECT * FROM exams WHERE isActive = 1 ORDER BY sortOrder ASC, name ASC")
    fun observeExams(): Flow<List<ExamEntity>>

    @Query("SELECT * FROM exam_sections WHERE examId = :examId ORDER BY sortOrder ASC")
    fun observeExamSections(examId: String): Flow<List<ExamSectionEntity>>

    @Query("SELECT * FROM subjects ORDER BY sortOrder ASC, name ASC")
    fun observeSubjects(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM topics ORDER BY sortOrder ASC, name ASC")
    fun observeTopics(): Flow<List<TopicEntity>>

    @Upsert
    suspend fun upsertProfessions(items: List<ProfessionEntity>)

    @Upsert
    suspend fun upsertExams(items: List<ExamEntity>)

    @Upsert
    suspend fun upsertExamSections(items: List<ExamSectionEntity>)

    @Upsert
    suspend fun upsertSubjects(items: List<SubjectEntity>)

    @Upsert
    suspend fun upsertTopics(items: List<TopicEntity>)

    // ------------------------------------------------------------------------
    // QUESTIONS & MULTI-EXAM LINKS
    // ------------------------------------------------------------------------
    @Query("SELECT * FROM questions ORDER BY isCustom ASC, sortOrder ASC, id ASC")
    fun observeAllQuestionEntities(): Flow<List<QuestionEntity>>

    @Transaction
    @Query("SELECT * FROM questions ORDER BY isCustom ASC, sortOrder ASC, id ASC")
    fun observeAllQuestionsWithProgress(): Flow<List<QuestionWithProgressRelation>>

    @Query(
        """
        SELECT q.* FROM questions q
        INNER JOIN exam_question_links l ON l.questionId = q.id
        WHERE l.examId = :examId
        ORDER BY q.isCustom ASC, l.sortOrder ASC, q.id ASC
        """
    )
    fun observeQuestionEntitiesForExam(examId: String): Flow<List<QuestionEntity>>

    @Transaction
    @Query(
        """
        SELECT q.* FROM questions q
        INNER JOIN exam_question_links l ON l.questionId = q.id
        WHERE l.examId = :examId
        ORDER BY q.isCustom ASC, l.sortOrder ASC, q.id ASC
        """
    )
    fun observeQuestionsForExam(examId: String): Flow<List<QuestionWithProgressRelation>>

    @Query("SELECT * FROM exam_question_links")
    fun observeAllExamLinks(): Flow<List<ExamQuestionLinkEntity>>

    @Query("SELECT * FROM exam_question_links WHERE examId = :examId")
    fun observeExamLinksForExam(examId: String): Flow<List<ExamQuestionLinkEntity>>

    @Query("SELECT * FROM exam_question_links WHERE questionId = :questionId")
    suspend fun getLinksForQuestion(questionId: String): List<ExamQuestionLinkEntity>

    @Query("SELECT COUNT(*) FROM exam_question_links WHERE examId = :examId")
    suspend fun getLinkCountForExam(examId: String): Int

    @Upsert
    suspend fun upsertExamQuestionLinks(links: List<ExamQuestionLinkEntity>)

    @Query("SELECT COUNT(*) FROM questions WHERE isCustom = 0")
    suspend fun getBuiltInCount(): Int

    @Query("SELECT COUNT(*) FROM questions")
    suspend fun getTotalCount(): Int

    @Upsert
    suspend fun insertQuestions(questions: List<QuestionEntity>)

    @Query("DELETE FROM exam_question_links WHERE questionId IN (SELECT id FROM questions WHERE isCustom = 1)")
    suspend fun deleteCustomQuestionLinks()

    @Query("DELETE FROM questions WHERE isCustom = 1")
    suspend fun deleteCustomQuestions()

    // ------------------------------------------------------------------------
    // PROGRESS & MOCK HISTORY
    // ------------------------------------------------------------------------
    @Query("SELECT * FROM question_progress WHERE questionId = :questionId LIMIT 1")
    suspend fun getProgressForQuestion(questionId: String): QuestionProgressEntity?

    @Query("SELECT * FROM question_progress")
    fun observeAllProgress(): Flow<List<QuestionProgressEntity>>

    @Query("SELECT * FROM question_progress")
    suspend fun getAllProgressSnapshot(): List<QuestionProgressEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgress(progress: QuestionProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgressList(list: List<QuestionProgressEntity>)

    @Query("SELECT * FROM mock_history ORDER BY timestamp DESC LIMIT 25")
    fun observeMockHistory(): Flow<List<MockHistoryEntity>>

    @Query("SELECT * FROM mock_history ORDER BY timestamp DESC LIMIT 25")
    suspend fun getAllMockHistorySnapshot(): List<MockHistoryEntity>

    @Query("SELECT * FROM mock_history WHERE examId = :examId ORDER BY timestamp DESC LIMIT 25")
    fun observeMockHistoryForExam(examId: String): Flow<List<MockHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMockHistory(history: MockHistoryEntity)

    @Query("DELETE FROM mock_history")
    suspend fun clearMockHistory()

    @Query("DELETE FROM question_progress")
    suspend fun clearAllProgress()

    @Transaction
    @Query("SELECT * FROM questions WHERE isCustom = 1 ORDER BY sortOrder ASC, id ASC")
    suspend fun getCustomQuestionsSnapshot(): List<QuestionWithProgressRelation>
}
