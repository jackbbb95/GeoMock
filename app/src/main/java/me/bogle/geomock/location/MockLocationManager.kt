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
import javax.inject.Singleton

@Singleton
class MockLocationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val _currentMockLocation: MutableStateFlow<LatLng?> = MutableStateFlow(null)
    val currentMockLocation = _currentMockLocation.asStateFlow()

    /**
     * Pushes [locationLatLng] to every location provider.
     *
     * @return false when the app is no longer the system's mock location provider, either
     * because the check here says so or because a [SecurityException] came back. The caller
     * must stop looping in that case.
     */
    fun setMockLocation(locationLatLng: LatLng): Boolean {
        if (!context.isSelectedMockLocationApp()) {
            Timber.i("GeoMock is not the selected mock location provider")
            _currentMockLocation.update { null }
            return false
        }

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

        // Turn on mock mode. This throws if the user removes GeoMock from the "select mock
        // location app" setting while the mock is running, which used to crash the app.
        try {
            client.setMockMode(true)
        } catch (e: SecurityException) {
            Timber.e(e, "GeoMock is no longer the selected mock location provider")
            _currentMockLocation.update { null }
            return false
        }

        fun setMockLocationForProvider(provider: String) {
            val location = Location(provider).apply {
                latitude = locationLatLng.latitude
                longitude = locationLatLng.longitude
                accuracy = 10f
                altitude = 0.0
                time = System.currentTimeMillis()
                elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
            }

            try {
                client.setMockLocation(location)
                    .addOnSuccessListener {
                        _currentMockLocation.update { locationLatLng }
                        Timber.i("Set mock location to $locationLatLng for provider $provider")
                    }
                    .addOnFailureListener {
                        _currentMockLocation.update { null }
                        Timber.e(
                            it,
                            "Failed to set mock location to $locationLatLng for provider $provider"
                        )
                    }
            } catch (e: SecurityException) {
                _currentMockLocation.update { null }
                Timber.e(e, "Failed to set mock location to $locationLatLng for provider $provider")
            }
        }

        setMockLocationForProvider(LocationManager.GPS_PROVIDER)
        setMockLocationForProvider(LocationManager.NETWORK_PROVIDER)
        setMockLocationForProvider(LocationManager.PASSIVE_PROVIDER)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setMockLocationForProvider(LocationManager.FUSED_PROVIDER)
        }

        return true
    }

    fun unsetMockLocation() {
        if (!context.isSelectedMockLocationApp()) {
            // Not selected any more, so setMockMode() would only throw.
            _currentMockLocation.update { null }
            return
        }

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

        // Turn off mock mode. Also throws when GeoMock is not the mock location provider,
        // which happens whenever the service is destroyed after the user changed the setting.
        try {
            client.setMockMode(false)
        } catch (e: SecurityException) {
            Timber.e(e, "GeoMock is no longer the selected mock location provider")
        }

        _currentMockLocation.update { null }
    }
}
