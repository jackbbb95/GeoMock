package me.bogle.geomock.location

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.annotation.RequiresApi
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.io.IOException
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Forward geocoding backed by the platform [Geocoder].
 *
 * No API key, no billing, no third-party SDK, but the backend is whatever the
 * device ships (Google on most devices, a vendor service on some), so result
 * quality and even availability vary by device. See docs/address-search.md.
 */
@Singleton
class AddressGeocoder @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Resolves [query] to at most [MAX_RESULTS] addresses.
     *
     * [bounds] biases the results towards what is currently on screen, which is
     * the difference between "123 Main St" failing and resolving to the street
     * the user is looking at.
     */
    suspend fun search(query: String, bounds: LatLngBounds? = null): GeocodeOutcome {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return GeocodeOutcome.NoResults

        if (!Geocoder.isPresent()) {
            Timber.w("No geocoder backend available on this device")
            return GeocodeOutcome.Unavailable
        }

        val geocoder = Geocoder(context, Locale.getDefault())

        var addresses = geocodeOrNull(geocoder, trimmed, bounds) ?: return GeocodeOutcome.Error

        // The viewport bias is what makes "123 Main St" resolve to the street the
        // user is looking at, but it also hides well-known places elsewhere in the
        // world. If the biased lookup finds nothing, try again without it.
        if (addresses.isEmpty() && bounds != null) {
            addresses = geocodeOrNull(geocoder, trimmed, null) ?: return GeocodeOutcome.Error
        }

        val results = addresses.mapNotNull { it.toGeocodeResult() }
        return if (results.isEmpty()) GeocodeOutcome.NoResults else GeocodeOutcome.Success(results)
    }

    /**
     * Returns null when the lookup failed (network, timeout) and an empty list
     * when it succeeded but matched nothing.
     */
    private suspend fun geocodeOrNull(
        geocoder: Geocoder,
        query: String,
        bounds: LatLngBounds?
    ): List<Address>? = try {
        withTimeoutOrNull(TIMEOUT_MS) { geocode(geocoder, query, bounds) }
    } catch (e: IOException) {
        Timber.w(e, "Geocoding failed for %s", query)
        null
    } catch (e: IllegalArgumentException) {
        Timber.w(e, "Geocoder rejected query %s", query)
        emptyList()
    }

    private suspend fun geocode(
        geocoder: Geocoder,
        query: String,
        bounds: LatLngBounds?
    ): List<Address> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocodeAsync(geocoder, query, bounds)
        } else {
            geocodeBlocking(geocoder, query, bounds)
        }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private suspend fun geocodeAsync(
        geocoder: Geocoder,
        query: String,
        bounds: LatLngBounds?
    ): List<Address> = suspendCancellableCoroutine { continuation ->
        val listener = Geocoder.GeocodeListener { addresses ->
            if (continuation.isActive) continuation.resume(addresses ?: emptyList())
        }

        try {
            if (bounds != null) {
                geocoder.getFromLocationName(
                    query,
                    MAX_RESULTS,
                    bounds.southwest.latitude,
                    bounds.southwest.longitude,
                    bounds.northeast.latitude,
                    bounds.northeast.longitude,
                    listener
                )
            } else {
                geocoder.getFromLocationName(query, MAX_RESULTS, listener)
            }
        } catch (e: IOException) {
            if (continuation.isActive) continuation.resumeWithException(e)
        } catch (e: IllegalArgumentException) {
            if (continuation.isActive) continuation.resumeWithException(e)
        }
    }

    /**
     * The blocking overloads are deprecated from API 33, but they are the only
     * option below it and still work. Called off the main thread.
     */
    @Suppress("DEPRECATION")
    private suspend fun geocodeBlocking(
        geocoder: Geocoder,
        query: String,
        bounds: LatLngBounds?
    ): List<Address> = withContext(Dispatchers.IO) {
        if (bounds != null) {
            geocoder.getFromLocationName(
                query,
                MAX_RESULTS,
                bounds.southwest.latitude,
                bounds.southwest.longitude,
                bounds.northeast.latitude,
                bounds.northeast.longitude
            )
        } else {
            geocoder.getFromLocationName(query, MAX_RESULTS)
        } ?: emptyList()
    }

    private fun Address.toGeocodeResult(): GeocodeResult? {
        val label = (0..maxAddressLineIndex)
            .mapNotNull { getAddressLine(it) }
            .filter { it.isNotBlank() }
            .joinToString(", ")
            .ifBlank {
                listOfNotNull(featureName, locality, countryName)
                    .filter { it.isNotBlank() }
                    .joinToString(", ")
            }

        if (label.isBlank()) return null
        return GeocodeResult(label = label, latLng = LatLng(latitude, longitude))
    }

    private companion object {
        const val MAX_RESULTS = 5
        const val TIMEOUT_MS = 15_000L
    }
}

data class GeocodeResult(
    val label: String,
    val latLng: LatLng
)

sealed interface GeocodeOutcome {
    data class Success(val results: List<GeocodeResult>) : GeocodeOutcome
    data object NoResults : GeocodeOutcome
    data object Unavailable : GeocodeOutcome
    data object Error : GeocodeOutcome
}
