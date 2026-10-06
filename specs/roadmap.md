# Roadmap

> **Estado:** Borrador v0.3 · 2026-09-23
> Referencias: [mission.md](mission.md) · [tech-stack.md](tech-stack.md) · [intentos-fallidos.md](intentos-fallidos.md) · [CHANGELOG](../../CHANGELOG.md)
> Cada ítem marcado `[ ]` se convierte en su propia spec (`specs/AAAA-MM-DD-<nombre>/` con `spec.md`, `plan.md` y `checklist.md`) antes de implementarse. Usar la skill `/roadmap-spec`.
> **Cambios v0.2:** verificación con el WhatsApp del propio usuario, dentro del teléfono. Sin backend. iOS descartado.
> **Cambios v0.3:** la app informa si el número tiene WhatsApp; no detecta spam. Se descartan los ítems que clasificaban llamadas (validación de la hipótesis, feedback de precisión, señales combinadas, reputación colaborativa).

---

## Fase 0 — Validación y spike técnico 🔬
**Objetivo:** confirmar que vincular el WhatsApp del usuario funciona, es rápido y **no pone en riesgo su cuenta**, antes de construir la app.
**Criterio de salida:** decisión documentada *Go / No-Go*, con motor elegido y datos de riesgo de baneo.

> ⚠️ Todas las pruebas de la Fase 0 se hacen con **números secundarios** (SIMs de prueba), nunca con cuentas personales.

