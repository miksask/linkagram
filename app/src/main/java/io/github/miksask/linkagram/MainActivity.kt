package io.github.miksask.linkagram

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.miksask.linkagram.ui.analysis.AnalysisScreen
import io.github.miksask.linkagram.ui.analysis.AnalysisViewModel
import io.github.miksask.linkagram.ui.theme.LinkagramTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LinkagramTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val viewModel: AnalysisViewModel = viewModel()
                    AnalysisScreen(viewModel = viewModel)
                }
            }
        }
    }
}
