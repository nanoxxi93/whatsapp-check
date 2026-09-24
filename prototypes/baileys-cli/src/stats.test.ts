import { test } from 'node:test'
import assert from 'node:assert/strict'
import { percentile, summarize, verdict } from './stats.ts'

test('summarize con valores conocidos (1..20)', () => {
  const values = Array.from({ length: 20 }, (_, i) => 20 - i) // desordenado a propósito
  assert.deepEqual(summarize(values), { n: 20, min: 1, p50: 10, p95: 19, max: 20 })
})

test('summarize con un solo valor', () => {
  assert.deepEqual(summarize([1234]), { n: 1, min: 1234, p50: 1234, p95: 1234, max: 1234 })
})

test('summarize con lista vacía devuelve null', () => {
  assert.equal(summarize([]), null)
})

test('percentile por rango más cercano', () => {
  const sorted = [100, 200, 300, 400, 500, 600, 700, 800, 900, 1000]
  assert.equal(percentile(sorted, 50), 500)
  assert.equal(percentile(sorted, 95), 1000)
  assert.equal(percentile(sorted, 0), 100)
})

test('summarize no modifica la lista original', () => {
  const values = [3, 1, 2]
  summarize(values)
  assert.deepEqual(values, [3, 1, 2])
})

test('verdict: umbrales de latencia', () => {
  assert.equal(verdict(3000, 0), 'GO')
  assert.equal(verdict(3001, 0), 'GO_CON_RESERVAS')
  assert.equal(verdict(8000, 0), 'GO_CON_RESERVAS')
  assert.equal(verdict(8001, 0), 'NO_GO')
})

test('verdict: el peor criterio manda', () => {
  assert.equal(verdict(1000, 1), 'GO_CON_RESERVAS')
  assert.equal(verdict(1000, 3), 'NO_GO')
  assert.equal(verdict(9000, 0), 'NO_GO')
})

test('localDate usa la fecha local con ceros a la izquierda', async () => {
  const { localDate } = await import('./output.ts')
  assert.equal(localDate(new Date(2026, 0, 5, 23, 30)), '2026-01-05')
})
