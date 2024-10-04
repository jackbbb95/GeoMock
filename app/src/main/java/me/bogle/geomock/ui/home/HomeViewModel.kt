package me.bogle.geomock.ui.home

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import me.bogle.geomock.data.dataStore
import me.bogle.geomock.location.LocationManager
import me.bogle.geomock.location.MockLocationManager
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    val locationManager: LocationManager,
    val mockLocationManager: MockLocationManager
) : ViewModel() {

    // TODO: Use a real database
    fun addLocationToStarred(latLng: LatLng) {
        viewModelScope.launch {
            Timber.d("Adding $latLng to starred locations")
            context.dataStore.edit { settings ->
                if (settings[STARRED_LOCATIONS].isNullOrEmpty()) {
                    settings[STARRED_LOCATIONS] = latLng.toPreferenceString()
                } else {
                    val currentStarred = settings[STARRED_LOCATIONS].orEmpty().split(DELIMITER)
                    val newStarred = currentStarred + latLng.toPreferenceString()
                    settings[STARRED_LOCATIONS] = newStarred.joinToString(DELIMITER)
                }
            }
        }
    }

    fun removeLocationFromStarred(latLng: LatLng) {
        viewModelScope.launch {
            Timber.d("Removing $latLng to starred locations")
            context.dataStore.edit { settings ->
                val currentStarred = settings[STARRED_LOCATIONS].orEmpty().split(DELIMITER)
                val newStarred = currentStarred - latLng.toPreferenceString()
                settings[STARRED_LOCATIONS] = newStarred.joinToString(DELIMITER)
            }
        }
    }

    fun getStarredLocations(): Flow<List<LatLng>> =
        context.dataStore.data
            .distinctUntilChanged()
            .map { settings ->
                settings[STARRED_LOCATIONS].orEmpty()
                    .split(DELIMITER)
                    .filter { it.isNotEmpty() }
                    .map { it.toLatLng() }
            }.onEach { Timber.d("Starred Location Read: $it") }

    companion object {

        private const val DELIMITER = ";"
        private val STARRED_LOCATIONS = stringPreferencesKey("starred_locations")
    }
}

private fun LatLng.toPreferenceString() = "$latitude,$longitude"

private fun String.toLatLng() = this
    .split(",")
    .let { it[0].toDouble() to it[1].toDouble() }
    .let { LatLng(it.first, it.second) }