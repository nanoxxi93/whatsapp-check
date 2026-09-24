# Plan — Spike de `CallScreeningService` + overlay

> **Estado:** En progreso · **Fecha:** 2026-09-24
> **Relacionado:** [requirements.md](requirements.md) · [validation.md](validation.md) · [tech-stack](../product/tech-stack.md)

## Enfoque

Extender la app de pruebas `prototypes/android-engines` (ya tiene el motor whatsmeow y la sesión vinculada en el Poco) con el camino de la llamada. Es un **spike**: se busca comprobar en MIUI y en Android 14 lo que la documentación no garantiza (que el aviso se vea sobre la pantalla de llamada, también bloqueada, y que el proceso sobreviva mientras consulta). No se busca el diseño final.

**Hipótesis:** con el permiso de overlay concedido, el proceso sigue vivo mientras el aviso está visible (Android lo trata como perceptible), así que la verificación no necesita un foreground service propio. **Aparte**, a pedido del autor, un servicio "siempre activo" mantiene el proceso vivo entre llamadas para que la optimización de batería (sobre todo en MIUI) no cierre la app. El registro de salud dirá si hace falta.

## Diseño

```
Llamada entrante (número no guardado)
  └─► ScreeningService.onScreenCall(details)                    tScreen = 0
        ├─► respondToCall(allow)                                 ≤ 100 ms
        ├─► OverlayController.show("⏳ Verificando…")            tOverlay
        └─► CallCheck (corrutina en el scope de la app)
              ├─ normalizar número (PhoneNumberUtils, PE)
              ├─ WhatsmeowEngine.connect() + check() + close()
              └─► OverlayController.update(✅ / ⚠️ / ❔)          tResult
                    └─ calls.jsonl (número enmascarado)

Sin permiso de overlay (RF-05):
  onScreenCall → verifica dentro del plazo (máx. 4 s) → respondToCall(allow) → notificación heads-up
```

