package com.example

import android.os.Build
import android.os.Bundle
import android.view.Display
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ui.ClinEdgeRootApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableHighRefreshRateDisplay()
        enableEdgeToEdge()
        setContent {
            ClinEdgeRootApp()
        }
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
