package io.github.miksask.linkagram.core.url

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class UrlNormalizerTest(
    private val input: String,
    private val expected: Expected,
) {
    sealed interface Expected {
        data class Ok(val sourceUrl: String, val normalizedUrl: String) : Expected
        data class Err(val reason: InvalidUrlReason) : Expected
    }

    @Test
    fun normalize() {
        when (val result = UrlNormalizer.normalize(input)) {
            is UrlNormalizationResult.NormalizedUrl -> {
                assertTrue("Expected error for input=$input", expected is Expected.Ok)
                val ok = expected as Expected.Ok
                assertEquals(ok.normalizedUrl, result.normalizedUrl)
                assertEquals(ok.sourceUrl, result.sourceUrl)
            }
            is UrlNormalizationResult.InvalidUrl -> {
                assertTrue("Expected success for input=$input", expected is Expected.Err)
                assertEquals((expected as Expected.Err).reason, result.reason)
            }
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): List<Array<Any>> = listOf(
            arrayOf("", Expected.Err(InvalidUrlReason.Empty)),
            arrayOf("   ", Expected.Err(InvalidUrlReason.Empty)),
            arrayOf(
                "example.com/path",
                Expected.Ok("example.com/path", "https://example.com/path"),
            ),
            arrayOf(
                "  https://example.com  ",
                Expected.Ok("https://example.com", "https://example.com"),
            ),
            arrayOf(
                "Check this https://maps.example/x please",
                Expected.Ok("https://maps.example/x", "https://maps.example/x"),
            ),
            arrayOf("ftp://example.com", Expected.Err(InvalidUrlReason.UnsupportedScheme)),
            arrayOf("not a url at all", Expected.Err(InvalidUrlReason.NoUrlFound)),
            arrayOf("http://", Expected.Err(InvalidUrlReason.Malformed)),
            arrayOf(
                "https://example.com/a?b=1&c=2",
                Expected.Ok("https://example.com/a?b=1&c=2", "https://example.com/a?b=1&c=2"),
            ),
        )
    }
}
