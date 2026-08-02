package io.github.miksask.linkagram.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.miksask.linkagram.core.time.HistoryDateRangeCalculator
import io.github.miksask.linkagram.data.history.HistoryRepository
import io.github.miksask.linkagram.domain.HistoryDateFilter
import io.github.miksask.linkagram.domain.HistoryEntry
import io.github.miksask.linkagram.domain.HistoryQuery
import java.time.LocalDate
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HistoryUiState(
    val searchText: String = "",
    val dateFilter: HistoryDateFilter = HistoryDateFilter.All,
    val customStartDate: LocalDate? = null,
    val customEndDate: LocalDate? = null,
    val historyEnabled: Boolean = false,
    val isLoading: Boolean = true,
    val entries: List<HistoryEntry> = emptyList(),
    val matchCount: Int = 0,
    val totalCount: Int = 0,
    val readError: Boolean = false,
    val operationInProgress: Boolean = false,
    val pendingDelete: HistoryEntry? = null,
    val confirmClearAll: Boolean = false,
    val confirmDeleteMatching: Boolean = false,
    val snackbarMessage: HistorySnackbar? = null,
)

sealed interface HistorySnackbar {
    data class DeletedOne(val entry: HistoryEntry) : HistorySnackbar
    data class DeletedMany(val count: Int) : HistorySnackbar
    data object Restored : HistorySnackbar
    data object OperationFailed : HistorySnackbar
}

