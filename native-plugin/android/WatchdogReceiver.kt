package com.leeconmigo.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Se dispara cada ~15 minutos (programado desde MonitorService.onCreate).
 * Si algún fabricante mató el servicio de vigilancia para "ahorrar
 * batería", esto lo vuelve a levantar. No es 100% infalible contra
 * fabricantes muy agresivos (Xiaomi/Huawei/etc.), pero es una red de
 * seguridad importante — sin exención de batería puede tardar hasta este
 * intervalo en recuperarse.
 */
class WatchdogReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        context.startForegroundService(Intent(context, MonitorService::class.java))
    }
}
