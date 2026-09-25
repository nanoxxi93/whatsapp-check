# Validación — Prototipo de escritorio con Baileys

> **Estado:** Validado · **Fecha:** 2026-09-23
> **Relacionado:** [spec.md](spec.md) · [plan.md](plan.md)

## Criterios de aceptación

| ID | Requisito | Criterio (verificable) | Método | Obligatorio |
|---|---|---|---|---|
| CA-01 | RF-01 | Dado un número secundario sin vincular, cuando se ejecuta `link`, entonces se muestra un código de 8 caracteres y, al ingresarlo en *Vincular con número de teléfono*, la sesión queda abierta y el dispositivo aparece en *Dispositivos vinculados*. | Manual con número secundario | Sí |
| CA-02 | RF-01 | Si no se confirma que es un número secundario, `link` termina sin pedir código. | Manual | Sí |
| CA-03 | RF-02 | Después de `link`, un `check` en un proceso nuevo funciona sin volver a vincular. | Manual | Sí |
| CA-04 | RF-03, RF-04 | `check` de un número que se sabe que tiene WhatsApp → `HAS_WHATSAPP`; de `51000000001` (se consulta como `5100000001`, ver IF-007) → `NO_WHATSAPP`. | Manual | Sí |
| CA-05 | RF-04, RF-05, RNF-06 | Los tests de `result.ts` cubren: `exists: true`, `[]`, `exists: false`, `jid` distinto con una sola entrada, error lanzado, timeout y sesión cerrada, y todos pasan. | Test automático (`npm test`) | Sí |
| CA-06 | RF-04 | Con la red desconectada, `check` devuelve `UNKNOWN`, nunca `NO_WHATSAPP`. | Manual (desactivar red) | Sí |
| CA-07 | RF-06, RF-11 | Cada línea del JSONL tiene fecha, número enmascarado, resultado, `t_ws`, `t_ready`, `t_query` y `t_total`. `t_total` solo tiene valor en la consulta hecha justo después de conectar en frío (`cold: true`); en las demás es `null`. | Revisión del archivo | Sí |
| CA-08 | RF-07 | `bench --runs 20` termina y reporta mínimo, p50, p95 y máximo de cada fase; `stats.ts` tiene tests con valores conocidos. | Manual + test automático | Sí |
| CA-09 | RF-08 | En el código de `session.ts` están `markOnlineOnConnect: false`, sin sincronización de historial y sin llamadas a `readMessages` ni `sendPresenceUpdate`. | Revisión de código | Sí |
| CA-10 | RF-09 | Tras cerrar la sesión desde el teléfono (*Dispositivos vinculados → Cerrar sesión*), `check` informa "sesión cerrada, vuelve a vincular" y no consulta. | Manual | Sí |
| CA-11 | RF-10 | `status` muestra el número enmascarado; `unlink` elimina el dispositivo de *Dispositivos vinculados* y borra `.auth/`. | Manual | Sí |
| CA-12 | RNF-01 | Un lote de 51 números se rechaza sin confirmación; entre consultas hay 3–6 s (visible en los tiempos del JSONL). | Manual | Sí |
| CA-13 | RNF-02 | Ningún número completo aparece en la salida de consola de `check`, `bench` ni `status`. | Revisión de la salida | Sí |
| CA-14 | RNF-03 | Después de usar todos los comandos, `git status` no muestra `.auth/` ni `results/`. | Comando | Sí |
| CA-15 | RNF-04 | `package.json` tiene versiones exactas y existe `package-lock.json`. | Revisión | Sí |
| CA-16 | RNF-05 | Sin red, `check` termina en ≤ 20 s con `UNKNOWN`, sin quedar colgado. | Manual | Sí |

## Criterios Go / No-Go

