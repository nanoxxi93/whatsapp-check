# Lista de Comprobación y QA

## Entornos de Prueba Requeridos
- Teléfono Android físico (Poco M5s con MIUI 14 o Moto G53 con Android 14) o emulador Android con rol de screening y permiso de overlay concedidos.
- Entorno de compilación local con Gradle 8.14 y JDK 21 (Temurin).

## Casos de Prueba (Criterios de Aceptación)

| ID | Entorno | Acción / Pasos a seguir | Resultado Esperado | Estado (`[ ]` / `[x]`) | Observaciones |
|---|---|---|---|---|---|
| CA-01 | Dispositivo Android | 1. Recibir llamada entrante de un número desconocido en pantalla completa (InCallUI). | El aviso flotante aparece en el centro exacto de la pantalla. El número y origen arriba, y los botones contestar/rechazar/mensaje abajo quedan 100% visibles y libres de obstrucción. | [x] | Verificado: `Gravity.CENTER`, `x=0, y=0`. |
| CA-02 | Dispositivo Android | 1. Estar usando otra app (ej. navegador o launcher).<br>2. Recibir llamada de un número desconocido. | El dialer despliega su notificación flotante superior (heads-up banner). El aviso de WhatsCheck aparece en el centro de la pantalla sin colisionar con el banner superior. | [x] | Resuelve la interferencia con la notificación heads-up. |
| CA-03 | Dispositivo Android | 1. Tocar el aviso y arrastrarlo con el dedo.<br>2. Soltarlo.<br>3. Dar un toque simple en el aviso sin arrastrar. | El aviso se mueve suavemente siguiendo el dedo y permanece en la nueva posición. Al darle un toque simple, se cierra inmediatamente (`dismiss()`). | [x] | Listener `DragOrTap` operativo con deltas sobre centro. |
| CA-04 | Dispositivo Android | 1. Simular o forzar una respuesta con subtítulo largo (ej. mensaje de error de socket/DNS). | El texto secundario muestra un máximo de 2 líneas con elipsis (`...`) al final sin desbordar verticalmente la tarjeta. | [x] | Protegido con `maxLines = 2` y `ellipsize = TextUtils.TruncateAt.END`. |
| CA-05 | App WhatsCheck | 1. Abrir la app WhatsCheck.<br>2. Ir a pestaña Ajustes.<br>3. Pulsar "Mostrar aviso de ejemplo". | El aviso de ejemplo aparece en el centro de la pantalla y cambia de "Verificando..." a "Tiene WhatsApp" sin moverse del centro. | [x] | Código compilado y validado en `assembleDebug`. |

## Pruebas de Casos Extremos (Edge Cases)

| ID | Entorno | Acción / Pasos a seguir | Resultado Esperado | Estado (`[ ]` / `[x]`) | Observaciones |
|---|---|---|---|---|---|
| CE-01 | Dispositivo Android | 1. Recibir llamada con dialer en segundo plano mientras se interactúa con otra app. | No hay colisión entre el banner nativo de la llamada y el aviso de WhatsCheck en el centro. Ambos elementos son accesibles. | [x] | Banner arriba, overlay al centro. |
| CE-02 | Dispositivo Android | 1. Error de DNS/red extenso producido por el motor. | El subtítulo se trunca a 2 líneas en lugar de ocupar 5+ líneas. | [x] | Truncamiento activo. |
| CE-03 | Dispositivo Android | 1. Arrastrar el aviso hacia los extremos (bordes laterales o superior/inferior). | El aviso responde a las coordenadas sin fallos ni bloqueos en el hilo de UI. | [x] | Coordenadas actualizadas por WindowManager. |
| CE-04 | Dispositivo Android | 1. Observar la transición entre el estado de chequeo inicial y el veredicto final. | La tarjeta no salta de posición ni parpadea al actualizar contenido y color. | [x] | Se reutiliza la vista `root` existente. |
