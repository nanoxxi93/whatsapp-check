# Lista de Tareas: Duración configurable del aviso flotante (Overlay)

## Fase 1: Persistencia y Pruebas Unitarias (TDD)
- [x] 1.1 Diseñar e implementar pruebas unitarias para `Prefs` (`PrefsTest.kt`) validando el valor por defecto (8 s), valores permitidos {5, 8, 12} y corrección defensiva ante valores desconocidos (RF-02, CA-04, CE-03).
- [x] 1.2 Extender `Prefs` en `MainActivity.kt` con `overlayDurationSeconds(context: Context): Int` y `setOverlayDurationSeconds(context: Context, seconds: Int)` con validación de rango `{5, 8, 12}` (RF-02, CA-04).

## Fase 2: Lógica de Temporización y Gestos en OverlayController
- [x] 2.1 Desacoplar constantes y lógica de temporización en `OverlayController.kt`: fijar `CHECKING_TIMEOUT_MS = 10_000L` y temporizador de resultado dinámico basado en `Prefs.overlayDurationSeconds(context) * 1000L` (RF-01, RF-04, RF-05, CA-01, CA-02).
- [x] 2.2 Blindar el listener `DragOrTap` para pausar el temporizador de auto-ocultado durante el arrastre activo y reactivarlo al soltarlo si no fue descartado (CA-06, CE-02).

## Fase 3: Interfaz de Usuario y Ajustes en Compose
- [x] 3.1 Integrar selector desplegable `DropdownMenu` ("Duración del aviso") en la tarjeta "Funcionamiento" de `SettingsScreen.kt` con opciones "5 segundos", "8 segundos (recomendado)" y "12 segundos", conectándolo a `Prefs` (RF-03, CA-03, CA-04).
- [x] 3.2 Actualizar el botón "Mostrar aviso de ejemplo" en `SettingsScreen.kt` para que adopte la duración recién elegida y la muestre en su subtítulo durante la simulación (RF-04, CA-05, CE-04).

## Fase 4: Verificación, Compilación y Calidad
- [x] 4.1 Ejecutar suite de pruebas unitarias (`./gradlew test`) verificando que todas las pruebas pasen en verde.
- [x] 4.2 Compilar el APK con `./gradlew :app:assembleDebug` con JDK 21 garantizando ausencia de advertencias o errores de compilación.
- [x] 4.3 Actualizar el registro de tareas y preparar el checklist de verificación.
