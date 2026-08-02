package io.github.miksask.linkagram.ui.analysis

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.miksask.linkagram.R
import io.github.miksask.linkagram.core.clipboard.ClipboardUrlReader
import io.github.miksask.linkagram.domain.LocationInfo
import io.github.miksask.linkagram.domain.MapProvider
import io.github.miksask.linkagram.domain.RedirectStep
import io.github.miksask.linkagram.ui.theme.LinkagramTheme

@Composable
fun AnalysisScreen(
    viewModel: AnalysisViewModel,
    onHistorySaveNotice: (HistorySaveNotice) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(uiState.historySaveNotice) {
        val notice = uiState.historySaveNotice ?: return@LaunchedEffect
        onHistorySaveNotice(notice)
        viewModel.consumeHistorySaveNotice()
    }
    AnalysisScreenContent(
        state = uiState,
        onDraftUrlChanged = viewModel::onDraftUrlChanged,
        onAnalyze = viewModel::analyze,
        onPasteFromClipboard = {
            val text = ClipboardUrlReader(context).readText()
            viewModel.onPasteFromClipboard(text)
        },
        onCopyCoordinates = { text ->
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("coordinates", text))
            viewModel.onCoordinatesCopied()
        },
        onFindCoordinates = viewModel::onFindCoordinates,
        modifier = modifier,
    )
}

