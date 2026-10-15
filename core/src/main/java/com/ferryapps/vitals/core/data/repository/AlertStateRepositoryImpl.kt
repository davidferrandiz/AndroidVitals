package com.ferryapps.vitals.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import com.ferryapps.vitals.core.domain.alerts.AlertType
import com.ferryapps.vitals.core.domain.repository.AlertStateRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import java.io.IOException
import javax.inject.Inject

class AlertStateRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : AlertStateRepository {

    override suspend fun lastFired(): Map<AlertType, Long> {
        val prefs = dataStore.data
            .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
            .first()
        return AlertType.entries.mapNotNull { type -> prefs[key(type)]?.let { type to it } }.toMap()
    }

    override suspend fun markFired(type: AlertType, atMs: Long) {
        dataStore.edit { it[key(type)] = atMs }
    }

    private fun key(type: AlertType) = longPreferencesKey("alert_last_${type.name.lowercase()}")
}
