package io.github.miksask.linkagram.data.geocoding

import io.github.miksask.linkagram.domain.GeocodeResult
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class NominatimGeocoderTest {
    private lateinit var server: MockWebServer
    private lateinit var geocoder: NominatimGeocoder

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val client = OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.SECONDS)
            .callTimeout(3, TimeUnit.SECONDS)
            .build()
        geocoder = NominatimGeocoder(
            client = client,
            baseUrl = server.url("/"),
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun geocode_found_returnsCoordinates() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""[{"lat":"51.7554125","lon":"19.4463773","display_name":"Test"}]"""),
        )

        val result = geocoder.geocode(
            placeName = "Centrum",
            address = "Stefana Żeromskiego 115, 90-542 Łódź",
        )

        val found = result as GeocodeResult.Found
        assertEquals(51.7554125, found.latitude, 0.0)
        assertEquals(19.4463773, found.longitude, 0.0)
        val request = server.takeRequest()
        assertTrue(request.path!!.startsWith("/search?"))
        assertTrue(request.path!!.contains("format=jsonv2"))
        assertTrue(request.path!!.contains("limit=1"))
    }

    @Test
    fun geocode_emptyThenFound_retriesWithTrimmedQuery() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("[]"))
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""[{"lat":"51.7554125","lon":"19.4463773"}]"""),
        )

        val result = geocoder.geocode(
            placeName = "Centrum Kształcenia",
            address = "Stefana Żeromskiego 115, 90-542 Łódź",
        )

        assertTrue(result is GeocodeResult.Found)
        assertEquals(2, server.requestCount)
        val first = server.takeRequest()
        val second = server.takeRequest()
        assertTrue(first.requestUrl!!.queryParameter("q")!!.startsWith("Centrum"))
        assertTrue(second.requestUrl!!.queryParameter("q")!!.startsWith("Stefana"))
    }

    @Test
    fun geocode_emptyArray_returnsNotFound() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("[]"))

        val result = geocoder.geocode(placeName = null, address = "Nowhere Street 1")

        assertEquals(GeocodeResult.NotFound, result)
    }

    @Test
    fun geocode_malformedJson_returnsFailed() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("not-json"))

        val result = geocoder.geocode(placeName = null, address = "Somewhere")

        assertTrue(result is GeocodeResult.Failed)
    }

    @Test
    fun geocode_httpError_returnsFailed() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(503).setBody("busy"))

        val result = geocoder.geocode(placeName = null, address = "Somewhere")

        assertTrue(result is GeocodeResult.Failed)
    }

    @Test
    fun geocode_blankInputs_returnsNotFoundWithoutNetwork() = runBlocking {
        val result = geocoder.geocode(placeName = "  ", address = null)

        assertEquals(GeocodeResult.NotFound, result)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun buildQueries_progressiveTrim() {
        val queries = NominatimGeocoder.buildQueries(
            placeName = "Centrum",
            address = "Street 1, City",
        )
        assertEquals(
            listOf(
                "Centrum, Street 1, City",
                "Street 1, City",
                "City",
            ),
            queries,
        )
    }
}
