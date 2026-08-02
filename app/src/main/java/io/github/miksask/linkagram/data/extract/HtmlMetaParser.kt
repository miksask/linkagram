package io.github.miksask.linkagram.data.extract

import io.github.miksask.linkagram.domain.PageMeta

object HtmlMetaParser {
    private val TITLE_REGEX =
        Regex("""<title[^>]*>(.*?)</title>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val ATTR_REGEX =
        Regex("""([a-zA-Z_:][-a-zA-Z0-9_:.]*)\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s>]+))""")

    fun parse(html: String): PageMeta {
        val title = TITLE_REGEX.find(html)?.groupValues?.getOrNull(1)?.let { decodeBasicEntities(it.trim()) }
            ?.takeIf { it.isNotEmpty() }
        var ogTitle: String? = null
        var ogUrl: String? = null
        for (tag in findMetaTags(html)) {
            val attrs = parseAttributes(tag)
            val property = attrs["property"]?.lowercase()
            val name = attrs["name"]?.lowercase()
            val content = attrs["content"]?.let { decodeBasicEntities(it.trim()) }?.takeIf { it.isNotEmpty() }
                ?: continue
            when {
                property == "og:title" || name == "og:title" -> if (ogTitle == null) ogTitle = content
                property == "og:url" || name == "og:url" -> if (ogUrl == null) ogUrl = content
            }
        }
        return PageMeta(title = title, ogTitle = ogTitle, ogUrl = ogUrl)
    }

    /**
     * Locates `<meta ...>` tags without treating `>` inside quoted attribute values
     * as the tag terminator (common in og:title trip strings).
     */
    internal fun findMetaTags(html: String): List<String> {
        val tags = mutableListOf<String>()
        var index = 0
        while (index < html.length) {
            val start = html.indexOf("<meta", index, ignoreCase = true)
            if (start < 0) break
            var cursor = start + 5
            var quote: Char? = null
            while (cursor < html.length) {
                val ch = html[cursor]
                when {
                    quote != null -> if (ch == quote) quote = null
                    ch == '"' || ch == '\'' -> quote = ch
                    ch == '>' -> {
                        tags += html.substring(start, cursor + 1)
                        index = cursor + 1
                        break
                    }
                }
                cursor++
            }
            if (cursor >= html.length) break
        }
        return tags
    }

    private fun parseAttributes(tag: String): Map<String, String> {
        val attrs = linkedMapOf<String, String>()
        for (match in ATTR_REGEX.findAll(tag)) {
            val key = match.groupValues[1].lowercase()
            val value = match.groupValues[2]
                .ifEmpty { match.groupValues[3] }
                .ifEmpty { match.groupValues[4] }
            attrs[key] = value
        }
        return attrs
    }

    internal fun decodeBasicEntities(value: String): String =
        value
            .replace("&gt;", ">", ignoreCase = true)
            .replace("&lt;", "<", ignoreCase = true)
            .replace("&amp;", "&", ignoreCase = true)
            .replace("&quot;", "\"", ignoreCase = true)
            .replace("&#39;", "'")
            .replace("&apos;", "'", ignoreCase = true)
}
