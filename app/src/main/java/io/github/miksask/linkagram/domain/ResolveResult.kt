package io.github.miksask.linkagram.domain

data class RedirectStep(
    val fromUrl: String,
    val toUrl: String?,
    val statusCode: Int?,
)

sealed interface ResolveResult {
    data class Success(
        val finalUrl: String,
        val redirectChain: List<RedirectStep>,
    ) : ResolveResult

    data object InvalidInput : ResolveResult

    data class NetworkError(val message: String? = null) : ResolveResult

    data object Timeout : ResolveResult

    data class TooManyRedirects(
        val redirectChain: List<RedirectStep>,
    ) : ResolveResult

    data class RedirectLoop(
        val redirectChain: List<RedirectStep>,
    ) : ResolveResult

    data object UnsupportedProtocol : ResolveResult

    data class HttpError(
        val statusCode: Int,
        val url: String,
        val redirectChain: List<RedirectStep>,
    ) : ResolveResult

    data class UnknownError(val message: String? = null) : ResolveResult
}
