import { describe, expect, it } from 'vitest'
import { restanteDe } from './reloj-de-plazo'

const ahora = new Date('2026-09-10T15:00:00Z')
/** Las mismas frases que `RelojDePlazo.restante` en Flutter. */
describe('restanteDe', () => {
  it.each([
    ['2026-09-13T15:00:00Z', 'Faltan 3 días'],
    ['2026-09-11T16:00:00Z', 'Faltan 1 día'],
    ['2026-09-10T20:00:00Z', 'Faltan 5 h'],
    ['2026-09-10T15:30:00Z', 'Vence hoy'],
    ['2026-09-09T15:00:00Z', 'Vencido'],
  ])('%s → %s', (vence, esperado) => expect(restanteDe(new Date(vence), ahora)).toBe(esperado))
})
