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
private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"
private const val HARNESS_PREFS_NAME = "clinedge_appcheck_meta"
private const val KEY_HARNESS_DEBUG_ACTIVE = "harness_debug_token_active"
private const val RECAPTCHA_SITE_KEY_RES = "recaptcha_site_key"

@Volatile
internal var isAppCheckTokenWarmedUp: Boolean = false
    private set

internal fun markAppCheckWarmedUp() {
    isAppCheckTokenWarmedUp = true
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
 * - Immediately triggers a non-blocking background token pre-warm so reCAPTCHA Enterprise
 *   completes initialization while the user browses the app instead of blocking their first AI call.
 */
fun configureAppCheck(context: Context, intent: Intent?) {
    try {
        val appContext = context.applicationContext
        if (FirebaseApp.getApps(appContext).isEmpty()) {
            FirebaseApp.initializeApp(appContext)
        }
        val firebaseApp = FirebaseApp.getInstance()
        val appCheck = FirebaseAppCheck.getInstance()

        if (BuildConfig.DEBUG) {
            val debugToken = intent?.getStringExtra(EXTRA_DEBUG_TOKEN)?.trim().orEmpty()
            val metaPrefs = appContext.getSharedPreferences(HARNESS_PREFS_NAME, Context.MODE_PRIVATE)
            if (debugToken.isNotEmpty()) {
                val persistenceKey = firebaseApp.persistenceKey
                val debugStorePrefs = appContext.getSharedPreferences(
                    "$DEBUG_STORE_PREFIX$persistenceKey",
                    Context.MODE_PRIVATE
                )
                // CRITICAL: synchronous commit() before installing DebugAppCheckProviderFactory
                debugStorePrefs.edit()
                    .putString(DEBUG_SECRET_KEY, debugToken)
                    .commit()
                metaPrefs.edit()
                    .putBoolean(KEY_HARNESS_DEBUG_ACTIVE, true)
                    .commit()
                appCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
                Log.d(TAG, "App Check debug secret committed; DebugAppCheckProviderFactory installed.")
                preWarmAppCheckToken(appCheck)
                return
            }

            val hasHarnessToken = metaPrefs.getBoolean(KEY_HARNESS_DEBUG_ACTIVE, false)
            if (hasHarnessToken) {
                appCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
                Log.d(TAG, "Reusing previously provisioned harness debug token for App Check.")
                preWarmAppCheckToken(appCheck)
                return
            }

            // Physical device running sideloaded debug APK: prefer real reCAPTCHA Enterprise attestation
            if (installRecaptchaProviderIfConfigured(appContext, appCheck)) {
                Log.d(TAG, "No harness debug token found; using RecaptchaAppCheckProviderFactory on device.")
                preWarmAppCheckToken(appCheck)
                return
            }

            // Fallback when recaptcha_site_key is absent in a local debug environment
            appCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
            Log.w(TAG, "recaptcha_site_key unavailable; fell back to DebugAppCheckProviderFactory.")
            preWarmAppCheckToken(appCheck)
        } else {
            if (installRecaptchaProviderIfConfigured(appContext, appCheck)) {
                preWarmAppCheckToken(appCheck)
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to initialize Firebase App Check", e)
    }
}

/**
 * Non-blocking fire-and-forget pre-warm of the App Check token on app startup.
 * RecaptchaAppCheckProviderFactory initializes lazily on first token request (taking 3-12s on mobile);
 * pre-warming it at launch ensures the token is already cached in memory before the student taps AI Tutor.
 */
private fun preWarmAppCheckToken(appCheck: FirebaseAppCheck) {
    runCatching {
        appCheck.getAppCheckToken(false)
            .addOnSuccessListener {
                isAppCheckTokenWarmedUp = true
                Log.d(TAG, "App Check token pre-warmed and cached.")
            }
            .addOnFailureListener { err ->
                Log.w(TAG, "App Check background pre-warm failed (will retry on AI request): ${err.message}")
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
