import { parsePhoneNumberFromString, type CountryCode } from 'libphonenumber-js'

export const DEFAULT_COUNTRY: CountryCode = 'PE'

export class InvalidPhoneError extends Error {
  constructor(input: string) {
    super(`Número no válido: "${maskInText(input)}"`)
    this.name = 'InvalidPhoneError'
  }
}

/**
 * Normaliza a E.164 sin '+' (el formato que espera onWhatsApp), p. ej. "51987654321".
 * Acepta "+51 987 654 321", "51987654321" o el número nacional "987654321".
 * Se usa isPossible() y no isValid(): los números de spam pueden caer en rangos
 * poco comunes y no queremos rechazarlos antes de consultar.
 *
 * Ojo: libphonenumber quita un '0' inicial después del código de país aunque venga con '+'
 * ("+51000000001" → "5100000001"). Solo afecta a números que no pueden existir (IF-007).
 */
export function toE164Digits(input: string, country: CountryCode = DEFAULT_COUNTRY): string {
  const trimmed = input.trim()
  const candidates = trimmed.startsWith('+')
    ? [parsePhoneNumberFromString(trimmed)]
    : [parsePhoneNumberFromString(trimmed, country), parsePhoneNumberFromString(`+${trimmed.replace(/\D/g, '')}`)]

  const parsed = candidates.find((p) => p?.isPossible())
  if (!parsed) throw new InvalidPhoneError(input)
  return parsed.number.slice(1)
}

/** "51987654388" → "+5198*****88". Conserva los 4 primeros y los 2 últimos dígitos. */
export function maskPhone(digits: string): string {
  const clean = digits.replace(/\D/g, '')
  if (clean.length <= 6) return `+${'*'.repeat(clean.length)}`
  return `+${clean.slice(0, 4)}${'*'.repeat(clean.length - 6)}${clean.slice(-2)}`
}

/** Enmascara cualquier secuencia de 7+ dígitos dentro de un texto (logs, jids, errores). */
export function maskInText(text: string): string {
  return text.replace(/\+?\d{7,}/g, (m) => maskPhone(m))
}
