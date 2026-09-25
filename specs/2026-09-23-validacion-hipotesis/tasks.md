# Hoja de Tareas


- [ ] **T1** — `labels.ts` + validación en `readTargets` (etiqueta inválida → aviso y se omite) + deduplicación por número normalizado; tests. · cubre `RF-01`, `RF-03`
- [ ] **T2** — `phone-id.ts` (secreto generado la primera vez) + `phoneId` en cada registro; tests (estable, distinto para números distintos, sin colisiones en el rango `93696…`). · cubre `RF-02`, `RNF-03`
- [ ] **T3** — `analysis.ts`: Wilson, agrupación, último resultado por `phoneId`, sensibilidad, especificidad, precisión estimada y veredicto; tests con casos conocidos y bordes. · cubre `RF-04`, `RF-05`, `RF-06`, `RNF-04`
- [ ] **T4** — Comando `analyze [--since AAAA-MM-DD]` con tabla y veredicto. · cubre `RF-04`, `RF-05`, `RF-06`
- [ ] **T5** — `check --business` (si se aprueba RF-07). · cubre `RF-07`
- [ ] **T6** — README: formato de datos, etiquetas, cómo recolectar, `analyze`.
- [ ] **T7** — **Lote preliminar:** el autor etiqueta los 23 números del primer lote → `check --file data/llamadas-recibidas.csv` → `analyze`. Resultado preliminar en checklist.md (solo cifras). · Requiere al autor
- [ ] **T8** — **Recolección:** el autor agrega llamadas nuevas y el grupo `legitimo` hasta ≥ 30 por grupo; ≤ 1 lote por día. · Requiere al autor
- [ ] **T9** — `analyze` final → veredicto en checklist.md; actualizar la hipótesis en `mission.md` con los datos (con aprobación). · cubre objetivo del ítem

