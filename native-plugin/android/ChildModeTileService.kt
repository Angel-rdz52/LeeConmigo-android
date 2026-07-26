package com.leeconmigo.app

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/**
 * Tile de "Accesos rápidos" (la barra de arriba, junto a WiFi/Bluetooth).
 * Activar el bloqueo total no pide nada. Desactivarlo abre una pantalla
 * propia que exige el PIN de control parental.
 *
 * La app solo controla que el tile exista y qué hace al tocarlo — dónde
 * lo acomoda el usuario dentro de la barra de accesos rápidos es cosa de
 * Android, no nuestra.
 */
class ChildModeTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        actualizarTile()
    }

    override fun onClick() {
        super.onClick()
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val activo = prefs.getBoolean(KEY_CHILD_MODE, false)

        if (!activo) {
            // Activar el bloqueo total no requiere PIN — es la acción de emergencia.
            prefs.edit().putBoolean(KEY_CHILD_MODE, true).apply()
            actualizarTile()
        } else {
            // Para desactivar, se pide el PIN en una pantalla propia.
            @Suppress("DEPRECATION")
            val intent = Intent(this, ChildModePinActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivityAndCollapse(intent)
        }
    }

    private fun actualizarTile() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val activo = prefs.getBoolean(KEY_CHILD_MODE, false)
        qsTile?.apply {
            state = if (activo) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = "Modo niño"
            updateTile()
        }
    }
}
