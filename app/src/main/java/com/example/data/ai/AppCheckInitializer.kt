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

/**
 * Configures Firebase App Check before any Firebase AI Logic or Firestore calls.
 * Must be invoked in MainActivity.onCreate() right after super.onCreate(), before setContent().
 *
 * Strategy:
 * - If launched in the AI Studio streaming emulator / test harness (BuildConfig.DEBUG with
 *   FIREBASE_APPCHECK_DEBUG_TOKEN intent extra or previously stored harness token), synchronously
 *   commits the debug secret and installs DebugAppCheckProviderFactory.
 * - On physical Android devices installing a GitHub Actions debug APK (where no harness debug
 *   token is injected) AND on all Release builds, installs RecaptchaAppCheckProviderFactory with
 *   the provisioned recaptcha_site_key so real devices pass App Check attestation cleanly instead
 *   of generating an unregistered random debug UUID.
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
                return
            }

            val hasHarnessToken = metaPrefs.getBoolean(KEY_HARNESS_DEBUG_ACTIVE, false)
            if (hasHarnessToken) {
                appCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
                Log.d(TAG, "Reusing previously provisioned harness debug token for App Check.")
                return
            }

            // Physical device running sideloaded debug APK: prefer real reCAPTCHA Enterprise attestation
            if (installRecaptchaProviderIfConfigured(appContext, appCheck)) {
                Log.d(TAG, "No harness debug token found; using RecaptchaAppCheckProviderFactory on device.")
                return
            }

            // Fallback when recaptcha_site_key is absent in a local debug environment
            appCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
            Log.w(TAG, "recaptcha_site_key unavailable; fell back to DebugAppCheckProviderFactory.")
        } else {
            installRecaptchaProviderIfConfigured(appContext, appCheck)
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to initialize Firebase App Check", e)
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
