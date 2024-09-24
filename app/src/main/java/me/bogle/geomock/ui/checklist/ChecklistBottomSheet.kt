package me.bogle.geomock.ui.checklist

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import me.bogle.geomock.util.OnLifecycleEvent

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ChecklistBottomSheet() {
    val checklistViewModel: ChecklistViewModel = hiltViewModel()
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(confirmValueChange = { false })

    OnLifecycleEvent { _, event ->
        if (event == Lifecycle.Event.ON_RESUME) {
            checklistViewModel.performStartupCheck(context)
        }
    }

    val locationPermissionState = rememberMultiplePermissionsState(
        listOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = {}
    ) {
        when (val checklistState = checklistViewModel.uiState.collectAsStateWithLifecycle().value) {
            ChecklistState.Loading -> {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is ChecklistState.Incomplete -> {
                Checklist(
                    checklistItems = listOf(
                        checklistState.fineLocationItem,
                        checklistState.developerOptionsItem,
                        checklistState.mockLocationProviderItem
                    ),
                    onClick = { item ->
                        checklistViewModel.handleChecklistItemAction(
                            context = context,
                            checklistItemType = item.type,
                            onRequestLocationPermission = { locationPermissionState.launchMultiplePermissionRequest() }
                        )
                    }
                )
            }

            is ChecklistState.Complete -> {
                LaunchedEffect(checklistState) {
                    sheetState.hide()
                }
            }
        }
    }
}