# Changelog

Registro de cambios de WhatsCheck. Formato basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/); el proyecto usará [Versionado Semántico](https://semver.org/lang/es/) desde la primera versión publicada.

Mientras no haya versiones, todo va en **Sin publicar**, agrupado por fecha (la más reciente arriba).

Categorías: **Añadido** (algo nuevo) · **Cambiado** (algo existente se comporta distinto) · **Obsoleto** (se eliminará pronto) · **Eliminado** · **Corregido** (bugs) · **Seguridad**.

## [Sin publicar]

### 2026-10-06

#### Cambiado
- Configuración de entorno tras actualizar a Android Studio 2026.2.1: exclusión de `.idea/` y `*.iml` en `.gitignore` raíz, y configuración de JDK 21 (Temurin) para compilaciones por terminal y compatibilidad con Gradle ([IF-015](specs/intentos-fallidos.md#if-015--ejecutar-gradle-814-con-jbr-25-de-android-studio-20262)).

### 2026-09-24

#### Añadido
- Aviso inmediato si la sesión se cae o WhatsApp restringe la cuenta: notificación de alta prioridad, una sola vez por problema (detectado en la siguiente llamada o consulta).
- Diseño de la app de pruebas (v0.2, [spec](specs/2026-09-24-ui-app-pruebas/)): vistas Inicio / Registro / Ajustes con barra inferior, número vinculado (enmascarado, con "Mostrar"), consulta manual, vinculación guiada, ícono propio (auricular con insignia de verificado, sin imitar a WhatsApp) y tema claro/oscuro.
- Spike de llamadas en la app de pruebas: filtrado de llamadas de desconocidos (`CallScreeningService`, deja sonar al instante), aviso flotante ✅/⚠️/❔ arrastrable, notificación si no hay permiso de overlay, modo "siempre activo" con notificación fija, arranque al encender, guía de MIUI, y registro de llamadas y de salud con resumen y exportación desde el teléfono (sin PC).
- Spec del spike de llamadas y aviso flotante ([specs/2026-09-24-spike-llamadas-overlay](specs/2026-09-24-spike-llamadas-overlay/)).
- Ítem del roadmap **"Efectos secundarios en el WhatsApp principal" validado y cerrado**.

#### Cambiado
- Roadmap: el spike de llamadas va antes de la prueba de resistencia al baneo, que pasa a ser de uso real con el único número secundario.
- `tech-stack.md` §2: "no marcarse en línea" verificado en dispositivo y nuevo requisito de minimización de datos.
- Motor whatsmeow: **no guarda la agenda, los ajustes de chats ni los secretos de mensajes** del usuario (stores vacíos y limpieza al abrir); solo conserva lo que el protocolo necesita.

#### Corregido
- La app de pruebas mostraba la sesión como `UNLINKED` aunque estuviera vinculada: el motor se crea bajo demanda y la pantalla no leía el estado real.

### 2026-09-23

#### Añadido
- Constitución del producto en `specs/`: `mission.md`, `tech-stack.md` y `roadmap.md`.
- Contrato de `onWhatsApp` de Baileys y su traducción a `CheckResult` en `tech-stack.md`.
- Skill `/roadmap-spec` en `.claude/skills/roadmap-spec/` para convertir cada ítem del roadmap en una spec (`specs/AAAA-MM-DD-<nombre>/` con `spec.md`, `plan.md` y `checklist.md`).
- `CHANGELOG.md` (este archivo) y `specs/intentos-fallidos.md` para registrar lo que se probó y no funcionó.
- `CLAUDE.md` con las reglas para mantener ambos registros al día.
- Spec del prototipo de escritorio con Baileys ([specs/2026-09-23-prototipo-baileys-escritorio](specs/2026-09-23-prototipo-baileys-escritorio/)).
- Prototipo `prototypes/baileys-cli/` (Node + TypeScript, `@whiskeysockets/baileys` 7.0.0-rc14): comandos `link` (código de emparejamiento), `check`, `bench`, `status` y `unlink`; traducción de `onWhatsApp` a ✅/⚠️/❔ con tests; medición de latencia en frío con veredicto Go/No-Go; resultados en JSONL con números enmascarados. Pendiente la validación con un número secundario real.
- Spec de la comparación de motores en Android ([specs/2026-09-23-comparar-motores-android](specs/2026-09-23-comparar-motores-android/)): Baileys + `nodejs-mobile` (F1) contra whatsmeow + `gomobile` (F4, candidato nuevo).
- App Android de pruebas `prototypes/android-engines/` (Gradle 8.14.3, AGP 8.13.2, Kotlin 2.2.20, Compose): módulo `engine-api` con `WhatsAppPresenceEngine` y el contrato `toCheckResult` portado a Kotlin (11 tests), y pantalla para vincular, consultar y desvincular.
- Motor **whatsmeow (F4)** en la app de pruebas: módulo Go `wacheck` (vincular por código, `IsOnWhatsApp`, cerrar, desvincular) compilado con `gomobile` (`engine-whatsmeow/build-aar.sh`), con 3 tests de Go; agrega 17,2 MB al APK.
- Spec de los efectos secundarios en el WhatsApp principal ([specs/2026-09-23-efectos-whatsapp-principal](specs/2026-09-23-efectos-whatsapp-principal/)): observación con WhatsApp Web ("en línea", checks, notificaciones, no leídos) e inspección de la base local.
- Efectos en el WhatsApp principal verificados con WhatsApp real: con whatsmeow conectado (3 min y 5 conexiones cortas) el número **no aparece "en línea"**, los mensajes quedan en **doble check gris**, **llegan las notificaciones** y siguen **sin leer**; el texto de los mensajes **no queda en la base** ([validación](specs/2026-09-23-efectos-whatsapp-principal/checklist.md)).
- Acciones de prueba `hold` y `stats` en la app de pruebas.
- Entrada IF-014: para observar efectos hacen falta un número sin otros clientes activos, un observador con su "última vez" visible y un control positivo.
- Ítem del roadmap **"Comparar motores en Android" validado y cerrado**: motor elegido whatsmeow ([validación](specs/2026-09-23-comparar-motores-android/checklist.md)).
- Mediciones en Android con whatsmeow: p95 en frío **2,31 s** con wifi (Moto G53) y **2,15 s** con 4G (Poco M5s), 0/40 sin verificar. `bench.sh` y `BenchActivity` para medir en frío desde la PC.
- Entradas IF-012 (Baileys 7 en Node 18 necesita WebCrypto global) e IF-013 (Baileys + `nodejs-mobile` descartado: tamaño y páginas de 16 KB).
- Entrada IF-011 en `intentos-fallidos.md`: Cobalt (F2) descartado por exigir Java 25 y binarios nativos descargables.
- Ítem del roadmap **"Prototipo de escritorio con Baileys" validado y cerrado** (todos los criterios obligatorios pasaron).
- Resultados del prototipo con número secundario: vinculación por código ✅; conexión en frío + consulta **p95 2,36 s** en 20 corridas, sin fallos → **Go "bajo demanda"** en escritorio ([validación](specs/2026-09-23-prototipo-baileys-escritorio/checklist.md)).
- Entrada IF-010 en `intentos-fallidos.md` (cierre del WebSocket de Baileys antes de salir).
- IF-009 con causa documentada: con código de emparejamiento WhatsApp solo acepta nombres `Navegador (Sistema)` comunes.
- Entradas IF-008 (credenciales a medias tras una vinculación fallida) e IF-009 (sistema operativo inventado en la identidad del dispositivo) en `intentos-fallidos.md`.
- Entrada IF-007 en `intentos-fallidos.md`: libphonenumber quita el `0` después del código de país (`51000000001` → `5100000001`).

#### Eliminado
- Ítems del roadmap "Validación de la hipótesis", "Feedback de precisión", "Señales combinadas" y "Reputación colaborativa": descartados por el cambio de enfoque (la app no clasifica llamadas como spam). La spec escrita se conserva como descartada ([specs/2026-09-23-validacion-hipotesis](specs/2026-09-23-validacion-hipotesis/)).

#### Corregido
- Fechas: los registros usaban a veces la fecha UTC (2026-09-24) en lugar de la local (`date +%F`, 2026-09-23). Se unificaron, y `baileys-cli` nombra `results/AAAA-MM-DD.jsonl` con la fecha local.
- `baileys-cli`: tras cerrar la sesión desde el teléfono (401) se borran solas las credenciales locales; antes había que ejecutar `unlink` antes de volver a vincular.
- `baileys-cli`: ya no aborta ocasionalmente al salir en Windows (`Assertion failed … src\win\async.c`); se espera a que Baileys cierre el WebSocket (IF-010).
- `baileys-cli link`: un intento de vinculación sin terminar dejaba credenciales a medias que provocaban 401 en cada reintento. Ahora se borran, y una sesión solo cuenta como vinculada si el emparejamiento se completó (IF-008).
- `baileys-cli link`: la identidad por defecto pasa a ser la de Baileys, "Chrome (Mac OS)", porque con la propia "Chrome (WhatsCheck)" el teléfono rechazó el código. Nuevo `--browser` para comparar identidades, y Ctrl+C durante la vinculación borra las credenciales incompletas.
- Los logs de Baileys usan hora ISO; antes el timestamp numérico se enmascaraba como si fuera un teléfono.

#### Cambiado
- `.gitattributes`: excepción `*.bat text eol=crlf` (p. ej. `gradlew.bat`), porque `cmd.exe` puede fallar con scripts `.bat` en LF. Todo lo demás sigue en LF.
- **Motor elegido: whatsmeow (Go)** (`tech-stack.md` §2/§4, decisión en el roadmap). El dispositivo vinculado aparece como "Google Chrome (WhatsCheck)".
- `tech-stack.md`: candidatos de motor F1 (Baileys + `nodejs-mobile`) y F4 (whatsmeow + `gomobile`, nuevo); F2 (Cobalt) descartado.
- **Enfoque del producto** (`mission.md` v0.3): la app informa si el número tiene WhatsApp y **no detecta spam**. Se quita la métrica "precisión de la hipótesis" y "Marcar como spam" pasa a "Marcar como no deseado" (lista negra personal).
- `tech-stack.md` §2: en lugar de exigir un nombre de dispositivo propio, el onboarding debe decirle al usuario con qué nombre aparecerá; el nombre propio se investiga en la Fase 1 (IF-009).
- `baileys-cli`: la identidad `--browser whatscheck` pasa a ser "Google Chrome (WhatsCheck)", con el mismo formato que la de otro proyecto del autor que sí vincula.
- `baileys-cli`: `npm start` usa `node --import tsx` en lugar de `tsx`, con menos tiempo de arranque.
- `tech-stack.md`: los prototipos de la Fase 0 viven en `prototypes/` y no forman parte de la app.
- La verificación pasa a hacerse con el WhatsApp del propio usuario vinculado como dispositivo, dentro del teléfono. Se elimina el backend del MVP y se descarta iOS.
- Finales de línea fijados en LF para todo el repositorio con `.gitattributes`.
