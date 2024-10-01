package me.bogle.geomock.ui.checklist

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.launch
import me.bogle.geomock.util.OnLifecycleEvent

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ChecklistBottomSheet() {
    val checklistViewModel: ChecklistViewModel = hiltViewModel()
    val context = LocalContext.current
    val checklistState = checklistViewModel.uiState.collectAsStateWithLifecycle().value
    var openBottomSheet by rememberSaveable { mutableStateOf(true) }
    val sheetState = rememberModalBottomSheetState(
        confirmValueChange = if (checklistState == ChecklistState.Complete) {
            { true }
        } else {
            { false }
        })
    val scope = rememberCoroutineScope()

    val runtimePermissionState = rememberMultiplePermissionsState(
        listOfNotNull(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) android.Manifest.permission.POST_NOTIFICATIONS else null
        )
    )

    OnLifecycleEvent { _, event ->
        if (event == Lifecycle.Event.ON_RESUME) {
            checklistViewModel.performStartupCheck(context)
        }
    }

    if (openBottomSheet) {
        ModalBottomSheet(
            sheetState = sheetState,
            properties = ModalBottomSheetProperties(shouldDismissOnBackPress = false),
            dragHandle = null,
            onDismissRequest = {},
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            AnimatedContent(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .padding(top = 16.dp, bottom = 8.dp),
                targetState = checklistState,
                contentKey = { it::class },
                label = ""
            ) { state ->
                when (state) {
                    ChecklistState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    is ChecklistState.Incomplete -> {
                        Checklist(
                            checklistItems = listOf(
                                state.permissionsItem,
                                state.developerOptionsItem,
                                state.mockLocationProviderItem
                            ),
                            onClick = { item ->
                                checklistViewModel.handleChecklistItemAction(
                                    context = context,
                                    checklistItemType = item.type,
                                    onRequestPermissions = { runtimePermissionState.launchMultiplePermissionRequest() }
                                )
                            }
                        )
                    }

                    is ChecklistState.Complete -> {
                        LaunchedEffect(checklistState) {
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                if (!sheetState.isVisible) {
                                    openBottomSheet = false
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}