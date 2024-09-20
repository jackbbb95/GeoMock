package me.bogle.geomock.ui.checklist

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
import kotlinx.coroutines.launch

class ChecklistViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<ChecklistState>(ChecklistState.Loading)
    val uiState: StateFlow<ChecklistState> = _uiState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ChecklistState.Loading
        )

    suspend fun performStartupCheck(context: Context) {
        _uiState.update { ChecklistState.Loading }

        val hasFineLocationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val isSetAsMockLocationProvider = false // TODO

        _uiState.update {
            ChecklistState.Incomplete(
                fineLocationItem = ChecklistItem(
                    description = "Fine location access permission is required",
                    completionState = if (hasFineLocationPermission) ChecklistItemState.COMPLETE else ChecklistItemState.INCOMPLETE
                ),
                mockLocationProviderItem = ChecklistItem(
                    description = "GeoMock must be set as the system's mock location provider",
                    completionState = if (isSetAsMockLocationProvider) ChecklistItemState.COMPLETE else ChecklistItemState.INCOMPLETE
                )
            )
        }
    }

    fun handleAskingForFineLocation(context: Context) {
        viewModelScope.launch {
            (_uiState.value as? ChecklistState.Incomplete)?.let { current ->
                reduceState(
                    newState = current.copy(
                        fineLocationItem = current.fineLocationItem.copy(
                            completionState = ChecklistItemState.IN_PROGRESS
                        )
                    )
                )

                // TODO
                delay(2000)

                reduceState(
                    newState = current.copy(
                        fineLocationItem = current.fineLocationItem.copy(
                            completionState = ChecklistItemState.COMPLETE
                        )
                    )
                )
            }
        }
    }

    fun handleSettingMockLocationProvider(context: Context) {
        viewModelScope.launch {
            (_uiState.value as? ChecklistState.Incomplete)?.let { current ->
                reduceState(
                    newState = current.copy(
                        mockLocationProviderItem = current.mockLocationProviderItem.copy(
                            completionState = ChecklistItemState.IN_PROGRESS
                        )
                    )
                )

                // TODO
                delay(2000)

                reduceState(
                    newState = current.copy(
                        mockLocationProviderItem = current.mockLocationProviderItem.copy(
                            completionState = ChecklistItemState.COMPLETE
                        )
                    )
                )
            }
        }
    }

    private fun reduceState(newState: ChecklistState.Incomplete) {
        _uiState.update {
            if (
                newState.fineLocationItem.completionState == ChecklistItemState.COMPLETE &&
                newState.mockLocationProviderItem.completionState == ChecklistItemState.COMPLETE
            ) {
                ChecklistState.Complete
            } else {
                newState
            }
        }
    }
}

sealed interface ChecklistState {
    data object Loading : ChecklistState
    data class Incomplete(
        val fineLocationItem: ChecklistItem,
        val mockLocationProviderItem: ChecklistItem
    ) : ChecklistState

    data object Complete : ChecklistState
}

data class ChecklistItem(
    val description: String,
    val completionState: ChecklistItemState = ChecklistItemState.INCOMPLETE
)

enum class ChecklistItemState {
    INCOMPLETE, IN_PROGRESS, COMPLETE
}
