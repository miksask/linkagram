package io.github.miksask.linkagram.ui.history

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.miksask.linkagram.R
import io.github.miksask.linkagram.domain.HistoryEntry
import io.github.miksask.linkagram.domain.HistoryRedirect
import io.github.miksask.linkagram.domain.HistoryResultType
import io.github.miksask.linkagram.domain.MapProvider
import io.github.miksask.linkagram.ui.theme.LinkagramTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun HistoryDetailsScreen(
    viewModel: HistoryDetailsViewModel,
    onAnalyzeAgain: (String) -> Unit,
    onDeleted: (HistoryEntry) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(state.navigateBack, state.deletedEntry) {
        if (state.navigateBack) {
            state.deletedEntry?.let(onDeleted)
            viewModel.consumeNavigateBack()
            onBack()
        }
    }
    HistoryDetailsScreenContent(
        state = state,
        onAnalyzeAgain = {
            state.entry?.sourceUrl?.let(onAnalyzeAgain)
        },
        onDelete = viewModel::delete,
        onCopyCoordinates = { text ->
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("coordinates", text))
        },
        modifier = modifier,
    )
}

@Composable
fun HistoryDetailsScreenContent(
    state: HistoryDetailsUiState,
    onAnalyzeAgain: () -> Unit,
    onDelete: () -> Unit,
    onCopyCoordinates: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var coordinatesCopied by remember { mutableStateOf(false) }
    LaunchedEffect(state.entry?.id) {
        coordinatesCopied = false
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when {
            state.isLoading -> CircularProgressIndicator()
            state.missing || state.entry == null -> {
                Text(stringResource(R.string.history_missing))
            }
            else -> {
                val entry = state.entry
                Text(
                    text = stringResource(
                        R.string.history_completed_at_label,
                        formatTimestamp(entry.completedAtMillis),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
                LabeledBlock(
                    title = stringResource(R.string.history_source_url_label),
                    body = entry.sourceUrl,
                )
                LabeledBlock(
                    title = stringResource(R.string.normalized_url_label),
                    body = entry.normalizedUrl,
                )
                LabeledBlock(
                    title = stringResource(R.string.final_url_label),
                    body = entry.finalUrl,
                )
                Text(
                    text = stringResource(R.string.final_status_label, entry.finalStatusCode),
                    style = MaterialTheme.typography.bodyMedium,
                )
                when (entry.resultType) {
                    HistoryResultType.Map -> {
                        entry.provider?.let {
                            Text(
                                text = stringResource(R.string.provider_label, it.name),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        entry.placeName?.let {
                            Text(
                                text = stringResource(R.string.place_label, it),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                    HistoryResultType.RichLink -> {
                        entry.richLinkKind?.let {
                            Text(
                                text = stringResource(R.string.rich_link_kind_label, it.displayName),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        entry.placeName?.let {
                            Text(
                                text = stringResource(R.string.rich_link_title_label, it),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                    HistoryResultType.Url -> {
                        entry.placeName?.let {
                            Text(
                                text = stringResource(R.string.place_label, it),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
                entry.address?.let {
                    Text(
                        text = stringResource(R.string.address_label, it),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                entry.coordinatesText?.let { text ->
                    Text(
                        text = stringResource(R.string.coordinates_label, text),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Button(
                        onClick = {
                            onCopyCoordinates(text)
                            coordinatesCopied = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.operationInProgress,
                    ) {
                        Text(
                            text = stringResource(
                                if (coordinatesCopied) {
                                    R.string.coordinates_copied
                                } else {
                                    R.string.copy_coordinates
                                },
                            ),
                        )
                    }
                }
                if (entry.redirectChain.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.redirect_chain_label),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    entry.redirectChain.forEach { step ->
                        Text(
                            text = stringResource(
                                R.string.redirect_step_header,
                                step.ordinal + 1,
                                step.statusCode?.toString() ?: "—",
                            ),
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Text(text = step.fromUrl, style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = stringResource(R.string.redirect_to, step.toUrl ?: "—"),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                Button(
                    onClick = onAnalyzeAgain,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.operationInProgress,
                ) {
                    Text(stringResource(R.string.history_analyze_again))
                }
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.operationInProgress,
                ) {
                    Text(stringResource(R.string.history_delete))
                }
            }
        }
    }
}

@Composable
private fun LabeledBlock(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = title, style = MaterialTheme.typography.labelLarge)
        Text(text = body, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun formatTimestamp(millis: Long): String =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(millis))

@Preview(showBackground = true)
@Composable
private fun HistoryDetailsPreview() {
    LinkagramTheme {
        HistoryDetailsScreenContent(
            state = HistoryDetailsUiState(
                isLoading = false,
                entry = HistoryEntry(
                    id = "1",
                    completedAtMillis = 1_700_000_000_000,
                    sourceUrl = "maps.app.goo.gl/example",
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
                    redirectChain = listOf(
                        HistoryRedirect(
                            ordinal = 0,
                            fromUrl = "https://maps.app.goo.gl/example",
                            toUrl = "https://www.google.com/maps/@55.75,37.61,17z",
                            statusCode = 302,
                        ),
                    ),
                ),
            ),
            onAnalyzeAgain = {},
            onDelete = {},
            onCopyCoordinates = {},
        )
    }
}
