package com.whatscheck.engines

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.whatscheck.engines.call.KeepAliveService
import com.whatscheck.engines.ui.HomeScreen
import com.whatscheck.engines.ui.LogScreen
import com.whatscheck.engines.ui.SettingsScreen
import com.whatscheck.engines.ui.WhatsCheckTheme

/** App de pruebas con tres vistas: Inicio, Registro y Ajustes (spec 2026-09-24-ui-app-pruebas). */
class MainActivity : ComponentActivity() {
    /** Se incrementa en cada onResume y tras acciones, para releer permisos, sesión y registro. */
    private var tick by mutableIntStateOf(0)

    private val roleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { tick++ }
    private val notifLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { tick++ }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (Prefs.keepAlive(this)) KeepAliveService.start(this, "app")
        val engine = Engines.whatsmeow(applicationContext)

        setContent {
            WhatsCheckTheme {
                // --es tab log|settings abre esa pestaña (sirve para capturas por adb)
                val start = when (intent.getStringExtra("tab")) { "log" -> 1; "settings" -> 2; else -> 0 }
                var tab by rememberSaveable { mutableIntStateOf(start) }
                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            Tab.entries.forEachIndexed { i, t ->
                                NavigationBarItem(
                                    selected = tab == i,
                                    onClick = { tab = i; tick++ },
                                    icon = { Icon(t.icon, contentDescription = null) },
                                    label = { Text(t.label) },
                                    // Índigo de la marca (el verde por defecto recordaba a WhatsApp)
                                    colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primaryContainer),
                                )
                            }
                        }
                    },
                ) { padding ->
                    Box(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
                        when (Tab.entries[tab]) {
                            Tab.HOME -> HomeScreen(engine, tick, onOpenSettings = { tab = Tab.SETTINGS.ordinal }, onOpenLog = { tab = Tab.LOG.ordinal })
                            Tab.LOG -> LogScreen(tick)
                            Tab.SETTINGS -> SettingsScreen(
                                engine, tick,
                                requestRole = ::requestRole,
                                requestNotifications = { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                                onChanged = { tick++ },
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        tick++
    }

    private fun requestRole() {
        roleLauncher.launch(getSystemService(RoleManager::class.java).createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING))
    }

    private enum class Tab(val label: String, val icon: ImageVector) {
        HOME("Inicio", Icons.Filled.Home),
        LOG("Registro", Icons.AutoMirrored.Filled.List),
        SETTINGS("Ajustes", Icons.Filled.Settings),
    }
}

object Prefs {
    const val DEFAULT_OVERLAY_DURATION_SECONDS = 8
    val ALLOWED_OVERLAY_DURATIONS = listOf(5, 8, 12)

    fun sanitizeOverlayDuration(seconds: Int): Int =
        if (seconds in ALLOWED_OVERLAY_DURATIONS) seconds else DEFAULT_OVERLAY_DURATION_SECONDS

    private fun p(c: Context) = c.getSharedPreferences("whatscheck", Context.MODE_PRIVATE)
    fun keepAlive(c: Context) = p(c).getBoolean("keepAlive", true)
    fun setKeepAlive(c: Context, on: Boolean) = p(c).edit().putBoolean("keepAlive", on).apply()

    fun overlayDurationSeconds(c: Context): Int =
        sanitizeOverlayDuration(p(c).getInt("overlayDurationSeconds", DEFAULT_OVERLAY_DURATION_SECONDS))

    fun setOverlayDurationSeconds(c: Context, seconds: Int) {
        val sanitized = sanitizeOverlayDuration(seconds)
        p(c).edit().putInt("overlayDurationSeconds", sanitized).apply()
    }

    /** Último problema de sesión ya notificado (para avisar una sola vez por problema). */
    fun lastSessionAlert(c: Context): String? = p(c).getString("lastSessionAlert", null)
    fun setLastSessionAlert(c: Context, state: String?) = p(c).edit().putString("lastSessionAlert", state).apply()
}
