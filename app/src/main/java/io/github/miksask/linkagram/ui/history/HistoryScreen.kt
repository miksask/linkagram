package io.github.miksask.linkagram.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.miksask.linkagram.R
import io.github.miksask.linkagram.domain.HistoryDateFilter
import io.github.miksask.linkagram.domain.HistoryEntry
import io.github.miksask.linkagram.domain.HistoryResultType
import io.github.miksask.linkagram.domain.MapProvider
import io.github.miksask.linkagram.ui.common.UrlDisplay
import io.github.miksask.linkagram.ui.theme.LinkagramTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onOpenEntry: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryScreenContent(
        state = state,
        onSearchChanged = viewModel::onSearchChanged,
        onDateFilterSelected = viewModel::onDateFilterSelected,
        onCustomDatesSelected = viewModel::onCustomDatesSelected,
        onReset = viewModel::resetFilters,
        onOpenEntry = onOpenEntry,
        onDeleteEntry = viewModel::requestDelete,
        onClearAll = viewModel::showClearAllConfirmation,
        onDeleteMatching = viewModel::showDeleteMatchingConfirmation,
        onDismissConfirmations = viewModel::dismissConfirmations,
        onConfirmClearAll = viewModel::confirmClearAll,
        onConfirmDeleteMatching = viewModel::confirmDeleteMatching,
        modifier = modifier,
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreenContent(
    state: HistoryUiState,
    onSearchChanged: (String) -> Unit,
    onDateFilterSelected: (HistoryDateFilter) -> Unit,
    onCustomDatesSelected: (LocalDate, LocalDate) -> Unit = { _, _ -> },
    onReset: () -> Unit,
    onOpenEntry: (String) -> Unit,
    onDeleteEntry: (HistoryEntry) -> Unit,
    onClearAll: () -> Unit,
    onDeleteMatching: () -> Unit,
    onDismissConfirmations: () -> Unit,
    onConfirmClearAll: () -> Unit,
    onConfirmDeleteMatching: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pickingStart by remember { mutableStateOf(false) }
    var pickingEnd by remember { mutableStateOf(false) }
    var draftStart by remember { mutableStateOf<LocalDate?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = state.searchText,
            onValueChange = onSearchChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.history_search_label)) },
            placeholder = { Text(stringResource(R.string.history_search_placeholder)) },
            singleLine = true,
            enabled = !state.operationInProgress,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HistoryDateFilter.entries.forEach { filter ->
                FilterChip(
                    selected = state.dateFilter == filter,
                    onClick = {
                        if (filter == HistoryDateFilter.Custom) {
                            pickingStart = true
                        } else {
                            onDateFilterSelected(filter)
                        }
                    },
                    label = { Text(filterLabel(filter)) },
                    enabled = !state.operationInProgress,
                )
            }
        }
        if (state.dateFilter == HistoryDateFilter.Custom &&
            state.customStartDate != null &&
            state.customEndDate != null
        ) {
            Text(
                text = stringResource(
                    R.string.history_active_filter,
                    "${state.customStartDate} … ${state.customEndDate}",
                ),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (state.searchText.isNotBlank() || state.dateFilter != HistoryDateFilter.All) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (state.searchText.isNotBlank()) {
                    Text(
                        text = stringResource(R.string.history_active_search, state.searchText),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = stringResource(R.string.history_results_count, state.matchCount),
                    style = MaterialTheme.typography.bodySmall,
                )
                TextButton(onClick = onReset, enabled = !state.operationInProgress) {
                    Text(stringResource(R.string.history_reset))
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.totalCount > 0 && state.searchText.isBlank() &&
                state.dateFilter == HistoryDateFilter.All
            ) {
                OutlinedButton(
                    onClick = onClearAll,
                    enabled = !state.operationInProgress,
                ) {
                    Text(stringResource(R.string.history_clear))
                }
            } else if (state.matchCount > 0) {
                OutlinedButton(
                    onClick = onDeleteMatching,
                    enabled = !state.operationInProgress,
                ) {
                    Text(stringResource(R.string.history_delete_matching))
                }
            }
        }
        when {
            state.isLoading -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator()
                    Text(stringResource(R.string.history_loading))
                }
            }
            state.readError -> {
                Text(
                    text = stringResource(R.string.history_read_error),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            state.entries.isEmpty() -> {
                Text(text = emptyMessage(state), style = MaterialTheme.typography.bodyMedium)
            }
            else -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.entries, key = { it.id }) { entry ->
                        HistoryRow(
                            entry = entry,
                            onOpen = { onOpenEntry(entry.id) },
                            onDelete = { onDeleteEntry(entry) },
                            enabled = !state.operationInProgress,
                        )
                    }
                }
            }
        }
    }

    if (state.confirmClearAll) {
        AlertDialog(
            onDismissRequest = onDismissConfirmations,
            title = { Text(stringResource(R.string.history_clear_confirm_title)) },
            text = {
                Text(stringResource(R.string.history_clear_confirm_body, state.totalCount))
            },
            confirmButton = {
                TextButton(onClick = onConfirmClearAll) {
                    Text(stringResource(R.string.history_confirm_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissConfirmations) {
                    Text(stringResource(R.string.history_cancel))
                }
            },
        )
    }
    if (state.confirmDeleteMatching) {
        AlertDialog(
            onDismissRequest = onDismissConfirmations,
            title = { Text(stringResource(R.string.history_delete_matching_confirm_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.history_delete_matching_confirm_body,
                        state.matchCount,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = onConfirmDeleteMatching) {
                    Text(stringResource(R.string.history_confirm_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissConfirmations) {
                    Text(stringResource(R.string.history_cancel))
                }
            },
        )
    }

    if (pickingStart) {
        val startState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { pickingStart = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = startState.selectedDateMillis
                        if (millis != null) {
                            draftStart = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            pickingStart = false
                            pickingEnd = true
                        }
                    },
                ) {
                    Text(stringResource(R.string.custom_range_start))
                }
            },
            dismissButton = {
                TextButton(onClick = { pickingStart = false }) {
                    Text(stringResource(R.string.history_cancel))
                }
            },
        ) {
            DatePicker(state = startState)
        }
    }
    if (pickingEnd) {
        val endState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { pickingEnd = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = endState.selectedDateMillis
                        val start = draftStart
                        if (millis != null && start != null) {
                            val end = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            onCustomDatesSelected(start, end)
                            pickingEnd = false
                            draftStart = null
                        }
                    },
                ) {
                    Text(stringResource(R.string.custom_range_apply))
                }
            },
            dismissButton = {
                TextButton(onClick = { pickingEnd = false }) {
                    Text(stringResource(R.string.history_cancel))
                }
            },
        ) {
            DatePicker(state = endState)
        }
    }
}

