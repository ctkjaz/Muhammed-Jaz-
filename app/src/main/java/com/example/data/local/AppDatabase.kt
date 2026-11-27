package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ProfessionEntity::class,
        ExamEntity::class,
        ExamSectionEntity::class,
        SubjectEntity::class,
        TopicEntity::class,
        QuestionEntity::class,
        ExamQuestionLinkEntity::class,
        QuestionProgressEntity::class,
        MockHistoryEntity::class
    ],
    version = 6,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun questionDao(): QuestionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE questions ADD COLUMN difficulty TEXT NOT NULL DEFAULT 'Medium'")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE questions ADD COLUMN sourceStatus TEXT NOT NULL DEFAULT 'unverified'")
                db.execSQL("ALTER TABLE questions ADD COLUMN sourceNote TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create new multi-exam catalogue tables
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `professions` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        `isActive` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `exams` (
                        `id` TEXT NOT NULL,
                        `professionId` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `shortName` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `defaultMockQuestions` INTEGER NOT NULL,
                        `defaultMockMinutes` INTEGER NOT NULL,
                        `positiveMarks` REAL NOT NULL,
                        `negativeMarks` REAL NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        `isActive` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`professionId`) REFERENCES `professions`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_exams_professionId` ON `exams` (`professionId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `exam_sections` (
                        `id` TEXT NOT NULL,
                        `examId` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `weightagePercent` INTEGER NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`examId`) REFERENCES `exams`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_exam_sections_examId` ON `exam_sections` (`examId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `subjects` (
                        `code` TEXT NOT NULL,
                        `professionId` TEXT,
                        `name` TEXT NOT NULL,
                        `accentHex` TEXT NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        PRIMARY KEY(`code`)
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `topics` (
                        `id` TEXT NOT NULL,
                        `subjectCode` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`subjectCode`) REFERENCES `subjects`(`code`) ON UPDATE NO ACTION ON DELETE NO ACTION
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_topics_subjectCode` ON `topics` (`subjectCode`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `exam_question_links` (
                        `examId` TEXT NOT NULL,
                        `questionId` TEXT NOT NULL,
                        `sectionId` TEXT NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        `isPyqForExam` INTEGER NOT NULL,
                        `examYear` TEXT NOT NULL,
                        `examShift` TEXT NOT NULL,
                        PRIMARY KEY(`examId`, `questionId`),
                        FOREIGN KEY(`examId`) REFERENCES `exams`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION,
                        FOREIGN KEY(`questionId`) REFERENCES `questions`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION,
                        FOREIGN KEY(`sectionId`) REFERENCES `exam_sections`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_exam_question_links_examId_sectionId` ON `exam_question_links` (`examId`, `sectionId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_exam_question_links_questionId` ON `exam_question_links` (`questionId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_exam_question_links_sectionId` ON `exam_question_links` (`sectionId`)")

                // 2. Additive columns on existing questions & mock_history tables (IDs and progress untouched)
                db.execSQL("ALTER TABLE `questions` ADD COLUMN `topicId` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `questions` ADD COLUMN `sourceType` TEXT NOT NULL DEFAULT 'practice'")
                db.execSQL("ALTER TABLE `questions` ADD COLUMN `examYear` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `questions` ADD COLUMN `examShift` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `mock_history` ADD COLUMN `examId` TEXT NOT NULL DEFAULT 'rrb_nursing'")

                // 3. Seed Nursing profession, RRB Nursing exam, sections, and 16 subjects + default General topics
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO `professions` (`id`, `name`, `description`, `sortOrder`, `isActive`)
                    VALUES ('nursing', 'Nursing', 'Central & State Nursing Officer, Staff Nurse, and Nursing Superintendent exams', 0, 1)
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO `exams` (
                        `id`, `professionId`, `name`, `shortName`, `description`,
                        `defaultMockQuestions`, `defaultMockMinutes`, `positiveMarks`, `negativeMarks`, `sortOrder`, `isActive`
                    ) VALUES (
                        'rrb_nursing', 'nursing', 'RRB Nursing Superintendent & Staff Nurse', 'RRB Nursing',
                        '100-question CBT pattern (70% Nursing, 30% Non-Tech & CA/GK) • 90 min • +1 / -0.33',
                        100, 90, 1.0, 0.33, 0, 1
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO `exam_sections` (`id`, `examId`, `name`, `weightagePercent`, `sortOrder`) VALUES
                    ('rrb_nursing_core', 'rrb_nursing', 'Nursing Core (70%)', 70, 0),
                    ('rrb_nursing_ca_gk', 'rrb_nursing', 'Current Affairs & Static GK', 10, 1),
                    ('rrb_nursing_nontech', 'rrb_nursing', 'Science, Arithmetic & Reasoning (30%)', 20, 2)
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO `subjects` (`code`, `professionId`, `name`, `accentHex`, `sortOrder`) VALUES
                    ('MSN', 'nursing', 'Medical-Surgical', '#8E4456', 0),
                    ('Pharm', 'nursing', 'Pharmacology', '#756091', 1),
                    ('OBG', 'nursing', 'OBG & Midwifery', '#B78B79', 2),
                    ('Peds', 'nursing', 'Child Health (Peds)', '#A67942', 3),
                    ('CHN', 'nursing', 'Community Health', '#478271', 4),
                    ('Psych', 'nursing', 'Mental Health', '#666B8A', 5),
                    ('FON', 'nursing', 'Fundamentals (FON)', '#537B8A', 6),
                    ('Anat', 'nursing', 'Anatomy & Physio', '#5B8078', 7),
                    ('Admin', 'nursing', 'Admin & Research', '#726D70', 8),
                    ('CA-2026', NULL, '2025–26 National & Global', '#7D5360', 9),
                    ('Railways', NULL, 'Indian Railways & Budget', '#6A637D', 10),
                    ('Health-Prog', NULL, 'Health Drives & UIP', '#4E8570', 11),
                    ('Static-GK', NULL, 'Static GK & Polity', '#9F7A4D', 12),
                    ('Science', NULL, 'General Science (NCERT)', '#527D86', 13),
                    ('Math', NULL, 'Arithmetic & Dosage', '#A96D54', 14),
                    ('Reasoning', NULL, 'Logical Reasoning', '#7E6189', 15)
                    """.trimIndent()
                )
                // Ensure any unexpected custom subject codes also exist in `subjects`
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO `subjects` (`code`, `professionId`, `name`, `accentHex`, `sortOrder`)
                    SELECT DISTINCT `subject`, NULL, `subject`, '#726D70', 99
                    FROM `questions`
                    WHERE `subject` != ''
                    """.trimIndent()
                )
                // Seed a default "General" topic for every subject
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO `topics` (`id`, `subjectCode`, `name`, `sortOrder`)
                    SELECT REPLACE(LOWER(`code`), '-', '_') || '_general', `code`, 'General', 0
                    FROM `subjects`
                    """.trimIndent()
                )

                // 4. Backfill examYear, examShift, topicId, and honest sourceType on all existing questions
                db.execSQL(
                    """
                    UPDATE `questions`
                    SET
                        `examYear` = CASE
                            WHEN INSTR(`shiftYear`, '-') > 0 THEN SUBSTR(`shiftYear`, 1, INSTR(`shiftYear`, '-') - 1)
                            ELSE TRIM(`shiftYear`)
                        END,
                        `examShift` = CASE
                            WHEN INSTR(`shiftYear`, '-') > 0 THEN SUBSTR(`shiftYear`, INSTR(`shiftYear`, '-') + 1)
                            ELSE ''
                        END,
                        `topicId` = REPLACE(LOWER(`subject`), '-', '_') || '_general',
                        `sourceType` = CASE
                            WHEN (`category` = 'pyq' OR TRIM(`shiftYear`) != '') AND `sourceStatus` = 'verified' AND TRIM(`sourceNote`) != '' THEN 'official_pyq'
                            WHEN (`category` = 'pyq' OR TRIM(`shiftYear`) != '') THEN 'memory_based'
                            WHEN `category` = 'ca' THEN 'current_affairs'
                            ELSE 'practice'
                        END
                    """.trimIndent()
                )

                // 5. Link EVERY existing question (built-in + custom) to `rrb_nursing` without touching question_progress
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO `exam_question_links` (
                        `examId`, `questionId`, `sectionId`, `sortOrder`, `isPyqForExam`, `examYear`, `examShift`
                    )
                    SELECT
                        'rrb_nursing',
                        `id`,
                        CASE
                            WHEN `subject` IN ('MSN', 'Pharm', 'OBG', 'Peds', 'CHN', 'Psych', 'FON', 'Anat', 'Admin') THEN 'rrb_nursing_core'
                            WHEN `subject` IN ('CA-2026', 'Railways', 'Health-Prog', 'Static-GK') THEN 'rrb_nursing_ca_gk'
                            ELSE 'rrb_nursing_nontech'
                        END,
                        `sortOrder`,
                        CASE WHEN (`category` = 'pyq' OR TRIM(`shiftYear`) != '') THEN 1 ELSE 0 END,
                        `examYear`,
                        `examShift`
                    FROM `questions`
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `questions` ADD COLUMN `imageUrl` TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_questions_subject` ON `questions` (`subject`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_questions_category` ON `questions` (`category`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_questions_sourceType` ON `questions` (`sourceType`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_questions_isCustom_sortOrder` ON `questions` (`isCustom`, `sortOrder`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_question_progress_isBookmarked` ON `question_progress` (`isBookmarked`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_question_progress_lastOutcome` ON `question_progress` (`lastOutcome`)")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "rrb_nursing_cbt_v1.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
