package com.whatscheck.engines.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.json.JSONObject

/** Tarjeta con título, para agrupar cada bloque de las pantallas. */
@Composable
fun SectionCard(title: String? = null, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (title != null) Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

/** Presentación de un resultado: ✅ Tiene WhatsApp / ⚠️ No tiene WhatsApp / ❔ No se pudo verificar. */
enum class ResultLook(val emoji: String, val label: String) {
    HAS("✅", "Tiene WhatsApp"),
    NO("⚠️", "No tiene WhatsApp"),
    UNKNOWN("❔", "No se pudo verificar");

    @Composable
    fun color(): Color {
        val dark = isSystemInDarkTheme()
        return when (this) {
            HAS -> if (dark) Brand.HasDark else Brand.Has
            NO -> if (dark) Brand.NoDark else Brand.No
            UNKNOWN -> Brand.Unknown
        }
    }

    companion object {
        fun of(status: String?) = when (status) {
            "HAS_WHATSAPP" -> HAS
            "NO_WHATSAPP" -> NO
            else -> UNKNOWN
        }
    }
}

/** Etiqueta redondeada de color, p. ej. para el resultado o el estado de la sesión. */
@Composable
fun Pill(text: String, color: Color) {
    Surface(shape = RoundedCornerShape(50), color = color.copy(alpha = 0.14f)) {
        Text(
            text,
            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** Una llamada del registro: resultado, número enmascarado, cuándo y cuánto tardó. */
@Composable
fun CallRow(call: JSONObject) {
    val look = ResultLook.of(call.optString("status"))
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(look.color().copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) { Text(look.emoji) }
        Column(Modifier.weight(1f)) {
            Text(
                if (call.isNull("phone")) "Número oculto" else call.optString("phone"),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            val details = buildList {
                add(Fmt.whenIso(call.optString("ts")))
                if (!call.isNull("tResult")) add(Fmt.seconds(call.getLong("tResult")))
                if (call.optBoolean("locked")) add("bloqueado")
                if (call.optString("via") == "notificacion") add("por notificación")
            }
            Text(details.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (look == ResultLook.UNKNOWN && !call.isNull("detail")) {
                Text(call.optString("detail"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(look.label, style = MaterialTheme.typography.labelMedium, color = look.color())
    }
}
