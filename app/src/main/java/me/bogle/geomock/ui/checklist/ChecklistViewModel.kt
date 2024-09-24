package me.bogle.geomock.ui.checklist

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.bogle.geomock.location.MockLocationProviderManager
import me.bogle.geomock.location.MockLocationProviderState
import me.bogle.geomock.ui.checklist.SettingsConstants.EXTRA_BUILD_NUMBER
import me.bogle.geomock.ui.checklist.SettingsConstants.EXTRA_FRAGMENT_ARG_KEY
import me.bogle.geomock.ui.checklist.SettingsConstants.EXTRA_MOCK_LOCATION_APP
import me.bogle.geomock.ui.checklist.SettingsConstants.EXTRA_SHOW_FRAGMENT_ARGUMENTS
import javax.inject.Inject


@HiltViewModel
class ChecklistViewModel @Inject constructor(
    private val mockLocationProviderManager: MockLocationProviderManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChecklistState>(ChecklistState.Loading)
    val uiState: StateFlow<ChecklistState> = _uiState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ChecklistState.Loading
        )

    fun performStartupCheck(context: Context) {
        viewModelScope.launch {
            _uiState.update { ChecklistState.Loading }
            mockLocationProviderManager.checkMockLocationProviderState()

            val hasFineLocationPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            val isDeveloperOptionsEnabled = Settings.Secure.getInt(
                context.contentResolver,
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
                0
            ) == 1

            val isSetAsMockLocationProvider =
                mockLocationProviderManager.state.value == MockLocationProviderState.IsSetAsMockLocationProvider

            val state = ChecklistState.Incomplete(
                fineLocationItem = ChecklistItem(
                    type = ChecklistItemType.LOCATION,
                    description = "Fine location access permission is required",
                    completionState = if (hasFineLocationPermission) ChecklistItemState.COMPLETE else ChecklistItemState.INCOMPLETE
                ),
                developerOptionsItem = ChecklistItem(
                    type = ChecklistItemType.DEVELOPER_SETTINGS,
                    description = "Developer options must be enabled",
                    completionState = if (isDeveloperOptionsEnabled) ChecklistItemState.COMPLETE else ChecklistItemState.INCOMPLETE
                ),
                mockLocationProviderItem = ChecklistItem(
                    type = ChecklistItemType.MOCK_LOCATION_PROVIDER,
                    description = "GeoMock must be set as the system's mock location provider",
                    completionState = if (isSetAsMockLocationProvider) ChecklistItemState.COMPLETE else ChecklistItemState.INCOMPLETE
                )
            )

            reduceState(state)
        }
    }

    fun handleChecklistItemAction(
        context: Context,
        checklistItemType: ChecklistItemType,
        onRequestLocationPermission: () -> Unit
    ) {
        when (checklistItemType) {
            ChecklistItemType.LOCATION -> handleAskingForFineLocation(onRequestLocationPermission)
            ChecklistItemType.DEVELOPER_SETTINGS -> handleEnablingDeveloperOptions(context)
            ChecklistItemType.MOCK_LOCATION_PROVIDER -> handleSettingMockLocationProvider(context)
        }
    }

    private fun handleAskingForFineLocation(onRequestLocationPermission: () -> Unit) {
        viewModelScope.launch {
            (_uiState.value as? ChecklistState.Incomplete)?.let { current ->
                reduceState(
                    newState = current.copy(
                        fineLocationItem = current.fineLocationItem.copy(
                            completionState = ChecklistItemState.IN_PROGRESS
                        )
                    )
                )

                onRequestLocationPermission()

                // New state will be reduced in ON_RESUME
            }
        }
    }

    private fun handleEnablingDeveloperOptions(context: Context) {
        viewModelScope.launch {
            (_uiState.value as? ChecklistState.Incomplete)?.let { current ->
                reduceState(
                    newState = current.copy(
                        developerOptionsItem = current.developerOptionsItem.copy(
                            completionState = ChecklistItemState.IN_PROGRESS
                        )
                    )
                )

                val intent = Intent(Settings.ACTION_DEVICE_INFO_SETTINGS).apply {
                    val bundle = bundleOf(EXTRA_FRAGMENT_ARG_KEY to EXTRA_BUILD_NUMBER)
                    putExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS, bundle)
                }

                context.startActivity(intent)

                // New state will be reduced in ON_RESUME
            }
        }
    }

    private fun handleSettingMockLocationProvider(context: Context) {
        viewModelScope.launch {
            (_uiState.value as? ChecklistState.Incomplete)?.let { current ->
                reduceState(
                    newState = current.copy(
                        mockLocationProviderItem = current.mockLocationProviderItem.copy(
                            completionState = ChecklistItemState.IN_PROGRESS
                        )
                    )
                )

                val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
                    val bundle = bundleOf(EXTRA_FRAGMENT_ARG_KEY to EXTRA_MOCK_LOCATION_APP)
                    putExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS, bundle)
                }

                context.startActivity(intent)

                // New state will be reduced in ON_RESUME
            }
        }
    }

    private fun reduceState(newState: ChecklistState.Incomplete) {
        _uiState.update {
            if (
                newState.fineLocationItem.completionState == ChecklistItemState.COMPLETE &&
                newState.developerOptionsItem.completionState == ChecklistItemState.COMPLETE &&
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
        val developerOptionsItem: ChecklistItem,
        val mockLocationProviderItem: ChecklistItem
    ) : ChecklistState

    data object Complete : ChecklistState
}

data class ChecklistItem(
    val type: ChecklistItemType,
    val description: String,
    val completionState: ChecklistItemState = ChecklistItemState.INCOMPLETE
)

enum class ChecklistItemType {
    LOCATION, DEVELOPER_SETTINGS, MOCK_LOCATION_PROVIDER
}

enum class ChecklistItemState {
    INCOMPLETE, IN_PROGRESS, COMPLETE
}

private object SettingsConstants {
    const val EXTRA_FRAGMENT_ARG_KEY = ":settings:fragment_args_key"
    const val EXTRA_SHOW_FRAGMENT_ARGUMENTS = ":settings:show_fragment_args"

    const val EXTRA_BUILD_NUMBER = "build_number"
    const val EXTRA_MOCK_LOCATION_APP = "mock_location_app"
}