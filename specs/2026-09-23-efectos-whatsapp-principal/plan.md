# Plan — Efectos secundarios en el WhatsApp principal

> **Estado:** Validado · **Fecha:** 2026-09-23
> **Relacionado:** [spec.md](spec.md) · [checklist.md](checklist.md) · [tech-stack](../product/tech-stack.md)

## Enfoque

Experimento de observación con dos lados:
- **Lado sesión (yo, por `adb`):** conectar, mantener, cortar y contar filas de la base del motor whatsmeow en el Poco.
- **Lado observador (autor):** desde WhatsApp Web con su número personal, mirar el chat con el número secundario: "en línea", checks y notificaciones en el teléfono del secundario.

**Hipótesis:** como whatsmeow nunca envía presencia `available` ni confirmaciones de lectura, el secundario no aparece "en línea", los mensajes quedan en doble check gris y el teléfono sigue notificando.

## Diseño

| Módulo | Cambio |
|---|---|
| `engine-whatsmeow/go/wacheck.go` | `Hold(seconds)`: conectar, esperar y cerrar, contando eventos por tipo. `TableCounts()`: JSON `{tabla: filas}` desde `sqlite_master` |
| `app/.../BenchActivity.kt` | Acciones `hold` y `stats` por `adb`, que escriben en `files/results/actions.log` |
| En el teléfono | `run-as … grep -c "<texto de prueba>" files/whatsmeow/whatsmeow.db*` (RF-03) |

### Protocolo (T4)

Preparación: en el WhatsApp del secundario, *Privacidad → Últ. vez y en línea → Todos* y *Confirmaciones de lectura: activadas*, como estén normalmente. La app de WhatsApp del secundario queda **cerrada** y la pantalla apagada.

**Requisitos aprendidos en la primera corrida (2026-09-23):**
- El número secundario **no debe tener otros clientes vinculados activos** (p. ej. otro bot con Baileys): leen mensajes y dejan al teléfono sin notificaciones, lo que contamina la línea base.
- El **observador** también debe mostrar su propia "última vez y en línea"; si la oculta, WhatsApp no le deja ver la de otros (reciprocidad).
- **Control positivo** antes de A: con el WhatsApp del secundario abierto en el Moto, el observador debe ver "en línea". Si no lo ve, el escenario no sirve.

| Escenario | Qué hago yo | Qué hace el autor en WhatsApp Web | Qué se anota |
|---|---|---|---|
| **A. Línea base** | Nada (sesión desconectada) | Envía `prueba WhatsCheck A`; mira 30 s el encabezado del chat | ¿"en línea"? · checks (gris/azul) · ¿notificación en el teléfono del secundario? |
| **B. Sesión mantenida** | `hold 60` | Durante esos 60 s envía `prueba WhatsCheck B` y mira el encabezado | Lo mismo que A |
| **C. Conexiones cortas** | 5 consultas en frío (`bench.sh … 5`), como llamadas reales | Mira el encabezado durante las 5 | ¿Aparece "en línea" aunque sea un instante? |
| **D. Base local** | `stats` + `grep` de `prueba WhatsCheck` en los archivos de la base | — | Filas por tabla; ¿aparece el texto? |
| **E. Cierre** | — | El autor abre el WhatsApp del secundario en el teléfono | ¿Los chats de A y B siguen como no leídos? Recién ahí deberían pasar a azul |

## Decisiones

| Decisión | Alternativas consideradas | Motivo |
|---|---|---|
| Mantener la sesión 60 s en el escenario B | Solo conexiones de ~2 s | Da tiempo a observar; si no hay efecto en 60 s, tampoco en 2 s |
| Inspeccionar la base en el teléfono | Copiarla a la PC | La base tiene las llaves de la sesión; no debe salir del teléfono |
| Contar eventos por tipo, sin contenido | Registrar los eventos | Cumple "no guarda mensajes" incluso en los logs de prueba |
| Reemplazar los stores de contactos, ajustes de chats y secretos de mensajes por versiones vacías, y vaciar esas tablas al abrir | Desactivar la sincronización del estado de la app | whatsmeow no tiene opción para no sincronizarlo: se dispara cuando el teléfono comparte las llaves. El store es intercambiable (`store.Device`), así que se descartan los datos en lugar de guardarlos. Las sesiones cifradas se conservan: sin ellas no hay confirmación de entrega y el remitente reintenta |



| Riesgo | Mitigación |
|---|---|
| El autor abre la app de WhatsApp del secundario y contamina la prueba | El protocolo lo prohíbe hasta el escenario E; se mira solo la notificación |
| La privacidad del secundario oculta "en línea" | RNF-02: se verifica antes |
| whatsmeow guarda contactos por la sincronización de estado de la app | Medirlo (D) y decidir en T5 |

## Cambios a documentos base

- **Aprobado y aplicado (2026-09-24):** `tech-stack.md` §2, requisito de no marcarse "en línea" marcado como **verificado en dispositivo**.
- **Aprobado y aplicado (2026-09-24):** fila de **minimización de datos** en tech-stack §2.

