package io.github.miksask.linkagram.domain

data class CompletedAnalysis(
    val sourceUrl: String,
    val normalizedUrl: String,
    val finalUrl: String,
    val finalStatusCode: Int,
    val redirectChain: List<RedirectStep>,
    val location: LocationInfo?,
    val richLink: RichLinkInfo? = null,
    val completedAtMillis: Long,
)

enum class HistoryResultType {
    Url,
    Map,
    RichLink,
}

data class HistoryRedirect(
    val ordinal: Int,
    val fromUrl: String,
    val toUrl: String?,
    val statusCode: Int?,
)

data class HistoryEntry(
    val id: String,
    val completedAtMillis: Long,
    val sourceUrl: String,
    val normalizedUrl: String,
    val finalUrl: String,
    val finalStatusCode: Int,
    val resultType: HistoryResultType,
    val provider: MapProvider?,
    val richLinkKind: RichLinkKind? = null,
    val placeName: String?,
    val address: String?,
    val latitude: Double?,
    val longitude: Double?,
    val redirectCount: Int,
    val redirectChain: List<HistoryRedirect> = emptyList(),
    val recordVersion: Int = 1,
) {
    val coordinatesText: String?
        get() {
            val lat = latitude ?: return null
            val lon = longitude ?: return null
            return CoordinateFormatter.format(lat, lon)
        }
}

enum class HistoryDateFilter {
    All,
    Today,
    Last7Days,
    Last30Days,
    Custom,
}

data class HistoryQuery(
    val searchText: String = "",
    val dateFilter: HistoryDateFilter = HistoryDateFilter.All,
    val customStartDateInclusiveMillis: Long? = null,
    val customEndExclusiveMillis: Long? = null,
)

sealed interface HistorySaveResult {
    data class Saved(val entryId: String) : HistorySaveResult
    data object SkippedDisabled : HistorySaveResult
    data class Failed(val message: String? = null) : HistorySaveResult
}
