package me.bogle.geomock.ui.home

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import me.bogle.geomock.location.LocationManager
import me.bogle.geomock.location.MockLocationManager
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    val locationManager: LocationManager,
    val mockLocationManager: MockLocationManager
) : ViewModel()