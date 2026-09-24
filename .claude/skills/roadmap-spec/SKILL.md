---
name: roadmap-spec
description: Avanza el roadmap de WhatsCheck un paso a la vez con spec-driven development. Toma el siguiente ítem pendiente de specs/product/roadmap.md (o el que se indique), crea la carpeta specs/YYYY-MM-DD-nombre-del-feature/ con requirements.md, plan.md y validation.md, y después de la aprobación guía la implementación y la validación hasta marcar el ítem como hecho. Usar cuando el usuario pida avanzar el roadmap, especificar el siguiente paso o feature, o invoque /roadmap-spec.
argument-hint: "[siguiente | texto del ítem del roadmap | ruta de una spec existente]"
---

# Avanzar el roadmap con specs

Cada ítem `[ ]` de `specs/product/roadmap.md` se convierte en una carpeta de spec antes de implementarse. Esta skill hace ese paso completo en **tres etapas con aprobación del usuario entre cada una**. Nunca saltes una etapa.

Documentos base que se leen **siempre** antes de escribir nada:
- `specs/product/mission.md`: principios de diseño y alcance (lo que el producto NO hace).
- `specs/product/tech-stack.md`: arquitectura, módulos, contrato del motor y riesgos.
- `specs/product/roadmap.md`: fases, ítems, decisiones abiertas y tomadas.
- `specs/product/intentos-fallidos.md`: lo que ya se probó o descartó y no hay que repetir.

Registros que esta skill mantiene al día (reglas completas en `CLAUDE.md`):
- `CHANGELOG.md`: qué cambió.
- `specs/product/intentos-fallidos.md`: qué no funcionó.

Todo el contenido de las specs se escribe en **español**, con finales de línea LF.

---

## Etapa 1 — Elegir el ítem

Interpreta `$ARGUMENTS`:

| Argumento | Qué hacer |
|---|---|
| vacío o `siguiente` | El primer ítem `- [ ]` en orden de aparición, **dentro de la fase más temprana que tenga pendientes**. Ignora los ítems ya marcados `[x]` o tachados. |
| texto | El ítem del roadmap que mejor coincida. Si hay más de uno posible, pregunta cuál. |
| ruta `specs/AAAA-MM-DD-*` | Retomar esa spec existente en la etapa que indique su campo **Estado** (ver abajo). |

Antes de crear nada:
1. Revisa si ya existe una carpeta `specs/*-<nombre>/` para ese ítem, o si el ítem ya tiene un enlace a una spec en el roadmap. Si existe, **retómala** en lugar de crear otra.
2. Revisa dependencias: si el ítem necesita una decisión abierta del roadmap o un ítem anterior sin terminar (p. ej. el motor elegido en la Fase 0), dilo y pregunta si seguir igual.
3. Confirma con el usuario en una línea: *"Siguiente ítem: <ítem> (Fase N). ¿Lo especifico?"*.

## Etapa 2 — Escribir la spec

### Carpeta
```
specs/<AAAA-MM-DD>-<nombre-del-feature>/
├── requirements.md
├── plan.md
└── validation.md
```
- **Fecha:** la de hoy, obtenida con `date +%F`. No la inventes ni la copies de otra spec.
- **Nombre:** kebab-case, en español, sin tildes ni ñ (`vinculacion-codigo-emparejamiento`, `aviso-flotante`), de 2 a 5 palabras, derivado del título del ítem.

### Preguntas antes de escribir
Si el ítem deja dudas que cambian el diseño (comportamiento visible, alcance, decisiones abiertas del roadmap), haz **como máximo 3 preguntas** concretas antes de escribir. No preguntes lo que ya responden los documentos base; cítalos en su lugar.

### Contenido
Usa las plantillas de esta skill y reemplaza todo lo que está entre `<…>`:
- [templates/requirements.md](templates/requirements.md): **qué** hay que construir y **por qué**.
- [templates/plan.md](templates/plan.md): **cómo** se construye, con tareas ordenadas.
- [templates/validation.md](templates/validation.md): **cómo se demuestra** que está bien.

