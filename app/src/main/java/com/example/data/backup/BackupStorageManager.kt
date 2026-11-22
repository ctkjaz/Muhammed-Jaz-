package com.example.data.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupFolderSummary(
    val isCustomFolder: Boolean = false,
    val displayLabel: String = BackupStorageLogic.DEFAULT_FOLDER_LABEL,
    val backupCount: Int = 0,
    val latestBackupName: String? = null,
    val latestBackupTimestampMs: Long = 0L
)

data class SavedBackupResult(
    val fileName: String,
    val folderLabel: String,
    val usedCustomFolder: Boolean
)

data class LoadedBackupResult(
    val fileName: String,
    val content: String,
    val folderLabel: String
)

object BackupStorageLogic {
    const val DEFAULT_FOLDER_NAME = "ClinEdgeBackups"
    const val DEFAULT_FOLDER_LABEL = "Documents/ClinEdgeBackups (Default)"
    const val BACKUP_FILE_PREFIX = "clinedge-backup-"

    fun buildBackupFileName(timestampMs: Long = System.currentTimeMillis()): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd-HHmm", Locale.US)
        return "${BACKUP_FILE_PREFIX}${formatter.format(Date(timestampMs))}.json"
    }

    fun formatTreeUriDisplayLabel(treeUriString: String): String {
        if (treeUriString.isBlank()) return DEFAULT_FOLDER_LABEL
        val decoded = runCatching {
            java.net.URLDecoder.decode(treeUriString, Charsets.UTF_8.name())
        }.getOrDefault(treeUriString)
        val treePart = decoded.substringAfter("/tree/", missingDelimiterValue = decoded)
        val cleaned = when {
            treePart.startsWith("primary:", ignoreCase = true) ->
                treePart.removePrefix("primary:").trimStart('/')
            treePart.contains(":") ->
                treePart.substringAfter(":").trimStart('/')
            else -> treePart.trimStart('/')
        }
        return cleaned.ifBlank { "Selected Storage Folder" }
    }

    fun isBackupFileName(fileName: String): Boolean {
        val lower = fileName.lowercase(Locale.US)
        return lower.endsWith(".json") || lower.endsWith(".clinedge")
    }
}

class BackupStorageManager(private val context: Context) {

    fun getDefaultBackupDirectory(): File {
        val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
        val dir = File(baseDir, BackupStorageLogic.DEFAULT_FOLDER_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun hasPersistedTreeAccess(treeUriString: String): Boolean {
        if (treeUriString.isBlank()) return false
        val target = runCatching { Uri.parse(treeUriString) }.getOrNull() ?: return false
        return context.contentResolver.persistedUriPermissions.any { perm ->
            perm.uri == target && perm.isReadPermission && perm.isWritePermission
        }
    }

    fun persistTreePermission(uri: Uri): String {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, flags)
        }
        return BackupStorageLogic.formatTreeUriDisplayLabel(uri.toString())
    }

    fun releaseTreePermission(treeUriString: String) {
        if (treeUriString.isBlank()) return
        runCatching {
            val uri = Uri.parse(treeUriString)
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.releasePersistableUriPermission(uri, flags)
        }
    }

    suspend fun inspectActiveFolder(
        customTreeUri: String,
        customLabel: String
    ): BackupFolderSummary = withContext(Dispatchers.IO) {
        if (customTreeUri.isNotBlank() && hasPersistedTreeAccess(customTreeUri)) {
            val label = customLabel.ifBlank { BackupStorageLogic.formatTreeUriDisplayLabel(customTreeUri) }
            val entries = listSafBackupFiles(Uri.parse(customTreeUri))
            val latest = entries.maxByOrNull { it.lastModified }
            return@withContext BackupFolderSummary(
                isCustomFolder = true,
                displayLabel = label,
                backupCount = entries.size,
                latestBackupName = latest?.displayName,
                latestBackupTimestampMs = latest?.lastModified ?: 0L
            )
        }

        val dir = getDefaultBackupDirectory()
        val files = dir.listFiles()
            ?.filter { it.isFile && BackupStorageLogic.isBackupFileName(it.name) }
            .orEmpty()
        val latest = files.maxByOrNull { it.lastModified() }
        BackupFolderSummary(
            isCustomFolder = false,
            displayLabel = BackupStorageLogic.DEFAULT_FOLDER_LABEL,
            backupCount = files.size,
            latestBackupName = latest?.name,
            latestBackupTimestampMs = latest?.lastModified() ?: 0L
        )
    }

