package me.bogle.geomock.ui.home

import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The starred and last-known locations live in DataStore as `"latitude,longitude"` strings.
 * They outlive app updates and can come back from a backup, so anything malformed has to be
 * rejected instead of throwing inside the flow the home screen collects.
 */
class LatLngPreferenceParsingTest {

    @Test
    fun `parses a well formed pair`() {
        assertEquals(LatLng(51.5074, -0.1278), "51.5074,-0.1278".toLatLngOrNull())
    }

    @Test
    fun `tolerates surrounding whitespace`() {
        assertEquals(LatLng(51.5074, -0.1278), " 51.5074 , -0.1278 ".toLatLngOrNull())
    }

    @Test
    fun `handles negative and zero coordinates`() {
        assertEquals(LatLng(0.0, 0.0), "0.0,0.0".toLatLngOrNull())
        assertEquals(LatLng(-33.8688, 151.2093), "-33.8688,151.2093".toLatLngOrNull())
    }

    @Test
    fun `rejects an empty value`() {
        assertNull("".toLatLngOrNull())
    }

    @Test
    fun `rejects a single coordinate`() {
        // This is what crashed with IndexOutOfBoundsException: split() yields one element.
        assertNull("51.5074".toLatLngOrNull())
    }

    @Test
    fun `rejects more than two coordinates`() {
        assertNull("51.5074,-0.1278,17.0".toLatLngOrNull())
    }

    @Test
    fun `rejects non numeric values`() {
        assertNull("null,null".toLatLngOrNull())
        assertNull("NaN,NaN".toLatLngOrNull())
        assertNull("51.5074,notanumber".toLatLngOrNull())
    }

    @Test
    fun `rejects coordinates outside the valid range`() {
        // LatLng() itself throws IllegalArgumentException for these.
        assertNull("91.0,0.0".toLatLngOrNull())
        assertNull("-91.0,0.0".toLatLngOrNull())
        assertNull("0.0,181.0".toLatLngOrNull())
        assertNull("0.0,-181.0".toLatLngOrNull())
    }

    @Test
    fun `rejects the separator only`() {
        assertNull(",".toLatLngOrNull())
    }
}
