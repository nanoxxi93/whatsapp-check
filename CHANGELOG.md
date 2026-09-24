# Changelog

Registro de cambios de WhatsCheck. Formato basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/); el proyecto usará [Versionado Semántico](https://semver.org/lang/es/) desde la primera versión publicada.

Mientras no haya versiones, todo va en **Sin publicar**, agrupado por fecha (la más reciente arriba).

Categorías: **Añadido** (algo nuevo) · **Cambiado** (algo existente se comporta distinto) · **Obsoleto** (se eliminará pronto) · **Eliminado** · **Corregido** (bugs) · **Seguridad**.

## [Sin publicar]

### 2026-09-23

#### Añadido
- Constitución del producto en `specs/product/`: `mission.md`, `tech-stack.md` y `roadmap.md`.
- Contrato de `onWhatsApp` de Baileys y su traducción a `CheckResult` en `tech-stack.md`.
- Skill `/roadmap-spec` en `.claude/skills/roadmap-spec/` para convertir cada ítem del roadmap en una spec (`specs/AAAA-MM-DD-<nombre>/` con `requirements.md`, `plan.md` y `validation.md`).
- `CHANGELOG.md` (este archivo) y `specs/product/intentos-fallidos.md` para registrar lo que se probó y no funcionó.
- `CLAUDE.md` con las reglas para mantener ambos registros al día.
- Spec del prototipo de escritorio con Baileys ([specs/2026-09-23-prototipo-baileys-escritorio](specs/2026-09-23-prototipo-baileys-escritorio/)).
- Prototipo `prototypes/baileys-cli/` (Node + TypeScript, `@whiskeysockets/baileys` 7.0.0-rc14): comandos `link` (código de emparejamiento), `check`, `bench`, `status` y `unlink`; traducción de `onWhatsApp` a ✅/⚠️/❔ con tests; medición de latencia en frío con veredicto Go/No-Go; resultados en JSONL con números enmascarados. Pendiente la validación con un número secundario real.
- Ítem del roadmap **"Prototipo de escritorio con Baileys" validado y cerrado** (todos los criterios obligatorios pasaron).
- Resultados del prototipo con número secundario: vinculación por código ✅; conexión en frío + consulta **p95 2,36 s** en 20 corridas, sin fallos → **Go "bajo demanda"** en escritorio ([validación](specs/2026-09-23-prototipo-baileys-escritorio/validation.md)).
- Entrada IF-010 en `intentos-fallidos.md` (cierre del WebSocket de Baileys antes de salir).
- Entradas IF-008 (credenciales a medias tras una vinculación fallida) e IF-009 (sistema operativo inventado en la identidad del dispositivo) en `intentos-fallidos.md`.
- Entrada IF-007 en `intentos-fallidos.md`: libphonenumber quita el `0` después del código de país (`51000000001` → `5100000001`).

#### Corregido
- `baileys-cli`: tras cerrar la sesión desde el teléfono (401) se borran solas las credenciales locales; antes había que ejecutar `unlink` antes de volver a vincular.
- `baileys-cli`: ya no aborta ocasionalmente al salir en Windows (`Assertion failed … src\win\async.c`); se espera a que Baileys cierre el WebSocket (IF-010).
- `baileys-cli link`: un intento de vinculación sin terminar dejaba credenciales a medias que provocaban 401 en cada reintento. Ahora se borran, y una sesión solo cuenta como vinculada si el emparejamiento se completó (IF-008).
- `baileys-cli link`: la identidad por defecto pasa a ser la de Baileys, "Chrome (Mac OS)", porque con la propia "Chrome (WhatsCheck)" el teléfono rechazó el código. Nuevo `--browser` para comparar identidades, y Ctrl+C durante la vinculación borra las credenciales incompletas.
- Los logs de Baileys usan hora ISO; antes el timestamp numérico se enmascaraba como si fuera un teléfono.

#### Cambiado
- `tech-stack.md` §2: en lugar de exigir un nombre de dispositivo propio, el onboarding debe decirle al usuario con qué nombre aparecerá; el nombre propio se investiga en la Fase 1 (IF-009).
- `baileys-cli`: la identidad `--browser whatscheck` pasa a ser "Google Chrome (WhatsCheck)", con el mismo formato que la de otro proyecto del autor que sí vincula.
- `baileys-cli`: `npm start` usa `node --import tsx` en lugar de `tsx`, con menos tiempo de arranque.
- `tech-stack.md`: los prototipos de la Fase 0 viven en `prototypes/` y no forman parte de la app.
- La verificación pasa a hacerse con el WhatsApp del propio usuario vinculado como dispositivo, dentro del teléfono. Se elimina el backend del MVP y se descarta iOS.
- Finales de línea fijados en LF para todo el repositorio con `.gitattributes`.
