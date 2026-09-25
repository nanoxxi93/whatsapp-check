# Tech Stack

> **Estado:** Borrador v0.2 · 2026-09-23
> Las decisiones marcadas con 🔶 están **pendientes de validar** en la Fase 0 (spike técnico) del [roadmap](roadmap.md).
> **Cambios v0.2:** la verificación la hace el WhatsApp del propio usuario vinculado como dispositivo, dentro del teléfono. Se elimina el backend del MVP.

## 1. Restricciones de plataforma que definen la arquitectura

### Android — única plataforma ✅
Android permite todo lo que el producto necesita mediante APIs oficiales:

| Necesidad | API de Android |
|---|---|
| Enterarse de una llamada entrante y obtener el número | `CallScreeningService` + rol `RoleManager.ROLE_CALL_SCREENING` (Android 10+) |
| Mostrar un aviso flotante sobre la pantalla de llamada | `WindowManager` con `TYPE_APPLICATION_OVERLAY` + permiso `SYSTEM_ALERT_WINDOW` |
| Alternativa sin overlay | Notificación heads-up (`NotificationCompat`, canal de prioridad alta) |
| Saber si el número está en contactos | Lo resuelve el sistema: `CallScreeningService` solo se invoca para números **no guardados** (a menos que la app sea el dialer por defecto) |
| Mantener vivo el proceso mientras se verifica | Foreground service de corta duración (`shortService` o `specialUse`) 🔶 |

### iOS — descartado para este enfoque ❌
- iOS no permite ejecutar un cliente de WhatsApp en segundo plano ni hacer una consulta de red libre cuando entra una llamada.
- *Live Caller ID Lookup* (iOS 18+) exige un servidor PIR aprobado por Apple, lo que contradice el diseño sin servidor y la verificación con la cuenta del usuario.

## 2. Cómo se verifica si un número tiene WhatsApp

No existe una API oficial de Meta para consultar si un número arbitrario tiene WhatsApp. Opciones evaluadas:

| Opción | Estado | Motivo |
|---|---|---|
| A. WhatsApp Business Cloud API | ❌ Descartada | No expone un endpoint para verificar números arbitrarios |
| B. Proveedor comercial | ❌ Descartada | Costo por consulta; en la práctica usa el mismo protocolo no oficial |
| C. Backend propio con pool de números (Baileys) | ❌ Descartada | Todo el volumen cae en pocas cuentas → baneos; hay que comprar y rotar SIMs |
| D. Contacto temporal + sincronización de WhatsApp | ❌ Descartada | La sincronización tarda minutos u horas; no sirve en tiempo real |
| E. `wa.me` | ❌ Descartada | Solo distingue cuentas Business; una cuenta personal y un número inexistente devuelven la misma página (probado 2026-09-23) |
| **F. WhatsApp del propio usuario vinculado como dispositivo, dentro del teléfono** | ✅ **Elegida** | Cada cuenta consulta solo las llamadas que recibe; no hay servidor ni costos |
| G. WhatsApp del usuario vinculado, pero la sesión corre en un backend | ❌ Descartada | El servidor tendría las llaves de todas las cuentas (acceso a mensajes) y todas las sesiones saldrían de la misma IP de datacenter, lo que facilita un baneo masivo |

### Motor de protocolo en el dispositivo 🔶

