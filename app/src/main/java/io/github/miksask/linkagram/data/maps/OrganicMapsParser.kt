package io.github.miksask.linkagram.data.maps

import io.github.miksask.linkagram.domain.LocationInfo
import io.github.miksask.linkagram.domain.MapParseResult
import io.github.miksask.linkagram.domain.MapProvider
import okhttp3.HttpUrl
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

internal object OrganicMapsParser : MapProviderParser {
    private val reservedFirstSegments = setOf(
        "map",
        "route",
        "search",
        "crosshair",
        "api",
    )

    override fun parse(url: HttpUrl): MapParseResult? {
        if (!matches(url)) return null

        val segments = url.pathSegments.filter { it.isNotEmpty() }
        val clear = parseClearCoordinates(segments)
        if (clear != null) return clear

        if (segments.firstOrNull()?.lowercase() == "map") {
            return parseMapApi(url)
        }

        val ge0 = parseGe0(segments)
        if (ge0 != null) return ge0

        return MapParseResult.Parsed(
            LocationInfo(provider = MapProvider.OrganicMaps),
        )
    }

    private fun parseClearCoordinates(segments: List<String>): MapParseResult? {
        val first = segments.firstOrNull() ?: return null
        val coords = CoordinateParsing.parseLatLonPair(first) ?: return null
        val placeName = segments.getOrNull(1)?.let { normalizeName(it) }
        return MapParseResult.Parsed(
            LocationInfo(
                provider = MapProvider.OrganicMaps,
                placeName = placeName,
                latitude = coords.first,
                longitude = coords.second,
            ),
        )
    }

    private fun parseMapApi(url: HttpUrl): MapParseResult {
        val coords = url.queryParameter("ll")?.let { CoordinateParsing.parseLatLonPair(it) }
        val placeName = url.queryParameter("n")?.let { normalizeName(it) }
        return MapParseResult.Parsed(
            LocationInfo(
                provider = MapProvider.OrganicMaps,
                placeName = placeName,
                latitude = coords?.first,
                longitude = coords?.second,
            ),
        )
    }

    private fun parseGe0(segments: List<String>): MapParseResult? {
        val encoded = segments.firstOrNull() ?: return null
        if (encoded.lowercase() in reservedFirstSegments) return null
        val coords = Ge0Decoder.decode(encoded) ?: return null
        val placeName = segments.getOrNull(1)?.let { normalizeName(it) }
        return MapParseResult.Parsed(
            LocationInfo(
                provider = MapProvider.OrganicMaps,
                placeName = placeName,
                latitude = coords.first,
                longitude = coords.second,
            ),
        )
    }

    /**
     * Organic Maps share names use `_` / `+` as spaces; path segments from OkHttp
     * are already percent-decoded, but query values may still need decoding.
     */
    private fun normalizeName(raw: String): String? {
        val decoded = runCatching {
            URLDecoder.decode(raw, StandardCharsets.UTF_8)
        }.getOrDefault(raw)
        val normalized = decoded.replace('_', ' ').replace('+', ' ').trim()
        return normalized.takeIf { it.isNotEmpty() }
    }

    private fun matches(url: HttpUrl): Boolean {
        val host = url.host.lowercase()
        return host == "omaps.app" || host.endsWith(".omaps.app")
    }
}
