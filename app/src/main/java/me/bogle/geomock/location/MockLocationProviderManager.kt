package me.bogle.geomock.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class MockLocationProviderManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _state =
        MutableStateFlow<MockLocationProviderState>(MockLocationProviderState.Loading)
    val state: StateFlow<MockLocationProviderState> = _state.asStateFlow()

    // https://stackoverflow.com/a/78790501
    suspend fun checkMockLocationProviderState() {
        val client = LocationServices.getFusedLocationProviderClient(context)
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

        try {
            client.setMockMode(true).addOnCompleteListener {
                _state.update { MockLocationProviderState.IsPresumedToBeSetAsMockLocationProvider }
            }.await()

            client.setMockMode(false).addOnCompleteListener {
                _state.update { MockLocationProviderState.IsSetAsMockLocationProvider }
            }.await()
        } catch (e: SecurityException) {
            _state.update { MockLocationProviderState.IsNotSetAsMockLocationProvider }
        } catch (e: Exception) {
            _state.update { MockLocationProviderState.ErrorCheckingMockLocationProvider }
        }
    }
}

sealed interface MockLocationProviderState {
    data object Loading : MockLocationProviderState
    data object RequiresLocationPermission : MockLocationProviderState
    data object IsNotSetAsMockLocationProvider : MockLocationProviderState
    data object ErrorCheckingMockLocationProvider : MockLocationProviderState
    data object IsPresumedToBeSetAsMockLocationProvider : MockLocationProviderState
    data object IsSetAsMockLocationProvider : MockLocationProviderState
}