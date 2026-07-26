package com.leeconmigo.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Pantalla que tapa por completo la app bloqueada. Se construye la UI por
 * código (sin XML) para simplificar la integración en el proyecto host.
 */
class LockActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        val appLabel = intent.getStringExtra("appLabel") ?: "esta app"

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.gravity = Gravity.CENTER
        root.setBackgroundColor(Color.parseColor("#33404D"))
        root.setPadding(64, 64, 64, 64)

        val emoji = TextView(this)
        emoji.text = "🔒"
        emoji.textSize = 64f
        emoji.gravity = Gravity.CENTER
        root.addView(emoji)

        val titulo = TextView(this)
        titulo.text = "$appLabel está bloqueada"
        titulo.textSize = 22f
        titulo.setTextColor(Color.WHITE)
        titulo.gravity = Gravity.CENTER
        titulo.setPadding(0, 32, 0, 16)
        root.addView(titulo)

        val subtitulo = TextView(this)
        subtitulo.text = "Pide a un adulto que use estrellas en LeeConmigo para desbloquearla."
        subtitulo.textSize = 16f
        subtitulo.setTextColor(Color.parseColor("#D3ECFB"))
        subtitulo.gravity = Gravity.CENTER
        subtitulo.setPadding(0, 0, 0, 32)
        root.addView(subtitulo)

        val boton = Button(this)
        boton.text = "Abrir LeeConmigo"
        boton.setOnClickListener {
            val abrir = packageManager.getLaunchIntentForPackage(packageName)
            abrir?.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (abrir != null) startActivity(abrir)
            finish()
        }
        root.addView(boton)

        setContentView(root)
    }

    // Evita salir de la pantalla de bloqueo con el botón "atrás": lo manda al inicio en vez de a la app bloqueada.
    override fun onBackPressed() {
        val abrir = packageManager.getLaunchIntentForPackage(packageName)
        abrir?.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        if (abrir != null) startActivity(abrir)
        finish()
    }
}
