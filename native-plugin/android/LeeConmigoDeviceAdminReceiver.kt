package com.leeconmigo.app

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

/**
 * Mientras esta app sea administrador de dispositivo activo, Android
 * bloquea el botón "Desinstalar" en Ajustes — hay que desactivar primero
 * el administrador (un paso extra, con esta advertencia personalizada).
 * No es tan fuerte como "Device Owner" (que sí permite exigir un PIN
 * propio antes de desactivar), pero no requiere aprovisionar el celular
 * de forma especial.
 */
class LeeConmigoDeviceAdminReceiver : DeviceAdminReceiver() {
    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return "Esto es parte del Control Parental de LeeConmigo. Si lo desactivás, cualquiera va a poder desinstalar la app sin restricciones."
    }
}
