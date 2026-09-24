# Requisitos — Efectos secundarios en el WhatsApp principal

> **Estado:** Validado · **Fecha:** 2026-09-23
> **Ítem del roadmap:** Fase 0 — Efectos secundarios en el WhatsApp principal (`S`)
> **Relacionado:** [plan.md](plan.md) · [validation.md](validation.md) · [roadmap](../product/roadmap.md) · [tech-stack §2](../product/tech-stack.md#configuración-obligatoria-de-la-sesión)

## Objetivo

Comprobar con WhatsApp real que la sesión vinculada de whatsmeow **no cambia nada de lo que ven los contactos del usuario ni de cómo se comporta su WhatsApp**, y que la base local no guarda mensajes. Es la condición de la misión *"No altera el WhatsApp del usuario"* y *"No lee, guarda, muestra ni envía mensajes"*.

## Contexto

- Motor elegido: whatsmeow ([comparación](../2026-09-23-comparar-motores-android/)). Por código nunca llama a `SendPresence` ni a `MarkRead` y no descarga el historial (`ManualHistorySyncDownload`). Eso es **revisión de código**; falta comprobar el comportamiento real.
- Riesgos conocidos de un dispositivo vinculado:
  1. Que el usuario aparezca **"en línea"** para sus contactos.
  2. Que el teléfono principal **deje de mostrar notificaciones** mientras hay un dispositivo "activo" (WhatsApp lo hace con WhatsApp Web).
  3. Que los mensajes se marquen **leídos** (doble check azul) o que el chat deje de figurar como no leído en el teléfono.
  4. Que la base local guarde **datos de más**. El store de whatsmeow tiene tablas `whatsmeow_contacts`, `whatsmeow_chat_settings`, `whatsmeow_event_buffer`, `whatsmeow_retry_buffer` y `whatsmeow_message_secrets`, además de las de sesión.
- **Montaje disponible:** el número secundario, vinculado con whatsmeow en el Poco M5s, y como **observador** el número personal del autor en WhatsApp Web en su PC.
- Durante una llamada real la sesión estaría conectada ~2 s. Para poder observar, la prueba la mantiene conectada **60 s** (el peor caso) y también repite conexiones cortas como las reales.

## Historias de usuario

- Como usuario de WhatsCheck, quiero que mis contactos no me vean "en línea" por culpa de la app, y que mi teléfono me siga avisando de los mensajes, para que la app sea invisible en mi WhatsApp.

## Requisitos funcionales

| ID | Requisito | Prioridad |
|---|---|---|
| RF-01 | Acción `hold` en la app de pruebas: conectar la sesión y mantenerla abierta N segundos (por defecto 60) sin consultar, registrando los eventos recibidos por tipo (sin contenido) en `actions.log`. | Obligatorio |
| RF-02 | Acción `stats`: listar las tablas de la base de whatsmeow con su cantidad de filas, sin mostrar su contenido. | Obligatorio |
| RF-03 | Comprobar en el dispositivo, sin copiar la base a la PC, si un texto de prueba conocido aparece en los archivos de la base (`whatsmeow.db`, `-wal`, `-shm`). | Obligatorio |
| RF-04 | Protocolo de observación reproducible (plan.md) con cuatro escenarios: línea base sin sesión conectada, sesión mantenida 60 s, 5 conexiones cortas como las reales, e inspección de la base. | Obligatorio |
| RF-05 | Si la tabla de contactos u otra se llena con datos personales, evaluar si whatsmeow permite evitarlo y documentar la decisión (minimización de datos). | Obligatorio |

## Requisitos no funcionales

| ID | Requisito | Métrica |
|---|---|---|
| RNF-01 | Observaciones reproducibles. | Mismo mensaje de prueba por escenario (`prueba WhatsCheck <escenario>`); se anota hora y lo observado |
| RNF-02 | La visibilidad de "en línea" del número secundario debe estar en **"Todos"** durante la prueba; si no, el resultado no sirve. | Se verifica y se anota antes de empezar |
| RNF-03 | Sin números ni textos personales en el repo. | Solo se registran observaciones (sí/no, tiempos) y conteos de filas |

## Principios de la misión que aplican

| Principio | Cómo se respeta en este feature |
|---|---|
| No altera el WhatsApp del usuario | Es lo que se verifica: presencia, notificaciones, confirmaciones de lectura, estado de no leído. |
| No lee, guarda, muestra ni envía mensajes | Se busca el texto de prueba en la base (RF-03) y se cuentan filas (RF-02). |
| Lo del usuario se queda en su teléfono | La inspección se hace en el dispositivo; la base no se copia a la PC. |

## Fuera de alcance

- Riesgo de bloqueo por uso sostenido: ítem *Prueba de resistencia al baneo*.
- Integración con llamadas reales: ítem *Spike de `CallScreeningService` + overlay*.
- Cifrado de la base con Keystore: Fase 1.

## Dependencias

- Sesión de whatsmeow vinculada en el Poco (lista) y conectado por USB.
- Teléfono con el WhatsApp del número secundario (el que recibe los mensajes), con la **app de WhatsApp cerrada** y la pantalla apagada durante cada escenario, para que no sea él quien marque "en línea" o lea.
- Observador: WhatsApp Web del número personal del autor.

## Preguntas abiertas

- [x] ¿Dónde está el WhatsApp del secundario? → En el **Moto G53** (app cerrada durante la prueba); la sesión de whatsmeow está en el **Poco M5s**. Privacidad de "Últ. vez y en línea" puesta en **Todos**. *(2026-09-23)*
