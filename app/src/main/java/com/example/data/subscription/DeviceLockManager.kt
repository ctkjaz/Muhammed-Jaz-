package com.example.data.subscription

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import java.security.MessageDigest
import java.util.Locale

data class DeviceIdentity(
    val deviceHash: String,
    val shortDeviceCode: String,
    val deviceName: String
)

/**
 * Play-policy-compliant device fingerprint for 1-free-trial-per-phone anti-abuse locking.
 * Uses only SHA-256 of Settings.Secure.ANDROID_ID (zero dangerous permissions, never IMEI/MAC).
 */
class DeviceLockManager(private val context: Context) {

    @SuppressLint("HardwareIds")
    fun resolveDeviceIdentity(): DeviceIdentity {
        val androidId = runCatching {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        }.getOrNull()?.trim().orEmpty()

        val rawSeed = if (androidId.isNotBlank()) {
            "clinedge_trial_v1:$androidId"
        } else {
            val fallback = "${Build.BRAND}:${Build.MODEL}:${Build.DEVICE}:${Build.FINGERPRINT}"
            "clinedge_trial_v1:fallback:$fallback"
        }
        val hash = sha256Hex(rawSeed)
        val shortCode = formatShortCode(hash)
        val modelName = buildDeviceName()
        return DeviceIdentity(
            deviceHash = hash,
            shortDeviceCode = shortCode,
            deviceName = modelName
        )
    }

    companion object {
        fun sha256Hex(input: String): String {
            val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
            val sb = StringBuilder(bytes.size * 2)
            for (b in bytes) {
                sb.append(String.format(Locale.US, "%02x", b))
            }
            return sb.toString()
        }

        fun formatShortCode(deviceHash: String): String {
            val clean = deviceHash.uppercase(Locale.US).filter { it.isLetterOrDigit() }
            val prefix = clean.take(8).padEnd(8, '0')
            return "CE-$prefix"
        }

        fun buildDeviceName(): String {
            val brand = Build.MANUFACTURER.orEmpty().trim()
            val model = Build.MODEL.orEmpty().trim()
            val combined = when {
                brand.isBlank() && model.isBlank() -> "Android Phone"
                model.startsWith(brand, ignoreCase = true) -> model
                else -> "$brand $model".trim()
            }
            return combined.take(64)
        }
    }
}
