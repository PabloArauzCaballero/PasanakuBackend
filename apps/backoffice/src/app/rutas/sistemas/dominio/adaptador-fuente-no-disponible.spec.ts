import { describe, expect, it } from 'vitest'
import { adaptadorFuenteNoDisponible } from './adaptador-fuente-no-disponible'

describe('adaptadorFuenteNoDisponible · los nueve puertos rechazan con un error tipado', () => {
  const claves = Object.keys(adaptadorFuenteNoDisponible) as (keyof typeof adaptadorFuenteNoDisponible)[]

  it('son exactamente nueve', () => {
    expect(claves).toHaveLength(9)
  })

  it.each(claves)('%s: rechaza con ErrorFuenteNoDisponible, nunca resuelve con datos', async (clave) => {
    await expect(adaptadorFuenteNoDisponible[clave]()).rejects.toMatchObject({
      tipo: 'fuente-no-disponible',
      mensaje: expect.any(String),
    })
  })
})
