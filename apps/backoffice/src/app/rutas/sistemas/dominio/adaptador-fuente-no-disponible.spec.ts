import { describe, expect, it } from 'vitest'
import { adaptadorFuenteNoDisponible } from './adaptador-fuente-no-disponible'

describe('adaptadorFuenteNoDisponible · los siete puertos rechazan con un error tipado', () => {
  const claves = Object.keys(adaptadorFuenteNoDisponible) as (keyof typeof adaptadorFuenteNoDisponible)[]

  it('son exactamente siete (Base de datos y Outbox leen su contrato directo desde 2026-10-28/29)', () => {
    expect(claves).toHaveLength(7)
  })

  it.each(claves)('%s: rechaza con ErrorFuenteNoDisponible, nunca resuelve con datos', async (clave) => {
    await expect(adaptadorFuenteNoDisponible[clave]()).rejects.toMatchObject({
      tipo: 'fuente-no-disponible',
      mensaje: expect.any(String),
    })
  })
})
