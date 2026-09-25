# Hoja de Tareas


- [x] **T1** — Estructura de `prototypes/baileys-cli/`: `package.json` con versiones exactas, `tsconfig.json`, `.gitignore` (`.auth/`, `results/`, `node_modules/`), scripts `start` y `test`. · cubre `RNF-03`, `RNF-04`
- [x] **T2** — `phone.ts`: normalizar a E.164 sin `+` (país por defecto PE) y enmascarar; con tests. · cubre `RNF-02`, `RF-01`
- [x] **T3** — `result.ts`: traducir la respuesta de `onWhatsApp` a `CheckResult` con coincidencia por `jid`; tests con todos los casos del contrato (`exists: true`, `[]`, `exists: false`, `jid` distinto, error, sesión cerrada). · cubre `RF-04`, `RF-05`, `RNF-06`
- [x] **T4** — `session.ts`: crear socket con la configuración obligatoria, credenciales en `.auth/`, medición de `t_ws` y `t_ready`, timeout de 15 s y manejo de motivos de desconexión (sesión cerrada / cuenta restringida). · cubre `RF-02`, `RF-06`, `RF-08`, `RF-09`, `RNF-05`
- [x] **T5** — Comando `link`: confirmación de número secundario, `requestPairingCode()`, pasos en pantalla, espera de `open`. · cubre `RF-01`, `RF-02`
- [x] **T6** — `check.ts` + comando `check` (argumentos y `--file`): timeout de 5 s por consulta, pausa 3–6 s, tope de 50. · cubre `RF-03`, `RF-04`, `RF-06`, `RNF-01`, `RNF-05`
- [x] **T7** — `output.ts`: tabla en consola con números enmascarados + JSONL en `results/`. · cubre `RF-11`, `RNF-02`
- [x] **T8** — `stats.ts` (con tests) + comando `bench --runs N`: tope de 30, pausa ≥ 10 s entre conexiones. · cubre `RF-07`, `RNF-01`
- [x] **T9** — Comandos `status` y `unlink`. · cubre `RF-10`
- [x] **T10** — `README.md` con instalación, uso, advertencia de número secundario y cómo leer los resultados.
- [x] **T11** — Validación manual con el número secundario del autor (ver [checklist.md](checklist.md)). Requiere al autor.

