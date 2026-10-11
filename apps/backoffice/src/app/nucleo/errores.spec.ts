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
