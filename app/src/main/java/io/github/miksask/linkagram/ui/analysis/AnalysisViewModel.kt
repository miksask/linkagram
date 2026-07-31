package io.github.miksask.linkagram.ui.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.miksask.linkagram.core.url.InvalidUrlReason
import io.github.miksask.linkagram.core.url.UrlNormalizationResult
import io.github.miksask.linkagram.core.url.UrlNormalizer
import io.github.miksask.linkagram.data.maps.MapUrlParser
import io.github.miksask.linkagram.data.resolver.RedirectResolver
import io.github.miksask.linkagram.domain.CoordinateFormatter
import io.github.miksask.linkagram.domain.LocationInfo
import io.github.miksask.linkagram.domain.MapParseResult
import io.github.miksask.linkagram.domain.RedirectStep
import io.github.miksask.linkagram.domain.ResolveResult
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
    val normalizedUrl: String? = null,
    val finalUrl: String? = null,
    val redirectChain: List<RedirectStep> = emptyList(),
    val location: LocationInfo? = null,
    val coordinatesText: String? = null,
    val coordinatesCopied: Boolean = false,
    val validationError: ValidationError? = null,
    val resolveError: ResolveError? = null,
    val isAnalyzing: Boolean = false,
)

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

class AnalysisViewModel(
    private val resolveUrl: (String) -> ResolveResult = { RedirectResolver().resolve(it) },
    private val mapUrlParser: MapUrlParser = MapUrlParser(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AnalysisUiState())
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()

    private var analyzeJob: Job? = null

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
                        draftUrl = normalized.url,
                        normalizedUrl = normalized.url,
                        isAnalyzing = true,
                    )
                }
                analyzeJob = viewModelScope.launch {
                    val result = withContext(ioDispatcher) {
                        resolveUrl(normalized.url)
                    }
                    applyResolveResult(result)
                }
            }
        }
    }

    fun onCoordinatesCopied() {
        _uiState.update { it.copy(coordinatesCopied = true) }
    }

    private fun applyResolveResult(result: ResolveResult) {
        when (result) {
            is ResolveResult.Success -> {
                val parsed = mapUrlParser.parse(result.finalUrl)
                val location = (parsed as? MapParseResult.Parsed)?.location
                _uiState.update {
                    it.copy(
                        isAnalyzing = false,
                        finalUrl = result.finalUrl,
                        redirectChain = result.redirectChain,
                        resolveError = null,
                        location = location,
                        coordinatesText = location?.toCoordinatesText(),
                        coordinatesCopied = false,
                    )
                }
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
                        redirectChain = result.redirectChain,
                        resolveError = ResolveError.Http,
                        location = location,
                        coordinatesText = location?.toCoordinatesText(),
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

    private fun clearResults(state: AnalysisUiState): AnalysisUiState =
        state.copy(
            validationError = null,
            resolveError = null,
            normalizedUrl = null,
            finalUrl = null,
            redirectChain = emptyList(),
            location = null,
            coordinatesText = null,
            coordinatesCopied = false,
        )

    private fun LocationInfo.toCoordinatesText(): String? {
        val lat = latitude ?: return null
        val lon = longitude ?: return null
        return CoordinateFormatter.format(lat, lon)
    }

    private fun InvalidUrlReason.toValidationError(): ValidationError = when (this) {
        InvalidUrlReason.Empty -> ValidationError.Empty
        InvalidUrlReason.Malformed -> ValidationError.Malformed
        InvalidUrlReason.UnsupportedScheme -> ValidationError.UnsupportedScheme
        InvalidUrlReason.NoUrlFound -> ValidationError.NoUrlFound
    }
}
