package com.example

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Display
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.data.notifications.ClinEdgeNotificationManager
import com.example.ui.ClinEdgeRootApp
import kotlinx.coroutines.flow.MutableStateFlow

data class NotificationDeepLink(
    val target: String,
    val doubtId: String = "",
    val nonce: Long = System.currentTimeMillis()
)

class MainActivity : ComponentActivity() {
    private val pendingDeepLink = MutableStateFlow<NotificationDeepLink?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableHighRefreshRateDisplay()
        enableEdgeToEdge()
        captureDeepLink(intent)
        setContent {
            ClinEdgeRootApp(
                pendingDeepLink = pendingDeepLink,
                onConsumeDeepLink = { pendingDeepLink.value = null }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        captureDeepLink(intent)
    }

    private fun captureDeepLink(intent: Intent?) {
        val target = intent?.getStringExtra(ClinEdgeNotificationManager.EXTRA_OPEN_TARGET)
            ?.takeIf { it.isNotBlank() } ?: return
        val doubtId = intent.getStringExtra(ClinEdgeNotificationManager.EXTRA_DOUBT_ID).orEmpty()
        pendingDeepLink.value = NotificationDeepLink(target = target, doubtId = doubtId)
    }

    /**
     * Opts the window into the device's highest native refresh rate (e.g., 90Hz / 120Hz)
     * so Jetpack Compose animations and rationale reveals run at native frame cadence.
     */
    private fun enableHighRefreshRateDisplay() {
        runCatching {
            val activeDisplay: Display? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                display
            } else {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay
            }
            val currentMode = activeDisplay?.mode ?: return
            val bestMode = activeDisplay.supportedModes
                .filter {
                    it.physicalWidth == currentMode.physicalWidth &&
                        it.physicalHeight == currentMode.physicalHeight
                }
                .maxByOrNull { it.refreshRate }
                ?: return

            if (bestMode.refreshRate > currentMode.refreshRate + 1f) {
                val params = window.attributes
                params.preferredDisplayModeId = bestMode.modeId
                params.preferredRefreshRate = bestMode.refreshRate
                window.attributes = params
            }
        }
    }
}
