package io.github.miksask.linkagram.data.history

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

interface HistorySettingsRepository {
    val historyEnabled: Flow<Boolean>
    suspend fun isHistoryEnabled(): Boolean
    suspend fun setHistoryEnabled(enabled: Boolean)
}

class DataStoreHistorySettingsRepository(
    private val dataStore: DataStore<Preferences>,
) : HistorySettingsRepository {
    override val historyEnabled: Flow<Boolean> = dataStore.data
        .map { prefs -> prefs[KEY_HISTORY_ENABLED] ?: false }
        .distinctUntilChanged()

    override suspend fun isHistoryEnabled(): Boolean = historyEnabled.first()

    override suspend fun setHistoryEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_HISTORY_ENABLED] = enabled
        }
    }

    companion object {
        const val DATA_STORE_FILE = "history_settings.preferences_pb"
        private val KEY_HISTORY_ENABLED = booleanPreferencesKey("history_enabled")

        fun create(context: Context): HistorySettingsRepository {
            val appContext = context.applicationContext
            val dataStore = PreferenceDataStoreFactory.create(
                scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
                produceFile = { appContext.preferencesDataStoreFile(DATA_STORE_FILE) },
            )
            return DataStoreHistorySettingsRepository(dataStore)
        }
    }
}
