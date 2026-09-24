# Requisitos — Diseño de la app de pruebas

> **Estado:** En progreso · **Fecha:** 2026-09-24
> **Origen:** pedido directo del autor durante el [spike de llamadas](../2026-09-24-spike-llamadas-overlay/) ("trabajemos de una vez"); no es un ítem del roadmap. Adelanta, en versión de prueba, pantallas de la Fase 1 (ajustes, vinculación) y de la Fase 2 (historial).
> **Relacionado:** [plan.md](plan.md) · [validation.md](validation.md) · [misión](../product/mission.md)

## Objetivo

Que la app que el autor usa a diario durante la semana de uso real se vea y se entienda como un producto, no como una herramienta de depuración: tres vistas claras, un ícono propio, el estado real de la sesión y el número vinculado.

## Contexto

- Hoy la app es una sola pantalla larga con "Filtrado de llamadas" arriba y "Motor / whatsmeow" abajo.
- **Error visible:** la sesión figura `UNLINKED` aunque está vinculada. El motor se crea recién al entrar una llamada o al consultar, y la pantalla lee un valor inicial que nunca se actualiza.
- El autor quiere ver **a qué número está vinculada** la sesión.
- Detalles internos como "motor" o "whatsmeow" no deben aparecer en la interfaz (el motor ya está elegido).

## Requisitos funcionales

| ID | Requisito | Prioridad |
|---|---|---|
| RF-01 | Navegación inferior con tres vistas: **Inicio** (la que se abre), **Registro** y **Ajustes**. | Obligatorio |
| RF-02 | **Inicio**: tarjeta de cuenta con el **estado real** de la sesión y el **número vinculado** (enmascarado, con opción de mostrarlo completo); tarjeta de protección ("Protección activa" o qué falta); **consultar un número a mano**; las **últimas llamadas** filtradas. Sin la palabra "motor" ni "whatsmeow". | Obligatorio |
| RF-03 | Si no hay sesión: Inicio muestra la **vinculación guiada** (número, botón, código grande copiado al portapapeles y los pasos en WhatsApp). | Obligatorio |
| RF-04 | **Registro**: resumen (✅/⚠️/❔, tiempos p50/p95, último latido) y **lista de llamadas** (número enmascarado, resultado, hora, tiempo hasta el resultado, si fue con pantalla bloqueada); botones **Copiar resumen** y **Exportar**. | Obligatorio |
| RF-05 | **Ajustes**: permisos con explicación (rol, aviso flotante, notificaciones, batería), accesos de MIUI, modo "siempre activo", aviso de ejemplo, **desvincular** (con confirmación) y versión. | Obligatorio |
| RF-06 | **Ícono propio** adaptable (con variante monocromática para íconos temáticos) y un ícono de notificación propio. No imita la marca de WhatsApp: no usa su verde ni su globo de chat. | Obligatorio |
| RF-07 | Tema claro y oscuro (sigue al sistema), con una paleta propia. | Obligatorio |

## Requisitos no funcionales

| ID | Requisito | Métrica |
|---|---|---|
| RNF-01 | Estado correcto al abrir la app. | Con la sesión vinculada, Inicio muestra "Vinculada" y el número en ≤ 1 s |
| RNF-02 | Privacidad en pantalla. | Número vinculado enmascarado por defecto; en Registro solo números enmascarados |
| RNF-03 | Sin dependencias nuevas que no estén en caché. | Solo `material-icons-core` (1.7.8, en caché) |

## Principios de la misión que aplican

| Principio | Cómo se respeta |
|---|---|
| Honestidad en la señal | Textos "Tiene / No tiene WhatsApp", nunca "spam". |
| Transparencia sobre el riesgo | Ajustes explica para qué es cada permiso y deja desvincular en un paso. |
| Lo del usuario se queda en su teléfono | El número vinculado se muestra solo en el teléfono y enmascarado por defecto. |

## Fuera de alcance

- Onboarding completo con consentimiento de riesgo (Fase 1).
- Filtros o búsqueda en el registro (Fase 2).
