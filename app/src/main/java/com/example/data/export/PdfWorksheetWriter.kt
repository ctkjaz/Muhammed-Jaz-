package com.example.data.export

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.data.model.DifficultyFilter
import com.example.data.model.Question
import com.example.data.model.QuestionItem
import com.example.data.model.SubjectCatalog
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil

data class PdfWorksheetOptions(
    val title: String,
    val subtitle: String,
    val inlineAnswers: Boolean = false,
    val includeAnswerKey: Boolean = true
)

object PdfWorksheetWriter {
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN_X = 36f
    private const val MARGIN_TOP = 38f
    private const val MARGIN_BOTTOM = 42f
    private const val CONTENT_WIDTH = (PAGE_WIDTH - MARGIN_X * 2).toInt()
    private const val MAX_Y = PAGE_HEIGHT - MARGIN_BOTTOM

    fun sourceDisplayName(sourceKey: String): String = when (sourceKey) {
        "MIXED" -> "All Questions"
        "BOOKMARKS" -> "Bookmarked Revision"
        "MISTAKES" -> "Recent Mistakes"
        "LOWEST_ACCURACY" -> "Lowest Accuracy"
        "PYQ" -> "PYQ-Tagged Questions"
        "NURSING" -> "Nursing Core (70%)"
        "CA" -> "Current Affairs & Static GK"
        "GEN" -> "Science, Math & Reasoning"
        else -> SubjectCatalog.displayNameFor(sourceKey)
    }

    fun resolveWorksheetQuestions(
        sourceKey: String,
        all: List<QuestionItem>,
        lowestAccuracy: List<QuestionItem>,
        difficultyFilter: DifficultyFilter,
        limit: Int
    ): List<QuestionItem> {
        val base = when (sourceKey) {
            "MIXED" -> all
            "BOOKMARKS" -> all.filter { it.isBookmarked }
            "MISTAKES" -> all.filter { it.lastOutcome == "wrong" }
            "LOWEST_ACCURACY" -> lowestAccuracy
            "PYQ" -> all.filter { it.question.category == "pyq" || it.question.shiftYear.isNotBlank() }
            "NURSING" -> all.filter { SubjectCatalog.isNursing(it.question) }
            "CA" -> all.filter { SubjectCatalog.isCa(it.question) }
            "GEN" -> all.filter { SubjectCatalog.isGen(it.question) }
            else -> all.filter { it.question.subject == sourceKey }
        }
        val filtered = difficultyFilter.difficulty?.let { diff ->
            base.filter { it.question.difficulty == diff }
        } ?: base
        return if (limit > 0) filtered.take(limit) else filtered
    }

    fun buildSuggestedPdfFileName(
        sourceKey: String,
        difficultyFilter: DifficultyFilter,
        questionCount: Int,
        inlineAnswers: Boolean
    ): String {
        val srcSlug = sourceKey.lowercase(Locale.US).replace(Regex("[^a-z0-9]+"), "-").trim('-')
        val diffSlug = difficultyFilter.difficulty?.label?.lowercase(Locale.US) ?: "all"
        val modeSlug = if (inlineAnswers) "study" else "mock"
        return "rrb-nursing-$srcSlug-$diffSlug-${questionCount}q-$modeSlug.pdf"
    }

    fun estimatePageCount(
        questionCount: Int,
        inlineAnswers: Boolean,
        includeAnswerKey: Boolean
    ): Int {
        if (questionCount <= 0) return 0
        val perPage = if (inlineAnswers) 4.0 else 6.0
        val questionPages = ceil(questionCount / perPage).toInt().coerceAtLeast(1)
        val keyPages = if (!inlineAnswers && includeAnswerKey) {
            ceil(questionCount / 14.0).toInt().coerceAtLeast(1)
        } else {
            0
        }
        return questionPages + keyPages
    }

    fun formatQuestionTagLine(index: Int, question: Question): String {
        val qTagParts = buildList {
            add(question.subject)
            add(question.difficulty.label)
            question.pyqBadgeText()?.let { add(it) }
        }
        return "Q${index + 1}  •  ${qTagParts.joinToString("  •  ")}"
    }

