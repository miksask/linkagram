package io.github.miksask.linkagram.domain

data class PageMeta(
    val title: String? = null,
    val ogTitle: String? = null,
    val ogUrl: String? = null,
) {
    val isEmpty: Boolean
        get() = title.isNullOrBlank() && ogTitle.isNullOrBlank() && ogUrl.isNullOrBlank()
}
