# Requisitos — Comparar motores en Android

> **Estado:** Validado · **Fecha:** 2026-09-23
> **Ítem del roadmap:** Fase 0 — Comparar motores en Android (`L`)
> **Relacionado:** [plan.md](plan.md) · [checklist.md](checklist.md) · [roadmap](../product/roadmap.md) · [tech-stack §2](../product/tech-stack.md#motor-de-protocolo-en-el-dispositivo-)

## Objetivo

Elegir, con mediciones en un teléfono Android real, el motor que implementará `WhatsAppPresenceEngine` en la app. Cierra la **decisión abierta 1** del roadmap (qué motor usar) y aporta los datos de Android a la **decisión abierta 2** (conexión bajo demanda o persistente).

## Contexto

- En escritorio, Baileys 7 ya mostró que la idea funciona: vinculación por código ✅ y conexión en frío + consulta p95 2,36 s ([prototipo](../2026-09-23-prototipo-baileys-escritorio/checklist.md)). Pero **solo cargar Baileys tarda 1,4–1,9 s** y eso no entraba en esa medición: en Android el arranque del motor cuenta.
- Candidatos revisados el 2026-09-23:

| Motor | Estado | Riesgo principal |
|---|---|---|
| **F1. Baileys 7 + `nodejs-mobile`** | `nodejs-mobile` publicado solo hasta **Node 18.20.4** (oct. 2024); hay una rama sin publicar `update22-9-0`. Baileys 7 y 6.7 declaran `node >= 20`. Su parte nativa (`whatsapp-rust-bridge`) es WebAssembly, portable | Que Baileys no funcione en Node 18; tamaño del APK; arranque del runtime |
| ~~F2. Cobalt (JVM)~~ | Exige Java 25 y natives descargables | Descartado: [IF-011](../product/intentos-fallidos.md#if-011--motor-jvm-cobalt-en-android) |
| **F4. whatsmeow (Go) + `gomobile`** (nuevo) | Muy activo (último commit 2026-09-21), MPL-2.0. `PairPhone(ctx, phone, showPushNotification, clientType, clientDisplayName)` e `IsOnWhatsApp(ctx, phones)` | Puente Go ↔ Kotlin; almacenamiento de la sesión (SQLite); licencia MPL-2.0 |

- `whatsmeow` recibe un `clientDisplayName` al vincular: podría resolver que el teléfono muestre un nombre genérico ([IF-009](../product/intentos-fallidos.md#if-009--vincular-con-un-sistema-operativo-inventado-en-browser)).
- Entorno: Android Studio, Android SDK (plataformas 35/36) con emulador, JDK 21, Go 1.27.0, `gomobile`, NDK 30.0.16248370.

## Historias de usuario

- Como desarrollador, quiero una app de pruebas con los dos motores detrás de la misma interfaz, para medirlos en igualdad de condiciones en el mismo teléfono.
- Como desarrollador, quiero ver el arranque del motor, la conexión y la consulta por separado, para saber dónde se va el tiempo.

## Requisitos funcionales

| ID | Requisito | Prioridad |
|---|---|---|
| RF-01 | App Android de pruebas (`prototypes/android-engines/`) con una interfaz `WhatsAppPresenceEngine` como la de [tech-stack](../product/tech-stack.md#contrato-de-la-consulta-onwhatsapp-de-baileys) y un selector de motor. | Obligatorio |
| RF-02 | Implementación **F4** (whatsmeow vía `gomobile bind` → `.aar`). | Obligatorio |
| RF-03 | ~~Implementación **F1** (Baileys 7 en `nodejs-mobile`).~~ **Cambiado 2026-09-23 (aprobado por el autor):** F1 se descarta por análisis sin construirlo en Android ([IF-013](../product/intentos-fallidos.md#if-013--motor-baileys--nodejs-mobile-en-android)). Texto original: Si Baileys no arranca en Node 18, se documenta el error, se intenta **un** plan B acotado (empaquetar con esbuild para Node 18 o Baileys 6.7) y, si tampoco funciona, F1 queda como No-Go con evidencia. | Obligatorio |
| RF-04 | Por motor: vincular con código de emparejamiento, consultar un número y desvincular. La traducción a `HAS_WHATSAPP` / `NO_WHATSAPP` / `UNKNOWN` sigue el mismo contrato y los mismos casos de prueba que `prototypes/baileys-cli/src/result.ts`. | Obligatorio |
| RF-05 | Benchmark en el teléfono: N corridas de **arranque en frío del motor + conexión + consulta + cierre**, midiendo por separado `tEngine` (runtime + librería), `tReady` y `tQuery`, con mínimo, p50, p95 y máximo. | Obligatorio |
| RF-06 | Registrar tamaño del APK (solo `arm64-v8a`) con cada motor y sin ninguno, y memoria (PSS) durante una consulta. | Obligatorio |
| RF-07 | ~~Con F4, probar `clientDisplayName = "WhatsCheck"`~~ → resuelto por documentación: whatsmeow indica que el servidor solo acepta `Navegador (Sistema)` comunes (IF-009). Se anota qué muestra el teléfono con `"Chrome (Linux)"`. | Deseable |
| RF-08 | Exportar los resultados como JSONL con el mismo formato que `baileys-cli` (números enmascarados), para compararlos con los de escritorio. | Deseable |

## Requisitos no funcionales

| ID | Requisito | Métrica |
|---|---|---|
| RNF-01 | **Solo números secundarios.** | Todas las pruebas con el número secundario; ningún número real en el repo |
| RNF-02 | **Credenciales en el almacenamiento privado de la app** (no en almacenamiento compartido). | `context.filesDir`; excluido del backup (`android:allowBackup="false"` en la app de pruebas) |
| RNF-03 | **Condiciones comparables.** | Mismo teléfono, misma red; se anota tipo de red (wifi / datos móviles). Pausa ≥ 10 s entre corridas |
| RNF-04 | **Configuración obligatoria de la sesión** (tech-stack §2) en ambos motores. | Sin "en línea", sin historial, sin confirmaciones de lectura, mensajes descartados |
| RNF-05 | **Límite de dispositivos vinculados.** | El número secundario ya tiene 1 (el CLI de escritorio); las pruebas no deben dejar más de 3 en total |

## Principios de la misión que aplican

| Principio | Cómo se respeta en este feature |
|---|---|
| Lo del usuario se queda en su teléfono | Las credenciales viven en `filesDir` de la app; no hay backend (RNF-02). |
| Fallar en silencio | Mismo contrato de `UNKNOWN` en ambos motores (RF-04). |
| No altera el WhatsApp del usuario | Configuración obligatoria de la sesión en ambos motores (RNF-04). |
| Transparencia sobre el riesgo | La app de pruebas es solo para el autor y números secundarios; no se distribuye. |

## Fuera de alcance

- `CallScreeningService`, overlay y notificaciones: ítem **Spike de `CallScreeningService` + overlay**.
- Efectos en el WhatsApp principal: ítem propio (aquí solo se aplica la configuración).
- Cifrado de credenciales con Keystore, UI final, onboarding: Fase 1.
- Conexión persistente: se mide solo en frío; la persistente se evalúa en la prueba de resistencia al baneo.

## Dependencias

- **Un teléfono Android físico** del autor, con depuración USB. La latencia con datos móviles no se puede medir en el emulador.
- **Instalar Go, `gomobile` y el Android NDK** en la máquina de desarrollo (se pide permiso antes).
- Sesión del número secundario: se vincula una vez por motor, desde el teléfono de pruebas.

## Preguntas abiertas

- [x] ¿Qué teléfono Android se usa para las pruebas? → **Moto G53 (Android 14)** como principal, por tener un Android más cercano al de Google, y **Poco M5s (Android 13, MIUI)** para comprobar el comportamiento con la capa de Xiaomi. *(2026-09-23)*
- [x] Herramientas → el autor instaló Go 1.27.0, `gomobile`/`gobind`, NDK 30.0.16248370 y las herramientas de línea de comandos del SDK. *(2026-09-23)*
- [ ] *(Pasa al ítem "Revisión legal y de distribución")* Licencia MPL-2.0 de whatsmeow: permite una app cerrada si no se modifican sus archivos (y, si se modifican, hay que publicar esos archivos). Se revisa en el ítem **Revisión legal y de distribución**. — *informativo*
