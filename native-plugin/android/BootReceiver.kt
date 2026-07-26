package com.leeconmigo.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Si el teléfono se reinicia (apagado/encendido), el servicio de vigilancia
 * no arranca solo — sin esto, después de un reinicio todas las apps
 * quedarían desbloqueadas hasta que alguien abriera LeeConmigo a mano.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            context.startForegroundService(Intent(context, MonitorService::class.java))
        }
    }
}
