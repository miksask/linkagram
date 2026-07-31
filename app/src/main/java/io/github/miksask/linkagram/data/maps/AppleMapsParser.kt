package io.github.miksask.linkagram.data.maps

import io.github.miksask.linkagram.domain.LocationInfo
import io.github.miksask.linkagram.domain.MapParseResult
import io.github.miksask.linkagram.domain.MapProvider
import okhttp3.HttpUrl

internal object AppleMapsParser : MapProviderParser {
    override fun parse(url: HttpUrl): MapParseResult? {
        if (!matches(url)) return null

        val coords = url.queryParameter("ll")?.let { CoordinateParsing.parseLatLonPair(it) }
        val placeName = url.queryParameter("q")
        val address = url.queryParameter("address")

        return MapParseResult.Parsed(
            LocationInfo(
                provider = MapProvider.AppleMaps,
                placeName = placeName,
                address = address,
                latitude = coords?.first,
                longitude = coords?.second,
            ),
        )
    }

    private fun matches(url: HttpUrl): Boolean {
        val host = url.host.lowercase()
        return host == "maps.apple.com" || host.endsWith(".maps.apple.com")
    }
}
