package io.github.miksask.linkagram.ui.navigation

object LinkagramDestinations {
    const val ANALYZE = "analyze"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val HISTORY_DETAILS = "history/{entryId}"

    fun historyDetails(entryId: String): String = "history/$entryId"
}
