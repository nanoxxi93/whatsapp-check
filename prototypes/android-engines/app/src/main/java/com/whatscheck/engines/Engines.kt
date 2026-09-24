package com.whatscheck.engines

import android.content.Context
import com.whatscheck.engine.WhatsAppPresenceEngine
import com.whatscheck.engines.whatsmeow.WhatsmeowEngine
import java.io.File

/** Motores compilados en esta app de pruebas. T5 agrega Baileys. */
object Engines {
    /** Cada motor guarda su sesión en su propia carpeta dentro de filesDir (RNF-02). */
    fun available(context: Context): List<WhatsAppPresenceEngine> = listOf(
        WhatsmeowEngine(File(context.filesDir, "whatsmeow")),
    )
}

object Phone {
    /** "51987654388" → "+5198*****88" (mismo formato que baileys-cli). */
    fun mask(digits: String): String {
        val clean = digits.filter(Char::isDigit)
        if (clean.length <= 6) return "+" + "*".repeat(clean.length)
        return "+" + clean.take(4) + "*".repeat(clean.length - 6) + clean.takeLast(2)
    }

    /** Enmascara cualquier secuencia de 7+ dígitos dentro de un texto (mensajes de error, jids). */
    fun maskInText(text: String): String = Regex("""\+?\d{7,}""").replace(text) { mask(it.value) }
}
