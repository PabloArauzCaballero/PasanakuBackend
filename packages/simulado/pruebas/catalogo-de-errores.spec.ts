import { existsSync, readFileSync, readdirSync } from 'node:fs'
import { join, resolve } from 'node:path'
import { describe, expect, it } from 'vitest'
import { construirCatalogo } from '../src/catalogo'

/**
 * **El catálogo de errores del simulado**: un rechazo simulado solo puede usar un código
 * `AP-CUnn-nn` que el contrato de esa operación (o el caso de uso) declare. Un código
 * inventado deja a la pantalla probando un mensaje que el backend nunca va a enviar.
 *
 * Fuentes del catálogo: comentarios `# AP-CUnn-nn` del YAML de cada contrato y las tablas de
 * `docs/CasosDeUso/*.md` (ver `src/catalogo.ts`).
 */
const GENERADO = resolve(__dirname, '../generado')
const EJEMPLOS = resolve(__dirname, '../ejemplos')

const servicios = existsSync(GENERADO) ? readdirSync(GENERADO).filter((a) => a.endsWith('.json')).map((a) => a.replace(/\.json$/, '')) : []
const origen = existsSync(join(GENERADO, 'ORIGEN.txt')) ? readFileSync(join(GENERADO, 'ORIGEN.txt'), 'utf8').trim() : null
const carpetaCU = [origen ? join(origen, 'docs/CasosDeUso') : '', resolve(__dirname, '../../../docs/CasosDeUso')].find((c) => c !== '' && existsSync(c))

const contratos = servicios.map((servicio) => ({
  servicio,
  yaml: readFileSync(join(GENERADO, `${servicio}.yaml`), 'utf8'),
  documento: JSON.parse(readFileSync(join(GENERADO, `${servicio}.json`), 'utf8')),
}))
const catalogo = construirCatalogo(contratos, carpetaCU)

type Rechazo = { servicio: string; operacion: string; estado: number; codigo: unknown }
const rechazos: Rechazo[] = []
for (const servicio of servicios) {
  const carpeta = join(EJEMPLOS, servicio)
  if (!existsSync(carpeta)) continue
  for (const nombre of readdirSync(carpeta).filter((a) => a.endsWith('.json'))) {
    const ejemplos = JSON.parse(readFileSync(join(carpeta, nombre), 'utf8'))
    const rechazo = ejemplos.escenarios?.rechazo
    if (rechazo) rechazos.push({ servicio, operacion: ejemplos.operacion, estado: rechazo.estado, codigo: rechazo.cuerpo?.codigo })
  }
}

describe('el catálogo de errores', () => {
  it('se construye desde los contratos y no está vacío', () => {
    expect(catalogo.global.size).toBeGreaterThan(50)
    expect(rechazos.length).toBeGreaterThan(50)
  })

  it('cada rechazo simulado usa un código que existe en el catálogo', () => {
    const fuera = rechazos.filter((r) => typeof r.codigo !== 'string' || !catalogo.global.has(r.codigo))
    expect(fuera.map((r) => `${r.servicio}/${r.operacion} → ${String(r.codigo)}`)).toEqual([])
  })

  it('cuando el contrato menciona códigos junto a la operación, el rechazo usa uno de ellos', () => {
    const ajenos = rechazos
      .filter((r) => (catalogo.porOperacion.get(`${r.servicio}/${r.operacion}`) ?? []).length > 0)
      .filter((r) => !(catalogo.porOperacion.get(`${r.servicio}/${r.operacion}`) ?? []).some((e) => e.codigo === r.codigo))
    expect(ajenos.map((r) => `${r.servicio}/${r.operacion} → ${String(r.codigo)}`)).toEqual([])
  })

  it('los rechazos de negocio responden 4xx, nunca 2xx con cuerpo de error', () => {
    expect(rechazos.filter((r) => r.estado < 400).map((r) => `${r.servicio}/${r.operacion}`)).toEqual([])
  })
})
