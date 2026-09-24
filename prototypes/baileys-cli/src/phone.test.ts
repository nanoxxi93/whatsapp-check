import { test } from 'node:test'
import assert from 'node:assert/strict'
import { InvalidPhoneError, maskInText, maskPhone, toE164Digits } from './phone.ts'

test('normaliza formatos peruanos a E.164 sin +', () => {
  assert.equal(toE164Digits('987654321'), '51987654321') // nacional
  assert.equal(toE164Digits('51987654321'), '51987654321') // internacional sin +
  assert.equal(toE164Digits('+51 987 654 321'), '51987654321') // con + y espacios
  assert.equal(toE164Digits('+51-987-654-321'), '51987654321')
})

test('acepta números de otros países con + o sin + si no son peruanos', () => {
  assert.equal(toE164Digits('+52 55 1234 5678'), '525512345678')
  assert.equal(toE164Digits('+55 11 91234 5678'), '5511912345678')
  assert.equal(toE164Digits('5511912345678'), '5511912345678')
})

test('libphonenumber quita el 0 después del código de país, con o sin + (IF-007)', () => {
  // Documenta el comportamiento: solo pasa con números imposibles (en PE nada empieza con 0 tras el 51)
  assert.equal(toE164Digits('51000000001'), '5100000001')
  assert.equal(toE164Digits('+51000000001'), '5100000001')
  assert.equal(toE164Digits('51900000001'), '51900000001')
})

test('un número nacional que empieza con 51 (fijo de Puno) sigue siendo nacional', () => {
  assert.equal(toE164Digits('51123456'), '5151123456')
})

test('rechaza entradas que no son números', () => {
  assert.throws(() => toE164Digits('hola'), InvalidPhoneError)
  assert.throws(() => toE164Digits('123'), InvalidPhoneError)
  assert.throws(() => toE164Digits(''), InvalidPhoneError)
})

test('el error no expone el número completo', () => {
  try {
    toE164Digits('99999999999999999999')
    assert.fail('debió lanzar')
  } catch (e) {
    assert.ok(!(e as Error).message.includes('99999999999999999999'))
  }
})

test('enmascara conservando 4 primeros y 2 últimos dígitos', () => {
  assert.equal(maskPhone('51987654388'), '+5198*****88')
  assert.equal(maskPhone('+51987654388'), '+5198*****88')
  assert.equal(maskPhone('123456'), '+******')
})

test('enmascara números dentro de un texto', () => {
  assert.equal(maskInText('jid 51987654388@s.whatsapp.net falló'), 'jid +5198*****88@s.whatsapp.net falló')
  assert.equal(maskInText('{"id":"51987654388:12@s.whatsapp.net"}'), '{"id":"+5198*****88:12@s.whatsapp.net"}')
  assert.equal(maskInText('código 428, 20 s'), 'código 428, 20 s')
})
