package com.leeconmigo.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject

/**
 * Servicio en primer plano que revisa cada 1.5s qué app está en uso y,
 * si está en la lista de bloqueadas y no tiene un desbloqueo vigente,
 * lanza LockActivity encima de ella.
 *
 * Requiere que el usuario haya concedido manualmente:
 *  - Acceso a datos de uso (Ajustes > Apps con acceso especial > Acceso a datos de uso)
 *  - Permiso para mostrarse sobre otras apps
 */
class MonitorService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var ultimaAppBloqueadaMostrada: String? = null

    private val loop = object : Runnable {
        override fun run() {
            revisarAppEnPrimerPlano()
            handler.postDelayed(this, 1500)
        }
    }

    override fun onCreate() {
        super.onCreate()
        crearCanalNotificacion()
        startForeground(NOTIF_ID, construirNotificacion())
        handler.post(loop)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(loop)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun prefs() = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun appEnPrimerPlano(): String? {
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val fin = System.currentTimeMillis()
        val inicio = fin - 10_000 // ventana de los últimos 10s
        val eventos = usm.queryEvents(inicio, fin)
        var ultimoPaquete: String? = null
        val evento = UsageEvents.Event()
        while (eventos.hasNextEvent()) {
            eventos.getNextEvent(evento)
            if (evento.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                ultimoPaquete = evento.packageName
            }
        }
        return ultimoPaquete
    }

    private fun revisarAppEnPrimerPlano() {
        val paquete = appEnPrimerPlano() ?: return
        if (paquete == packageName) return // no bloquearse a sí misma

        val listaJson = prefs().getString(KEY_BLOCKLIST, "[]") ?: "[]"
        val lista = JSONArray(listaJson)
        var appBloqueada: JSONObject? = null
        for (i in 0 until lista.length()) {
            val item = lista.getJSONObject(i)
            if (item.getString("packageName") == paquete) {
                appBloqueada = item
                break
            }
        }
        if (appBloqueada == null) {
            ultimaAppBloqueadaMostrada = null
            return
        }

        val unlocksJson = prefs().getString(KEY_UNLOCKS, "{}") ?: "{}"
        val unlocks = JSONObject(unlocksJson)
        val hasta = if (unlocks.has(paquete)) unlocks.getLong(paquete) else 0L
        if (hasta > System.currentTimeMillis()) {
            return // sigue desbloqueada, no hacer nada
        }

        // Evita relanzar la pantalla de bloqueo en cada ciclo si ya se está mostrando.
        if (ultimaAppBloqueadaMostrada == paquete) return
        ultimaAppBloqueadaMostrada = paquete

        val intent = Intent(this, LockActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        intent.putExtra("appLabel", appBloqueada.optString("label", "esta app"))
        startActivity(intent)
    }

    private fun crearCanalNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CANAL_ID, "LeeConmigo Control Parental", NotificationManager.IMPORTANCE_MIN
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(canal)
        }
    }

    private fun construirNotificacion(): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            Notification.Builder(this, CANAL_ID) else Notification.Builder(this)

        return builder
            .setContentTitle("LeeConmigo")
            .setContentText("Protegiendo el tiempo de pantalla")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val NOTIF_ID = 4821
        const val CANAL_ID = "leeconmigo_monitor"
    }
}
