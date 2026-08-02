package io.github.miksask.linkagram.data.history

import io.github.miksask.linkagram.core.url.SearchNormalizer
import io.github.miksask.linkagram.domain.CompletedAnalysis
import io.github.miksask.linkagram.domain.HistoryEntry
import io.github.miksask.linkagram.domain.HistoryRedirect
import io.github.miksask.linkagram.domain.HistoryResultType
import io.github.miksask.linkagram.domain.MapProvider
import java.util.UUID

object HistoryMapper {
    const val MAX_ENTRIES = 5_000
    const val RECORD_VERSION = 1

    fun toEntities(
        analysis: CompletedAnalysis,
        id: String = UUID.randomUUID().toString(),
    ): Pair<HistoryEntryEntity, List<HistoryRedirectEntity>> {
        val hasMap = analysis.location != null
        val entry = HistoryEntryEntity(
            id = id,
            completedAtMillis = analysis.completedAtMillis,
            sourceUrl = analysis.sourceUrl,
            normalizedUrl = analysis.normalizedUrl,
            finalUrl = analysis.finalUrl,
            finalStatusCode = analysis.finalStatusCode,
            resultType = if (hasMap) HistoryResultType.Map.name else HistoryResultType.Url.name,
            provider = analysis.location?.provider?.name,
            placeName = analysis.location?.placeName,
            address = analysis.location?.address,
            latitude = analysis.location?.latitude,
            longitude = analysis.location?.longitude,
            redirectCount = analysis.redirectChain.size,
            sourceUrlSearch = SearchNormalizer.normalize(analysis.sourceUrl),
            finalUrlSearch = SearchNormalizer.normalize(analysis.finalUrl),
            placeNameSearch = SearchNormalizer.normalize(analysis.location?.placeName),
            addressSearch = SearchNormalizer.normalize(analysis.location?.address),
            recordVersion = RECORD_VERSION,
        )
        val redirects = analysis.redirectChain.mapIndexed { index, step ->
            HistoryRedirectEntity(
                historyEntryId = id,
                ordinal = index,
                fromUrl = step.fromUrl,
                toUrl = step.toUrl,
                statusCode = step.statusCode,
            )
        }
        return entry to redirects
    }

    fun toDomain(
        entry: HistoryEntryEntity,
        redirects: List<HistoryRedirectEntity> = emptyList(),
    ): HistoryEntry =
        HistoryEntry(
            id = entry.id,
            completedAtMillis = entry.completedAtMillis,
            sourceUrl = entry.sourceUrl,
            normalizedUrl = entry.normalizedUrl,
            finalUrl = entry.finalUrl,
            finalStatusCode = entry.finalStatusCode,
            resultType = runCatching { HistoryResultType.valueOf(entry.resultType) }
                .getOrDefault(HistoryResultType.Url),
            provider = entry.provider?.let { name ->
                runCatching { MapProvider.valueOf(name) }.getOrNull()
            },
            placeName = entry.placeName,
            address = entry.address,
            latitude = entry.latitude,
            longitude = entry.longitude,
            redirectCount = entry.redirectCount,
            redirectChain = redirects
                .sortedBy { it.ordinal }
                .map {
                    HistoryRedirect(
                        ordinal = it.ordinal,
                        fromUrl = it.fromUrl,
                        toUrl = it.toUrl,
                        statusCode = it.statusCode,
                    )
                },
            recordVersion = entry.recordVersion,
        )

    fun toEntities(entry: HistoryEntry): Pair<HistoryEntryEntity, List<HistoryRedirectEntity>> {
        val entity = HistoryEntryEntity(
            id = entry.id,
            completedAtMillis = entry.completedAtMillis,
            sourceUrl = entry.sourceUrl,
            normalizedUrl = entry.normalizedUrl,
            finalUrl = entry.finalUrl,
            finalStatusCode = entry.finalStatusCode,
            resultType = entry.resultType.name,
            provider = entry.provider?.name,
            placeName = entry.placeName,
            address = entry.address,
            latitude = entry.latitude,
            longitude = entry.longitude,
            redirectCount = entry.redirectCount,
            sourceUrlSearch = SearchNormalizer.normalize(entry.sourceUrl),
            finalUrlSearch = SearchNormalizer.normalize(entry.finalUrl),
            placeNameSearch = SearchNormalizer.normalize(entry.placeName),
            addressSearch = SearchNormalizer.normalize(entry.address),
            recordVersion = entry.recordVersion,
        )
        val redirects = entry.redirectChain.map {
            HistoryRedirectEntity(
                historyEntryId = entry.id,
                ordinal = it.ordinal,
                fromUrl = it.fromUrl,
                toUrl = it.toUrl,
                statusCode = it.statusCode,
            )
        }
        return entity to redirects
    }
}
