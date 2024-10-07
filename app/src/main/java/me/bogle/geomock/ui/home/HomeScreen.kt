package me.bogle.geomock.ui.home

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraMoveStartedReason
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.ComposeMapColorScheme
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import me.bogle.geomock.location.MockLocationService
import me.bogle.geomock.ui.checklist.ChecklistDialog
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
    val mainMarkerState = rememberMarkerState()

    // Set initial camera position to current location
    var hasSetLocationOnInitOrUserInteracted by remember { mutableStateOf(false) }
    val checkListState = checklistViewModel.uiState
        .collectAsStateWithLifecycle()
        .value
    val mockLocationLatLng = homeViewModel.mockLocationManager
        .currentMockLocation
        .collectAsStateWithLifecycle()
        .value

    val starredLocationMarkers = homeViewModel.getStarredLocations()
        .collectAsStateWithLifecycle(emptyList())
        .value
        .map { MarkerState.invoke(position = it) }

    LaunchedEffect(Unit) {
        // Set camera position to last known location (if available), on startup
        homeViewModel.getLastRealLocation().first()?.let { currentLatLng ->
            val position = CameraPosition(currentLatLng, 10f, 0f, 0f)
            val cameraUpdate = CameraUpdateFactory.newCameraPosition(position)
            cameraPositionState.animate(cameraUpdate)
        }
    }

    LaunchedEffect(checkListState) {
        if (!hasSetLocationOnInitOrUserInteracted && checkListState.hasLocationPermission()) {
            scope.launch {
                // Check the current *real* location and set the camera position, but only
                // if the user has not interacted with the map yet
                homeViewModel.locationManager.getCurrentLocation()?.let { currentLatLng ->
                    if (!hasSetLocationOnInitOrUserInteracted) {
                        val position = CameraPosition(currentLatLng, 10f, 0f, 0f)
                        val cameraUpdate = CameraUpdateFactory.newCameraPosition(position)
                        cameraPositionState.animate(cameraUpdate)
                        homeViewModel.saveLastRealLocation(currentLatLng)
                        hasSetLocationOnInitOrUserInteracted = true
                    }
                }
            }
        }
    }

    // Cancel any automatic camera panning if user has interacted with the map already
    LaunchedEffect(cameraPositionState.isMoving) {
        if (!hasSetLocationOnInitOrUserInteracted && cameraPositionState.cameraMoveStartedReason == CameraMoveStartedReason.GESTURE) {
            hasSetLocationOnInitOrUserInteracted = true
        }
    }

    // Set marker position to match the center of the current camera position or the current mock location
    LaunchedEffect(cameraPositionState.position.target) {
        mainMarkerState.position = mockLocationLatLng ?: cameraPositionState.position.target
    }

    // Reset camera position to the center of the camera state after mocking is finished
    LaunchedEffect(mockLocationLatLng) {
        if (mockLocationLatLng == null) {
            mainMarkerState.position = cameraPositionState.position.target
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            FloatingActionButtonLayout(
                context = context,
                mockLocationLatLng = mockLocationLatLng,
                mainMarkerState = mainMarkerState,
                cameraPositionState = cameraPositionState,
                scope = scope
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding()
                )
        ) {
            GoogleMap(
                uiSettings = MapUiSettings(
                    tiltGesturesEnabled = false,
                    myLocationButtonEnabled = false,
                    mapToolbarEnabled = false,
                    zoomControlsEnabled = false,
                    rotationGesturesEnabled = false,
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
                cameraPositionState = cameraPositionState,
                mapColorScheme = ComposeMapColorScheme.FOLLOW_SYSTEM
            ) {
                // Show starred markers (minus the currently mocked one)
                if (mockLocationLatLng == null) {
                    starredLocationMarkers.forEach {
                        Marker(
                            state = it,
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)
                        )
                    }
                }

                // Show main marker
                Marker(
                    state = mainMarkerState,
                    icon = if (mockLocationLatLng != null) {
                        BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                    } else {
                        BitmapDescriptorFactory.defaultMarker()
                    }
                )
            }

            AnimatedVisibility(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                visible = mockLocationLatLng != null,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                val isStarred = starredLocationMarkers.any { it.position == mockLocationLatLng }
                FloatingActionButton(
                    onClick = {
                        mockLocationLatLng?.let {
                            if (isStarred) {
                                homeViewModel.removeLocationFromStarred(it)
                            } else {
                                homeViewModel.addLocationToStarred(it)
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (isStarred) {
                            Icons.Default.Star
                        } else {
                            Icons.Default.StarOutline
                        },
                        contentDescription = "star"
                    )
                }
            }
        }

        ChecklistDialog()
    }
}

@Composable
private fun FloatingActionButtonLayout(
    context: Context,
    mockLocationLatLng: LatLng?,
    mainMarkerState: MarkerState,
    cameraPositionState: CameraPositionState,
    scope: CoroutineScope
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
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

                    val text = (it ?: mainMarkerState.position).prettyPrint()
                    Text(
                        text = text,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
        ExtendedFloatingActionButton(
            text = {
                AnimatedContent(mockLocationLatLng, label = "mock location fab") {
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

@Preview
@Composable
private fun HomeScreenPreview() {
    HomeScreen()
}