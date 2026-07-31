package io.github.miksask.linkagram

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.miksask.linkagram.core.url.UrlExtractor
import io.github.miksask.linkagram.ui.analysis.AnalysisScreen
import io.github.miksask.linkagram.ui.analysis.AnalysisViewModel
import io.github.miksask.linkagram.ui.theme.LinkagramTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val initialUrl = extractIncomingUrl(intent)
        setContent {
            LinkagramTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val viewModel: AnalysisViewModel = viewModel()
                    LaunchedEffect(initialUrl) {
                        if (initialUrl != null) {
                            viewModel.onIncomingUrl(initialUrl)
                        }
                    }
                    AnalysisScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
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
