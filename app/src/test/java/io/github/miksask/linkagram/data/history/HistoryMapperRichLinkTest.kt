package io.github.miksask.linkagram.data.history

import io.github.miksask.linkagram.domain.CompletedAnalysis
import io.github.miksask.linkagram.domain.HistoryResultType
import io.github.miksask.linkagram.domain.RichLinkInfo
import io.github.miksask.linkagram.domain.RichLinkKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HistoryMapperRichLinkTest {
    @Test
    fun toEntities_andBack_preservesRichLink() {
        val analysis = CompletedAnalysis(
            sourceUrl = "https://koleo.pl/p/1",
            normalizedUrl = "https://koleo.pl/p/1",
            finalUrl = "https://koleo.pl/connection/abc",
            finalStatusCode = 200,
            redirectChain = emptyList(),
            location = null,
            richLink = RichLinkInfo(
                kind = RichLinkKind.Koleo,
                title = "Warszawa Centralna > Łódź Fabryczna",
                canonicalUrl = "https://koleo.pl/connection/abc",
            ),
            completedAtMillis = 1_700_000_000_000L,
        )

        val (entity, _) = HistoryMapper.toEntities(analysis, id = "rich-1")
        assertEquals(HistoryResultType.RichLink.name, entity.resultType)
        assertEquals(RichLinkKind.Koleo.name, entity.provider)
        assertEquals("Warszawa Centralna > Łódź Fabryczna", entity.placeName)
        assertNull(entity.latitude)

        val domain = HistoryMapper.toDomain(entity)
        assertEquals(HistoryResultType.RichLink, domain.resultType)
        assertEquals(RichLinkKind.Koleo, domain.richLinkKind)
        assertNull(domain.provider)
        assertEquals("Warszawa Centralna > Łódź Fabryczna", domain.placeName)
    }
}
