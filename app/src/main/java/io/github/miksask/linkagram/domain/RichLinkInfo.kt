package io.github.miksask.linkagram.domain

enum class RichLinkKind {
    Koleo,
    ;

    val displayName: String
        get() = when (this) {
            Koleo -> "KOLEO"
        }
}

data class RichLinkInfo(
    val kind: RichLinkKind,
    val title: String,
    val canonicalUrl: String? = null,
)

sealed interface RichLinkParseResult {
    data class Parsed(val richLink: RichLinkInfo) : RichLinkParseResult

    data object Unsupported : RichLinkParseResult
}
