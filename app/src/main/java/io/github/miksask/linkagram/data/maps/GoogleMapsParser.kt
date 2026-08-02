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
        val fromData = CoordinateParsing.findDataCoordinates(pathAndFragment)
        val queryCandidate = url.queryParameter("q") ?: url.queryParameter("query")
        val fromQuery = queryCandidate?.let { CoordinateParsing.parseLatLonPair(it) }
        val coords = fromAt ?: fromData ?: fromQuery
        val place = extractPlace(url)

        return MapParseResult.Parsed(
            LocationInfo(
                provider = MapProvider.GoogleMaps,
                placeName = place?.name,
                address = place?.address,
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

    private data class PlaceParts(val name: String?, val address: String?)

    private fun extractPlace(url: HttpUrl): PlaceParts? {
        val segments = url.pathSegments.filter { it.isNotBlank() }
        val placeIndex = segments.indexOf("place")
        if (placeIndex >= 0 && placeIndex + 1 < segments.size) {
            val raw = segments[placeIndex + 1]
            if (!raw.startsWith("@") && !raw.startsWith("data=")) {
                val decoded = raw.replace('+', ' ')
                val commaIndex = decoded.indexOf(',')
                if (commaIndex > 0 && commaIndex + 1 < decoded.length) {
                    val name = decoded.substring(0, commaIndex).trim()
                    val address = decoded.substring(commaIndex + 1).trim()
                    if (name.isNotEmpty() && address.isNotEmpty()) {
                        return PlaceParts(name = name, address = address)
                    }
                }
                return PlaceParts(name = decoded, address = null)
            }
        }
        val q = url.queryParameter("q") ?: url.queryParameter("query")
        if (q != null && CoordinateParsing.parseLatLonPair(q) == null) {
            return PlaceParts(name = q, address = null)
        }
        return null
    }
}
