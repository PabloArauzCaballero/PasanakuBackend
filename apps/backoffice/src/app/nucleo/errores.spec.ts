import { describe, expect, it } from 'vitest'
import { mensajeDe } from './errores'

/**
 * H12.S1.M1 · defecto E3-e: el backoffice solo traducía `AP-CU68-06`; cualquier otro código de la
 * admisión o de la sustitución caía en «Algo salió mal de nuestro lado», que no dice qué hacer.
 */
const GENERICO = mensajeDe(undefined, 500)

describe('errores · códigos de CU-68 (admisión y sustitución de administrador)', () => {
  const codigos = Array.from({ length: 15 }, (_, i) => `AP-CU68-${String(i + 1).padStart(2, '0')}`)

  for (const codigo of codigos) {
    it(`${codigo} tiene un mensaje accionable y no el genérico de «algo salió mal»`, () => {
      const m = mensajeDe(codigo, 422)
      expect(m).not.toBe(GENERICO)
      expect(m).not.toContain('Algo salió mal')
      expect(m.length).toBeGreaterThan(30)
      expect(m).not.toContain(codigo)
    })
  }

  it('un código desconocido sigue cayendo en el mensaje por estado', () => {
    expect(mensajeDe('AP-CU68-99', 422)).toBe(GENERICO)
    expect(mensajeDe(undefined, 409)).toContain('Revisá el estado')
  })
})

/**
 * Los rechazos del servidor al decidir un expediente (CU-01 flujo 4b) llegan con su
 * texto: un 422 sin traducir dice «algo salió mal de nuestro lado», y el revisor cree
 * que es una falla del sistema cuando es una regla que lo frenó a propósito.
 */
describe('mensajeDe · decisión del expediente', () => {
  it.each([
    ['AP-CU01-08', 'ya fue resuelto'],
    ['AP-CU01-09', 'Faltan fotos'],
    ['AP-CU01-10', 'no tiene fecha de vencimiento'],
    ['AP-CU01-11', 'está vencido'],
  ])('%s se traduce', (codigo, texto) => {
    expect(mensajeDe(codigo, 422)).toContain(texto)
  })

  it('un código desconocido cae en el genérico por estado', () => {
    expect(mensajeDe('AP-CU99-99', 422)).toBe('Algo salió mal de nuestro lado. Probá de nuevo en un momento.')
  })

  it('502, 503 y 504 dicen que el servicio no está disponible y que se puede volver a probar', () => {
    for (const estado of [502, 503, 504]) expect(mensajeDe(undefined, estado)).toBe('Este servicio no está disponible en este momento. Probá de nuevo en unos minutos.')
    expect(mensajeDe(undefined, 501)).toBe('Algo salió mal de nuestro lado. Probá de nuevo en un momento.')
  })
})
