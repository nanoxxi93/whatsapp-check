# Plan de Implementación Técnico

## Información general sobre la arquitectura

El componente modificado es `OverlayController` (`com.whatscheck.engines.call.OverlayController`), el cual gestiona una ventana de tipo `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY` acoplada al sistema Android.

El flujo de visualización y control opera de la siguiente manera:
1. `ScreeningService` o la acción de prueba en `SettingsScreen` invocan `OverlayController.show(kind, subtitle)`.
2. Si la vista raíz `LinearLayout` no existe, se ejecuta `create()`, donde se instancian el título y el subtítulo, y se define el layout con `WindowManager.LayoutParams`.
3. El layout se posiciona con `gravity = Gravity.CENTER`, asegurando que `params.x = 0` y `params.y = 0` sitúen el contenedor flotante en el centro simétrico de la pantalla.
4. El listener de toque `DragOrTap` captura gestos táctiles. Al arrastrar, calcula los desplazamientos delta (`dx`, `dy`) sumándolos a las coordenadas originales (`params.x = originX + dx`, `params.y = originY + dy`) y actualiza la ventana con `WindowManager.updateViewLayout`. Si el gesto es un toque simple sin desplazamiento significativo (`<= 6dp`), se invoca `dismiss()`.

## Pila tecnológica y decisiones clave

- **Posicionamiento con `Gravity.CENTER` en WindowManager:**
  - **Por qué:** Permite centrar la ventana de forma nativa e independiente de densidades, muescas (*notch*), orificios de cámara (*punch-hole*) o dimensiones de pantalla específicas de cada fabricante (ej. MIUI en Poco M5s vs AOSP en Moto G53).
  - **Alternativas consideradas:**
    - *Posición superior entre barra y encabezado (`y ≈ 7.5%`):* Descartada porque cuando entra una llamada con otra aplicación activa, el sistema o el dialer despliega una notificación flotante emergente (*heads-up banner*) debajo de la barra de estado, con la que colisionaría directamente.
    - *Posición inferior encima de botones (`y ≈ 60%`):* Descartada porque interferiría con el teclado o botones contextuales de la llamada entrante ("Mensaje").
  - **Requisitos No Funcionales:** Cumple `RNF-01` (Nunca Estorbar) y `RNF-03` (Consistencia de plataforma sin factores frágiles de altura).

- **Restricción de líneas en subtítulo (`maxLines = 2`, `ellipsize = TextUtils.TruncateAt.END`):**
  - **Por qué:** Errores no controlados de sockets, DNS o handshakes pueden producir mensajes de error de varias líneas (observados en la validación del spike), lo cual expandiría la tarjeta hacia arriba o abajo desproporcionadamente.
  - **Alternativas consideradas:**
    - *Sin límite de líneas:* Provoca crecimiento vertical impredecible y obstrucción visual.
    - *Ocultar el subtítulo en errores:* Reduce la transparencia técnica requerida en el spike/fase de pruebas.

## Secuencia de implementación

1. **Ajuste en `OverlayController.kt`:**
   - En la configuración de `WindowManager.LayoutParams`, reemplazar `gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL` y el cálculo de porcentaje `y = ... * 0.22` por `gravity = Gravity.CENTER`, `x = 0`, `y = 0`.
   - En la instanciación del `TextView` secundario (`small`), agregar `maxLines = 2` y `ellipsize = android.text.TextUtils.TruncateAt.END`.
   - Actualizar el KDoc descriptivo de la clase para reflejar el comportamiento centrado.
2. **Verificación y pruebas:**
   - Compilación con `./gradlew :app:assembleDebug` usando JDK 21.
   - Ejecución del suite de pruebas unitarias (`./gradlew test`).
   - Comprobación del comportamiento visual y de arrastre en la app.

## Comprobación de la Constitución

- **Principio "Nunca Estorbar" (`constitution.md §UX`):**
  - Al ubicarse en el centro de la pantalla, el overlay no obstruye la parte superior (donde el dialer ubica el número telefónico en pantalla completa, o donde aparece el banner flotante si hay otra app abierta) ni la parte inferior (donde residen los botones nativos de responder/rechazar la llamada).
- **Principio "Fluidez" (`constitution.md §UX`):**
  - La inicialización en el hilo principal sigue siendo inmediata (`< 5 ms`), sin llamadas bloqueantes ni asignaciones pesadas.
- **Principio "Test-Driven Development / Estándares":**
  - No altera contratos de API ni introduce dependencias externas no autorizadas.

## Suposiciones y preguntas abiertas

- **Suposición 1:** El espacio central vertical de la pantalla en llamadas entrantes tanto en MIUI como en AOSP (Google Dialer) está libre de elementos críticos que impidan la interacción de llamada (confirmado en las capturas de pantalla de validación del Poco M5s).
- **Suposición 2:** Si el usuario requiere ver el fondo central momentáneamente, la funcionalidad preexistente de arrastre táctil y cierre por toque simple proporciona la vía de escape inmediata.
