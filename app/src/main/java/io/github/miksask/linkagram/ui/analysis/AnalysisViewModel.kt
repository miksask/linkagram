package io.github.miksask.linkagram.ui.analysis

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class AnalysisUiState(
    val draftUrl: String = "",
)

class AnalysisViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AnalysisUiState())
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()

    fun onDraftUrlChanged(value: String) {
        _uiState.update { it.copy(draftUrl = value) }
    }
}
