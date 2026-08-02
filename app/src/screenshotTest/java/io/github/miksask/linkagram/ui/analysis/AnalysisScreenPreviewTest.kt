package io.github.miksask.linkagram.ui.analysis

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import io.github.miksask.linkagram.domain.LocationInfo
import io.github.miksask.linkagram.domain.MapProvider
import io.github.miksask.linkagram.domain.RedirectStep
import io.github.miksask.linkagram.ui.theme.LinkagramTheme

@PreviewTest
@Preview(showBackground = true, name = "idle")
@Composable
fun AnalysisScreenIdlePreview() {
    LinkagramTheme {
        AnalysisScreenContent(
            state = AnalysisUiState(),
            onDraftUrlChanged = {},
            onAnalyze = {},
            onPasteFromClipboard = {},
            onCopyCoordinates = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true, name = "analyzing")
@Composable
fun AnalysisScreenAnalyzingPreview() {
    LinkagramTheme {
        AnalysisScreenContent(
            state = AnalysisUiState(
                draftUrl = "https://maps.app.goo.gl/example",
                isAnalyzing = true,
            ),
            onDraftUrlChanged = {},
            onAnalyze = {},
            onPasteFromClipboard = {},
            onCopyCoordinates = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true, name = "validation_error")
@Composable
fun AnalysisScreenValidationErrorPreview() {
    LinkagramTheme {
        AnalysisScreenContent(
            state = AnalysisUiState(
                draftUrl = "",
                validationError = ValidationError.Empty,
            ),
            onDraftUrlChanged = {},
            onAnalyze = {},
            onPasteFromClipboard = {},
            onCopyCoordinates = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true, name = "success_coordinates")
@Composable
fun AnalysisScreenSuccessCoordinatesPreview() {
    LinkagramTheme {
        AnalysisScreenContent(
            state = successState(),
            onDraftUrlChanged = {},
            onAnalyze = {},
            onPasteFromClipboard = {},
            onCopyCoordinates = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true, name = "resolve_error_partial_chain")
@Composable
fun AnalysisScreenResolveErrorPreview() {
    LinkagramTheme {
        AnalysisScreenContent(
            state = AnalysisUiState(
                draftUrl = "https://short.example/abc",
                redirectChain = listOf(
                    RedirectStep(
                        fromUrl = "https://short.example/abc",
                        toUrl = "https://maps.example/place",
                        statusCode = 302,
                    ),
                ),
                resolveError = ResolveError.TooManyRedirects,
            ),
            onDraftUrlChanged = {},
            onAnalyze = {},
            onPasteFromClipboard = {},
            onCopyCoordinates = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true, name = "long_url")
@Composable
fun AnalysisScreenLongUrlPreview() {
    val longUrl =
        "https://www.example.com/maps/place/" +
            "Very-Long-Place-Name-That-Should-Wrap-Instead-Of-Truncating/" +
            "@55.755826,37.617300,17z/data=!3m1!4b1!4m6!3m5!1s0x0:0x0!8m2!3d55.755826!4d37.6173"
    LinkagramTheme {
        AnalysisScreenContent(
            state = AnalysisUiState(
                draftUrl = longUrl,
                finalUrl = longUrl,
                finalStatusCode = 200,
                redirectChain = listOf(
                    RedirectStep(
                        fromUrl = "https://short.example/long",
                        toUrl = longUrl,
                        statusCode = 301,
                    ),
                ),
            ),
            onDraftUrlChanged = {},
            onAnalyze = {},
            onPasteFromClipboard = {},
            onCopyCoordinates = {},
        )
    }
}

@PreviewTest
@Preview(
    showBackground = true,
    name = "success_dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
fun AnalysisScreenSuccessDarkPreview() {
    LinkagramTheme(darkTheme = true) {
        AnalysisScreenContent(
            state = successState(),
            onDraftUrlChanged = {},
            onAnalyze = {},
            onPasteFromClipboard = {},
            onCopyCoordinates = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true, name = "no_coordinates")
@Composable
fun AnalysisScreenNoCoordinatesPreview() {
    LinkagramTheme {
        AnalysisScreenContent(
            state = AnalysisUiState(
                draftUrl = "https://maps.app.goo.gl/example",
                finalUrl =
                    "https://www.google.com/maps/place/Centrum+Ksztalcenia," +
                        "+Stefana+Zeromskiego+115,+90-542+Lodz/" +
                        "data=!4m2!3m1!1s0x471a352808de581d:0x9eac1c1927024e88",
                finalStatusCode = 200,
                location = LocationInfo(
                    provider = MapProvider.GoogleMaps,
                    placeName = "Centrum Ksztalcenia",
                    address = "Stefana Zeromskiego 115, 90-542 Lodz",
                ),
                geocodeState = GeocodeState.Available,
            ),
            onDraftUrlChanged = {},
            onAnalyze = {},
            onPasteFromClipboard = {},
            onCopyCoordinates = {},
            onFindCoordinates = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true, name = "approximate_coordinates")
@Composable
fun AnalysisScreenApproximateCoordinatesPreview() {
    LinkagramTheme {
        AnalysisScreenContent(
            state = AnalysisUiState(
                draftUrl = "https://maps.app.goo.gl/example",
                finalUrl =
                    "https://www.google.com/maps/place/Centrum+Ksztalcenia," +
                        "+Stefana+Zeromskiego+115,+90-542+Lodz/" +
                        "data=!4m2!3m1!1s0x471a352808de581d:0x9eac1c1927024e88",
                finalStatusCode = 200,
                location = LocationInfo(
                    provider = MapProvider.GoogleMaps,
                    placeName = "Centrum Ksztalcenia",
                    address = "Stefana Zeromskiego 115, 90-542 Lodz",
                    latitude = 51.7554125,
                    longitude = 19.4463773,
                ),
                coordinatesText = "51.7554125, 19.4463773",
                coordinatesAreApproximate = true,
            ),
            onDraftUrlChanged = {},
            onAnalyze = {},
            onPasteFromClipboard = {},
            onCopyCoordinates = {},
            onFindCoordinates = {},
        )
    }
}

private fun successState(): AnalysisUiState = AnalysisUiState(
    draftUrl = "https://maps.app.goo.gl/example",
    finalUrl = "https://www.google.com/maps/@55.755826,37.617300,17z",
    finalStatusCode = 200,
    redirectChain = listOf(
        RedirectStep(
            fromUrl = "https://maps.app.goo.gl/example",
            toUrl = "https://www.google.com/maps/@55.755826,37.617300,17z",
            statusCode = 302,
        ),
    ),
    location = LocationInfo(
        provider = MapProvider.GoogleMaps,
        placeName = "Red Square",
        address = "Moscow, Russia",
        latitude = 55.755826,
        longitude = 37.6173,
    ),
    coordinatesText = "55.755826, 37.6173",
)
