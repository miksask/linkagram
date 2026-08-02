package io.github.miksask.linkagram.data.resolver

import io.github.miksask.linkagram.domain.ResolveResult
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
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
    fun directUrl_returnsSuccessWithEmptyChain() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200))

        val result = resolver.resolve(server.url("/final").toString())

        val success = result as ResolveResult.Success
        assertEquals(server.url("/final").toString(), success.finalUrl)
        assertEquals(200, success.finalStatusCode)
        assertTrue(success.redirectChain.isEmpty())
    }

    @Test
    fun redirectChain_recordsEachHop() = runBlocking {
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
        assertEquals(200, success.finalStatusCode)
        assertEquals(2, success.redirectChain.size)
        assertEquals(301, success.redirectChain[0].statusCode)
        assertEquals(302, success.redirectChain[1].statusCode)
        assertEquals(server.url("/second").toString(), success.redirectChain[0].toUrl)
    }

    @Test
    fun tooManyRedirects_returnsLimitError() = runBlocking {
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
    fun redirectLoop_isDetected() = runBlocking {
        // /a -> /b -> /a: the third hop revisits /a and must stop before requesting it.
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
        assertEquals(2, server.requestCount)
    }

    @Test
    fun missingLocation_returnsHttpError() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(302))

        val result = resolver.resolve(server.url("/missing-location").toString())

        assertTrue(result is ResolveResult.HttpError)
        assertEquals(302, (result as ResolveResult.HttpError).statusCode)
    }

    @Test
    fun malformedLocation_returnsHttpError() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(302)
                .addHeader("Location", "://bad"),
        )

        val result = resolver.resolve(server.url("/bad-location").toString())

        assertTrue(result is ResolveResult.HttpError)
    }

    @Test
    fun invalidInput_noNetworkRequest() = runBlocking {
        val result = resolver.resolve("not a url")

        assertEquals(ResolveResult.InvalidInput, result)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun unsupportedProtocol_ftp() = runBlocking {
        val result = resolver.resolve("ftp://example.com/file")

        assertEquals(ResolveResult.UnsupportedProtocol, result)
    }

    @Test
    fun httpError_nonRedirectFailure() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500))

        val result = resolver.resolve(server.url("/error").toString())

        val error = result as ResolveResult.HttpError
        assertEquals(500, error.statusCode)
    }

    @Test
    fun slowResponse_cancellationPropagates() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeadersDelay(5, TimeUnit.SECONDS),
        )

        var cancelled = false
        try {
            withTimeout(500) { resolver.resolve(server.url("/slow").toString()) }
        } catch (_: TimeoutCancellationException) {
            cancelled = true
        }

        assertTrue(cancelled)
    }
}
