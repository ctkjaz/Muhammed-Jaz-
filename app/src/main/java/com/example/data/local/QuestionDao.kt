package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionDao {
    @Transaction
    @Query("SELECT * FROM questions ORDER BY isCustom ASC, sortOrder ASC, id ASC")
    fun observeAllQuestionsWithProgress(): Flow<List<QuestionWithProgressRelation>>

    @Query("SELECT COUNT(*) FROM questions WHERE isCustom = 0")
    suspend fun getBuiltInCount(): Int

    @Query("SELECT COUNT(*) FROM questions")
    suspend fun getTotalCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<QuestionEntity>)

    @Query("DELETE FROM questions WHERE isCustom = 1")
    suspend fun deleteCustomQuestions()

    @Query("SELECT * FROM question_progress WHERE questionId = :questionId LIMIT 1")
    suspend fun getProgressForQuestion(questionId: String): QuestionProgressEntity?

    @Query("SELECT * FROM question_progress")
    suspend fun getAllProgressSnapshot(): List<QuestionProgressEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgress(progress: QuestionProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgressList(list: List<QuestionProgressEntity>)

    @Query("SELECT * FROM mock_history ORDER BY timestamp DESC LIMIT 25")
    fun observeMockHistory(): Flow<List<MockHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMockHistory(history: MockHistoryEntity)

    @Query("DELETE FROM question_progress")
    suspend fun clearAllProgress()
}
