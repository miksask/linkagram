package io.github.miksask.linkagram.core.url

sealed interface UrlNormalizationResult {
    data class NormalizedUrl(
        val sourceUrl: String,
        val normalizedUrl: String,
    ) : UrlNormalizationResult

    data class InvalidUrl(val reason: InvalidUrlReason) : UrlNormalizationResult
}

enum class InvalidUrlReason {
    Empty,
    Malformed,
    UnsupportedScheme,
    NoUrlFound,
}

/**
 * Validates and normalizes user-supplied URL text.
 *
 * Rules are documented in docs/specs/001-url-input-normalization.md.
 */
object UrlNormalizer {
    private val explicitHttpUrlRegex = Regex(
        pattern = """https?://[^\s<>"']+""",
        option = RegexOption.IGNORE_CASE,
    )

    fun normalize(rawInput: String): UrlNormalizationResult {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) {
            return UrlNormalizationResult.InvalidUrl(InvalidUrlReason.Empty)
        }

        val candidate = extractCandidate(trimmed)
            ?: return UrlNormalizationResult.InvalidUrl(InvalidUrlReason.NoUrlFound)

        return normalizeCandidate(candidate)
    }

    private fun extractCandidate(trimmed: String): String? {
        val explicitMatch = explicitHttpUrlRegex.find(trimmed)
        if (explicitMatch != null) {
            return explicitMatch.value.trimEnd('/', '.', ',', ';', ')', ']', '}')
                .trimEnd()
                .ifEmpty { null }
        }

        // Whole string is the candidate when it looks like a host/path without scheme,
        // or already has another scheme (handled later as unsupported).
        if (trimmed.contains(Regex("""\s"""))) {
            return null
        }
        return trimmed
    }

    private fun normalizeCandidate(candidate: String): UrlNormalizationResult {
        val withScheme = if (hasScheme(candidate)) {
            candidate
        } else {
            "https://$candidate"
        }

        val scheme = schemeOf(withScheme)
            ?: return UrlNormalizationResult.InvalidUrl(InvalidUrlReason.Malformed)

        if (scheme != "http" && scheme != "https") {
            return UrlNormalizationResult.InvalidUrl(InvalidUrlReason.UnsupportedScheme)
        }

        return try {
            val uri = java.net.URI(withScheme)
            val host = uri.host
            if (host.isNullOrBlank()) {
                return UrlNormalizationResult.InvalidUrl(InvalidUrlReason.Malformed)
            }
            // Rebuild without rewriting path/query/fragment beyond URI parsing.
            val normalized = uri.toASCIIString()
            UrlNormalizationResult.NormalizedUrl(
                sourceUrl = candidate,
                normalizedUrl = normalized,
            )
        } catch (_: Exception) {
            UrlNormalizationResult.InvalidUrl(InvalidUrlReason.Malformed)
        }
    }

    private fun hasScheme(value: String): Boolean =
        value.contains("://") || value.startsWith(prefix = "http:", ignoreCase = true) ||
            value.startsWith(prefix = "https:", ignoreCase = true)

    private fun schemeOf(value: String): String? {
        val separator = value.indexOf("://")
        if (separator <= 0) return null
        return value.substring(0, separator).lowercase()
    }
}
