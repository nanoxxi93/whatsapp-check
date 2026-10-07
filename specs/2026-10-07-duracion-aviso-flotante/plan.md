# Plan de Implementación Técnico

## Información general sobre la arquitectura

El ajuste de la duración del aviso flotante afecta a tres componentes principales de la aplicación:
1. **Capa de Persistencia (`Prefs` en `MainActivity.kt`):**
   - Almacena la preferencia de duración del overlay en `SharedPreferences` del sistema (`whatscheck.xml`), con lectura defensiva (default 8 s si no existe o es inválido) y validación de rango sobre el conjunto `{5, 8, 12}`.
2. **Capa de Control Visual (`OverlayController` en `com.whatscheck.engines.call`):**
   - Desacopla el temporizador entre el estado preliminar `CHECKING` (fijado a 10 s por seguridad) y los estados finales `HAS`, `NO` y `UNKNOWN`.
   - Para los estados finales, programa el temporizador de auto-ocultamiento usando `Prefs.overlayDurationSeconds(context) * 1000L` (o un valor explícito si se suministra).
   - En el listener de gestos `DragOrTap`, pausa el temporizador de cierre mientras el usuario mantenga el toque o arrastre, y lo reactiva al soltar el aviso si no fue cerrado.
3. **Capa de Interfaz de Usuario (`SettingsScreen` en Compose):**
   - Incorpora en la tarjeta "Funcionamiento" un menú desplegable (`DropdownMenu`) con selector accesible (`OutlinedButton` con icono de flecha hacia abajo) que expone las opciones: "5 segundos", "8 segundos (recomendado)" y "12 segundos".
   - Al seleccionar una opción, actualiza el estado local, persiste el cambio mediante `Prefs.setOverlayDurationSeconds` e invoca `onChanged()`.
   - El botón "Mostrar aviso de ejemplo" simula la verificación mostrando el resultado y beneficiándose automáticamente de la nueva duración configurada.

## Pila tecnológica y decisiones clave

- **Separación de temporizadores en `OverlayController`:**
  - **Por qué:** El tiempo necesario para esperar una respuesta de red (`CHECKING`) no debe ser el mismo que el tiempo de permanencia de un resultado ya resuelto (`HAS`/`NO`/`UNKNOWN`). Una vez obtenido el veredicto en ~2 s, 8 s (o 5 s / 12 s) es el tiempo ideal para leerlo mientras suena el teléfono, reduciendo el residuo visual al colgar de 45 s a un valor no intrusivo.
  - **Alternativas consideradas:**
    - *Un único temporizador global acortado a 8 s:* Descartado porque si la verificación de whatsmeow tardara 3.5 s por latencia de red 4G, el aviso se cerraría solo 4.5 s después de mostrar el resultado, dejando poco tiempo de lectura.
    - *Detectar fin de llamada con `READ_PHONE_STATE`:* Descartado por ser invasivo en permisos y violar el principio de privacidad del proyecto (`RNF-01`).
- **Control de selección con `DropdownMenu` en Compose:**
  - **Por qué:** Permite una selección clara, compacta y estándar en Material 3, mostrando el valor activo sin saturar horizontalmente la tarjeta "Funcionamiento" en pantallas estrechas.
  - **Alternativas consideradas:**
    - *Fila de `FilterChip`:* Aunque es visualmente atractiva, con textos descriptivos ("8 segundos") puede desbordar o requerir wrapping en pantallas de menor ancho (ej. teléfonos compactos).
    - *`Slider` numérico:* Descartado porque permitiría valores arbitrarios (ej. 3.7 s) que complican la experiencia y no aportan valor frente a tres presets bien calibrados.
- **Validación defensiva en `Prefs`:**
  - **Por qué:** Evita valores anómalos o lecturas corruptas de `SharedPreferences`. Si el valor guardado no es 5, 8 ni 12, se retorna 8 sin lanzar excepciones.

## Secuencia de implementación

1. **Pruebas unitarias de `Prefs` (TDD):**
   - Crear suite de tests unitarios para validar que `overlayDurationSeconds` retorna 8 por defecto, persiste 5, 8 y 12 correctamente, y rechaza/corrige valores fuera del conjunto permitido.
2. **Actualización de `Prefs` en `MainActivity.kt`:**
   - Definir `DEFAULT_OVERLAY_DURATION_SECONDS = 8` y la lista de valores válidos `VALID_OVERLAY_DURATIONS = listOf(5, 8, 12)`.
   - Implementar `overlayDurationSeconds(context: Context): Int` y `setOverlayDurationSeconds(context: Context, seconds: Int)`.
3. **Modificación de `OverlayController.kt`:**
   - Reemplazar la constante monolítica `AUTO_HIDE_MS = 45_000L` por `CHECKING_TIMEOUT_MS = 10_000L` y el cálculo dinámico basado en la preferencia configurada para estados de resultado.
   - Refinar el listener de toque para pausar el temporizador mientras el usuario arrastra el aviso.
4. **Actualización de `SettingsScreen.kt`:**
   - Agregar el estado y menú desplegable (`DropdownMenu` con `ExposedDropdownMenuBox` o selector con `OutlinedButton`) en la tarjeta "Funcionamiento".
   - Conectar el selector a `Prefs.setOverlayDurationSeconds`.
   - Ajustar el botón "Mostrar aviso de ejemplo" para que informe la duración en su subtítulo y compruebe el comportamiento real.
5. **Verificación y build:**
   - Ejecutar pruebas automatizadas.
   - Compilar el proyecto con `./gradlew :app:assembleDebug` con JDK 21.

## Comprobación de la Constitución

- **Principio "Privacidad y Minimización" (`constitution.md §Privacidad`):**
  - No se solicita `READ_PHONE_STATE`, `READ_CALL_LOG` ni ningún permiso adicional. La solución es 100% local y no invasiva.
- **Principio "Nunca Estorbar" (`constitution.md §UX`):**
  - Resuelve directamente el problema de permanencia innecesaria del aviso una vez terminada la llamada, reduciendo el tiempo de permanencia en pantalla de 45 s a 8 s por defecto (o 5 s si el usuario lo prefiere).
- **Principio "Fluidez" (`constitution.md §UX`):**
  - La lectura de un entero de `SharedPreferences` en memoria tarda microsegundos (`< 0.1 ms`), garantizando cero impacto en el hilo principal.
- **Principio "Test-Driven Development" (`constitution.md §Estándares`):**
  - Se define la suite de pruebas previa a la implementación de la lógica de dominio y persistencia.

## Suposiciones y preguntas abiertas

- **Suposición 1:** 8 segundos es suficiente para que un usuario lea el resultado ("Tiene WhatsApp" / "No tiene WhatsApp") mientras suena el teléfono, pero lo bastante breve para que desaparezca pronto si cuelga o rechaza la llamada.
- **Suposición 2:** El ciclo de vida de `SharedPreferences` privado de la app (`whatscheck.xml`) es persistente entre llamadas y reinicios del dispositivo sin requerir base de datos relacional para esta configuración.
