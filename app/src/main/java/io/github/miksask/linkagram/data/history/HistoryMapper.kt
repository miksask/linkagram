package io.github.miksask.linkagram.data.history

import io.github.miksask.linkagram.core.url.SearchNormalizer
import io.github.miksask.linkagram.domain.CompletedAnalysis
import io.github.miksask.linkagram.domain.HistoryEntry
import io.github.miksask.linkagram.domain.HistoryRedirect
import io.github.miksask.linkagram.domain.HistoryResultType
import io.github.miksask.linkagram.domain.MapProvider
import io.github.miksask.linkagram.domain.RichLinkKind
import java.util.UUID

object HistoryMapper {
    const val MAX_ENTRIES = 5_000
    const val RECORD_VERSION = 1

    fun toEntities(
        analysis: CompletedAnalysis,
        id: String = UUID.randomUUID().toString(),
    ): Pair<HistoryEntryEntity, List<HistoryRedirectEntity>> {
        val resultType = when {
            analysis.location != null -> HistoryResultType.Map
            analysis.richLink != null -> HistoryResultType.RichLink
            else -> HistoryResultType.Url
        }
        val placeName = analysis.location?.placeName ?: analysis.richLink?.title
        val entry = HistoryEntryEntity(
            id = id,
            completedAtMillis = analysis.completedAtMillis,
            sourceUrl = analysis.sourceUrl,
            normalizedUrl = analysis.normalizedUrl,
            finalUrl = analysis.finalUrl,
            finalStatusCode = analysis.finalStatusCode,
            resultType = resultType.name,
            provider = when (resultType) {
                HistoryResultType.Map -> analysis.location?.provider?.name
                HistoryResultType.RichLink -> analysis.richLink?.kind?.name
                HistoryResultType.Url -> null
            },
            placeName = placeName,
            address = analysis.location?.address,
            latitude = analysis.location?.latitude,
            longitude = analysis.location?.longitude,
            redirectCount = analysis.redirectChain.size,
            sourceUrlSearch = SearchNormalizer.normalize(analysis.sourceUrl),
            finalUrlSearch = SearchNormalizer.normalize(analysis.finalUrl),
            placeNameSearch = SearchNormalizer.normalize(placeName),
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
    ): HistoryEntry {
        val resultType = runCatching { HistoryResultType.valueOf(entry.resultType) }
            .getOrDefault(HistoryResultType.Url)
        val richLinkKind = if (resultType == HistoryResultType.RichLink) {
            entry.provider?.let { name ->
                runCatching { RichLinkKind.valueOf(name) }.getOrNull()
            }
        } else {
            null
        }
        val mapProvider = if (resultType == HistoryResultType.Map) {
            entry.provider?.let { name ->
                runCatching { MapProvider.valueOf(name) }.getOrNull()
            }
        } else {
            null
        }
        return HistoryEntry(
            id = entry.id,
            completedAtMillis = entry.completedAtMillis,
            sourceUrl = entry.sourceUrl,
            normalizedUrl = entry.normalizedUrl,
            finalUrl = entry.finalUrl,
            finalStatusCode = entry.finalStatusCode,
            resultType = resultType,
            provider = mapProvider,
            richLinkKind = richLinkKind,
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
    }

    fun toEntities(entry: HistoryEntry): Pair<HistoryEntryEntity, List<HistoryRedirectEntity>> {
        val providerColumn = when (entry.resultType) {
            HistoryResultType.Map -> entry.provider?.name
            HistoryResultType.RichLink -> entry.richLinkKind?.name
            HistoryResultType.Url -> null
        }
        val entity = HistoryEntryEntity(
            id = entry.id,
            completedAtMillis = entry.completedAtMillis,
            sourceUrl = entry.sourceUrl,
            normalizedUrl = entry.normalizedUrl,
            finalUrl = entry.finalUrl,
            finalStatusCode = entry.finalStatusCode,
            resultType = entry.resultType.name,
            provider = providerColumn,
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
