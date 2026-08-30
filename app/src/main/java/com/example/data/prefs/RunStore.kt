package com.example.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.activeRunDataStore: DataStore<Preferences> by preferencesDataStore(name = "active_run")

/**
 * The single source of truth for the *current* (in-progress) run's item ids, persisted across
 * process death. This is deliberately tiny — one preferences key holding a CSV of catalog ids.
 * Saved/finished runs still live in Room ([com.example.data.db.RunEntity]); this is only the
 * volatile "what am I holding right now" list.
 */
class RunStore(private val context: Context) {

    /** Emits the persisted current-run item ids, newest write wins. Empty when nothing is stored. */
    val currentRunItemIds: Flow<List<Int>> = context.activeRunDataStore.data.map { prefs ->
        prefs[KEY].orEmpty()
            .split(',')
            .mapNotNull { it.trim().toIntOrNull() }
    }

    /** Overwrite the persisted current-run item ids (order preserved). */
    suspend fun save(ids: List<Int>) {
        context.activeRunDataStore.edit { prefs ->
            prefs[KEY] = ids.joinToString(",")
        }
    }

    private companion object {
        val KEY = stringPreferencesKey("current_run_item_ids")
    }
}
