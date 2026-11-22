package com.example.data.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

enum class AppUpdateStatus {
    IDLE,
    CHECKING,
    UP_TO_DATE,
    UPDATE_AVAILABLE,
    ERROR
}

data class AppUpdateUiState(
    val status: AppUpdateStatus = AppUpdateStatus.IDLE,
    val currentVersion: String = "",
    val latestVersion: String = "",
    val releaseTitle: String = "",
    val releaseNotes: String = "",
    val downloadUrl: String = "",
    val releasePageUrl: String = "",
    val statusMessage: String = "Tap to check for new versions",
    val showDialog: Boolean = false
)

object AppUpdateVersionLogic {
    const val GITHUB_RELEASES_API_URL =
        "https://api.github.com/repos/ctkjaz/Muhammed-Jaz-/releases/latest"
    const val GITHUB_RELEASES_WEB_URL =
        "https://github.com/ctkjaz/Muhammed-Jaz-/releases"

    fun normalizeVersionParts(rawVersion: String): List<Int> {
        val cleaned = rawVersion.trim()
            .removePrefix("v")
            .removePrefix("V")
            .substringBefore("-")
            .substringBefore("+")
        val parts = cleaned.split(".")
            .mapNotNull { token ->
                val digits = token.takeWhile { it.isDigit() }
                digits.toIntOrNull()
            }
        return if (parts.isEmpty()) listOf(0) else parts
    }

    fun isNewerVersion(remoteVersion: String, currentVersion: String): Boolean {
        val remote = normalizeVersionParts(remoteVersion)
        val current = normalizeVersionParts(currentVersion)
        val maxLen = maxOf(remote.size, current.size)
        for (i in 0 until maxLen) {
            val r = remote.getOrElse(i) { 0 }
            val c = current.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }

    fun parseGitHubReleaseJson(
        jsonText: String,
        currentVersion: String
    ): AppUpdateUiState {
        val root = JSONObject(jsonText)
        val tagName = root.optString("tag_name", "").trim()
        val releaseName = root.optString("name", "").trim().ifBlank { tagName }
        val body = root.optString("body", "").trim()
        val htmlUrl = root.optString("html_url", "").trim().ifBlank { GITHUB_RELEASES_WEB_URL }

        var apkDownloadUrl = ""
        val assets = root.optJSONArray("assets")
        if (assets != null) {
            for (i in 0 until assets.length()) {
                val asset = assets.optJSONObject(i) ?: continue
                val assetName = asset.optString("name", "")
                val url = asset.optString("browser_download_url", "").trim()
                if (assetName.endsWith(".apk", ignoreCase = true) && url.startsWith("https://")) {
                    apkDownloadUrl = url
                    break
                }
            }
        }

        val effectiveDownloadUrl = apkDownloadUrl.ifBlank { htmlUrl }
        val cleanRemoteLabel = tagName.removePrefix("v").removePrefix("V").ifBlank { releaseName }

        return if (tagName.isNotBlank() && isNewerVersion(tagName, currentVersion)) {
            AppUpdateUiState(
                status = AppUpdateStatus.UPDATE_AVAILABLE,
                currentVersion = currentVersion,
                latestVersion = cleanRemoteLabel,
                releaseTitle = releaseName.ifBlank { "ClinEdge v$cleanRemoteLabel" },
                releaseNotes = body.ifBlank { "A newer version of ClinEdge is ready to download." },
                downloadUrl = effectiveDownloadUrl,
                releasePageUrl = htmlUrl,
                statusMessage = "Update available: v$cleanRemoteLabel",
                showDialog = true
            )
        } else {
            AppUpdateUiState(
                status = AppUpdateStatus.UP_TO_DATE,
                currentVersion = currentVersion,
                latestVersion = cleanRemoteLabel.ifBlank { currentVersion },
                releaseTitle = releaseName,
                releaseNotes = body,
                downloadUrl = effectiveDownloadUrl,
                releasePageUrl = htmlUrl,
                statusMessage = "You're up to date (v$currentVersion)",
                showDialog = false
            )
        }
    }
}

object AppUpdateChecker {
    suspend fun checkLatestRelease(currentVersion: String): AppUpdateUiState = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(AppUpdateVersionLogic.GITHUB_RELEASES_API_URL)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6_000
                readTimeout = 6_000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "ClinEdge-Android/$currentVersion")
            }

            val code = connection.responseCode
            when (code) {
                HttpURLConnection.HTTP_OK -> {
                    val body = connection.inputStream.bufferedReader().use { it.readText() }
                    AppUpdateVersionLogic.parseGitHubReleaseJson(body, currentVersion)
                }
                HttpURLConnection.HTTP_NOT_FOUND -> {
                    AppUpdateUiState(
                        status = AppUpdateStatus.UP_TO_DATE,
                        currentVersion = currentVersion,
                        latestVersion = currentVersion,
                        releasePageUrl = AppUpdateVersionLogic.GITHUB_RELEASES_WEB_URL,
                        statusMessage = "You're on the latest version (v$currentVersion)",
                        showDialog = false
                    )
                }
                else -> {
                    AppUpdateUiState(
                        status = AppUpdateStatus.ERROR,
                        currentVersion = currentVersion,
                        releasePageUrl = AppUpdateVersionLogic.GITHUB_RELEASES_WEB_URL,
                        statusMessage = "Update server returned HTTP $code. Tap to retry.",
                        showDialog = false
                    )
                }
            }
        } catch (_: Exception) {
            AppUpdateUiState(
                status = AppUpdateStatus.ERROR,
                currentVersion = currentVersion,
                releasePageUrl = AppUpdateVersionLogic.GITHUB_RELEASES_WEB_URL,
                statusMessage = "Could not check for updates right now. Check connection.",
                showDialog = false
            )
        } finally {
            connection?.disconnect()
        }
    }
}
