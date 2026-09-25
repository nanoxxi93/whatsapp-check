# Hoja de Tareas


- [x] **T1** — Entorno: instalar Go, `gomobile` (`gomobile init`) y el NDK. Verificar `gomobile bind -target=android/arm64` con un paquete de prueba. *(con permiso del autor)*
- [x] **T2** — Proyecto Android base: Gradle KTS, `engine-api` con `WhatsAppPresenceEngine` y `toCheckResult` + tests JUnit que repiten los casos de `result.test.ts`, y la `MainActivity`. · cubre `RF-01`, `RF-04`
- [x] **T3** — **F4 whatsmeow:** módulo Go (`Link`/`PairPhone`, `Check`/`IsOnWhatsApp`, `Unlink`, `Close`) con la configuración obligatoria de la sesión; `.aar` con `gomobile bind`; wrapper Kotlin. · cubre `RF-02`, `RF-04`, `RF-07`, `RNF-02`, `RNF-04`
- [x] **T4** — Bench en proceso `:engine` + medición de `tEngine`/`tReady`/`tQuery`, tamaño del APK y PSS; exportar JSONL. · cubre `RF-05`, `RF-06`, `RF-08`
- [x] ~~**T5**~~ *(reemplazada 2026-09-23: F1 descartado por análisis con la evidencia de CA-04 y CA-06, [IF-013](../product/intentos-fallidos.md#if-013--motor-baileys--nodejs-mobile-en-android); aprobado por el autor)* — **F1 Baileys:** `nodejs-mobile` + bundle de Baileys; si falla en Node 18, un intento de plan B; si falla, registrar en `intentos-fallidos.md`. · cubre `RF-03`, `RF-04`, `RNF-04`
- [x] **T6** — Mediciones en el teléfono del autor: vincular cada motor, bench de 20 corridas con wifi y 20 con datos móviles, tamaño del APK y memoria. *(requiere al autor)* · cubre `RNF-03`, `RNF-05`
  - *2026-09-23:* el Moto G53 no tiene saldo de datos; las mediciones con **datos móviles** (las que deciden) se hacen en el **Poco M5s** para **ambos motores**, para que la comparación sea en el mismo teléfono y la misma red. El Moto queda para las mediciones con wifi.
- [x] **T7** — Comparación y decisión: tabla en checklist.md; decisión 1 del roadmap a *Decisiones tomadas*; propuesta de cambios en tech-stack.

