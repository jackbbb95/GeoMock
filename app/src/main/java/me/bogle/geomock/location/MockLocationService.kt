package me.bogle.geomock.location

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EditLocation
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.Action
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import me.bogle.geomock.MainActivity
import me.bogle.geomock.R
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MockLocationService : LifecycleService() {

    @Inject
    lateinit var mockLocationManager: MockLocationManager

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (intent?.action == ACTION_CLOSE) {
            stopSelf()
            return START_NOT_STICKY
        }

        // A null intent means the system restarted the service after the process was killed,
        // so there is no coordinate to mock any more. This has to be checked before
        // startForeground(): on Android 12+ a background restart may not start a foreground
        // service, and the refusal is fatal ("Unable to start service ... with null"). NaN
        // also catches a missing extra, which the old null check could not, because
        // getDoubleExtra returns a primitive.
        val mockLatitude = intent?.getDoubleExtra(LATITUDE_EXTRA, Double.NaN) ?: Double.NaN
        val mockLongitude = intent?.getDoubleExtra(LONGITUDE_EXTRA, Double.NaN) ?: Double.NaN

        if (mockLatitude.isNaN() || mockLongitude.isNaN()) {
            stopSelf()
            return START_NOT_STICKY
        }

        // Started for real from the UI, so startForegroundService() is in play and the system
        // requires startForeground() within a few seconds.
        if (!startForegroundSafely()) {
            stopSelf()
            return START_NOT_STICKY
        }

        val mockLatLng = LatLng(mockLatitude, mockLongitude)

        // Will auto-cancel when service lifecycle is destroyed
        lifecycleScope.launch {
            while (isActive) {
                if (!mockLocationManager.setMockLocation(mockLatLng)) {
                    // GeoMock was removed as the system's mock location provider. Every retry
                    // would throw a SecurityException, so stop and let the user set it again.
                    Timber.w("No longer the mock location provider, stopping the service")
                    stopSelf()
                    break
                }
                Timber.d("Location mock was updated: $mockLatLng")
                delay(5000)
            }
        }

        // The coordinates only exist in the start intent, so a sticky restart could never
        // resume the mock anyway; it produced a null intent and stopped immediately.
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        mockLocationManager.unsetMockLocation()
        super.onDestroy()
    }

    /**
     * @return false when the notification could not be posted, which means the service cannot
     * legally keep running in the foreground and the caller has to stop it.
     */
    private fun startForegroundSafely(): Boolean =
        try {
            startForeground(NOTIFICATION_ID, buildNotification())
            true
        } catch (e: SecurityException) {
            // Android 14+ enforces the location permissions for the "location" foreground
            // service type, and they can be revoked while the service is running.
            Timber.e(e, "Not allowed to run the mock location service in the foreground")
            false
        } catch (e: IllegalStateException) {
            // Android 12+ blocks starting a foreground service from the background
            // (ForegroundServiceStartNotAllowedException).
            Timber.e(e, "Starting the foreground service was not allowed")
            false
        }

    private fun buildNotification(): Notification {
        val notificationManager = (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Channel used to notify the user of their continuously mocked location",
            NotificationManager.IMPORTANCE_LOW
        )

        notificationManager.createNotificationChannel(channel)

        val pendingIntent = Intent(this, MainActivity::class.java)
            .apply {
                flags = FLAG_ACTIVITY_SINGLE_TOP
            }.run {
                PendingIntent.getActivity(applicationContext, 0, this, PendingIntent.FLAG_IMMUTABLE)
            }

        val stopPendingIntent = Intent(this, MockLocationService::class.java)
            .apply {
                action = ACTION_CLOSE
            }.run {
                PendingIntent.getService(
                    applicationContext,
                    0,
                    this,
                    PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }

        val stopAction = Action(R.drawable.ic_notification, "Stop", stopPendingIntent)

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("GeoMock is mocking your location")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOngoing(true)
            .addAction(stopAction)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    companion object {

        const val NOTIFICATION_ID = 1523
        const val NOTIFICATION_CHANNEL_ID = "mock_location_channel_id"
        const val LATITUDE_EXTRA = "latitude"
        const val LONGITUDE_EXTRA = "longitude"
        const val ACTION_CLOSE = "action_close"
    }
}