Deciden la **estrategia de conexión** de [tech-stack §3](../product/tech-stack.md#estrategia-de-conexión-). Se miden con `bench --runs 20` sobre un número con WhatsApp, anotando el tipo de red.

| Métrica | Go "bajo demanda" | Go con reservas | No-Go "bajo demanda" |
|---|---|---|---|
| `t_total` p95 (conexión en frío + 1 consulta) | ≤ 3 s | 3–8 s: sirve gracias al aviso "Verificando…", pero se prioriza evaluar la conexión persistente | > 8 s: se pasa a conexión persistente |
| Corridas con `UNKNOWN` por fallo de conexión | 0 de 20 | 1–2 de 20 | ≥ 3 de 20 |
| Vinculación con código de emparejamiento (CA-01) | Funciona | — | No funciona con ninguna versión de Baileys → se reabre [IF-006](../product/intentos-fallidos.md#if-006--vincular-whatsapp-con-código-qr) |

## Plan de pruebas

### Automáticas
- `npm test` (`node:test` con `tsx`): `phone.test.ts`, `result.test.ts`, `stats.test.ts`.

### Manuales
Todas con el **número secundario** del autor, nunca con una cuenta personal.
1. `link <secundario>` → ingresar el código en el teléfono (CA-01, CA-02).
2. `status` (CA-11, primera parte).
3. `check <número con WhatsApp> 51000000001` (CA-03, CA-04, CA-07, CA-13).
4. `bench <número con WhatsApp> --runs 20` (CA-08, Go/No-Go).
5. Desactivar la red → `check` (CA-06, CA-16).
6. Lote de 51 números (CA-12).
7. Cerrar la sesión desde el teléfono → `check` (CA-10).
8. Volver a vincular → `unlink` (CA-11, segunda parte).
9. `git status` (CA-14).

## Definición de terminado

- [x] Todos los criterios obligatorios pasaron y están en el registro.
- [x] `npm test` en verde.
- [x] Sin números completos ni credenciales en consola, git o archivos versionados.
- [x] Tareas de [plan.md](plan.md) marcadas.
- [x] Resultado Go / No-Go anotado y, si corresponde, decisión 2 del roadmap (*¿conexión bajo demanda o persistente?*) actualizada.
- [x] Ítem marcado `[x]` en el roadmap.

## Registro de resultados

| Fecha | Criterio | Método / comando | Resultado | Evidencia / notas |
|---|---|---|---|---|
| 2026-09-23 | CA-05 | `npm test` | ✅ Pasa | 26 tests, 0 fallos. `result.test.ts` cubre `exists: true`, `[]`, `exists: false`, `jid` distinto, varias entradas, `undefined`, error, timeout y sesión cerrada |
| 2026-09-23 | CA-08 (parte automática) | `npm test` | ✅ Pasa | `stats.test.ts`: percentiles con valores conocidos y umbrales del veredicto. Falta la corrida real de `bench` |
| 2026-09-23 | CA-02 | `echo no \| npm start -- link 987654321` | ✅ Pasa | "Cancelado. No se pidió ningún código." Exit 1, sin crear `.auth/` |
| 2026-09-23 | CA-09 | Revisión de `src/session.ts` + `grep` | ✅ Pasa | `markOnlineOnConnect: false`, `syncFullHistory: false`, `shouldSyncHistoryMessage: () => false`; ninguna llamada a `readMessages`, `sendPresenceUpdate` ni manejador de `messages.upsert` |
| 2026-09-23 | CA-12 (rechazo) | `check --file` con 51 números | ✅ Pasa | "Son 51 números y el máximo por ejecución es 50. Usa --max 51…" Exit 1, sin conectar. Falta verificar las pausas de 3–6 s en el JSONL |
| 2026-09-23 | CA-15 | Revisión de `package.json` | ✅ Pasa | Todas las versiones exactas (`@whiskeysockets/baileys` 7.0.0-rc14, sin `^`); `package-lock.json` presente |
| 2026-09-23 | CA-14 (parcial) | `git check-ignore -v` | ✅ Pasa | `.auth/`, `results/` y `node_modules/` ignorados. Repetir `git status` después de las pruebas manuales |
| 2026-09-23 | — (humo) | Socket con credenciales vacías hasta el evento `qr`, sin pedir código | ✅ Conecta | WebSocket abierto en 302 ms; listo para pedir código en 643 ms. Baileys 7.0.0-rc14 conecta desde la máquina de desarrollo (sin error 405 de versión) |
| 2026-09-23 | CA-01 (intento 1) | `link +5192*****19` con el número secundario | ❌ No completó | Se mostró el código `5S6J-KEHD`, pero el emparejamiento no terminó (credenciales sin `registered` ni `account`). Los 3 reintentos fallaron con 401 por credenciales a medias → corregido, ver IF-008. Pendiente repetir |
| 2026-09-23 | CA-01 (intento 2) | `link` con credenciales limpias, identidad "Chrome (WhatsCheck)" | ❌ Rechazado por el teléfono | El teléfono respondió *"No se pudo vincular el dispositivo. Comprueba que el número de teléfono es correcto en tu dispositivo u obtén un código nuevo"*. Se cambia la identidad por defecto a `Browsers.macOS('Chrome')` y se agrega `--browser`. Pendiente repetir |
| 2026-09-23 | CA-01 | `link +5192*****19` (identidad por defecto `macos`) | ✅ Pasa | Código aceptado en el teléfono; credenciales con `registered: true` y `account`. `status` muestra `+5192*****19` |
| 2026-09-23 | CA-11 (parte 1) | `status` | ✅ Pasa | "Sesión vinculada: +5192*****19" |
| 2026-09-23 | CA-03 | `check` en un proceso nuevo, sin volver a vincular | ✅ Pasa | Conectó con las credenciales guardadas |
| 2026-09-23 | CA-04 | `check +5192*****19 51000000001 --network wifi-dev` | ✅ Pasa | El propio número secundario → `HAS_WHATSAPP` (0,28 s); `51000000001` (consultado como `5100000001`) → `NO_WHATSAPP` (0,16 s). Se usó el propio número para no consultar números de terceros |
| 2026-09-23 | CA-07 | Revisión de `results/2026-09-23.jsonl` | ✅ Pasa | Ambas líneas con `ts`, `phone` enmascarado, `status`, `tWs`, `tReady`, `tQuery` y `tTotal` (solo en la fría: 1962 ms) |
| 2026-09-23 | CA-13 (check) | Salida de `check` | ✅ Pasa | Solo números enmascarados en tabla, progreso y logs de Baileys |
| 2026-09-23 | CA-12 (pausas) | Tiempos del JSONL de `check` | ✅ Pasa | 3,4 s entre la primera y la segunda consulta (dentro de 3–6 s) |
| 2026-09-23 | CA-08 | `bench +5192*****19 --runs 20 --network wifi-dev` | ✅ Pasa | 20/20 ✅. p50 / p95 / máx: sesión lista 1,40 / 2,05 / 2,25 s · consulta 0,21 / 0,32 / 0,38 s · **total en frío 1,59 / 2,36 / 2,63 s** |
| 2026-09-23 | **Go / No-Go** | Mismo `bench` | ✅ **Go "bajo demanda"** | p95 total 2,36 s (≤ 3 s) y 0 de 20 corridas sin verificar. Red: wifi doméstica, escritorio. Vinculación con código: funciona (CA-01) |
| 2026-09-23 | — (observación) | Logs del `bench`, corrida 15 | ℹ️ | Baileys registró `timed out waiting for message` y un `TypeError` en `fetchPrivacySettings` (consultas iniciales automáticas, `fireInitQueries`). La corrida terminó bien (✅, 1,21 s): probablemente la conexión se cerró antes de que esas consultas respondieran. No afecta la verificación |
| 2026-09-23 | CA-10 | Cerrar sesión desde el teléfono → `check` | ✅ Pasa | "❔ La sesión fue cerrada desde el teléfono… (código 401)", resultado `session_closed`, sin consultar |
| 2026-09-23 | CA-06 | `check` sin red (autor; confirmó que fue con el wifi desconectado) | ✅ Pasa | `UNKNOWN` (`not_connected`, código 408), nunca `NO_WHATSAPP` |
| 2026-09-23 | CA-16 | Mismo `check` sin red | ✅ Pasa | Terminó en 57 ms (entre el inicio del socket y el registro del resultado), sin colgarse |
| 2026-09-23 | CA-11 (parte 2, parcial) | `unlink` con la sesión ya cerrada desde el teléfono | ✅ Rama local | "La sesión ya estaba cerrada desde el teléfono; se borraron las credenciales locales." **Falta** probar `unlink` con la sesión activa y ver que el dispositivo desaparezca del teléfono |
| 2026-09-23 | CA-01 (revinculación) | `link` después de `unlink` | ✅ Pasa | Código aceptado → 515 → reconexión → "✅ Vinculado" |
| 2026-09-23 | CA-01 (identidad propia) | `unlink` + `link --browser whatscheck` (`['WhatsCheck', 'Google Chrome', '1.0.0']`) | ✅ Vincula | Credenciales nuevas a las 19:34. El teléfono muestra **"Google Chrome (Mac OS)"**, no "WhatsCheck". Ver nota en IF-009 |
| 2026-09-23 | CA-11 (parte 2) | `unlink` con la sesión activa | ✅ Pasa | "Sesión cerrada en WhatsApp y credenciales borradas". En el teléfono, el dispositivo pasó a **"cierre de sesión pendiente"**: WhatsApp aceptó la desvinculación y la aplica con demora (normal según el autor) |
| 2026-09-23 | CA-13 (bench y status) | Salidas de `bench` y `status` | ✅ Pasa | Solo `+5192*****19`. Con `check` ya había pasado |
| 2026-09-23 | — (observación) | ~30 conexiones con la sincronización de historial desactivada | ℹ️ | Ningún error de sesión atribuible a la falta de equivalencias número ↔ LID. Se mantiene desactivada (ver spec.md) |
| 2026-09-23 | CA-14 | `git status` después de usar todos los comandos | ✅ Pasa | No aparecen `.auth/` ni `results/` |
| 2026-09-23 | — (defecto) | `link` tras un 401 | ❌ → corregido | Respondía "Ya hay una sesión vinculada" porque las credenciales inválidas seguían guardadas. Ahora un 401 las borra solas |
| 2026-09-23 | — (defecto) | Salida del proceso tras `check` | ❌ → corregido | 1 de ~25 ejecuciones abortó con `Assertion failed … src\win\async.c`. Ver IF-010. Después del arreglo: 5 ejecuciones seguidas con exit 0; `end()` tarda 0,2–0,3 s |
| 2026-09-23 | — (observación) | Tiempos por fase dentro del proceso | ℹ️ | **Solo cargar Baileys tarda 1,4–1,9 s** y no está incluido en `t_total` (que empieza con los módulos ya cargados). En Android, si el motor arranca en frío cuando entra la llamada, ese tiempo se suma. Con `npx tsx` había unos 7 s más de arranque de herramientas; `npm start` ahora usa `node --import tsx` |
| 2026-09-23 | — (observación) | Logs de Baileys al conectar | ℹ️ | Aviso *"DANGER: DISABLING ALL SYNC…"* (ver pregunta abierta) y *"no name present, ignoring presence update request"*: Baileys no llega a enviar presencia porque la cuenta vinculada no tiene nombre. No afecta a la consulta |
| 2026-09-23 | — | Casos sin sesión: `status`, `check`, número inválido, `bench --runs 31` | ✅ Correcto | Mensajes claros, exit 1, no se crean `.auth/` ni `results/` |

## Bloqueos

Ninguno. Todos los criterios obligatorios pasaron.

