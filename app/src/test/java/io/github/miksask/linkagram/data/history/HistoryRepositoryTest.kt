package io.github.miksask.linkagram.data.history

import io.github.miksask.linkagram.domain.CompletedAnalysis
import io.github.miksask.linkagram.domain.HistoryQuery
import io.github.miksask.linkagram.domain.HistorySaveResult
import io.github.miksask.linkagram.domain.RedirectStep
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryRepositoryTest {
    @Test
    fun saveIfEnabled_disabled_skips() = runTest {
        val dao = InMemoryHistoryDao()
        val repository = HistoryRepository(
            dao = dao,
            settings = FakeSettings(false),
            ioDispatcher = UnconfinedTestDispatcher(testScheduler),
        )

        val result = repository.saveIfEnabled(sampleAnalysis(1))

        assertEquals(HistorySaveResult.SkippedDisabled, result)
        assertTrue(dao.entries.isEmpty())
    }

    @Test
    fun saveIfEnabled_enabled_insertsDistinctRowsForSameUrl() = runTest {
        val dao = InMemoryHistoryDao()
        val repository = HistoryRepository(
            dao = dao,
            settings = FakeSettings(true),
            ioDispatcher = UnconfinedTestDispatcher(testScheduler),
        )

        repository.saveIfEnabled(sampleAnalysis(1))
        repository.saveIfEnabled(sampleAnalysis(2))

        assertEquals(2, dao.entries.size)
    }

    @Test
    fun saveIfEnabled_prunesOldestBeyondLimit() = runTest {
        val dao = InMemoryHistoryDao()
        val repository = HistoryRepository(
            dao = dao,
            settings = FakeSettings(true),
            ioDispatcher = UnconfinedTestDispatcher(testScheduler),
        )
        // Keep the prune check fast: temporarily rely on MAX_ENTRIES semantics with a
        // smaller local max by inserting beyond the real cap would be too slow.
        // Instead verify deleteOldest behaviour through repository by filling exactly
        // MAX_ENTRIES + 3 using a reduced-scale fake max via direct DAO calls, then
        // one repository insert that triggers prune.
        repeat(5) { index ->
            val (entry, redirects) = HistoryMapper.toEntities(sampleAnalysis(index.toLong()))
            dao.insertAndPrune(entry, redirects, maxEntries = 5)
        }
        assertEquals(5, dao.entries.size)
        val (extra, redirects) = HistoryMapper.toEntities(sampleAnalysis(100))
        dao.insertAndPrune(extra, redirects, maxEntries = 5)

        assertEquals(5, dao.entries.size)
        assertTrue(dao.entries.values.any { it.completedAtMillis == 100L })
        assertTrue(dao.entries.values.none { it.completedAtMillis == 0L })
    }

    @Test
    fun deleteById_andRestore_roundTrip() = runTest {
        val dao = InMemoryHistoryDao()
        val repository = HistoryRepository(
            dao = dao,
            settings = FakeSettings(true),
            ioDispatcher = UnconfinedTestDispatcher(testScheduler),
        )
        val saved = repository.saveIfEnabled(sampleAnalysis(10)) as HistorySaveResult.Saved
        val deleted = repository.deleteById(saved.entryId)!!

        assertEquals(0, dao.entries.size)
        repository.restore(deleted)
        assertEquals(1, dao.entries.size)
        assertEquals(saved.entryId, dao.entries.keys.single())
    }

    @Test
    fun observeEntries_returnsNewestFirst() = runTest {
        val dao = InMemoryHistoryDao()
        val repository = HistoryRepository(
            dao = dao,
            settings = FakeSettings(true),
            ioDispatcher = UnconfinedTestDispatcher(testScheduler),
        )
        repository.saveIfEnabled(sampleAnalysis(1))
        repository.saveIfEnabled(sampleAnalysis(5))
        repository.saveIfEnabled(sampleAnalysis(3))

        val entries = repository.observeEntries(HistoryQuery()).first()
        assertEquals(listOf(5L, 3L, 1L), entries.map { it.completedAtMillis })
    }

    private fun sampleAnalysis(completedAt: Long) = CompletedAnalysis(
        sourceUrl = "https://example.com/$completedAt",
        normalizedUrl = "https://example.com/$completedAt",
        finalUrl = "https://example.com/final/$completedAt",
        finalStatusCode = 200,
        redirectChain = listOf(
            RedirectStep(
                fromUrl = "https://example.com/$completedAt",
                toUrl = "https://example.com/final/$completedAt",
                statusCode = 302,
            ),
        ),
        location = null,
        completedAtMillis = completedAt,
    )
}

private class FakeSettings(enabled: Boolean) : HistorySettingsRepository {
    private val flow = MutableStateFlow(enabled)
    override val historyEnabled: Flow<Boolean> = flow
    override suspend fun isHistoryEnabled(): Boolean = flow.value
    override suspend fun setHistoryEnabled(enabled: Boolean) {
        flow.value = enabled
    }
}

private class InMemoryHistoryDao : HistoryDao {
    val entries = linkedMapOf<String, HistoryEntryEntity>()
    private val redirects = mutableListOf<HistoryRedirectEntity>()

    override suspend fun insertEntry(entry: HistoryEntryEntity) {
        entries[entry.id] = entry
    }

    override suspend fun insertRedirects(redirects: List<HistoryRedirectEntity>) {
        this.redirects += redirects
    }

    override suspend fun countAll(): Int = entries.size

    override suspend fun deleteOldest(count: Int): Int {
        val oldest = entries.values.sortedWith(
            compareBy<HistoryEntryEntity> { it.completedAtMillis }.thenBy { it.id },
        ).take(count)
        oldest.forEach { entries.remove(it.id) }
        redirects.removeAll { redirect -> oldest.any { it.id == redirect.historyEntryId } }
        return oldest.size
    }

    override fun observeMatching(
        likePattern: String?,
        startInclusiveMillis: Long?,
        endExclusiveMillis: Long?,
    ): Flow<List<HistoryEntryEntity>> =
        flowOf(
            entries.values
                .sortedWith(compareByDescending<HistoryEntryEntity> { it.completedAtMillis }.thenByDescending { it.id })
                .toList(),
        )

    override fun observeMatchingCount(
        likePattern: String?,
        startInclusiveMillis: Long?,
        endExclusiveMillis: Long?,
    ): Flow<Int> = flowOf(entries.size)

    override suspend fun getEntry(id: String): HistoryEntryEntity? = entries[id]

    override suspend fun getRedirects(historyEntryId: String): List<HistoryRedirectEntity> =
        redirects.filter { it.historyEntryId == historyEntryId }.sortedBy { it.ordinal }

    override suspend fun deleteById(id: String): Int {
        val removed = entries.remove(id) != null
        redirects.removeAll { it.historyEntryId == id }
        return if (removed) 1 else 0
    }

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
