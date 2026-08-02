package io.github.miksask.linkagram.data.extract

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HtmlMetaParserTest {
    @Test
    fun parse_extractsTitleAndOgTags() {
        val html = """
            <html><head>
            <title>Page Title &gt;&gt; KOLEO</title>
            <meta property="og:title" content="OG Title &gt; Dest" />
            <meta property="og:url" content="https://koleo.pl/connection/abc" />
            </head><body></body></html>
        """.trimIndent()

        val meta = HtmlMetaParser.parse(html)

        assertEquals("Page Title >> KOLEO", meta.title)
        assertEquals("OG Title > Dest", meta.ogTitle)
        assertEquals("https://koleo.pl/connection/abc", meta.ogUrl)
    }

    @Test
    fun parse_missingTags_returnsNullFields() {
        val meta = HtmlMetaParser.parse("<html><body>no meta</body></html>")
        assertNull(meta.title)
        assertNull(meta.ogTitle)
        assertNull(meta.ogUrl)
    }

    @Test
    fun parse_ogTitleWithGreaterThanInsideQuotes() {
        val html =
            """<meta property="og:title" content="Trip A > Trip B >> KOLEO" />"""
        val meta = HtmlMetaParser.parse(html)
        assertEquals("Trip A > Trip B >> KOLEO", meta.ogTitle)
    }
}
