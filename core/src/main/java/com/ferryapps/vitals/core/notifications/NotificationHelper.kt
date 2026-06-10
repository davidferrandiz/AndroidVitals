package com.ferryapps.vitals.core.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_MONITOR = "vitals_monitor"
        const val CHANNEL_ALERTS = "vitals_alerts"
        const val NOTIFICATION_ID_FOREGROUND = 1
    }

    private val manager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createChannels() {
        listOf(
            NotificationChannel(CHANNEL_MONITOR, "Monitor", NotificationManager.IMPORTANCE_LOW)
                .apply { description = "Ongoing system vitals monitoring" },
            NotificationChannel(CHANNEL_ALERTS, "Alerts", NotificationManager.IMPORTANCE_HIGH)
                .apply { description = "Threshold alerts for CPU and RAM" }
        ).forEach(manager::createNotificationChannel)
    }

    fun buildForegroundNotification(title: String, text: String): Notification =
        NotificationCompat.Builder(context, CHANNEL_MONITOR)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()

    fun buildAlertNotification(title: String, text: String): Notification =
        NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setAutoCancel(true)
            .build()
}
