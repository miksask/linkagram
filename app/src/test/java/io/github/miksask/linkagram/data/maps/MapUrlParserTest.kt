package io.github.miksask.linkagram.data.maps

import io.github.miksask.linkagram.domain.MapParseResult
import io.github.miksask.linkagram.domain.MapProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class MapUrlParserTest(
    private val url: String,
    private val expectedProvider: MapProvider?,
    private val expectedLat: Double?,
    private val expectedLon: Double?,
    private val expectedPlace: String?,
) {
    private val parser = MapUrlParser()

    @Test
    fun parse() {
        val result = parser.parse(url)
        if (expectedProvider == null) {
            assertEquals(MapParseResult.Unsupported, result)
            return
        }
        val parsed = result as MapParseResult.Parsed
        assertEquals(expectedProvider, parsed.location.provider)
        assertEquals(expectedLat, parsed.location.latitude)
        assertEquals(expectedLon, parsed.location.longitude)
        assertEquals(expectedPlace, parsed.location.placeName)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): List<Array<Any?>> = listOf(
            arrayOf(
                "https://www.google.com/maps/@55.75,37.62,14z",
                MapProvider.GoogleMaps,
                55.75,
                37.62,
                null,
            ),
            arrayOf(
                "https://www.google.com/maps/place/Red+Square/@55.7539,37.6208,17z",
                MapProvider.GoogleMaps,
                55.7539,
                37.6208,
                "Red Square",
            ),
            arrayOf(
                "https://maps.google.com/?q=55.75,37.62",
                MapProvider.GoogleMaps,
                55.75,
                37.62,
                null,
            ),
            arrayOf(
                "https://yandex.ru/maps/?ll=37.62,55.75&text=Moscow",
                MapProvider.YandexMaps,
                55.75,
                37.62,
                "Moscow",
            ),
            arrayOf(
                "https://www.openstreetmap.org/#map=15/55.75/37.62",
                MapProvider.OpenStreetMap,
                55.75,
                37.62,
                null,
            ),
            arrayOf(
                "https://www.openstreetmap.org/?mlat=55.75&mlon=37.62",
                MapProvider.OpenStreetMap,
                55.75,
                37.62,
                null,
            ),
            arrayOf(
                "https://maps.apple.com/?ll=55.75,37.62&q=Park",
                MapProvider.AppleMaps,
                55.75,
                37.62,
                "Park",
            ),
            arrayOf(
                "https://example.com/place?lat=55.75&lon=37.62",
                MapProvider.Generic,
                55.75,
                37.62,
                null,
            ),
            arrayOf(
                "https://example.com/place?lat=999&lon=37.62",
                null,
                null,
                null,
                null,
            ),
            arrayOf(
                "https://example.com/about",
                null,
                null,
                null,
                null,
            ),
        )
    }
}

class CoordinateFormatterTest {
    @Test
    fun format_latLon() {
        assertEquals(
            "55.75, 37.62",
            io.github.miksask.linkagram.domain.CoordinateFormatter.format(55.75, 37.62),
        )
    }

    @Test
    fun invalidCoordinates_rejectedByValidator() {
        assertTrue(!io.github.miksask.linkagram.domain.CoordinateValidator.isValid(999.0, 0.0))
        assertNull(CoordinateParsing.validOrNull(999.0, 0.0))
    }
}
