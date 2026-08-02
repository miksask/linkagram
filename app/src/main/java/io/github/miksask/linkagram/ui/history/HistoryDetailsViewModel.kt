package io.github.miksask.linkagram.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.miksask.linkagram.data.history.HistoryRepository
import io.github.miksask.linkagram.domain.HistoryEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HistoryDetailsUiState(
    val isLoading: Boolean = true,
    val entry: HistoryEntry? = null,
    val missing: Boolean = false,
    val operationInProgress: Boolean = false,
    val deletedEntry: HistoryEntry? = null,
    val navigateBack: Boolean = false,
)

class HistoryDetailsViewModel(
    private val entryId: String,
    private val historyRepository: HistoryRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HistoryDetailsUiState())
    val uiState: StateFlow<HistoryDetailsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, missing = false) }
            val entry = historyRepository.getEntry(entryId)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    entry = entry,
                    missing = entry == null,
                )
            }
        }
    }

    fun delete() {
        if (_uiState.value.operationInProgress) return
        viewModelScope.launch {
            _uiState.update { it.copy(operationInProgress = true) }
            val deleted = historyRepository.deleteById(entryId)
            _uiState.update {
                it.copy(
                    operationInProgress = false,
                    deletedEntry = deleted,
                    entry = null,
                    navigateBack = true,
                )
            }
        }
    }

    fun consumeNavigateBack() {
        _uiState.update { it.copy(navigateBack = false) }
    }

    class Factory(
        private val entryId: String,
        private val historyRepository: HistoryRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HistoryDetailsViewModel::class.java)) {
                return HistoryDetailsViewModel(entryId, historyRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}
