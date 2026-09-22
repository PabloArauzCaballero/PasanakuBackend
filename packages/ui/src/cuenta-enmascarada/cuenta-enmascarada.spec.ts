import { describe, expect, it } from 'vitest'
import { enmascarar } from './cuenta-enmascarada'

describe('enmascarar', () => {
  it('deja solo los últimos cuatro, sin espacios', () => {
    expect(enmascarar('4321 0000 1234')).toBe('•••• 1234')
  })
})
