package io.github.miksask.linkagram.data.extract

import io.github.miksask.linkagram.domain.PageMeta
import io.github.miksask.linkagram.domain.RichLinkInfo
import io.github.miksask.linkagram.domain.RichLinkKind
import io.github.miksask.linkagram.domain.RichLinkParseResult
import okhttp3.HttpUrl

object KoleoRichLinkExtractor : RichLinkExtractor {
    private val TRAILING_KOLEO = Regex("""\s*(?:>>|&gt;&gt;)\s*KOLEO\s*$""", RegexOption.IGNORE_CASE)

    override fun parse(url: HttpUrl, pageMeta: PageMeta): RichLinkParseResult? {
        if (!RichLinkHostAllowlist.shouldCapture(url.host)) return null
        val rawTitle = pageMeta.ogTitle?.takeIf { it.isNotBlank() }
            ?: pageMeta.title?.takeIf { it.isNotBlank() }
            ?: return RichLinkParseResult.Unsupported
        val title = cleanupTitle(rawTitle)
        if (title.isBlank()) return RichLinkParseResult.Unsupported
        return RichLinkParseResult.Parsed(
            RichLinkInfo(
                kind = RichLinkKind.Koleo,
                title = title,
                canonicalUrl = pageMeta.ogUrl?.takeIf { it.isNotBlank() },
            ),
        )
    }

    internal fun cleanupTitle(raw: String): String =
        TRAILING_KOLEO.replace(HtmlMetaParser.decodeBasicEntities(raw).trim(), "").trim()
}
