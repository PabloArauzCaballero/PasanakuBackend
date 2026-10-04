import { describe, expect, it } from 'vitest'
import { mensajeDe } from './errores'

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
})