    fun writeWorksheetPdf(
        questions: List<QuestionItem>,
        options: PdfWorksheetOptions,
        outputStream: OutputStream
    ): Int {
        require(questions.isNotEmpty()) { "Cannot export an empty question list." }

        val doc = PdfDocument()
        val accentPaint = Paint().apply { color = Color.rgb(84, 47, 59); isAntiAlias = true }
        val dividerPaint = Paint().apply { color = Color.rgb(214, 201, 190); strokeWidth = 0.8f }
        val boxBgPaint = Paint().apply { color = Color.rgb(246, 240, 233); isAntiAlias = true }

        val tagPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(84, 47, 59)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(52, 56, 61)
            textSize = 17f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val subPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(87, 82, 87)
            textSize = 10f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val metaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(114, 108, 115)
            textSize = 8.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val stemPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(52, 56, 61)
            textSize = 10.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val optionPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(52, 56, 61)
            textSize = 9.8f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val answerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(42, 107, 90)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val rationalePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(87, 82, 87)
            textSize = 9.0f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(114, 108, 115)
            textSize = 8.5f
        }

        var pageNumber = 0
        var page: PdfDocument.Page? = null
        var canvas: Canvas? = null
        var y = MARGIN_TOP

        fun finishCurrentPage() {
            val currentCanvas = canvas
            val currentPage = page
            if (currentCanvas != null && currentPage != null) {
                currentCanvas.drawLine(MARGIN_X, PAGE_HEIGHT - 28f, PAGE_WIDTH - MARGIN_X, PAGE_HEIGHT - 28f, dividerPaint)
                currentCanvas.drawText(
                    "ClinEdge • Offline Practice Worksheet",
                    MARGIN_X,
                    PAGE_HEIGHT - 16f,
                    footerPaint
                )
                val pageLabel = "Page $pageNumber"
                val labelW = footerPaint.measureText(pageLabel)
                currentCanvas.drawText(
                    pageLabel,
                    PAGE_WIDTH - MARGIN_X - labelW,
                    PAGE_HEIGHT - 16f,
                    footerPaint
                )
                doc.finishPage(currentPage)
            }
            page = null
            canvas = null
        }

        fun startNewPage() {
            finishCurrentPage()
            pageNumber++
            val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            page = doc.startPage(info)
            canvas = page?.canvas
            y = MARGIN_TOP
        }

        fun makeLayout(text: CharSequence, paint: TextPaint, width: Int): StaticLayout {
            return StaticLayout.Builder.obtain(text, 0, text.length, paint, width.coerceAtLeast(40))
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(2.5f, 1.0f)
                .setIncludePad(false)
                .build()
        }

        fun drawLayout(c: Canvas, layout: StaticLayout, x: Float, topY: Float) {
            c.save()
            c.translate(x, topY)
            layout.draw(c)
            c.restore()
        }

        try {
            startNewPage()
            val c1 = canvas!!

            // Page 1 Hero Header
            val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date())
            val titleLayout = makeLayout(options.title, titlePaint, CONTENT_WIDTH - 24)
            val subLayout = makeLayout(options.subtitle, subPaint, CONTENT_WIDTH - 24)
            val headerHeight = 28f + titleLayout.height + 6f + subLayout.height + 26f
            c1.drawRect(MARGIN_X, y, PAGE_WIDTH - MARGIN_X, y + headerHeight, boxBgPaint)
            c1.drawRect(MARGIN_X, y, MARGIN_X + 4f, y + headerHeight, accentPaint)
            c1.drawText("CLINEDGE • PRINTABLE WORKSHEET", MARGIN_X + 14f, y + 16f, tagPaint)
            drawLayout(c1, titleLayout, MARGIN_X + 14f, y + 24f)
            drawLayout(c1, subLayout, MARGIN_X + 14f, y + 24f + titleLayout.height + 5f)
            val metaLineY = y + headerHeight - 9f
            c1.drawText(
                "${questions.size} MCQs   |   Date: $dateStr   |   Name: ______________________   Score: ____ / ${questions.size}",
                MARGIN_X + 14f,
                metaLineY,
                subPaint
            )
            y += headerHeight + 16f

