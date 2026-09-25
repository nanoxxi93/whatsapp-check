# Plan — Diseño de la app de pruebas

> **Estado:** En progreso · **Fecha:** 2026-09-24
> **Relacionado:** [spec.md](spec.md) · [checklist.md](checklist.md)

## Diseño

| Módulo | Cambio |
|---|---|
| `engine-whatsmeow/go/wacheck.go` | `LinkedPhone()`: dígitos del número de la sesión (vacío si no hay) |
| `WhatsmeowEngine.kt` | `refresh()`: crea el motor en segundo plano y publica el estado real; `linkedPhone()` |
| `ui/Theme.kt` | Paleta propia (índigo + verde de "verificado"), esquemas claro y oscuro |
| `ui/HomeScreen.kt` | Cuenta, protección, consulta manual, últimas llamadas; vinculación guiada si no hay sesión |
| `ui/LogScreen.kt` | Resumen + lista de llamadas + copiar/exportar |
| `ui/SettingsScreen.kt` | Permisos, MIUI, siempre activo, aviso de ejemplo, desvincular, versión |
| `MainActivity.kt` | `Scaffold` + `NavigationBar` con las tres vistas (estado simple, sin librería de navegación) |
| `call/Journal.kt` | `calls()` para la lista del registro |
| `res/` | Ícono adaptable (`mipmap-anydpi-v26`), primer plano y monocromo vectoriales, ícono de notificación |

**Ícono:** fondo índigo (`#283593`), auricular blanco y una insignia verde con un check abajo a la derecha. Significa "llamada verificada" y no se parece al logo de WhatsApp (globo verde).

## Decisiones

| Decisión | Alternativas | Motivo |
|---|---|---|
| Navegación con estado propio | `navigation-compose` | Tres pestañas no justifican una dependencia nueva (RNF-03) |
| Íconos de `material-icons-core` | `material-icons-extended` | El extendido agrega varios MB sin minificar; los básicos alcanzan |
| Ícono vectorial adaptable | PNG por densidad | minSdk 29 ≥ 26: basta el adaptable, con monocromo para Android 13+ |
| Desvincular en Ajustes con confirmación | En Inicio | Acción destructiva; lejos del uso diario |



