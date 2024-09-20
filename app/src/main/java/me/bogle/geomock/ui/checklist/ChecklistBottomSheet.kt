package me.bogle.geomock.ui.checklist

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistBottomSheet() {
    val checklistViewModel: ChecklistViewModel = viewModel()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val sheetState = rememberModalBottomSheetState(confirmValueChange = { false })

    LaunchedEffect(lifecycleOwner) {
        checklistViewModel.performStartupCheck(context)
    }

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = {}
    ) {
        when (val checklistState = checklistViewModel.uiState.collectAsStateWithLifecycle().value) {
            ChecklistState.Loading -> {
                CircularProgressIndicator()
            }

            is ChecklistState.Incomplete -> {
                Checklist(
                    fineLocationItem = checklistState.fineLocationItem,
                    onFineLocationItemClicked = {

                        checklistViewModel.handleAskingForFineLocation(context)
                    },
                    mockLocationProviderItem = checklistState.mockLocationProviderItem,
                    onMockLocationProviderItemClicked = {
                        checklistViewModel.handleSettingMockLocationProvider(
                            context
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