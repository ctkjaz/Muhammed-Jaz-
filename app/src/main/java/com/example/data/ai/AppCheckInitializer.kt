package com.example.data.ai

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory

private const val TAG = "ClinEdgeAppCheck"
private const val EXTRA_DEBUG_TOKEN = "FIREBASE_APPCHECK_DEBUG_TOKEN"
private const val DEBUG_STORE_PREFIX = "com.google.firebase.appcheck.debug.store."
private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"

/**
 * Configures Firebase App Check before any Firebase AI Logic or Firestore calls.
 * Must be invoked in MainActivity.onCreate() right after super.onCreate(), before setContent().
 */
fun configureAppCheck(context: Context, intent: Intent?) {
    runCatching {
        val appContext = context.applicationContext
        val firebaseApp = FirebaseApp.initializeApp(appContext) ?: FirebaseApp.getInstance()
        val firebaseAppCheck = Firebase.appCheck

        if (BuildConfig.DEBUG) {
            val debugToken = intent?.getStringExtra(EXTRA_DEBUG_TOKEN)?.trim().orEmpty()
            if (debugToken.isNotEmpty()) {
                val prefsName = "$DEBUG_STORE_PREFIX${firebaseApp.persistenceKey}"
                appContext.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
                    .edit()
                    .putString(DEBUG_SECRET_KEY, debugToken)
                    .commit()
            }
            firebaseAppCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
        } else {
            val siteKeyResId = appContext.resources.getIdentifier(
                "recaptcha_site_key",
                "string",
                appContext.packageName
            )
            if (siteKeyResId != 0) {
                val siteKey = appContext.getString(siteKeyResId).trim()
                if (siteKey.isNotEmpty()) {
                    firebaseAppCheck.installAppCheckProviderFactory(
                        RecaptchaAppCheckProviderFactory.getInstance(siteKey)
                    )
                } else {
                    Log.w(TAG, "recaptcha_site_key resource is blank; skipping release App Check provider.")
                }
            } else {
                Log.w(TAG, "recaptcha_site_key resource missing; skipping release App Check provider.")
            }
        }
    }.onFailure { error ->
        Log.e(TAG, "Failed to configure Firebase App Check", error)
    }
}
