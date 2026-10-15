package com.ferryapps.vitals.feature.widget

import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.ferryapps.vitals.core.domain.model.BatteryStatus
import com.ferryapps.vitals.designsystem.format.Formatters
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Date

/**
 * Widget de pantalla de inicio: batería y temperatura; con más ancho, también RAM y espacio.
 * Se refresca cada 30 min por el sistema (updatePeriodMillis) y cada 15 min desde
 * VitalsWorker si el historial o los avisos están activos. Cada refresco es una lectura suelta.
 */
class VitalsWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(COMPACT, WIDE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entry = EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
        val snapshot = withContext(Dispatchers.IO) {
            WidgetSnapshot(
                battery = runCatching { entry.battery().current() }.getOrNull(),
                memory = runCatching { entry.memory().current() }.getOrNull(),
                storage = runCatching { entry.storage().current() }.getOrNull(),
                temperatureUnit = entry.settings().settings.first().temperatureUnit,
                updatedAtMs = System.currentTimeMillis(),
            )
        }
        provideContent {
            GlanceTheme {
                WidgetContent(snapshot)
            }
        }
    }

    companion object {
        val WIDE_MIN_WIDTH = 250.dp
        private val COMPACT = DpSize(110.dp, 48.dp)
        private val WIDE = DpSize(WIDE_MIN_WIDTH, 48.dp)
    }
}

class VitalsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = VitalsWidget()
}

@Composable
private fun WidgetContent(snapshot: WidgetSnapshot) {
    val context = LocalContext.current
    val wide = LocalSize.current.width >= VitalsWidget.WIDE_MIN_WIDTH
    val colors = GlanceTheme.colors
    val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        ?: Intent(Intent.ACTION_MAIN).setPackage(context.packageName)
    val battery = snapshot.battery

    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(colors.widgetBackground)
            .cornerRadius(20.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .clickable(actionStartActivity(launch)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = battery?.let { "${Formatters.integer(it.levelPercent)} %" } ?: "—",
                style = TextStyle(color = colors.onSurface, fontSize = 22.sp, fontWeight = FontWeight.Bold),
                maxLines = 1
            )
            Spacer(GlanceModifier.height(2.dp))
            val detail = context.getString(
                when (battery?.status) {
                    BatteryStatus.CHARGING -> R.string.widget_charging
                    BatteryStatus.FULL -> R.string.widget_full
                    else -> R.string.widget_battery
                }
            )
            val temperature = battery?.let {
                Formatters.temperature(it.temperatureCelsius, snapshot.temperatureUnit, decimals = 0)
            }
            Text(
                text = listOfNotNull(detail, temperature).joinToString(" · "),
                style = TextStyle(color = colors.onSurfaceVariant, fontSize = 12.sp),
                maxLines = 1
            )
        }
        if (wide) {
            Spacer(GlanceModifier.width(12.dp))
            Column(modifier = GlanceModifier.defaultWeight()) {
                val memory = snapshot.memory
                Text(
                    text = context.getString(R.string.widget_ram, memory?.let { Formatters.percent(it.usedPercent) } ?: "—"),
                    style = TextStyle(color = colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium),
                    maxLines = 1
                )
                Spacer(GlanceModifier.height(2.dp))
                val storage = snapshot.storage
                Text(
                    text = context.getString(
                        R.string.widget_storage_free,
                        storage?.let { Formatters.bytes(context, it.freeBytes) } ?: "—"
                    ),
                    style = TextStyle(color = colors.onSurfaceVariant, fontSize = 12.sp),
                    maxLines = 1
                )
                Text(
                    text = context.getString(
                        R.string.widget_updated,
                        DateFormat.getTimeFormat(context).format(Date(snapshot.updatedAtMs))
                    ),
                    style = TextStyle(color = colors.onSurfaceVariant, fontSize = 11.sp),
                    maxLines = 1
                )
            }
        }
    }
}
