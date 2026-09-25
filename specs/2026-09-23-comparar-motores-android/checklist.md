# Validación — Comparar motores en Android

> **Estado:** Validado · **Fecha:** 2026-09-23
> **Relacionado:** [spec.md](spec.md) · [plan.md](plan.md)

## Criterios de aceptación

| ID | Requisito | Criterio (verificable) | Método | Obligatorio |
|---|---|---|---|---|
| CA-01 | RF-01 | La app de pruebas compila (`./gradlew assembleDebug`) y se instala en el teléfono; permite elegir motor. | Build + manual | Sí |
| CA-02 | RF-04 | Los tests JUnit de `toCheckResult` cubren los mismos casos que `result.test.ts` (`exists: true`, `[]`, `exists: false`, `jid` distinto, varias entradas, respuesta nula, error, timeout, sesión cerrada) y pasan. | `./gradlew test` | Sí |
| CA-03 | RF-02, RF-04 | **F4:** vincula el número secundario con código, consulta el propio número (→ `HAS_WHATSAPP`) y `51000000001` (→ `NO_WHATSAPP`), y desvincula. | Manual en el teléfono | Sí |
| CA-04 | RF-03, RF-04 | **F1:** lo mismo que CA-03, **o** evidencia registrada en `intentos-fallidos.md` de por qué no corre (error textual, versión, plan B intentado). | Manual / registro | Sí |
| CA-05 | RF-05 | Por cada motor que funcione: bench de 20 corridas en frío con wifi y 20 con datos móviles, con `tEngine`, `tReady`, `tQuery` y total (mín., p50, p95, máx.). | Bench en el teléfono | Sí |
| CA-06 | RF-06 | Tamaño del APK `arm64-v8a` base, con F4 y con F1; PSS durante una consulta por motor. | `apkanalyzer` / `adb shell dumpsys meminfo` | Sí |
| CA-07 | RNF-02 | Las credenciales están en `filesDir` y `allowBackup="false"`. | Revisión de código + `adb shell run-as` | Sí |
| CA-08 | RNF-04 | En ambos motores, la sesión no marca "en línea", no sincroniza historial ni envía confirmaciones de lectura (revisión de configuración y código). | Revisión de código | Sí |
| CA-09 | RNF-05 | Al terminar, el número secundario no tiene más dispositivos vinculados que el CLI de escritorio y, como mucho, el motor elegido. | Manual en el teléfono | Sí |
| CA-10 | RF-07 | Nombre que muestra el teléfono con `clientDisplayName = "WhatsCheck"` en F4, anotado. | Manual | No |
| CA-11 | RF-08 | JSONL exportado con números enmascarados y el mismo formato que `baileys-cli`. | Revisión del archivo | No |

## Criterios Go / No-Go

Por motor, con el bench de **datos móviles** (el caso realista). Total = `tReady` + `tQuery`, donde `tReady` ya incluye `tEngine` (se miden desde el mismo inicio). `tProcess` (arranque del proceso de Android hasta que empieza el trabajo del motor) se informa aparte, porque también lo paga la app final. El peor criterio define el resultado del motor.

| Métrica | Go | Go con reservas | No-Go |
|---|---|---|---|
| Vinculación por código | Funciona | — | No funciona |
| Total en frío p95 | ≤ 3 s | 3–8 s (sirve con el aviso "Verificando…") | > 8 s |
| Corridas sin verificar (❔) | 0 de 20 | 1–2 de 20 | ≥ 3 de 20 |
| Tamaño agregado al APK | ≤ 25 MB | 25–60 MB | > 60 MB |

**Regla de elección:** entre los motores con mejor resultado, el de menor total p95. Si la diferencia es < 0,5 s, el de menor tamaño. Si ninguno es Go o Go con reservas, se reabre la decisión con F3 (implementación propia) o con la conexión persistente.

## Plan de pruebas

### Automáticas
- `./gradlew test`: `toCheckResult` en `engine-api` (casos de CA-02).
- `go test ./...` en el módulo Go, para las funciones puras que haya (normalización del resultado).

### Manuales
Con el **número secundario**, en el teléfono del autor:
1. Instalar la app de pruebas; elegir F4; vincular; consultar el propio número y `51000000001`; bench wifi + datos; desvincular (CA-03, CA-05, CA-10).
2. Lo mismo con F1 (CA-04, CA-05).
3. Tamaño del APK y memoria (CA-06).
4. Revisar *Dispositivos vinculados* al terminar (CA-09).

## Definición de terminado

- [x] Todos los criterios obligatorios pasaron y están en el registro.
- [x] `./gradlew test` y `go test` en verde.
- [x] Sin números completos ni credenciales en consola, git o archivos versionados.
- [x] Tareas de [plan.md](plan.md) marcadas.
- [x] Motor elegido; decisión 1 del roadmap en *Decisiones tomadas*; tech-stack actualizado (con aprobación); dato de Android sumado a la decisión 2.
- [x] Ítem marcado `[x]` en el roadmap.

