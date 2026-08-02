package io.github.miksask.linkagram.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.miksask.linkagram.R
import io.github.miksask.linkagram.ui.theme.LinkagramTheme

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreenContent(
        state = state,
        onHistoryEnabledChanged = viewModel::setHistoryEnabled,
        onClearHistory = viewModel::showClearConfirmation,
        onDismissClear = viewModel::dismissClearConfirmation,
        onConfirmClear = viewModel::confirmClearAll,
        modifier = modifier,
    )
}

@Composable
fun SettingsScreenContent(
    state: SettingsUiState,
    onHistoryEnabledChanged: (Boolean) -> Unit,
    onClearHistory: () -> Unit,
    onDismissClear: () -> Unit,
    onConfirmClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.settings_history_toggle),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = state.historyEnabled,
                onCheckedChange = onHistoryEnabledChanged,
                enabled = !state.operationInProgress,
            )
        }
        Text(
            text = stringResource(
                if (state.historyEnabled) {
                    R.string.settings_history_helper_on
                } else {
                    R.string.settings_history_helper_off
                },
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = stringResource(R.string.settings_history_privacy),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(
            onClick = onClearHistory,
            enabled = !state.operationInProgress && state.totalCount > 0,
        ) {
            Text(stringResource(R.string.settings_clear_history))
        }
    }

    if (state.confirmClearAll) {
        AlertDialog(
            onDismissRequest = onDismissClear,
            title = { Text(stringResource(R.string.history_clear_confirm_title)) },
            text = {
                Text(stringResource(R.string.history_clear_confirm_body, state.totalCount))
            },
            confirmButton = {
                TextButton(onClick = onConfirmClear) {
                    Text(stringResource(R.string.history_confirm_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissClear) {
                    Text(stringResource(R.string.history_cancel))
                }
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    LinkagramTheme {
        SettingsScreenContent(
            state = SettingsUiState(historyEnabled = false, totalCount = 3),
            onHistoryEnabledChanged = {},
            onClearHistory = {},
            onDismissClear = {},
            onConfirmClear = {},
        )
    }
}
