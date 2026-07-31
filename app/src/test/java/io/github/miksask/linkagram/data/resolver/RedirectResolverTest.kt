package io.github.miksask.linkagram.data.resolver

import io.github.miksask.linkagram.domain.ResolveResult
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class RedirectResolverTest {
    private lateinit var server: MockWebServer
    private lateinit var resolver: RedirectResolver

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val client = OkHttpClient.Builder()
            .followRedirects(false)
            .followSslRedirects(false)
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.SECONDS)
            .callTimeout(3, TimeUnit.SECONDS)
            .build()
        resolver = RedirectResolver(client = client, maxRedirects = 10)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun directUrl_returnsSuccessWithEmptyChain() {
        server.enqueue(MockResponse().setResponseCode(200))

        val result = resolver.resolve(server.url("/final").toString())

        val success = result as ResolveResult.Success
        assertEquals(server.url("/final").toString(), success.finalUrl)
        assertTrue(success.redirectChain.isEmpty())
    }

    @Test
    fun redirectChain_recordsEachHop() {
        server.enqueue(
            MockResponse()
                .setResponseCode(301)
                .addHeader("Location", "/second"),
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(302)
                .addHeader("Location", "/final"),
        )
        server.enqueue(MockResponse().setResponseCode(200))

        val result = resolver.resolve(server.url("/start").toString())

        val success = result as ResolveResult.Success
        assertEquals(server.url("/final").toString(), success.finalUrl)
        assertEquals(2, success.redirectChain.size)
        assertEquals(301, success.redirectChain[0].statusCode)
        assertEquals(302, success.redirectChain[1].statusCode)
        assertEquals(server.url("/second").toString(), success.redirectChain[0].toUrl)
    }

    @Test
    fun tooManyRedirects_returnsLimitError() {
        repeat(11) { index ->
            server.enqueue(
                MockResponse()
                    .setResponseCode(302)
                    .addHeader("Location", "/hop-${index + 1}"),
            )
        }

        val result = resolver.resolve(server.url("/hop-0").toString())

        assertTrue(result is ResolveResult.TooManyRedirects)
        assertEquals(10, (result as ResolveResult.TooManyRedirects).redirectChain.size)
    }

    @Test
    fun redirectLoop_isDetected() {
        server.enqueue(
            MockResponse()
                .setResponseCode(302)
                .addHeader("Location", "/a"),
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(302)
                .addHeader("Location", "/b"),
        )
        // Third response unused if loop detected when revisiting /a after /b points back.
        server.enqueue(
            MockResponse()
                .setResponseCode(302)
                .addHeader("Location", "/a"),
        )

        // Build explicit loop: /a -> /b -> /a
        // Start at /a
        // Actually enqueue for /a, /b, then when back to /a we detect loop before request
        // Wait - visited adds URL before request. /a visited, redirect to /b. /b visited, redirect to /a.
        // Next iteration /a already in visited -> RedirectLoop.
        // But we need the Location on /b to point to /a. Re-setup:
        server.shutdown()
        server = MockWebServer()
        server.start()
        val client = OkHttpClient.Builder()
            .followRedirects(false)
            .followSslRedirects(false)
            .build()
        resolver = RedirectResolver(client = client, maxRedirects = 10)

        server.enqueue(
            MockResponse()
                .setResponseCode(302)
                .addHeader("Location", server.url("/b").toString()),
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(302)
                .addHeader("Location", server.url("/a").toString()),
        )

        val result = resolver.resolve(server.url("/a").toString())

        assertTrue(result is ResolveResult.RedirectLoop)
    }

    @Test
    fun missingLocation_returnsHttpError() {
        server.enqueue(MockResponse().setResponseCode(302))

        val result = resolver.resolve(server.url("/missing-location").toString())

        assertTrue(result is ResolveResult.HttpError)
        assertEquals(302, (result as ResolveResult.HttpError).statusCode)
    }

    @Test
    fun malformedLocation_returnsHttpError() {
        server.enqueue(
            MockResponse()
                .setResponseCode(302)
                .addHeader("Location", "://bad"),
        )

        val result = resolver.resolve(server.url("/bad-location").toString())

        assertTrue(result is ResolveResult.HttpError)
    }

    @Test
    fun invalidInput_noNetworkRequest() {
        val result = resolver.resolve("not a url")

        assertEquals(ResolveResult.InvalidInput, result)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun unsupportedProtocol_ftp() {
        val result = resolver.resolve("ftp://example.com/file")

        assertEquals(ResolveResult.UnsupportedProtocol, result)
    }

    @Test
    fun httpError_nonRedirectFailure() {
        server.enqueue(MockResponse().setResponseCode(500))

        val result = resolver.resolve(server.url("/error").toString())

        val error = result as ResolveResult.HttpError
        assertEquals(500, error.statusCode)
    }
}
