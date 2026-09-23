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

#### Cambiado
- La verificación pasa a hacerse con el WhatsApp del propio usuario vinculado como dispositivo, dentro del teléfono. Se elimina el backend del MVP y se descarta iOS.
- Finales de línea fijados en LF para todo el repositorio con `.gitattributes`.
