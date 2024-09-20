package me.bogle.geomock.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import me.bogle.geomock.ui.checklist.ChecklistBottomSheet

@Composable
fun HomeScreen() {
    val singapore = LatLng(1.35, 103.87)
    val singaporeMarkerState = rememberMarkerState(position = singapore)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(singapore, 10f)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("Show bottom sheet") },
                icon = { Icon(Icons.Filled.Add, contentDescription = "") },
                onClick = {}
            )
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
                    myLocationButtonEnabled = true,
                    zoomControlsEnabled = false,
                    scrollGesturesEnabled = true,
                    scrollGesturesEnabledDuringRotateOrZoom = true
                ),
                cameraPositionState = cameraPositionState
            ) {
                Marker(
                    state = singaporeMarkerState,
                    title = "Singapore",
                    snippet = "Marker in Singapore"
                )
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