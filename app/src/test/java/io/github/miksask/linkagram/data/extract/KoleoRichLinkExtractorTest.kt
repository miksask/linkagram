package io.github.miksask.linkagram.data.extract

import io.github.miksask.linkagram.domain.PageMeta
import io.github.miksask.linkagram.domain.RichLinkKind
import io.github.miksask.linkagram.domain.RichLinkParseResult
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KoleoRichLinkExtractorTest {
    @Test
    fun parse_connectionUrl_prefersOgTitleAndCanonical() {
        val url = "https://koleo.pl/connection/2747f783-020b-5132-bd3c-306811a48739".toHttpUrl()
        val meta = PageMeta(
            title = "Fallback title >> KOLEO",
            ogTitle = "PKP Warszawa Centralna 02-08-2026 04:12 > Łódź Fabryczna 02-08-2026 05:44 >> KOLEO",
            ogUrl = "https://koleo.pl/connection/2747f783-020b-5132-bd3c-306811a48739",
        )

        val parsed = KoleoRichLinkExtractor.parse(url, meta) as RichLinkParseResult.Parsed

        assertEquals(RichLinkKind.Koleo, parsed.richLink.kind)
        assertEquals(
            "PKP Warszawa Centralna 02-08-2026 04:12 > Łódź Fabryczna 02-08-2026 05:44",
            parsed.richLink.title,
        )
        assertEquals(
            "https://koleo.pl/connection/2747f783-020b-5132-bd3c-306811a48739",
            parsed.richLink.canonicalUrl,
        )
    }

    @Test
    fun parse_shortLink_usesTitleWhenOgMissing() {
        val url = "https://koleo.pl/p/6632331672".toHttpUrl()
        val meta = PageMeta(
            title = "PKP A 01-01-2026 10:00 &gt; B 01-01-2026 11:00 &gt;&gt; KOLEO",
        )

        val parsed = KoleoRichLinkExtractor.parse(url, meta) as RichLinkParseResult.Parsed

        assertEquals("PKP A 01-01-2026 10:00 > B 01-01-2026 11:00", parsed.richLink.title)
    }

    @Test
    fun parse_nonKoleoHost_returnsNull() {
        val url = "https://example.com/connection/1".toHttpUrl()
        assertNull(KoleoRichLinkExtractor.parse(url, PageMeta(title = "Trip >> KOLEO")))
    }

    @Test
    fun parse_blankTitle_returnsUnsupported() {
        val url = "https://koleo.pl/connection/x".toHttpUrl()
        val result = KoleoRichLinkExtractor.parse(url, PageMeta(title = "   "))
        assertTrue(result is RichLinkParseResult.Unsupported)
    }

    @Test
    fun registry_parsesKoleo() {
        val registry = RichLinkExtractorRegistry()
        val result = registry.parse(
            "https://www.koleo.pl/en/connection/abc",
            PageMeta(ogTitle = "Station A > Station B >> KOLEO"),
        ) as RichLinkParseResult.Parsed
        assertEquals("Station A > Station B", result.richLink.title)
    }
}
