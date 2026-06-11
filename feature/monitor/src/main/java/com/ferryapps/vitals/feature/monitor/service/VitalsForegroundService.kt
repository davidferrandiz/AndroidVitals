package com.ferryapps.vitals.feature.monitor.service

// Foreground Service desactivado: Play Store requiere declaración formal + vídeo
// justificativo para FOREGROUND_SERVICE_SPECIAL_USE. Las métricas se recogen
// directamente desde MonitorViewModel. Descomentar si se decide activar en el futuro.

/*
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.ferryapps.vitals.core.domain.usecase.GetCpuUsageUseCase
import com.ferryapps.vitals.core.domain.usecase.GetMemoryUseCase
import com.ferryapps.vitals.core.domain.usecase.GetThreadCountUseCase
import com.ferryapps.vitals.core.notifications.NotificationHelper
import com.ferryapps.vitals.feature.monitor.ui.MonitorUiState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

// Para activar:
// 1. Añadir al app/AndroidManifest.xml:
//      <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
//      <uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
// 2. Añadir al feature/monitor/AndroidManifest.xml dentro de <application>:
//      <service
//          android:name="com.ferryapps.vitals.feature.monitor.service.VitalsForegroundService"
//          android:foregroundServiceType="specialUse"
//          android:exported="false" />
// 3. Declarar en Play Console → App content → Foreground service special use
// 4. En MonitorViewModel.init: context.startForegroundService(Intent(context, VitalsForegroundService::class.java))

@AndroidEntryPoint
class VitalsForegroundService : Service() {

    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var getCpuUsage: GetCpuUsageUseCase
    @Inject lateinit var getMemory: GetMemoryUseCase
    @Inject lateinit var getThreadCount: GetThreadCountUseCase
    @Inject lateinit var serviceState: MonitorServiceState

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var collectingJob: Job? = null

    private val notificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(
            NotificationHelper.NOTIFICATION_ID_FOREGROUND,
            notificationHelper.buildForegroundNotification("Vitals", "Iniciando…")
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (collectingJob?.isActive != true) {
            collectingJob = scope.launch {
                combine(
                    getCpuUsage(),
                    getMemory(),
                    getThreadCount()
                ) { cpu, mem, threads ->
                    MonitorUiState(cpuPercent = cpu, memory = mem, threadCount = threads)
                }.collect { state ->
                    serviceState.update(state)
                    notificationManager.notify(
                        NotificationHelper.NOTIFICATION_ID_FOREGROUND,
                        notificationHelper.buildForegroundNotification(
                            title = "Vitals",
                            text = "CPU %.1f%%  ·  RAM %.1f%%".format(
                                state.cpuPercent,
                                state.memory.usedPercent
                            )
                        )
                    )
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
*/
