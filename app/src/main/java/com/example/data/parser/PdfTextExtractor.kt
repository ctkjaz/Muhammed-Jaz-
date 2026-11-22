package com.example.data.parser

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.zip.Inflater

/**
 * Pure-Kotlin on-device text extractor for Admin Content Push.
 * Supports:
 *  - Plain text (.txt) and JSON (.json) files
 *  - Standard text-based PDFs (uncompressed and FlateDecode/zlib compressed content streams,
 *    Tj / TJ / ' text operators, literal and hex strings, and basic ToUnicode CMap tables).
 * Note: Scanned image-only PDFs without an embedded text layer return an empty/short extraction
 * and prompt the admin to paste OCR text into the Paste tab.
 */
object PdfTextExtractor {

    data class ExtractionResult(
        val text: String,
        val isLikelyScannedPdf: Boolean = false,
        val fileType: String = "text"
    )

    suspend fun extractFromUri(context: Context, uri: Uri): Result<ExtractionResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw IllegalStateException("Could not open selected file.")
                extractFromBytes(bytes)
            }
        }

    fun extractFromBytes(bytes: ByteArray): ExtractionResult {
        if (bytes.isEmpty()) {
            return ExtractionResult(text = "", isLikelyScannedPdf = false, fileType = "empty")
        }
        val header = String(bytes.copyOfRange(0, minOf(bytes.size, 8)), Charsets.ISO_8859_1)
        if (header.startsWith("%PDF")) {
            val extracted = extractTextFromPdfBytes(bytes)
            val cleaned = normalizeExtractedText(extracted)
            val scanned = cleaned.length < 30
            return ExtractionResult(
                text = cleaned,
                isLikelyScannedPdf = scanned,
                fileType = "pdf"
            )
        }
        val text = String(bytes, Charsets.UTF_8)
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
            if (!streamText.contains("BT") || !streamText.contains("ET")) continue
            val blockText = extractTextBlocks(streamText, cmap)
            if (blockText.isNotBlank()) {
                output.append(blockText).append('\n')
            }
        }

        // Fallback for PDFs with uncompressed inline text outside standard stream wrappers
        if (output.length < 20 && raw.contains("BT") && raw.contains("ET")) {
            output.append(extractTextBlocks(raw, cmap))
        }
        return output.toString()
    }

    private fun extractPdfStreams(bytes: ByteArray, rawLatin1: String): List<String> {
        val result = mutableListOf<String>()
        var searchIdx = 0
        while (searchIdx < rawLatin1.length) {
            val streamPos = rawLatin1.indexOf("stream", searchIdx)
            if (streamPos < 0) break

            val dictStart = maxOf(0, streamPos - 512)
            val dictWindow = rawLatin1.substring(dictStart, streamPos)
            val isFlate = dictWindow.contains("/FlateDecode")

            var dataStart = streamPos + 6
            if (dataStart < bytes.size && bytes[dataStart] == '\r'.code.toByte()) dataStart++
            if (dataStart < bytes.size && bytes[dataStart] == '\n'.code.toByte()) dataStart++

            val endStreamPos = rawLatin1.indexOf("endstream", dataStart)
            if (endStreamPos < 0) break

            var dataEnd = endStreamPos
            while (dataEnd > dataStart && (bytes[dataEnd - 1] == '\n'.code.toByte() || bytes[dataEnd - 1] == '\r'.code.toByte())) {
                dataEnd--
            }

            if (dataEnd > dataStart) {
                val slice = bytes.copyOfRange(dataStart, dataEnd)
                val decodedBytes = if (isFlate) inflateZlib(slice) else slice
                if (decodedBytes != null && decodedBytes.isNotEmpty()) {
                    result.add(String(decodedBytes, Charsets.ISO_8859_1))
                }
            }
            searchIdx = endStreamPos + 9
        }
        return result
    }

    private fun inflateZlib(compressed: ByteArray): ByteArray? {
        return inflateAttempt(compressed, nowrap = false)
            ?: inflateAttempt(compressed, nowrap = true)
    }

    private fun inflateAttempt(compressed: ByteArray, nowrap: Boolean): ByteArray? {
        val inflater = Inflater(nowrap)
        return try {
            inflater.setInput(compressed)
            val out = ByteArrayOutputStream(compressed.size * 2)
            val buffer = ByteArray(4096)
            var guard = 0
            while (!inflater.finished() && guard < 2000) {
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
        val map = mutableMapOf<String, String>()
        val bfCharRegex = Regex("<([0-9A-Fa-f]+)>\\s*<([0-9A-Fa-f]+)>")
        val bfRangeRegex = Regex("<([0-9A-Fa-f]+)>\\s*<([0-9A-Fa-f]+)>\\s*<([0-9A-Fa-f]+)>")

        for (stream in streams) {
            if (!stream.contains("beginbfchar") && !stream.contains("beginbfrange")) continue

            val bfCharBlocks = Regex("beginbfchar([\\s\\S]*?)endbfchar").findAll(stream)
            for (block in bfCharBlocks) {
                for (m in bfCharRegex.findAll(block.groupValues[1])) {
                    val src = m.groupValues[1].uppercase()
                    val dst = hexToUnicode(m.groupValues[2])
                    if (dst.isNotEmpty()) map[src] = dst
                }
            }

            val bfRangeBlocks = Regex("beginbfrange([\\s\\S]*?)endbfrange").findAll(stream)
            for (block in bfRangeBlocks) {
                for (m in bfRangeRegex.findAll(block.groupValues[1])) {
                    val startHex = m.groupValues[1].uppercase()
                    val endHex = m.groupValues[2].uppercase()
                    val dstHex = m.groupValues[3].uppercase()
                    val start = startHex.toIntOrNull(16) ?: continue
                    val end = endHex.toIntOrNull(16) ?: continue
                    val dstStart = dstHex.toIntOrNull(16) ?: continue
                    if (end - start in 0..256) {
                        for (code in start..end) {
                            val key = code.toString(16).uppercase().padStart(startHex.length, '0')
                            val ch = (dstStart + (code - start)).toChar().toString()
                            map[key] = ch
                        }
                    }
                }
            }
        }
        return map
    }

    private fun extractTextBlocks(content: String, cmap: Map<String, String>): String {
        val sb = StringBuilder()
        var idx = 0
        while (idx < content.length) {
            val bt = content.indexOf("BT", idx)
            if (bt < 0) break
            val et = content.indexOf("ET", bt + 2)
            if (et < 0) break
            val block = content.substring(bt + 2, et)
            parseOperatorsInBlock(block, cmap, sb)
            sb.append('\n')
            idx = et + 2
        }
        return sb.toString()
    }

    private fun parseOperatorsInBlock(block: String, cmap: Map<String, String>, out: StringBuilder) {
        var i = 0
        val n = block.length
        while (i < n) {
            val ch = block[i]
            when (ch) {
                '(' -> {
                    val (decoded, nextIdx) = readLiteralString(block, i)
                    i = nextIdx
                    val op = peekNextToken(block, i)
                    if (op == "Tj" || op == "'" || op == "\"") {
                        out.append(decoded)
                        if (op == "'" || op == "\"") out.append('\n')
                    }
                }
                '<' -> {
                    if (i + 1 < n && block[i + 1] == '<') {
                        i += 2
                    } else {
                        val end = block.indexOf('>', i + 1)
                        if (end > i) {
                            val hex = block.substring(i + 1, end)
                            i = end + 1
                            val op = peekNextToken(block, i)
                            if (op == "Tj" || op == "'" || op == "\"") {
                                out.append(decodeHexString(hex, cmap))
                                if (op == "'" || op == "\"") out.append('\n')
                            }
                        } else {
                            i++
                        }
                    }
                }
                '[' -> {
                    val arraySb = StringBuilder()
                    i++
                    while (i < n && block[i] != ']') {
                        when (block[i]) {
                            '(' -> {
                                val (decoded, nextIdx) = readLiteralString(block, i)
                                arraySb.append(decoded)
                                i = nextIdx
                            }
                            '<' -> {
                                val end = block.indexOf('>', i + 1)
                                if (end > i) {
                                    arraySb.append(decodeHexString(block.substring(i + 1, end), cmap))
                                    i = end + 1
                                } else {
                                    i++
                                }
                            }
                            '-', in '0'..'9' -> {
                                val startNum = i
                                if (block[i] == '-') i++
                                while (i < n && (block[i].isDigit() || block[i] == '.')) i++
                                val num = block.substring(startNum, i).toDoubleOrNull() ?: 0.0
                                // Large negative kerning values in PDF TJ arrays represent word spaces
                                if (num < -160.0 && arraySb.isNotEmpty() && !arraySb.last().isWhitespace()) {
                                    arraySb.append(' ')
                                }
                            }
                            else -> i++
                        }
                    }
                    if (i < n && block[i] == ']') i++
                    val op = peekNextToken(block, i)
                    if (op == "TJ") {
                        out.append(arraySb)
                    }
                }
                'T' -> {
                    if (i + 1 < n && (block[i + 1] == '*' || block[i + 1] == 'd' || block[i + 1] == 'D')) {
                        if (out.isNotEmpty() && out.last() != '\n') {
                            out.append('\n')
                        }
                        i += 2
                    } else {
                        i++
                    }
                }
                else -> i++
            }
        }
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
                    '\r', '\n' -> { i += 2 }
                    in '0'..'7' -> {
                        var octEnd = i + 2
                        while (octEnd < n && octEnd < i + 4 && s[octEnd] in '0'..'7') octEnd++
                        val code = s.substring(i + 1, octEnd).toIntOrNull(8)
                        if (code != null) sb.append(code.toChar())
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
                sb.append(c)
                i++
            }
        }
        return sb.toString() to i
    }

    private fun peekNextToken(s: String, fromIndex: Int): String {
        var i = fromIndex
        val n = s.length
        while (i < n && s[i].isWhitespace()) i++
        if (i >= n) return ""
        val start = i
        while (i < n && !s[i].isWhitespace() && s[i] !in charArrayOf('(', ')', '<', '>', '[', ']')) {
            i++
        }
        return s.substring(start, i)
    }

    private fun decodeHexString(rawHex: String, cmap: Map<String, String>): String {
        val hex = rawHex.filter { it in '0'..'9' || it in 'A'..'F' || it in 'a'..'f' }.uppercase()
        if (hex.isEmpty()) return ""
        if (cmap.isNotEmpty()) {
            val sb = StringBuilder()
            val step = if (hex.length % 4 == 0 && cmap.keys.any { it.length == 4 }) 4 else 2
            var i = 0
            while (i + step <= hex.length) {
                val token = hex.substring(i, i + step)
                val mapped = cmap[token]
                if (mapped != null) {
                    sb.append(mapped)
                } else {
                    val code = token.toIntOrNull(16)
                    if (code != null && code in 32..126) sb.append(code.toChar())
                }
                i += step
            }
            if (sb.isNotEmpty()) return sb.toString()
        }
        return hexToUnicode(hex)
    }

    private fun hexToUnicode(hex: String): String {
        val clean = if (hex.length % 2 == 1) "${hex}0" else hex
        // UTF-16BE BOM check
        if (clean.startsWith("FEFF") && clean.length >= 8) {
            val sb = StringBuilder()
            var i = 4
            while (i + 4 <= clean.length) {
                val code = clean.substring(i, i + 4).toIntOrNull(16) ?: 32
                sb.append(code.toChar())
                i += 4
            }
            return sb.toString()
        }
        val sb = StringBuilder()
        var i = 0
        while (i + 2 <= clean.length) {
            val code = clean.substring(i, i + 2).toIntOrNull(16) ?: 32
            if (code == 10 || code == 13 || code >= 32) {
                sb.append(code.toChar())
            }
            i += 2
        }
        return sb.toString()
    }

    private fun normalizeExtractedText(raw: String): String {
        return raw
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .replace(Regex("[ \\t]+"), " ")
            .replace(Regex("\n{3,}"), "\n\n")
            .lines()
            .joinToString("\n") { it.trim() }
            .trim()
    }
}
