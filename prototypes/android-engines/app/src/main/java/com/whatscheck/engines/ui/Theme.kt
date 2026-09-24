package com.whatscheck.engines.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Paleta propia: índigo de marca + verde "verificado" + ámbar "sin WhatsApp". No usa el verde de WhatsApp. */
object Brand {
    val Indigo = Color(0xFF283593)
    val Has = Color(0xFF15803D)
    val HasDark = Color(0xFF4ADE80)
    val No = Color(0xFFB45309)
    val NoDark = Color(0xFFFBBF24)
    val Unknown = Color(0xFF64748B)

    /** Verde de "verificado" legible en el tema actual. */
    @Composable
    fun ok(): Color = if (isSystemInDarkTheme()) HasDark else Has

    /** Ámbar de aviso legible en el tema actual. */
    @Composable
    fun warn(): Color = if (isSystemInDarkTheme()) NoDark else No
}

private val Light = lightColorScheme(
    primary = Brand.Indigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE1FF),
    onPrimaryContainer = Color(0xFF0B1452),
    secondary = Brand.Has,
    secondaryContainer = Color(0xFFD7F5DF),
    onSecondaryContainer = Color(0xFF052E16),
    tertiary = Brand.No,
    tertiaryContainer = Color(0xFFFFEDD5),
    onTertiaryContainer = Color(0xFF451A03),
    background = Color(0xFFF7F7FC),
    surface = Color(0xFFF7F7FC),
    surfaceContainer = Color(0xFFEDEEF6),
    surfaceContainerHigh = Color(0xFFE6E7F1),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFB9C3FF),
    onPrimary = Color(0xFF14207A),
    primaryContainer = Color(0xFF2E3A92),
    onPrimaryContainer = Color(0xFFDDE1FF),
    secondary = Brand.HasDark,
    secondaryContainer = Color(0xFF14532D),
    onSecondaryContainer = Color(0xFFD7F5DF),
    tertiary = Brand.NoDark,
    tertiaryContainer = Color(0xFF713F12),
    onTertiaryContainer = Color(0xFFFFEDD5),
    background = Color(0xFF111318),
    surface = Color(0xFF111318),
    surfaceContainer = Color(0xFF1C1F27),
    surfaceContainerHigh = Color(0xFF262A33),
)

@Composable
fun WhatsCheckTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}
