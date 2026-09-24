# Validación — Diseño de la app de pruebas

> **Estado:** En progreso · **Fecha:** 2026-09-24
> **Relacionado:** [requirements.md](requirements.md) · [plan.md](plan.md)

## Criterios de aceptación

| ID | Requisito | Criterio | Método | Obligatorio |
|---|---|---|---|---|
| CA-01 | RF-01 | La app abre en Inicio y la barra inferior cambia entre Inicio, Registro y Ajustes. | Captura en el Poco | Sí |
| CA-02 | RF-02, RNF-01 | Con la sesión vinculada, Inicio muestra "Vinculada" y el número enmascarado (y completo al tocar "Mostrar"), sin "motor" ni "whatsmeow". | Captura | Sí |
| CA-03 | RF-02 | La consulta manual devuelve ✅/⚠️/❔ con el tiempo. | Manual | Sí |
| CA-04 | RF-04 | Registro lista las llamadas de `calls.jsonl` y su resumen coincide con el del archivo. | Captura + revisión | Sí |
| CA-05 | RF-05 | Ajustes muestra el estado de cada permiso y "Desvincular" pide confirmación. | Captura | Sí |
| CA-06 | RF-06 | El ícono se ve en el lanzador del Poco, y la notificación "WhatsCheck activo" usa el ícono propio. | Captura / observación del autor | Sí |
| CA-07 | RF-07 | Las tres vistas se leen bien en tema oscuro. | Captura con `cmd uimode night yes` | Sí |
| CA-08 | RF-03 | Sin sesión, Inicio muestra la vinculación guiada. | Revisión de código (no se desvincula la sesión real para probarlo) | No |

## Registro de resultados

| Fecha | Criterio | Método / comando | Resultado | Evidencia / notas |
|---|---|---|---|---|
| 2026-09-24 | CA-01, CA-02, CA-05, CA-07 | Capturas en el Poco (`am start --es tab …`), tema oscuro (el del teléfono) y claro (`cmd uimode night no`, restaurado a `yes`) | ✅ | Inicio: "Cuenta de WhatsApp" con el número enmascarado, "Vinculada · lista", "Protección activa", consulta y últimas llamadas; sin "motor" ni "whatsmeow". Ajustes: los 4 permisos en ✅, tarjeta de MIUI, siempre activo. Correcciones tras la primera captura: verde con más contraste en oscuro (`Brand.ok()`) y pestaña activa en índigo (el verde por defecto recordaba a WhatsApp) |
| 2026-09-24 | CA-04 (parcial) | Captura de Registro | ✅ Vacío correcto | Contadores en 0 y mensaje de lista vacía; falta verlo con llamadas reales durante la semana |
| 2026-09-24 | T1–T4 | `go test`, `build-aar.sh`, `./gradlew :app:assembleDebug`, instalación en el Poco | ✅ | Versión 0.2.0. Causa del `UNLINKED`: el motor se crea bajo demanda y la pantalla leía el valor inicial; ahora Inicio llama a `refresh()` al abrir y en cada `onResume`. Nuevo `LinkedPhone()` en Go. Para capturas: `am start … --es tab log|settings` (MIUI bloquea `input` por adb) |
