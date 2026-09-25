# Especificaciones (Spec-Driven Development)

WhatsCheck sigue la metodología **Spec-Driven Development (SDD)**. Antes de escribir una sola línea de código, cada funcionalidad se diseña, se valida, se desglosa y se audita de forma rigurosa.

Este directorio (`specs/`) contiene tanto los documentos fundacionales del proyecto como las carpetas individuales de cada funcionalidad a implementar.

## 1. Documentos Fundacionales (La Base)

Estos archivos mandan sobre todo el código y deben ser consultados continuamente por las skills:

- [`mission.md`](mission.md) — Qué es WhatsCheck, para quién está diseñado y sus principios de experiencia de usuario.
- [`tech-stack.md`](tech-stack.md) — La pila tecnológica, patrones aprobados y arquitectura.
- [`roadmap.md`](roadmap.md) — El historial de versiones, lo que está en curso y lo que vendrá.
- [`intentos-fallidos.md`](intentos-fallidos.md) — Registro de enfoques y herramientas que fallaron y no deben repetirse.

## 2. El Flujo de Trabajo SDD (Paso a Paso)

Cuando se va a implementar una funcionalidad nueva, el trabajo se guarda en su propia carpeta `specs/AAAA-MM-DD-nombre-en-kebab/`. 

Las skills de IA están diseñadas para ejecutarse estrictamente en este orden. Cada skill produce un artefacto que la siguiente skill consume:

### Etapa 1: Diseño y Requisitos (El Qué)
1. **`specify`**: Dialoga contigo para definir exactamente qué se va a hacer. Genera el **`spec.md`** (Criterios de Aceptación, Requisitos y Casos Extremos).
2. **`clarify`**: Actúa como un Analista de Producto. Lee el `spec.md` buscando palabras ambiguas, lagunas lógicas o casos no contemplados. Reescribe el documento para blindarlo.

### Etapa 2: Arquitectura y Tareas (El Cómo)
3. **`plan`**: Traduce el `spec.md` en un diseño de arquitectura técnica generando **`plan.md`**. Justifica librerías, dependencias y valida explícitamente el cumplimiento con los documentos fundacionales.
4. **`tasks`**: Lee el `plan.md` y lo desglosa en unidades de trabajo ejecutables agrupadas por fases (Setup, Backend, UI, Seguridad). Genera la hoja de ruta **`tasks.md`**.
5. **`checklist`**: Vuelve al `spec.md` y genera el plan de pruebas manuales y QA (User Acceptance Testing) en **`checklist.md`**. Define los pasos exactos para comprobar la funcionalidad.

### Etapa 3: Auditoría y Construcción (La Ejecución)
6. **`analyze`**: Actúa como control de calidad cruzado. Revisa automáticamente que *nada* se haya escapado entre `spec`, `plan` y `tasks`, detectando cuellos de botella antes de codificar.
7. **`implement`**: El motor de desarrollo. Lee el `tasks.md` y ejecuta TDD (Test-Driven Development) tarea por tarea, marcando las casillas a medida que escribe el código y pausando ante cualquier bloqueo.

### Etapa 4: Finalización (Fuera del SDD)
Estas skills son atajos de flujo de trabajo genéricos para cerrar el ciclo:
8. **`close`**: Corre los linters y tests, verifica que `tasks.md` y `checklist.md` estén marcados como Vistos (`[x]`), consolida todo en un commit limpio y empuja la rama (pudiendo generar el PR).
9. **`publish`**: Se usa desde `main` para hacer el salto SemVer, actualizar el CHANGELOG, compilar el binario firmado (APK), crear el Git Tag y lanzar el release en GitHub.

*(Nota: Los arreglos menores aislados no requieren crear toda una carpeta de spec, pueden ser ejecutados y cerrados directamente).*


