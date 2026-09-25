# Constitución del Proyecto

Este archivo captura los principios, restricciones y estándares no negociables que rigen el proyecto **WhatsCheck**. Sirve como barandilla para el desarrollo asistido por IA (Spec-Driven Development) y es la única fuente de la verdad para estas reglas. Las decisiones técnicas puntuales viven en `tech-stack.md` y la motivación en `mission.md`.

## Estándares Tecnológicos
- **Plataforma Única:** Android nativo (minSdk 29, targetSdk 35+). iOS está completamente descartado.
- **Lenguaje y UI:** Kotlin 2.x y Jetpack Compose.
- **Arquitectura:** Clean Architecture modular (core, feature, engine) e Inyección de Dependencias con Hilt.
- **Sin Backend Propio:** La app funciona con filosofía local-first sin servidores del proyecto para procesar datos. El único servicio remoto permitido es Firebase Remote Config (como kill-switch de emergencia).

## Privacidad y Restricciones de Dominio
- **Minimización de Datos Extrema:** La sesión de WhatsApp integrada (motor `whatsmeow`) **NO** debe guardar agenda, **NO** debe descargar historial de chats, **NO** debe guardar secretos de mensajes. Todo lo entrante se descarta sin procesar.
- **Comportamiento Pasivo:** La app **NO** debe alterar el estado del usuario en su WhatsApp principal. Esto significa: prohibido llamar a endpoints de presencia (no mostrar "en línea"), prohibido enviar confirmaciones de entrega o lectura.
- **Aislamiento Seguro:** Las credenciales de la sesión de WhatsApp vinculada se cifran obligatoriamente con Android Keystore (AES-GCM) y se deben excluir del backup (`allowBackup=false`).

## Principios de Experiencia de Usuario (UX)
- **Nunca Estorbar:** El overlay o notificación de verificación nunca debe tapar los botones nativos del sistema para contestar o rechazar la llamada.
- **Fluidez (No Retrasar la Llamada):** El `CallScreeningService` debe permitir la llamada de inmediato (`respondToCall(allow)`) en lugar de retenerla para evitar problemas de timeout del sistema. La verificación ocurre en paralelo mientras el teléfono ya está sonando.
- **Fallar en Silencio y Honestidad:** Ante cualquier error de red, caída del motor o falta de sesión, el resultado debe ser `UNKNOWN` (No se pudo verificar). Nunca emitir falsos positivos o negativos. El veredicto final es descriptivo ("No tiene WhatsApp"), jamás afirmativo sobre intenciones ("Es SPAM").

## Estándares de Codificación
- **Test-Driven Development (TDD):** Sin un test que falle primero, no se escribe código de producto.
- **Aislamiento del Motor:** El uso del motor de protocolo (Whatsmeow) debe estar encapsulado detrás de una interfaz de dominio (`WhatsAppPresenceEngine`) de manera que pueda reemplazarse o simularse (`FakePresenceEngine`) para los tests.
- **Compatibilidad y Versionado:** Commits semánticos (`feat:`, `fix:`, `docs:`). Todo el código está en español para la documentación e inglés para variables y funciones.

## Gobernanza y Manejo de Riesgos
- **Consentimiento de Riesgo:** La interfaz de vinculación debe obligar al usuario a leer y aceptar que el uso de clientes no oficiales conlleva un riesgo de restricción de su cuenta por parte de Meta.
- **Control Antiveto:** El mecanismo kill-switch en Remote Config debe estar siempre disponible para desactivar remotamente las consultas si se detectan baneos masivos.
