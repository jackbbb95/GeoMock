package me.bogle.geomock.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

class LocationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    suspend fun getCurrentLocation(): LatLng? {
        val client = LocationServices.getFusedLocationProviderClient(context)

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Timber.i("Location permissions were expected, but were not granted")
            return null
        }

        return try {
            client.getCurrentLocation(
                CurrentLocationRequest.Builder().build(),
                CancellationTokenSource().token
            )
                .addOnSuccessListener { Timber.i("Successfully retrieved current location: $it") }
                .await()?.let { LatLng(it.latitude, it.longitude) }
        } catch (e: CancellationException) {
            // Never swallow cancellation, it would break structured concurrency.
            throw e
        } catch (e: Exception) {
            // A failed fix is routine: location services off, no fix yet, request timed out,
            // or Play Services returned an error. `await()` rethrows the failure, so without
            // this the app crashed instead of just falling back to "no known location".
            Timber.e(e, "Failed to retrieve current location")
            null
        }
    }
}
