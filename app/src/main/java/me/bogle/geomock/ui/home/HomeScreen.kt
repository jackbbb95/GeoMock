package me.bogle.geomock.ui.home

import android.annotation.SuppressLint
import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import me.bogle.geomock.location.MockLocationService
import me.bogle.geomock.ui.checklist.ChecklistBottomSheet
import me.bogle.geomock.ui.checklist.ChecklistViewModel
import me.bogle.geomock.ui.checklist.hasLocationPermission
import me.bogle.geomock.util.prettyPrint

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun HomeScreen() {
    val checklistViewModel = hiltViewModel<ChecklistViewModel>()
    val homeViewModel = hiltViewModel<HomeViewModel>()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val cameraPositionState = rememberCameraPositionState()
    val centerMarkerState = rememberMarkerState()

    // Set initial camera position to current location
    var hasSetLocationOnInit by remember { mutableStateOf(false) }
    val checkListState = checklistViewModel.uiState.collectAsStateWithLifecycle().value
    val mockLocationLatLng =
        homeViewModel.mockLocationManager.currentMockLocation.collectAsStateWithLifecycle().value
    LaunchedEffect(checkListState) {
        if (!hasSetLocationOnInit && checkListState.hasLocationPermission()) {
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
    centerMarkerState.position = mockLocationLatLng ?: cameraPositionState.position.target

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = {
                    Text(text = if (mockLocationLatLng == null) "Set mock location" else "Reset")
                },
                icon = {
                    val icon =
                        if (mockLocationLatLng == null) Icons.Filled.Add else Icons.Filled.Clear
                    Icon(
                        imageVector = icon,
                        contentDescription = ""
                    )
                },
                onClick = {
                    scope.launch {
                        if (mockLocationLatLng == null) {
                            val targetLocation = cameraPositionState.position.target

                            val intent = Intent(context, MockLocationService::class.java).apply {
                                putExtra(
                                    MockLocationService.LATITUDE_EXTRA,
                                    targetLocation.latitude
                                )
                                putExtra(
                                    MockLocationService.LONGITUDE_EXTRA,
                                    targetLocation.longitude
                                )
                            }
                            context.startForegroundService(intent)
                        } else {
                            val intent = Intent(context, MockLocationService::class.java)
                            context.stopService(intent)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
        ) {
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

            Column {
                Text(
                    "Current marker location: ${centerMarkerState.position.prettyPrint()}",
                    color = Color.Red
                )

                mockLocationLatLng?.let {
                    Text(
                        "Current mock location: ${it.prettyPrint()}",
                        color = Color.Black
                    )
                }
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