package com.rork.blockblastsolver.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.historyDataStore: DataStore<Preferences> by preferencesDataStore(name = "solver_history")

/** Persists scanned turns so History and Statistik survive app restarts. */
class HistoryRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val recordsKey = stringPreferencesKey("records_json")

    val records: Flow<List<ScanRecord>> = context.historyDataStore.data
        .catch { throwable ->
            Log.w(TAG, "Failed to read history, starting empty: ${throwable.message}")
            emit(androidx.datastore.preferences.core.emptyPreferences())
        }
        .map { prefs ->
            val raw = prefs[recordsKey] ?: return@map emptyList()
            runCatching { json.decodeFromString<HistoryPayload>(raw).records }
                .onFailure { Log.w(TAG, "History payload corrupt, discarding: ${it.message}") }
                .getOrDefault(emptyList())
                .sortedByDescending { it.timestampMillis }
        }

    suspend fun add(record: ScanRecord) {
        update { current -> (listOf(record) + current).take(MAX_RECORDS) }
    }

    suspend fun delete(id: String) {
        update { current -> current.filterNot { it.id == id } }
    }

    suspend fun clear() {
        update { emptyList() }
    }

    private suspend fun update(transform: (List<ScanRecord>) -> List<ScanRecord>) {
        context.historyDataStore.edit { prefs ->
            val current = prefs[recordsKey]
                ?.let { raw -> runCatching { json.decodeFromString<HistoryPayload>(raw).records }.getOrDefault(emptyList()) }
                ?: emptyList()
            prefs[recordsKey] = json.encodeToString(HistoryPayload(transform(current)))
        }
    }

    private companion object {
        const val TAG = "HistoryRepository"
        const val MAX_RECORDS = 200
    }
}