| Módulo | Cambio |
|---|---|
| `app/.../call/ScreeningService.kt` | Nuevo. `CallScreeningService` con `BIND_SCREENING_SERVICE` |
| `app/.../call/CallCheck.kt` | Nuevo. Normalización, consulta con el motor, tiempos y registro en `calls.jsonl` |
| `app/.../call/OverlayController.kt` | Nuevo. Ventana `TYPE_APPLICATION_OVERLAY` con Views simples (no Compose, para no montar un ciclo de vida en un servicio); arrastrable; toque para cerrar; se oculta sola a los 45 s |
| `app/.../call/ResultNotifier.kt` | Nuevo. Canal de prioridad alta y notificación heads-up |
| `MainActivity.kt` | Estado y pedido de rol, overlay y notificaciones; botón "Mostrar aviso de ejemplo" |
| `AndroidManifest.xml` | Servicio con `android.permission.BIND_SCREENING_SERVICE`; permisos `SYSTEM_ALERT_WINDOW`, `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `RECEIVE_BOOT_COMPLETED`, `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` |
| `app/.../call/KeepAliveService.kt` | Nuevo. Foreground service `specialUse` con notificación fija de prioridad mínima; latido cada hora en `health.jsonl`; `START_STICKY` |
| `app/.../call/BootReceiver.kt` | Nuevo. Reinicia el servicio al encender el teléfono |
| `app/.../call/Journal.kt` | Nuevo. Escribe `calls.jsonl` y `health.jsonl`; calcula el resumen; arma el archivo para compartir (`FileProvider`) |

Posición inicial del aviso: **tercio superior**, debajo del nombre o número que muestra el marcador. Los botones de contestar y colgar suelen estar en la mitad inferior. Se ajusta con las capturas de T6.

## Decisiones

| Decisión | Alternativas consideradas | Motivo |
|---|---|---|
| Sin foreground service en el camino con overlay | `shortService` / `specialUse` / `dataSync` | Un overlay visible mantiene el proceso perceptible; evita tipos de FGS que Google Play revisa. Si el spike muestra que MIUI mata el proceso, se agrega un FGS y se registra en `intentos-fallidos.md` |
| En el camino sin overlay, esperar el resultado antes de responder (máx. 4 s) | Responder al instante y notificar después | Sin overlay no hay nada que mantenga vivo el proceso después de responder; mientras no responde, Telecom mantiene el servicio enlazado. Android da ~5 s |
| `PhoneNumberUtils.formatNumberToE164` | libphonenumber | Viene con Android; alcanza para el spike. La app final evaluará libphonenumber (tech-stack) |
| Views simples en el overlay | `ComposeView` | Compose en una ventana de servicio necesita montar `LifecycleOwner` y `SavedStateRegistryOwner`; para un spike no aporta |
| Cierre por toque o a los 45 s | Cerrar al contestar o colgar | Detectar el estado de la llamada requiere `READ_PHONE_STATE`, que la app no pide |
| Servicio "siempre activo" con tipo `specialUse` | `dataSync`, `shortService`, sin servicio | Pedido del autor. `specialUse` describe honestamente el caso (proteger al usuario durante llamadas entrantes); `dataSync` tiene límite de 6 h/día en Android 15 y `shortService` dura 3 min. En Google Play requiere justificación: queda para la revisión legal y de distribución |
| Resumen y exportación en la app | Extraer por `adb` | El autor no quiere depender de la PC durante la semana de uso real |

## Tareas

- [x] **T1** — Manifest, `ScreeningService` (respuesta inmediata + lanzar la verificación) y pedido del rol en `MainActivity`. · cubre `RF-01`, `RF-02`
- [x] **T2** — `CallCheck`: normalización, consulta con el motor, tiempos y `calls.jsonl` enmascarado. · cubre `RF-03`, `RF-06`, `RNF-04`
- [x] **T3** — `OverlayController` (Verificando → resultado, arrastrable, toque, 45 s) y el botón "Mostrar aviso de ejemplo". · cubre `RF-04`, `RF-07`
- [x] **T4** — Camino sin overlay: espera ≤ 4 s y notificación heads-up. · cubre `RF-05`
- [x] **T4b** — `KeepAliveService` + `BootReceiver` + pedido de exclusión de batería; `Journal` con `health.jsonl`, resumen en pantalla y "Exportar". · cubre `RF-08`, `RF-09`, `RF-10`
- [ ] **T5** — Instalar en el Poco (y el Moto); conceder rol y permisos, incluidos los permisos propios de MIUI; guía corta en el README y en la pantalla. · cubre `RF-07`, `RF-08`
- [ ] **T6** — **Semana de uso real** (decisión del autor, 2026-09-24): las llamadas de desconocidos que reciba el Poco; el autor toma una captura de pantalla de al menos una llamada entrante con el aviso y anota si alguna llamada se comportó raro. Una llamada desde el Moto (contacto guardado) para CA-03. Al final, el autor exporta el registro desde la app. *(requiere al autor)* · cubre `RNF-01`, `RNF-02`, `RNF-03`, `RNF-05`
- [ ] **T7** — Analizar lo exportado (resumen, latencias, huecos en los latidos, eventos de sesión) y dejar la app instalada para seguir la prueba de resistencia al baneo.

## Riesgos

| Riesgo | Mitigación |
|---|---|
| MIUI no muestra el overlay sobre la llamada o con la pantalla bloqueada | Conceder los permisos propios de MIUI; si aun así falla, probar la notificación heads-up y documentarlo (Fase 1: guía por fabricante) |
| MIUI mata el proceso al quitar la app de recientes y Telecom no la despierta | Probar "Inicio automático" y la exclusión de ahorro de batería; si no alcanza, FGS o limitación documentada |
| La conexión en frío tarda más con la pantalla apagada o el teléfono en reposo (Doze) | Medir `tResult` con la pantalla bloqueada; si es peor, evaluar la conexión persistente (decisión abierta 1) |
| El número del que se llama está guardado en contactos → no se invoca el servicio | Llamar desde un número no guardado (pregunta abierta) |

## Cambios a documentos base

- Si se confirma que no hace falta un FGS: `tech-stack.md` §1 y §3, cambiar "Foreground service de corta duración 🔶" por lo verificado.
- Permisos propios de MIUI a sumar al ítem de onboarding de la Fase 1 ("guía por fabricante").
