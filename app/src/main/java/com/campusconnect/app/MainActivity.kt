package com.campusconnect.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.campusconnect.app.data.DeepLink
import com.campusconnect.app.push.Notifier
import com.campusconnect.app.ui.AppRoot
import com.campusconnect.app.ui.theme.CampusTheme

class MainActivity : ComponentActivity() {
    private val configured get() = BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY.isNotBlank()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as CampusApp
        handleIntent(intent)
        setContent {
            CampusTheme {
                if (!configured) {
                    ConfigMissing()
                } else {
                    // Switching language rebuilds the UI so every string and date format updates.
                    val lang by app.container.settings.lang.collectAsState()
                    Lang.set(lang)
                    key(lang) { AppRoot(app.container) }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(i: Intent?) {
        if (!configured) return
        val kind = i?.getStringExtra(Notifier.EXTRA_KIND) ?: return
        (application as CampusApp).container.pendingLink.value = DeepLink(kind, i.getStringExtra(Notifier.EXTRA_TARGET))
        i.removeExtra(Notifier.EXTRA_KIND)
    }
}

@Composable
private fun ConfigMissing() {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            "Supabase is not configured.\n\nAdd SUPABASE_URL and SUPABASE_ANON_KEY to local.properties and rebuild.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
    }
}
