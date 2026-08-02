package io.github.miksask.linkagram.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.miksask.linkagram.data.history.HistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val historyEnabled: Boolean = false,
    val totalCount: Int = 0,
    val confirmClearAll: Boolean = false,
    val operationInProgress: Boolean = false,
    val clearedCount: Int? = null,
)

class SettingsViewModel(
    private val historyRepository: HistoryRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            historyRepository.historyEnabled.collect { enabled ->
                _uiState.update { it.copy(historyEnabled = enabled) }
            }
        }
        refreshCount()
    }

    fun setHistoryEnabled(enabled: Boolean) {
        viewModelScope.launch {
            historyRepository.setHistoryEnabled(enabled)
        }
    }

    fun showClearConfirmation() {
        viewModelScope.launch {
            val total = historyRepository.totalCount()
            _uiState.update {
                it.copy(totalCount = total, confirmClearAll = total > 0)
            }
        }
    }

    fun dismissClearConfirmation() {
        _uiState.update { it.copy(confirmClearAll = false) }
    }

    fun confirmClearAll() {
        if (_uiState.value.operationInProgress) return
        viewModelScope.launch {
            _uiState.update { it.copy(operationInProgress = true, confirmClearAll = false) }
            val count = historyRepository.clearAll()
            _uiState.update {
                it.copy(
                    operationInProgress = false,
                    totalCount = 0,
                    clearedCount = count,
                )
            }
        }
    }

    fun consumeClearedCount() {
        _uiState.update { it.copy(clearedCount = null) }
    }

    fun refreshCount() {
        viewModelScope.launch {
            val total = historyRepository.totalCount()
            _uiState.update { it.copy(totalCount = total) }
        }
    }

    class Factory(
        private val historyRepository: HistoryRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                return SettingsViewModel(historyRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}
