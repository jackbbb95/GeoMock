package me.bogle.geomock.ui.home

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import me.bogle.geomock.data.dataStore
import me.bogle.geomock.location.AddressGeocoder
import me.bogle.geomock.location.GeocodeOutcome
import me.bogle.geomock.location.LocationManager
import me.bogle.geomock.location.MockLocationManager
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    val locationManager: LocationManager,
    val mockLocationManager: MockLocationManager,
    private val addressGeocoder: AddressGeocoder
) : ViewModel() {

    // TODO: Use a real database

    private val _addressSearchState = MutableStateFlow<AddressSearchState>(AddressSearchState.Idle)
    val addressSearchState: StateFlow<AddressSearchState> = _addressSearchState.asStateFlow()

    private var searchJob: Job? = null

    fun searchAddress(query: String, bounds: LatLngBounds?) {
        searchJob?.cancel()

        if (query.isBlank()) {
            _addressSearchState.value = AddressSearchState.Idle
            return
        }

        searchJob = viewModelScope.launch {
            _addressSearchState.value = AddressSearchState.Searching
            _addressSearchState.value = when (val outcome = addressGeocoder.search(query, bounds)) {
                is GeocodeOutcome.Success -> AddressSearchState.Results(outcome.results)
                GeocodeOutcome.NoResults -> AddressSearchState.NoResults
                GeocodeOutcome.Unavailable -> AddressSearchState.Unavailable
                GeocodeOutcome.Error -> AddressSearchState.Error
            }
        }
    }

    fun clearAddressSearch() {
        searchJob?.cancel()
        _addressSearchState.value = AddressSearchState.Idle
    }

    fun saveLastRealLocation(latLng: LatLng) = writePreferences { settings ->
        settings[LAST_LOCATION] = latLng.toPreferenceString()
    }

    fun getLastRealLocation(): Flow<LatLng?> =
        readablePreferences()
            .map { settings ->
                settings[LAST_LOCATION]?.toLatLngOrNull()
            }

    fun addLocationToStarred(latLng: LatLng) {
        Timber.d("Adding $latLng to starred locations")
        writePreferences { settings ->
            if (settings[STARRED_LOCATIONS].isNullOrEmpty()) {
                settings[STARRED_LOCATIONS] = latLng.toPreferenceString()
            } else {
                val currentStarred = settings[STARRED_LOCATIONS].orEmpty().split(DELIMITER)
                val newStarred = currentStarred + latLng.toPreferenceString()
                settings[STARRED_LOCATIONS] = newStarred.joinToString(DELIMITER)
            }
        }
    }

    fun removeLocationFromStarred(latLng: LatLng) {
        Timber.d("Removing $latLng to starred locations")
        writePreferences { settings ->
            val currentStarred = settings[STARRED_LOCATIONS].orEmpty().split(DELIMITER)
            val newStarred = currentStarred - latLng.toPreferenceString()
            settings[STARRED_LOCATIONS] = newStarred.joinToString(DELIMITER)
        }
    }

    fun getStarredLocations(): Flow<List<LatLng>> =
        readablePreferences()
            .map { settings ->
                settings[STARRED_LOCATIONS].orEmpty()
                    .split(DELIMITER)
                    .filter { it.isNotEmpty() }
                    .mapNotNull { it.toLatLngOrNull() }
            }
            .distinctUntilChanged()
            .onEach { Timber.d("Starred Location Read: $it") }

    /**
     * Reads the preferences, falling back to empty ones when the file cannot be read. The
     * result is collected straight into composition, so an IOException escaping here used to
     * take down the whole home screen.
     */
    private fun readablePreferences(): Flow<Preferences> =
        context.dataStore.data.catch { exception ->
            if (exception is IOException) {
                Timber.e(exception, "Failed to read preferences")
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }

    private fun writePreferences(transform: suspend (MutablePreferences) -> Unit) {
        viewModelScope.launch {
            try {
                context.dataStore.edit(transform)
            } catch (e: IOException) {
                // A full disk or an unreadable file, not worth crashing over a saved location.
                Timber.e(e, "Failed to write preferences")
            }
        }
    }

    companion object {

        private const val DELIMITER = ";"
        private val STARRED_LOCATIONS = stringPreferencesKey("starred_locations")
        private val LAST_LOCATION = stringPreferencesKey("last_locations")
    }
}

private fun LatLng.toPreferenceString() = "$latitude,$longitude"

/**
 * Parses a `"latitude,longitude"` preference and returns null for anything that is not a
 * valid pair. The value outlives app updates and can be restored from a backup or written by
 * an older version, so a malformed entry must not crash the screen that collects it.
 */
internal fun String.toLatLngOrNull(): LatLng? {
    val parts = split(",")
    if (parts.size != 2) return null

    val latitude = parts[0].trim().toDoubleOrNull() ?: return null
    val longitude = parts[1].trim().toDoubleOrNull() ?: return null
    if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return null

    return LatLng(latitude, longitude)
}
