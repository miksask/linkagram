package io.github.miksask.linkagram.core.url

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SearchNormalizerTest {
    @Test
    fun normalize_trimsAndLowercasesUnicode() {
        assertEquals("москва", SearchNormalizer.normalize("  Москва  "))
        assertEquals("berlin", SearchNormalizer.normalize("Berlin"))
    }

    @Test
    fun toLikePattern_empty_returnsNull() {
        assertNull(SearchNormalizer.toLikePattern("   "))
    }

    @Test
    fun toLikePattern_escapesWildcards() {
        assertEquals("%100\\%\\_off%", SearchNormalizer.toLikePattern("100%_off"))
    }
}
