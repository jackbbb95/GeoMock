package me.bogle.geomock.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "settings",
    // Without a handler, a corrupt preferences file (interrupted write, full disk, or a
    // backup restored from another version of the app) throws CorruptionException out of
    // every read, which crashed the home screen on launch every time. Losing the saved
    // locations is a much better outcome than a crash loop.
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() }
)
