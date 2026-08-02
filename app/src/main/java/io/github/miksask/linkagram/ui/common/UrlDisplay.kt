package io.github.miksask.linkagram.ui.common

import android.net.Uri

object UrlDisplay {
    fun shorten(url: String, maxLength: Int = 48): String {
        val host = runCatching { Uri.parse(url).host }.getOrNull()
        if (!host.isNullOrBlank()) {
            val path = runCatching { Uri.parse(url).encodedPath }.getOrNull().orEmpty()
            val candidate = if (path.isBlank() || path == "/") host else "$host$path"
            return ellipsize(candidate, maxLength)
        }
        return ellipsize(url, maxLength)
    }

    private fun ellipsize(value: String, maxLength: Int): String {
        if (value.length <= maxLength) return value
        if (maxLength <= 3) return value.take(maxLength)
        return value.take(maxLength - 1) + "…"
    }
}
