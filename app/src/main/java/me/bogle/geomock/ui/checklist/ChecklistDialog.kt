package me.bogle.geomock.ui.checklist

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import me.bogle.geomock.util.OnLifecycleEvent

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ChecklistDialog() {
    val checklistViewModel: ChecklistViewModel = hiltViewModel()
    val context = LocalContext.current
    var showDialog by rememberSaveable { mutableStateOf(true) }

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

    if (showDialog) {
        Dialog(
            properties = DialogProperties(
                dismissOnClickOutside = false,
                dismissOnBackPress = false,
                usePlatformDefaultWidth = false
            ),
            onDismissRequest = { /* No-op */ }
        ) {
            AnimatedContent(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(AlertDialogDefaults.containerColor),
                targetState = checklistViewModel.uiState.collectAsStateWithLifecycle().value,
                label = "checklist_content"
            ) { state ->
                when (state) {
                    ChecklistState.Loading -> {
                        Box(
                            modifier = Modifier
                                .defaultMinSize(minHeight = 180.dp)
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    is ChecklistState.Incomplete -> {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(
                                16.dp,
                                Alignment.CenterVertically
                            ),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                modifier = Modifier,
                                imageVector = Icons.Default.Checklist,
                                contentDescription = "",
                                tint = MaterialTheme.colorScheme.secondary
                            )

                            Text(
                                modifier = Modifier.padding(bottom = 8.dp),
                                text = "A few things before starting...",
                                style = MaterialTheme.typography.headlineSmall
                            )

                            ChecklistItems(
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
                    }

                    is ChecklistState.Complete -> {
                        LaunchedEffect(state) {
                            showDialog = false
                        }
                    }
                }
            }
        }
    }
}