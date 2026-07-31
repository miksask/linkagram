package io.github.miksask.linkagram.core.url

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UrlExtractorTest {
    @Test
    fun fromViewIntentData_returnsTrimmedValue() {
        assertEquals(
            "https://example.com",
            UrlExtractor.fromViewIntentData("  https://example.com  "),
        )
    }

    @Test
    fun fromViewIntentData_nullOrBlank_returnsNull() {
        assertNull(UrlExtractor.fromViewIntentData(null))
        assertNull(UrlExtractor.fromViewIntentData("   "))
    }

    @Test
    fun fromSendIntentText_returnsTrimmedValue() {
        assertEquals(
            "See https://example.com now",
            UrlExtractor.fromSendIntentText("  See https://example.com now  "),
        )
    }

    @Test
    fun fromSendIntentText_nullOrBlank_returnsNull() {
        assertNull(UrlExtractor.fromSendIntentText(null))
        assertNull(UrlExtractor.fromSendIntentText(""))
    }
}
