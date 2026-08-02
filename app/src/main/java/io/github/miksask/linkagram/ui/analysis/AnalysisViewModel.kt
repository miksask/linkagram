package io.github.miksask.linkagram.ui.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.miksask.linkagram.core.url.InvalidUrlReason
import io.github.miksask.linkagram.core.url.UrlNormalizationResult
import io.github.miksask.linkagram.core.url.UrlNormalizer
import io.github.miksask.linkagram.data.geocoding.NominatimGeocoder
import io.github.miksask.linkagram.data.history.HistoryRepository
import io.github.miksask.linkagram.data.maps.MapUrlParser
import io.github.miksask.linkagram.data.resolver.RedirectResolver
import io.github.miksask.linkagram.domain.CompletedAnalysis
import io.github.miksask.linkagram.domain.CoordinateFormatter
import io.github.miksask.linkagram.domain.GeocodeResult
import io.github.miksask.linkagram.domain.HistorySaveResult
import io.github.miksask.linkagram.domain.LocationInfo
import io.github.miksask.linkagram.domain.MapParseResult
import io.github.miksask.linkagram.domain.RedirectStep
import io.github.miksask.linkagram.domain.ResolveResult
import java.time.Clock
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AnalysisUiState(
    val draftUrl: String = "",
    val sourceUrl: String? = null,
    val normalizedUrl: String? = null,
    val finalUrl: String? = null,
    val finalStatusCode: Int? = null,
    val redirectChain: List<RedirectStep> = emptyList(),
    val location: LocationInfo? = null,
    val coordinatesText: String? = null,
    val coordinatesCopied: Boolean = false,
    val coordinatesAreApproximate: Boolean = false,
    val geocodeState: GeocodeState = GeocodeState.Unavailable,
    val validationError: ValidationError? = null,
    val resolveError: ResolveError? = null,
    val isAnalyzing: Boolean = false,
    val historySaveNotice: HistorySaveNotice? = null,
)

sealed interface GeocodeState {
    data object Unavailable : GeocodeState
    data object Available : GeocodeState
    data object Loading : GeocodeState
    data object NotFound : GeocodeState
    data object Failed : GeocodeState
}

enum class ValidationError {
    Empty,
    Malformed,
    UnsupportedScheme,
    NoUrlFound,
}

enum class ResolveError {
    Network,
    Timeout,
    TooManyRedirects,
    RedirectLoop,
    UnsupportedProtocol,
    Http,
    Unknown,
}

enum class HistorySaveNotice {
    Saved,
    SaveFailed,
}

