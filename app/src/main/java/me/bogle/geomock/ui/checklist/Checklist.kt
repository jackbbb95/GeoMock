package me.bogle.geomock.ui.checklist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import me.bogle.geomock.ui.GeoMockPreview
import me.bogle.geomock.ui.GeoMockThemedPreview

@Composable
fun Checklist(
    checklistItems: List<ChecklistItem>,
    onClick: (ChecklistItem) -> Unit,
) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
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
    Card(
        modifier = modifier,
        border = BorderStroke(
            width = 3.dp,
            color = when (completionState) {
                ChecklistItemState.INCOMPLETE -> Color(0xFFC70039)
                ChecklistItemState.IN_PROGRESS -> Color.DarkGray
                ChecklistItemState.COMPLETE -> Color(0xFF097969)
            }
        ),
        onClick = onClick,
        enabled = completionState == ChecklistItemState.INCOMPLETE
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

@GeoMockPreview
@Composable
private fun ChecklistPreview() = GeoMockThemedPreview {
    Checklist(
        checklistItems = listOf(
            ChecklistItem(
                type = ChecklistItemType.LOCATION,
                description = "Fine location access permission is required",
                completionState = ChecklistItemState.COMPLETE
            ),
            ChecklistItem(
                type = ChecklistItemType.DEVELOPER_SETTINGS,
                description = "Developer options must be enabled",
                completionState = ChecklistItemState.INCOMPLETE
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