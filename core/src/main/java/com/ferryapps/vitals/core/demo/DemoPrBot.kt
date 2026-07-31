package com.ferryapps.vitals.core.demo

object DemoPrBot {
    // BUG a propósito: !! revienta con NPE si s es null
    fun firstChar(s: String?): Char = s!![0]
}
