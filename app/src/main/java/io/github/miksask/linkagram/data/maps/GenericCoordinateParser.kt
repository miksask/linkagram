package io.github.miksask.linkagram.data.maps

import io.github.miksask.linkagram.domain.LocationInfo
import io.github.miksask.linkagram.domain.MapParseResult
import io.github.miksask.linkagram.domain.MapProvider
import okhttp3.HttpUrl

internal object GenericCoordinateParser : MapProviderParser {
    private val latKeys = listOf("lat", "latitude", "mlat")
    private val lonKeys = listOf("lon", "lng", "long", "longitude", "mlon")

    override fun parse(url: HttpUrl): MapParseResult? {
        val lat = latKeys.firstNotNullOfOrNull { key ->
            url.queryParameter(key)?.toDoubleOrNull()
        }
        val lon = lonKeys.firstNotNullOfOrNull { key ->
            url.queryParameter(key)?.toDoubleOrNull()
        }
        val coords = CoordinateParsing.validOrNull(lat, lon) ?: return null

        return MapParseResult.Parsed(
            LocationInfo(
                provider = MapProvider.Generic,
                latitude = coords.first,
                longitude = coords.second,
            ),
        )
    }
}
