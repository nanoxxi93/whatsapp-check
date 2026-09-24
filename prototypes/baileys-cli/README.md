# baileys-cli — prototipo de la Fase 0

Herramienta de línea de comandos para validar, antes de construir la app Android, que:

1. Se puede vincular un WhatsApp **con código de emparejamiento** (sin QR).
2. `onWhatsApp()` responde según el [contrato](../../specs/product/tech-stack.md#contrato-de-la-consulta-onwhatsapp-de-baileys).
3. "Conectar en frío + consultar" es lo bastante rápido para hacerlo mientras suena una llamada.

Spec: [specs/2026-09-23-prototipo-baileys-escritorio](../../specs/2026-09-23-prototipo-baileys-escritorio/).

> ⚠️ **Usar solo con un número secundario de pruebas.** Es un cliente no oficial de WhatsApp: la cuenta vinculada podría ser restringida o suspendida.

## Instalación

Requiere Node ≥ 20.

```bash
cd prototypes/baileys-cli
npm install
npm test
```

## Uso

```bash
npm start -- link 987654321                 # vincula el número secundario (pide confirmación)
npm start -- status                         # ¿hay sesión guardada?
npm start -- check 51987654321 51000000001  # consulta uno o varios números
npm start -- check --file numeros.txt       # un "número[,etiqueta]" por línea; # = comentario
npm start -- bench 51987654321 --runs 20 --network wifi-casa
npm start -- unlink                         # cierra la sesión en WhatsApp y borra .auth/
```

Los números sin código de país se interpretan como peruanos (`--country` para cambiarlo).

### Vincular

`link` muestra un código de 8 caracteres. En el teléfono del número secundario: **WhatsApp → Dispositivos vinculados → Vincular un dispositivo → Vincular con el número de teléfono**, y escribirlo. El dispositivo aparece como **"Chrome (Mac OS)"**, la identidad por defecto de Baileys.

Si el teléfono responde *"No se pudo vincular el dispositivo"*, corta con Ctrl+C (se borran las credenciales incompletas) y prueba otra identidad: `--browser ubuntu`, `--browser windows` o `--browser whatscheck` (nombre propio "Google Chrome (WhatsCheck)", vincula, pero con código de emparejamiento el teléfono igual muestra un nombre genérico).

### Resultados

| Símbolo | Significado |
|---|---|
| ✅ tiene WhatsApp | `onWhatsApp` devolvió `exists: true` |
| ⚠️ no tiene WhatsApp | Devolvió `[]` con la sesión abierta y sin error |
| ❔ no verificado | Error, timeout, sesión cerrada o sin conexión. **Nunca** se interpreta como "no tiene" |

Motivos de ❔: `timeout`, `error`, `session_closed`, `no_response` (onWhatsApp devolvió `undefined`), `ambiguous`, `not_connected`.

Cada consulta se agrega a `results/AAAA-MM-DD.jsonl` con el número enmascarado y los tiempos en ms:

| Campo | Qué mide |
|---|---|
| `tWs` | Desde el inicio hasta que el WebSocket abre |
| `tReady` | Desde el inicio hasta que la sesión está lista (`connection: open`) |
| `tQuery` | Duración de la consulta `onWhatsApp` |
| `tTotal` | `tReady + tQuery`, solo en la primera consulta tras conectar (`cold: true`) |

`bench` imprime mínimo, p50, p95 y máximo de cada fase y el veredicto Go / No-Go de la spec.

## Protecciones

- `link` exige confirmar que el número es secundario.
- `check`: pausa aleatoria de 3–6 s entre números y máximo 50 por ejecución (`--max N` para superarlo, con confirmación).
- `bench`: máximo 30 corridas y pausa de 10–15 s entre conexiones.
- Si WhatsApp cierra la sesión (401) o la rechaza (403), se detiene y lo explica.
- Los números se enmascaran en consola, en los logs de Baileys y en los resultados.
- `.auth/` (credenciales) y `results/` están en `.gitignore`.

## Configuración de la sesión

Según [tech-stack §2](../../specs/product/tech-stack.md#configuración-obligatoria-de-la-sesión): `markOnlineOnConnect: false` (Baileys envía presencia `unavailable` y recibos `inactive`, no de lectura), sin sincronizar historial y sin manejador de mensajes. Ver `src/session.ts`.

`LOG_LEVEL=debug npm start -- check …` muestra los logs de Baileys (con números enmascarados).