@Composable
fun AnalysisScreenContent(
    state: AnalysisUiState,
    onDraftUrlChanged: (String) -> Unit,
    onAnalyze: () -> Unit,
    onPasteFromClipboard: () -> Unit,
    onCopyCoordinates: (String) -> Unit,
    onFindCoordinates: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.app_tagline),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        OutlinedTextField(
            value = state.draftUrl,
            onValueChange = onDraftUrlChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = stringResource(R.string.url_input_label)) },
            placeholder = { Text(text = stringResource(R.string.url_input_placeholder)) },
            singleLine = true,
            enabled = !state.isAnalyzing,
            isError = state.validationError != null,
            supportingText = {
                state.validationError?.let { error ->
                    Text(text = stringResource(error.messageRes()))
                }
            },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onPasteFromClipboard,
                modifier = Modifier.weight(1f),
                enabled = !state.isAnalyzing,
            ) {
                Text(text = stringResource(R.string.paste_from_clipboard))
            }
            Button(
                onClick = onAnalyze,
                modifier = Modifier.weight(1f),
                enabled = !state.isAnalyzing,
            ) {
                Text(text = stringResource(R.string.analyze))
            }
        }
        if (state.isAnalyzing) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularProgressIndicator()
                Text(text = stringResource(R.string.analyzing))
            }
        }
        state.resolveError?.let { error ->
            Text(
                text = stringResource(error.messageRes()),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        state.finalUrl?.let { url ->
            ResultSection(
                title = stringResource(R.string.final_url_label),
                body = url,
            )
        }
        state.finalStatusCode?.let { code ->
            Text(
                text = stringResource(R.string.final_status_label, code),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        state.location?.let { location ->
            LocationSection(
                location = location,
                coordinatesText = state.coordinatesText,
                coordinatesCopied = state.coordinatesCopied,
                coordinatesAreApproximate = state.coordinatesAreApproximate,
                geocodeState = state.geocodeState,
                onCopyCoordinates = onCopyCoordinates,
                onFindCoordinates = onFindCoordinates,
            )
        }
        if (state.redirectChain.isNotEmpty()) {
            Text(
                text = stringResource(R.string.redirect_chain_label),
                style = MaterialTheme.typography.labelLarge,
            )
            state.redirectChain.forEachIndexed { index, step ->
                RedirectStepRow(index = index + 1, step = step)
            }
        }
    }
}

@Composable
private fun LocationSection(
    location: LocationInfo,
    coordinatesText: String?,
    coordinatesCopied: Boolean,
    coordinatesAreApproximate: Boolean,
    geocodeState: GeocodeState,
    onCopyCoordinates: (String) -> Unit,
    onFindCoordinates: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.location_section_label),
            style = MaterialTheme.typography.labelLarge,
        )
        Text(
            text = stringResource(R.string.provider_label, location.provider.displayName()),
            style = MaterialTheme.typography.bodyMedium,
        )
        location.placeName?.let {
            Text(
                text = stringResource(R.string.place_label, it),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        location.address?.let {
            Text(
                text = stringResource(R.string.address_label, it),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        coordinatesText?.let { text ->
            Text(
                text = stringResource(R.string.coordinates_label, text),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (coordinatesAreApproximate) {
                Text(
                    text = stringResource(R.string.coordinates_approximate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(onClick = { onCopyCoordinates(text) }) {
                Text(
                    text = stringResource(
                        if (coordinatesCopied) R.string.coordinates_copied else R.string.copy_coordinates,
                    ),
                )
            }
        }
        if (coordinatesText == null) {
            when (geocodeState) {
                GeocodeState.Available, GeocodeState.NotFound, GeocodeState.Failed -> {
                    OutlinedButton(onClick = onFindCoordinates) {
                        Text(text = stringResource(R.string.find_coordinates))
                    }
                }
                GeocodeState.Loading -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator()
                        Text(text = stringResource(R.string.finding_coordinates))
                    }
                }
                GeocodeState.Unavailable -> Unit
            }
            when (geocodeState) {
                GeocodeState.NotFound -> {
                    Text(
                        text = stringResource(R.string.error_geocode_not_found),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                GeocodeState.Failed -> {
                    Text(
                        text = stringResource(R.string.error_geocode_failed),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                else -> Unit
            }
        }
    }
}

@Composable
private fun ResultSection(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = title, style = MaterialTheme.typography.labelLarge)
        Text(text = body, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun RedirectStepRow(index: Int, step: RedirectStep) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = stringResource(
                R.string.redirect_step_header,
                index,
                step.statusCode?.toString() ?: "—",
            ),
            style = MaterialTheme.typography.labelMedium,
        )
        Text(
            text = step.fromUrl,
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = stringResource(R.string.redirect_to, step.toUrl ?: "—"),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

private fun MapProvider.displayName(): String = when (this) {
    MapProvider.GoogleMaps -> "Google Maps"
    MapProvider.YandexMaps -> "Yandex Maps"
    MapProvider.OpenStreetMap -> "OpenStreetMap"
    MapProvider.AppleMaps -> "Apple Maps"
    MapProvider.OrganicMaps -> "Organic Maps"
    MapProvider.Generic -> "Generic"
}

private fun ValidationError.messageRes(): Int = when (this) {
    ValidationError.Empty -> R.string.error_url_empty
    ValidationError.Malformed -> R.string.error_url_malformed
    ValidationError.UnsupportedScheme -> R.string.error_url_unsupported_scheme
    ValidationError.NoUrlFound -> R.string.error_url_not_found
}

private fun ResolveError.messageRes(): Int = when (this) {
    ResolveError.Network -> R.string.error_resolve_network
    ResolveError.Timeout -> R.string.error_resolve_timeout
    ResolveError.TooManyRedirects -> R.string.error_resolve_too_many_redirects
    ResolveError.RedirectLoop -> R.string.error_resolve_loop
    ResolveError.UnsupportedProtocol -> R.string.error_resolve_unsupported_protocol
    ResolveError.Http -> R.string.error_resolve_http
    ResolveError.Unknown -> R.string.error_resolve_unknown
}

@Preview(showBackground = true)
@Composable
private fun AnalysisScreenPreview() {
    LinkagramTheme {
        AnalysisScreenContent(
            state = AnalysisUiState(
                draftUrl = "https://maps.example/place",
                finalUrl = "https://maps.example/place",
                finalStatusCode = 200,
                location = LocationInfo(
                    provider = MapProvider.Generic,
                    latitude = 55.75,
                    longitude = 37.61,
                ),
                coordinatesText = "55.75, 37.61",
            ),
            onDraftUrlChanged = {},
            onAnalyze = {},
            onPasteFromClipboard = {},
            onCopyCoordinates = {},
        )
    }
}
