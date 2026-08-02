package io.github.miksask.linkagram

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.miksask.linkagram.core.url.UrlExtractor
import io.github.miksask.linkagram.ui.LinkagramApp
import io.github.miksask.linkagram.ui.theme.LinkagramTheme
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    private val incomingUrl = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Only on a fresh start: after a configuration change the ViewModel already
        // holds the current draft and must not be overwritten by the original intent.
        if (savedInstanceState == null) {
            incomingUrl.value = extractIncomingUrl(intent)
        }
        val container = (application as LinkagramApplication).container
        setContent {
            LinkagramTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val url by incomingUrl.collectAsStateWithLifecycle()
                    LinkagramApp(
                        resolveUrl = container.redirectResolver::resolve,
                        mapUrlParser = container.mapUrlParser,
                        geocode = container.nominatimGeocoder::geocode,
                        historyRepository = container.historyRepository,
                        pendingIncomingUrl = url,
                        onIncomingUrlConsumed = { incomingUrl.value = null },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingUrl.value = extractIncomingUrl(intent)
    }

    private fun extractIncomingUrl(intent: Intent?): String? {
        if (intent == null) return null
        return when (intent.action) {
            Intent.ACTION_SEND -> UrlExtractor.fromSendIntentText(
                intent.getStringExtra(Intent.EXTRA_TEXT),
            )
            Intent.ACTION_VIEW -> UrlExtractor.fromViewIntentData(intent.dataString)
            else -> null
        }
    }
}
