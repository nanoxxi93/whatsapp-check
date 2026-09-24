# Requisitos — Prototipo de escritorio con Baileys

> **Estado:** Validado · **Fecha:** 2026-09-23
> **Ítem del roadmap:** Fase 0 — Prototipo de escritorio con Baileys (`S`)
> **Relacionado:** [plan.md](plan.md) · [validation.md](validation.md) · [roadmap](../product/roadmap.md)

## Objetivo

Tener una herramienta de línea de comandos en Node que vincule un número **secundario** con código de emparejamiento, consulte si una lista de números tiene WhatsApp y **mida cuánto tarda una conexión en frío más una consulta**. Con eso se decide si la estrategia "conectar bajo demanda" de [tech-stack §3](../product/tech-stack.md#estrategia-de-conexión-) es viable, antes de invertir en el motor para Android.

## Contexto

- El comportamiento de `onWhatsApp()` ya está confirmado en un proyecto existente del autor ([tech-stack, contrato](../product/tech-stack.md#contrato-de-la-consulta-onwhatsapp-de-baileys)). Lo que **no** está validado es la vinculación por código de emparejamiento ([IF-006](../product/intentos-fallidos.md#if-006--vincular-whatsapp-con-código-qr)) ni la latencia de conectar desde cero en cada llamada.
- El proyecto existente del autor mantiene la sesión siempre conectada. Esta app se conectaría y desconectaría en cada llamada, así que la latencia de arranque es el dato que falta.
- Esta herramienta se reutilizará en los siguientes ítems de la Fase 0: **validación de la hipótesis** (consultar listas de números) y **prueba de resistencia al baneo** (uso diario realista). Por eso debe aceptar listas y dejar resultados en un formato procesable.
- Baileys vigente en npm: `7.0.0-rc14` (etiqueta `latest`, publicada el 2026-07-29; el paquete `baileys` y `@whiskeysockets/baileys` apuntan a la misma versión). La línea 6.x queda como `legacy` (`6.7.24`).

## Historias de usuario

- Como desarrollador, quiero vincular un número secundario escribiendo un código en *Dispositivos vinculados*, para comprobar que la vinculación sin QR funciona.
- Como desarrollador, quiero consultar uno o varios números y ver ✅/⚠️/❔ con los tiempos de cada fase, para saber si la verificación cabe en el tiempo que suena una llamada.
- Como desarrollador, quiero repetir N veces "conectar en frío + consultar" y obtener p50/p95, para decidir la estrategia de conexión con datos.

## Requisitos funcionales

| ID | Requisito | Prioridad |
|---|---|---|
| RF-01 | `link <número>`: normaliza el número a E.164, pide confirmación de que es un **número secundario**, solicita el código de emparejamiento y lo muestra junto con los pasos (*WhatsApp → Dispositivos vinculados → Vincular con número de teléfono*). Termina cuando la sesión queda abierta. | Obligatorio |
| RF-02 | Las credenciales se guardan en una carpeta local ignorada por git y se reutilizan en los siguientes comandos sin volver a vincular. | Obligatorio |
| RF-03 | `check <número…>` y `check --file <archivo>`: conecta en frío, consulta cada número con `onWhatsApp()` y desconecta. | Obligatorio |
| RF-04 | La respuesta se traduce a `HAS_WHATSAPP` / `NO_WHATSAPP` / `UNKNOWN` según el contrato de [tech-stack](../product/tech-stack.md#contrato-de-la-consulta-onwhatsapp-de-baileys): `exists: true` → `HAS_WHATSAPP`; `[]` o `exists: false` con sesión abierta y sin error → `NO_WHATSAPP`; cualquier error, timeout o sesión cerrada → `UNKNOWN`. | Obligatorio |
| RF-05 | La coincidencia se hace por el `jid` devuelto. Si el `jid` no coincide exactamente con el número enviado (p. ej. normalización de Brasil o México), se acepta si es la única entrada y se marca `jidMismatch: true` en el resultado. | Obligatorio |
| RF-06 | Mide por separado: tiempo hasta abrir el socket, hasta que la sesión está lista (`connection: open`), de cada consulta y total. | Obligatorio |
| RF-07 | `bench <número> --runs N`: repite N veces "conexión en frío + consulta + desconexión" y reporta mínimo, p50, p95 y máximo de cada fase. | Obligatorio |
| RF-08 | La sesión se abre con la configuración obligatoria de [tech-stack §2](../product/tech-stack.md#configuración-obligatoria-de-la-sesión): sin marcarse "en línea", sin sincronizar historial, sin confirmaciones de lectura y descartando los mensajes entrantes sin guardarlos. | Obligatorio |
| RF-09 | Detecta y reporta con un mensaje claro cuando la sesión fue cerrada desde el teléfono o la cuenta fue restringida (motivo de desconexión de Baileys), y en ese caso no sigue consultando. | Obligatorio |
| RF-10 | `status`: indica si hay sesión guardada y para qué número (enmascarado). `unlink`: cierra la sesión en WhatsApp y borra las credenciales locales. | Obligatorio |
| RF-11 | Los resultados se muestran en una tabla en consola y se agregan a un archivo JSONL (una línea por consulta, con fecha, resultado y tiempos). | Obligatorio |

## Requisitos no funcionales

| ID | Requisito | Métrica |
|---|---|---|
| RNF-01 | **Protección de la cuenta de prueba:** en consultas por lote, pausa aleatoria entre números y tope por ejecución. | Pausa de 3–6 s entre consultas; máximo 50 números por ejecución (configurable con confirmación). `bench` con máximo 30 corridas y pausa ≥ 10 s entre conexiones. |
| RNF-02 | **Privacidad:** los números nunca aparecen completos en los logs de consola ni en los logs de Baileys. | Enmascarados como `+5198*****88` |
| RNF-03 | **Credenciales y resultados fuera del repositorio.** | `.auth/` y `results/` en `.gitignore`; `git status` limpio después de usar la herramienta |
| RNF-04 | **Reproducible.** | Versión exacta de Baileys fijada en `package.json` (sin `^`) y `package-lock.json` versionado |
| RNF-05 | **Timeouts acotados** para que un fallo no cuelgue la herramienta. | Conexión: 15 s · consulta: 5 s |
| RNF-06 | Lógica de traducción del resultado cubierta por tests automáticos. | 100 % de los casos del contrato (RF-04, RF-05) |

## Principios de la misión que aplican

| Principio | Cómo se respeta en este feature |
|---|---|
| Fallar en silencio | Error, timeout o sesión caída → `UNKNOWN`; nunca `NO_WHATSAPP` (RF-04). |
| Lo del usuario se queda en su teléfono | Aquí "su teléfono" es la máquina local: credenciales solo en `.auth/`, nunca en git (RNF-03). |
| Transparencia sobre el riesgo | `link` exige confirmar que es un número secundario (RF-01). |
| No altera el WhatsApp del usuario | Configuración obligatoria de la sesión (RF-08). |

## Fuera de alcance

- Analizar si los números de spam tienen WhatsApp: ítem **Validación de la hipótesis** (usará esta herramienta).
- Comprobar efectos en el WhatsApp principal (aparecer en línea, notificaciones): ítem **Efectos secundarios en el WhatsApp principal**. Aquí solo se configura la sesión para evitarlos.
- Uso sostenido durante semanas: ítem **Prueba de resistencia al baneo**.
- Cualquier código de Android: ítem **Comparar motores en Android**.
- Conexión persistente: solo se mide la conexión en frío. La persistente se compara en la prueba de baneos.

## Dependencias

- Node ≥ 20 (en la máquina de desarrollo: Node 24.19.0, npm 12.0.2).
- **Un número secundario con WhatsApp instalado en un teléfono**, disponible para vincular. Lo aporta el autor.
- Conexión a internet estable. Anotar el tipo de red en cada medición; la latencia de escritorio no equivale a la de un teléfono con datos móviles.

## Preguntas abiertas

- [x] ¿Qué versión de Baileys usa el proyecto existente del autor? → `@whiskeysockets/baileys` `^7.0.0-rc14`. Aquí se usa el mismo paquete fijado en `7.0.0-rc14` exacto (RNF-04). *(2026-09-23)*
- [x] ¿Baileys acepta un nombre de dispositivo propio ("WhatsCheck") con código de emparejamiento? → Vincula, pero con código de emparejamiento el teléfono no muestra ese nombre ("Google Chrome (Mac OS)"). Con QR sí se muestra (confirmado en otro proyecto del autor). Se investiga en la spec de vinculación de la Fase 1. Ver [IF-009](../product/intentos-fallidos.md#if-009--vincular-con-un-sistema-operativo-inventado-en-browser). *(2026-09-23)*
- [x] **Sincronización mínima de historial.** Baileys 7 advierte al conectar: *"DISABLING ALL SYNC BY shouldSyncHistoryMsg PREVENTS BAILEYS FROM ACCESSING INITIAL LID MAPPINGS, LEADING TO INSTABILITY AND SESSION ERRORS"*. RF-08 pide no sincronizar historial, pero sin la sincronización inicial Baileys no recibe las equivalencias número ↔ LID de los contactos. `onWhatsApp` no las usa (consulta por número con USync), así que por ahora **se mantiene desactivada** y se observa si aparecen errores de sesión. Si aparecen, evaluar permitir solo los tipos que traen esas equivalencias, sin guardar mensajes. → **Se mantiene desactivada:** en ~30 conexiones no hubo errores de sesión atribuibles. Se vuelve a evaluar con el motor de Android, donde la sesión vivirá más tiempo. *(2026-09-23)*
