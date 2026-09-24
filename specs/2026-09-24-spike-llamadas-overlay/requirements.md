# Requisitos — Spike de `CallScreeningService` + overlay

> **Estado:** En progreso · **Fecha:** 2026-09-24
> **Ítem del roadmap:** Fase 0 — Spike de `CallScreeningService` + overlay (`M`)
> **Relacionado:** [plan.md](plan.md) · [validation.md](validation.md) · [roadmap](../product/roadmap.md) · [tech-stack §1 y §3](../product/tech-stack.md#3-arquitectura-general)

## Objetivo

Demostrar en los teléfonos reales del autor el flujo completo del producto: **entra una llamada de un desconocido → la app recibe el número → deja sonar la llamada de inmediato → consulta con whatsmeow → muestra ✅ / ⚠️ / ❔ en un aviso flotante sobre la pantalla de llamada**, sin tapar los botones de contestar y colgar. La misma app queda instalada para la *prueba de resistencia al baneo* con uso real.

## Contexto

- El motor está elegido y medido: whatsmeow, total en frío p95 **2,15 s** con 4G ([comparación](../2026-09-23-comparar-motores-android/validation.md)). No altera el WhatsApp del usuario ([efectos](../2026-09-23-efectos-whatsapp-principal/validation.md)).
- Arquitectura prevista en [tech-stack §3](../product/tech-stack.md#3-arquitectura-general): `CallScreeningService` → `respondToCall(allow)` inmediato → verificación → aviso. Android da ~5 s para responder; responder primero evita ese límite.
- Teléfonos: **Poco M5s** (Android 13, MIUI 14), donde **entran las llamadas del autor** y ya está vinculada la sesión de whatsmeow, y **Moto G53** (Android 14), que tiene el WhatsApp del número secundario y sirve para llamar al Poco.
- MIUI tiene permisos propios además de los de Android: *"Mostrar ventanas emergentes mientras se ejecuta en segundo plano"*, *"Mostrar en pantalla de bloqueo"* y *"Inicio automático"*. Pueden impedir el aviso aunque Android lo permita.
- `CallScreeningService` **solo se invoca para números que no están en los contactos** (salvo que la app sea el marcador predeterminado). Para probar, el número que llama no puede estar guardado en el Poco.

## Historias de usuario

- Como usuario, cuando me llama un número desconocido quiero ver en la pantalla de la llamada, en menos de 3 segundos, si ese número tiene WhatsApp, sin que eso me impida contestar o colgar.

## Requisitos funcionales

| ID | Requisito | Prioridad |
|---|---|---|
| RF-01 | La app pide el rol `ROLE_CALL_SCREENING` con `RoleManager` y muestra si lo tiene. | Obligatorio |
| RF-02 | `CallScreeningService.onScreenCall`: toma el número de `Call.Details.handle`, responde **de inmediato** `respondToCall(allow)` sin silenciar ni rechazar, y lanza la verificación. | Obligatorio |
| RF-03 | Normalización del número entrante a E.164 sin `+` con `PhoneNumberUtils.formatNumberToE164` (país por defecto PE). Si no se puede normalizar (número oculto o inválido), no se consulta y el aviso muestra ❔. | Obligatorio |
| RF-04 | **Aviso flotante** (`TYPE_APPLICATION_OVERLAY`): aparece con "⏳ Verificando…" y cambia a ✅ *Tiene WhatsApp* / ⚠️ *No tiene WhatsApp* / ❔ *No se pudo verificar*. Es chico, se puede arrastrar, se cierra con un toque y se oculta solo a los 45 s. | Obligatorio |
| RF-05 | Si no hay permiso de overlay: **notificación heads-up** con el resultado. En ese caso la app espera el resultado (máx. 4 s) antes de responder la llamada, para seguir enlazada mientras consulta; al vencer el plazo responde igual y la notificación muestra ❔. | Obligatorio |
| RF-06 | Registro de cada llamada filtrada en `files/results/calls.jsonl`: fecha, número **enmascarado**, resultado, motivo si es ❔, tiempos (`tScreen` → respuesta, `tOverlay` → aviso visible, `tResult` → resultado mostrado) y si se usó overlay o notificación. | Obligatorio |
| RF-07 | Pantalla de la app de pruebas: estado del rol, del permiso de overlay, de notificaciones y de la optimización de batería, con botones para pedirlos, y un aviso de prueba ("Mostrar aviso de ejemplo") para ajustar la posición sin esperar una llamada. | Obligatorio |
| RF-08 | **Modo "siempre activo"** (activado por defecto): servicio en primer plano con una notificación fija de prioridad mínima ("WhatsCheck activo"), que se reinicia al encender el teléfono. Pedido de **excluir la app de la optimización de batería** y guía de los permisos propios de MIUI. *(Agregado 2026-09-24 a pedido del autor: las optimizaciones de batería suelen cerrar este tipo de apps.)* | Obligatorio |
| RF-09 | **Recolección sin PC:** pantalla de **resumen** en la app (llamadas filtradas, ✅/⚠️/❔, p50/p95 de `tResult`, errores, estado de la sesión, último latido) y botón **"Exportar"** que comparte `calls.jsonl` y `health.jsonl` con el menú de compartir de Android (solo números enmascarados). *(Agregado 2026-09-24 a pedido del autor.)* | Obligatorio |
| RF-10 | **Registro de salud** en `files/results/health.jsonl`: un latido por hora mientras el servicio vive, arranque del servicio (y si fue tras reiniciar el teléfono), y eventos de la sesión de whatsmeow que importan para la prueba de baneo (desvinculación, baneo temporal, fallos de conexión). | Obligatorio |

## Requisitos no funcionales

| ID | Requisito | Métrica |
|---|---|---|
| RNF-01 | **Nunca estorbar la llamada.** | `respondToCall(allow)` en ≤ 100 ms (salvo RF-05, ≤ 4 s); ninguna llamada perdida ni silenciada por la app |
| RNF-02 | **Latencia del resultado.** | Desde que entra la llamada hasta ver el resultado: p95 ≤ 3 s (con la app en frío) |
| RNF-03 | **El aviso no tapa los botones** de contestar y colgar. | Captura de pantalla de la llamada entrante en ambos teléfonos |
| RNF-04 | **Privacidad.** | Números enmascarados en el log y en logcat; no se piden `READ_CONTACTS`, `READ_CALL_LOG` ni `READ_PHONE_STATE` |
| RNF-05 | **Funciona con la app cerrada** (quitada de recientes) y con la pantalla bloqueada. | Comprobado en ambos teléfonos |

## Principios de la misión que aplican

| Principio | Cómo se respeta en este feature |
|---|---|
| Nunca estorbar la llamada | Responde "permitir" de inmediato (RNF-01); el aviso es chico, arrastrable y se cierra solo (RF-04). |
| Fallar en silencio | Sin sesión, sin red, número oculto o error → ❔, nunca ⚠️ (RF-03, RF-04). |
| Honestidad en la señal | El texto dice "Tiene / No tiene WhatsApp", no "spam". |
| Lo del usuario se queda en su teléfono | El log de llamadas queda en `filesDir`, enmascarado. |

## Fuera de alcance

- Detectar cuándo se contesta o se cuelga para cerrar el aviso: requiere `READ_PHONE_STATE`, que la app no pide. En el spike el aviso se cierra con un toque o a los 45 s.
- Silenciar o rechazar automáticamente: Fase 3.
- Caché de resultados, historial, onboarding final: Fase 1.

## Dependencias

- Sesión de whatsmeow vinculada en el Poco (lista).
- **Un teléfono que llame al Poco con un número que no esté guardado en sus contactos** (el Moto, si su número no está guardado en el Poco).
- Llamadas reales de desconocidos durante **una semana** (decisión del autor, 2026-09-24): no hay que llamar a propósito ni conectar el teléfono a la PC. Esa semana cuenta también como la primera de la prueba de resistencia al baneo.

## Preguntas abiertas

- [x] ¿El número del Moto está guardado en los contactos del Poco? → **Sí.** No se usa para las pruebas positivas; sirve para CA-03 (un contacto **no** dispara el aviso). Las pruebas positivas son las llamadas reales de desconocidos durante una semana. *(2026-09-24)*
