package com.example.data.parser

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.Inflater
import kotlin.math.abs

/**
 * Pure-Kotlin on-device text extractor for ClinEdge PDF / TXT / JSON imports.
 * Supports:
 *  - Plain text (.txt) and JSON (.json) files (with UTF-8 BOM stripping)
 *  - Text-based PDFs (uncompressed, FlateDecode/zlib, ASCIIHexDecode, ASCII85Decode streams;
 *    proper PDF lexical tokenization so strings containing "BT"/"ET"/"Td" never corrupt output;
 *    position-aware Td/TD/Tm/T* line vs space detection; ToUnicode CMap bfchar + bfrange
 *    including array-form bfrange; WinAnsiEncoding 0x80..0x9F glyph mapping).
 *  - Reliable Android ContentResolver URI reading with AssetFileDescriptor fallback for OEM
 *    document providers (ColorOS, OxygenOS, OneUI, WhatsApp, Google Drive).
 */
object PdfTextExtractor {

    private const val MAX_FILE_BYTES = 25 * 1024 * 1024 // 25 MB safety limit

    data class ExtractionResult(
        val text: String,
        val isLikelyScannedPdf: Boolean = false,
        val fileType: String = "text"
    )

    suspend fun extractFromUri(context: Context, uri: Uri): Result<ExtractionResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                val bytes = readUriBytes(context, uri)
                    ?: throw IllegalStateException("Could not open selected file.")
                extractFromBytes(bytes)
            }
        }

    private fun readUriBytes(context: Context, uri: Uri): ByteArray? {
        val resolver = context.contentResolver
        val fromStream = runCatching {
            resolver.openInputStream(uri)?.use { readBoundedBytes(it) }
        }.getOrNull()
        if (fromStream != null && fromStream.isNotEmpty()) return fromStream

        return runCatching {
            resolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
                afd.createInputStream().use { readBoundedBytes(it) }
            }
        }.getOrNull()
    }

    private fun readBoundedBytes(input: InputStream): ByteArray {
        val out = ByteArrayOutputStream()
        val buf = ByteArray(16 * 1024)
        var total = 0
        while (true) {
            val read = input.read(buf)
            if (read <= 0) break
            val allowed = minOf(read, MAX_FILE_BYTES - total)
            if (allowed > 0) {
                out.write(buf, 0, allowed)
                total += allowed
            }
            if (total >= MAX_FILE_BYTES) break
        }
        return out.toByteArray()
    }

    fun extractFromBytes(bytes: ByteArray): ExtractionResult {
        if (bytes.isEmpty()) {
            return ExtractionResult(text = "", isLikelyScannedPdf = false, fileType = "empty")
        }
        val headerWindow = String(bytes.copyOfRange(0, minOf(bytes.size, 1024)), Charsets.ISO_8859_1)
        if (headerWindow.contains("%PDF-")) {
            val extracted = extractTextFromPdfBytes(bytes)
            val cleaned = normalizeExtractedText(extracted)
            val scanned = cleaned.length < 30
            return ExtractionResult(
                text = cleaned,
                isLikelyScannedPdf = scanned,
                fileType = "pdf"
            )
        }
        val text = String(bytes, Charsets.UTF_8).removePrefix("\uFEFF")
        val trimmed = text.trim()
        val type = if (trimmed.startsWith("{") || trimmed.startsWith("[")) "json" else "text"
        return ExtractionResult(
            text = trimmed,
            isLikelyScannedPdf = false,
            fileType = type
        )
    }

    internal fun extractTextFromPdfBytes(bytes: ByteArray): String {
        val raw = String(bytes, Charsets.ISO_8859_1)
        val streams = extractPdfStreams(bytes, raw)
        val cmap = parseToUnicodeCmaps(streams)
        val output = StringBuilder()

        for (streamText in streams) {
            if (streamText.contains("beginbfchar") || streamText.contains("beginbfrange")) continue
            if (!streamText.contains("BT") && !streamText.contains("Tj") && !streamText.contains("TJ")) continue
            val blockText = extractTextFromContentStream(streamText, cmap)
            if (blockText.isNotBlank()) {
                output.append(blockText).append('\n')
            }
        }

        // Fallback for PDFs with uncompressed inline content outside standard stream wrappers
        if (output.length < 20 && raw.contains("BT") && (raw.contains("Tj") || raw.contains("TJ"))) {
            output.append(extractTextFromContentStream(raw, cmap))
        }
        return output.toString()
    }

    private fun extractPdfStreams(bytes: ByteArray, rawLatin1: String): List<String> {
        val result = mutableListOf<String>()
        var searchIdx = 0
        val len = rawLatin1.length
        while (searchIdx < len) {
            val streamPos = rawLatin1.indexOf("stream", searchIdx)
            if (streamPos < 0) break

            // Ensure "stream" is a standalone PDF keyword (not "endstream" or part of an identifier)
            val prevChar = if (streamPos > 0) rawLatin1[streamPos - 1] else '\n'
            val afterPos = streamPos + 6
            val nextChar = if (afterPos < len) rawLatin1[afterPos] else ' '
            val validStart = (prevChar.isWhitespace() || prevChar == '>') &&
                (nextChar == '\r' || nextChar == '\n')
            if (!validStart) {
                searchIdx = afterPos
                continue
            }

            val dictStart = maxOf(0, streamPos - 640)
            val dictWindow = rawLatin1.substring(dictStart, streamPos)

            var dataStart = afterPos
            if (dataStart < bytes.size && bytes[dataStart] == '\r'.code.toByte()) dataStart++
            if (dataStart < bytes.size && bytes[dataStart] == '\n'.code.toByte()) dataStart++

            val endStreamPos = rawLatin1.indexOf("endstream", dataStart)
            if (endStreamPos < 0) break

            // Skip binary non-text streams (images, embedded fonts, XRef tables, XML metadata)
            val isNonTextBinary = dictWindow.contains("/Subtype /Image") ||
                dictWindow.contains("/Subtype/Image") ||
                dictWindow.contains("/Type /XRef") ||
                dictWindow.contains("/Type/XRef") ||
                dictWindow.contains("/Subtype /Type1C") ||
                dictWindow.contains("/Subtype/Type1C") ||
                dictWindow.contains("/Subtype /CIDFontType0C") ||
                dictWindow.contains("/Subtype/CIDFontType0C") ||
                dictWindow.contains("/Subtype /OpenType") ||
                dictWindow.contains("/Subtype/OpenType") ||
                dictWindow.contains("/Subtype /XML") ||
                dictWindow.contains("/Subtype/XML") ||
                dictWindow.contains("/DCTDecode") ||
                dictWindow.contains("/JPXDecode") ||
                dictWindow.contains("/JBIG2Decode") ||
                dictWindow.contains("/CCITTFaxDecode")

            if (!isNonTextBinary) {
                var dataEnd = endStreamPos
                while (dataEnd > dataStart &&
                    (bytes[dataEnd - 1] == '\n'.code.toByte() || bytes[dataEnd - 1] == '\r'.code.toByte())
                ) {
                    dataEnd--
                }
                if (dataEnd > dataStart) {
                    var decoded: ByteArray? = bytes.copyOfRange(dataStart, dataEnd)
                    val lastDictIdx = dictWindow.lastIndexOf("<<")
                    val objDict = if (lastDictIdx >= 0) dictWindow.substring(lastDictIdx) else dictWindow

                    if (objDict.contains("/ASCII85Decode") || objDict.contains("/A85")) {
                        decoded = decoded?.let { decodeAscii85(it) }
                    } else if (objDict.contains("/ASCIIHexDecode") || objDict.contains("/AHx")) {
                        decoded = decoded?.let { decodeAsciiHex(it) }
                    }
                    if (objDict.contains("/FlateDecode") || objDict.contains("/Fl")) {
                        decoded = decoded?.let { inflateZlib(it) }
                    }

                    if (decoded != null && decoded.isNotEmpty()) {
                        result.add(String(decoded, Charsets.ISO_8859_1))
                    }
                }
            }
            searchIdx = endStreamPos + 9
        }
        return result
    }

    private fun decodeAsciiHex(input: ByteArray): ByteArray {
        val hex = String(input, Charsets.ISO_8859_1)
            .substringBefore('>')
            .filter { it in '0'..'9' || it in 'A'..'F' || it in 'a'..'f' }
        val clean = if (hex.length % 2 == 1) "${hex}0" else hex
        val out = ByteArrayOutputStream(clean.length / 2)
        var i = 0
        while (i + 2 <= clean.length) {
            val b = clean.substring(i, i + 2).toIntOrNull(16) ?: 0
            out.write(b)
            i += 2
        }
        return out.toByteArray()
    }

    private fun decodeAscii85(input: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        val group = IntArray(5)
        var count = 0
        var i = 0
        while (i < input.size) {
            val ch = input[i].toInt() and 0xFF
            i++
            if (ch == '~'.code) break
            if (ch <= 32) continue
            if (ch == 'z'.code && count == 0) {
                out.write(0); out.write(0); out.write(0); out.write(0)
                continue
            }
            if (ch < 33 || ch > 117) continue
            group[count++] = ch - 33
            if (count == 5) {
                var value = 0L
                for (k in 0..4) value = value * 85L + group[k]
                out.write(((value shr 24) and 0xFF).toInt())
                out.write(((value shr 16) and 0xFF).toInt())
                out.write(((value shr 8) and 0xFF).toInt())
                out.write((value and 0xFF).toInt())
                count = 0
            }
        }
        if (count > 1) {
            for (k in count..4) group[k] = 84
            var value = 0L
            for (k in 0..4) value = value * 85L + group[k]
            for (k in 0 until count - 1) {
                out.write(((value shr (24 - k * 8)) and 0xFF).toInt())
            }
        }
        return out.toByteArray()
    }

    private fun inflateZlib(compressed: ByteArray): ByteArray? {
        return inflateAttempt(compressed, nowrap = false)
            ?: inflateAttempt(compressed, nowrap = true)
    }

    private fun inflateAttempt(compressed: ByteArray, nowrap: Boolean): ByteArray? {
        val inflater = Inflater(nowrap)
        return try {
            inflater.setInput(compressed)
            val out = ByteArrayOutputStream(minOf(compressed.size * 3, 2 * 1024 * 1024))
            val buffer = ByteArray(8192)
            var guard = 0
            while (!inflater.finished() && guard < 4000) {
                val count = inflater.inflate(buffer)
                if (count > 0) {
                    out.write(buffer, 0, count)
                } else if (inflater.needsInput() || inflater.needsDictionary()) {
                    break
                }
                guard++
            }
            out.toByteArray().takeIf { it.isNotEmpty() }
        } catch (_: Exception) {
            null
        } finally {
            inflater.end()
        }
    }

    private fun parseToUnicodeCmaps(streams: List<String>): Map<String, String> {
        val map = HashMap<String, String>()
        val bfCharRegex = Regex("<([0-9A-Fa-f]+)>\\s*<([0-9A-Fa-f]+)>")
        val bfRangeScalarRegex = Regex("<([0-9A-Fa-f]+)>\\s*<([0-9A-Fa-f]+)>\\s*<([0-9A-Fa-f]+)>")
        val bfRangeArrayRegex = Regex("<([0-9A-Fa-f]+)>\\s*<([0-9A-Fa-f]+)>\\s*\\[([\\s\\S]*?)]")
        val hexTokenRegex = Regex("<([0-9A-Fa-f]+)>")

        for (stream in streams) {
            if (!stream.contains("beginbfchar") && !stream.contains("beginbfrange")) continue

            for (block in Regex("beginbfchar([\\s\\S]*?)endbfchar").findAll(stream)) {
                for (m in bfCharRegex.findAll(block.groupValues[1])) {
                    val src = m.groupValues[1].uppercase()
                    val dst = decodeUtf16BeHex(m.groupValues[2])
                    if (dst.isNotEmpty()) map[src] = dst
                }
            }

            for (block in Regex("beginbfrange([\\s\\S]*?)endbfrange").findAll(stream)) {
                val body = block.groupValues[1]
                // 1) Array form: <start> <end> [<u1> <u2> ...]
                for (m in bfRangeArrayRegex.findAll(body)) {
                    val startHex = m.groupValues[1].uppercase()
                    val endHex = m.groupValues[2].uppercase()
                    val start = startHex.toIntOrNull(16) ?: continue
                    val end = endHex.toIntOrNull(16) ?: continue
                    if (end - start !in 0..2048) continue
                    val targets = hexTokenRegex.findAll(m.groupValues[3]).map { it.groupValues[1] }.toList()
                    for (offset in 0..minOf(end - start, targets.lastIndex)) {
                        val key = (start + offset).toString(16).uppercase().padStart(startHex.length, '0')
                        val dst = decodeUtf16BeHex(targets[offset])
                        if (dst.isNotEmpty()) map[key] = dst
                    }
                }
                // 2) Scalar increment form: <start> <end> <dstStart>
                for (m in bfRangeScalarRegex.findAll(body)) {
                    val startHex = m.groupValues[1].uppercase()
                    val endHex = m.groupValues[2].uppercase()
                    val dstHex = m.groupValues[3].uppercase()
                    val start = startHex.toIntOrNull(16) ?: continue
                    val end = endHex.toIntOrNull(16) ?: continue
                    val dstStart = dstHex.toIntOrNull(16) ?: continue
                    if (end - start in 0..2048) {
                        for (code in start..end) {
                            val key = code.toString(16).uppercase().padStart(startHex.length, '0')
                            val cp = dstStart + (code - start)
                            if (cp in 1..0xFFFF) {
                                map[key] = cp.toChar().toString()
                            }
                        }
                    }
                }
            }
        }
        return map
    }

    /**
     * Proper lexical tokenizer for PDF content streams.
     * Tracks BT..ET text objects, literal/hex strings, TJ arrays, and numeric operands so
     * words like "DIABETES", "CATHETER", "FETAL", or "BETWEEN" inside strings can NEVER
     * prematurely end a text block, and horizontal `tx 0 Td` / same-Y `Tm` moves keep
     * option labels like `c) Recent and past memory loss` on the same line.
     */
    private fun extractTextFromContentStream(content: String, cmap: Map<String, String>): String {
        val out = StringBuilder()
        val n = content.length
        var i = 0
        var inText = false
        var curX = 0.0
        var curY = 0.0
        var hasPos = false
        var leading = 14.0

        val numStack = ArrayList<Double>(8)
        var pendingText: String? = null
        var pendingIsArray = false

        fun appendLineBreak() {
            if (out.isNotEmpty() && out.last() != '\n') {
                out.append('\n')
            }
        }

        fun appendSpaceIfNeeded() {
            if (out.isNotEmpty() && !out.last().isWhitespace()) {
                out.append(' ')
            }
        }

        while (i < n) {
            val ch = content[i]
            when {
                ch.isWhitespace() -> i++
                ch == '%' -> {
                    // PDF comment until end of line
                    while (i < n && content[i] != '\n' && content[i] != '\r') i++
                }
                ch == '(' -> {
                    val (decoded, nextIdx) = readLiteralString(content, i)
                    i = nextIdx
                    if (inText) {
                        pendingText = decoded
                        pendingIsArray = false
                    }
                }
                ch == '<' -> {
                    if (i + 1 < n && content[i + 1] == '<') {
                        i += 2
                        numStack.clear()
                    } else {
                        val end = content.indexOf('>', i + 1)
                        if (end > i) {
                            val hex = content.substring(i + 1, end)
                            i = end + 1
                            if (inText) {
                                pendingText = decodeHexString(hex, cmap)
                                pendingIsArray = false
                            }
                        } else {
                            i++
                        }
                    }
                }
                ch == '>' -> {
                    i += if (i + 1 < n && content[i + 1] == '>') 2 else 1
                }
                ch == '[' -> {
                    val (arrayText, nextIdx) = readTextArray(content, i, cmap, inText)
                    i = nextIdx
                    if (inText) {
                        pendingText = arrayText
                        pendingIsArray = true
                    }
                }
                ch == '/' -> {
                    // PDF name token (/F1, /TT0, etc.)
                    i++
                    while (i < n && !isDelimiterOrSpace(content[i])) i++
                }
                ch == '-' || ch == '+' || ch == '.' || ch in '0'..'9' -> {
                    val start = i
                    i++
                    while (i < n && (content[i] in '0'..'9' || content[i] == '.')) i++
                    val num = content.substring(start, i).toDoubleOrNull()
                    if (num != null) {
                        if (numStack.size >= 12) numStack.removeAt(0)
                        numStack.add(num)
                    }
                }
                else -> {
                    val start = i
                    while (i < n && !isDelimiterOrSpace(content[i])) i++
                    val op = content.substring(start, i)
                    when (op) {
                        "BT" -> {
                            inText = true
                            curX = 0.0
                            curY = 0.0
                            hasPos = false
                            pendingText = null
                            numStack.clear()
                        }
                        "ET" -> {
                            if (inText) {
                                appendLineBreak()
                            }
                            inText = false
                            pendingText = null
                            numStack.clear()
                        }
                        "Td", "TD" -> {
                            if (inText && numStack.size >= 2) {
                                val tx = numStack[numStack.size - 2]
                                val ty = numStack[numStack.size - 1]
                                if (op == "TD") leading = -ty
                                if (abs(ty) > 1.5) {
                                    appendLineBreak()
                                } else if (tx > 2.0) {
                                    appendSpaceIfNeeded()
                                }
                                curX += tx
                                curY += ty
                                hasPos = true
                            }
                            numStack.clear()
                        }
                        "Tm" -> {
                            if (inText && numStack.size >= 6) {
                                val e = numStack[numStack.size - 2]
                                val f = numStack[numStack.size - 1]
                                if (hasPos) {
                                    if (abs(f - curY) > 1.5) {
                                        appendLineBreak()
                                    } else if (e - curX > 2.0) {
                                        appendSpaceIfNeeded()
                                    }
                                }
                                curX = e
                                curY = f
                                hasPos = true
                            }
                            numStack.clear()
                        }
                        "T*" -> {
                            if (inText) {
                                appendLineBreak()
                                curY -= leading
                            }
                            numStack.clear()
                        }
                        "TL" -> {
                            if (numStack.isNotEmpty()) {
                                leading = numStack.last()
                            }
                            numStack.clear()
                        }
                        "Tj" -> {
                            if (inText && pendingText != null && !pendingIsArray) {
                                out.append(pendingText)
                            }
                            pendingText = null
                            numStack.clear()
                        }
                        "'", "\"" -> {
                            if (inText) {
                                appendLineBreak()
                                if (pendingText != null && !pendingIsArray) {
                                    out.append(pendingText)
                                }
                            }
                            pendingText = null
                            numStack.clear()
                        }
                        "TJ" -> {
                            if (inText && pendingText != null) {
                                out.append(pendingText)
                            }
                            pendingText = null
                            numStack.clear()
                        }
                        else -> {
                            numStack.clear()
                        }
                    }
                }
            }
        }
        return out.toString()
    }

    private fun readTextArray(
        content: String,
        startIdx: Int,
        cmap: Map<String, String>,
        decode: Boolean
    ): Pair<String, Int> {
        val sb = StringBuilder()
        var i = startIdx + 1
        val n = content.length
        while (i < n && content[i] != ']') {
            when (content[i]) {
                '(' -> {
                    val (decoded, nextIdx) = readLiteralString(content, i)
                    if (decode) sb.append(decoded)
                    i = nextIdx
                }
                '<' -> {
                    if (i + 1 < n && content[i + 1] == '<') {
                        i += 2
                    } else {
                        val end = content.indexOf('>', i + 1)
                        if (end > i) {
                            if (decode) {
                                sb.append(decodeHexString(content.substring(i + 1, end), cmap))
                            }
                            i = end + 1
                        } else {
                            i++
                        }
                    }
                }
                '-', '+', '.', in '0'..'9' -> {
                    val startNum = i
                    i++
                    while (i < n && (content[i] in '0'..'9' || content[i] == '.')) i++
                    if (decode) {
                        val num = content.substring(startNum, i).toDoubleOrNull() ?: 0.0
                        // Negative kerning values in PDF TJ arrays represent inter-word spaces
                        if (num < -130.0 && sb.isNotEmpty() && !sb.last().isWhitespace()) {
                            sb.append(' ')
                        }
                    }
                }
                else -> i++
            }
        }
        if (i < n && content[i] == ']') i++
        return sb.toString() to i
    }

    private fun isDelimiterOrSpace(c: Char): Boolean {
        return c.isWhitespace() ||
            c == '(' || c == ')' || c == '<' || c == '>' ||
            c == '[' || c == ']' || c == '{' || c == '}' ||
            c == '/' || c == '%'
    }

    private fun readLiteralString(s: String, startIndex: Int): Pair<String, Int> {
        val sb = StringBuilder()
        var i = startIndex + 1
        var depth = 1
        val n = s.length
        while (i < n && depth > 0) {
            val c = s[i]
            if (c == '\\' && i + 1 < n) {
                val next = s[i + 1]
                when (next) {
                    'n' -> { sb.append('\n'); i += 2 }
                    'r' -> { sb.append('\r'); i += 2 }
                    't' -> { sb.append('\t'); i += 2 }
                    'b' -> { sb.append('\b'); i += 2 }
                    'f' -> { sb.append(' '); i += 2 }
                    '(', ')', '\\' -> { sb.append(next); i += 2 }
                    '\r' -> {
                        i += if (i + 2 < n && s[i + 2] == '\n') 3 else 2
                    }
                    '\n' -> { i += 2 }
                    in '0'..'7' -> {
                        var octEnd = i + 2
                        while (octEnd < n && octEnd < i + 4 && s[octEnd] in '0'..'7') octEnd++
                        val code = s.substring(i + 1, octEnd).toIntOrNull(8)
                        if (code != null) sb.append(mapWinAnsiByte(code))
                        i = octEnd
                    }
                    else -> { sb.append(next); i += 2 }
                }
            } else if (c == '(') {
                depth++
                sb.append(c)
                i++
            } else if (c == ')') {
                depth--
                if (depth > 0) sb.append(c)
                i++
            } else {
                val code = c.code and 0xFF
                sb.append(mapWinAnsiByte(code))
                i++
            }
        }
        return sb.toString() to i
    }

    private fun mapWinAnsiByte(code: Int): Char = when (code) {
        0x80 -> '€'
        0x82 -> '‚'
        0x84 -> '„'
        0x85 -> '…'
        0x88 -> '^'
        0x8B -> '‹'
        0x91, 0x92 -> '\''
        0x93, 0x94 -> '"'
        0x95 -> '•'
        0x96 -> '–'
        0x97 -> '—'
        0x98 -> '~'
        0x9B -> '›'
        0xA0 -> ' '
        else -> code.toChar()
    }

    private fun decodeHexString(rawHex: String, cmap: Map<String, String>): String {
        val hex = rawHex.filter { it in '0'..'9' || it in 'A'..'F' || it in 'a'..'f' }.uppercase()
        if (hex.isEmpty()) return ""
        if (cmap.isNotEmpty()) {
            val has4 = cmap.keys.any { it.length == 4 }
            val has2 = cmap.keys.any { it.length == 2 }
            val step = when {
                has4 && hex.length % 4 == 0 -> 4
                has2 && hex.length % 2 == 0 -> 2
                has4 -> 4
                else -> 2
            }
            val sb = StringBuilder()
            var hits = 0
            var total = 0
            var i = 0
            while (i + step <= hex.length) {
                total++
                val token = hex.substring(i, i + step)
                val mapped = cmap[token]
                if (mapped != null) {
                    sb.append(mapped)
                    hits++
                } else {
                    val code = token.toIntOrNull(16)
                    if (code != null && (code in 32..126 || code in 160..255)) {
                        sb.append(code.toChar())
                    }
                }
                i += step
            }
            if (hits > 0 && sb.isNotEmpty()) return sb.toString()
        }
        return hexToUnicode(hex)
    }

    /** Decodes a ToUnicode CMap target hex value (always UTF-16BE when >= 4 hex digits). */
    private fun decodeUtf16BeHex(rawHex: String): String {
        val clean = rawHex.uppercase()
        if (clean.length <= 2) {
            val cp = clean.toIntOrNull(16) ?: return ""
            return if (cp > 0) cp.toChar().toString() else ""
        }
        val padded = if (clean.length % 4 != 0) clean.padStart(((clean.length / 4) + 1) * 4, '0') else clean
        val sb = StringBuilder()
        var i = 0
        while (i + 4 <= padded.length) {
            val cp = padded.substring(i, i + 4).toIntOrNull(16) ?: 0
            if (cp > 0) sb.append(cp.toChar())
            i += 4
        }
        return sb.toString()
    }

    private fun hexToUnicode(hex: String): String {
        val clean = if (hex.length % 2 == 1) "${hex}0" else hex
        // UTF-16BE BOM check
        if (clean.startsWith("FEFF") && clean.length >= 8) {
            return decodeUtf16BeHex(clean.substring(4))
        }
        // Heuristic: UTF-16BE without BOM (every high byte is 00 for Latin text)
        if (clean.length >= 4 && clean.length % 4 == 0) {
            var zeroHighBytes = 0
            var pairs = 0
            var idx = 0
            while (idx + 4 <= clean.length) {
                pairs++
                if (clean[idx] == '0' && clean[idx + 1] == '0') zeroHighBytes++
                idx += 4
            }
            if (pairs > 0 && zeroHighBytes * 2 >= pairs) {
                return decodeUtf16BeHex(clean)
            }
        }
        val sb = StringBuilder()
        var i = 0
        while (i + 2 <= clean.length) {
            val code = clean.substring(i, i + 2).toIntOrNull(16) ?: 32
            if (code == 10 || code == 13 || code >= 32) {
                sb.append(mapWinAnsiByte(code))
            }
            i += 2
        }
        return sb.toString()
    }

    private fun normalizeExtractedText(raw: String): String {
        return raw
            .replace("\u00AD", "") // soft hyphen
            .replace("\uFEFF", "")
            .replace("\u00A0", " ")
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .replace(Regex("[ \\t]+"), " ")
            .replace(Regex("\n{3,}"), "\n\n")
            .lines()
            .joinToString("\n") { it.trim() }
            .trim()
    }
}
