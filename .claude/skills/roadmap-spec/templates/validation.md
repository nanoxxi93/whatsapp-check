# Validación — <Nombre del feature>

> **Estado:** Borrador · **Fecha:** <AAAA-MM-DD>
> **Relacionado:** [requirements.md](requirements.md) · [plan.md](plan.md)

## Criterios de aceptación

Cada requisito de [requirements.md](requirements.md) tiene al menos un criterio.

| ID | Requisito | Criterio (verificable) | Método | Obligatorio |
|---|---|---|---|---|
| CA-01 | RF-01 | <Dado … cuando … entonces …> | Test unitario / instrumentado / manual en dispositivo / medición | Sí |
| CA-02 | RNF-01 | <p. ej. p95 < 3 s en 20 llamadas de prueba> | Medición | Sí |

## Criterios Go / No-Go

<Solo para ítems de investigación. Umbrales numéricos que deciden si se sigue adelante. Si no aplica: "No aplica — ítem de implementación".>

| Métrica | Go si | No-Go si |
|---|---|---|
| <…> | <…> | <…> |

## Plan de pruebas

### Automáticas
- <Qué se testea y con qué herramienta del tech-stack (JUnit 5, MockK, Turbine, Compose UI Test…).>

### Manuales
- <Pasos concretos, dispositivo y versión de Android. Si hay sesión real de WhatsApp: **solo números secundarios**.>

## Definición de terminado

- [ ] Todos los criterios obligatorios pasaron y están en el registro.
- [ ] Tests automáticos en verde y lint sin errores nuevos.
- [ ] Sin números de teléfono ni datos de sesión en logs, crash reports ni analítica.
- [ ] Tareas de [plan.md](plan.md) marcadas.
- [ ] Ítem marcado `[x]` en el roadmap y decisiones movidas a *Decisiones tomadas* si aplica.

## Registro de resultados

Se llena durante la validación, con resultados reales.

| Fecha | Criterio | Método / comando | Resultado | Evidencia / notas |
|---|---|---|---|---|
| | | | | |

## Bloqueos

<Criterios que no se pudieron cumplir, motivo y qué falta. Vacío si no hay.>
