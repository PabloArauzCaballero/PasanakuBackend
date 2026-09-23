import { describe, expect, it } from 'vitest'
import { adaptadorSimuladoSistemas } from './adaptador'

describe('adaptadorSimuladoSistemas · emite datos y nunca falla', () => {
  const claves = Object.keys(adaptadorSimuladoSistemas) as (keyof typeof adaptadorSimuladoSistemas)[]

  it('son exactamente nueve puertos', () => {
    expect(claves).toHaveLength(9)
  })

  it.each(claves)('%s: resuelve (nunca rechaza)', async (clave) => {
    await expect(adaptadorSimuladoSistemas[clave]()).resolves.toBeDefined()
  })

  it('servicios: trae los cuatro servicios de ejemplo', async () => {
    const r = await adaptadorSimuladoSistemas.servicios()
    expect(r.length).toBeGreaterThan(0)
  })

  it('outbox: trae mensajes y descartados juntos', async () => {
    const r = await adaptadorSimuladoSistemas.outbox()
    expect(r.mensajes.length).toBeGreaterThan(0)
    expect(r.descartados.length).toBeGreaterThan(0)
  })

  it('despliegues: trae despliegues e interruptores juntos', async () => {
    const r = await adaptadorSimuladoSistemas.despliegues()
    expect(r.despliegues.length).toBeGreaterThan(0)
    expect(r.interruptores.length).toBeGreaterThan(0)
  })
})
