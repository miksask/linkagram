package io.github.miksask.linkagram.data.maps

import io.github.miksask.linkagram.domain.LocationInfo
import io.github.miksask.linkagram.domain.MapParseResult
import io.github.miksask.linkagram.domain.MapProvider
import okhttp3.HttpUrl

internal object YandexMapsParser : MapProviderParser {
    override fun parse(url: HttpUrl): MapParseResult? {
        if (!matches(url)) return null

        val ll = url.queryParameter("ll")?.let { CoordinateParsing.parseLatLonPair(it, latFirst = false) }
        val pt = url.queryParameter("pt")
            ?.substringBefore('~')
            ?.let { CoordinateParsing.parseLatLonPair(it, latFirst = false) }
        val coords = ll ?: pt

        return MapParseResult.Parsed(
            LocationInfo(
                provider = MapProvider.YandexMaps,
                placeName = url.queryParameter("text"),
                latitude = coords?.first,
                longitude = coords?.second,
            ),
        )
    }

    private fun matches(url: HttpUrl): Boolean {
        val host = url.host.lowercase()
        val path = url.encodedPath.lowercase()
        return (host.contains("yandex.") || host.contains("ya.ru")) &&
            (path.contains("/maps") || host.startsWith("maps.yandex.") || host.startsWith("map.yandex."))
    }
}