| Opción | Pros | Contras |
|---|---|---|
| ~~F1. Baileys (TypeScript) sobre `nodejs-mobile`~~ | — | **Descartada:** runtime de 50–62 MB, sin alineación de 16 KB en la versión publicada, Node 18 con flag, sin nombre propio visible ([IF-013](intentos-fallidos.md#if-013--motor-baileys--nodejs-mobile-en-android)) |
| ~~F2. Librería JVM nativa (Cobalt)~~ | — | **Descartada:** exige Java 25 y descarga binarios nativos ([IF-011](intentos-fallidos.md#if-011--motor-jvm-cobalt-en-android)) |
| ✅ **F4. whatsmeow (Go) + `gomobile` — ELEGIDA** | Madura y muy activa (base del puente de WhatsApp de Matrix); `PairPhone` con nombre visible e `IsOnWhatsApp`; binario nativo sin runtime de JS | Puente Go ↔ Kotlin; licencia MPL-2.0 (revisar en la revisión legal); sesión en SQLite |
| F3. Implementación propia mínima en Kotlin | Control total, solo lo necesario | Mucho trabajo criptográfico (Noise, emparejamiento); último recurso |

**Decisión (2026-09-23):** **whatsmeow (F4)**. En Android (Moto G53 con wifi y Poco M5s con 4G) vinculó por código y tuvo, en frío, un total p95 de **2,15 s con datos móviles** y 0/20 sin verificar; agrega 17,2 MB al APK (6,4 MB comprimido), alineado a 16 KB ([comparación](../2026-09-23-comparar-motores-android/checklist.md)). El motor queda detrás de `WhatsAppPresenceEngine`.

### Configuración obligatoria de la sesión

Las reglas innegociables de minimización de datos, privacidad y comportamiento pasivo están descritas en [constitution.md](constitution.md). Aquí solo se detalla el motor elegido (whatsmeow).

### Contrato de la consulta (`onWhatsApp` de Baileys)

Comportamiento confirmado en un proyecto existente del autor que ya usa Baileys (2026-09-23):

```ts
await sock.onWhatsApp("51987654321")
// Número con WhatsApp
[{ jid: "51987654321@s.whatsapp.net", exists: true }]
// Número sin WhatsApp
[]
```

Traducción al dominio (`CheckResult`):

| Respuesta del motor | `CheckResult` |
|---|---|
| Arreglo con una entrada `exists: true` | `HAS_WHATSAPP` |
| Arreglo vacío `[]` **o** entrada con `exists: false` | `NO_WHATSAPP` |
| `undefined` (Baileys 7, la consulta USync no trajo resultado), excepción, timeout, sesión desconectada o desvinculada | `UNKNOWN` — **nunca** `NO_WHATSAPP` |

Reglas:
- Se envía el número en E.164 **sin** `+` (salida de libphonenumber sin el signo).
- Se valida la respuesta por el `jid` devuelto, no por la posición en el arreglo. En algunos países WhatsApp normaliza el número (p. ej. el `9` adicional de Brasil o el `1` de México), así que el `jid` puede no coincidir exactamente con lo enviado. 🔶 Definir la regla de coincidencia por país.
- Un arreglo vacío **solo** cuenta como `NO_WHATSAPP` si la llamada terminó sin error y la sesión estaba conectada. Es la única forma de distinguir "no tiene" de "no se pudo verificar".
- Se ignoran los campos adicionales que devuelvan versiones más nuevas del motor (p. ej. `lid`).
- Implementaciones de referencia con los mismos casos de prueba: `prototypes/baileys-cli/src/result.ts` (TS) y `prototypes/android-engines/engine-api/…/CheckResult.kt` (Kotlin). Con whatsmeow, `IsOnWhatsApp` devuelve `{ JID, IsIn }` y se traduce a `{ jid, exists }` en Go.

```kotlin
interface WhatsAppPresenceEngine {
    val sessionState: StateFlow<SessionState>          // UNLINKED, CONNECTING, READY, LOGGED_OUT, BANNED
    suspend fun requestPairingCode(phoneE164: String): String
    suspend fun check(phoneE164: String): CheckResult  // HAS_WHATSAPP | NO_WHATSAPP | UNKNOWN
    suspend fun unlink()
}
```

## 3. Arquitectura general

```
┌──────────────────────── Dispositivo Android ────────────────────────┐
│                                                                      │
│  Llamada entrante                                                    │
│        │                                                             │
│        ▼                                                             │
│  CallScreeningService                                                │
│        ├─► respondToCall(allow)  ← inmediato, nunca retrasa la llamada│
│        └─► VerificationService (foreground, corta duración)          │
│                 │                                                    │
│                 ├─► Overlay: "⏳ Verificando…"                        │
│                 ▼                                                    │
│            CheckNumberUseCase                                        │
│              │          │                                            │
│     Caché Room ◄┘          └─► WhatsAppPresenceEngine ──(WebSocket)───┼──► servidores de WhatsApp
│                                  (sesión vinculada del usuario)      │
│                 ▼                                                    │
│        Overlay / notificación: ✅ ⚠️ ❔                               │
└──────────────────────────────────────────────────────────────────────┘
```

**Decisión clave: responder a la llamada primero y verificar después.** `respondToCall(allow)` se envía de inmediato, así el límite de ~5 s de Android deja de aplicar. El aviso aparece en "Verificando…" y se actualiza cuando llega el resultado. El teléfono sigue sonando entre 20 y 30 s, así que hay margen. Excepción: si el usuario activó "silenciar/rechazar sin WhatsApp" (Fase 3), se espera el resultado hasta 3 s antes de responder.

### Estrategia de conexión 🔶

| Estrategia | Pros | Contras |
|---|---|---|
| **Bajo demanda** (conectar cuando entra la llamada) | Sin consumo de batería en reposo; sin notificación permanente | Conexión + login + consulta deben caber en < 3 s |
| Conexión persistente (foreground service) | Consulta instantánea | Batería, notificación permanente, los fabricantes matan el proceso |

⚠️ Conectar y desconectar en cada llamada es un patrón poco común para un dispositivo vinculado. WhatsApp Web y Desktop suelen quedarse conectados mientras están abiertos. En la prueba de baneos de la Fase 0 hay que comparar ambas estrategias, no solo la latencia.

**Provisional:** bajo demanda + un `WorkManager` periódico (p. ej. cada 24 h) que reconecta brevemente para mantener viva la sesión y detectar si fue desvinculada. En la Fase 0 se mide la latencia real de "conexión en frío + consulta".

### Sin backend

Regla estricta definida en [constitution.md](constitution.md). Solo se usa Firebase Remote Config para un kill switch.

## 4. App Android

| Área | Elección | Justificación |
|---|---|---|
| Lenguaje | **Kotlin 2.x** | Estándar de Android |
| UI | **Jetpack Compose** + Material 3 | UI de ajustes/historial; el overlay usa `ComposeView` dentro de `WindowManager` |
| Min SDK / Target SDK | **minSdk 29** (Android 10) / **targetSdk 35+** | Android 10 introduce `ROLE_CALL_SCREENING` |
| Arquitectura | Clean Architecture ligera + MVVM, módulos por feature | Casos de uso testeables sin framework |
| Inyección de dependencias | **Hilt** | Integración nativa con Services y ViewModels |
| Concurrencia | **Kotlin Coroutines + Flow** | `withTimeout` en conexión y consulta |
| Motor WhatsApp | **whatsmeow (Go) vía `gomobile bind`** → `.aar` (solo `arm64-v8a`), sesión en SQLite (`modernc.org/sqlite`, sin cgo) | Ver sección 2; referencia: `prototypes/android-engines/engine-whatsmeow/` |
| Persistencia | **Room** (caché de resultados + historial) | Consultas locales < 10 ms |
| Credenciales de sesión | Archivo cifrado con **Android Keystore** (AES-GCM), `allowBackup=false` para ese archivo | Nunca salen del dispositivo |
| Preferencias | **DataStore** (Preferences) | Modo de aviso, auto-silenciar, TTL de caché |
| Normalización de números | **libphonenumber** (Google) | Convertir a E.164 antes de consultar/cachear |
| Tareas periódicas | **WorkManager** | Mantener viva la sesión |
| Configuración remota | **Firebase Remote Config** | Kill switch y versión mínima |
| Build | Gradle Kotlin DSL + Version Catalog | |

### Módulos
```
app/                      # Application, navegación, DI raíz
core/model                # Entidades de dominio (PhoneNumber, CheckResult, SessionState)
core/data                 # Repositorio, Room, almacenamiento cifrado
core/domain               # CheckNumberUseCase, reglas de caché
engine/api                # Interfaz WhatsAppPresenceEngine
engine/whatsmeow          # Implementación con whatsmeow (Go vía gomobile)
feature/linking           # Consentimiento de riesgo + código de emparejamiento
feature/screening         # CallScreeningService + VerificationService
feature/overlay           # Aviso flotante y notificación
feature/onboarding        # Rol, permisos y guía por fabricante
feature/history           # Historial y listas blanca/negra
feature/settings          # Ajustes, desvincular, borrar datos
```

### Permisos
| Permiso / rol | Uso | Obligatorio |
|---|---|---|
| `ROLE_CALL_SCREENING` | Recibir el número de llamadas entrantes | Sí |
| `SYSTEM_ALERT_WINDOW` | Aviso flotante; además exime del límite para iniciar foreground services desde background | No (si falta → notificación) |
| `POST_NOTIFICATIONS` (Android 13+) | Notificación heads-up | Sí si no hay overlay |
| `FOREGROUND_SERVICE` (+ tipo) | Mantener vivo el proceso durante la verificación | Sí |
| `INTERNET` | Conexión con WhatsApp | Sí |
| `READ_CONTACTS` / `READ_CALL_LOG` | **No se solicitan** | — |

## 5. Distribución 🔶

La [política de abuso de dispositivos y redes](https://support.google.com/googleplay/android-developer/answer/9888379) de Google Play prohíbe que una app use un servicio o API de terceros de forma que viole sus términos. Una app que actúa como cliente no oficial de WhatsApp **tiene alto riesgo de ser rechazada o retirada de Play Store**.

| Canal | Pros | Contras |
|---|---|---|
| Google Play | Alcance, actualizaciones automáticas | Riesgo alto de rechazo o retiro |
| APK propio (web + actualizador integrado) | Control total | El usuario debe permitir "orígenes desconocidos"; menor confianza |
| F-Droid / Obtainium (si el código es abierto) | Confianza, actualizaciones | Menor alcance; F-Droid marca la *anti-feature* de servicio no libre |

Actualizar rápido es crítico: cuando WhatsApp cambia el protocolo, **todos** los usuarios pierden la verificación hasta que instalan la versión nueva.

## 6. Calidad y entrega

| Área | Herramienta |
|---|---|
| Tests unitarios | JUnit 5, MockK, Turbine (Flows), kotlinx-coroutines-test |
| Tests de UI | Compose UI Test |
| Tests del servicio de llamadas | Pruebas instrumentadas + llamadas reales (el emulador simula llamadas con `adb emu gsm call`) |
| Motor | `FakePresenceEngine` para tests; pruebas manuales **solo con números secundarios** de prueba, nunca con cuentas personales |
| Prototipos de la Fase 0 | Viven en `prototypes/` (p. ej. `prototypes/baileys-cli/`, Node + TypeScript). Son herramientas de validación: no forman parte de la app ni se distribuyen |
| Lint / formato | ktlint, detekt, Android Lint |
| CI/CD | GitHub Actions → Firebase App Distribution (beta) → canal elegido en la sección 5 |
| Crash reporting | Firebase Crashlytics (sin PII: sin números ni datos de sesión) |
| Analítica | Eventos mínimos y anónimos (resultado, latencia, sesión perdida, feedback de spam) |

## 7. Riesgos

| Riesgo | Impacto | Mitigación |
|---|---|---|
| **Baneo o restricción de la cuenta personal del usuario** | **Crítico** para el usuario | Consentimiento explícito; volumen mínimo (solo desconocidos + caché); no alterar su comportamiento en WhatsApp; beta primero con números secundarios |
| **Riesgo correlacionado:** la carga está repartida, pero todos los usuarios usan el mismo cliente. Si Meta detecta su "huella" (versión, forma de conectarse, patrón de consultas), puede banear a todos a la vez | **Crítico** | Kill switch remoto; seguir de cerca los cambios del motor; despliegue gradual por porcentaje; monitorear sesiones perdidas |
| La app tiene llaves con acceso técnico a los mensajes del usuario | Alto (confianza y seguridad) | Llaves solo en el dispositivo, cifradas con Keystore; mensajes descartados sin procesar; considerar publicar el código |
| Rechazo o retiro de Google Play | Alto | Plan de distribución alternativo (sección 5) |
| Cambios de protocolo de WhatsApp | Alto | Motor intercambiable; versión mínima remota; canal de actualización rápido |
| Conexión en frío demasiado lenta (> 3 s) | Medio | Aviso "Verificando…" que se actualiza después; caché; conexión persistente como alternativa |
| La sesión vinculada cambia el comportamiento del WhatsApp principal (notificaciones, "en línea") | Medio | Configuración obligatoria de la sección 2; validarlo en la Fase 0 |
| Optimizaciones de batería de fabricantes (MIUI, One UI) | Medio | Guía de onboarding por fabricante; pruebas en dispositivos reales |
| Límite de 4 dispositivos vinculados por cuenta | Bajo | Detectarlo al vincular y explicar al usuario |

