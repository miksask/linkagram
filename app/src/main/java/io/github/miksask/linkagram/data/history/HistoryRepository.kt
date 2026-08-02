package io.github.miksask.linkagram.data.history

import io.github.miksask.linkagram.core.time.HistoryDateRangeCalculator
import io.github.miksask.linkagram.core.url.SearchNormalizer
import io.github.miksask.linkagram.domain.CompletedAnalysis
import io.github.miksask.linkagram.domain.HistoryEntry
import io.github.miksask.linkagram.domain.HistoryQuery
import io.github.miksask.linkagram.domain.HistorySaveResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class HistoryRepository(
    private val dao: HistoryDao,
    private val settings: HistorySettingsRepository,
    private val dateRangeCalculator: HistoryDateRangeCalculator = HistoryDateRangeCalculator(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    val historyEnabled: Flow<Boolean> = settings.historyEnabled

    suspend fun setHistoryEnabled(enabled: Boolean) {
        withContext(ioDispatcher) {
            settings.setHistoryEnabled(enabled)
        }
    }

    suspend fun saveIfEnabled(analysis: CompletedAnalysis): HistorySaveResult =
        withContext(ioDispatcher) {
            if (!settings.isHistoryEnabled()) {
                return@withContext HistorySaveResult.SkippedDisabled
            }
            try {
                val (entry, redirects) = HistoryMapper.toEntities(analysis)
                dao.insertAndPrune(entry, redirects, HistoryMapper.MAX_ENTRIES)
                HistorySaveResult.Saved(entry.id)
            } catch (e: Exception) {
                HistorySaveResult.Failed(e.message)
            }
        }

    fun observeEntries(query: HistoryQuery): Flow<List<HistoryEntry>> {
        val like = SearchNormalizer.toLikePattern(query.searchText)
        val bounds = dateRangeCalculator.bounds(
            filter = query.dateFilter,
            customStartInclusiveMillis = query.customStartDateInclusiveMillis,
            customEndExclusiveMillis = query.customEndExclusiveMillis,
        )
        return dao.observeMatching(
            likePattern = like,
            startInclusiveMillis = bounds.startInclusiveMillis,
            endExclusiveMillis = bounds.endExclusiveMillis,
        ).map { entities ->
            entities.map { entity -> HistoryMapper.toDomain(entity) }
        }.flowOn(ioDispatcher)
    }

    fun observeCount(query: HistoryQuery): Flow<Int> {
        val like = SearchNormalizer.toLikePattern(query.searchText)
        val bounds = dateRangeCalculator.bounds(
            filter = query.dateFilter,
            customStartInclusiveMillis = query.customStartDateInclusiveMillis,
            customEndExclusiveMillis = query.customEndExclusiveMillis,
        )
        return dao.observeMatchingCount(
            likePattern = like,
            startInclusiveMillis = bounds.startInclusiveMillis,
            endExclusiveMillis = bounds.endExclusiveMillis,
        ).flowOn(ioDispatcher)
    }

    fun observeEntriesWithCount(query: HistoryQuery): Flow<Pair<List<HistoryEntry>, Int>> =
        combine(observeEntries(query), observeCount(query)) { entries, count ->
            entries to count
        }

    suspend fun getEntry(id: String): HistoryEntry? =
        withContext(ioDispatcher) {
            val entry = dao.getEntry(id) ?: return@withContext null
            val redirects = dao.getRedirects(id)
            HistoryMapper.toDomain(entry, redirects)
        }

    suspend fun deleteById(id: String): HistoryEntry? =
        withContext(ioDispatcher) {
            val entry = dao.getEntry(id) ?: return@withContext null
            val redirects = dao.getRedirects(id)
            val domain = HistoryMapper.toDomain(entry, redirects)
            dao.deleteById(id)
            domain
        }

    suspend fun restore(entry: HistoryEntry) {
        withContext(ioDispatcher) {
            val (entity, redirects) = HistoryMapper.toEntities(entry)
            dao.restoreEntry(entity, redirects)
        }
    }

    suspend fun clearAll(): Int =
        withContext(ioDispatcher) {
            dao.deleteAll()
        }

    suspend fun deleteMatching(query: HistoryQuery): Int =
        withContext(ioDispatcher) {
            val like = SearchNormalizer.toLikePattern(query.searchText)
            val bounds = dateRangeCalculator.bounds(
                filter = query.dateFilter,
                customStartInclusiveMillis = query.customStartDateInclusiveMillis,
                customEndExclusiveMillis = query.customEndExclusiveMillis,
            )
            dao.deleteMatching(
                likePattern = like,
                startInclusiveMillis = bounds.startInclusiveMillis,
                endExclusiveMillis = bounds.endExclusiveMillis,
            )
        }

    suspend fun totalCount(): Int =
        withContext(ioDispatcher) {
            dao.countAll()
        }
}
