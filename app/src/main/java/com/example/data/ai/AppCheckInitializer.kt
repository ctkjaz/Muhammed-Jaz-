package com.example.data.ai

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory

private const val TAG = "ClinEdgeAppCheck"
private const val EXTRA_DEBUG_TOKEN = "FIREBASE_APPCHECK_DEBUG_TOKEN"
private const val DEBUG_STORE_PREFIX = "com.google.firebase.appcheck.debug.store."
private const val TOKEN_STORE_PREFIX = "com.google.firebase.appcheck.store."
private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"
private const val HARNESS_PREFS_NAME = "clinedge_appcheck_meta"
private const val KEY_HARNESS_DEBUG_ACTIVE = "harness_debug_token_active"
private const val KEY_LAST_PROVIDER_MODE = "last_appcheck_provider_mode"
private const val RECAPTCHA_SITE_KEY_RES = "recaptcha_site_key"

private const val MODE_DEBUG_HARNESS = "debug_harness"
private const val MODE_RECAPTCHA = "recaptcha"
private const val MODE_DEBUG_FALLBACK = "debug_fallback"

@Volatile
internal var isAppCheckTokenWarmedUp: Boolean = false
    private set

@Volatile
internal var activeAppCheckProviderMode: String = ""
    private set

@Volatile
internal var lastAppCheckWarmupError: String? = null
    private set

@Volatile
private var appCheckInstalledForProcess: Boolean = false

internal fun markAppCheckWarmedUp() {
    isAppCheckTokenWarmedUp = true
    lastAppCheckWarmupError = null
}

internal fun resetAppCheckWarmupState(errorMessage: String? = null) {
    isAppCheckTokenWarmedUp = false
    lastAppCheckWarmupError = errorMessage
}

/**
 * Configures Firebase App Check before any Firebase AI Logic or Firestore calls.
 * Must be invoked in MainActivity.onCreate() right after super.onCreate(), before setContent().
 *
 * Strategy:
 * - If launched in the AI Studio streaming emulator / test harness (BuildConfig.DEBUG with
 *   FIREBASE_APPCHECK_DEBUG_TOKEN intent extra or previously stored harness token), synchronously
 *   commits the debug secret and installs DebugAppCheckProviderFactory.
 * - On physical Android devices installing a sideloaded debug APK (where no harness debug
 *   token is injected) AND on all Release builds, installs RecaptchaAppCheckProviderFactory with
 *   the provisioned recaptcha_site_key so real devices pass App Check attestation cleanly.
 * - Clears any stale cached App Check token store whenever the active provider mode switches
 *   across app upgrades (e.g., Debug provider -> reCAPTCHA Enterprise) so Firebase never sends
 *   an invalid cached token.
 * - Guards against redundant provider re-installation on Activity recreations (rotation / theme
 *   changes) unless a fresh FIREBASE_APPCHECK_DEBUG_TOKEN intent extra is supplied.
 */
