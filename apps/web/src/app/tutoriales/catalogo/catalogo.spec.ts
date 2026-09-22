import { readFileSync, readdirSync, statSync } from 'node:fs'
import { join } from 'node:path'
import { describe, expect, it } from 'vitest'
import { validarCatalogo } from '@aportaya/tutoriales/validacion'
import { CATALOGO_DEL_SITIO } from './catalogo'
import { RUTAS_DEL_SITIO } from '../proveer'

/**
 * **La prueba que cuida el catálogo del sitio.** Un recorrido que señala una página que
 * ya no existe, o un elemento que nadie marcó, se descubre acá y no cuando un visitante
 * lo abre.
 */
describe('el catálogo del sitio público', () => {
  it('no tiene ningún problema de configuración', () => {
    expect(validarCatalogo(CATALOGO_DEL_SITIO, RUTAS_DEL_SITIO)).toEqual([])
  })

  it('las rutas declaradas son las que app.routes.ts monta de verdad', () => {
    const rutas = readFileSync(join(__dirname, '../../app.routes.ts'), 'utf8')
    for (const ruta of RUTAS_DEL_SITIO) {
      if (ruta === '/') continue
      expect(rutas, `la ruta ${ruta} no aparece en app.routes.ts`).toContain(`path: '${ruta.slice(1)}'`)
    }
  })

  it('cada objetivo de cada paso está marcado con data-tutorial-id en alguna plantilla', () => {
    const fuentes = leerFuentes(join(__dirname, '../..')) + leerFuentes(join(__dirname, '../../../../../../packages/tutoriales/src'))
    const objetivos = new Set(CATALOGO_DEL_SITIO.flatMap((t) => t.pasos.map((p) => p.objetivo)).filter((o): o is string => o !== undefined))
    const faltantes = [...objetivos].filter((o) => !fuentes.includes(`data-tutorial-id="${o}"`))
    expect(faltantes, 'objetivos sin elemento que los lleve').toEqual([])
  })

  it('ningún tutorial del sitio pide permisos: acá nadie tiene cuenta', () => {
    expect(CATALOGO_DEL_SITIO.every((t) => (t.permisos ?? []).length === 0)).toBe(true)
    expect(CATALOGO_DEL_SITIO.flatMap((t) => t.pasos).every((p) => p.permiso === undefined)).toBe(true)
  })

  it('todos declaran cuánto duran y hay uno para empezar', () => {
    expect(CATALOGO_DEL_SITIO.every((t) => (t.minutos ?? 0) > 0)).toBe(true)
    expect(CATALOGO_DEL_SITIO.some((t) => t.obligatorio === true)).toBe(true)
  })
})

function leerFuentes(raiz: string): string {
  const partes: string[] = []
  const recorrer = (dir: string): void => {
    for (const entrada of readdirSync(dir)) {
      const camino = join(dir, entrada)
      if (statSync(camino).isDirectory()) recorrer(camino)
      else if (camino.endsWith('.ts') && !camino.endsWith('.spec.ts')) partes.push(readFileSync(camino, 'utf8'))
    }
  }
  recorrer(raiz)
  return partes.join('\n')
}
