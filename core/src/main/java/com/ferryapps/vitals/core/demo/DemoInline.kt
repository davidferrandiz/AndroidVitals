package com.ferryapps.vitals.core.demo

/**
 * Archivo de prueba para el AI PR review bot (comentarios inline).
 * Contiene bugs a propósito. NO mergear este PR.
 */
object DemoInline {

    // BUG [alta]: !! provoca NullPointerException si `items` es null
    fun first(items: List<String>?): String = items!!.first()

    // BUG [alta]: object con estado mutable global -> viola R-DI-4 (singleton ad-hoc)
    var lastAccessMillis: Long = 0L
}
