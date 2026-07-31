package io.github.miksask.linkagram.data.maps

import io.github.miksask.linkagram.domain.LocationInfo
import io.github.miksask.linkagram.domain.MapParseResult
import io.github.miksask.linkagram.domain.MapProvider
import okhttp3.HttpUrl

internal object OpenStreetMapParser : MapProviderParser {
    override fun parse(url: HttpUrl): MapParseResult? {
        if (!matches(url)) return null

        val fromHash = url.fragment
            ?.let { Regex("""map=\d+/(-?\d+(?:\.\d+)?)/(-?\d+(?:\.\d+)?)""").find(it) }
            ?.let {
                CoordinateParsing.validOrNull(
                    it.groupValues[1].toDoubleOrNull(),
                    it.groupValues[2].toDoubleOrNull(),
                )
            }

        val fromQuery = CoordinateParsing.validOrNull(
            url.queryParameter("mlat")?.toDoubleOrNull(),
            url.queryParameter("mlon")?.toDoubleOrNull(),
        )

        val fromGeo = url.queryParameter("geo")?.let { CoordinateParsing.parseLatLonPair(it) }
        val coords = fromHash ?: fromQuery ?: fromGeo

        return MapParseResult.Parsed(
            LocationInfo(
                provider = MapProvider.OpenStreetMap,
                latitude = coords?.first,
                longitude = coords?.second,
            ),
        )
    }

    private fun matches(url: HttpUrl): Boolean {
        val host = url.host.lowercase()
        return host == "openstreetmap.org" ||
            host.endsWith(".openstreetmap.org") ||
            host == "osm.org" ||
            host.endsWith(".osm.org")
    }
}
