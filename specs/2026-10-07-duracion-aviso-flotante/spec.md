# Duración configurable del aviso flotante (Overlay)

## Resumen
Reducir el tiempo de permanencia automática del aviso flotante (`OverlayController`) en pantalla tras mostrar el resultado de verificación (de 45 s a un valor predeterminado de 8 s, con selector desplegable en Ajustes de 5 s, 8 s y 12 s), para evitar que el aviso continúe visible innecesariamente mucho tiempo después de que la llamada ha finalizado o ha sido atendida/rechazada, sin necesidad de permisos telefónicos invasivos.

## Casos de usuario
- **CU-01:** Como usuario que recibe una llamada entrante desconocida, quiero que el aviso con el resultado (✅ / ⚠️ / ❔) se cierre automáticamente a los pocos segundos (8 s por defecto) tras leerlo, para que no se quede estorbando en mi pantalla una vez que la llamada haya terminado o haya sido colgada/rechazada.
- **CU-02:** Como usuario que atiende llamadas rápido o prefiere una experiencia más pausada, quiero poder seleccionar en la pantalla de Ajustes mediante un menú desplegable cuánto tiempo dura el aviso con el resultado (5 s, 8 s o 12 s).
- **CU-03:** Como usuario que prueba los ajustes, quiero pulsar "Mostrar aviso de ejemplo" y comprobar en tiempo real la duración recién seleccionada para validar cómo se siente en pantalla.
- **CU-04:** Como usuario que recibe una llamada donde la verificación tarda o falla por problemas de red, quiero que el estado preliminar ("⏳ Verificando…") tenga un límite de seguridad de 10 s para no dejar colgada la ventana flotante indefinidamente.
- **CU-05:** Como usuario que desea descartar el aviso antes de que expire el tiempo, quiero poder tocarlo una sola vez para cerrarlo al instante, o moverlo arrastrándolo libremente.

## Criterios de aceptación
- [ ] **CA-01:** Cuando el aviso flotante muestra el resultado final (`HAS`, `NO` o `UNKNOWN`), el temporizador de auto-ocultado se programa con la duración configurada (por defecto 8 segundos; opciones 5 s, 8 s, 12 s).
- [ ] **CA-02:** Mientras el aviso permanece en estado preliminar (`CHECKING` / "⏳ Verificando…"), el temporizador de auto-ocultado tiene un límite de seguridad de 10 segundos antes de cerrarse si no llega una respuesta.
- [ ] **CA-03:** En la pantalla de Ajustes (sección "Funcionamiento"), existe una fila con selector desplegable (`DropdownMenu`) titulado "Duración del aviso" con las opciones: "5 segundos", "8 segundos (recomendado)" y "12 segundos", mostrando claramente la opción activa.
- [ ] **CA-04:** Al seleccionar una opción en el desplegable, el valor numérico (5, 8 o 12) se persiste de inmediato en `SharedPreferences` bajo la clave `overlayDurationSeconds` y se actualiza el estado visible en la UI.
- [ ] **CA-05:** El botón "Mostrar aviso de ejemplo" en Ajustes lee la duración configurada y programa el auto-cierre con ese tiempo exacto una vez simulado el resultado, permitiendo comprobar la experiencia de inmediato.
- [ ] **CA-06:** El cierre manual por toque simple (*tap*) y la interacción de arrastre (*drag*) siguen respondiendo de inmediato sin interferencias del temporizador.

## Requisitos funcionales
- **RF-01 (Duración diferenciada por estado):** `OverlayController` debe desacoplar el temporizador de auto-ocultado entre el estado de carga (`CHECKING`: constante fija de 10 s de seguridad) y los estados de resultado final (`HAS`, `NO`, `UNKNOWN`: duración en segundos obtenida de preferencias o recibida como parámetro).
- **RF-02 (Persistencia en preferencias):** `Prefs` debe implementar `overlayDurationSeconds(context: Context): Int` con valor por defecto de 8, y `setOverlayDurationSeconds(context: Context, seconds: Int)`, validando que el valor pertenezca al conjunto permitido {5, 8, 12}.
- **RF-03 (Control desplegable en Ajustes):** `SettingsScreen` debe incorporar en la tarjeta "Funcionamiento" un menú desplegable (`DropdownMenu`) con selector accesible que liste las tres duraciones posibles y persista la elección al seleccionarla.
- **RF-04 (Comportamiento interactivo de prueba):** El botón "Mostrar aviso de ejemplo" debe leer la preferencia actual y pasar dicha duración a la simulación del resultado final del overlay.
- **RF-05 (Integración en llamadas reales):** `ScreeningService` y `CallCheck` deben asegurar que al invocar `overlay.show()` con el resultado de la verificación de llamadas desconocidas, se aplique la duración configurada por el usuario.

## Requisitos no funcionales
- **RNF-01 (Privacidad y Cero Permisos Nuevos):** La solución no debe solicitar ni requerir `READ_PHONE_STATE`, `READ_CALL_LOG` ni ningún otro permiso sensible en tiempo de ejecución, respetando estrictamente `specs/constitution.md` y `specs/mission.md`.
- **RNF-02 (Principio "Nunca Estorbar"):** El aviso debe abandonar la pantalla rápidamente tras el fin o atención de la llamada sin exigir interacción del usuario, reduciendo drásticamente la fricción visual (de 45 s a 8 s por defecto).
- **RNF-03 (Rendimiento y reactividad en Main Thread):** La reprogramación de timers en `Handler(Looper.getMainLooper())` y la lectura de preferencias en memoria no deben introducir latencia medible ni tirones en el hilo principal (`< 1 ms`).

## Casos extremos
- **CE-01 (Verificación lenta que responde cerca del límite de Telecom):** Si la consulta a whatsmeow tarda 3.8 s y pasa a `UNKNOWN` o resultado justo antes del timeout de 4 s, el temporizador de 10 s de `CHECKING` se cancela de inmediato y se programa el nuevo temporizador de resultado (5 s, 8 s o 12 s según la preferencia), asegurando que el usuario tenga el tiempo completo para leer el veredicto.
- **CE-02 (El usuario arrastra el aviso mientras expira el temporizador):** Si el usuario está arrastrando el aviso en pantalla cuando transcurre el tiempo, el aviso no debe desaparecer debajo de su dedo mientras lo mantiene presionado; el cierre se produce tras soltarlo o al tocarlo.
- **CE-03 (Preferencia no encontrada o corrupta):** Si `SharedPreferences` no tiene valor o devuelve un número no contemplado (ej. diferente de 5, 8 o 12), `Prefs.overlayDurationSeconds()` devuelve 8 de manera defensiva.
- **CE-04 (Cambio de ajuste con overlay activo):** Si el usuario modifica la duración en Ajustes e inmediatamente pulsa "Mostrar aviso de ejemplo", el aviso toma la nueva duración seleccionada sin requerir reiniciar la aplicación.
- **CE-05 (Llamadas sucesivas o actualizaciones de estado rápidas):** Cualquier callback de `hide` programado con anterioridad se cancela mediante `main.removeCallbacks(hide)` antes de programar el nuevo, garantizando que no existan carreras ni cierres prematuros debidos a tareas diferidas residuales.
