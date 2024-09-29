package me.bogle.geomock.location

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
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
        if (intent?.action == ACTION_CLOSE) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification())

        val mockLatitude = intent?.getDoubleExtra(LATITUDE_EXTRA, 0.0)
        val mockLongitude = intent?.getDoubleExtra(LONGITUDE_EXTRA, 0.0)

        if (mockLatitude == null || mockLongitude == null) {
            stopSelf()
            return super.onStartCommand(intent, flags, startId)
        }

        val mockLatLng = LatLng(mockLatitude, mockLongitude)

        // Will auto-cancel when service lifecycle is destroyed
        lifecycleScope.launch {
            while (isActive) {
                mockLocationManager.setMockLocation(mockLatLng)
                Timber.d("Location mock was updated: $mockLatLng")
                delay(5000)
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        mockLocationManager.unsetMockLocation()
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val notificationManager = (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Channel used to notify the user of their continuously mocked location",
            NotificationManager.IMPORTANCE_HIGH
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

        val stopAction = Action(R.drawable.ic_launcher_foreground, "Stop", stopPendingIntent)

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
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