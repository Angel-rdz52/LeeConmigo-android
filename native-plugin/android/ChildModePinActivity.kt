package com.leeconmigo.app

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.security.MessageDigest

class ChildModePinActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.gravity = Gravity.CENTER
        root.setBackgroundColor(Color.parseColor("#33404D"))
        root.setPadding(72, 72, 72, 72)

        val titulo = TextView(this)
        titulo.text = "🔒 Ingresá el PIN de Control Parental para desactivar el Modo niño"
        titulo.setTextColor(Color.WHITE)
        titulo.textSize = 18f
        titulo.gravity = Gravity.CENTER
        titulo.setPadding(0, 0, 0, 32)
        root.addView(titulo)

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        input.setTextColor(Color.BLACK)
        input.setBackgroundColor(Color.WHITE)
        input.setPadding(24, 24, 24, 24)
        root.addView(input)

        val espacio = TextView(this)
        espacio.text = ""
        espacio.setPadding(0, 24, 0, 0)
        root.addView(espacio)

        val botonConfirmar = Button(this)
        botonConfirmar.text = "Desactivar"
        botonConfirmar.setOnClickListener {
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val hashGuardado = prefs.getString(KEY_ADMIN_PIN_HASH, null)
            val ingresado = input.text.toString().trim()

            if (hashGuardado != null && ingresado.isNotEmpty() && sha256(ingresado) == hashGuardado) {
                prefs.edit().putBoolean(KEY_CHILD_MODE, false).apply()
                finish()
            } else {
                titulo.text = "❌ PIN incorrecto. Intentá de nuevo."
                input.text.clear()
            }
        }
        root.addView(botonConfirmar)

        val botonCancelar = Button(this)
        botonCancelar.text = "Cancelar"
        botonCancelar.setOnClickListener { finish() }
        root.addView(botonCancelar)

        setContentView(root)
    }

    private fun sha256(texto: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(texto.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
