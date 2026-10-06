# Lista de Tareas: Posicionamiento central del aviso flotante (Overlay)

## Fase 1: Implementación de Posicionamiento y Restricciones de UI
- [x] 1.1 Configurar el layout de WindowManager en `OverlayController.kt` con `Gravity.CENTER`, `x = 0`, `y = 0` y actualizar KDoc. (RF-01, RF-02, CA-01, CA-02).
- [x] 1.2 Restringir la altura del subtítulo del overlay en `OverlayController.kt` con `maxLines = 2` y `ellipsize = TextUtils.TruncateAt.END`. (RF-03, CA-04, CE-02).

## Fase 2: Verificación, Pruebas y Regresión
- [x] 2.1 Ejecutar la compilación del proyecto (`./gradlew :app:assembleDebug`) verificando compatibilidad con JDK 21.
- [x] 2.2 Ejecutar las pruebas unitarias existentes (`./gradlew test`) verificando que no existan regresiones.
- [x] 2.3 Validar interacciones táctiles: asegurar que el listener `DragOrTap` mantenga el soporte de arrastre relativo al centro y cierre por toque simple. (CA-03, CE-03).
