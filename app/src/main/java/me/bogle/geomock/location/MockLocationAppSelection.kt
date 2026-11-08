package me.bogle.geomock.location

import android.app.AppOpsManager
import android.content.Context
import android.os.Process
import timber.log.Timber

/**
 * Whether the user picked GeoMock under "Settings > Developer options > Select mock location
 * app".
 *
 * Checking this before touching the fused location client is not just an optimisation. When
 * GeoMock is not selected, Play Services throws `SecurityException` from inside its own
 * connection callback on a background handler thread — not through the `Task` it returns. No
 * try/catch at the call site can see that, and it terminates the process.
 */
internal fun Context.isSelectedMockLocationApp(): Boolean {
    val appOps = getSystemService(AppOpsManager::class.java) ?: return false

    val mode = try {
        appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_MOCK_LOCATION,
            Process.myUid(),
            packageName
        )
    } catch (e: SecurityException) {
        Timber.e(e, "Could not read the mock location app op")
        return false
    }

    return mode == AppOpsManager.MODE_ALLOWED
}
