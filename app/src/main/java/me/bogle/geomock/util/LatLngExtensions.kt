package me.bogle.geomock.util

import com.google.android.gms.maps.model.LatLng
import java.util.Locale

fun LatLng.prettyPrint(): String =
    "${this.latitude.roundToCoordinate()}, ${this.longitude.roundToCoordinate()}"

private fun Double.roundToCoordinate(): String =
    String.format(Locale.getDefault(), "%.5f", this)