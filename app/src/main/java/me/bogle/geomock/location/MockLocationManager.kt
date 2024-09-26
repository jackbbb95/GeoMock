package me.bogle.geomock.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.SystemClock
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber
import javax.inject.Inject

class MockLocationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val _currentMockLocation: MutableStateFlow<LatLng?> = MutableStateFlow(null)
    val currentMockLocation = _currentMockLocation.asStateFlow()

    fun setMockLocation(locationLatLng: LatLng) {
        val client = LocationServices.getFusedLocationProviderClient(context)
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Timber.i("Location permissions were expected, but were not granted")
        }

        // Turn on mock mode
        client.setMockMode(true)

        // TODO this needs to be moved to a background service so that it can periodically update location

        fun setMockLocationForProvider(provider: String) {
            val location = Location(provider).apply {
                latitude = locationLatLng.latitude
                longitude = locationLatLng.longitude
                accuracy = 10f
                altitude = 0.0
                time = System.currentTimeMillis()
                elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
            }

            client.setMockLocation(location)
                .addOnSuccessListener {
                    _currentMockLocation.update { locationLatLng }
                    Timber.i("Set mock location to $locationLatLng for provider $provider")
                }
                .addOnFailureListener {
                    _currentMockLocation.update { null }
                    Timber.e(it, "Failed to set mock location to $locationLatLng for provider $provider")
                }
        }

        setMockLocationForProvider(LocationManager.GPS_PROVIDER)
        setMockLocationForProvider(LocationManager.NETWORK_PROVIDER)
        setMockLocationForProvider(LocationManager.PASSIVE_PROVIDER)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setMockLocationForProvider(LocationManager.FUSED_PROVIDER)
        }
    }

    fun unsetMockLocation() {
        val client = LocationServices.getFusedLocationProviderClient(context)
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Timber.i("Location permissions were expected, but were not granted")
        }

        // Turn off mock mode
        client.setMockMode(false)

        _currentMockLocation.update { null }
    }
}