@Composable
private fun HistoryRow(
    entry: HistoryEntry,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    enabled: Boolean,
) {
    val deleteCd = stringResource(R.string.history_delete_content_description)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onOpen)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = UrlDisplay.shorten(entry.sourceUrl),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = UrlDisplay.shorten(entry.finalUrl),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            entry.placeName?.let {
                Text(text = it, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            }
            entry.address?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall, maxLines = 2)
            }
            entry.coordinatesText?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall)
            }
            entry.provider?.let {
                Text(
                    text = stringResource(R.string.provider_label, it.name),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(
                text = stringResource(
                    R.string.history_completed_at_label,
                    formatTimestamp(entry.completedAtMillis),
                ),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = stringResource(R.string.history_redirects_count, entry.redirectCount),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        IconButton(
            onClick = onDelete,
            enabled = enabled,
            modifier = Modifier.semantics { contentDescription = deleteCd },
        ) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun filterLabel(filter: HistoryDateFilter): String = when (filter) {
    HistoryDateFilter.All -> stringResource(R.string.history_filter_all)
    HistoryDateFilter.Today -> stringResource(R.string.history_filter_today)
    HistoryDateFilter.Last7Days -> stringResource(R.string.history_filter_7d)
    HistoryDateFilter.Last30Days -> stringResource(R.string.history_filter_30d)
    HistoryDateFilter.Custom -> stringResource(R.string.history_filter_custom)
}

@Composable
private fun emptyMessage(state: HistoryUiState): String = when {
    !state.historyEnabled && state.totalCount == 0 ->
        stringResource(R.string.history_empty_disabled)
    state.searchText.isNotBlank() ->
        stringResource(R.string.history_no_search_matches)
    state.dateFilter != HistoryDateFilter.All ->
        stringResource(R.string.history_no_filter_matches)
    else -> stringResource(R.string.history_empty)
}

private fun formatTimestamp(millis: Long): String =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(millis))

@Preview(showBackground = true)
@Composable
private fun HistoryScreenPreview() {
    LinkagramTheme {
        HistoryScreenContent(
            state = HistoryUiState(
                isLoading = false,
                historyEnabled = true,
                totalCount = 1,
                matchCount = 1,
                entries = listOf(
                    HistoryEntry(
                        id = "1",
                        completedAtMillis = 1_700_000_000_000,
                        sourceUrl = "https://maps.app.goo.gl/example",
                        normalizedUrl = "https://maps.app.goo.gl/example",
                        finalUrl = "https://www.google.com/maps/@55.75,37.61,17z",
                        finalStatusCode = 200,
                        resultType = HistoryResultType.Map,
                        provider = MapProvider.GoogleMaps,
                        placeName = "Red Square",
                        address = "Moscow",
                        latitude = 55.75,
                        longitude = 37.61,
                        redirectCount = 1,
                    ),
                ),
            ),
            onSearchChanged = {},
            onDateFilterSelected = {},
            onReset = {},
            onOpenEntry = {},
            onDeleteEntry = {},
            onClearAll = {},
            onDeleteMatching = {},
            onDismissConfirmations = {},
            onConfirmClearAll = {},
            onConfirmDeleteMatching = {},
        )
    }
}