Reglas:
- **Trazabilidad:** cada requisito tiene un ID (`RF-01`, `RNF-01`). Cada tarea de `plan.md` y cada criterio de `validation.md` indica qué IDs cubre. **Ningún requisito queda sin criterio de validación.**
- **Principios de la misión:** en `requirements.md`, lista los principios de `mission.md` que aplican a este ítem y cómo se respetan. Si un requisito choca con alguno, dilo explícitamente en lugar de ocultarlo.
- **Coherencia con el tech-stack:** usa los módulos, interfaces y nombres que define `tech-stack.md` (p. ej. `WhatsAppPresenceEngine`, `CheckResult`, `feature/overlay`). Si el ítem obliga a cambiar el tech-stack, anótalo en `plan.md` → *Cambios a documentos base*. No edites los documentos base sin preguntar.
- **Ítems de investigación (típicos de la Fase 0):** `plan.md` describe el experimento (hipótesis, montaje, datos a medir) en lugar de tareas de código, y `validation.md` define los **criterios Go / No-Go** con umbrales numéricos.
- **Seguridad de cuentas:** cualquier prueba que conecte una sesión real de WhatsApp debe decir explícitamente que usa **números secundarios**, nunca cuentas personales, mientras el roadmap no lo permita.
- **Intentos fallidos:** no propongas en `plan.md` un enfoque que ya esté en `intentos-fallidos.md`, salvo que se cumpla su condición de *Reconsiderar si*. En ese caso, cita el ID (`IF-NNN`) en *Decisiones* y explica qué cambió. Si descartas alternativas relevantes al escribir el plan, regístralas ahí como `Descartado por análisis`.
- No rellenes secciones con texto genérico: si una sección no aplica, escribe `No aplica — <motivo>`.

### Registrar la spec
- En `roadmap.md`, agrega el enlace al final de la línea del ítem, sin marcarlo como hecho:
  `- [ ] **Aviso flotante** — … \`M\` → [spec](../2026-09-23-aviso-flotante/)`
- Deja las tres specs con `**Estado:** Borrador`.
- En `CHANGELOG.md`, agrega bajo *Añadido*: `Spec de <feature> ([specs/AAAA-MM-DD-<nombre>](specs/AAAA-MM-DD-<nombre>/)).`

Termina la etapa con un resumen corto: carpeta creada, número de requisitos, tareas y criterios, y las preguntas abiertas que queden. **Pide aprobación explícita** antes de implementar. Si el usuario pide cambios, edítalos y vuelve a pedir aprobación.

## Etapa 3 — Implementar y validar (solo con aprobación)

Cuando el usuario apruebe:
1. Cambia el estado de las tres specs a `Aprobado`, y a `En progreso` al empezar.
2. Implementa siguiendo las tareas de `plan.md` **en orden**, marcando cada `- [ ]` como `- [x]` al terminarla. Si descubres que el plan está mal o incompleto, **detente**, actualiza `plan.md` (y `requirements.md` si cambia el alcance) y avisa antes de seguir.
   - **Cada vez que algo falle** y obligue a cambiar de enfoque (una librería que no sirve, una API que no se comporta como dice su documentación, un fabricante que mata el servicio, un experimento con resultado negativo), regístralo en `intentos-fallidos.md` en ese momento, no al final. Enlaza la entrada desde `plan.md` → *Decisiones*.
3. Ejecuta lo que dice `validation.md`: tests automáticos, lint, y deja listas las pruebas manuales que requieran un dispositivo o una llamada real. Esas no las marques como pasadas: pide al usuario el resultado.
4. Completa el **Registro de resultados** de `validation.md` con resultados reales (fecha, comando o método, resultado, evidencia). Nunca marques un criterio como pasado sin haberlo verificado.
5. Solo cuando **todos** los criterios obligatorios pasen:
   - Estado de las tres specs → `Validado`.
   - En `roadmap.md`, marca el ítem `- [x]` (conserva el enlace a la spec).
   - Si el ítem cerró una decisión abierta del roadmap, muévela a *Decisiones tomadas* con fecha y motivo.
   - En `CHANGELOG.md`, registra lo que el feature agrega o cambia, desde el punto de vista del usuario o del proyecto, con enlace a la spec.
6. Si algún criterio falla y no se puede resolver, deja el estado en `En progreso`, documenta el bloqueo en `validation.md` y no marques el ítem. Si el fallo descarta el enfoque (típico en un No-Go de la Fase 0), regístralo también en `intentos-fallidos.md`.

No hagas commits salvo que el usuario lo pida. Si lo pide, un commit por etapa es lo recomendado: `docs(specs): spec de <feature>` y luego `feat(<modulo>): <feature>`.

## Estados de una spec

| Estado | Significa | Siguiente paso |
|---|---|---|
| `Borrador` | Spec escrita, sin aprobar | Revisión y aprobación (Etapa 2) |
| `Aprobado` | Lista para implementar | Etapa 3 |
| `En progreso` | Implementación o validación en curso | Continuar tareas y criterios pendientes |
| `Validado` | Todos los criterios obligatorios pasaron; ítem marcado en el roadmap | Ninguno |
| `Descartado` | Se decidió no hacerlo (anotar motivo en `requirements.md`) | Ninguno |