    suspend fun saveBackupToActiveFolder(
        backupContent: String,
        customTreeUri: String,
        customLabel: String
    ): Result<SavedBackupResult> = withContext(Dispatchers.IO) {
        runCatching {
            val fileName = BackupStorageLogic.buildBackupFileName()
            if (customTreeUri.isNotBlank() && hasPersistedTreeAccess(customTreeUri)) {
                val treeUri = Uri.parse(customTreeUri)
                val parentDocId = DocumentsContract.getTreeDocumentId(treeUri)
                val parentDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, parentDocId)
                val createdUri = DocumentsContract.createDocument(
                    context.contentResolver,
                    parentDocUri,
                    "application/json",
                    fileName
                ) ?: error("Could not create backup file in selected folder.")
                context.contentResolver.openOutputStream(createdUri)?.use { out ->
                    out.write(backupContent.toByteArray(Charsets.UTF_8))
                    out.flush()
                } ?: error("Could not open backup file for writing.")

                val label = customLabel.ifBlank { BackupStorageLogic.formatTreeUriDisplayLabel(customTreeUri) }
                return@runCatching SavedBackupResult(
                    fileName = fileName,
                    folderLabel = label,
                    usedCustomFolder = true
                )
            }

            val dir = getDefaultBackupDirectory()
            val target = File(dir, fileName)
            target.writeText(backupContent, Charsets.UTF_8)
            SavedBackupResult(
                fileName = fileName,
                folderLabel = BackupStorageLogic.DEFAULT_FOLDER_LABEL,
                usedCustomFolder = false
            )
        }
    }

    suspend fun readLatestBackupFromActiveFolder(
        customTreeUri: String,
        customLabel: String
    ): Result<LoadedBackupResult> = withContext(Dispatchers.IO) {
        runCatching {
            if (customTreeUri.isNotBlank() && hasPersistedTreeAccess(customTreeUri)) {
                val treeUri = Uri.parse(customTreeUri)
                val entries = listSafBackupFiles(treeUri)
                val latest = entries.maxByOrNull { it.lastModified }
                    ?: error("No backup files found in selected folder.")
                val content = context.contentResolver.openInputStream(latest.uri)
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    ?.takeIf { it.isNotBlank() }
                    ?: error("Selected backup file is empty.")
                val label = customLabel.ifBlank { BackupStorageLogic.formatTreeUriDisplayLabel(customTreeUri) }
                return@runCatching LoadedBackupResult(
                    fileName = latest.displayName,
                    content = content,
                    folderLabel = label
                )
            }

            val dir = getDefaultBackupDirectory()
            val latest = dir.listFiles()
                ?.filter { it.isFile && BackupStorageLogic.isBackupFileName(it.name) }
                ?.maxByOrNull { it.lastModified() }
                ?: error("No backup files found in default folder yet.")
            val content = latest.readText(Charsets.UTF_8).takeIf { it.isNotBlank() }
                ?: error("Backup file is empty.")
            LoadedBackupResult(
                fileName = latest.name,
                content = content,
                folderLabel = BackupStorageLogic.DEFAULT_FOLDER_LABEL
            )
        }
    }

    private data class SafBackupEntry(
        val uri: Uri,
        val displayName: String,
        val lastModified: Long
    )

    private fun listSafBackupFiles(treeUri: Uri): List<SafBackupEntry> {
        val result = mutableListOf<SafBackupEntry>()
        val parentDocId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull()
            ?: return emptyList()
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        )
        runCatching {
            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val modIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                val mimeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                while (cursor.moveToNext()) {
                    val docId = if (idIdx >= 0) cursor.getString(idIdx) else continue
                    val name = if (nameIdx >= 0) cursor.getString(nameIdx).orEmpty() else ""
                    val mime = if (mimeIdx >= 0) cursor.getString(mimeIdx).orEmpty() else ""
                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) continue
                    if (!BackupStorageLogic.isBackupFileName(name)) continue
                    val modified = if (modIdx >= 0) cursor.getLong(modIdx) else 0L
                    val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                    result.add(SafBackupEntry(uri = docUri, displayName = name, lastModified = modified))
                }
            }
        }
        return result
    }
}
