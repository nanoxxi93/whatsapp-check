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
| **F1. Baileys (TypeScript) sobre `nodejs-mobile`** | La librería más usada y mantenida; comunidad grande; `onWhatsApp()` y `requestPairingCode()` listos | Suma decenas de MB al APK; requiere un puente Kotlin ↔ Node; arranque del runtime |
| **F2. Librería JVM nativa** (p. ej. Cobalt, Java) | Se integra directo con Kotlin; APK más liviano | Comunidad más chica; hay que verificar mantenimiento y soporte de pairing code |
| F3. Implementación propia mínima en Kotlin | Control total, solo lo necesario | Mucho trabajo criptográfico (Noise, emparejamiento); último recurso |

**Decisión provisional:** comparar F1 y F2 en la Fase 0. El motor queda detrás de la interfaz `WhatsAppPresenceEngine`, para poder reemplazarlo sin tocar el resto de la app.

### Configuración obligatoria de la sesión
Sea cual sea el motor, la sesión vinculada debe:

| Requisito | Motivo |
|---|---|
| Vincular por **código de emparejamiento**, no por QR | El usuario no puede escanear un QR en su propia pantalla |
| **No** marcarse "en línea" al conectar (`markOnlineOnConnect: false` en Baileys) | No cambiar lo que ven los contactos ni silenciar las notificaciones del teléfono principal 🔶 |
| **No** sincronizar el historial de chats | Privacidad, menos datos y conexión más rápida |
| **No** enviar confirmaciones de lectura ni de entrega de lectura | No alterar el WhatsApp del usuario |
| Descartar todo mensaje entrante sin guardarlo | Solo se usa la sesión para consultar números |
| El onboarding le dice al usuario **con qué nombre aparecerá** el dispositivo (p. ej. "Google Chrome (Mac OS)") | Con código de emparejamiento WhatsApp no muestra el nombre que envía el cliente ([IF-009](intentos-fallidos.md#if-009--vincular-con-un-sistema-operativo-inventado-en-browser)); el usuario debe poder reconocerlo en *Dispositivos vinculados*. Un nombre propio ("WhatsCheck") se investiga en la Fase 1 |
| Credenciales cifradas con **Android Keystore** y excluidas del backup | Nunca salen del teléfono |

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
- Implementación de referencia con tests: `prototypes/baileys-cli/src/result.ts` y `result.test.ts`.

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
No hay servidor en el MVP. Lo único remoto es **Firebase Remote Config**, que se usa como:
- **Interruptor de emergencia (kill switch):** si se detecta una ola de baneos, se desactiva la verificación en todas las instalaciones sin publicar una versión nueva.
- **Versión mínima del motor:** obliga a actualizar cuando WhatsApp cambia el protocolo.

## 4. App Android

| Área | Elección | Justificación |
|---|---|---|
| Lenguaje | **Kotlin 2.x** | Estándar de Android |
| UI | **Jetpack Compose** + Material 3 | UI de ajustes/historial; el overlay usa `ComposeView` dentro de `WindowManager` |
| Min SDK / Target SDK | **minSdk 29** (Android 10) / **targetSdk 35+** | Android 10 introduce `ROLE_CALL_SCREENING` |
| Arquitectura | Clean Architecture ligera + MVVM, módulos por feature | Casos de uso testeables sin framework |
| Inyección de dependencias | **Hilt** | Integración nativa con Services y ViewModels |
| Concurrencia | **Kotlin Coroutines + Flow** | `withTimeout` en conexión y consulta |
| Motor WhatsApp | **Baileys + nodejs-mobile** o **librería JVM** 🔶 | Ver sección 2 |
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
engine/baileys            # Implementación F1 (nodejs-mobile + bridge)   🔶
engine/jvm                # Implementación F2 (librería JVM)             🔶
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
