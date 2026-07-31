package io.github.miksask.linkagram.ui.analysis

import org.junit.Assert.assertEquals
import org.junit.Test

class AnalysisViewModelTest {
    @Test
    fun initialState_hasEmptyDraftUrl() {
        val viewModel = AnalysisViewModel()

        assertEquals("", viewModel.uiState.value.draftUrl)
    }

    @Test
    fun onDraftUrlChanged_updatesDraftUrl() {
        val viewModel = AnalysisViewModel()

        viewModel.onDraftUrlChanged("https://example.com/maps")

        assertEquals("https://example.com/maps", viewModel.uiState.value.draftUrl)
    }
}
