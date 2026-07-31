package io.github.miksask.linkagram.core.url

/**
 * Extracts a raw URL candidate from Android intent payloads without normalizing.
 */
object UrlExtractor {
    fun fromViewIntentData(dataString: String?): String? {
        val value = dataString?.trim().orEmpty()
        return value.ifEmpty { null }
    }

    fun fromSendIntentText(sharedText: String?): String? {
        val value = sharedText?.trim().orEmpty()
        return value.ifEmpty { null }
    }
}
