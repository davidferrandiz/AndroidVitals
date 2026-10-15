package com.ferryapps.vitals.core.data.sysfs

import java.io.File
import java.io.IOException

/**
 * Lectura de ficheros de /proc y /sys. Muchos están bloqueados por SELinux según el
 * fabricante: un fallo de lectura es un caso normal, no un error, y devuelve null.
 */
internal object SysFs {

    fun read(path: String): String? = read(File(path))

    fun read(file: File): String? =
        try {
            file.readText()
        } catch (e: IOException) {
            null
        } catch (e: SecurityException) {
            null
        }

    fun exists(path: String): Boolean =
        try {
            File(path).exists()
        } catch (e: SecurityException) {
            false
        }
}
