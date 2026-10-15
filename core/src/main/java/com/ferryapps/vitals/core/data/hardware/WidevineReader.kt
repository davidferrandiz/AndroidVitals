package com.ferryapps.vitals.core.data.hardware

import android.media.MediaDrm
import android.media.UnsupportedSchemeException
import android.os.Build
import java.util.UUID
import javax.inject.Inject

/** Nivel de seguridad de Widevine: L1 permite streaming en HD en la mayoría de plataformas. */
class WidevineReader @Inject constructor() {

    fun securityLevel(): String? {
        if (!MediaDrm.isCryptoSchemeSupported(WIDEVINE_UUID)) return null
        val drm = try {
            MediaDrm(WIDEVINE_UUID)
        } catch (e: UnsupportedSchemeException) {
            return null
        }
        return try {
            drm.getPropertyString("securityLevel").trim().takeIf { it.isNotEmpty() }
        } catch (e: IllegalStateException) {
            null
        } finally {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                drm.close()
            } else {
                @Suppress("DEPRECATION")
                drm.release()
            }
        }
    }

    private companion object {
        val WIDEVINE_UUID: UUID = UUID.fromString("edef8ba9-79d6-4ace-a3c8-27dcd51d21ed")
    }
}
