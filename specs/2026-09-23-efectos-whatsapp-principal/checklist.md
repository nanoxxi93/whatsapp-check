# Validación — Efectos secundarios en el WhatsApp principal

> **Estado:** Validado · **Fecha:** 2026-09-23
> **Relacionado:** [spec.md](spec.md) · [plan.md](plan.md)

## Criterios de aceptación

| ID | Requisito | Criterio (verificable) | Método | Obligatorio |
|---|---|---|---|---|
| CA-01 | RF-01 | `hold 60` mantiene la sesión conectada 60 s y registra en `actions.log` los eventos por tipo, sin contenido. | Manual por `adb` + revisión del log | Sí |
| CA-02 | RF-02 | `stats` lista las tablas de whatsmeow con su cantidad de filas; tests de Go de `TableCounts` sobre un store vacío. | `go test` + `adb` | Sí |
| CA-03 | RF-04 | **Escenario B:** durante `hold 60`, el observador **no ve "en línea"** en el chat del secundario. | Observación del autor | Sí |
| CA-04 | RF-04 | **Escenario C:** durante 5 conexiones cortas, el observador **no ve "en línea"** ni un instante. | Observación del autor | Sí |
| CA-05 | RF-04 | **Escenario B:** el mensaje enviado durante `hold` queda en **doble check gris**, no azul, hasta el escenario E. | Observación del autor | Sí |
| CA-06 | RF-04 | **Escenario B:** el teléfono del secundario **muestra la notificación** del mensaje enviado durante `hold`. | Observación del autor | Sí |
| CA-07 | RF-04 | **Escenario E:** los chats de A y B siguen **no leídos** en el teléfono del secundario hasta que se abren. | Observación del autor | Sí |
| CA-08 | RF-03 | El texto `prueba WhatsCheck` **no aparece** en `whatsmeow.db`, `-wal` ni `-shm` después de B. | `run-as … grep -c` | Sí |
| CA-09 | RF-05 | Queda documentado qué tablas tienen filas y, si alguna guarda datos personales (p. ej. contactos), la decisión tomada. | Revisión | Sí |

## Criterios Go / No-Go

| Resultado | Go | Go con reservas | No-Go |
|---|---|---|---|
| "En línea" visible (CA-03, CA-04) | Nunca | — | Alguna vez → la app delataría al usuario |
| Confirmación de lectura (CA-05, CA-07) | Nunca | — | Algún mensaje pasa a azul o a leído |
| Notificaciones del teléfono (CA-06) | Llegan normal | Se retrasan o se silencian solo mientras la sesión está conectada (~2 s en uso real) | Se silencian también después de desconectar |
| Mensajes en la base (CA-08) | Nunca | — | Aparece el texto → hay que cambiar la configuración antes de seguir |

## Plan de pruebas

### Automáticas
- `go test ./...`: `TableCounts` sobre un store vacío.

