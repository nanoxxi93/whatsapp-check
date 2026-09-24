# Plan — Comparar motores en Android

> **Estado:** Validado · **Fecha:** 2026-09-23
> **Relacionado:** [requirements.md](requirements.md) · [validation.md](validation.md) · [tech-stack](../product/tech-stack.md)

## Enfoque

Una **app de pruebas mínima** (no es la app final) con dos implementaciones de `WhatsAppPresenceEngine`. Se empieza por **F4 (whatsmeow)** porque no tiene el bloqueo de versión de Node; después se hace **F1 (Baileys)**, con un plan B acotado si Baileys no corre en Node 18. Las mediciones se hacen en un teléfono real y se comparan con los criterios de [validation.md](validation.md#criterios-go--no-go).

**Hipótesis:** whatsmeow (binario Go nativo) arranca mucho más rápido que Baileys (runtime de Node + carga de módulos JS) y agrega menos MB al APK. Con eso, "bajo demanda" sería viable en Android.

## Diseño

```
prototypes/android-engines/
├── settings.gradle.kts / build.gradle.kts / gradle/libs.versions.toml
├── app/                         # Activity única en Compose: motor, vincular, consultar, bench, exportar
│   └── src/main/.../MainActivity.kt
├── engine-api/                  # interfaz WhatsAppPresenceEngine, CheckResult, SessionState, toCheckResult (puerto de result.ts)
├── engine-whatsmeow/            # wrapper Kotlin del .aar generado por gomobile
│   └── go/                      # módulo Go: API mínima exportada a Kotlin (Link, Check, Unlink, Close)
└── engine-baileys/              # nodejs-mobile + bundle de Baileys; puente por mensajes (JSON) Kotlin ↔ Node
    └── nodejs-project/
```

| Módulo | Cambio |
|---|---|
| `engine-api` | Interfaz de [tech-stack](../product/tech-stack.md#contrato-de-la-consulta-onwhatsapp-de-baileys) + `toCheckResult` en Kotlin, con los **mismos casos de prueba** que `result.test.ts` |
| `engine-whatsmeow/go` | `package wacheck`: funciones simples (strings y errores) para que `gomobile bind` las exponga. Sesión en SQLite (`modernc.org/sqlite`, Go puro, sin cgo) dentro de `filesDir` |
| `engine-baileys` | `nodejs-mobile` (AAR de la release 18.20.4) + Baileys 7 empaquetado con esbuild; mismas opciones de sesión que `baileys-cli/src/session.ts` |
| `app` | Pantalla de pruebas y bench; exporta JSONL con números enmascarados |

Medición de `tEngine`:
- **F4:** desde la primera llamada al `.aar` hasta que el cliente Go está creado con la sesión cargada.
- **F1:** desde `startNodeWithArguments` hasta que el script Node avisa que cargó Baileys.

Para la corrida en frío, `bench.sh` (en la PC) hace `am force-stop` de la app y lanza `BenchActivity` con `adb`. Cada corrida arranca un proceso nuevo, igual que cuando entre una llamada con la app cerrada, y el resultado queda en `files/results/*.jsonl` del teléfono. *(Cambio respecto del diseño inicial: más simple que un proceso `:engine` separado y mide lo mismo; además suma `tProcess`.)*

## Decisiones

| Decisión | Alternativas consideradas | Motivo |
|---|---|---|
| Agregar **whatsmeow (F4)** como candidato | Solo F1 vs F2 | F2 descartado ([IF-011](../product/intentos-fallidos.md#if-011--motor-jvm-cobalt-en-android)); whatsmeow es maduro, activo y tiene `PairPhone` e `IsOnWhatsApp` |
| F4 primero | F1 primero | F1 tiene un bloqueo conocido (Node 18 vs `node >= 20`); empezar por F4 da un resultado útil antes |
| `modernc.org/sqlite` para la sesión de whatsmeow | `mattn/go-sqlite3` | El segundo requiere cgo y complica compilar para Android |
| Plan B de F1 limitado a **un** intento | Compilar `nodejs-mobile` desde la rama `update22-9-0` | Compilar Node para Android es un proyecto en sí mismo; si hace falta, F1 queda No-Go y se registra |
| Motor en un proceso `:engine` aparte | Mismo proceso que la UI | Permite medir el arranque en frío de verdad y se parece a la app final, donde el motor corre en un servicio |
| App de pruebas en `prototypes/` | Directamente en la app final | Es descartable; la app final se arma en la Fase 1 con el motor elegido |
| El wrapper Kotlin de whatsmeow vive en el módulo `app`, no en un módulo `engine-whatsmeow` de Android | Módulo de librería propio | AGP no admite dependencias `.aar` locales dentro de un módulo de librería; `engine-whatsmeow/` queda solo con el código Go y `build-aar.sh` |
| `libgojni.so` sin símbolos (`-trimpath -ldflags="-s -w"`) | Build por defecto | Pasa de 35,5 MB a 17,2 MB; es lo que se usaría en producción |
| Nombre al vincular `"Chrome (Linux)"` | `"WhatsCheck"` | WhatsApp solo acepta `Navegador (Sistema)` comunes con código de emparejamiento ([IF-009](../product/intentos-fallidos.md#if-009--vincular-con-un-sistema-operativo-inventado-en-browser)) |

## Tareas

- [x] **T1** — Entorno: instalar Go, `gomobile` (`gomobile init`) y el NDK. Verificar `gomobile bind -target=android/arm64` con un paquete de prueba. *(con permiso del autor)*
- [x] **T2** — Proyecto Android base: Gradle KTS, `engine-api` con `WhatsAppPresenceEngine` y `toCheckResult` + tests JUnit que repiten los casos de `result.test.ts`, y la `MainActivity`. · cubre `RF-01`, `RF-04`
- [x] **T3** — **F4 whatsmeow:** módulo Go (`Link`/`PairPhone`, `Check`/`IsOnWhatsApp`, `Unlink`, `Close`) con la configuración obligatoria de la sesión; `.aar` con `gomobile bind`; wrapper Kotlin. · cubre `RF-02`, `RF-04`, `RF-07`, `RNF-02`, `RNF-04`
- [x] **T4** — Bench en proceso `:engine` + medición de `tEngine`/`tReady`/`tQuery`, tamaño del APK y PSS; exportar JSONL. · cubre `RF-05`, `RF-06`, `RF-08`
- [x] ~~**T5**~~ *(reemplazada 2026-09-23: F1 descartado por análisis con la evidencia de CA-04 y CA-06, [IF-013](../product/intentos-fallidos.md#if-013--motor-baileys--nodejs-mobile-en-android); aprobado por el autor)* — **F1 Baileys:** `nodejs-mobile` + bundle de Baileys; si falla en Node 18, un intento de plan B; si falla, registrar en `intentos-fallidos.md`. · cubre `RF-03`, `RF-04`, `RNF-04`
- [x] **T6** — Mediciones en el teléfono del autor: vincular cada motor, bench de 20 corridas con wifi y 20 con datos móviles, tamaño del APK y memoria. *(requiere al autor)* · cubre `RNF-03`, `RNF-05`
  - *2026-09-23:* el Moto G53 no tiene saldo de datos; las mediciones con **datos móviles** (las que deciden) se hacen en el **Poco M5s** para **ambos motores**, para que la comparación sea en el mismo teléfono y la misma red. El Moto queda para las mediciones con wifi.
- [x] **T7** — Comparación y decisión: tabla en validation.md; decisión 1 del roadmap a *Decisiones tomadas*; propuesta de cambios en tech-stack.

## Riesgos

| Riesgo | Mitigación |
|---|---|
| Baileys no corre en Node 18 | Plan B acotado; si no, F1 No-Go con evidencia. F4 no depende de esto |
| `gomobile` no soporta bien algún tipo (slices, structs) | API Go mínima con strings/JSON; solo tipos simples cruzan el puente |
| whatsmeow no permite desactivar algo de la configuración obligatoria | Revisar sus opciones (`EmitAppStateEventsOnFullSync`, presencia, historial); si algo no se puede, documentarlo y pesarlo en la decisión |
| Cada motor ocupa un dispositivo vinculado del número secundario | Desvincular al terminar cada motor (RNF-05) |
| Los fabricantes matan procesos en segundo plano | Irrelevante aquí (la app está en primer plano); se aborda en la Fase 1 |

## Cambios a documentos base

- `tech-stack.md` §2, tabla *Motor de protocolo*: marcar F2 como descartado (IF-011) y agregar **F4. whatsmeow (Go) + gomobile**. **Aprobado y aplicado (2026-09-23).**
- `tech-stack.md` §4 *App Android*: la fila "Motor WhatsApp" pasa a mencionar F1/F4 en lugar de "librería JVM". **Aprobado y aplicado (2026-09-23).**
- **Aprobado y aplicado (2026-09-23):** `tech-stack.md` §2, requisito del nombre del dispositivo → *"El dispositivo se muestra como 'Google Chrome (WhatsCheck)': con whatsmeow el nombre visible sale del sistema operativo del registro y el nombre del código de emparejamiento usa un valor común válido ('Chrome (Linux)')"*. Motivo: CA-10 e IF-009.
- **Aplicado (2026-09-23):** la decisión 1 del roadmap pasa a *Decisiones tomadas* (motor = whatsmeow); tech-stack §2 y §4 marcan F4 como elegido y F1 como descartado.
