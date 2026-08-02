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
                "https://www.google.com/maps/place/Politechniczna,+90-001+%D0%9B%D0%BE%D0%B4%D0%B7%D1%8C/" +
                    "data=!4m6!3m5!1s0x471a35270981de07:0x64b118caf362b246!7e2!8m2!3d51.7528303!4d19.4449704!18m1!1e1",
                MapProvider.GoogleMaps,
                51.7528303,
                19.4449704,
                "Politechniczna",
            ),
            arrayOf(
                "https://www.google.com/maps/place/Red+Square/@55.7539,37.6208,17z/" +
                    "data=!3d51.0!4d19.0",
                MapProvider.GoogleMaps,
                55.7539,
                37.6208,
                "Red Square",
            ),
            arrayOf(
                "https://www.google.com/maps/place/Somewhere/data=!3d999!4d19.0",
                MapProvider.GoogleMaps,
                null,
                null,
                "Somewhere",
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
                "https://omaps.app/04NPTpEAVb/%C5%81%C4%85kowa%2C_23%2F25",
                MapProvider.OrganicMaps,
                51.7566,
                19.43773,
                "Łąkowa, 23/25",
            ),
            arrayOf(
                "https://omaps.app/34.71399,33.14058/Amathus",
                MapProvider.OrganicMaps,
                34.71399,
                33.14058,
                "Amathus",
            ),
            arrayOf(
                "https://omaps.app/map?v=1&ll=50.08,14.41&n=Prague",
                MapProvider.OrganicMaps,
                50.08,
                14.41,
                "Prague",
            ),
            arrayOf(
                "https://omaps.app/map?v=1",
                MapProvider.OrganicMaps,
                null,
                null,
                null,
            ),
            arrayOf(
                "https://omaps.app/!!!bad!!!",
                MapProvider.OrganicMaps,
                null,
                null,
                null,
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

class GoogleMapsPlaceSplitTest {
    private val parser = MapUrlParser()

    @Test
    fun parse_placeWithAddressAndDataCoords_splitsNameAndAddress() {
        val url =
            "https://www.google.com/maps/place/Politechniczna,+90-001+%D0%9B%D0%BE%D0%B4%D0%B7%D1%8C/" +
                "data=!4m6!3m5!1s0x471a35270981de07:0x64b118caf362b246!7e2!8m2!3d51.7528303!4d19.4449704!18m1!1e1"
        val parsed = parser.parse(url) as MapParseResult.Parsed
        assertEquals(MapProvider.GoogleMaps, parsed.location.provider)
        assertEquals("Politechniczna", parsed.location.placeName)
        assertEquals("90-001 Лодзь", parsed.location.address)
        assertEquals(51.7528303, parsed.location.latitude)
        assertEquals(19.4449704, parsed.location.longitude)
    }

    @Test
    fun parse_placeWithAddressNoCoords_extractsAddressWithoutCoordinates() {
        val url =
            "https://www.google.com/maps/place/Centrum+Kszta%C5%82cenia+Zawodowego+i+Ustawicznego+w+%C5%81odzi," +
                "+Stefana+%C5%BBeromskiego+115,+90-542+%C5%81%C3%B3d%C5%BA/" +
                "data=!4m2!3m1!1s0x471a352808de581d:0x9eac1c1927024e88!18m1!1e1"
        val parsed = parser.parse(url) as MapParseResult.Parsed
        assertEquals(MapProvider.GoogleMaps, parsed.location.provider)
        assertEquals(
            "Centrum Kształcenia Zawodowego i Ustawicznego w Łodzi",
            parsed.location.placeName,
        )
        assertEquals("Stefana Żeromskiego 115, 90-542 Łódź", parsed.location.address)
        assertNull(parsed.location.latitude)
        assertNull(parsed.location.longitude)
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

    @Test
    fun findDataCoordinates_extractsAdjacentPair() {
        assertEquals(
            51.7528303 to 19.4449704,
            CoordinateParsing.findDataCoordinates("!8m2!3d51.7528303!4d19.4449704!18m1"),
        )
    }

    @Test
    fun findDataCoordinates_rejectsOutOfRange() {
        assertNull(CoordinateParsing.findDataCoordinates("!3d999!4d19.0"))
    }
}
