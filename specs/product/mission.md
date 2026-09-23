# Misión del producto

> **Nombre provisional:** WhatsCheck
> **Estado:** Borrador v0.2 · 2026-09-23
> **Cambios v0.2:** la verificación se hace desde el WhatsApp del propio usuario, vinculado como dispositivo; se elimina la dependencia de un backend o proveedor externo.

## Pitch

WhatsCheck es una app móvil que, **en el momento en que entra una llamada**, verifica si el número que llama tiene una cuenta de WhatsApp y muestra un aviso discreto (burbuja flotante o notificación) **sin bloquear la pantalla de llamada**, para que el usuario decida en segundos si contestar o no.

La verificación la hace **la cuenta de WhatsApp del propio usuario**, vinculada a la app como un dispositivo más (igual que WhatsApp Web). Cada usuario consulta solo los números que lo llaman a él, así que la carga queda repartida y todo ocurre dentro de su teléfono.

## Problema

- Las llamadas de telemarketing, promociones, cobranzas agresivas y estafas son cada vez más frecuentes, y suelen venir de números desconocidos.
- Las apps identificadoras de spam tradicionales dependen de bases de datos colaborativas que **no cubren números nuevos** (los call centers rotan líneas constantemente).
- En Latinoamérica y otros mercados donde WhatsApp es casi universal, una persona o negocio real **casi siempre** tiene WhatsApp en su número. Las líneas masivas de call center (troncales SIP, números virtuales, rangos rotativos) **casi nunca** lo tienen.
- Hoy el usuario no tiene forma de saber esto antes de contestar; tendría que colgar, guardar el número y buscarlo en WhatsApp.

## Hipótesis central

> *"Un número desconocido que no tiene WhatsApp tiene una probabilidad significativamente mayor de ser publicidad, spam o una línea poco confiable."*

Es una **señal heurística, no una verdad absoluta**. Hay excepciones legítimas y frecuentes:

- Teléfonos fijos y centrales de empresas (bancos, clínicas, entidades de gobierno).
- Personas mayores o usuarios sin smartphone.
- Números internacionales o de países donde WhatsApp no es dominante.

Y también **falsos negativos**: muchas empresas que hacen llamadas comerciales usan WhatsApp Business en la misma línea (caso observado: una fintech de préstamos con cuenta Business). La señal no las detecta.

Por eso el producto **informa, no juzga**: el aviso dice *"Este número no tiene WhatsApp"*, no *"Esto es SPAM"*. Validar la hipótesis con datos reales es parte del roadmap (Fase 0 y métricas).

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
- Acción rápida post-llamada: "Abrir chat de WhatsApp" (si tiene) o "Marcar como spam" (feedback propio).
- Lista blanca / lista negra personal.
- Opción avanzada (opt-in): silenciar o rechazar automáticamente llamadas sin WhatsApp.

## Qué NO es este producto (fuera de alcance)

- No es un bloqueador agresivo por defecto: nunca rechaza llamadas sin consentimiento explícito.
- **No lee, guarda, muestra ni envía mensajes de WhatsApp.** La sesión vinculada se usa solo para consultar si un número existe. Cualquier mensaje que llegue a la sesión se descarta sin procesarlo.
- **No altera el WhatsApp del usuario:** no lo muestra "en línea", no envía confirmaciones de lectura y no sincroniza el historial de chats.
- No muestra foto de perfil, nombre, "última conexión" ni ningún otro dato del perfil de WhatsApp del tercero. Solo el booleano "tiene / no tiene" (ver decisión abierta sobre cuentas Business en el roadmap).
- No es un marcador (dialer) de reemplazo.

## Principios de diseño

1. **Nunca estorbar la llamada.** Si hay duda entre mostrar algo o no molestar, no molestar.
2. **Fallar en silencio.** Sin red, sin sesión o con timeout → estado "no verificado", nunca un falso "sin WhatsApp".
3. **Lo del usuario se queda en su teléfono.** Las credenciales de la sesión vinculada se cifran con Android Keystore y nunca salen del dispositivo. No hay servidor que las reciba.
4. **Transparencia sobre el riesgo.** Antes de vincular, el usuario lee en lenguaje claro que la app usa un método **no oficial** de WhatsApp y que existe el riesgo de que su cuenta sea restringida o suspendida. Solo continúa si lo acepta de forma explícita.
5. **Honestidad en la señal.** El texto del aviso describe un hecho, no un veredicto.

## Métricas de éxito

| Métrica | Objetivo MVP |
|---|---|
| Latencia p95 desde que suena hasta que se muestra el resultado | < 3 s |
| Tasa de verificaciones exitosas (no "no verificado") | > 90 % |
| Precisión de la hipótesis: % de números "sin WhatsApp" que el usuario marca como spam/no deseado | > 70 % |
| **Cuentas de usuarios restringidas o baneadas** | **0** en la beta (cualquier caso detiene el despliegue) |
| Sesiones que se desvinculan solas por mes | < 5 % |
| Retención a 30 días | > 40 % |
| Crash-free sessions | > 99.5 % |

## Diferenciadores frente a Truecaller / Hiya / identificadores del sistema

- No requiere que alguien más haya reportado el número: funciona desde la primera llamada de una línea nueva.
- No exige subir la agenda de contactos del usuario a ningún servidor.
- No hay servidor central: sin costos por consulta y sin base de datos de números de terceros.
- Complementario, no sustituto: puede convivir con el identificador de spam del sistema.