class HistoryViewModel(
    private val historyRepository: HistoryRepository,
    private val dateRangeCalculator: HistoryDateRangeCalculator = HistoryDateRangeCalculator(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private val rawSearch = MutableStateFlow("")
    private val dateFilter = MutableStateFlow(HistoryDateFilter.All)
    private val customStart = MutableStateFlow<LocalDate?>(null)
    private val customEnd = MutableStateFlow<LocalDate?>(null)
    private var observeJob: Job? = null
    private var debounceJob: Job? = null

    init {
        viewModelScope.launch {
            historyRepository.historyEnabled.collect { enabled ->
                _uiState.update { it.copy(historyEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            combine(rawSearch, dateFilter, customStart, customEnd) { search, filter, start, end ->
                QueryInputs(search, filter, start, end)
            }.distinctUntilChanged().collect { inputs ->
                debounceJob?.cancel()
                debounceJob = viewModelScope.launch {
                    delay(SEARCH_DEBOUNCE_MS)
                    restartObservation(inputs)
                }
            }
        }
    }

    fun onSearchChanged(value: String) {
        rawSearch.value = value
        _uiState.update { it.copy(searchText = value) }
    }

    fun onDateFilterSelected(filter: HistoryDateFilter) {
        dateFilter.value = filter
        _uiState.update { it.copy(dateFilter = filter) }
        if (filter != HistoryDateFilter.Custom) {
            customStart.value = null
            customEnd.value = null
            _uiState.update { it.copy(customStartDate = null, customEndDate = null) }
        }
    }

    fun onCustomDatesSelected(start: LocalDate, end: LocalDate) {
        val orderedStart = minOf(start, end)
        val orderedEnd = maxOf(start, end)
        customStart.value = orderedStart
        customEnd.value = orderedEnd
        dateFilter.value = HistoryDateFilter.Custom
        _uiState.update {
            it.copy(
                dateFilter = HistoryDateFilter.Custom,
                customStartDate = orderedStart,
                customEndDate = orderedEnd,
            )
        }
    }

    fun resetFilters() {
        rawSearch.value = ""
        dateFilter.value = HistoryDateFilter.All
        customStart.value = null
        customEnd.value = null
        _uiState.update {
            it.copy(
                searchText = "",
                dateFilter = HistoryDateFilter.All,
                customStartDate = null,
                customEndDate = null,
            )
        }
    }

    fun requestDelete(entry: HistoryEntry) {
        if (_uiState.value.operationInProgress) return
        viewModelScope.launch {
            _uiState.update { it.copy(operationInProgress = true) }
            runCatching { historyRepository.deleteById(entry.id) }
                .onSuccess { deleted ->
                    _uiState.update {
                        it.copy(
                            operationInProgress = false,
                            snackbarMessage = deleted?.let(HistorySnackbar::DeletedOne),
                        )
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            operationInProgress = false,
                            snackbarMessage = HistorySnackbar.OperationFailed,
                        )
                    }
                }
        }
    }

    fun undoDelete(entry: HistoryEntry) {
        if (_uiState.value.operationInProgress) return
        viewModelScope.launch {
            _uiState.update { it.copy(operationInProgress = true) }
            runCatching { historyRepository.restore(entry) }
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            operationInProgress = false,
                            snackbarMessage = HistorySnackbar.Restored,
                        )
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            operationInProgress = false,
                            snackbarMessage = HistorySnackbar.OperationFailed,
                        )
                    }
                }
        }
    }

    fun showClearAllConfirmation() {
        viewModelScope.launch {
            val total = historyRepository.totalCount()
            _uiState.update {
                it.copy(
                    totalCount = total,
                    confirmClearAll = total > 0,
                    confirmDeleteMatching = false,
                )
            }
        }
    }

    fun showDeleteMatchingConfirmation() {
        val count = _uiState.value.matchCount
        if (count <= 0) return
        if (isClearAllEquivalent()) {
            showClearAllConfirmation()
            return
        }
        _uiState.update { it.copy(confirmDeleteMatching = true, confirmClearAll = false) }
    }

    fun dismissConfirmations() {
        _uiState.update { it.copy(confirmClearAll = false, confirmDeleteMatching = false) }
    }

    fun confirmClearAll() {
        if (_uiState.value.operationInProgress) return
        viewModelScope.launch {
            _uiState.update { it.copy(operationInProgress = true, confirmClearAll = false) }
            runCatching { historyRepository.clearAll() }
                .onSuccess { count ->
                    resetFilters()
                    _uiState.update {
                        it.copy(
                            operationInProgress = false,
                            snackbarMessage = HistorySnackbar.DeletedMany(count),
                        )
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            operationInProgress = false,
                            snackbarMessage = HistorySnackbar.OperationFailed,
                        )
                    }
                }
        }
    }

    fun confirmDeleteMatching() {
        if (_uiState.value.operationInProgress) return
        if (isClearAllEquivalent()) {
            confirmClearAll()
            return
        }
        viewModelScope.launch {
            val query = currentQuery()
            _uiState.update { it.copy(operationInProgress = true, confirmDeleteMatching = false) }
            runCatching { historyRepository.deleteMatching(query) }
                .onSuccess { count ->
                    _uiState.update {
                        it.copy(
                            operationInProgress = false,
                            snackbarMessage = HistorySnackbar.DeletedMany(count),
                        )
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            operationInProgress = false,
                            snackbarMessage = HistorySnackbar.OperationFailed,
                        )
                    }
                }
        }
    }

    fun consumeSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun isClearAllEquivalent(): Boolean =
        _uiState.value.searchText.isBlank() &&
            _uiState.value.dateFilter == HistoryDateFilter.All

    private fun restartObservation(inputs: QueryInputs) {
        observeJob?.cancel()
        _uiState.update { it.copy(isLoading = true, readError = false) }
        val query = inputs.toQuery()
        observeJob = viewModelScope.launch {
            combine(
                historyRepository.observeEntries(query),
                historyRepository.observeCount(query),
                historyRepository.historyEnabled,
            ) { entries, count, enabled ->
                Triple(entries, count, enabled)
            }.catch {
                _uiState.update {
                    it.copy(isLoading = false, readError = true, entries = emptyList(), matchCount = 0)
                }
            }.collect { (entries, count, enabled) ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        readError = false,
                        entries = entries,
                        matchCount = count,
                        historyEnabled = enabled,
                    )
                }
            }
        }
        viewModelScope.launch {
            runCatching { historyRepository.totalCount() }
                .onSuccess { total -> _uiState.update { it.copy(totalCount = total) } }
        }
    }

    private fun currentQuery(): HistoryQuery = QueryInputs(
        search = rawSearch.value,
        filter = dateFilter.value,
        start = customStart.value,
        end = customEnd.value,
    ).toQuery()

    private fun QueryInputs.toQuery(): HistoryQuery {
        val customBounds = if (filter == HistoryDateFilter.Custom && start != null && end != null) {
            dateRangeCalculator.customInclusiveLocalDates(start, end)
        } else {
            null
        }
        return HistoryQuery(
            searchText = search,
            dateFilter = filter,
            customStartDateInclusiveMillis = customBounds?.startInclusiveMillis,
            customEndExclusiveMillis = customBounds?.endExclusiveMillis,
        )
    }

    private data class QueryInputs(
        val search: String,
        val filter: HistoryDateFilter,
        val start: LocalDate?,
        val end: LocalDate?,
    )

    class Factory(
        private val historyRepository: HistoryRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
                return HistoryViewModel(historyRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }

    companion object {
        const val SEARCH_DEBOUNCE_MS = 300L
    }
}
