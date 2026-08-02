package io.github.miksask.linkagram.core.url

import java.util.Locale

object SearchNormalizer {
    fun normalize(value: String?): String =
        value?.trim()?.lowercase(Locale.ROOT).orEmpty()

    fun escapeLike(value: String): String =
        buildString(value.length) {
            value.forEach { ch ->
                when (ch) {
                    '\\', '%', '_' -> {
                        append('\\')
                        append(ch)
                    }
                    else -> append(ch)
                }
            }
        }

    fun toLikePattern(rawQuery: String): String? {
        val normalized = normalize(rawQuery)
        if (normalized.isEmpty()) return null
        return "%${escapeLike(normalized)}%"
    }
}
