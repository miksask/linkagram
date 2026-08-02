package io.github.miksask.linkagram.ui.history

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import io.github.miksask.linkagram.domain.HistoryEntry
import io.github.miksask.linkagram.domain.HistoryResultType
import io.github.miksask.linkagram.domain.MapProvider
import io.github.miksask.linkagram.ui.settings.SettingsScreenContent
import io.github.miksask.linkagram.ui.settings.SettingsUiState
import io.github.miksask.linkagram.ui.theme.LinkagramTheme

private val sampleEntry = HistoryEntry(
    id = "1",
    completedAtMillis = 1_700_000_000_000,
    sourceUrl = "https://maps.app.goo.gl/example",
    normalizedUrl = "https://maps.app.goo.gl/example",
    finalUrl = "https://www.google.com/maps/@55.75,37.61,17z",
    finalStatusCode = 200,
    resultType = HistoryResultType.Map,
    provider = MapProvider.GoogleMaps,
    placeName = "Red Square",
    address = "Moscow, Russia",
    latitude = 55.75,
    longitude = 37.61,
    redirectCount = 1,
)

@PreviewTest
@Preview(showBackground = true, name = "history_empty")
@Composable
fun HistoryEmptyPreview() {
    LinkagramTheme {
        HistoryScreenContent(
            state = HistoryUiState(isLoading = false, historyEnabled = true, totalCount = 0),
            onSearchChanged = {},
            onDateFilterSelected = {},
            onReset = {},
            onOpenEntry = {},
            onDeleteEntry = {},
            onClearAll = {},
            onDeleteMatching = {},
            onDismissConfirmations = {},
            onConfirmClearAll = {},
            onConfirmDeleteMatching = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true, name = "history_list")
@Composable
fun HistoryListPreview() {
    LinkagramTheme {
        HistoryScreenContent(
            state = HistoryUiState(
                isLoading = false,
                historyEnabled = true,
                totalCount = 1,
                matchCount = 1,
                entries = listOf(sampleEntry),
            ),
            onSearchChanged = {},
            onDateFilterSelected = {},
            onReset = {},
            onOpenEntry = {},
            onDeleteEntry = {},
            onClearAll = {},
            onDeleteMatching = {},
            onDismissConfirmations = {},
            onConfirmClearAll = {},
            onConfirmDeleteMatching = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true, name = "history_details")
@Composable
fun HistoryDetailsPreview() {
    LinkagramTheme {
        HistoryDetailsScreenContent(
            state = HistoryDetailsUiState(isLoading = false, entry = sampleEntry),
            onAnalyzeAgain = {},
            onDelete = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true, name = "settings_history_off")
@Composable
fun SettingsHistoryOffPreview() {
    LinkagramTheme {
        SettingsScreenContent(
            state = SettingsUiState(historyEnabled = false, totalCount = 2),
            onHistoryEnabledChanged = {},
            onClearHistory = {},
            onDismissClear = {},
            onConfirmClear = {},
        )
    }
}

@PreviewTest
@Preview(
    showBackground = true,
    name = "history_list_dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
fun HistoryListDarkPreview() {
    LinkagramTheme(darkTheme = true) {
        HistoryScreenContent(
            state = HistoryUiState(
                isLoading = false,
                historyEnabled = true,
                totalCount = 1,
                matchCount = 1,
                entries = listOf(sampleEntry),
            ),
            onSearchChanged = {},
            onDateFilterSelected = {},
            onReset = {},
            onOpenEntry = {},
            onDeleteEntry = {},
            onClearAll = {},
            onDeleteMatching = {},
            onDismissConfirmations = {},
            onConfirmClearAll = {},
            onConfirmDeleteMatching = {},
        )
    }
}
