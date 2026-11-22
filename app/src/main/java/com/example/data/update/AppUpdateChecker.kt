package com.example.data.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
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
    const val GITHUB_ALL_RELEASES_API_URL =
        "https://api.github.com/repos/ctkjaz/Muhammed-Jaz-/releases?per_page=10"
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
        return parseReleaseObject(root, currentVersion)
    }

    fun parseGitHubReleasesListJson(
        jsonText: String,
        currentVersion: String
    ): AppUpdateUiState? {
        val array = JSONArray(jsonText)
        var bestObj: JSONObject? = null
        var bestTag = ""
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            if (item.optBoolean("draft", false)) continue
            val tag = item.optString("tag_name", "").trim()
            if (tag.isBlank()) continue
            if (bestObj == null || isNewerVersion(tag, bestTag)) {
                bestObj = item
                bestTag = tag
            }
        }
        return bestObj?.let { parseReleaseObject(it, currentVersion) }
    }

    private fun parseReleaseObject(
        root: JSONObject,
        currentVersion: String
    ): AppUpdateUiState {
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
                statusMessage = "Update available: v$cleanRemoteLabel — tap to download",
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
    private data class HttpFetchResult(val code: Int, val body: String)

    private fun fetchJson(urlString: String, currentVersion: String): HttpFetchResult {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8_000
                readTimeout = 8_000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
                setRequestProperty("User-Agent", "ClinEdge-Android/$currentVersion")
            }
            val code = connection.responseCode
            val body = if (code in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                ""
            }
            HttpFetchResult(code, body)
        } finally {
            connection?.disconnect()
        }
    }

    suspend fun checkLatestRelease(currentVersion: String): AppUpdateUiState = withContext(Dispatchers.IO) {
        try {
            // 1. Primary endpoint: /releases/latest
            val latestRes = fetchJson(AppUpdateVersionLogic.GITHUB_RELEASES_API_URL, currentVersion)
            if (latestRes.code == HttpURLConnection.HTTP_OK && latestRes.body.isNotBlank()) {
                return@withContext AppUpdateVersionLogic.parseGitHubReleaseJson(latestRes.body, currentVersion)
            }

            // 2. Fallback endpoint: /releases list (handles prereleases or newly tagged builds)
            if (latestRes.code == HttpURLConnection.HTTP_NOT_FOUND) {
                val listRes = fetchJson(AppUpdateVersionLogic.GITHUB_ALL_RELEASES_API_URL, currentVersion)
                if (listRes.code == HttpURLConnection.HTTP_OK && listRes.body.isNotBlank()) {
                    val parsed = AppUpdateVersionLogic.parseGitHubReleasesListJson(listRes.body, currentVersion)
                    if (parsed != null) {
                        return@withContext parsed
                    }
                }
                return@withContext AppUpdateUiState(
                    status = AppUpdateStatus.ERROR,
                    currentVersion = currentVersion,
                    releasePageUrl = AppUpdateVersionLogic.GITHUB_RELEASES_WEB_URL,
                    downloadUrl = AppUpdateVersionLogic.GITHUB_RELEASES_WEB_URL,
                    statusMessage = "No public GitHub release found yet. Ensure the repo is public and the Build APK workflow has published a release.",
                    showDialog = false
                )
            }

            val reason = when (latestRes.code) {
                HttpURLConnection.HTTP_FORBIDDEN -> "GitHub API rate limit reached. Open Releases page directly."
                else -> "Update server returned HTTP ${latestRes.code}. Tap to retry."
            }
            AppUpdateUiState(
                status = AppUpdateStatus.ERROR,
                currentVersion = currentVersion,
                releasePageUrl = AppUpdateVersionLogic.GITHUB_RELEASES_WEB_URL,
                downloadUrl = AppUpdateVersionLogic.GITHUB_RELEASES_WEB_URL,
                statusMessage = reason,
                showDialog = false
            )
        } catch (_: Exception) {
            AppUpdateUiState(
                status = AppUpdateStatus.ERROR,
                currentVersion = currentVersion,
                releasePageUrl = AppUpdateVersionLogic.GITHUB_RELEASES_WEB_URL,
                downloadUrl = AppUpdateVersionLogic.GITHUB_RELEASES_WEB_URL,
                statusMessage = "Could not reach GitHub updates. Check your internet connection.",
                showDialog = false
            )
        }
    }
}
