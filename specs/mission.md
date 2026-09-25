# Misión del producto

> **Nombre provisional:** WhatsCheck
> **Estado:** Borrador v0.3 · 2026-09-23
> **Cambios v0.2:** la verificación se hace desde el WhatsApp del propio usuario, vinculado como dispositivo; se elimina la dependencia de un backend o proveedor externo.
> **Cambios v0.3:** la app informa si el número tiene WhatsApp; **no detecta spam**. La relación "sin WhatsApp ≈ spam" pasa de hipótesis a validar a contexto de uso.

## Pitch

WhatsCheck es una app móvil que, **en el momento en que entra una llamada**, verifica si el número que llama tiene una cuenta de WhatsApp y muestra un aviso discreto (burbuja flotante o notificación) **sin bloquear la pantalla de llamada**, para que el usuario decida en segundos si contestar o no.

La verificación la hace **la cuenta de WhatsApp del propio usuario**, vinculada a la app como un dispositivo más (igual que WhatsApp Web). Cada usuario consulta solo los números que lo llaman a él, así que la carga queda repartida y todo ocurre dentro de su teléfono.

## Problema

- Las llamadas de telemarketing, promociones, cobranzas agresivas y estafas son cada vez más frecuentes, y suelen venir de números desconocidos.
- Las apps identificadoras de spam tradicionales dependen de bases de datos colaborativas que **no cubren números nuevos** (los call centers rotan líneas constantemente).
- En Latinoamérica y otros mercados donde WhatsApp es casi universal, una persona o negocio real **casi siempre** tiene WhatsApp en su número. Las líneas masivas de call center (troncales SIP, números virtuales, rangos rotativos) **casi nunca** lo tienen.
- Hoy el usuario no tiene forma de saber esto antes de contestar; tendría que colgar, guardar el número y buscarlo en WhatsApp.

## Qué informa la app (y qué no)

La app responde un solo hecho: **¿el número que llama tiene WhatsApp?** No clasifica la llamada como spam ni calcula un nivel de riesgo. Qué hacer con ese dato lo decide el usuario.

Por qué es útil: mucha gente ya usa la falta de WhatsApp como pista de que una llamada puede ser publicidad o de una línea poco seria, pero hoy solo puede comprobarlo después de colgar. La app le da ese dato mientras suena el teléfono.

La app no presenta esa relación como una regla, porque tiene excepciones en ambos sentidos:

- Números legítimos **sin** WhatsApp: teléfonos fijos y centrales de empresas (bancos, clínicas, entidades de gobierno), personas mayores o sin smartphone, números internacionales.
- Llamadas comerciales **con** WhatsApp: muchas empresas usan WhatsApp Business en la misma línea (caso observado: una fintech de préstamos con cuenta Business).

Por eso el aviso dice *"Este número no tiene WhatsApp"*, nunca *"Esto es SPAM"*. **Decisión 2026-09-23:** no se valida estadísticamente la relación entre WhatsApp y spam, porque el producto no la afirma.

## Usuarios objetivo

| Persona | Descripción | Necesidad principal |
|---|---|---|
| **Usuario saturado** | Recibe 3+ llamadas comerciales por semana. Usa Android, WhatsApp a diario. | Decidir en 2 segundos si contesta, sin abrir otra app. |
| **Profesional independiente** | Recibe llamadas legítimas de números desconocidos (clientes nuevos). No puede ignorar todo lo desconocido. | Distinguir un cliente potencial de un call center. |
| **Familiar protector** | Configura el teléfono de padres/abuelos. | Una señal simple y visible que reduzca el riesgo de estafas telefónicas. |

**Requisito para todos:** tener WhatsApp (o WhatsApp Business) instalado en el mismo teléfono Android y aceptar vincularlo a la app.

## Propuesta de valor

1. **Tiempo real:** la señal aparece mientras suena el teléfono, no después.
2. **No intrusiva:** nunca tapa los botones de contestar/colgar; se puede descartar o mover.
3. **Cero configuración de listas:** funciona con números nunca vistos antes, sin depender de reportes de otros usuarios.
4. **Todo en el teléfono:** no hay servidor propio. El número entrante solo se consulta a WhatsApp, desde la cuenta del propio usuario.
5. **Gratis de operar:** no hay costo por consulta, porque cada usuario usa su propia cuenta.

## Funcionalidades clave (visión)

### Núcleo
- **Vinculación de WhatsApp** mediante código de emparejamiento (*"Vincular con número de teléfono"*). No se puede usar QR, porque el usuario no puede escanear un código que se muestra en su propia pantalla.
- Detección de llamada entrante de número **no guardado en contactos**.
- Verificación "¿tiene WhatsApp?" desde la sesión vinculada, con resultado en < 3 s.
- Aviso flotante (overlay) pequeño y arrastrable **o** notificación heads-up, según preferencia del usuario.
- Estados claros: ⏳ *Verificando…* · ✅ *Tiene WhatsApp* · ⚠️ *No tiene WhatsApp* · ❔ *No se pudo verificar*.
- Detección de sesión perdida (desvinculada o baneada) con aviso para volver a vincular.

### Complementarias
- Caché local de resultados para no volver a consultar el mismo número.
- Historial de llamadas verificadas con su resultado.
- Acción rápida post-llamada: "Abrir chat de WhatsApp" (si tiene) o "Marcar como no deseado" (lista negra personal).
- Lista blanca / lista negra personal.
- Opción avanzada (opt-in): silenciar o rechazar automáticamente llamadas sin WhatsApp.

## Qué NO es este producto (fuera de alcance)

Ver los apartados de **Minimización de Datos** y **Comportamiento Pasivo** en [constitution.md](constitution.md).

## Principios de diseño

Están definidos como reglas estrictas en el apartado **Principios de Experiencia de Usuario (UX)** de [constitution.md](constitution.md).

## Métricas de éxito

| Métrica | Objetivo MVP |
|---|---|
| Latencia p95 desde que suena hasta que se muestra el resultado | < 3 s |
| Tasa de verificaciones exitosas (no "no verificado") | > 90 % |
| **Cuentas de usuarios restringidas o baneadas** | **0** en la beta (cualquier caso detiene el despliegue) |
| Sesiones que se desvinculan solas por mes | < 5 % |
| Retención a 30 días | > 40 % |
| Crash-free sessions | > 99.5 % |

## Diferenciadores frente a Truecaller / Hiya / identificadores del sistema

- No requiere que alguien más haya reportado el número: funciona desde la primera llamada de una línea nueva.
- No exige subir la agenda de contactos del usuario a ningún servidor.
- No hay servidor central: sin costos por consulta y sin base de datos de números de terceros.
- Complementario, no sustituto: puede convivir con el identificador de spam del sistema.

