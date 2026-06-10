package com.ferryapps.vitals.core.domain.model

data class StorageInfo(
    val totalBytes: Long,
    val usedBytes: Long,
    val freeBytes: Long
) {
    val usedPercent: Float get() =
        if (totalBytes > 0) usedBytes.toFloat() / totalBytes * 100f else 0f

    fun totalGb() = totalBytes / 1_073_741_824.0
    fun usedGb()  = usedBytes  / 1_073_741_824.0
    fun freeGb()  = freeBytes  / 1_073_741_824.0
}