- [x] ~~Confirmar el comportamiento de `onWhatsApp()`~~ — ya validado en un proyecto existente del autor: `[{ jid, exists: true }]` si existe, `[]` si no (ver contrato en [tech-stack.md](tech-stack.md#contrato-de-la-consulta-onwhatsapp-de-baileys)).
- [x] **Prototipo de escritorio con Baileys** — script Node que vincula **con código de emparejamiento** (no QR) y consulta una lista de números; mide la latencia de conexión en frío + consulta. `S` → [spec](../2026-09-23-prototipo-baileys-escritorio/)
  - *Resultado (2026-09-23):* vinculación por código ✅; conexión en frío + consulta p95 **2,36 s** → Go "bajo demanda" en escritorio. Hallazgos: IF-007 a IF-010.
- ~~**Validación de la hipótesis**~~ — *Descartado 2026-09-23:* la app informa si el número tiene WhatsApp y no afirma que sea spam, así que no hace falta validar esa relación. → [spec descartada](../2026-09-23-validacion-hipotesis/)
- [x] **Comparar motores en Android** — F1 (Baileys + nodejs-mobile) vs F4 (whatsmeow + gomobile); F2 (Cobalt) descartado por análisis (IF-011): tamaño del APK, latencia de conexión en frío + consulta, soporte de pairing code, mantenimiento. `L` → [spec](../2026-09-23-comparar-motores-android/)
  - *Resultado (2026-09-23):* **whatsmeow elegido.** Vinculación por código ✅; total en frío p95 2,15 s con 4G (Poco M5s) y 2,31 s con wifi (Moto G53), 0/40 sin verificar; +17,2 MB al APK, alineado a 16 KB; se muestra como "Google Chrome (WhatsCheck)". Cobalt y Baileys + `nodejs-mobile` descartados (IF-011, IF-013).
  - *Del prototipo de escritorio:* medir también el **arranque del motor** (runtime + carga de la librería). En escritorio, solo cargar Baileys tarda 1,4–1,9 s, además de los ~1,6 s (p50) de conectar y consultar.
- [x] **Efectos secundarios en el WhatsApp principal** — con la sesión vinculada: ¿el usuario aparece "en línea"?, ¿se silencian las notificaciones del teléfono?, ¿llegan confirmaciones de lectura? Todo debe dar "no". `S` → [spec](../2026-09-23-efectos-whatsapp-principal/)
  - *Resultado (2026-09-23):* con whatsmeow conectado no aparece "en línea", los mensajes quedan en doble check gris, las notificaciones llegan y siguen sin leer; el texto no queda en la base. Además, la sesión dejó de guardar la agenda, los ajustes de chats y los secretos de mensajes. Montaje: IF-014.
- [x] **Spike de `CallScreeningService` + overlay** — responder de inmediato, mostrar "Verificando…" y actualizar el aviso; probar en los teléfonos del autor (Xiaomi Poco M5s con MIUI y Moto G53) sin tapar los botones de la llamada. `M` → [spec](../2026-09-24-spike-llamadas-overlay/)
  - *Resultado (2026-10-06):* validado con 49 llamadas reales en 13 días (Poco M5s con MIUI 14). Overlay 100% visible incluso bloqueado (79,6% de llamadas). Latencia p50 2,04 s / p95 3,32 s en verificadas. 0 bloqueos ni restricciones. Ajuste de timeout de overlay (4 s) y red activa (`isOnline`) para no demorar en fallos de DNS ([validación](../2026-09-24-spike-llamadas-overlay/checklist.md)).
  - *Diseño de la app de pruebas (2026-09-24, pedido del autor):* Inicio / Registro / Ajustes, ícono propio y tema claro/oscuro → [spec](../2026-09-24-ui-app-pruebas/). Adelanta en versión de prueba la vinculación guiada y los ajustes de la Fase 1 y el historial de la Fase 2.
- [x] **Posicionamiento central del aviso flotante** — centrado en pantalla (`Gravity.CENTER`) para no tapar número ni botones, ni colisionar con el banner de llamada. `XS` → [spec](../2026-10-06-posicionamiento-overlay/)
- [ ] **Prueba de resistencia al baneo** — **uso real** durante 3–4 semanas: la app del spike instalada en el teléfono donde el autor recibe llamadas (Poco M5s), vinculada al **único número secundario** disponible; cada llamada de un desconocido genera una consulta. Registrar consultas, desconexiones y cualquier restricción. Con una sola cuenta es un indicio, no una estadística. `L`
  - *Reordenado 2026-09-24 (autor):* va después del spike, porque simular consultas (números fijos o al azar) no representa el uso real, y consultar números al azar sería rastrear a terceros.
  - *Evidencia previa:* el autor usa Baileys con su **número personal** en otro proyecto (sesión persistente en servidor, envía mensajes automáticos y llama a `onWhatsApp()` solo antes de escribir a un contacto nuevo). Ese patrón de consultas es parecido al de esta app. Diferencia clave a probar: aquí la sesión **se conecta y desconecta en cada llamada**, y reconectar seguido podría ser una señal propia de detección.
- [ ] **Revisión legal y de distribución** — términos de servicio de WhatsApp, protección de datos (p. ej. Ley 29733 en Perú), política de Google Play; elegir canal de distribución. `M`

---

## Fase 1 — MVP (Android) 🚀
**Objetivo:** un usuario instala la app, vincula su WhatsApp y ve el aviso en su próxima llamada desconocida.
**Criterio de salida:** beta cerrada con 20–50 usuarios durante 4 semanas, **cero cuentas restringidas** y latencia p95 < 3 s.

### Vinculación y sesión
- [ ] **Pantalla de consentimiento de riesgo** — explica en lenguaje claro que el método no es oficial y que la cuenta puede ser restringida; exige aceptación explícita. `S`
- [ ] **Vinculación por código de emparejamiento** — el usuario ingresa su número, la app muestra el código de 8 caracteres y guía los pasos: *WhatsApp → Dispositivos vinculados → Vincular con número de teléfono*. `M`
  - *De los prototipos:* una vinculación sin terminar deja credenciales que provocan 401 (IF-008): borrarlas. El usuario vincula desde el mismo teléfono: **copiar el código al portapapeles** automáticamente. Con whatsmeow el dispositivo aparece como "Google Chrome (WhatsCheck)" (IF-009).
- [ ] **Motor `WhatsAppPresenceEngine`** con la implementación elegida en la Fase 0 y la configuración obligatoria (sin "en línea", sin historial, sin confirmaciones de lectura, mensajes descartados). `L`
- [ ] **Almacenamiento cifrado de credenciales** con Android Keystore, excluido del backup. `S`
- [ ] **Salud de la sesión** — reconexión periódica con WorkManager; detectar desvinculación o baneo y avisar al usuario. `M`
- [ ] **Desvincular y borrar todo** desde ajustes. `S`

### Llamadas y aviso
- [ ] **Onboarding de permisos** — rol de call screening, overlay (opcional), notificaciones y guía por fabricante. `M`
- [ ] **Screening de llamada** — `respondToCall(allow)` inmediato → normalización E.164 → caché local → motor, con timeout. `M`
- [ ] **Aviso flotante** — burbuja compacta, arrastrable; "Verificando…" → resultado; se cierra al contestar/colgar o con un toque. `M`
- [ ] **Notificación heads-up** como alternativa cuando no hay permiso de overlay. `S`
- [ ] **Caché local** en Room (TTL: 7 días "tiene WhatsApp", 24 h "no tiene"). `S`
- [ ] **Pantalla de ajustes** — modo de aviso (flotante / notificación / ambos), activar/desactivar. `S`

### Operación
- [ ] **Kill switch y versión mínima** con Firebase Remote Config. `S`
- [ ] **Despliegue gradual** por porcentaje de instalaciones. `S`
- [ ] Crashlytics + métricas anónimas (latencia, resultado, sesiones perdidas). `S`
- [ ] **Canal de distribución y actualizador** según la decisión de la Fase 0. `M`

---

## Fase 2 — Utilidad diaria 📋
**Objetivo:** que la app sea útil también después de la llamada y aprenda de las decisiones del usuario.

- [ ] **Historial** de llamadas verificadas con su resultado y fecha. `M`
- [ ] **Acciones post-llamada** — "Abrir chat en WhatsApp", "Marcar como no deseado", "Es de confianza" (alimentan las listas personales). `S`
- [ ] **Lista blanca / lista negra** personal que tiene prioridad sobre la verificación. `S`
- ~~**Feedback de precisión**~~ — *Descartado 2026-09-23:* dependía de la validación de la hipótesis.
- [ ] **Personalización del aviso** — posición, tamaño, tema claro/oscuro, duración. `S`
- [ ] **Soporte multi-SIM** — mostrar en qué línea entra la llamada. `S`

---

## Fase 3 — Protección activa 🛡️
**Objetivo:** pasar de informar a actuar, siempre con consentimiento explícito.

- [ ] **Auto-silenciar** llamadas sin WhatsApp (opt-in; espera el resultado hasta 3 s antes de responder). `S`
- [ ] **Auto-rechazar** llamadas sin WhatsApp (opt-in, con resumen diario de lo bloqueado). `M`
- ~~**Señales combinadas**~~ — *Descartado 2026-09-23:* calculaba un nivel de riesgo, es decir, clasificaba la llamada como spam, y eso queda fuera del enfoque de la app.
- [ ] **Reglas por horario** — p. ej. silenciar números sin WhatsApp de noche. `S`
- [ ] **Widget / tile de ajustes rápidos** para pausar la protección. `S`

---

## Fase 4 — Crecimiento 🌎

- [ ] **Monetización** — sin costo por consulta, así que encaja un pago único o una versión "Pro" (protección activa, historial ilimitado) en lugar de cobrar por uso. `M`
- [ ] **Localización** — español, portugués, inglés. `S`
- [ ] **Código abierto** del motor y del manejo de la sesión, para generar confianza (la app tiene acceso técnico a la cuenta del usuario). `M`
- ~~**Reputación colaborativa opcional**~~ — *Descartado 2026-09-23:* reportes de spam entre usuarios; queda fuera del enfoque, y además requeriría un backend.

---

## Leyenda de esfuerzo

| Tamaño | Estimación |
|---|---|
| `XS` | ≤ 1 día |
| `S` | 2–3 días |
| `M` | 1 semana |
| `L` | 2 semanas |
| `XL` | 3+ semanas |

## Decisiones abiertas

1. ¿Conexión bajo demanda o persistente? → depende de la latencia medida en la **Fase 0**. *Avance 2026-09-23:* en escritorio, conectar en frío + consultar tarda p95 **2,36 s** (Go "bajo demanda", [validación](../2026-09-23-prototipo-baileys-escritorio/checklist.md)). *Android (whatsmeow, 2026-09-23):* p95 **2,15 s** con 4G en el Poco M5s y 2,31 s con wifi en el Moto G53, contando el arranque del motor ([comparación](../2026-09-23-comparar-motores-android/checklist.md)). La latencia ya no es un obstáculo para "bajo demanda"; falta ver el riesgo de bloqueo de reconectar seguido.
2. ¿Canal de distribución: Google Play, APK propio o F-Droid? → **Fase 0**.
3. ¿Mercado de lanzamiento inicial (país)? Afecta el marco legal y el país por defecto para normalizar números.
4. ¿Mostrar el nombre de la empresa cuando el número es una cuenta WhatsApp Business? Ayudaría con el caso de empresas comerciales que sí tienen WhatsApp, pero choca con el principio de no mostrar datos del perfil.

## Decisiones tomadas

| Fecha | Decisión | Motivo |
|---|---|---|
| 2026-09-23 | Solo Android | iOS no permite ejecutar un cliente en segundo plano ni overlays |
| 2026-09-23 | `wa.me` descartado | Solo distingue cuentas Business; una cuenta personal y un número inexistente se ven igual |
| 2026-09-23 | Verificación con el WhatsApp del propio usuario, dentro del teléfono | Reparte la carga entre usuarios, sin servidor ni costos, y las credenciales no salen del teléfono |
| 2026-09-23 | Sin backend en el MVP | La sesión corre en el teléfono; solo se usa Remote Config como kill switch |
| 2026-09-23 | La app informa si el número tiene WhatsApp; no detecta spam. Se descartan la validación de la hipótesis, el feedback de precisión, las señales combinadas y la reputación colaborativa | Decisión del autor: el producto no afirma que "sin WhatsApp" signifique spam, así que no necesita demostrarlo |
| 2026-09-23 | **Motor: whatsmeow (Go) vía gomobile** | Go en todos los criterios en Android (p95 2,15 s con 4G, 0/20 fallos, +17,2 MB, 16 KB, nombre propio visible). Cobalt y Baileys + `nodejs-mobile` descartados (IF-011, IF-013) |
| 2026-09-23 | La vinculación por código de emparejamiento funciona con Baileys 7 | Validado en el prototipo de escritorio con un número secundario |
| 2026-09-23 | Se mantiene desactivada la sincronización de historial | `onWhatsApp` no la necesita; sin errores de sesión en ~30 conexiones. Se reevalúa en Android |
