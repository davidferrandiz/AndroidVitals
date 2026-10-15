package com.ferryapps.vitals.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.ferryapps.vitals.core.domain.repository.EngagementRepository
import kotlinx.coroutines.flow.first
import java.io.IOException
import javax.inject.Inject

class EngagementRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : EngagementRepository {

    override suspend fun recordLaunch() {
        dataStore.edit { it[KEY_LAUNCHES] = (it[KEY_LAUNCHES] ?: 0) + 1 }
    }

    override suspend fun markEngaged() {
        dataStore.edit { it[KEY_ENGAGED] = true }
    }

    override suspend fun shouldAskForReview(nowMs: Long): Boolean {
        val prefs = try {
            dataStore.data.first()
        } catch (e: IOException) {
            return false
        }
        val launches = prefs[KEY_LAUNCHES] ?: 0
        val engaged = prefs[KEY_ENGAGED] ?: false
        val lastAsked = prefs[KEY_REVIEW_ASKED_AT] ?: 0L
        return launches >= MIN_LAUNCHES && engaged && (lastAsked == 0L || nowMs - lastAsked > MIN_INTERVAL_MS)
    }

    override suspend fun markReviewAsked(nowMs: Long) {
        dataStore.edit { it[KEY_REVIEW_ASKED_AT] = nowMs }
    }

    private companion object {
        const val MIN_LAUNCHES = 3
        const val MIN_INTERVAL_MS = 120L * 24 * 60 * 60 * 1000
        val KEY_LAUNCHES = intPreferencesKey("launch_count")
        val KEY_ENGAGED = booleanPreferencesKey("engaged")
        val KEY_REVIEW_ASKED_AT = longPreferencesKey("review_asked_at")
    }
}
