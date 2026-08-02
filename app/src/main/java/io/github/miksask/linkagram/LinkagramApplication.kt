package io.github.miksask.linkagram

import android.app.Application
import io.github.miksask.linkagram.data.geocoding.NominatimGeocoder
import io.github.miksask.linkagram.data.history.DataStoreHistorySettingsRepository
import io.github.miksask.linkagram.data.history.HistoryRepository
import io.github.miksask.linkagram.data.history.HistorySettingsRepository
import io.github.miksask.linkagram.data.history.LinkagramDatabase
import io.github.miksask.linkagram.data.maps.MapUrlParser
import io.github.miksask.linkagram.data.resolver.RedirectResolver

class LinkagramApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

class AppContainer(application: Application) {
    private val database: LinkagramDatabase = LinkagramDatabase.create(application)

    val historySettingsRepository: HistorySettingsRepository =
        DataStoreHistorySettingsRepository.create(application)

    val historyRepository: HistoryRepository = HistoryRepository(
        dao = database.historyDao(),
        settings = historySettingsRepository,
    )

    val redirectResolver: RedirectResolver = RedirectResolver()
    val mapUrlParser: MapUrlParser = MapUrlParser()
    val nominatimGeocoder: NominatimGeocoder = NominatimGeocoder()
}
