package io.github.miksask.linkagram.data.maps

import io.github.miksask.linkagram.domain.CoordinateValidator

/**
 * Decodes Organic Maps / MAPS.ME ge0 short-link coordinate blobs.
 *
 * Algorithm matches
 * [organicmaps/url-processor ge0.ts](https://github.com/organicmaps/url-processor/blob/master/src/ge0.ts)
 * `decodeLatLonZoom`. The first character encodes zoom; only its alphabet
 * validity is checked — zoom itself is not returned.
 */
internal object Ge0Decoder {
    private const val GE0_MAX_POINT_BYTES = 10
    private const val GE0_MAX_COORD_BITS = GE0_MAX_POINT_BYTES * 3

    /** Base64url alphabet `A-Za-z0-9-_`, indexed by character code. */
    private val base64Reverse: IntArray = IntArray(128) { -1 }.also { table ->
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
        for (i in alphabet.indices) {
            table[alphabet[i].code] = i
        }
    }

    /**
     * @return lat/lon pair rounded to 5 decimals, or null if the blob is invalid.
     */
    fun decode(encodedLatLonZoom: String): Pair<Double, Double>? {
        if (encodedLatLonZoom.isEmpty()) return null

        val zoomChar = encodedLatLonZoom[0].code
        if (zoomChar >= base64Reverse.size) return null
        val zoomValue = base64Reverse[zoomChar]
        if (zoomValue < 0 || zoomValue > 63) return null

        val latLonStr = encodedLatLonZoom.substring(1)
        if (latLonStr.length > GE0_MAX_POINT_BYTES) return null

        var lat = 0
        var lon = 0
        var shift = GE0_MAX_COORD_BITS - 3
        for (i in latLonStr.indices) {
            val code = latLonStr[i].code
            if (code >= base64Reverse.size) return null
            val a = base64Reverse[code]
            if (a < 0) return null
            val lat1 = (((a shr 5) and 1) shl 2) or (((a shr 3) and 1) shl 1) or ((a shr 1) and 1)
            val lon1 = (((a shr 4) and 1) shl 2) or (((a shr 2) and 1) shl 1) or (a and 1)
            lat = lat or (lat1 shl shift)
            lon = lon or (lon1 shl shift)
            shift -= 3
        }

        val unusedBytes = GE0_MAX_POINT_BYTES - latLonStr.length
        // Center of the remaining precision square; skip when all bits are present
        // (JS `1 << -1` is not portable — treat unusedBytes == 0 as no bias).
        if (unusedBytes > 0) {
            val middleOfSquare = 1 shl (3 * unusedBytes - 1)
            lat += middleOfSquare
            lon += middleOfSquare
        }

        var latitude = (lat.toDouble() / ((1 shl GE0_MAX_COORD_BITS) - 1)) * 180.0 - 90.0
        var longitude = (lon.toDouble() / (1 shl GE0_MAX_COORD_BITS)) * 360.0 - 180.0

        latitude = Math.round(latitude * 1e5) / 1e5
        longitude = Math.round(longitude * 1e5) / 1e5

        if (!CoordinateValidator.isValid(latitude, longitude)) return null
        // Mirror url-processor: reject poles and antimeridian extremes.
        if (latitude <= -90.0 || latitude >= 90.0 ||
            longitude <= -180.0 || longitude >= 180.0
        ) {
            return null
        }
        return latitude to longitude
    }
}
