package com.ferryapps.vitals.core.domain.usecase

import com.ferryapps.vitals.core.domain.model.TopProcess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import javax.inject.Inject

class GetTopProcessesUseCase @Inject constructor() {

    operator fun invoke(limit: Int = 8): Flow<List<TopProcess>> = flow {
        while (true) {
            emit(readTopProcesses(limit))
            delay(3_000)
        }
    }.flowOn(Dispatchers.IO)

    private fun readTopProcesses(limit: Int): List<TopProcess> {
        val procDir = File("/proc")
        if (!procDir.exists()) return emptyList()

        return procDir.listFiles()
            ?.filter { it.isDirectory && it.name.all { c -> c.isDigit() } }
            ?.mapNotNull { dir ->
                val pid  = dir.name.toIntOrNull() ?: return@mapNotNull null
                val name = readProcessName(dir) ?: return@mapNotNull null
                val ram  = readProcessRam(dir)
                TopProcess(pid = pid, name = name, ramKb = ram)
            }
            ?.filter { it.ramKb > 0 }
            ?.sortedByDescending { it.ramKb }
            ?.take(limit)
            ?: emptyList()
    }

    private fun readProcessName(procDir: File): String? {
        // /proc/<pid>/cmdline contiene el nombre del proceso separado por null bytes
        val cmdline = runCatching {
            File(procDir, "cmdline").readBytes()
                .takeWhile { it != 0.toByte() }
                .toByteArray()
                .toString(Charsets.UTF_8)
                .trim()
        }.getOrNull()

        if (!cmdline.isNullOrBlank()) {
            // Nos quedamos solo con el nombre del paquete o binario (sin path completo)
            return cmdline.substringAfterLast('/').take(30)
        }

        // Fallback: /proc/<pid>/comm (nombre del thread principal, máx 15 chars)
        return runCatching {
            File(procDir, "comm").readText().trim()
        }.getOrNull()
    }

    private fun readProcessRam(procDir: File): Long {
        // /proc/<pid>/status tiene VmRSS (RAM física usada)
        return runCatching {
            File(procDir, "status").useLines { lines ->
                lines.firstOrNull { it.startsWith("VmRSS:") }
                    ?.trim()
                    ?.split("\\s+".toRegex())
                    ?.getOrNull(1)
                    ?.toLongOrNull()
                    ?: 0L
            }
        }.getOrDefault(0L)
    }
}
