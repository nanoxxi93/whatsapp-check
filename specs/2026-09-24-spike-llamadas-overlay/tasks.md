# Hoja de Tareas


- [x] **T1** — Manifest, `ScreeningService` (respuesta inmediata + lanzar la verificación) y pedido del rol en `MainActivity`. · cubre `RF-01`, `RF-02`
- [x] **T2** — `CallCheck`: normalización, consulta con el motor, tiempos y `calls.jsonl` enmascarado. · cubre `RF-03`, `RF-06`, `RNF-04`
- [x] **T3** — `OverlayController` (Verificando → resultado, arrastrable, toque, 45 s) y el botón "Mostrar aviso de ejemplo". · cubre `RF-04`, `RF-07`
- [x] **T4** — Camino sin overlay: espera ≤ 4 s y notificación heads-up. · cubre `RF-05`
- [x] **T4b** — `KeepAliveService` + `BootReceiver` + pedido de exclusión de batería; `Journal` con `health.jsonl`, resumen en pantalla y "Exportar". · cubre `RF-08`, `RF-09`, `RF-10`
- [x] **T5** — Instalar en el Poco (y el Moto); conceder rol y permisos, incluidos los permisos propios de MIUI; guía corta en el README y en la pantalla. · cubre `RF-07`, `RF-08`
- [x] **T6** — **Semana de uso real** (2026-09-24 a 2026-10-06): 49 llamadas reales de desconocidos recibidas en el Poco; 39 en pantalla bloqueada (overlay 100% visible); exportación generada desde la app. · cubre `RNF-01`, `RNF-02`, `RNF-03`, `RNF-05`
- [x] **T7** — Analizar lo exportado: latencias p50=2,04 s / p95=3,32 s; 0 baneos / 0 desvinculaciones; ajuste de timeout de overlay (4 s) y chequeo de red previa (`isOnline`) para evitar demoras en fallos de DNS; app lista para continuar la prueba de baneo prolongada.

