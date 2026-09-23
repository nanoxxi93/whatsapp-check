# Roadmap

> **Estado:** Borrador v0.2 · 2026-09-23
> Referencias: [mission.md](mission.md) · [tech-stack.md](tech-stack.md) · [intentos-fallidos.md](intentos-fallidos.md) · [CHANGELOG](../../CHANGELOG.md)
> Cada ítem marcado `[ ]` se convierte en su propia spec (`specs/AAAA-MM-DD-<nombre>/` con `requirements.md`, `plan.md` y `validation.md`) antes de implementarse. Usar la skill `/roadmap-spec`.
> **Cambios v0.2:** verificación con el WhatsApp del propio usuario, dentro del teléfono. Sin backend. iOS descartado.

---

## Fase 0 — Validación y spike técnico 🔬
**Objetivo:** confirmar que vincular el WhatsApp del usuario funciona, es rápido y **no pone en riesgo su cuenta**, antes de construir la app.
**Criterio de salida:** decisión documentada *Go / No-Go*, con motor elegido y datos de riesgo de baneo.

> ⚠️ Todas las pruebas de la Fase 0 se hacen con **números secundarios** (SIMs de prueba), nunca con cuentas personales.

- [x] ~~Confirmar el comportamiento de `onWhatsApp()`~~ — ya validado en un proyecto existente del autor: `[{ jid, exists: true }]` si existe, `[]` si no (ver contrato en [tech-stack.md](tech-stack.md#contrato-de-la-consulta-onwhatsapp-de-baileys)).
- [ ] **Prototipo de escritorio con Baileys** — script Node que vincula **con código de emparejamiento** (no QR) y consulta una lista de números; mide la latencia de conexión en frío + consulta. `S`
- [ ] **Validación de la hipótesis** — con ese prototipo, medir qué % de 50–100 números de spam conocidos y 50–100 legítimos tiene WhatsApp. Si la diferencia no es clara, el producto no tiene sentido. `M`
- [ ] **Comparar motores en Android** — F1 (Baileys + nodejs-mobile) vs F2 (librería JVM): tamaño del APK, latencia de conexión en frío + consulta, soporte de pairing code, mantenimiento. `L`
- [ ] **Efectos secundarios en el WhatsApp principal** — con la sesión vinculada: ¿el usuario aparece "en línea"?, ¿se silencian las notificaciones del teléfono?, ¿llegan confirmaciones de lectura? Todo debe dar "no". `S`
- [ ] **Prueba de resistencia al baneo** — 3–5 números secundarios con uso realista (5–20 consultas/día) durante 3–4 semanas. Registrar cualquier restricción o desvinculación. `L`
  - *Evidencia previa:* el autor usa Baileys con su **número personal** en otro proyecto (sesión persistente en servidor, envía mensajes automáticos y llama a `onWhatsApp()` solo antes de escribir a un contacto nuevo). Ese patrón de consultas es parecido al de esta app. Diferencia clave a probar: aquí la sesión **se conecta y desconecta en cada llamada**, y reconectar seguido podría ser una señal propia de detección.
- [ ] **Spike de `CallScreeningService` + overlay** — responder de inmediato, mostrar "Verificando…" y actualizar el aviso; probar en Samsung, Xiaomi y Pixel sin tapar los botones de la llamada. `M`
- [ ] **Revisión legal y de distribución** — términos de servicio de WhatsApp, protección de datos (p. ej. Ley 29733 en Perú), política de Google Play; elegir canal de distribución. `M`

---

## Fase 1 — MVP (Android) 🚀
**Objetivo:** un usuario instala la app, vincula su WhatsApp y ve el aviso en su próxima llamada desconocida.
**Criterio de salida:** beta cerrada con 20–50 usuarios durante 4 semanas, **cero cuentas restringidas** y latencia p95 < 3 s.

### Vinculación y sesión
- [ ] **Pantalla de consentimiento de riesgo** — explica en lenguaje claro que el método no es oficial y que la cuenta puede ser restringida; exige aceptación explícita. `S`
- [ ] **Vinculación por código de emparejamiento** — el usuario ingresa su número, la app muestra el código de 8 caracteres y guía los pasos: *WhatsApp → Dispositivos vinculados → Vincular con número de teléfono*. `M`
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
- [ ] **Acciones post-llamada** — "Abrir chat en WhatsApp", "Marcar como spam", "Es de confianza". `S`
- [ ] **Lista blanca / lista negra** personal que tiene prioridad sobre la verificación. `S`
- [ ] **Feedback de precisión** — el "Marcar como spam" alimenta la métrica de validación de la hipótesis. `S`
- [ ] **Personalización del aviso** — posición, tamaño, tema claro/oscuro, duración. `S`
- [ ] **Soporte multi-SIM** — mostrar en qué línea entra la llamada. `S`

---

## Fase 3 — Protección activa 🛡️
**Objetivo:** pasar de informar a actuar, siempre con consentimiento explícito.

- [ ] **Auto-silenciar** llamadas sin WhatsApp (opt-in; espera el resultado hasta 3 s antes de responder). `S`
- [ ] **Auto-rechazar** llamadas sin WhatsApp (opt-in, con resumen diario de lo bloqueado). `M`
- [ ] **Señales combinadas** — sumar al resultado de WhatsApp otras señales (prefijos conocidos de call centers, frecuencia de llamadas del mismo rango, reportes propios) para un nivel de riesgo bajo/medio/alto. `L`
- [ ] **Reglas por horario** — p. ej. silenciar números sin WhatsApp de noche. `S`
- [ ] **Widget / tile de ajustes rápidos** para pausar la protección. `S`

---

## Fase 4 — Crecimiento 🌎

- [ ] **Monetización** — sin costo por consulta, así que encaja un pago único o una versión "Pro" (protección activa, historial ilimitado) en lugar de cobrar por uso. `M`
- [ ] **Localización** — español, portugués, inglés. `S`
- [ ] **Código abierto** del motor y del manejo de la sesión, para generar confianza (la app tiene acceso técnico a la cuenta del usuario). `M`
- [ ] **Reputación colaborativa opcional** — reportes anónimos agregados entre usuarios (con consentimiento; requeriría un backend). `XL`

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

1. ¿Motor Baileys + nodejs-mobile o librería JVM? → **Fase 0**.
2. ¿Conexión bajo demanda o persistente? → depende de la latencia medida en la **Fase 0**.
3. ¿Canal de distribución: Google Play, APK propio o F-Droid? → **Fase 0**.
4. ¿Mercado de lanzamiento inicial (país)? Afecta la validación de la hipótesis y el marco legal.
5. ¿Mostrar el nombre de la empresa cuando el número es una cuenta WhatsApp Business? Ayudaría con el caso de empresas comerciales que sí tienen WhatsApp, pero choca con el principio de no mostrar datos del perfil.

## Decisiones tomadas

| Fecha | Decisión | Motivo |
|---|---|---|
| 2026-09-23 | Solo Android | iOS no permite ejecutar un cliente en segundo plano ni overlays |
| 2026-09-23 | `wa.me` descartado | Solo distingue cuentas Business; una cuenta personal y un número inexistente se ven igual |
| 2026-09-23 | Verificación con el WhatsApp del propio usuario, dentro del teléfono | Reparte la carga entre usuarios, sin servidor ni costos, y las credenciales no salen del teléfono |
| 2026-09-23 | Sin backend en el MVP | La sesión corre en el teléfono; solo se usa Remote Config como kill switch |
