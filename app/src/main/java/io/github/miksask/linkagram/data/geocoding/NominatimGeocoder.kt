package io.github.miksask.linkagram.data.geocoding

import io.github.miksask.linkagram.domain.CoordinateValidator
import io.github.miksask.linkagram.domain.GeocodeResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONException
import java.io.IOException
import java.io.InterruptedIOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Opt-in address geocoding via OpenStreetMap Nominatim.
 * Called only after an explicit user tap; never during analyze().
 */
class NominatimGeocoder(
    private val client: OkHttpClient = defaultClient(),
    private val baseUrl: HttpUrl = DEFAULT_BASE_URL.toHttpUrl(),
) {
    suspend fun geocode(placeName: String?, address: String?): GeocodeResult {
        val queries = buildQueries(placeName, address)
        if (queries.isEmpty()) return GeocodeResult.NotFound

        var lastFailure: GeocodeResult.Failed? = null
        for (query in queries) {
            when (val result = lookup(query)) {
                is GeocodeResult.Found -> return result
                GeocodeResult.NotFound -> continue
                is GeocodeResult.Failed -> lastFailure = result
            }
        }
        return lastFailure ?: GeocodeResult.NotFound
    }

    private suspend fun lookup(query: String): GeocodeResult {
        val url = baseUrl.newBuilder()
            .addPathSegment("search")
            .addQueryParameter("format", "jsonv2")
            .addQueryParameter("limit", "1")
            .addQueryParameter("q", query)
            .build()
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .get()
            .build()

        return try {
            client.newCall(request).await().use { response ->
                if (!response.isSuccessful) {
                    return GeocodeResult.Failed("HTTP ${response.code}")
                }
                val body = response.body.string()
                parseBody(body)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: InterruptedIOException) {
            GeocodeResult.Failed("timeout")
        } catch (e: IOException) {
            GeocodeResult.Failed(e.message)
        } catch (e: JSONException) {
            GeocodeResult.Failed(e.message)
        }
    }

    private fun parseBody(body: String): GeocodeResult {
        val array = JSONArray(body)
        if (array.length() == 0) return GeocodeResult.NotFound
        val first = array.getJSONObject(0)
        val lat = first.getString("lat").toDoubleOrNull()
        val lon = first.getString("lon").toDoubleOrNull()
        if (lat == null || lon == null) return GeocodeResult.Failed("missing lat/lon")
        if (!CoordinateValidator.isValid(lat, lon)) return GeocodeResult.Failed("out of range")
        return GeocodeResult.Found(latitude = lat, longitude = lon)
    }

    companion object {
        private const val DEFAULT_BASE_URL = "https://nominatim.openstreetmap.org"
        private const val USER_AGENT =
            "Linkagram/0.1 (https://github.com/miksask/linkagram; contact via GitHub issues)"

        fun defaultClient(): OkHttpClient =
            OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .callTimeout(30, TimeUnit.SECONDS)
                .build()

        /**
         * Try the full "place, address" string first, then drop leading
         * comma-separated components until only the last segment remains.
         * Nominatim often fails on a verbose place name but succeeds on the
         * postal address alone.
         */
        internal fun buildQueries(placeName: String?, address: String?): List<String> {
            val parts = buildList {
                placeName?.trim()?.takeIf { it.isNotEmpty() }?.let(::add)
                address?.trim()?.takeIf { it.isNotEmpty() }?.let { addr ->
                    addr.split(',').map { it.trim() }.filter { it.isNotEmpty() }.forEach(::add)
                }
            }
            if (parts.isEmpty()) return emptyList()
            return parts.indices.map { index ->
                parts.drop(index).joinToString(", ")
            }.distinct()
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
