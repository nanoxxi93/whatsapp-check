# Intentos fallidos y enfoques descartados

Registro de lo que **se probó y no funcionó**, o se descartó tras analizarlo, para no repetirlo. Antes de proponer un enfoque, una librería o una solución a un bug, busca aquí primero.

- Una entrada por intento, la más reciente arriba. **Nunca se borran**: si algo cambia, se agrega una nota al final de la entrada.
- **Tipo:** `Probado` (se ejecutó y falló) o `Descartado por análisis` (no se ejecutó; se descartó por documentación, políticas o razonamiento).
- **Reconsiderar si** es obligatorio: define qué tendría que cambiar para que valga la pena volver a intentarlo.

## Índice

| ID | Fecha | Tema | Tipo |
|---|---|---|---|
| [IF-006](#if-006--vincular-whatsapp-con-código-qr) | 2026-09-23 | Vincular WhatsApp con código QR | Descartado por análisis |
| [IF-005](#if-005--sesiones-de-los-usuarios-en-un-backend) | 2026-09-23 | Sesiones de los usuarios en un backend | Descartado por análisis |
| [IF-004](#if-004--contacto-temporal--sincronización-de-whatsapp) | 2026-09-23 | Contacto temporal + sincronización de WhatsApp | Descartado por análisis |
| [IF-003](#if-003--backend-con-pool-de-números-propios) | 2026-09-23 | Backend con pool de números propios | Descartado por análisis |
| [IF-002](#if-002--whatsapp-business-cloud-api) | 2026-09-23 | WhatsApp Business Cloud API | Descartado por análisis |
| [IF-001](#if-001--verificar-con-wame) | 2026-09-23 | Verificar con `wa.me` | Probado |

---

## Plantilla

```markdown
## IF-NNN — <Título corto>

- **Fecha:** AAAA-MM-DD
- **Tipo:** Probado | Descartado por análisis
- **Contexto:** <Qué se buscaba resolver. Enlace a la spec o ítem del roadmap.>
- **Qué se intentó:** <Enfoque, librería, versión, configuración, comando.>
- **Resultado:** <Qué pasó realmente. Mensajes de error textuales si los hay.>
- **Por qué no sirve:** <La causa, no solo el síntoma.>
- **Evidencia:** <Comando, salida, enlace, commit o archivo.>
- **En su lugar:** <Qué se hizo o se recomienda. Enlace.>
- **Reconsiderar si:** <Qué tendría que cambiar para volver a intentarlo.>
```

---

## IF-006 — Vincular WhatsApp con código QR

- **Fecha:** 2026-09-23
- **Tipo:** Descartado por análisis
- **Contexto:** Vincular la cuenta del usuario como dispositivo ([tech-stack §2](tech-stack.md#2-cómo-se-verifica-si-un-número-tiene-whatsapp)).
- **Qué se intentó:** El flujo por QR, el habitual de WhatsApp Web y de Baileys.
- **Por qué no sirve:** La app corre en el mismo teléfono que WhatsApp; el usuario no puede escanear con su cámara un QR que está en su propia pantalla.
- **En su lugar:** Código de emparejamiento (*Vincular con número de teléfono*), `requestPairingCode()` en Baileys.
- **Reconsiderar si:** Se agrega un flujo de vinculación desde un segundo dispositivo (p. ej. una web de apoyo).

## IF-005 — Sesiones de los usuarios en un backend

- **Fecha:** 2026-09-23
- **Tipo:** Descartado por análisis
- **Contexto:** Dónde corre la sesión vinculada de cada usuario.
- **Qué se intentó:** Correr las sesiones de Baileys de todos los usuarios en un servidor propio.
- **Por qué no sirve:** El servidor tendría las llaves de todas las cuentas, con acceso técnico a sus mensajes. Además, todas las sesiones saldrían de la misma IP de datacenter, lo que facilita que Meta las detecte y las banee en masa.
- **En su lugar:** La sesión corre dentro del teléfono de cada usuario, con credenciales cifradas en Android Keystore.
- **Reconsiderar si:** Nunca por motivos de privacidad, salvo un cambio radical del modelo de producto.

## IF-004 — Contacto temporal + sincronización de WhatsApp

- **Fecha:** 2026-09-23
- **Tipo:** Descartado por análisis
- **Contexto:** Verificar sin cliente no oficial, usando la app oficial de WhatsApp del teléfono.
- **Qué se intentó:** Insertar el número entrante como contacto y leer si WhatsApp crea un *raw contact* `com.whatsapp`.
- **Por qué no sirve:** WhatsApp sincroniza los contactos cuando quiere (minutos u horas), no bajo demanda. Además ensucia la agenda del usuario y requiere `READ_CONTACTS`/`WRITE_CONTACTS`.
- **En su lugar:** `onWhatsApp()` desde la sesión vinculada.
- **Reconsiderar si:** Aparece una forma documentada de forzar la sincronización, o se acepta una verificación posterior a la llamada, no en tiempo real.

## IF-003 — Backend con pool de números propios

- **Fecha:** 2026-09-23
- **Tipo:** Descartado por análisis
- **Contexto:** Verificar los números desde un servicio central.
- **Qué se intentó:** Un backend con Baileys y varias SIMs propias que atienden las consultas de todos los usuarios (similar a lo que hacen los proveedores comerciales).
- **Por qué no sirve:** Todo el volumen cae en pocas cuentas que consultan muchos números desconocidos, un patrón típico de baneo. Hay que comprar y rotar SIMs, y el costo crece con los usuarios.
- **En su lugar:** Cada usuario verifica con su propia cuenta, así el volumen por cuenta es mínimo.
- **Reconsiderar si:** La vinculación de la cuenta del usuario resulta inviable (baneos en la Fase 0 o rechazo de los usuarios).

## IF-002 — WhatsApp Business Cloud API

- **Fecha:** 2026-09-23
- **Tipo:** Descartado por análisis
- **Contexto:** Buscar una forma oficial de saber si un número tiene WhatsApp.
- **Qué se intentó:** Revisar la API oficial de WhatsApp Business (Cloud API).
- **Por qué no sirve:** No expone un endpoint para consultar si un número arbitrario tiene WhatsApp. El endpoint `/contacts` de la antigua API on-premise fue retirado.
- **En su lugar:** Cliente no oficial (Baileys) con la cuenta del usuario.
- **Reconsiderar si:** Meta publica un endpoint oficial de verificación de números.

## IF-001 — Verificar con `wa.me`

- **Fecha:** 2026-09-23
- **Tipo:** Probado
- **Contexto:** Verificar sin backend y sin cliente no oficial, solo con la URL pública para iniciar chats.
- **Qué se intentó:** `GET https://wa.me/<número>` con user-agent de Android, comparando un número con WhatsApp Business contra uno inexistente (`51000000001`).
- **Resultado:** Ambos responden HTTP 200 y redirigen a `api.whatsapp.com/send/?phone=…` (~220 KB, ~1,7–1,9 s). El número Business muestra nombre, foto y `Business Account`; el inexistente muestra la página genérica *"Chat on WhatsApp with …"*.
- **Por qué no sirve:** Solo distingue cuentas **Business**. Una cuenta personal y un número sin WhatsApp devuelven la misma página genérica, y ese es justo el caso que la app necesita distinguir. WhatsApp solo informa que un número no existe dentro de la app o con sesión iniciada.
- **Evidencia:** Comparación con `curl -L` + `diff` de ambas páginas, sesión del 2026-09-23.
- **En su lugar:** `onWhatsApp()` desde la sesión vinculada ([tech-stack, contrato](tech-stack.md#contrato-de-la-consulta-onwhatsapp-de-baileys)).
- **Reconsiderar si:** Se quiere mostrar el **nombre de empresas** con cuenta Business (decisión abierta 5 del roadmap); para eso sí sirve.
- **Nota:** No se confirmó con un número personal real que la página sea idéntica a la de un número inexistente; es el comportamiento esperado.