### Manuales
Protocolo A–E de [plan.md](plan.md#protocolo-t4), con el número secundario y el observador en WhatsApp Web.

## Definición de terminado

- [x] Todos los criterios obligatorios pasaron y están en el registro.
- [x] `go test` en verde.
- [x] Sin números ni textos personales en git ni en los documentos.
- [x] Tareas de [plan.md](plan.md) marcadas.
- [x] tech-stack §2 actualizado según el resultado (con aprobación).
- [x] Ítem marcado `[x]` en el roadmap.

## Registro de resultados

| Fecha | Criterio | Método / comando | Resultado | Evidencia / notas |
|---|---|---|---|---|
| 2026-09-23 | CA-02 | `go test ./...` | ✅ Pasa | 5 tests; `TableCounts` sobre store vacío devuelve las tablas con 0 filas; `Hold` sin sesión falla |
| 2026-09-23 | CA-02 (dispositivo) / CA-09 | `action stats` en el Poco, antes del protocolo | ℹ️ | `contacts` **131**, `chat_settings` 10, `app_state_mutation_macs` 331, `app_state_sync_keys` 13, `app_state_version` 5, `pre_keys` 812, `sessions` 1, `identity_keys` 1, `lid_map` 1, `device` 1; **`event_buffer`, `retry_buffer`, `message_secrets`, `sender_keys`: 0**. whatsmeow sincroniza la agenda del secundario al vincular (datos que la app no necesita) → T5 |
| 2026-09-23 | Escenario A (línea base) | Observador envía `prueba WhatsCheck A` con la sesión de whatsmeow **desconectada** (proceso detenido) | ❌ **Línea base inválida** | Observador: sin "en línea", **checks azules**, **sin notificación** en el Moto. Como whatsmeow no estaba conectado, otro cliente del número secundario leyó el mensaje y dejó al teléfono sin notificar. Hipótesis: el otro proyecto del autor tiene una sesión vinculada y siempre conectada en ese mismo número (marca como leído; con un dispositivo activo WhatsApp no notifica en el teléfono). Pendiente confirmar en *Dispositivos vinculados* |
| 2026-09-23 | Escenario A2 (línea base) | Otro proyecto del autor desvinculado del número secundario; whatsmeow desconectado; observador envía `prueba WhatsCheck A2` | ✅ Limpia (checks y notificaciones) | **Dos checks grises** y **notificación en el Moto**. Se confirma que la contaminación de A venía del otro cliente vinculado (leía mensajes y, al estar siempre activo, dejaba el teléfono sin notificación). "En línea": el observador no lo veía nunca, ni con WhatsApp abierto en el Moto → control positivo fallido |
| 2026-09-23 | Control de "en línea" | Revisión de la privacidad del observador | ⚠️ Corregido | El número personal del autor tenía oculta su "última vez"; por la regla de reciprocidad de WhatsApp tampoco veía la de otros. El autor la activó. Se repite desde la línea base con control positivo. Un primer `hold 300` (23:24) se cortó antes de observar B |
| 2026-09-23 | Control positivo + escenario A3 | Observador con su "última vez" visible; WhatsApp del secundario abierto en el Moto y luego cerrado; `prueba WhatsCheck A3` | ✅ Línea base válida | Con la app abierta el observador **ve "en línea"** (el control funciona); cerrada: sin "en línea", **dos grises**, **notificación en el Moto** |
| 2026-09-23 | CA-01 | `action hold --es seconds 180` (23:49:55–23:53:00) | ✅ Pasa | `actions.log`: `{"*events.Connected":1,"*events.Message":4,"*events.OfflineSyncCompleted":1,"*events.OfflineSyncPreview":1,"*events.Receipt":1}`: solo tipos, sin contenido |
| 2026-09-23 | CA-03, CA-05, CA-06 | Escenario B: observador envía `prueba WhatsCheck B` y `B2` con whatsmeow conectado | ✅ Pasa | **Sin "en línea"**, **dos checks grises**, **notificaciones en el Moto** con los mensajes sin leer |
| 2026-09-23 | CA-04 | Escenario C: 5 conexiones cortas (`bench.sh … 5`, 23:53:01–23:54:25; totales 1,07–2,61 s) | ✅ Pasa | El observador **no vio "en línea"** en ningún momento |
| 2026-09-23 | CA-07 | Escenario E: el autor abre WhatsApp en el Moto | ✅ Pasa | Recién al abrir la app aparece "en línea", y recién al entrar al chat los mensajes pasan a **azul**. Hasta entonces seguían sin leer |
| 2026-09-23 | CA-08 | Escenario D: `run-as … grep -c -a 'prueba WhatsCheck'` en `whatsmeow.db`, `-wal`, `-shm` | ✅ Pasa | **0 coincidencias** en los tres archivos (también sin distinguir mayúsculas). whatsmeow no guarda el texto de los mensajes |
| 2026-09-23 | CA-09 | `action stats` después de B y C | ℹ️ → T5 | `contacts` 132 (+1: nombre público del observador), `message_secrets` **6** (una llave por mensaje recibido: ID y remitente, sin texto), `chat_settings` 10, `sessions`/`identity_keys` 2 (sesión cifrada con el observador, **necesaria** para descifrar y confirmar la entrega) |
| 2026-09-24 | CA-09 (T5) | Stores de contactos, ajustes de chats y secretos reemplazados por versiones que no guardan nada (`nostore.go`), y vaciado de esas tablas al abrir el motor; 7 tests de Go | ✅ Pasa | En el Poco: consulta ✅ (2,4 s) y `stats` con **`contacts` 0, `chat_settings` 0, `message_secrets` 0**. Quedan solo sesión, llaves, `lid_map` 1, `privacy_tokens` 1 y, del estado de la app, hashes (`mutation_macs` 330) y versiones, sin nombres ni números legibles |

## Bloqueos

- ~~**Línea base contaminada**~~ resuelto: el autor desvinculó su otro proyecto del número secundario.

Ninguno pendiente. Todos los criterios obligatorios pasaron.
