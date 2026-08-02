package io.github.miksask.linkagram.ui.analysis

import io.github.miksask.linkagram.data.history.HistoryDao
import io.github.miksask.linkagram.data.history.HistoryEntryEntity
import io.github.miksask.linkagram.data.history.HistoryRedirectEntity
import io.github.miksask.linkagram.data.history.HistoryRepository
import io.github.miksask.linkagram.data.history.HistorySettingsRepository
import io.github.miksask.linkagram.domain.GeocodeResult
import io.github.miksask.linkagram.domain.ResolveResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
                        finalStatusCode = 200,
                        redirectChain = emptyList(),
                    )
                },
                ioDispatcher = dispatcher,
            )
            viewModel.onDraftUrlChanged("example.com/path")

            viewModel.analyze()
            advanceUntilIdle()

            assertEquals("https://example.com/path", viewModel.uiState.value.finalUrl)
            assertEquals(200, viewModel.uiState.value.finalStatusCode)
            assertEquals("example.com/path", viewModel.uiState.value.sourceUrl)
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
                        finalStatusCode = 200,
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
            assertEquals(GeocodeState.Unavailable, viewModel.uiState.value.geocodeState)
            assertFalse(viewModel.uiState.value.coordinatesAreApproximate)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun analyze_mapUrlWithoutCoords_setsGeocodeAvailable() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val viewModel = AnalysisViewModel(
                resolveUrl = {
                    ResolveResult.Success(
                        finalUrl =
                            "https://www.google.com/maps/place/Centrum," +
                                "+Stefana+%C5%BBeromskiego+115,+90-542+%C5%81%C3%B3d%C5%BA/" +
                                "data=!4m2!3m1!1s0x471a352808de581d:0x9eac1c1927024e88",
                        finalStatusCode = 200,
                        redirectChain = emptyList(),
                    )
                },
                ioDispatcher = dispatcher,
            )
            viewModel.onDraftUrlChanged("https://maps.app.goo.gl/example")

            viewModel.analyze()
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.coordinatesText)
            assertEquals(GeocodeState.Available, viewModel.uiState.value.geocodeState)
            assertFalse(viewModel.uiState.value.coordinatesAreApproximate)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun onFindCoordinates_success_setsApproximateCoordinates() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val viewModel = AnalysisViewModel(
                resolveUrl = {
                    ResolveResult.Success(
                        finalUrl =
                            "https://www.google.com/maps/place/Centrum," +
                                "+Stefana+%C5%BBeromskiego+115,+90-542+%C5%81%C3%B3d%C5%BA/" +
                                "data=!4m2!3m1!1s0x471a352808de581d:0x9eac1c1927024e88",
                        finalStatusCode = 200,
                        redirectChain = emptyList(),
                    )
                },
                geocode = { _, _ ->
                    GeocodeResult.Found(latitude = 51.7554125, longitude = 19.4463773)
                },
                ioDispatcher = dispatcher,
            )
            viewModel.onDraftUrlChanged("https://maps.app.goo.gl/example")
            viewModel.analyze()
            advanceUntilIdle()

            viewModel.onFindCoordinates()
            advanceUntilIdle()

            assertEquals("51.7554125, 19.4463773", viewModel.uiState.value.coordinatesText)
            assertTrue(viewModel.uiState.value.coordinatesAreApproximate)
            assertEquals(GeocodeState.Unavailable, viewModel.uiState.value.geocodeState)
            assertEquals(51.7554125, viewModel.uiState.value.location?.latitude)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun onFindCoordinates_success_withHistoryEnabled_updatesSavedEntry() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dao = FakeHistoryDao()
            val repository = HistoryRepository(
                dao = dao,
                settings = FakeHistorySettings(enabled = true),
                ioDispatcher = dispatcher,
            )
            val viewModel = AnalysisViewModel(
                resolveUrl = {
                    ResolveResult.Success(
                        finalUrl =
                            "https://www.google.com/maps/place/Centrum," +
                                "+Stefana+%C5%BBeromskiego+115,+90-542+%C5%81%C3%B3d%C5%BA/" +
                                "data=!4m2!3m1!1s0x471a352808de581d:0x9eac1c1927024e88",
                        finalStatusCode = 200,
                        redirectChain = emptyList(),
                    )
                },
                geocode = { _, _ ->
                    GeocodeResult.Found(latitude = 51.7554125, longitude = 19.4463773)
                },
                historyRepository = repository,
                ioDispatcher = dispatcher,
            )
            viewModel.onDraftUrlChanged("https://maps.app.goo.gl/example")
            viewModel.analyze()
            advanceUntilIdle()

            assertEquals(1, dao.entries.size)
            assertNull(dao.entries.values.single().latitude)

            viewModel.onFindCoordinates()
            advanceUntilIdle()

            val entry = dao.entries.values.single()
            assertEquals(51.7554125, entry.latitude)
            assertEquals(19.4463773, entry.longitude)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun onFindCoordinates_notFound_setsGeocodeNotFound() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val viewModel = AnalysisViewModel(
                resolveUrl = {
                    ResolveResult.Success(
                        finalUrl =
                            "https://www.google.com/maps/place/Centrum," +
                                "+Stefana+%C5%BBeromskiego+115,+90-542+%C5%81%C3%B3d%C5%BA/" +
                                "data=!4m2!3m1!1s0x471a352808de581d:0x9eac1c1927024e88",
                        finalStatusCode = 200,
                        redirectChain = emptyList(),
                    )
                },
                geocode = { _, _ -> GeocodeResult.NotFound },
                ioDispatcher = dispatcher,
            )
            viewModel.onDraftUrlChanged("https://maps.app.goo.gl/example")
            viewModel.analyze()
            advanceUntilIdle()

            viewModel.onFindCoordinates()
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.coordinatesText)
            assertEquals(GeocodeState.NotFound, viewModel.uiState.value.geocodeState)
            assertFalse(viewModel.uiState.value.coordinatesAreApproximate)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun onDraftUrlChanged_clearsApproximateCoordinates() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val viewModel = AnalysisViewModel(
                resolveUrl = {
                    ResolveResult.Success(
                        finalUrl =
                            "https://www.google.com/maps/place/Centrum," +
                                "+Stefana+%C5%BBeromskiego+115,+90-542+%C5%81%C3%B3d%C5%BA/" +
                                "data=!4m2!3m1!1s0x471a352808de581d:0x9eac1c1927024e88",
                        finalStatusCode = 200,
                        redirectChain = emptyList(),
                    )
                },
                geocode = { _, _ ->
                    GeocodeResult.Found(latitude = 51.7554125, longitude = 19.4463773)
                },
                ioDispatcher = dispatcher,
            )
            viewModel.onDraftUrlChanged("https://maps.app.goo.gl/example")
            viewModel.analyze()
            advanceUntilIdle()
            viewModel.onFindCoordinates()
            advanceUntilIdle()

            viewModel.onDraftUrlChanged("https://example.com")

            assertNull(viewModel.uiState.value.coordinatesText)
            assertFalse(viewModel.uiState.value.coordinatesAreApproximate)
            assertEquals(GeocodeState.Unavailable, viewModel.uiState.value.geocodeState)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun analyze_success_withHistoryEnabled_savesEntry() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dao = FakeHistoryDao()
            val repository = HistoryRepository(
                dao = dao,
                settings = FakeHistorySettings(enabled = true),
                ioDispatcher = dispatcher,
            )
            val viewModel = AnalysisViewModel(
                resolveUrl = {
                    ResolveResult.Success(
                        finalUrl = "https://example.com/final",
                        finalStatusCode = 200,
                        redirectChain = emptyList(),
                    )
                },
                historyRepository = repository,
                ioDispatcher = dispatcher,
            )
            viewModel.onDraftUrlChanged("https://example.com")

            viewModel.analyze()
            advanceUntilIdle()

            assertEquals(1, dao.entries.size)
            assertEquals(HistorySaveNotice.Saved, viewModel.uiState.value.historySaveNotice)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun analyze_success_withHistoryDisabled_doesNotSave() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dao = FakeHistoryDao()
            val repository = HistoryRepository(
                dao = dao,
                settings = FakeHistorySettings(enabled = false),
                ioDispatcher = dispatcher,
            )
            val viewModel = AnalysisViewModel(
                resolveUrl = {
                    ResolveResult.Success(
                        finalUrl = "https://example.com/final",
                        finalStatusCode = 200,
                        redirectChain = emptyList(),
                    )
                },
                historyRepository = repository,
                ioDispatcher = dispatcher,
            )
            viewModel.onDraftUrlChanged("https://example.com")

            viewModel.analyze()
            advanceUntilIdle()

            assertTrue(dao.entries.isEmpty())
            assertNull(viewModel.uiState.value.historySaveNotice)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun analyze_timeout_doesNotSave() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dao = FakeHistoryDao()
            val repository = HistoryRepository(
                dao = dao,
                settings = FakeHistorySettings(enabled = true),
                ioDispatcher = dispatcher,
            )
            val viewModel = AnalysisViewModel(
                resolveUrl = { ResolveResult.Timeout },
                historyRepository = repository,
                ioDispatcher = dispatcher,
            )
            viewModel.onDraftUrlChanged("https://example.com")

            viewModel.analyze()
            advanceUntilIdle()

            assertTrue(dao.entries.isEmpty())
            assertEquals(ResolveError.Timeout, viewModel.uiState.value.resolveError)
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

