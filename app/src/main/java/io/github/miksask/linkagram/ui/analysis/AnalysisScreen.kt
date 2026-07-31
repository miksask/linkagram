package io.github.miksask.linkagram.ui.analysis

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.miksask.linkagram.R
import io.github.miksask.linkagram.ui.theme.LinkagramTheme

@Composable
fun AnalysisScreen(
    viewModel: AnalysisViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AnalysisScreenContent(
        draftUrl = uiState.draftUrl,
        onDraftUrlChanged = viewModel::onDraftUrlChanged,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreenContent(
    draftUrl: String,
    onDraftUrlChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.app_name)) },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.app_tagline),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            OutlinedTextField(
                value = draftUrl,
                onValueChange = onDraftUrlChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = stringResource(R.string.url_input_label)) },
                placeholder = { Text(text = stringResource(R.string.url_input_placeholder)) },
                singleLine = true,
            )
            Text(
                text = stringResource(R.string.bootstrap_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AnalysisScreenPreview() {
    LinkagramTheme {
        AnalysisScreenContent(
            draftUrl = "https://maps.example/place",
            onDraftUrlChanged = {},
        )
    }
}
