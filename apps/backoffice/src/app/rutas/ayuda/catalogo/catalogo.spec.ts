import { readFileSync, readdirSync, statSync } from 'node:fs'
import { join } from 'node:path'
import { describe, expect, it } from 'vitest'
import { validarCatalogo } from '@aportaya/tutoriales/validacion'
import { CATALOGO_DE_TUTORIALES } from './catalogo'
import { RUTAS_CONOCIDAS } from './proveer'

/**
 * **La prueba que cuida el catálogo real.** Un tutorial que apunta a una pantalla que
 * ya no existe, o a un objetivo que nadie marcó, se descubre acá y no cuando alguien
 * lo abre.
 */
describe('el catálogo del backoffice', () => {
  it('no tiene ningún problema de configuración', () => {
    expect(validarCatalogo(CATALOGO_DE_TUTORIALES, RUTAS_CONOCIDAS)).toEqual([])
  })

  it('las rutas declaradas como conocidas son las que app.routes.ts monta de verdad', () => {
    const rutas = readFileSync(join(__dirname, '../../../app.routes.ts'), 'utf8')
    for (const ruta of RUTAS_CONOCIDAS) {
      expect(rutas, `la ruta ${ruta} no aparece en app.routes.ts`).toContain(`path: '${ruta.slice(1)}'`)
    }
  })

  it('cada objetivo de cada paso está marcado con data-tutorial-id en alguna plantilla', () => {
    // Las anclas del propio centro de tutoriales viven en el paquete compartido, no en
    // `src/app`: se miran los dos árboles.
    const fuentes = leerFuentes(join(__dirname, '../../..')) + leerFuentes(join(__dirname, '../../../../../../../packages/tutoriales/src'))
    const faltantes = objetivos().filter((o) => !fuentes.includes(`data-tutorial-id="${o}"`) && !fuentes.includes(`'${prefijoDinamico(o)}' +`))
    expect(faltantes, 'objetivos sin elemento que los lleve').toEqual([])
  })

  it('todo tutorial con ruta puede lanzarse desde esa pantalla', () => {
    for (const t of CATALOGO_DE_TUTORIALES) {
      if (t.ruta === undefined) continue
      expect(RUTAS_CONOCIDAS.some((r) => t.ruta === r || t.ruta?.startsWith(`${r}/`))).toBe(true)
    }
  })

  it('hay al menos un tutorial obligatorio y todos declaran cuánto duran', () => {
    expect(CATALOGO_DE_TUTORIALES.some((t) => t.obligatorio === true)).toBe(true)
    expect(CATALOGO_DE_TUTORIALES.every((t) => (t.minutos ?? 0) > 0)).toBe(true)
  })

  it('ningún paso pide una acción que escriba en el servidor', () => {
    const tipos = CATALOGO_DE_TUTORIALES.flatMap((t) => t.pasos.map((p) => p.accion?.tipo ?? 'ninguna'))
    expect([...new Set(tipos)].every((tipo) => ['ninguna', 'clic', 'escribir', 'elegir', 'navegar', 'aparezca'].includes(tipo))).toBe(true)
  })
})

const objetivos = (): string[] => [...new Set(CATALOGO_DE_TUTORIALES.flatMap((t) => t.pasos.map((p) => p.objetivo).filter((o): o is string => o !== undefined)))]

/** `menu-operacion` lo pinta un `[attr.data-tutorial-id]="'menu-' + s.ruta"`. */
const prefijoDinamico = (objetivo: string): string => `${objetivo.split('-')[0]}-`

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
