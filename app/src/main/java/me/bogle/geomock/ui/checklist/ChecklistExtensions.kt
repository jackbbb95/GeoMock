package me.bogle.geomock.ui.checklist

fun ChecklistState.hasLocationPermission() = this is ChecklistState.Complete ||
        this is ChecklistState.Incomplete &&
        this.permissionsItem.completionState == ChecklistItemState.COMPLETE