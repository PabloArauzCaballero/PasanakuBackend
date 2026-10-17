/**
 * Quién dice ser la persona en el ingreso: su correo o su teléfono, en un solo campo.
 *
 * El contrato (`EntradaAutenticacion`) exige **exactamente uno** de `correo` o
 * `telefonoE164`; esta función decide cuál con una regla que la persona entiende sin
 * leerla: si tiene arroba es un correo, si no, son los 8 dígitos del teléfono.
 */
export type Identificador = { correo: string } | { telefonoE164: string }

export type LecturaDeIdentificador = { ok: true; valor: Identificador } | { ok: false; error: string }

/** Lo mínimo para no mandar al servidor algo que seguro rebota; el servidor valida igual. */
const CORREO = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const OCHO_DIGITOS = /^\d{8}$/

export function leerIdentificador(texto: string): LecturaDeIdentificador {
  const limpio = texto.trim()
  if (!limpio) return { ok: false, error: 'Escribí tu correo o tu teléfono.' }

  if (limpio.includes('@')) {
    return CORREO.test(limpio)
      ? { ok: true, valor: { correo: limpio } }
      : { ok: false, error: 'Ese correo no está completo. Ejemplo: nombre@aportaya.bo' }
  }

  const digitos = limpio.replace(/\D/g, '').replace(/^591(?=\d{8}$)/, '')
  return OCHO_DIGITOS.test(digitos)
    ? { ok: true, valor: { telefonoE164: `+591${digitos}` } }
    : { ok: false, error: 'El teléfono son 8 dígitos, sin el +591.' }
}