fun configureAppCheck(context: Context, intent: Intent?) {
    try {
        val appContext = context.applicationContext
        if (FirebaseApp.getApps(appContext).isEmpty()) {
            FirebaseApp.initializeApp(appContext)
        }
        val firebaseApp = FirebaseApp.getInstance()
        val appCheck = FirebaseAppCheck.getInstance()
        val persistenceKey = firebaseApp.persistenceKey
        val metaPrefs = appContext.getSharedPreferences(HARNESS_PREFS_NAME, Context.MODE_PRIVATE)
        val incomingDebugToken = if (BuildConfig.DEBUG) {
            intent?.getStringExtra(EXTRA_DEBUG_TOKEN)?.trim().orEmpty()
        } else {
            ""
        }

        // Avoid re-installing the factory on every configuration change unless a fresh harness token arrived.
        if (appCheckInstalledForProcess && incomingDebugToken.isEmpty()) {
            return
        }

        if (BuildConfig.DEBUG) {
            if (incomingDebugToken.isNotEmpty()) {
                val debugStorePrefs = appContext.getSharedPreferences(
                    "$DEBUG_STORE_PREFIX$persistenceKey",
                    Context.MODE_PRIVATE
                )
                // CRITICAL: synchronous commit() before installing DebugAppCheckProviderFactory
                debugStorePrefs.edit()
                    .putString(DEBUG_SECRET_KEY, incomingDebugToken)
                    .commit()
                clearStaleTokenStoreIfProviderChanged(
                    appContext = appContext,
                    persistenceKey = persistenceKey,
                    metaPrefs = metaPrefs,
                    newMode = MODE_DEBUG_HARNESS,
                    forceClear = true
                )
                metaPrefs.edit()
                    .putBoolean(KEY_HARNESS_DEBUG_ACTIVE, true)
                    .putString(KEY_LAST_PROVIDER_MODE, MODE_DEBUG_HARNESS)
                    .commit()
                appCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
                activeAppCheckProviderMode = MODE_DEBUG_HARNESS
                appCheckInstalledForProcess = true
                Log.d(TAG, "App Check debug secret committed; DebugAppCheckProviderFactory installed.")
                preWarmAppCheckToken(appCheck, forceRefresh = true)
                return
            }

            val hasHarnessToken = metaPrefs.getBoolean(KEY_HARNESS_DEBUG_ACTIVE, false)
            if (hasHarnessToken) {
                appCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
                activeAppCheckProviderMode = MODE_DEBUG_HARNESS
                appCheckInstalledForProcess = true
                Log.d(TAG, "Reusing previously provisioned harness debug token for App Check.")
                preWarmAppCheckToken(appCheck, forceRefresh = false)
                return
            }

            // Physical device running sideloaded debug APK: use real reCAPTCHA Enterprise attestation
            val switchedToRecaptcha = clearStaleTokenStoreIfProviderChanged(
                appContext = appContext,
                persistenceKey = persistenceKey,
                metaPrefs = metaPrefs,
                newMode = MODE_RECAPTCHA,
                forceClear = false
            )
            if (installRecaptchaProviderIfConfigured(appContext, appCheck)) {
                metaPrefs.edit()
                    .putString(KEY_LAST_PROVIDER_MODE, MODE_RECAPTCHA)
                    .commit()
                activeAppCheckProviderMode = MODE_RECAPTCHA
                appCheckInstalledForProcess = true
                Log.d(TAG, "No harness debug token found; using RecaptchaAppCheckProviderFactory on device.")
                preWarmAppCheckToken(appCheck, forceRefresh = switchedToRecaptcha)
                return
            }

            // Fallback when recaptcha_site_key is absent in a local debug environment
            clearStaleTokenStoreIfProviderChanged(
                appContext = appContext,
                persistenceKey = persistenceKey,
                metaPrefs = metaPrefs,
                newMode = MODE_DEBUG_FALLBACK,
                forceClear = false
            )
            metaPrefs.edit()
                .putString(KEY_LAST_PROVIDER_MODE, MODE_DEBUG_FALLBACK)
                .commit()
            appCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
            activeAppCheckProviderMode = MODE_DEBUG_FALLBACK
            appCheckInstalledForProcess = true
            Log.w(TAG, "recaptcha_site_key unavailable; fell back to DebugAppCheckProviderFactory.")
            preWarmAppCheckToken(appCheck, forceRefresh = false)
        } else {
            val switchedToRecaptcha = clearStaleTokenStoreIfProviderChanged(
                appContext = appContext,
                persistenceKey = persistenceKey,
                metaPrefs = metaPrefs,
                newMode = MODE_RECAPTCHA,
                forceClear = false
            )
            if (installRecaptchaProviderIfConfigured(appContext, appCheck)) {
                metaPrefs.edit()
                    .putString(KEY_LAST_PROVIDER_MODE, MODE_RECAPTCHA)
                    .commit()
                activeAppCheckProviderMode = MODE_RECAPTCHA
                appCheckInstalledForProcess = true
                preWarmAppCheckToken(appCheck, forceRefresh = switchedToRecaptcha)
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to initialize Firebase App Check", e)
        resetAppCheckWarmupState(e.message)
    }
}

/**
 * Clears the Firebase App Check cached token SharedPreferences when switching attestation
 * providers across upgrades (e.g., DebugAppCheckProvider -> RecaptchaAppCheckProvider),
 * preventing Firebase from sending an outdated/unverified cached token.
 */
private fun clearStaleTokenStoreIfProviderChanged(
    appContext: Context,
    persistenceKey: String,
    metaPrefs: android.content.SharedPreferences,
    newMode: String,
    forceClear: Boolean
): Boolean {
    val previousMode = metaPrefs.getString(KEY_LAST_PROVIDER_MODE, null)
    if (!forceClear && previousMode == newMode) return false
    return runCatching {
        val tokenPrefs = appContext.getSharedPreferences(
            "$TOKEN_STORE_PREFIX$persistenceKey",
            Context.MODE_PRIVATE
        )
        tokenPrefs.edit().clear().commit()
        Log.d(TAG, "Cleared stale App Check token cache (provider transition: $previousMode -> $newMode).")
        true
    }.getOrDefault(false)
}

/**
 * Non-blocking fire-and-forget pre-warm of the App Check token on app startup.
 * RecaptchaAppCheckProviderFactory initializes lazily on first token request (taking 3-12s on mobile);
 * pre-warming it at launch ensures the token is already cached in memory before the student taps AI Tutor.
 */
private fun preWarmAppCheckToken(appCheck: FirebaseAppCheck, forceRefresh: Boolean) {
    runCatching {
        appCheck.getAppCheckToken(forceRefresh)
            .addOnSuccessListener { result ->
                if (result.token.isNotBlank()) {
                    markAppCheckWarmedUp()
                    Log.d(TAG, "App Check token pre-warmed and cached (provider=$activeAppCheckProviderMode).")
                }
            }
            .addOnFailureListener { err ->
                resetAppCheckWarmupState(err.message)
                Log.w(TAG, "App Check background pre-warm failed (provider=$activeAppCheckProviderMode): ${err.message}")
            }
    }
}

private fun installRecaptchaProviderIfConfigured(
    appContext: Context,
    appCheck: FirebaseAppCheck
): Boolean {
    val resId = appContext.resources.getIdentifier(
        RECAPTCHA_SITE_KEY_RES,
        "string",
        appContext.packageName
    )
    if (resId == 0) {
        Log.e(TAG, "recaptcha_site_key string resource not found in package.")
        return false
    }
    val siteKey = appContext.getString(resId).trim()
    if (siteKey.isBlank()) {
        Log.e(TAG, "recaptcha_site_key resource string is blank.")
        return false
    }
    appCheck.installAppCheckProviderFactory(
        RecaptchaAppCheckProviderFactory.getInstance(siteKey)
    )
    Log.d(TAG, "RecaptchaAppCheckProviderFactory installed.")
    return true
}
