package io.github.miksask.linkagram.data.maps

import io.github.miksask.linkagram.domain.LocationInfo
import io.github.miksask.linkagram.domain.MapParseResult
import io.github.miksask.linkagram.domain.MapProvider
import okhttp3.HttpUrl

internal object GoogleMapsParser : MapProviderParser {
    override fun parse(url: HttpUrl): MapParseResult? {
        if (!matches(url)) return null

        val pathAndFragment = buildString {
            append(url.encodedPath)
            url.fragment?.let { append('#').append(it) }
        }
        val fromAt = CoordinateParsing.findAtCoordinates(pathAndFragment)
        val queryCandidate = url.queryParameter("q") ?: url.queryParameter("query")
        val fromQuery = queryCandidate?.let { CoordinateParsing.parseLatLonPair(it) }
        val coords = fromAt ?: fromQuery

        return MapParseResult.Parsed(
            LocationInfo(
                provider = MapProvider.GoogleMaps,
                placeName = extractPlaceName(url),
                latitude = coords?.first,
                longitude = coords?.second,
            ),
        )
    }

    private fun matches(url: HttpUrl): Boolean {
        val host = url.host.lowercase()
        val path = url.encodedPath.lowercase()
        return when {
            host == "maps.google.com" || host.startsWith("maps.google.") -> true
            host == "www.google.com" || host == "google.com" -> path.startsWith("/maps")
            host.endsWith(".google.com") && path.startsWith("/maps") -> true
            else -> false
        }
    }

    private fun extractPlaceName(url: HttpUrl): String? {
        val segments = url.pathSegments.filter { it.isNotBlank() }
        val placeIndex = segments.indexOf("place")
        if (placeIndex >= 0 && placeIndex + 1 < segments.size) {
            val raw = segments[placeIndex + 1]
            if (!raw.startsWith("@")) {
                return raw.replace('+', ' ')
            }
        }
        val q = url.queryParameter("q") ?: url.queryParameter("query")
        if (q != null && CoordinateParsing.parseLatLonPair(q) == null) {
            return q
        }
        return null
    }
}