## Registro de resultados

| Fecha | Criterio | Método / comando | Resultado | Evidencia / notas |
|---|---|---|---|---|
| 2026-09-23 | — (T1, entorno) | `gomobile bind -target=android/arm64 -androidapi 29` sobre un paquete mínimo | ✅ Funciona | `.aar` de 1,4 MB en 45 s; `jni/arm64-v8a/libgojni.so` de 3,4 MB (el costo fijo del runtime de Go, sin whatsmeow). Go 1.27.0, NDK 30.0.16248370 |
| 2026-09-23 | CA-02 | `./gradlew :engine-api:test` | ✅ Pasa | 11 tests, 0 fallos. Mismos casos que `result.test.ts`: `exists: true`, `[]`, `exists: false`, `jid` distinto, varias entradas, ambiguo, respuesta nula, error, timeout, sesión cerrada, `jidUser` |
| 2026-09-23 | CA-01 (compilación) | `./gradlew :app:assembleDebug` | ✅ Compila | Gradle 8.14.3, AGP 8.13.2, Kotlin 2.2.20, Compose BOM 2025.09.00, compileSdk 36, minSdk 29. APK debug sin motores: 25,2 MB (base sin minificar; los tamaños de CA-06 se comparan contra la misma base). Falta instalar en el teléfono y tener motores para elegir |
| 2026-09-23 | T3 (whatsmeow, sin teléfono) | `go vet`, `go test`, `GOOS=android GOARCH=arm64 CGO_ENABLED=0 go build`, `build-aar.sh`, `assembleDebug` | ✅ | 3 tests de Go (store sin sesión, reapertura, formato del nombre). whatsmeow `v0.0.0-20260921121126-35ae40906e74`, `modernc.org/sqlite` 1.59.0. `.aar` 6,7 MB; `libgojni.so` **17,2 MB** sin símbolos (35,5 MB con símbolos) |
| 2026-09-23 | CA-06 (parcial) | Tamaño del APK debug `arm64-v8a` | ℹ️ | Base 25,2 MB → con F4 **42,4 MB (+17,2 MB)**. Las `.so` van sin comprimir en el APK (minSdk ≥ 23); la descarga desde una tienda sería menor. Criterio ≤ 25 MB: cumple |
| 2026-09-23 | CA-08 (F4, revisión de código) | Revisión de `wacheck.go` y del código de whatsmeow | ✅ | Nunca se llama a `SendPresence` (sin "en línea"; recibos de entrega `inactive`), `ManualHistorySyncDownload = true` (sin historial), sin manejador de mensajes ni `MarkRead` |
| 2026-09-23 | CA-01 | `adb install` + `am start` en Moto G53 5G (Android 14, arm64) | ✅ Pasa | Instalada y abierta en 1,5 s, sin errores en logcat; muestra el motor `whatsmeow` para elegir |
| 2026-09-23 | CA-03 (vincular) | "Vincular" en la app con el número secundario | ✅ Pasa | Código aceptado en el teléfono; sesión `READY`. Observación del autor: el código no se podía copiar → ahora se copia solo al portapapeles y el texto es seleccionable (anotar en la spec de vinculación de la Fase 1) |
| 2026-09-23 | CA-03 (consultas) | `bench.sh whatsmeow` con 1 corrida en frío por número | ✅ Pasa | Número secundario propio → `HAS_WHATSAPP` (total 1,00 s: proceso 0,37 · motor 0,08 · sesión 0,84 · consulta 0,16). `51000000001` → `NO_WHATSAPP` (total 1,90 s). whatsmeow recibe el número tal cual, sin normalizar con libphonenumber. Falta desvincular al final |
| 2026-09-23 | CA-10 | Nombre en *Dispositivos vinculados* con whatsmeow (código `"Chrome (Linux)"`, `DeviceProps.Os = "WhatsCheck"`) | ✅ | El teléfono muestra **"Google Chrome (WhatsCheck)"** (confirmado por el autor). Con whatsmeow sí se puede mostrar un nombre propio (IF-009 actualizado) |
| 2026-09-23 | CA-05 (F4, wifi) | `bench.sh whatsmeow <secundario> 20 wifi` en Moto G53 | ✅ 20/20 | p50 / p95 / máx: proceso 0,37 / 0,40 / 0,82 s · motor 0,06 / 0,07 / 0,08 s · sesión lista 0,98 / 1,87 / 2,01 s · consulta 0,19 / 0,53 / 0,57 s · **total 1,15 / 2,31 / 2,49 s**. 0 sin verificar. PSS p50 77 MB. Veredicto con wifi: Go |
| 2026-09-23 | CA-04 (factibilidad de F1, en PC) | Baileys 7 con Node 18.20.8 oficial | ⚠️ Viable con ajuste | `import` OK. Sin WebCrypto global falla (IF-012); con `--experimental-global-webcrypto` conecta y hace el handshake. La consulta no se completó porque la sesión del CLI ya estaba cerrada (401): el autor había eliminado ese dispositivo desde el teléfono, no fue un cierre de WhatsApp. Falta probarlo dentro de `nodejs-mobile` en Android |
| 2026-09-23 | CA-05 (F4, **datos móviles**) | `bench.sh whatsmeow <secundario> 20 datos` en Poco M5s (Android 13, MIUI 14), 4G Bitel, sin wifi | ✅ 20/20 | p50 / p95 / máx: proceso 0,46 / 0,49 / 0,51 s · motor 0,04 / 0,05 / 0,05 s · sesión lista 1,16 / 1,80 / 1,82 s · consulta 0,30 / 0,40 / 0,41 s · **total 1,47 / 2,15 / 2,21 s**. 0 sin verificar. PSS p50 104 MB. MIUI no bloqueó los arranques desde adb |
| 2026-09-23 | **Go / No-Go F4** | Tabla de criterios | ✅ **Go** | Vinculación por código: funciona (Moto y Poco) · total p95 con datos **2,15 s** (≤ 3 s) · **0/20** sin verificar · **+17,2 MB** al APK (≤ 25 MB). Extra: `.so` alineada a 16 KB y nombre propio visible ("Google Chrome (WhatsCheck)") |
| 2026-09-23 | CA-04 | Evidencia de F1 en `intentos-fallidos.md` | ✅ Pasa (con evidencia) | F1 descartado por análisis con aprobación del autor: [IF-012](../product/intentos-fallidos.md#if-012--baileys-7-en-node-18-sin-webcrypto-global) (Node 18 + flag) e [IF-013](../product/intentos-fallidos.md#if-013--motor-baileys--nodejs-mobile-en-android) (tamaño, 16 KB, nombre, carga). Por eso CA-05 y CA-08 aplican solo a F4 |
| 2026-09-23 | **Elección** | Regla de elección | ✅ **whatsmeow (F4)** | Único motor Go en todos los criterios |
| 2026-09-23 | CA-06 (librerías nativas) | `llvm-readelf -l`, `llvm-strip`, `gzip -6` sobre las `.so` arm64 | ℹ️ | **F4 whatsmeow** `libgojni.so`: 17,2 MB (sin símbolos), **6,4 MB comprimida**, alineación LOAD **0x4000 (16 KB) ✅**. **F1 `nodejs-mobile` 18.20.4** `libnode.so`: 62,5 MB (49,5 MB sin símbolos), **15,4 MB comprimida**, alineación LOAD **0x1000 (4 KB) ❌**: no cumple el requisito de páginas de 16 KB de Google Play para apps que apuntan a Android 15+; el arreglo está en el repo (nov. 2025) pero sin release. F1 además debe sumar Baileys y sus dependencias |
| 2026-09-23 | CA-07 | Manifest + `adb run-as … ls files/whatsmeow` + `dumpsys package` en el Poco | ✅ Pasa | `allowBackup="false"` (sin flag `ALLOW_BACKUP`); sesión en `files/whatsmeow/whatsmeow.db` (+ `-wal`, `-shm`) con permisos `rw-------` del usuario de la app; nada en `/sdcard/Android/data` |
| 2026-09-23 | CA-03 (desvincular) | `am start … BenchActivity --es action unlink` en el Poco | ✅ Pasa | El motor respondió `UNLINKED` y borró `files/whatsmeow/` (quedó vacía). En el teléfono, el dispositivo pasó a **"cierre de sesión pendiente"**, el estado normal mientras WhatsApp aplica la desvinculación. Con esto CA-03 queda completo (vincular, consultar ✅/⚠️ y desvincular) |
| 2026-09-23 | CA-09 | *Dispositivos vinculados* del número secundario | ✅ Pasa | El autor desvinculó a mano el CLI de escritorio y el Moto; queda **solo la sesión del Poco**, que se conserva para los siguientes ítems de la Fase 0 |
| 2026-09-23 | CA-11 | Revisión de `results/android-*.jsonl` | ✅ Pasa | 22 líneas, todas con el número enmascarado (`+5192*****19` / `+5100****01`); mismo formato de tiempos que `baileys-cli` más `tProcess`, `engine`, `device` y `pssKb`. Nota: `bench.sh` sobrescribía el archivo local entre teléfonos (se perdió la copia local de las corridas del Moto, que están en esta tabla); ahora el nombre incluye el modelo |

## Bloqueos

Ninguno. Todos los criterios obligatorios pasaron.
