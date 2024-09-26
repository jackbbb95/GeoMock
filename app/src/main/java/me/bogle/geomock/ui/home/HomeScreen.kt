package me.bogle.geomock.ui.home

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import kotlinx.coroutines.launch
import me.bogle.geomock.ui.checklist.ChecklistBottomSheet
import me.bogle.geomock.ui.checklist.ChecklistViewModel
import me.bogle.geomock.ui.checklist.hasLocationPermission

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun HomeScreen() {
    val checklistViewModel = hiltViewModel<ChecklistViewModel>()
    val homeViewModel = hiltViewModel<HomeViewModel>()
    val scope = rememberCoroutineScope()

    val cameraPositionState = rememberCameraPositionState()
    val centerMarkerState = rememberMarkerState()

    // Set initial camera position to current location
    var hasSetLocationOnInit by rememberSaveable { mutableStateOf(false) }
    val checkListState = checklistViewModel.uiState.collectAsStateWithLifecycle().value
    if (!hasSetLocationOnInit && checkListState.hasLocationPermission()) {
        LaunchedEffect(checklistViewModel) {
            scope.launch {
                homeViewModel.locationManager.getCurrentLocation()?.let { currentLatLng ->
                    val position = CameraPosition(currentLatLng, 10f, 0f, 0f)
                    val cameraUpdate = CameraUpdateFactory.newCameraPosition(position)
                    cameraPositionState.animate(cameraUpdate)
                    hasSetLocationOnInit = true
                }
            }
        }
    }

    // Set marker position to match the center of the current camera position
    centerMarkerState.position = cameraPositionState.position.target

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("Show bottom sheet") },
                icon = { Icon(Icons.Filled.Add, contentDescription = "") },
                onClick = { }
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            GoogleMap(
                uiSettings = MapUiSettings(
                    tiltGesturesEnabled = false,
                    myLocationButtonEnabled = false,
                    zoomControlsEnabled = false,
                    scrollGesturesEnabled = true,
                    scrollGesturesEnabledDuringRotateOrZoom = true
                ),
                onMapClick = { latLng ->
                    scope.launch {
                        val currentPosition = cameraPositionState.position
                        val position = CameraPosition(
                            latLng,
                            currentPosition.zoom,
                            currentPosition.tilt,
                            currentPosition.bearing
                        )
                        val cameraUpdate = CameraUpdateFactory.newCameraPosition(position)
                        cameraPositionState.animate(cameraUpdate)
                    }
                },
                cameraPositionState = cameraPositionState
            ) {
                Marker(state = centerMarkerState)
            }
        }

        ChecklistBottomSheet()
    }
}

@Preview
@Composable
private fun HomeScreenPreview() {
    HomeScreen()
}