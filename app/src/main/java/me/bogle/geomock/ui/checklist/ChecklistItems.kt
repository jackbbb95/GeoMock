package me.bogle.geomock.ui.checklist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import me.bogle.geomock.ui.GeoMockPreview
import me.bogle.geomock.ui.GeoMockThemedPreview

@Composable
fun ChecklistItems(
    checklistItems: List<ChecklistItem>,
    onClick: (ChecklistItem) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(
            16.dp,
            Alignment.CenterVertically
        ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        checklistItems.forEach { item ->
            ChecklistCard(
                modifier = Modifier.fillMaxWidth(),
                description = item.description,
                completionState = item.completionState,
                onClick = { onClick(item) }
            )
        }
    }
}

@Composable
private fun ChecklistCard(
    modifier: Modifier,
    description: String,
    completionState: ChecklistItemState,
    onClick: () -> Unit
) {
    val isEnabled = completionState == ChecklistItemState.INCOMPLETE ||
            completionState == ChecklistItemState.IN_PROGRESS

    val isComplete = completionState == ChecklistItemState.COMPLETE

    Card(
        modifier = modifier,
        onClick = onClick,
        enabled = isEnabled,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(80.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            val foregroundColor =
                if (isEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
            Text(
                modifier = Modifier.weight(1f),
                text = description,
                color = foregroundColor,
                style = if (isComplete) TextStyle(textDecoration = TextDecoration.LineThrough) else TextStyle.Default
            )

            when (completionState) {
                ChecklistItemState.INCOMPLETE -> {
                    Icon(
                        modifier = Modifier.padding(end = 8.dp),
                        imageVector = Icons.Default.Error,
                        contentDescription = "",
                        tint = foregroundColor
                    )
                }

                ChecklistItemState.IN_PROGRESS -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(24.dp),
                        color = foregroundColor
                    )
                }

                ChecklistItemState.LOCKED -> {
                    Icon(
                        modifier = Modifier.padding(end = 8.dp),
                        imageVector = Icons.Default.Lock,
                        contentDescription = "",
                        tint = foregroundColor
                    )
                }

                ChecklistItemState.COMPLETE -> {
                    Icon(
                        modifier = Modifier.padding(end = 8.dp),
                        imageVector = Icons.Default.Check,
                        contentDescription = "",
                        tint = foregroundColor
                    )
                }
            }
        }
    }
}

@GeoMockPreview
@Composable
private fun ChecklistPreview() = GeoMockThemedPreview {
    ChecklistItems(
        checklistItems = listOf(
            ChecklistItem(
                type = ChecklistItemType.PERMISSIONS,
                description = "Fine location access permission is required",
                completionState = ChecklistItemState.COMPLETE
            ),
            ChecklistItem(
                type = ChecklistItemType.DEVELOPER_SETTINGS,
                description = "Developer options must be enabled",
                completionState = ChecklistItemState.IN_PROGRESS
            ),
            ChecklistItem(
                type = ChecklistItemType.DEVELOPER_SETTINGS,
                description = "Developer options must be enabled",
                completionState = ChecklistItemState.LOCKED
            ),
            ChecklistItem(
                type = ChecklistItemType.MOCK_LOCATION_PROVIDER,
                description = "GeoMock must be set as the system's mock location provider",
                completionState = ChecklistItemState.INCOMPLETE
            )
        ),
        onClick = { }
    )
}