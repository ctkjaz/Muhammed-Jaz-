package com.example.data.model

import android.os.Build
import java.util.Locale
import java.util.UUID

sealed class SessionCheckResult {
    data object Valid : SessionCheckResult()
    data object ClaimNeeded : SessionCheckResult()
    data class Revoked(val activeDeviceName: String) : SessionCheckResult()
}

object AccountSessionLogic {
    const val MAX_PHONE_LENGTH = 20
    const val MAX_SESSION_ID_LENGTH = 64
    const val MAX_DEVICE_NAME_LENGTH = 64

    fun generateSessionId(): String = UUID.randomUUID().toString()

    fun buildDeviceLabel(
        manufacturer: String = Build.MANUFACTURER.orEmpty(),
        model: String = Build.MODEL.orEmpty()
    ): String {
        val cleanMan = manufacturer.trim()
        val cleanModel = model.trim()
        val raw = when {
            cleanModel.isBlank() && cleanMan.isBlank() -> "Android Phone"
            cleanMan.isBlank() -> cleanModel
            cleanModel.isBlank() -> cleanMan.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString()
            }
            cleanModel.startsWith(cleanMan, ignoreCase = true) -> cleanModel
            else -> "${cleanMan.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }} $cleanModel"
        }
        return raw.replace(Regex("\\s+"), " ").trim().take(MAX_DEVICE_NAME_LENGTH)
    }

    fun evaluateRemoteSession(
        localSessionId: String,
        remoteSessionId: String?,
        remoteDeviceName: String?
    ): SessionCheckResult {
        val cleanRemote = remoteSessionId?.trim().orEmpty()
        if (cleanRemote.isBlank()) {
            return SessionCheckResult.ClaimNeeded
        }
        val cleanLocal = localSessionId.trim()
        if (cleanLocal.isBlank() || cleanRemote != cleanLocal) {
            val deviceLabel = remoteDeviceName?.trim().orEmpty().ifBlank { "another device" }
            return SessionCheckResult.Revoked(activeDeviceName = deviceLabel)
        }
        return SessionCheckResult.Valid
    }

    fun buildForcedSignOutMessage(otherDeviceName: String?): String {
        val label = otherDeviceName?.trim().orEmpty()
        return if (label.isNotBlank() && !label.equals("another device", ignoreCase = true)) {
            "Signed out because your account was logged in on $label. Sign in again to switch access back to this phone."
        } else {
            "Signed out because your account was logged in on another phone. Sign in again to switch access back to this device."
        }
    }

    fun sanitizePhoneNumber(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return ""
        val builder = StringBuilder()
        trimmed.forEachIndexed { idx, ch ->
            when {
                ch.isDigit() -> builder.append(ch)
                ch == '+' && idx == 0 && builder.isEmpty() -> builder.append(ch)
                (ch == ' ' || ch == '-') && builder.isNotEmpty() && builder.last() != ' ' -> builder.append(ch)
            }
        }
        return builder.toString().trim().take(MAX_PHONE_LENGTH)
    }

    fun isPhoneNumberValidOrEmpty(raw: String): Boolean {
        val cleaned = sanitizePhoneNumber(raw)
        if (cleaned.isEmpty()) return true
        val digitCount = cleaned.count { it.isDigit() }
        return digitCount in 10..15
    }
}
