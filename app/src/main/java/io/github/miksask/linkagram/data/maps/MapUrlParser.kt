package io.github.miksask.linkagram.data.maps

import io.github.miksask.linkagram.domain.MapParseResult
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

class MapUrlParser {
    private val parsers: List<MapProviderParser> = listOf(
        GoogleMapsParser,
        YandexMapsParser,
        OpenStreetMapParser,
        AppleMapsParser,
        OrganicMapsParser,
        GenericCoordinateParser,
    )

    fun parse(url: String): MapParseResult {
        val httpUrl = url.toHttpUrlOrNull() ?: return MapParseResult.Unsupported
        for (parser in parsers) {
            val result = parser.parse(httpUrl)
            if (result != null) return result
        }
        return MapParseResult.Unsupported
    }
}
