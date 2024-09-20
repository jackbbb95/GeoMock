package me.bogle.geomock.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState

@Composable
fun HomeScreen() {
    val singapore = LatLng(1.35, 103.87)
    val singaporeMarkerState = rememberMarkerState(position = singapore)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(singapore, 10f)
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        GoogleMap(cameraPositionState = cameraPositionState) {
            Marker(
                state = singaporeMarkerState,
                title = "Singapore",
                snippet = "Marker in Singapore"
            )
        }
    }
}

@Preview
@Composable
private fun HomeScreenPreview() {
    HomeScreen()
}