package com.leeconmigo.app

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Process
import android.provider.Settings
import com.getcapacitor.JSArray
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import org.json.JSONArray
import org.json.JSONObject

// Nombre de las SharedPreferences que comparten el plugin y el MonitorService.
const val PREFS_NAME = "leeconmigo_blocker_prefs"
const val KEY_BLOCKLIST = "apps_json"
const val KEY_UNLOCKS = "unlocks_json"

@CapacitorPlugin(name = "AppBlocker")
class AppBlockerPlugin : Plugin() {

    private fun prefs() = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // -------- Listado de apps instaladas (para el panel de administrador) --------
    @PluginMethod
    fun listInstalledApps(call: PluginCall) {
        val pm = context.packageManager
        val propioPaquete = context.packageName
        val resultado = JSArray()

        val apps: List<ApplicationInfo> = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        for (app in apps) {
            if (app.packageName == propioPaquete) continue
            // Solo apps con ícono lanzable (evita servicios internos del sistema).
            val launchIntent = pm.getLaunchIntentForPackage(app.packageName) ?: continue
            val label = pm.getApplicationLabel(app).toString()
            val item = JSObject()
            item.put("packageName", app.packageName)
            item.put("label", label)
            resultado.put(item)
        }

        val ret = JSObject()
        ret.put("apps", resultado)
        call.resolve(ret)
    }

    // -------- Lista de apps bloqueadas configuradas por el administrador --------
    @PluginMethod
    fun getBlockedApps(call: PluginCall) {
        val json = prefs().getString(KEY_BLOCKLIST, "[]")
        val ret = JSObject()
        ret.put("apps", JSArray(json))
        call.resolve(ret)
    }

    @PluginMethod
    fun setBlockedApps(call: PluginCall) {
        val apps = call.getArray("apps") ?: JSArray()
        prefs().edit().putString(KEY_BLOCKLIST, apps.toString()).apply()
        call.resolve()
    }

    // -------- Desbloqueo temporal canjeado con estrellas --------
    @PluginMethod
    fun unlockApp(call: PluginCall) {
        val packageName = call.getString("packageName") ?: return call.reject("Falta packageName")
        val minutes = call.getInt("minutes") ?: 30

        val unlocksJson = prefs().getString(KEY_UNLOCKS, "{}")
        val unlocks = JSONObject(unlocksJson ?: "{}")
        val hasta = System.currentTimeMillis() + minutes * 60_000L
        unlocks.put(packageName, hasta)
        prefs().edit().putString(KEY_UNLOCKS, unlocks.toString()).apply()

        call.resolve()
    }

    // -------- Permisos especiales de Android --------
    @PluginMethod
    fun hasUsageAccessPermission(call: PluginCall) {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val modo = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        val ret = JSObject()
        ret.put("granted", modo == AppOpsManager.MODE_ALLOWED)
        call.resolve(ret)
    }

    @PluginMethod
    fun requestUsageAccessPermission(call: PluginCall) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
        call.resolve()
    }

    @PluginMethod
    fun hasOverlayPermission(call: PluginCall) {
        val ret = JSObject()
        ret.put("granted", Settings.canDrawOverlays(context))
        call.resolve(ret)
    }

    @PluginMethod
    fun requestOverlayPermission(call: PluginCall) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:" + context.packageName)
        )
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
        call.resolve()
    }

    // -------- Arranca el servicio que vigila qué app está en primer plano --------
    @PluginMethod
    fun startMonitorService(call: PluginCall) {
        val intent = Intent(context, MonitorService::class.java)
        context.startForegroundService(intent)
        call.resolve()
    }
}
