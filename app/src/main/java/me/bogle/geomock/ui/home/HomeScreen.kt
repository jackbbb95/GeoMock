package me.bogle.geomock.ui.home

import android.annotation.SuppressLint
import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
    val checkListState = checklistViewModel.uiState
        .collectAsStateWithLifecycle()
        .value
    val mockLocationLatLng = homeViewModel.mockLocationManager
        .currentMockLocation
        .collectAsStateWithLifecycle()
        .value

    // Set the camera position to the current *real* location of the user
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
    LaunchedEffect(cameraPositionState.position.target) {
        centerMarkerState.position = mockLocationLatLng ?: cameraPositionState.position.target
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    shape = FloatingActionButtonDefaults.extendedFabShape,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    elevation = CardDefaults.elevatedCardElevation()
                ) {
                    AnimatedContent(mockLocationLatLng, label = "") {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val icon = it?.let { Icons.Default.GpsFixed } ?: Icons.Default.GpsOff
                            Icon(
                                modifier = Modifier.size(14.dp),
                                imageVector = icon,
                                contentDescription = "location icon"
                            )

                            val text = (it ?: centerMarkerState.position).prettyPrint()
                            Text(
                                text = text,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )


                        }
                    }
                }

                ExtendedFloatingActionButton(
                    text = {
                        AnimatedContent(mockLocationLatLng, label = "floating action button") {
                            val text = it?.let { "Reset" } ?: "Set mock location"
                            Text(text = text)
                        }
                    },
                    icon = {
                        AnimatedContent(mockLocationLatLng, label = "floating action button") {
                            val icon = it?.let { Icons.Filled.Clear } ?: Icons.Filled.Add
                            Icon(
                                imageVector = icon,
                                contentDescription = ""
                            )
                        }
                    },
                    onClick = {
                        scope.launch {
                            if (mockLocationLatLng == null) {
                                val targetLocation = cameraPositionState.position.target

                                val intent =
                                    Intent(context, MockLocationService::class.java).apply {
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
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
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
        }

        ChecklistBottomSheet()
    }
}

@Preview
@Composable
private fun HomeScreenPreview() {
    HomeScreen()
}