private class FakeHistorySettings(
    enabled: Boolean,
) : HistorySettingsRepository {
    private val enabledFlow = MutableStateFlow(enabled)
    override val historyEnabled: Flow<Boolean> = enabledFlow
    override suspend fun isHistoryEnabled(): Boolean = enabledFlow.value
    override suspend fun setHistoryEnabled(enabled: Boolean) {
        enabledFlow.value = enabled
    }
}

private class FakeHistoryDao : HistoryDao {
    val entries = linkedMapOf<String, HistoryEntryEntity>()
    val redirects = mutableListOf<HistoryRedirectEntity>()

    override suspend fun insertEntry(entry: HistoryEntryEntity) {
        entries[entry.id] = entry
    }

    override suspend fun insertRedirects(redirects: List<HistoryRedirectEntity>) {
        this.redirects += redirects
    }

    override suspend fun countAll(): Int = entries.size

    override suspend fun deleteOldest(count: Int): Int {
        val toRemove = entries.values.sortedBy { it.completedAtMillis }.take(count)
        toRemove.forEach { entries.remove(it.id) }
        return toRemove.size
    }

    override fun observeMatching(
        likePattern: String?,
        startInclusiveMillis: Long?,
        endExclusiveMillis: Long?,
    ): Flow<List<HistoryEntryEntity>> = flowOf(entries.values.toList())

    override fun observeMatchingCount(
        likePattern: String?,
        startInclusiveMillis: Long?,
        endExclusiveMillis: Long?,
    ): Flow<Int> = flowOf(entries.size)

    override suspend fun getEntry(id: String): HistoryEntryEntity? = entries[id]

    override suspend fun updateCoordinates(
        id: String,
        latitude: Double,
        longitude: Double,
    ): Int {
        val existing = entries[id] ?: return 0
        entries[id] = existing.copy(latitude = latitude, longitude = longitude)
        return 1
    }

    override suspend fun getRedirects(historyEntryId: String): List<HistoryRedirectEntity> =
        redirects.filter { it.historyEntryId == historyEntryId }

    override suspend fun deleteById(id: String): Int =
        if (entries.remove(id) != null) 1 else 0

    override suspend fun deleteAll(): Int {
        val size = entries.size
        entries.clear()
        redirects.clear()
        return size
    }

    override suspend fun deleteMatching(
        likePattern: String?,
        startInclusiveMillis: Long?,
        endExclusiveMillis: Long?,
    ): Int = deleteAll()
}
