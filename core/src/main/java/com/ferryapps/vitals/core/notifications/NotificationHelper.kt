package com.ferryapps.vitals.core.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.ferryapps.vitals.core.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Único sitio donde se crean canales y notificaciones (ver CLAUDE.md). */
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_ALERTS = "vitals_alerts"

        /** Canal del antiguo servicio en primer plano. Se borra para que no quede en Ajustes del sistema. */
        private const val LEGACY_CHANNEL_MONITOR = "vitals_monitor"

        const val ID_BATTERY_HOT = 101
        const val ID_STORAGE_LOW = 102
    }

    private val manager: NotificationManager = context.getSystemService(NotificationManager::class.java)

    fun createChannels() {
        manager.deleteNotificationChannel(LEGACY_CHANNEL_MONITOR)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ALERTS,
                context.getString(R.string.notif_channel_alerts_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = context.getString(R.string.notif_channel_alerts_desc) }
        )
    }

    /** En Android 13+ hace falta el permiso; en cualquier versión el usuario puede bloquearlas. */
    fun canPostNotifications(): Boolean = hasPermission() && manager.areNotificationsEnabled()

    private fun hasPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /**
     * Muestra un aviso. [contentIntent] es lo que se abre al tocarlo; null = la propia app.
     * Devuelve false si no se pudo mostrar (sin permiso o notificaciones bloqueadas).
     */
    fun showAlert(id: Int, title: String, text: String, contentIntent: Intent? = null): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        if (!manager.areNotificationsEnabled()) return false

        val target = contentIntent ?: context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pending = target?.let {
            PendingIntent.getActivity(
                context,
                id,
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_stat_vitals)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        manager.notify(id, notification)
        return true
    }
}
