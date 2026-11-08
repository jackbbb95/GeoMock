package me.bogle.geomock.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import timber.log.Timber
import javax.inject.Inject

class MockLocationProviderManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Determines whether GeoMock is the system's selected mock location provider.
     *
     * [isSelectedMockLocationApp] reads the "android:mock_location" app-op, which is the
     * developer setting itself and the value the location service enforces. It is read locally
     * and immediately, unlike probing with
     * [com.google.android.gms.location.FusedLocationProviderClient.setMockMode], which
     * round-trips through Play services and could keep reporting the previous state for a while
     * after the setting had changed.
     */
    suspend fun checkMockLocationProviderState(): MockLocationProviderState {
        if (!hasLocationPermission()) {
            return MockLocationProviderState.RequiresLocationPermission
        }

        // A selection made in the developer settings can take a moment to become visible here
        repeat(APP_OP_CHECK_ATTEMPTS) { attempt ->
            if (context.isSelectedMockLocationApp()) {
                Timber.i("GeoMock is the selected mock location app")
                return MockLocationProviderState.IsSetAsMockLocationProvider
            }

            if (attempt < APP_OP_CHECK_ATTEMPTS - 1) {
                delay(APP_OP_CHECK_INTERVAL_MS)
            }
        }

        Timber.i("GeoMock is not the selected mock location app")
        return MockLocationProviderState.IsNotSetAsMockLocationProvider
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private companion object {
        const val APP_OP_CHECK_ATTEMPTS = 3
        const val APP_OP_CHECK_INTERVAL_MS = 200L
    }
}

sealed interface MockLocationProviderState {
    data object RequiresLocationPermission : MockLocationProviderState
    data object IsNotSetAsMockLocationProvider : MockLocationProviderState
    data object IsSetAsMockLocationProvider : MockLocationProviderState
}
