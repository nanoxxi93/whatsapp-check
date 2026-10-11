# WhatsCheck

App Android que, al entrar una llamada de un número desconocido, verifica si tiene WhatsApp (con la cuenta del propio usuario vinculada como dispositivo) y muestra un aviso sin bloquear la llamada.

**Specs primero (SDD):** El proyecto usa **Spec-Driven Development**. Toda funcionalidad nueva nace en [`specs/`](./specs) con sus requerimientos, plan técnico, y desglose de tareas, pasando por un ciclo de auditoría antes de tocar código. La misión, pila técnica y hoja de ruta viven allí.
El ciclo completo se ejecuta usando las skills ubicadas en [`.claude/skills/`](./.claude/skills) en este orden estricto: `specify` → `clarify` → `plan` → `tasks` → `checklist` → `analyze` → `implement` → `close` → `publish`. Para entender el detalle del proceso, **lee siempre [`specs/README.md`](./specs/README.md) primero**.

**Memoria Local de la IA:** Para evitar errores repetitivos de entorno local (como usar CRLF en Windows, o ejecutar comandos bash incompatibles con PowerShell), cualquier IA que trabaje en este repo buscará y respetará el archivo **`.ai-context.md`** (vinculado a `.cursorrules` y `.clauderc`). Ese archivo NO se commitea, y la IA tiene la orden de **actualizarlo** ante cualquier tropiezo con el sistema operativo local para "recordarlo" en chats futuros.

## Documentación

- **Requisitos y Planificación**: Ver [`specs/README.md`](./specs/README.md).
- **Intentos Fallidos**: Ver [`specs/intentos-fallidos.md`](./specs/intentos-fallidos.md) antes de proponer soluciones.
- **Cambios**: Registrados en [`CHANGELOG.md`](./CHANGELOG.md).

## Descargo de responsabilidad / Disclaimer

Este proyecto es una prueba de concepto (PoC) y prototipo de investigación técnica para explorar la interoperabilidad entre Android Telecom (`CallScreeningService`), Jetpack Compose y motores Go (`whatsmeow`) compilados vía `gomobile`. Desarrollado con fines exclusivamente educativos y de experimentación personal. No está afiliado, mantenido, autorizado ni respaldado por WhatsApp ni por Meta Platforms, Inc.

