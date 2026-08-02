package io.github.miksask.linkagram.data.resolver

import io.github.miksask.linkagram.data.extract.HtmlMetaParser
import io.github.miksask.linkagram.data.extract.MetaCapturePolicy
import io.github.miksask.linkagram.data.extract.RichLinkHostAllowlist
import io.github.miksask.linkagram.domain.PageMeta
import io.github.miksask.linkagram.domain.RedirectStep
import io.github.miksask.linkagram.domain.ResolveResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.io.InterruptedIOException
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.min

class RedirectResolver(
    private val client: OkHttpClient = defaultClient(),
    private val maxRedirects: Int = DEFAULT_MAX_REDIRECTS,
    private val metaCapturePolicy: MetaCapturePolicy = RichLinkHostAllowlist,
    private val maxMetaBodyBytes: Int = DEFAULT_MAX_META_BODY_BYTES,
) {
    suspend fun resolve(url: String): ResolveResult {
        val trimmed = url.trim()
        val explicitScheme = SCHEME_REGEX.find(trimmed)?.groupValues?.get(1)?.lowercase()
        if (explicitScheme != null && explicitScheme != "http" && explicitScheme != "https") {
            return ResolveResult.UnsupportedProtocol
        }

        val startUrl = trimmed.toHttpUrlOrNull()
            ?: return ResolveResult.InvalidInput

        if (startUrl.scheme != "http" && startUrl.scheme != "https") {
            return ResolveResult.UnsupportedProtocol
        }

        val chain = mutableListOf<RedirectStep>()
        val visited = linkedSetOf<String>()
        var current = startUrl

        repeat(maxRedirects + 1) { hopIndex ->
            coroutineContext.ensureActive()

            val currentString = current.toString()
            if (!visited.add(currentString)) {
                return ResolveResult.RedirectLoop(chain.toList())
            }

            if (hopIndex == maxRedirects) {
                return ResolveResult.TooManyRedirects(chain.toList())
            }

            val request = Request.Builder()
                .url(current)
                .header("User-Agent", USER_AGENT)
                .get()
                .build()

            try {
                client.newCall(request).await().use { response ->
                    val code = response.code
                    if (code in REDIRECT_CODES) {
                        val location = response.header("Location")
                        if (location.isNullOrBlank()) {
                            return httpError(code, currentString, chain, toUrl = null)
                        }
                        if (isMalformedAbsoluteLocation(location)) {
                            return httpError(code, currentString, chain, toUrl = location)
                        }
                        val next = current.resolve(location)
                            ?: return httpError(code, currentString, chain, toUrl = location)
                        if (next.scheme != "http" && next.scheme != "https") {
                            return ResolveResult.UnsupportedProtocol
                        }
                        chain += RedirectStep(
                            fromUrl = currentString,
                            toUrl = next.toString(),
                            statusCode = code,
                        )
                        current = next
                        return@repeat
                    }

                    if (code in 200..299) {
                        val pageMeta = capturePageMeta(current.host, response)
                        return ResolveResult.Success(
                            finalUrl = currentString,
                            finalStatusCode = code,
                            redirectChain = chain.toList(),
                            pageMeta = pageMeta,
                        )
                    }

                    return ResolveResult.HttpError(
                        statusCode = code,
                        url = currentString,
                        redirectChain = chain.toList(),
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: InterruptedIOException) {
                // Covers socket read/connect timeouts and the OkHttp call timeout.
                return ResolveResult.Timeout
            } catch (e: IOException) {
                return ResolveResult.NetworkError(e.message)
            } catch (e: Exception) {
                return ResolveResult.UnknownError(e.message)
            }
        }

        return ResolveResult.TooManyRedirects(chain.toList())
    }

    private fun httpError(
        code: Int,
        url: String,
        chain: List<RedirectStep>,
        toUrl: String?,
    ): ResolveResult.HttpError =
        ResolveResult.HttpError(
            statusCode = code,
            url = url,
            redirectChain = chain + RedirectStep(
                fromUrl = url,
                toUrl = toUrl,
                statusCode = code,
            ),
        )

    private fun capturePageMeta(host: String?, response: Response): PageMeta? {
        if (!metaCapturePolicy.shouldCapture(host)) return null
        val body = response.body
        val source = body.source()
        source.request(maxMetaBodyBytes.toLong())
        val buffer = source.buffer
        val toRead = min(buffer.size, maxMetaBodyBytes.toLong())
        if (toRead <= 0L) return null
        val charset: Charset = body.contentType()?.charset(Charsets.UTF_8) ?: Charsets.UTF_8
        val html = buffer.clone().readString(toRead, charset)
        val meta = HtmlMetaParser.parse(html)
        return meta.takeUnless { it.isEmpty }
    }

    companion object {
        const val DEFAULT_MAX_REDIRECTS = 10
        const val DEFAULT_MAX_META_BODY_BYTES = 256 * 1024
        private const val USER_AGENT = "Linkagram/0.1"
        private val REDIRECT_CODES = setOf(301, 302, 303, 307, 308)
        private val SCHEME_REGEX = Regex("""^([a-zA-Z][a-zA-Z0-9+.-]*):""")

        fun defaultClient(): OkHttpClient =
            OkHttpClient.Builder()
                .followRedirects(false)
                .followSslRedirects(false)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .callTimeout(30, TimeUnit.SECONDS)
                .build()

        /**
         * Absolute or protocol-relative Location values must parse as HTTP(S) URLs.
         * Values like `://bad` are rejected instead of being treated as relative paths.
         */
        internal fun isMalformedAbsoluteLocation(location: String): Boolean {
            val trimmed = location.trim()
            if (trimmed.startsWith("://")) return true
            if (trimmed.startsWith("//")) {
                return "https:$trimmed".toHttpUrlOrNull() == null
            }
            if (!trimmed.contains("://")) return false
            return trimmed.toHttpUrlOrNull() == null
        }

        private suspend fun Call.await(): Response =
            suspendCancellableCoroutine { continuation ->
                continuation.invokeOnCancellation { cancel() }
                enqueue(
                    object : Callback {
                        override fun onResponse(call: Call, response: Response) {
                            continuation.resume(response)
                        }

                        override fun onFailure(call: Call, e: IOException) {
                            if (continuation.isCancelled) return
                            continuation.resumeWithException(e)
                        }
                    },
                )
            }
    }
}
