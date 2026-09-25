# Validación — Validación de la hipótesis

> **Estado:** Descartado (2026-09-23) · **Fecha:** 2026-09-23
> **Relacionado:** [spec.md](spec.md) · [plan.md](plan.md)

## Criterios de aceptación

| ID | Requisito | Criterio (verificable) | Método | Obligatorio |
|---|---|---|---|---|
| CA-01 | RF-01 | Un archivo con etiquetas `spam`, `Legítimo`, `contacto` y `otra`: se aceptan las tres primeras (normalizadas) y `otra` se omite con aviso. | Test + manual | Sí |
| CA-02 | RF-02, RNF-03 | `phoneId` es igual para el mismo número en dos ejecuciones, distinto para los 7 números del rango `93695…`/`93696…`, y el secreto se crea una sola vez en `data/`. | Test | Sí |
| CA-03 | RF-03 | Un archivo con el mismo número dos veces genera una sola consulta. | Test + manual | Sí |
| CA-04 | RF-04 | `analyze` sobre un JSONL de prueba muestra por etiqueta n, con/sin WhatsApp, no verificados y % sin WhatsApp con IC 95 %; si un `phoneId` aparece varias veces, usa el resultado más reciente. | Test | Sí |
| CA-05 | RF-04 | Wilson 95 % coincide con valores de referencia: 0/10 → [0 %, 27,8 %]; 5/10 → [23,7 %, 76,3 %]; 10/10 → [72,2 %, 100 %]. | Test | Sí |
| CA-06 | RF-05 | Los `UNKNOWN` no entran en sensibilidad ni especificidad y se muestran aparte. | Test | Sí |
| CA-07 | RF-05 | El veredicto sigue la tabla Go / No-Go para casos límite (60 %, 85 %, n = 29 / 30). | Test | Sí |
| CA-08 | RF-06 | La precisión estimada coincide con el cálculo manual para un caso conocido. | Test | No |
| CA-09 | RF-07 | Con `--business`, los registros con WhatsApp tienen `isBusiness` `true`/`false`/`null`, los demás `null`, y el JSONL no contiene nombre, descripción ni foto. | Manual + revisión del JSONL | No (solo si se aprueba RF-07) |
| CA-10 | RNF-01 | `git status` no muestra `data/` ni `results/`; `checklist.md` y el CHANGELOG no contienen números de teléfono. | Comando + `git grep` | Sí |
| CA-11 | Objetivo | Existe un resultado **definitivo** con n ≥ 30 en `spam` y en `legitimo`, registrado abajo con su veredicto. | `analyze` | Sí |

## Criterios Go / No-Go

Se evalúan con `analyze` sobre números **desconocidos** etiquetados. Sensibilidad = % de `spam` **sin** WhatsApp. Especificidad = % de `legitimo` **con** WhatsApp. El peor criterio define el resultado.

| Métrica | Go | Go con reservas | No-Go |
|---|---|---|---|
| Sensibilidad | ≥ 60 % | 40–59 % | < 40 % |
| Especificidad | ≥ 85 % | 70–84 % | < 70 % |
| Tamaño de muestra | ≥ 30 por grupo | < 30 en algún grupo → el resultado es **preliminar**, no definitivo | — |

Cómo se lee:
- **Go:** la señal sola justifica el producto tal como está en la misión.
- **Go con reservas:** la señal ayuda pero no alcanza sola. Se adelantan las *señales combinadas* (Fase 3) y se ajusta el texto del aviso.
- **No-Go:** "no tiene WhatsApp" no distingue spam de llamadas legítimas. Se detiene el roadmap y se revisa la misión.

Además se informa, sin que decida el veredicto: % de spam con WhatsApp **Business** (si se aprueba RF-07) y cuántos números de spam comparten rango.

## Plan de pruebas

### Automáticas
- `npm test`: `labels.test.ts`, `phone-id.test.ts`, `analysis.test.ts` y los tests existentes.

### Manuales
1. Archivo de prueba con etiquetas válidas, inválidas y duplicados → `check --file` (CA-01, CA-03).
2. Lote preliminar: `check --file data/llamadas-recibidas.csv` (ya etiquetado) → `analyze` (CA-04, resultado preliminar).
3. `git status` y `git grep` de números (CA-10).
4. Lotes siguientes durante la recolección → `analyze` final (CA-11).

## Definición de terminado

- [ ] Todos los criterios obligatorios pasaron y están en el registro.
- [ ] `npm test` en verde.
- [ ] Sin números de teléfono en git ni en los documentos; solo cifras agregadas.
- [ ] Tareas de [plan.md](plan.md) marcadas.
- [ ] Veredicto definitivo anotado; `mission.md` actualizado con los datos (con aprobación).
- [ ] Ítem marcado `[x]` en el roadmap.

## Registro de resultados

Solo cifras agregadas. Nunca números de teléfono.

| Fecha | Criterio | Método / comando | Resultado | Evidencia / notas |
|---|---|---|---|---|
| | | | | |

## Bloqueos

