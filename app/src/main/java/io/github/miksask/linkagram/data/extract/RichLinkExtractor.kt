package io.github.miksask.linkagram.data.extract

import io.github.miksask.linkagram.domain.PageMeta
import io.github.miksask.linkagram.domain.RichLinkParseResult
import okhttp3.HttpUrl

fun interface RichLinkExtractor {
    fun parse(url: HttpUrl, pageMeta: PageMeta): RichLinkParseResult?
}