            // Questions loop
            questions.forEachIndexed { idx, item ->
                val q = item.question
                val qTagText = formatQuestionTagLine(idx, q)
                val stemLayout = makeLayout(q.questionText, stemPaint, CONTENT_WIDTH)
                val optionLayouts = q.options.mapIndexed { optIdx, opt ->
                    val letter = ('A' + optIdx)
                    makeLayout("$letter.  $opt", optionPaint, CONTENT_WIDTH - 14)
                }
                val correctLetter = ('A' + q.correctIndex)
                val correctText = q.options.getOrElse(q.correctIndex) { "" }
                val ansLayout = if (options.inlineAnswers) {
                    makeLayout("Answer: $correctLetter) $correctText", answerPaint, CONTENT_WIDTH - 12)
                } else null
                val expLayout = if (options.inlineAnswers && q.explanation.isNotBlank()) {
                    makeLayout("Rationale: ${q.explanation}", rationalePaint, CONTENT_WIDTH - 12)
                } else null

                val blockHeight = 14f +
                    stemLayout.height + 6f +
                    optionLayouts.sumOf { it.height + 4 } +
                    (ansLayout?.let { it.height + 6f } ?: 0f) +
                    (expLayout?.let { it.height + 4f } ?: 0f) +
                    14f

                if (y + blockHeight > MAX_Y && y > MARGIN_TOP + 20f) {
                    startNewPage()
                }

                val c = canvas!!
                c.drawText(qTagText, MARGIN_X, y + 9f, metaPaint)
                y += 14f
                drawLayout(c, stemLayout, MARGIN_X, y)
                y += stemLayout.height + 6f

                optionLayouts.forEach { optLayout ->
                    drawLayout(c, optLayout, MARGIN_X + 10f, y)
                    y += optLayout.height + 4f
                }

                if (ansLayout != null) {
                    y += 2f
                    drawLayout(c, ansLayout, MARGIN_X + 10f, y)
                    y += ansLayout.height + 4f
                }
                if (expLayout != null) {
                    drawLayout(c, expLayout, MARGIN_X + 10f, y)
                    y += expLayout.height + 4f
                }

                y += 5f
                c.drawLine(MARGIN_X, y, PAGE_WIDTH - MARGIN_X, y, dividerPaint)
                y += 10f
            }

            // Answer Key & Rationales section at the end if Mock Test Booklet mode
            if (!options.inlineAnswers && options.includeAnswerKey) {
                startNewPage()
                val keyCanvas = canvas!!
                keyCanvas.drawText("ANSWER KEY & CLINICAL RATIONALES", MARGIN_X, y + 14f, titlePaint)
                y += 22f
                keyCanvas.drawText(options.title, MARGIN_X, y + 10f, subPaint)
                y += 18f
                keyCanvas.drawLine(MARGIN_X, y, PAGE_WIDTH - MARGIN_X, y, dividerPaint)
                y += 12f

                // Compact 5-column quick answer key grid
                val colWidth = CONTENT_WIDTH / 5f
                questions.chunked(5).forEachIndexed { rowIdx, rowItems ->
                    if (y + 18f > MAX_Y) {
                        startNewPage()
                    }
                    val rowCanvas = canvas!!
                    rowItems.forEachIndexed { colIdx, item ->
                        val qNum = rowIdx * 5 + colIdx + 1
                        val letter = ('A' + item.question.correctIndex)
                        rowCanvas.drawText(
                            "$qNum. $letter",
                            MARGIN_X + colIdx * colWidth,
                            y + 10f,
                            answerPaint
                        )
                    }
                    y += 16f
                }

                y += 8f
                canvas!!.drawLine(MARGIN_X, y, PAGE_WIDTH - MARGIN_X, y, dividerPaint)
                y += 14f

                // Concise rationales
                questions.forEachIndexed { idx, item ->
                    val q = item.question
                    val letter = ('A' + q.correctIndex)
                    val optText = q.options.getOrElse(q.correctIndex) { "" }
                    val ansLine = makeLayout("Q${idx + 1} • Ans: $letter ($optText)", answerPaint, CONTENT_WIDTH)
                    val expLine = if (q.explanation.isNotBlank()) {
                        makeLayout(q.explanation, rationalePaint, CONTENT_WIDTH - 8)
                    } else null
                    val itemHeight = ansLine.height + (expLine?.let { it.height + 4f } ?: 0f) + 10f

                    if (y + itemHeight > MAX_Y && y > MARGIN_TOP + 20f) {
                        startNewPage()
                    }

                    val c = canvas!!
                    drawLayout(c, ansLine, MARGIN_X, y)
                    y += ansLine.height + 3f
                    if (expLine != null) {
                        drawLayout(c, expLine, MARGIN_X + 6f, y)
                        y += expLine.height + 4f
                    }
                    y += 5f
                }
            }

            finishCurrentPage()
            doc.writeTo(outputStream)
            outputStream.flush()
            return pageNumber
        } finally {
            doc.close()
        }
    }
}
