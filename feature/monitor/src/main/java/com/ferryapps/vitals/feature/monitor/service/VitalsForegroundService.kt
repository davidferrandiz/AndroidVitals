package com.ferryapps.vitals.feature.monitor.service

// Foreground Service desactivado para distribución en stores (Play Console rechaza
// apps con foregroundServiceType="dataSync" sin un caso de uso aprobado).
// La recogida de CPU/RAM/threads se hace ahora directamente desde MonitorViewModel.
// Mantener todo este fichero comentado hasta que se justifique formalmente.

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
