package com.ferryapps.vitals.core.domain.widget

/**
 * Refresca el widget de pantalla de inicio. La implementación vive en :feature:widget y
 * VitalsWorker la usa sin depender de ese módulo (las features no se importan entre sí).
 */
interface WidgetUpdater {
    suspend fun updateAll()
}
