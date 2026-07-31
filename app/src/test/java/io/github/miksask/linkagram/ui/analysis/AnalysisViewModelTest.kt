package io.github.miksask.linkagram.ui.analysis

import io.github.miksask.linkagram.domain.ResolveResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AnalysisViewModelTest {
    @Test
    fun initialState_hasEmptyDraftUrl() {
        val viewModel = AnalysisViewModel(
            resolveUrl = { ResolveResult.InvalidInput },
            ioDispatcher = StandardTestDispatcher(),
        )

        assertEquals("", viewModel.uiState.value.draftUrl)
        assertNull(viewModel.uiState.value.validationError)
    }

    @Test
    fun onDraftUrlChanged_updatesDraftUrl() {
        val viewModel = AnalysisViewModel(
            resolveUrl = { ResolveResult.InvalidInput },
            ioDispatcher = StandardTestDispatcher(),
        )

        viewModel.onDraftUrlChanged("https://example.com/maps")

        assertEquals("https://example.com/maps", viewModel.uiState.value.draftUrl)
    }

    @Test
    fun analyze_emptyInput_setsValidationError() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val viewModel = AnalysisViewModel(
                resolveUrl = { ResolveResult.InvalidInput },
                ioDispatcher = dispatcher,
            )

            viewModel.analyze()
            advanceUntilIdle()

            assertEquals(ValidationError.Empty, viewModel.uiState.value.validationError)
            assertNull(viewModel.uiState.value.finalUrl)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun analyze_validUrl_resolvesFinalUrl() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val viewModel = AnalysisViewModel(
                resolveUrl = {
                    ResolveResult.Success(
                        finalUrl = "https://example.com/path",
                        redirectChain = emptyList(),
                    )
                },
                ioDispatcher = dispatcher,
            )
            viewModel.onDraftUrlChanged("example.com/path")

            viewModel.analyze()
            advanceUntilIdle()

            assertEquals("https://example.com/path", viewModel.uiState.value.finalUrl)
            assertNull(viewModel.uiState.value.validationError)
            assertNull(viewModel.uiState.value.resolveError)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun analyze_mapUrl_extractsCoordinates() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val viewModel = AnalysisViewModel(
                resolveUrl = {
                    ResolveResult.Success(
                        finalUrl = "https://www.google.com/maps/@55.75,37.62,14z",
                        redirectChain = emptyList(),
                    )
                },
                ioDispatcher = dispatcher,
            )
            viewModel.onDraftUrlChanged("https://www.google.com/maps/@55.75,37.62,14z")

            viewModel.analyze()
            advanceUntilIdle()

            assertEquals("55.75, 37.62", viewModel.uiState.value.coordinatesText)
            assertEquals(55.75, viewModel.uiState.value.location?.latitude)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun onPasteFromClipboard_blank_setsEmptyError() {
        val viewModel = AnalysisViewModel(
            resolveUrl = { ResolveResult.InvalidInput },
            ioDispatcher = StandardTestDispatcher(),
        )

        viewModel.onPasteFromClipboard("  ")

        assertEquals(ValidationError.Empty, viewModel.uiState.value.validationError)
    }

    @Test
    fun onIncomingUrl_prefillsDraft() {
        val viewModel = AnalysisViewModel(
            resolveUrl = { ResolveResult.InvalidInput },
            ioDispatcher = StandardTestDispatcher(),
        )

        viewModel.onIncomingUrl("https://shared.example/x")

        assertEquals("https://shared.example/x", viewModel.uiState.value.draftUrl)
    }

    @Test
    fun analyze_resolveTimeout_setsResolveError() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val viewModel = AnalysisViewModel(
                resolveUrl = { ResolveResult.Timeout },
                ioDispatcher = dispatcher,
            )
            viewModel.onDraftUrlChanged("https://example.com")

            viewModel.analyze()
            advanceUntilIdle()

            assertEquals(ResolveError.Timeout, viewModel.uiState.value.resolveError)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
