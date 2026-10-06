# Posicionamiento central del aviso flotante (Overlay)

## Resumen
Ajustar la posición de inicio del aviso flotante (`OverlayController`) al centro exacto de la pantalla (`Gravity.CENTER`) para garantizar que no obstruya los datos del llamante (número y origen) ni los controles de llamada en pantalla completa, y evitar colisiones visuales con la notificación emergente (*heads-up banner*) del marcador telefónico cuando el usuario recibe una llamada fuera de la aplicación de teléfono.

## Casos de usuario
- **CU-01:** Como usuario que recibe una llamada entrante en pantalla completa, quiero ver el aviso de verificación de WhatsCheck en el centro de la pantalla para poder leer claramente el número de teléfono y origen arriba, y pulsar responder/rechazar abajo sin que nada los tape.
- **CU-02:** Como usuario que recibe una llamada mientras navega en otra aplicación (o en la pantalla de inicio), quiero que el aviso aparezca en el centro de la pantalla para no tapar la notificación emergente (*heads-up / banner*) nativa de llamada que aparece en la parte superior debajo de la barra de estado.
- **CU-03:** Como usuario que desea despejar la vista temporalmente, quiero poder arrastrar el aviso flotante a cualquier lugar de la pantalla o cerrarlo con un toque simple.

## Criterios de aceptación
- [ ] **CA-01:** En la pantalla completa de llamada entrante, el aviso flotante se muestra centrado vertical y horizontalmente (`Gravity.CENTER`), dejando despejada la zona superior (número, operador, país) y la zona inferior (botones contestar/rechazar/mensaje).
- [ ] **CA-02:** Con el teléfono en uso en otra app o launcher, el aviso flotante aparece en el centro de la pantalla sin superponerse ni ocultar el banner emergente de llamada nativo que surge debajo de la barra de estado.
- [ ] **CA-03:** El aviso sigue permitiendo arrastre táctil fluido (*drag*) desde su posición central y se cierra al hacer un toque simple (*tap*) sin arrastre.
- [ ] **CA-04:** Subtítulos extensos (errores de red, DNS o timeout) no desbordan la altura del aviso (limitados a un máximo de 2 líneas con puntos suspensivos `ellipsize`).
- [ ] **CA-05:** El botón "Mostrar aviso de ejemplo" en Ajustes renderiza el aviso en la posición central esperada.

## Requisitos funcionales
- **RF-01 (Posicionamiento centrado):** La ventana flotante (`WindowManager.LayoutParams`) debe configurarse con gravedad centrada tanto horizontal como verticalmente (`Gravity.CENTER`), con desplazamientos iniciales `x = 0`, `y = 0`.
- **RF-02 (Interacción táctil):** El controlador de gestos táctiles debe calcular los desplazamientos relativos a la posición inicial centrada manteniendo la posibilidad de mover el overlay libremente por la pantalla.
- **RF-03 (Compactación de subtítulo):** La vista de subtítulo del aviso debe tener un tope máximo de líneas (`maxLines = 2`) y truncamiento elíptico al final (`ellipsize = TextUtils.TruncateAt.END`) para garantizar que la tarjeta conserve proporciones compactas ante mensajes de error o detalles largos.
- **RF-04 (Compatibilidad con ciclo de vida):** El aviso debe mantener su comportamiento de auto-ocultamiento a los 45 segundos y cierre inmediato al tocarlo.

## Requisitos no funcionales
- **RNF-01 (Principio "Nunca Estorbar"):** El aviso flotante no debe superponerse a los controles nativos de llamada ni a la información de identificación del número entrante, en cumplimiento directo de `specs/constitution.md` (UX).
- **RNF-02 (Rendimiento y fluidez):** El cálculo y posicionamiento del overlay deben realizarse en el hilo principal sin introducir bloqueos (`< 16 ms`), preservando la métrica de `tOverlay` del spike (`p95 ≤ 418 ms`).
- **RNF-03 (Consistencia de plataforma):** La solución no debe depender de coordenadas fijas ni porcentajes frágiles sujetos a variaciones de marcos, cámaras frontales (*punch-hole*) o barras de estado de diferentes fabricantes (MIUI vs Moto / AOSP).

## Casos extremos
- **CE-01 (Llamada con dialer en segundo plano):** El usuario está usando otra aplicación (navegador, mensajería, etc.) cuando entra la llamada. El dialer nativo despliega un banner de notificación emergente (*heads-up*) en el borde superior de la pantalla. El overlay aparece en el centro de la pantalla; no hay colisión ni bloqueo de los botones del banner superior ni de los controles de la app en uso.
- **CE-02 (Subtítulo con error de red largo o traza de socket):** Si ocurre un error de handshake con una descripción muy larga (ej. resolución de DNS fallida como en la captura real del spike), el subtítulo se trunca a 2 líneas en vez de expandirse verticalmente a 5+ líneas.
- **CE-03 (Arrastre hacia los bordes de la pantalla):** Si el usuario arrastra el aviso hacia los extremos, el `WindowManager` maneja las coordenadas sin congelar la interfaz ni generar excepciones de renderizado.
- **CE-04 (Actualización de estado en pantalla):** Cuando el aviso pasa de `CHECKING` ("Verificando…") a `HAS`/`NO`/`UNKNOWN`, su posición centrada (o la posición a la que fue arrastrado por el usuario) no debe reiniciarse ni parpadear.
