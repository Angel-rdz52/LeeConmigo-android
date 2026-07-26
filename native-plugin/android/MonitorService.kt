package com.leeconmigo.app

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import org.json.JSONArray
import org.json.JSONObject

/**
 * Servicio en primer plano que revisa cada 1.5s qué app está en uso y,
 * si está en la lista de bloqueadas y no tiene un desbloqueo vigente,
 * lanza LockActivity encima de ella.
 *
 * También mantiene actualizada la notificación persistente con la cuenta
 * regresiva de cualquier desbloqueo activo, y dispara un aviso con
 * vibración cuando a un desbloqueo le quedan 5 unidades (5 minutos en modo
 * normal, 5 segundos en modo prueba).
 *
 * Requiere que el usuario haya concedido manualmente:
 *  - Acceso a datos de uso (Ajustes > Apps con acceso especial > Acceso a datos de uso)
 *  - Permiso para mostrarse sobre otras apps
 */
class MonitorService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var ultimaAppBloqueadaMostrada: String? = null
    // Recuerda qué "hasta" ya avisamos por cada paquete, para no repetir el aviso
    // de 5 unidades en cada ciclo mientras dure ese mismo desbloqueo.
    private val avisosEnviados = HashMap<String, Long>()

    private val loop = object : Runnable {
        override fun run() {
            revisarAppEnPrimerPlano()
            actualizarNotificacionYAvisos()
            handler.postDelayed(this, 800)
        }
    }

    override fun onCreate() {
        super.onCreate()
        crearCanalesNotificacion()
        startForeground(NOTIF_ID, construirNotificacionBase())
        programarWatchdog()
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

    /** Red de seguridad: si Android mata este servicio, esta alarma lo revive
     *  cada ~15 minutos aunque el teléfono esté en reposo profundo (Doze). */
    private fun programarWatchdog() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, WatchdogReceiver::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, flags)

        alarmManager.setInexactRepeating(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + AlarmManager.INTERVAL_FIFTEEN_MINUTES,
            AlarmManager.INTERVAL_FIFTEEN_MINUTES,
            pendingIntent
        )
    }

    private fun modoPrueba() = prefs().getBoolean(KEY_TEST_MODE, false)

    /** 60_000 ms normalmente ("minutos"); 1_000 ms en modo prueba ("segundos"). */
    private fun unidadMs() = if (modoPrueba()) 1_000L else 60_000L

    private fun unidadLabel() = if (modoPrueba()) "segundos" else "minutos"

    private fun listaBloqueadas(): JSONArray =
        JSONArray(prefs().getString(KEY_BLOCKLIST, "[]") ?: "[]")

    private fun etiquetaDe(paquete: String, lista: JSONArray): String {
        for (i in 0 until lista.length()) {
            val item = lista.getJSONObject(i)
            if (item.getString("packageName") == paquete) {
                return item.optString("label", paquete)
            }
        }
        return paquete
    }

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
        if (paquete == packageName) {
            // Estamos en LeeConmigo (o en la pantalla de bloqueo, que es parte
            // de esta misma app). "Olvidamos" el bloqueo anterior para que, si
            // el niño vuelve a abrir la app bloqueada, se le muestre de nuevo
            // la pantalla de bloqueo en vez de asumir que ya se mostró.
            ultimaAppBloqueadaMostrada = null
            return
        }

        val lista = listaBloqueadas()
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

        val unlocks = JSONObject(prefs().getString(KEY_UNLOCKS, "{}") ?: "{}")
        val hasta = if (unlocks.has(paquete)) unlocks.getLong(paquete) else 0L
        val childModeActivo = prefs().getBoolean(KEY_CHILD_MODE, false)
        if (!childModeActivo && hasta > System.currentTimeMillis()) {
            return // sigue desbloqueada, no hacer nada
        }

        // Evita relanzar la pantalla de bloqueo en cada ciclo si ya se está mostrando.
        if (ultimaAppBloqueadaMostrada == paquete) return
        ultimaAppBloqueadaMostrada = paquete

        val intent = Intent(this, LockActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        intent.putExtra("appLabel", appBloqueada.optString("label", "esta app"))
        intent.putExtra("packageName", paquete)
        startActivity(intent)
    }

    /**
     * Recorre todos los desbloqueos activos: actualiza la notificación
     * persistente con la cuenta regresiva y dispara el aviso de "quedan 5
     * unidades" una sola vez por desbloqueo.
     */
    private fun actualizarNotificacionYAvisos() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (prefs().getBoolean(KEY_CHILD_MODE, false)) {
            manager.notify(NOTIF_ID, construirNotificacionBase("🧒 Modo niño activo — todo bloqueado"))
            return
        }

        val ahora = System.currentTimeMillis()
        val unlocksJson = prefs().getString(KEY_UNLOCKS, "{}") ?: "{}"
        val unlocks = JSONObject(unlocksJson)
        val lista = listaBloqueadas()
        val unidad = unidadMs()

        var masCercano: Pair<String, Long>? = null // (paquete, restanteMs)
        var cantidadActivos = 0
        val keysAEliminar = mutableListOf<String>()

        val claves = unlocks.keys()
        while (claves.hasNext()) {
            val paquete = claves.next()
            val hasta = unlocks.getLong(paquete)
            val restante = hasta - ahora

            if (restante <= 0) {
                keysAEliminar.add(paquete)
                avisosEnviados.remove(paquete)
                continue
            }

            cantidadActivos++
            if (masCercano == null || restante < masCercano!!.second) {
                masCercano = Pair(paquete, restante)
            }

            // Aviso de "quedan 5 unidades", una sola vez por desbloqueo.
            val umbral = 5 * unidad
            if (restante <= umbral && avisosEnviados[paquete] != hasta) {
                avisosEnviados[paquete] = hasta
                enviarAvisoTiempoPorAgotarse(etiquetaDe(paquete, lista))
            }
        }

        // Limpieza de desbloqueos ya vencidos para no acumular basura en prefs.
        if (keysAEliminar.isNotEmpty()) {
            keysAEliminar.forEach { unlocks.remove(it) }
            prefs().edit().putString(KEY_UNLOCKS, unlocks.toString()).apply()
        }

        if (masCercano != null) {
            val (paquete, restanteMs) = masCercano!!
            val etiqueta = etiquetaDe(paquete, lista)
            val extra = if (cantidadActivos > 1) " (+${cantidadActivos - 1} más)" else ""
            val texto = "🔓 ${etiqueta}: quedan ${formatearRestante(restanteMs)}$extra"
            manager.notify(NOTIF_ID, construirNotificacionBase(texto))
        } else {
            manager.notify(NOTIF_ID, construirNotificacionBase(null))
        }
    }

    private fun formatearRestante(ms: Long): String {
        val totalSeg = ms / 1000
        val min = totalSeg / 60
        val seg = totalSeg % 60
        return if (min > 0) String.format("%d:%02d min", min, seg) else "${seg}s"
    }

    private fun enviarAvisoTiempoPorAgotarse(etiquetaApp: String) {
        val texto = "⏳ A \"$etiquetaApp\" le quedan 5 ${unidadLabel()}"

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            Notification.Builder(this, CANAL_AVISOS_ID) else Notification.Builder(this)

        val notif = builder
            .setContentTitle("LeeConmigo")
            .setContentText(texto)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(etiquetaApp.hashCode(), notif)

        // En Android 8+ el canal ya vibra solo (enableVibration); en versiones
        // más viejas no existen canales, así que vibramos nosotros a mano.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            vibrar()
        }
    }

    private fun vibrar() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 250, 150, 250), -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 250, 150, 250), -1)
        }
    }

    private fun crearCanalesNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val canalMonitor = NotificationChannel(
                CANAL_ID, "LeeConmigo Control Parental", NotificationManager.IMPORTANCE_MIN
            )
            manager.createNotificationChannel(canalMonitor)

            val canalAvisos = NotificationChannel(
                CANAL_AVISOS_ID, "LeeConmigo Avisos de tiempo", NotificationManager.IMPORTANCE_HIGH
            )
            canalAvisos.enableVibration(true)
            canalAvisos.vibrationPattern = longArrayOf(0, 250, 150, 250)
            manager.createNotificationChannel(canalAvisos)
        }
    }

    private fun construirNotificacionBase(textoExtra: String? = null): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            Notification.Builder(this, CANAL_ID) else Notification.Builder(this)

        return builder
            .setContentTitle("LeeConmigo")
            .setContentText(textoExtra ?: "Protegiendo el tiempo de pantalla")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val NOTIF_ID = 4821
        const val CANAL_ID = "leeconmigo_monitor"
        const val CANAL_AVISOS_ID = "leeconmigo_avisos"
    }
}