class AnalysisViewModel(
    private val resolveUrl: suspend (String) -> ResolveResult = RedirectResolver()::resolve,
    private val mapUrlParser: MapUrlParser = MapUrlParser(),
    private val geocode: suspend (String?, String?) -> GeocodeResult =
        NominatimGeocoder()::geocode,
    private val historyRepository: HistoryRepository? = null,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AnalysisUiState())
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()

    private var analyzeJob: Job? = null
    private var geocodeJob: Job? = null

    fun onDraftUrlChanged(value: String) {
        _uiState.update { clearResults(it.copy(draftUrl = value)) }
    }

    fun onIncomingUrl(raw: String) {
        _uiState.update { clearResults(it.copy(draftUrl = raw)) }
    }

    fun onPasteFromClipboard(clipboardText: String?) {
        if (clipboardText.isNullOrBlank()) {
            _uiState.update {
                clearResults(it).copy(validationError = ValidationError.Empty)
            }
            return
        }
        onDraftUrlChanged(clipboardText.trim())
    }

    fun analyze() {
        analyzeJob?.cancel()
        geocodeJob?.cancel()
        when (val normalized = UrlNormalizer.normalize(_uiState.value.draftUrl)) {
            is UrlNormalizationResult.InvalidUrl -> {
                _uiState.update {
                    clearResults(it).copy(
                        validationError = normalized.reason.toValidationError(),
                        isAnalyzing = false,
                    )
                }
            }
            is UrlNormalizationResult.NormalizedUrl -> {
                _uiState.update {
                    clearResults(it).copy(
                        draftUrl = normalized.normalizedUrl,
                        sourceUrl = normalized.sourceUrl,
                        normalizedUrl = normalized.normalizedUrl,
                        isAnalyzing = true,
                    )
                }
                analyzeJob = viewModelScope.launch {
                    val result = withContext(ioDispatcher) {
                        resolveUrl(normalized.normalizedUrl)
                    }
                    applyResolveResult(
                        result = result,
                        sourceUrl = normalized.sourceUrl,
                        normalizedUrl = normalized.normalizedUrl,
                    )
                }
            }
        }
    }

    fun onFindCoordinates() {
        val state = _uiState.value
        val location = state.location ?: return
        if (state.coordinatesText != null) return
        if (state.geocodeState == GeocodeState.Loading) return
        if (!canGeocode(location)) return

        geocodeJob?.cancel()
        _uiState.update { it.copy(geocodeState = GeocodeState.Loading) }
        geocodeJob = viewModelScope.launch {
            val result = withContext(ioDispatcher) {
                geocode(location.placeName, location.address)
            }
            applyGeocodeResult(result)
        }
    }

    fun onCoordinatesCopied() {
        _uiState.update { it.copy(coordinatesCopied = true) }
    }

    fun consumeHistorySaveNotice() {
        _uiState.update { it.copy(historySaveNotice = null) }
    }

    private fun applyGeocodeResult(result: GeocodeResult) {
        when (result) {
            is GeocodeResult.Found -> {
                val text = CoordinateFormatter.format(result.latitude, result.longitude)
                _uiState.update {
                    it.copy(
                        coordinatesText = text,
                        coordinatesAreApproximate = true,
                        coordinatesCopied = false,
                        geocodeState = GeocodeState.Unavailable,
                        location = it.location?.copy(
                            latitude = result.latitude,
                            longitude = result.longitude,
                        ),
                    )
                }
            }
            GeocodeResult.NotFound -> {
                _uiState.update { it.copy(geocodeState = GeocodeState.NotFound) }
            }
            is GeocodeResult.Failed -> {
                _uiState.update { it.copy(geocodeState = GeocodeState.Failed) }
            }
        }
    }

    private suspend fun applyResolveResult(
        result: ResolveResult,
        sourceUrl: String,
        normalizedUrl: String,
    ) {
        when (result) {
            is ResolveResult.Success -> {
                val parsed = mapUrlParser.parse(result.finalUrl)
                val location = (parsed as? MapParseResult.Parsed)?.location
                _uiState.update {
                    it.copy(
                        isAnalyzing = false,
                        finalUrl = result.finalUrl,
                        finalStatusCode = result.finalStatusCode,
                        redirectChain = result.redirectChain,
                        resolveError = null,
                        location = location,
                        coordinatesText = location?.toCoordinatesText(),
                        coordinatesCopied = false,
                        coordinatesAreApproximate = false,
                        geocodeState = initialGeocodeState(location),
                    )
                }
                maybeSaveHistory(
                    CompletedAnalysis(
                        sourceUrl = sourceUrl,
                        normalizedUrl = normalizedUrl,
                        finalUrl = result.finalUrl,
                        finalStatusCode = result.finalStatusCode,
                        redirectChain = result.redirectChain,
                        location = location,
                        completedAtMillis = clock.millis(),
                    ),
                )
            }
            ResolveResult.InvalidInput -> {
                _uiState.update {
                    it.copy(
                        isAnalyzing = false,
                        validationError = ValidationError.Malformed,
                        resolveError = null,
                    )
                }
            }
            is ResolveResult.NetworkError -> {
                _uiState.update {
                    it.copy(isAnalyzing = false, resolveError = ResolveError.Network)
                }
            }
            ResolveResult.Timeout -> {
                _uiState.update {
                    it.copy(isAnalyzing = false, resolveError = ResolveError.Timeout)
                }
            }
            is ResolveResult.TooManyRedirects -> {
                _uiState.update {
                    it.copy(
                        isAnalyzing = false,
                        redirectChain = result.redirectChain,
                        resolveError = ResolveError.TooManyRedirects,
                    )
                }
            }
            is ResolveResult.RedirectLoop -> {
                _uiState.update {
                    it.copy(
                        isAnalyzing = false,
                        redirectChain = result.redirectChain,
                        resolveError = ResolveError.RedirectLoop,
                    )
                }
            }
            ResolveResult.UnsupportedProtocol -> {
                _uiState.update {
                    it.copy(isAnalyzing = false, resolveError = ResolveError.UnsupportedProtocol)
                }
            }
            is ResolveResult.HttpError -> {
                val parsed = mapUrlParser.parse(result.url)
                val location = (parsed as? MapParseResult.Parsed)?.location
                _uiState.update {
                    it.copy(
                        isAnalyzing = false,
                        finalUrl = result.url,
                        finalStatusCode = result.statusCode,
                        redirectChain = result.redirectChain,
                        resolveError = ResolveError.Http,
                        location = location,
                        coordinatesText = location?.toCoordinatesText(),
                        coordinatesAreApproximate = false,
                        geocodeState = initialGeocodeState(location),
                    )
                }
            }
            is ResolveResult.UnknownError -> {
                _uiState.update {
                    it.copy(isAnalyzing = false, resolveError = ResolveError.Unknown)
                }
            }
        }
    }

    private suspend fun maybeSaveHistory(analysis: CompletedAnalysis) {
        val repository = historyRepository ?: return
        when (repository.saveIfEnabled(analysis)) {
            is HistorySaveResult.Saved -> {
                _uiState.update { it.copy(historySaveNotice = HistorySaveNotice.Saved) }
            }
            HistorySaveResult.SkippedDisabled -> Unit
            is HistorySaveResult.Failed -> {
                _uiState.update { it.copy(historySaveNotice = HistorySaveNotice.SaveFailed) }
            }
        }
    }

    private fun clearResults(state: AnalysisUiState): AnalysisUiState =
        state.copy(
            validationError = null,
            resolveError = null,
            sourceUrl = null,
            normalizedUrl = null,
            finalUrl = null,
            finalStatusCode = null,
            redirectChain = emptyList(),
            location = null,
            coordinatesText = null,
            coordinatesCopied = false,
            coordinatesAreApproximate = false,
            geocodeState = GeocodeState.Unavailable,
            historySaveNotice = null,
        )

    private fun LocationInfo.toCoordinatesText(): String? {
        val lat = latitude ?: return null
        val lon = longitude ?: return null
        return CoordinateFormatter.format(lat, lon)
    }

    private fun initialGeocodeState(location: LocationInfo?): GeocodeState =
        if (location != null && !location.hasCoordinates && canGeocode(location)) {
            GeocodeState.Available
        } else {
            GeocodeState.Unavailable
        }

    private fun canGeocode(location: LocationInfo): Boolean {
        val place = location.placeName?.trim().orEmpty()
        val address = location.address?.trim().orEmpty()
        return place.isNotEmpty() || address.isNotEmpty()
    }

    private fun InvalidUrlReason.toValidationError(): ValidationError = when (this) {
        InvalidUrlReason.Empty -> ValidationError.Empty
        InvalidUrlReason.Malformed -> ValidationError.Malformed
        InvalidUrlReason.UnsupportedScheme -> ValidationError.UnsupportedScheme
        InvalidUrlReason.NoUrlFound -> ValidationError.NoUrlFound
    }

    class Factory(
        private val resolveUrl: suspend (String) -> ResolveResult,
        private val mapUrlParser: MapUrlParser,
        private val geocode: suspend (String?, String?) -> GeocodeResult,
        private val historyRepository: HistoryRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AnalysisViewModel::class.java)) {
                return AnalysisViewModel(
                    resolveUrl = resolveUrl,
                    mapUrlParser = mapUrlParser,
                    geocode = geocode,
                    historyRepository = historyRepository,
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}
