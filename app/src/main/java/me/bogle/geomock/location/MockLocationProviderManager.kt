package me.bogle.geomock.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class MockLocationProviderManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _state =
        MutableStateFlow<MockLocationProviderState>(MockLocationProviderState.Loading)
    val state: StateFlow<MockLocationProviderState> = _state.asStateFlow()

    suspend fun checkMockLocationProviderState() {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            _state.update { MockLocationProviderState.RequiresLocationPermission }

            return
        }

        // This used to probe the fused location client with `setMockMode(true)` and rely on
        // catching the resulting SecurityException. That put the exception inside Play
        // Services' own connection callback, which the app cannot catch, so every return to
        // the app from a device where GeoMock was not selected crashed. The app op is the
        // same signal, read locally.
        _state.update {
            if (context.isSelectedMockLocationApp()) {
                MockLocationProviderState.IsSetAsMockLocationProvider
            } else {
                MockLocationProviderState.IsNotSetAsMockLocationProvider
            }
        }
    }
}

sealed interface MockLocationProviderState {
    data object Loading : MockLocationProviderState
    data object RequiresLocationPermission : MockLocationProviderState
    data object IsNotSetAsMockLocationProvider : MockLocationProviderState
    data object ErrorCheckingMockLocationProvider : MockLocationProviderState
    data object IsSetAsMockLocationProvider : MockLocationProviderState
}
