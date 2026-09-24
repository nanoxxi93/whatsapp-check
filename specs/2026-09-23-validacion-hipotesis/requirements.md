# Requisitos — Validación de la hipótesis

> **Estado:** Descartado (2026-09-23) · **Fecha:** 2026-09-23
> **Ítem del roadmap:** Fase 0 — Validación de la hipótesis (`M`)
> **Relacionado:** [plan.md](plan.md) · [validation.md](validation.md) · [roadmap](../product/roadmap.md) · [misión](../product/mission.md#qué-informa-la-app-y-qué-no)

## Motivo del descarte

**2026-09-23, decisión del autor:** la app informa si el número tiene WhatsApp; no es un detector de spam. Como el producto no afirma que "sin WhatsApp" signifique spam, no hace falta validar esa relación. Además, hay pocos números legítimos desconocidos disponibles para formar un grupo de comparación. La misión se actualizó en consecuencia (v0.3). No se implementó nada de esta spec; se conserva como registro de la decisión.

## Objetivo

Medir con llamadas reales si **"no tiene WhatsApp"** separa las llamadas no deseadas de las legítimas lo suficiente como para que valga la pena construir la app. Es la decisión *Go / No-Go* del producto: si la señal no discrimina, el resto del roadmap no tiene sentido tal como está planteado.

## Contexto

- La herramienta ya existe y está validada: `prototypes/baileys-cli` consulta listas con `check --file` y guarda los resultados en JSONL con etiqueta ([spec anterior](../2026-09-23-prototipo-baileys-escritorio/)).
- La hipótesis tiene excepciones conocidas en ambos sentidos ([misión](../product/mission.md#qué-informa-la-app-y-qué-no)): negocios legítimos sin WhatsApp (fijos, bancos) y empresas comerciales **con** WhatsApp Business.
- **Primer lote (2026-09-23):** 23 números que llamaron al autor en los últimos 5 días, guardados en `prototypes/baileys-cli/data/llamadas-recibidas.csv` (fuera de git), sin etiquetar todavía. 7 de ellos están en el mismo rango (`93695…`/`93696…`), un patrón típico de call center.
- **Sesgo a evitar:** el grupo "legítimo" debe ser de números **desconocidos** que llamaron y resultaron legítimos (courier, banco, clínica, cliente nuevo). Los contactos guardados casi siempre tienen WhatsApp e inflarían el resultado; además, la app no se activa con contactos guardados.
- Cada lote de consultas usa la cuenta secundaria con un patrón realista (pocas consultas al día). Eso también sirve de evidencia temprana para la *prueba de resistencia al baneo*.

## Historias de usuario

- Como autor, quiero etiquetar cada llamada recibida como `spam` o `legitimo` y consultarlas en lotes, para acumular datos reales sin arriesgar la cuenta de prueba.
- Como autor, quiero un resumen con porcentajes, intervalos de confianza y un veredicto, para decidir si seguir con el producto.

## Requisitos funcionales

| ID | Requisito | Prioridad |
|---|---|---|
| RF-01 | Formato de datos `número,etiqueta` con etiquetas `spam` (publicidad, estafa, cobranza no deseada, encuestas), `legitimo` (llamada deseada de un número no guardado) y `contacto` (solo como referencia, no entra en el veredicto). `check` avisa si una etiqueta no es válida y omite ese número. | Obligatorio |
| RF-02 | Cada registro del JSONL lleva un `phoneId` estable y no reversible (HMAC-SHA256 del número con un secreto local), para agrupar y deduplicar sin guardar el número. El enmascarado actual colisiona dentro de un mismo rango: `+5193*****17`. | Obligatorio |
| RF-03 | `check` elimina duplicados dentro del mismo archivo (el mismo número pudo llamar varias veces). | Obligatorio |
| RF-04 | Comando `analyze`: lee los resultados JSONL, toma el resultado más reciente por `phoneId` y muestra por etiqueta: n, con WhatsApp, sin WhatsApp, no verificados, y el % sin WhatsApp con intervalo de confianza del 95 % (Wilson). | Obligatorio |
| RF-05 | `analyze` calcula **sensibilidad** (% de `spam` sin WhatsApp), **especificidad** (% de `legitimo` con WhatsApp) y el **veredicto Go / No-Go** de [validation.md](validation.md). Los `UNKNOWN` se excluyen del cálculo y se informan aparte. | Obligatorio |
| RF-06 | `analyze` estima la **precisión del aviso** (de los "⚠️ sin WhatsApp", cuántos son spam) con la proporción de spam observada en las llamadas del autor. | Deseable |
| RF-07 | Opción `check --business`: para los números con WhatsApp, consultar si es una cuenta **Business** y guardar solo el booleano `isBusiness` en el JSONL. Nunca nombre, foto ni descripción. Mide cuántas llamadas comerciales esquivan la señal usando WhatsApp Business. | Deseable |

## Requisitos no funcionales

| ID | Requisito | Métrica |
|---|---|---|
| RNF-01 | **Números reales fuera de git.** | `data/` en `.gitignore`; en `validation.md` y en el CHANGELOG solo van cifras agregadas, sin números |
| RNF-02 | **Protección de la cuenta.** Se mantienen los topes del prototipo. | Máx. 50 números por ejecución, pausas de 3–6 s; se recomienda ≤ 1 lote por día |
| RNF-03 | **`phoneId` no reversible.** | Secreto aleatorio de 32 bytes en `data/.phone-id-secret`, generado la primera vez; ID de 16 caracteres hex |
| RNF-04 | Lógica de análisis (Wilson, sensibilidad, especificidad, veredicto, deduplicación) cubierta por tests. | Todos los casos del veredicto y bordes (n = 0, 0 %, 100 %) |

## Principios de la misión que aplican

| Principio | Cómo se respeta en este feature |
|---|---|
| Honestidad en la señal | El resultado decide qué texto mostrar. Si la señal es débil, se dice en la misión en lugar de venderla como detector de spam. |
| Fallar en silencio | `UNKNOWN` no cuenta como "sin WhatsApp" en el análisis (RF-05). |
| No mostrar datos del perfil de terceros | `--business` guarda solo un booleano, y solo para el análisis (RF-07). |

## Fuera de alcance

- Señales combinadas (rango de numeración, frecuencia): Fase 3. Aquí solo se **anota** lo observado, como el rango `93695…`/`93696…`.
- Prueba de resistencia al baneo con varias cuentas: ítem propio.
- Mostrar el nombre de empresas Business: decisión abierta 5 del roadmap.

## Dependencias

- Sesión vinculada del número secundario (ya existe).
- **El autor etiqueta los números** y reúne un grupo `legitimo` de números desconocidos. Objetivo: ≥ 30 por grupo; ideal 50.

## Preguntas abiertas

- [ ] ¿Los 23 números del primer lote son todos no deseados, o alguno fue legítimo? — *autor, antes de T7*
- [ ] ¿De dónde sale el grupo `legitimo`? Propuesta: el historial de llamadas de las últimas semanas (números no guardados que resultaron ser courier, banco, clínica, clientes), más los que lleguen durante la recolección. — *autor*
- [ ] ¿Se incluye `--business` (RF-07)? Suma una consulta extra por cada número con WhatsApp. — *autor, al aprobar la spec*
