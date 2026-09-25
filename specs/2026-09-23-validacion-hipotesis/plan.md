# Plan — Validación de la hipótesis

> **Estado:** Descartado (2026-09-23) · **Fecha:** 2026-09-23
> **Relacionado:** [spec.md](spec.md) · [checklist.md](checklist.md) · [tech-stack](../product/tech-stack.md)

## Enfoque

Es un **experimento** con dos partes:

1. **Herramienta:** extender `prototypes/baileys-cli` con etiquetas validadas, `phoneId`, deduplicación y un comando `analyze`. La lógica estadística va en funciones puras con tests.
2. **Recolección:** el autor etiqueta las llamadas y las consulta en lotes pequeños durante unos días, hasta llegar a ≥ 30 números por grupo. Se corre `analyze` sobre el primer lote (resultado preliminar) y al final (resultado definitivo).

**Hipótesis:** entre números desconocidos, los de `spam` tienen WhatsApp con mucha menos frecuencia que los `legitimo`. Umbrales en [checklist.md](checklist.md#criterios-go--no-go).

## Diseño

| Módulo | Cambio |
|---|---|
| `prototypes/baileys-cli/.gitignore` | Agregar `data/` (ya hecho al guardar el primer lote) |
| `src/labels.ts` | Nuevo. Etiquetas válidas y normalización (`Spam` → `spam`, `legítimo` → `legitimo`) |
| `src/phone-id.ts` | Nuevo. `phoneId(digits)` = HMAC-SHA256 con el secreto de `data/.phone-id-secret`, 16 hex |
| `src/analysis.ts` | Nuevo, **puro**. Wilson 95 %, agrupación por etiqueta, último resultado por `phoneId`, sensibilidad, especificidad, precisión estimada y veredicto |
| `src/output.ts` | `ResultRecord` suma `phoneId` e `isBusiness` (`boolean \| null`) |
| `src/cli.ts` | `check`: valida etiquetas, deduplica y agrega `--business`. Nuevo comando `analyze` |

Flujo:
```
data/*.csv (número,etiqueta) ──check──► results/AAAA-MM-DD.jsonl (phoneId, label, status, isBusiness)
                                                    │
                                          analyze ◄─┘  → tabla por etiqueta + IC 95 % + veredicto
```

`--business`: después de `onWhatsApp`, si `HAS_WHATSAPP`, llamar a `sock.getBusinessProfile(jid)` con timeout de 5 s. `isBusiness = true` si devuelve perfil, `false` si no, `null` si falla. No se guarda nada del perfil.

## Decisiones

| Decisión | Alternativas consideradas | Motivo |
|---|---|---|
| Intervalo de Wilson 95 % | Intervalo normal (Wald) | Con n chico y proporciones cercanas a 0 % o 100 %, Wald da intervalos absurdos (negativos o de ancho cero) |
| `phoneId` con HMAC y secreto local | Enmascarado; hash sin secreto | El enmascarado colisiona en rangos como `93696…`. Un SHA-256 sin secreto se revierte probando los ~10⁹ números peruanos posibles |
| Secreto en `data/`, no en `.auth/` | `.auth/` | `.auth/` se borra con `unlink` o tras un 401; los IDs cambiarían entre lotes |
| El veredicto lo deciden los peores criterios | Promedio | Coherente con el prototipo: una señal que falla en un grupo no sirve aunque el otro salga bien |
| `contacto` solo como referencia | Usarlo como grupo legítimo | La app no se activa con contactos guardados y casi todos tienen WhatsApp: sesgaría la especificidad hacia arriba |



| Riesgo | Mitigación |
|---|---|
| Muestra chica o sesgada (todo el spam del mismo call center) | Informar el IC 95 %; anotar cuántos números vienen del mismo rango; el veredicto exige n ≥ 30 por grupo |
| Etiquetas equivocadas (una llamada que parecía spam era legítima) | Etiqueta de quien atendió la llamada; ante duda, no incluir el número |
| Grupo `legitimo` difícil de reunir | Aceptar recolección más lenta; mientras tanto, veredicto "preliminar" |
| Más consultas → más riesgo para la cuenta secundaria | Topes y pausas del prototipo; ≤ 1 lote por día |

## Cambios a documentos base

- `mission.md` → *Hipótesis central*: agregar los resultados medidos (sensibilidad, especificidad, n, fecha) cuando exista el veredicto final. Se aplica solo con aprobación.

