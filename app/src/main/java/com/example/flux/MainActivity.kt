package com.example.flux

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.flux.core.PreferencesManager
import com.example.flux.ui.screens.BrowserScreen
import com.example.flux.core.GeckoRuntimeHolder
import org.mozilla.geckoview.ContentBlocking
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (GeckoRuntimeHolder.runtime == null) {
            val contentBlockingSettings = ContentBlocking.Settings.Builder()
                .antiTracking(
                    ContentBlocking.AntiTracking.AD or
                    ContentBlocking.AntiTracking.ANALYTIC or
                    ContentBlocking.AntiTracking.SOCIAL or
                    ContentBlocking.AntiTracking.CRYPTOMINING or
                    ContentBlocking.AntiTracking.FINGERPRINTING
                )
                .safeBrowsing(
                    ContentBlocking.SafeBrowsing.MALWARE or
                    ContentBlocking.SafeBrowsing.PHISHING
                )
                .cookieBehavior(ContentBlocking.CookieBehavior.ACCEPT_NON_TRACKERS)
                .build()

            val darkMode = PreferencesManager.loadDarkMode(this)
            val runtimeSettings = GeckoRuntimeSettings.Builder()
                .contentBlocking(contentBlockingSettings)
                .preferredColorScheme(
                    if (darkMode) GeckoRuntimeSettings.COLOR_SCHEME_DARK
                    else GeckoRuntimeSettings.COLOR_SCHEME_LIGHT
                )
                .build()

            GeckoRuntimeHolder.runtime = GeckoRuntime.create(this, runtimeSettings)
        }

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BrowserScreen()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (this.isFinishing) {
            GeckoRuntimeHolder.runtime?.shutdown()
        }
    }
}