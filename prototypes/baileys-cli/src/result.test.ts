import { test } from 'node:test'
import assert from 'node:assert/strict'
import { jidUser, toCheckResult } from './result.ts'

const N = '51987654321'
const JID = `${N}@s.whatsapp.net`
const open = (entries: { jid: string; exists: boolean }[] | undefined) =>
  ({ kind: 'response', entries, sessionOpen: true }) as const

test('exists: true → HAS_WHATSAPP', () => {
  assert.deepEqual(toCheckResult(N, open([{ jid: JID, exists: true }])), {
    status: 'HAS_WHATSAPP',
    jid: JID,
    jidMismatch: false,
  })
})

test('[] con sesión abierta → NO_WHATSAPP', () => {
  assert.deepEqual(toCheckResult(N, open([])), { status: 'NO_WHATSAPP', jidMismatch: false })
})

test('exists: false (versiones anteriores de Baileys) → NO_WHATSAPP', () => {
  assert.equal(toCheckResult(N, open([{ jid: JID, exists: false }])).status, 'NO_WHATSAPP')
})

test('jid distinto con una sola entrada → se acepta y se marca jidMismatch', () => {
  // p. ej. México: se envía 52155… y WhatsApp responde 5255…
  const r = toCheckResult('5215512345678', open([{ jid: '525512345678@s.whatsapp.net', exists: true }]))
  assert.deepEqual(r, { status: 'HAS_WHATSAPP', jid: '525512345678@s.whatsapp.net', jidMismatch: true })
})

test('varias entradas: se usa la que coincide exactamente', () => {
  const r = toCheckResult(N, open([
    { jid: '51900000000@s.whatsapp.net', exists: true },
    { jid: JID, exists: true },
  ]))
  assert.equal(r.jid, JID)
  assert.equal(r.jidMismatch, false)
})

test('varias entradas y ninguna coincide → UNKNOWN ambiguous', () => {
  const r = toCheckResult(N, open([
    { jid: '51900000000@s.whatsapp.net', exists: true },
    { jid: '51900000001@s.whatsapp.net', exists: true },
  ]))
  assert.deepEqual(r, { status: 'UNKNOWN', jidMismatch: false, reason: 'ambiguous' })
})

test('onWhatsApp devolvió undefined → UNKNOWN no_response', () => {
  assert.equal(toCheckResult(N, open(undefined)).reason, 'no_response')
})

test('error lanzado → UNKNOWN, nunca NO_WHATSAPP', () => {
  const r = toCheckResult(N, { kind: 'error', error: new Error('boom') })
  assert.deepEqual(r, { status: 'UNKNOWN', jidMismatch: false, reason: 'error' })
})

test('timeout → UNKNOWN, nunca NO_WHATSAPP', () => {
  assert.deepEqual(toCheckResult(N, { kind: 'timeout' }), { status: 'UNKNOWN', jidMismatch: false, reason: 'timeout' })
})

test('[] con la sesión cerrada → UNKNOWN session_closed, nunca NO_WHATSAPP', () => {
  const r = toCheckResult(N, { kind: 'response', entries: [], sessionOpen: false })
  assert.deepEqual(r, { status: 'UNKNOWN', jidMismatch: false, reason: 'session_closed' })
})

test('jidUser ignora el sufijo de dispositivo', () => {
  assert.equal(jidUser('51987654321:12@s.whatsapp.net'), N)
  assert.equal(jidUser(JID), N)
})
