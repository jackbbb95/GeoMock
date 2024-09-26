package me.bogle.geomock.location

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class MockLocationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    suspend fun setCurrentLocation() {
        // TODO
    }
}