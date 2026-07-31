package io.github.miksask.linkagram.domain

enum class MapProvider {
    GoogleMaps,
    YandexMaps,
    OpenStreetMap,
    AppleMaps,
    Generic,
}

data class LocationInfo(
    val provider: MapProvider,
    val placeName: String? = null,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
) {
    val hasCoordinates: Boolean
        get() = latitude != null && longitude != null
}

sealed interface MapParseResult {
    data class Parsed(val location: LocationInfo) : MapParseResult

    data object Unsupported : MapParseResult
}

object CoordinateFormatter {
    fun format(latitude: Double, longitude: Double): String =
        "$latitude, $longitude"
}

object CoordinateValidator {
    fun isValid(latitude: Double, longitude: Double): Boolean =
        latitude in -90.0..90.0 && longitude in -180.0..180.0
}
