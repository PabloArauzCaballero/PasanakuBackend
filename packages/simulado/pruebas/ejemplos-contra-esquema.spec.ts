import Ajv2020 from 'ajv/dist/2020'
import addFormats from 'ajv-formats'
import { existsSync, readFileSync, readdirSync } from 'node:fs'
import { join, resolve } from 'node:path'
import { describe, expect, it } from 'vitest'
import { resolver } from '../src/referencias'
import type { Contrato, Operacion } from '../src/tipos'

/**
 * **La prueba de contrato del simulado**: cada ejemplo que Prism sirve —y que las
 * pruebas de Angular y Flutter consumen— valida contra el esquema de su respuesta. Un
 * ejemplo editado a mano que ya no encaja es una pantalla verde contra un mock roto.
 */
const GENERADO = resolve(__dirname, '../generado')
const EJEMPLOS = resolve(__dirname, '../ejemplos')

// `exclusiveMinimum: true` de OpenAPI 3.0 no es JSON Schema 2020; se tolera lo que el
// contrato use sin convertirlo en un fallo de la prueba.
const ajv = new Ajv2020({ strict: false, allErrors: true })
addFormats(ajv)

function esquemaDe(documento: Contrato, operacion: Operacion, estado: number) {
  const declarada = operacion.responses?.[String(estado)]
  if (!declarada) return null
  const resuelta = resolver(documento, declarada)
  return resuelta.content?.['application/json']?.schema ?? null
}

const servicios = existsSync(GENERADO) ? readdirSync(GENERADO).filter((a) => a.endsWith('.json')) : []

describe('cada ejemplo valida contra su esquema', () => {
  it('hay contratos generados (corré `yarn workspace @aportaya/simulado build`)', () => {
    expect(servicios.length).toBeGreaterThan(0)
  })
  for (const archivo of servicios) {
    const servicio = archivo.replace(/\.json$/, '')
    const documento = JSON.parse(readFileSync(join(GENERADO, archivo), 'utf8')) as Contrato
    const carpeta = join(EJEMPLOS, servicio)
    if (!existsSync(carpeta)) continue
    for (const nombre of readdirSync(carpeta).filter((a) => a.endsWith('.json'))) {
      const ejemplos = JSON.parse(readFileSync(join(carpeta, nombre), 'utf8'))
      it(`${servicio}/${ejemplos.operacion}`, () => {
        const operacion = documento.paths[ejemplos.ruta]?.[ejemplos.metodo]
        expect(operacion, `el contrato ya no tiene ${ejemplos.metodo} ${ejemplos.ruta}`).toBeDefined()
        for (const [escenario, ejemplo] of Object.entries<{ estado: number; cuerpo: unknown }>(ejemplos.escenarios)) {
          if (ejemplo.estado === 503 || ejemplo.estado === 202) continue // los agrega el simulado, no el contrato
          const esquema = esquemaDe(documento, operacion!, ejemplo.estado)
          if (!esquema) continue
          const validar = ajv.compile({ ...esquema, components: documento.components })
          expect(validar(ejemplo.cuerpo), `${escenario} (${ejemplo.estado}): ${ajv.errorsText(validar.errors)}`).toBe(true)
        }
      })
    }
  }
})
