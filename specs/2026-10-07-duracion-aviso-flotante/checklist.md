# Lista de Comprobación y QA

## Entornos de Prueba Requeridos
- Teléfono Android físico (Poco M5s con MIUI 14 o Moto G53 con Android 14) o emulador Android con rol de screening y permiso de overlay concedidos.
- Entorno de compilación local con Gradle 8.14 y JDK 21 (Temurin).
- Suite de pruebas unitarias (`./gradlew test`).

## Casos de Prueba (Criterios de Aceptación)

| ID | Entorno | Acción / Pasos a seguir | Resultado Esperado | Estado (`[ ]` / `[x]`) | Observaciones |
|---|---|---|---|---|---|
| CA-01 | Dispositivo / Emulador | 1. Recibir llamada de desconocido.<br>2. Dejar que termine la verificación (muestra resultado ✅, ⚠️ o ❔).<br>3. No tocar la pantalla y medir el tiempo hasta que el aviso desaparece. | El aviso se oculta solo exactamente a los 8 segundos (o según la duración configurada) tras mostrar el resultado final. | [x] | Desacoplado en `OverlayController.show()`. |
| CA-02 | Dispositivo / Emulador | 1. Simular inicio de screening sin conexión o con retraso artificial.<br>2. Observar el estado "⏳ Verificando…". | Si no llega resultado, el aviso se auto-oculta al vencer el margen de seguridad de 10 segundos. | [x] | `CHECKING_TIMEOUT_MS = 10_000L` activo. |
| CA-03 | App WhatsCheck | 1. Abrir WhatsCheck e ir a la pestaña Ajustes.<br>2. Localizar la sección "Funcionamiento". | Aparece la opción "Duración del aviso" con un selector desplegable (`DropdownMenu`) que lista: "5 segundos", "8 segundos (recomendado)" y "12 segundos", mostrando la selección actual. | [x] | Implementado con `DropdownMenu` en `SettingsScreen`. |
| CA-04 | App WhatsCheck / Tests | 1. En Ajustes, cambiar la duración a "5 segundos".<br>2. Salir de la app o reiniciar el proceso.<br>3. Volver a abrir Ajustes. | La opción "5 segundos" permanece seleccionada. El valor 5 está persistido en `SharedPreferences` bajo `overlayDurationSeconds`. | [x] | Verificado con tests unitarios en `PrefsTest`. |
| CA-05 | App WhatsCheck | 1. En Ajustes, seleccionar "5 segundos".<br>2. Pulsar "Mostrar aviso de ejemplo".<br>3. Observar la transición y medir el tiempo hasta el cierre automático. | El aviso pasa de "Verificando..." al resultado de ejemplo y se cierra automáticamente a los 5 segundos de mostrar el resultado. | [x] | Botón de ejemplo pasa duración activa a `OverlayController`. |
| CA-06 | Dispositivo / Emulador | 1. Disparar el aviso flotante (vía llamada o botón de ejemplo).<br>2. Tocar el aviso con un tap simple sin arrastrar. | El aviso se cierra de inmediato al toque, cancelando el timer de auto-ocultado. | [x] | `DragOrTap` mantiene `dismiss()` en tap simple. |

## Pruebas de Casos Extremos (Edge Cases)

| ID | Entorno | Acción / Pasos a seguir | Resultado Esperado | Estado (`[ ]` / `[x]`) | Observaciones |
|---|---|---|---|---|---|
| CE-01 | Dispositivo / Tests | 1. La consulta a whatsmeow tarda ~3.8 s (cerca del límite de Telecom).<br>2. El resultado llega al overlay. | El temporizador previo de 10 s de `CHECKING` se cancela de inmediato y se programa el nuevo temporizador de 8 s (o seleccionado), garantizando el tiempo completo de lectura. | [x] | `main.removeCallbacks(hide)` cancela el timer previo al llegar el resultado. |
| CE-02 | Dispositivo / Emulador | 1. Disparar el aviso.<br>2. Cuando estén por transcurrir los 8 segundos, mantener el dedo pulsado sobre el aviso y moverlo (arrastrar). | El aviso no desaparece debajo del dedo mientras el usuario lo esté arrastrando activamente. Se oculta tras soltarlo o al darle un toque simple. | [x] | Timer pausado en `ACTION_DOWN` y reprogramado al soltar en `ACTION_UP`. |
| CE-03 | Tests Unitarios | 1. Forzar en `SharedPreferences` un valor corrupto o no contemplado (ej. `99` o ausencia de clave).<br>2. Leer `Prefs.overlayDurationSeconds(context)`. | Devuelve defensivamente `8` sin lanzar excepciones ni provocar cierres de la app. | [x] | `sanitizeOverlayDuration` validado en test. |
| CE-04 | App WhatsCheck | 1. Cambiar la duración de 8 s a 12 s en Ajustes.<br>2. De inmediato pulsar "Mostrar aviso de ejemplo" sin reiniciar la app. | El aviso adopta la nueva duración de 12 s en la misma sesión. | [x] | Lectura reactiva inmediata tras seleccionar en menú. |
| CE-05 | Dispositivo / Tests | 1. Producir dos transiciones de estado consecutivas y rápidas en el overlay. | Cada llamada a `show` desvincula cualquier `hide` pendiente previo (`removeCallbacks(hide)`), evitando cierres prematuros por timers residuales. | [x] | `removeCallbacks(hide)` ejecutado antes de cada postDelayed. |
