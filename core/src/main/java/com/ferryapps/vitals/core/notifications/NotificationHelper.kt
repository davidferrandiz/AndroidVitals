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
import com.ferryapps.vitals.core.domain.alerts.AlertType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Único sitio donde se crean canales y notificaciones (ver CLAUDE.md).
 *
 * Un canal por aviso, para que el usuario pueda silenciar uno sin perder el otro desde los ajustes
 * del sistema. Ninguno sale como notificación emergente: batería con sonido normal (de noche, en
 * silencio) y espacio siempre en silencio.
 */
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_BATTERY = "vitals_alert_battery"
        const val CHANNEL_STORAGE = "vitals_alert_storage"

        /** Canales antiguos: el del servicio en primer plano y el de avisos con prioridad alta. */
        private val LEGACY_CHANNELS = listOf("vitals_monitor", "vitals_alerts")

        private const val ID_BATTERY_HOT = 101
        private const val ID_STORAGE_LOW = 102

        /** Pasado este tiempo la notificación de batería ya no dice nada útil y se quita sola. */
        private const val BATTERY_TIMEOUT_MS = 3 * 3_600_000L
    }

    private val manager: NotificationManager = context.getSystemService(NotificationManager::class.java)

    fun createChannels() {
        LEGACY_CHANNELS.forEach(manager::deleteNotificationChannel)
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(
                    CHANNEL_BATTERY,
                    context.getString(R.string.notif_channel_battery_name),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply { description = context.getString(R.string.notif_channel_battery_desc) },
                NotificationChannel(
                    CHANNEL_STORAGE,
                    context.getString(R.string.notif_channel_storage_name),
                    NotificationManager.IMPORTANCE_LOW
                ).apply { description = context.getString(R.string.notif_channel_storage_desc) },
            )
        )
    }

    /** En Android 13+ hace falta el permiso; en cualquier versión el usuario puede bloquearlas. */
    fun canPostNotifications(): Boolean = hasPermission() && manager.areNotificationsEnabled()

    private fun hasPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /**
     * Muestra un aviso. [contentIntent] es lo que se abre al tocarlo; null = la propia app.
     * [silent] quita sonido y vibración (horas de descanso).
     * Devuelve false si no se pudo mostrar (sin permiso, notificaciones o canal bloqueados).
     */
    fun showAlert(
        type: AlertType,
        title: String,
        text: String,
        contentIntent: Intent? = null,
        silent: Boolean = false,
    ): Boolean {
        if (!canPostNotifications()) return false
        val channelId = channelOf(type)
        if (manager.getNotificationChannel(channelId)?.importance == NotificationManager.IMPORTANCE_NONE) return false

        val id = idOf(type)
        val target = contentIntent ?: context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pending = target?.let {
            PendingIntent.getActivity(
                context,
                id,
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_vitals)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(if (type == AlertType.BATTERY_HOT) NotificationCompat.PRIORITY_DEFAULT else NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .setSilent(silent || type == AlertType.STORAGE_LOW)
            .apply { if (type == AlertType.BATTERY_HOT) setTimeoutAfter(BATTERY_TIMEOUT_MS) }
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        manager.notify(id, notification)
        return true
    }

    /** Quita el aviso si sigue en la barra (el problema ya se ha resuelto). */
    fun dismiss(type: AlertType) {
        manager.cancel(idOf(type))
    }

    private fun channelOf(type: AlertType) = when (type) {
        AlertType.BATTERY_HOT -> CHANNEL_BATTERY
        AlertType.STORAGE_LOW -> CHANNEL_STORAGE
    }

    private fun idOf(type: AlertType) = when (type) {
        AlertType.BATTERY_HOT -> ID_BATTERY_HOT
        AlertType.STORAGE_LOW -> ID_STORAGE_LOW
    }
}
