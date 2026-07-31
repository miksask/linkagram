package io.github.miksask.linkagram.data.resolver

import io.github.miksask.linkagram.domain.RedirectStep
import io.github.miksask.linkagram.domain.ResolveResult
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

class RedirectResolver(
    private val client: OkHttpClient = defaultClient(),
    private val maxRedirects: Int = DEFAULT_MAX_REDIRECTS,
) {
    fun resolve(url: String): ResolveResult {
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
                client.newCall(request).execute().use { response ->
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
                        return ResolveResult.Success(
                            finalUrl = currentString,
                            redirectChain = chain.toList(),
                        )
                    }

                    return ResolveResult.HttpError(
                        statusCode = code,
                        url = currentString,
                        redirectChain = chain.toList(),
                    )
                }
            } catch (_: SocketTimeoutException) {
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

    companion object {
        const val DEFAULT_MAX_REDIRECTS = 10
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
    }
}
