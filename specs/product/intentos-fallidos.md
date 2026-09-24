# Intentos fallidos y enfoques descartados

Registro de lo que **se probó y no funcionó**, o se descartó tras analizarlo, para no repetirlo. Antes de proponer un enfoque, una librería o una solución a un bug, busca aquí primero.

- Una entrada por intento, la más reciente arriba. **Nunca se borran**: si algo cambia, se agrega una nota al final de la entrada.
- **Tipo:** `Probado` (se ejecutó y falló) o `Descartado por análisis` (no se ejecutó; se descartó por documentación, políticas o razonamiento).
- **Reconsiderar si** es obligatorio: define qué tendría que cambiar para que valga la pena volver a intentarlo.

## Índice

| ID | Fecha | Tema | Tipo |
|---|---|---|---|
| [IF-010](#if-010--salir-sin-esperar-a-que-baileys-cierre-el-websocket) | 2026-09-23 | Salir sin esperar a que Baileys cierre el WebSocket | Probado |
| [IF-009](#if-009--vincular-con-un-sistema-operativo-inventado-en-browser) | 2026-09-23 | Vincular con un sistema operativo inventado en `browser` | Probado (no concluyente) |
| [IF-008](#if-008--reintentar-la-vinculación-con-credenciales-de-un-intento-fallido) | 2026-09-23 | Reintentar la vinculación con credenciales de un intento fallido | Probado |
| [IF-007](#if-007--usar-51000000001-como-número-inexistente-tal-cual) | 2026-09-23 | Usar `51000000001` como número inexistente "tal cual" | Probado |
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

## IF-010 — Salir sin esperar a que Baileys cierre el WebSocket

- **Fecha:** 2026-09-23
- **Tipo:** Probado
- **Contexto:** Cierre de la sesión al terminar `check`/`bench` en el prototipo ([spec](../2026-09-23-prototipo-baileys-escritorio/)). Windows 11, Node 24.19.0, `@whiskeysockets/baileys` 7.0.0-rc14.
- **Qué se intentó:** (1) Llamar a `sock.end(undefined)` sin esperar y luego `process.exit()`. (2) Esperar el evento `sock.ws.once('close')` antes de salir.
- **Resultado:** (1) Una de ~25 ejecuciones abortó al salir con `Assertion failed: !(handle->flags & UV_HANDLE_CLOSING), file src\win\async.c, line 94`. (2) El evento nunca llegaba y siempre se esperaba el tiempo máximo (2 s).
- **Por qué no sirve:** (1) El proceso salía mientras libuv todavía cerraba el socket. (2) `end()` de Baileys ejecuta `ws.removeAllListeners('close')` antes de cerrar, así que borra los listeners externos.
- **Evidencia:** Salida de consola del autor (2026-09-23); `node_modules/@whiskeysockets/baileys/lib/Socket/socket.js`, función `end` (~línea 472).
- **En su lugar:** `await sock.end(undefined)`: Baileys ya espera el `close` del WebSocket por dentro. Tarda 0,2–0,3 s. Se deja un tope de 2 s por seguridad (`closeSocket` en `src/session.ts`).
- **Reconsiderar si:** Baileys deja de esperar el cierre dentro de `end()`.

## IF-009 — Vincular con un sistema operativo inventado en `browser`

- **Fecha:** 2026-09-23
- **Tipo:** Probado
- **Contexto:** Requisito de tech-stack §2: "nombre de dispositivo visible y honesto (p. ej. 'WhatsCheck')". Prototipo de escritorio ([spec](../2026-09-23-prototipo-baileys-escritorio/)).
- **Qué se intentó:** `browser: ['WhatsCheck', 'Chrome', '0.1.0']` en `makeWASocket`, con `requestPairingCode()`. Baileys envía `companion_platform_display: "Chrome (WhatsCheck)"` y usa `WhatsCheck` como sistema operativo en el registro. `@whiskeysockets/baileys` 7.0.0-rc14.
- **Resultado:** Al ingresar el código, el teléfono respondió *"No se pudo vincular el dispositivo. Comprueba que el número de teléfono es correcto en tu dispositivo u obtén un código nuevo"*. En el intento siguiente, con el mismo número y `Browsers.macOS('Chrome')` (valor por defecto de Baileys), la vinculación funcionó a la primera.
- **Por qué no sirve:** WhatsApp parece validar que el sistema operativo sea uno conocido en la vinculación por código. No hay documentación oficial y solo se probó una vez, así que la causa es probable pero no está 100 % confirmada.
- **Evidencia:** Mensaje del teléfono reportado por el autor; `validation.md` de la spec, intentos 2 y 3 de CA-01.
- **En su lugar:** Identidad predefinida de Baileys (`--browser macos`, por defecto). El dispositivo aparece con un nombre genérico, no como "WhatsCheck".
- **Reconsiderar si:** Se necesita un nombre propio visible: probar `['Mac OS', 'WhatsCheck', …]` o variantes con un sistema operativo real. Ojo: el segundo valor define el `platform id` y debe ser un navegador que WhatsApp reconozca.
- **Nota 2026-09-23:** El autor informa que otro proyecto suyo con Baileys se muestra en *Dispositivos vinculados* con un nombre propio en la posición del sistema operativo, y funciona. Eso **debilita la conclusión** de esta entrada: el fallo pudo deberse a `'Chrome'` o a la versión `'0.1.0'`, o a otra causa del primer intento. Se prueba `['WhatsCheck', 'Google Chrome', '1.0.0']` (`--browser whatscheck`).
- **Nota 2026-09-23 (resultado):** Con `['WhatsCheck', 'Google Chrome', '1.0.0']` la vinculación por código **funcionó**, pero el teléfono muestra el dispositivo como **"Google Chrome (Mac OS)"**, no con "WhatsCheck". Conclusión provisional: (1) un sistema operativo inventado **no** impide vincular, así que el fallo original pudo tener otra causa o deberse a `'Chrome'`/`'0.1.0'`; (2) con código de emparejamiento, el nombre visible no sale de `browser` (Baileys envía `companion_platform_display: "Google Chrome (WhatsCheck)"` y `DeviceProps.os: "WhatsCheck"`). El otro proyecto del autor, que sí muestra su nombre propio, se vinculó **por QR**: con QR WhatsApp usa el sistema operativo que envía el cliente; con código, no.

## IF-008 — Reintentar la vinculación con credenciales de un intento fallido

- **Fecha:** 2026-09-23
- **Tipo:** Probado
- **Contexto:** Primera prueba real de `link` con el número secundario ([spec](../2026-09-23-prototipo-baileys-escritorio/)).
- **Qué se intentó:** Un primer `link` que mostró el código pero no completó el emparejamiento, y luego tres `link` seguidos con la misma carpeta `.auth/`. Baileys `@whiskeysockets/baileys` 7.0.0-rc14.
- **Resultado:** Los reintentos terminaban de inmediato con `401` ("sesión cerrada desde el teléfono"), sin mostrar código. `.auth/creds.json` tenía `registered: false`, `me: { name: "~" }` y sin `account`.
- **Por qué no sirve:** `requestPairingCode()` guarda `creds.me` antes de que el teléfono acepte el código. Al reconectar, Baileys ve `creds.me` e intenta **iniciar sesión** (`generateLoginNode`) en lugar de **registrarse** (`generateRegistrationNode`), y WhatsApp rechaza con 401 un dispositivo que nunca se emparejó. Además, `registered` no basta para saber si el emparejamiento terminó: Baileys lo pone en `true` cuando el teléfono acepta el código, pero `account` solo llega con `pair-success`.
- **Evidencia:** Salida de consola del autor; `node_modules/@whiskeysockets/baileys/lib/Socket/socket.js` (líneas ~320 y ~602) y `Socket/messages-recv.js` (~940).
- **En su lugar:** `link` borra las credenciales incompletas antes de empezar y también si la vinculación falla. Una sesión cuenta como vinculada solo si tiene `registered: true` **y** `account` (`isPaired` en `src/session.ts`). La app Android debe aplicar la misma regla.
- **Reconsiderar si:** Baileys cambia el flujo y deja de guardar `creds.me` en `requestPairingCode()`.

## IF-007 — Usar `51000000001` como número inexistente "tal cual"

- **Fecha:** 2026-09-23
- **Tipo:** Probado
- **Contexto:** Normalización a E.164 en el prototipo ([spec](../2026-09-23-prototipo-baileys-escritorio/)); el número de prueba "sin WhatsApp" de CA-04.
- **Qué se intentó:** Normalizar `51000000001` con `libphonenumber-js` 1.13.13 (país por defecto PE), primero como nacional y luego como internacional (`+51000000001`).
- **Resultado:** Siempre devuelve `+5100000001`: un dígito menos. También con `+` y también con los metadatos `max`, que además dicen `isValid() = false`.
- **Por qué no sirve:** libphonenumber trata el `0` después del código de país como prefijo nacional y lo quita. En Perú ningún número real empieza con `0` después del `51`, así que **solo afecta a números imposibles**; las llamadas reales no cambian. La versión de Java (Android) aplica la misma regla.
- **Evidencia:** `prototypes/baileys-cli/src/phone.test.ts`, test *"libphonenumber quita el 0 después del código de país"*.
- **En su lugar:** Se acepta el comportamiento. `51000000001` sigue sirviendo como número inexistente en CA-04, pero se consulta como `5100000001`, que tampoco existe. No hay que intentar "corregir" el orden de interpretación (nacional o internacional): el resultado es el mismo.
- **Reconsiderar si:** Aparece un número real que la normalización altere; en ese caso, enviar a `onWhatsApp` los dígitos crudos cuando la entrada ya es internacional.

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
