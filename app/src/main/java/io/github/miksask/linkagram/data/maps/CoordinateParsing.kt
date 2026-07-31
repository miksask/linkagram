package io.github.miksask.linkagram.data.maps

import io.github.miksask.linkagram.domain.CoordinateValidator
import okhttp3.HttpUrl

internal object CoordinateParsing {
    private val latLonPair = Regex(
        """^\s*(-?\d+(?:\.\d+)?)\s*,\s*(-?\d+(?:\.\d+)?)\s*$""",
    )

    fun parseLatLonPair(value: String, latFirst: Boolean = true): Pair<Double, Double>? {
        val match = latLonPair.matchEntire(value) ?: return null
        val first = match.groupValues[1].toDoubleOrNull() ?: return null
        val second = match.groupValues[2].toDoubleOrNull() ?: return null
        val (lat, lon) = if (latFirst) first to second else second to first
        return validOrNull(lat, lon)
    }

    fun validOrNull(lat: Double?, lon: Double?): Pair<Double, Double>? {
        if (lat == null || lon == null) return null
        return if (CoordinateValidator.isValid(lat, lon)) lat to lon else null
    }

    fun findAtCoordinates(text: String): Pair<Double, Double>? {
        val match = Regex("""@(-?\d+(?:\.\d+)?),(-?\d+(?:\.\d+)?)""").find(text) ?: return null
        return validOrNull(
            match.groupValues[1].toDoubleOrNull(),
            match.groupValues[2].toDoubleOrNull(),
        )
    }
}

internal fun interface MapProviderParser {
    fun parse(url: HttpUrl): io.github.miksask.linkagram.domain.MapParseResult?
}
