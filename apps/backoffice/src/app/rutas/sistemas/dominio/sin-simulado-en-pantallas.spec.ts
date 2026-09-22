import { readFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { describe, expect, it } from 'vitest'

/**
 * H2.S2.M4 — test de arquitectura: si alguna pantalla de `sistemas/` vuelve a
 * importar `@aportaya/simulado` directo (la forma en que el hallazgo original entraba
 * al bundle de producción), este spec falla. Las pantallas inyectan su puerto; quién
 * implementa el puerto es responsabilidad exclusiva de `proveedor-fuentes*.ts`.
 */
const RAIZ_SISTEMAS = join(dirname(fileURLToPath(import.meta.url)), '..')

const PANTALLAS = [
  'servicios/pantalla-servicios.ts',
  'despliegues/pantalla-despliegues.ts',
  'base-datos/pantalla-base-datos.ts',
  'respaldos/pantalla-respaldos.ts',
  'proveedores/pantalla-proveedores.ts',
  'outbox/pantalla-outbox.ts',
  'webhooks/pantalla-webhooks.ts',
  'accesos/pantalla-accesos.ts',
  'incidentes/pantalla-incidentes.ts',
]

describe('sin-simulado-en-pantallas · ninguna pantalla de sistemas/ importa @aportaya/simulado', () => {
  it.each(PANTALLAS)('%s no menciona @aportaya/simulado', (relativo) => {
    const contenido = readFileSync(join(RAIZ_SISTEMAS, relativo), 'utf-8')
    expect(contenido).not.toContain('@aportaya/simulado')
  })

  it('solo proveedor-fuentes.demo.ts (nunca proveedor-fuentes.ts, el de producción) menciona @aportaya/simulado', () => {
    const seguro = readFileSync(join(RAIZ_SISTEMAS, 'dominio/proveedor-fuentes.ts'), 'utf-8')
    expect(seguro).not.toContain('@aportaya/simulado')
  })
})
