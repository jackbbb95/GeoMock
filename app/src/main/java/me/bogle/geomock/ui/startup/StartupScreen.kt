package me.bogle.geomock.ui.startup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun StartupScreen(
    onNavigateToHome: () -> Unit
) {
    val vm = viewModel<StartupViewModel>()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner) {
        vm.performStartupCheck(context = context)
    }

    Scaffold { inset ->
        when (val state = vm.uiState.collectAsStateWithLifecycle().value) {
            StartupUIState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(inset),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is StartupUIState.Result -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(inset)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ChecklistCard(
                        modifier = Modifier.fillMaxWidth(),
                        description = state.fineLocationItem.description,
                        isComplete = state.fineLocationItem.isComplete
                    )

                    ChecklistCard(
                        modifier = Modifier.fillMaxWidth(),
                        description = state.mockLocationProviderItem.description,
                        isComplete = state.mockLocationProviderItem.isComplete
                    )
                }
            }

            else -> {}
        }
    }
}

@Composable
private fun ChecklistCard(
    modifier: Modifier,
    description: String,
    isComplete: Boolean
) {
    Card(
        modifier = modifier,
        border = BorderStroke(
            width = 3.dp,
            color = if (isComplete) Color(0xFF097969) else Color(0xFFC70039)
        ),
        onClick = {}
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(80.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = description
            )
            Icon(
                modifier = Modifier.padding(start = 4.dp),
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = ""
            )
        }
    }
}

@Preview
@Composable
private fun StartupScreenPreview() {
    StartupScreen { }
}