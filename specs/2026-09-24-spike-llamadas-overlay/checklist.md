# Validación — Spike de `CallScreeningService` + overlay

> **Estado:** En progreso · **Fecha:** 2026-09-24
> **Relacionado:** [spec.md](spec.md) · [plan.md](plan.md)

## Criterios de aceptación

| ID | Requisito | Criterio (verificable) | Método | Obligatorio |
|---|---|---|---|---|
| CA-01 | RF-01 | La app obtiene el rol de filtrado de llamadas en el Poco y en el Moto; la pantalla muestra "Rol: concedido". | Manual + `adb shell cmd role get-role-holders android.app.role.CALL_SCREENING` | Sí |
| CA-02 | RF-02, RNF-01 | En cada llamada de un número no guardado, `respondToCall(allow)` sale en ≤ 100 ms (`tScreen` en `calls.jsonl`) y el teléfono **sigue sonando** normalmente. | Llamadas reales + log | Sí |
| CA-03 | RF-02 | Una llamada de un número **guardado** no pasa por el servicio (no aparece en `calls.jsonl` ni muestra aviso). | Llamada real | Sí |
| CA-04 | RF-04, RNF-02 | El aviso aparece con "Verificando…" y cambia al resultado; el tiempo hasta el resultado (`tResult`) tiene p95 ≤ 3 s en las llamadas de la semana (mínimo 5). | Llamadas reales + registro exportado | Sí |
| CA-05 | RF-04 | Los resultados son coherentes: ✅ y ⚠️ aparecen según el número (el autor puede verificar alguno buscándolo en WhatsApp después) y ningún ❔ sin motivo registrado. | Registro + verificación puntual del autor | Sí |
| CA-06 | RNF-03 | Captura de al menos una llamada entrante en el Poco (tomada por el autor con el propio teléfono): el aviso **no tapa** contestar ni colgar. | Captura | Sí |
| CA-07 | RF-04 | El aviso se puede arrastrar, se cierra con un toque y se oculta solo a los 45 s. | Manual | Sí |
| CA-08 | RNF-05, RF-08 | Durante la semana funciona también con la pantalla bloqueada y aunque el autor quite la app de recientes; el registro de salud no muestra huecos de más de 2 h sin latido (salvo teléfono apagado). | Registro exportado | Sí |
| CA-09 | RF-05 | Sin permiso de overlay: la llamada suena (respuesta ≤ 4 s) y llega la notificación heads-up con el resultado. | Llamada real con el permiso revocado | No *(en la semana de uso real se usa el overlay; se prueba si hay ocasión)* |
| CA-10 | RF-03 | Número oculto o no normalizable → ❔ sin consultar. | Llamada con número oculto (si el autor puede) o test del normalizador | No |
| CA-11 | RF-06, RNF-04 | `calls.jsonl` tiene números enmascarados; `logcat` de la app no muestra números completos; el manifest no pide `READ_CONTACTS`, `READ_CALL_LOG` ni `READ_PHONE_STATE`. | Revisión | Sí |
| CA-12 | RF-08 | El servicio "siempre activo" arranca solo al encender el teléfono y su notificación es de prioridad mínima (no suena ni aparece como emergente). | Reinicio del Poco + registro | Sí |
| CA-13 | RF-09 | El resumen en pantalla coincide con el registro, y "Exportar" comparte los archivos sin números completos, sin necesidad de PC. | Manual en el teléfono | Sí |
| CA-14 | RF-11 | La notificación de sesión caída se ve con el ícono propio y abre la app; la lógica avisa una sola vez por problema. Si durante la semana la sesión se cae de verdad, llega la notificación. | `action alert-test` por adb + revisión de código | Sí |

## Criterios Go / No-Go

| Métrica | Go | Go con reservas | No-Go |
|---|---|---|---|
| Aviso visible sobre la pantalla de llamada | En ambos teléfonos, bloqueados y desbloqueados | Solo con permisos especiales de MIUI, o solo desbloqueado | No se ve en ninguno → replantear (solo notificación) |
| `tResult` p95 (10 llamadas) | ≤ 3 s | 3–8 s | > 8 s |
| Llamadas afectadas por la app (silenciadas, perdidas, cortadas) | 0 | — | ≥ 1 |
| Con la app quitada de recientes / durante la semana | Funciona, sin huecos en los latidos | Funciona solo con "Inicio automático" / exclusión de batería, o con huecos cortos | La app deja de filtrar llamadas |

## Plan de pruebas

### Automáticas
- `./gradlew :engine-api:test` y `go test` siguen en verde (no hay lógica nueva pura en este spike; la normalización usa `PhoneNumberUtils` del sistema).

### Manuales
Con el Moto llamando al Poco (número no guardado):
1. Conceder rol, overlay y notificaciones en el Poco; permisos de MIUI (CA-01).
2. 5 llamadas con la app en segundo plano y 5 con la app quitada de recientes; la mitad con la pantalla bloqueada (CA-02, CA-04, CA-05, CA-08).
3. Captura durante una llamada entrante en cada teléfono (CA-06).
4. Arrastrar, tocar y esperar 45 s (CA-07).
5. Revocar el overlay y llamar (CA-09).
6. Llamada desde un número guardado (CA-03).

## Definición de terminado

- [ ] Todos los criterios obligatorios pasaron y están en el registro.
- [ ] Tests existentes en verde.
- [ ] Sin números completos en git, logs versionados ni documentos.
- [ ] Tareas de [plan.md](plan.md) marcadas.
- [ ] tech-stack actualizado según el resultado (con aprobación); permisos de MIUI anotados para la Fase 1.
- [ ] Ítem marcado `[x]` en el roadmap y app instalada en el Poco para la prueba de resistencia al baneo.

## Registro de resultados

| Fecha | Criterio | Método / comando | Resultado | Evidencia / notas |
|---|---|---|---|---|
| 2026-09-24 | T1–T4b (compilación) | `./gradlew :app:assembleDebug` | ✅ Compila | `ScreeningService`, `CallCheck`, `OverlayController`, `Notifier`, `KeepAliveService` (`specialUse`), `BootReceiver`, `Journal` (resumen + exportar con `FileProvider` limitado a `files/results/`) |
| 2026-09-24 | CA-12 (parcial) | Instalación en el Poco + `dumpsys activity services` | ✅ Servicio activo | `KeepAliveService` con `isForeground=true` en el canal `servicio_activo` (importancia mínima). Falta comprobar el arranque tras reiniciar el teléfono |
| 2026-09-24 | CA-14 (visual) | `am start … BenchActivity --es action alert-test` + `cmd statusbar expand-notifications` | ✅ | "WhatsCheck se desvinculó de tu WhatsApp · Ya no se verifican las llamadas. Abre la app para vincular de nuevo.", con el ícono propio. La prueba no toca la sesión ni marca el aviso como enviado |
| 2026-09-24 | CA-11 (manifest) | Revisión de `AndroidManifest.xml` | ✅ | Sin `READ_CONTACTS`, `READ_CALL_LOG` ni `READ_PHONE_STATE`. `ScreeningService` protegido con `BIND_SCREENING_SERVICE` |

## Bloqueos

