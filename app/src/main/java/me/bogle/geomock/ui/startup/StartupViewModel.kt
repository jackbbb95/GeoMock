package me.bogle.geomock.ui.startup

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class StartupViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<StartupUIState>(StartupUIState.Initializing)
    val uiState: StateFlow<StartupUIState> = _uiState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StartupUIState.Initializing
        )

    suspend fun performStartupCheck(context: Context) {
        _uiState.update { StartupUIState.Loading }

        delay(500)

        val hasFineLocationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val isSetAsMockLocationProvider = false

        _uiState.update {
            StartupUIState.Result(
                fineLocationItem = ChecklistItem(
                    description = "Fine location access permission is required",
                    isComplete = hasFineLocationPermission
                ),
                mockLocationProviderItem = ChecklistItem(
                    description = "GeoMock must be set as the system's mock location provider",
                    isComplete = isSetAsMockLocationProvider
                )
            )
        }
    }
}

sealed interface StartupUIState {
    data object Initializing : StartupUIState
    data object Loading : StartupUIState
    data class Result(
        val fineLocationItem: ChecklistItem,
        val mockLocationProviderItem: ChecklistItem
    ) : StartupUIState
}

data class ChecklistItem(
    val description: String,
    val isComplete: Boolean = false
)
