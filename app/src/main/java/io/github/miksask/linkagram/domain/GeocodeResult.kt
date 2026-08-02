package io.github.miksask.linkagram.domain

sealed interface GeocodeResult {
    data class Found(
        val latitude: Double,
        val longitude: Double,
    ) : GeocodeResult

    data object NotFound : GeocodeResult

    data class Failed(
        val message: String? = null,
    ) : GeocodeResult
}
