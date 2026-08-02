package io.github.miksask.linkagram.data.extract

import io.github.miksask.linkagram.domain.PageMeta
import io.github.miksask.linkagram.domain.RichLinkParseResult
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

class RichLinkExtractorRegistry(
    private val extractors: List<RichLinkExtractor> = listOf(KoleoRichLinkExtractor),
) {
    fun parse(url: String, pageMeta: PageMeta?): RichLinkParseResult {
        if (pageMeta == null || pageMeta.isEmpty) return RichLinkParseResult.Unsupported
        val httpUrl = url.toHttpUrlOrNull() ?: return RichLinkParseResult.Unsupported
        for (extractor in extractors) {
            val result = extractor.parse(httpUrl, pageMeta) ?: continue
            if (result is RichLinkParseResult.Parsed) return result
        }
        return RichLinkParseResult.Unsupported
    }
}
