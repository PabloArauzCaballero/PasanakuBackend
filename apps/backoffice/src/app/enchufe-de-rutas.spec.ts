import { readFileSync, readdirSync, rmSync, statSync, writeFileSync } from 'node:fs'
import { join } from 'node:path'
import { describe, expect, it } from 'vitest'

/**
 * **La prueba del gate de F0**: agregar una ruta no toca nada fuera del directorio de
 * su dominio. `app.routes.ts` carga cada `<dominio>.routes.ts` por `loadChildren` una
 * vez y se congela.
 */
const APP = __dirname
const DOMINIOS = ['operacion', 'cumplimiento', 'sistemas', 'contabilidad', 'publicidad', 'ayuda']
const archivosDe = (c: string): string[] =>
  readdirSync(c).flatMap((e) => (statSync(join(c, e)).isDirectory() ? archivosDe(join(c, e)) : [join(c, e)]))

describe('el enchufe por dominio', () => {
  it('app.routes.ts carga los cinco dominios y cada uno tiene su archivo de rutas', () => {
    const enchufe = readFileSync(join(APP, 'app.routes.ts'), 'utf8')
    for (const d of DOMINIOS) {
      expect(enchufe).toContain(`./rutas/${d}/${d}.routes`)
      expect(statSync(join(APP, 'rutas', d, `${d}.routes.ts`)).isFile()).toBe(true)
    }
  })

  it('agregar una ruta vacía en un dominio no cambia ningún archivo fuera de él', () => {
    const huella = () => new Map(archivosDe(APP).filter((a) => !a.includes('/rutas/cumplimiento/')).map((a) => [a, readFileSync(a, 'utf8')]))
    const antes = huella()
    const rutas = join(APP, 'rutas/cumplimiento/cumplimiento.routes.ts')
    const rutasAntes = readFileSync(rutas, 'utf8')
    const nueva = join(APP, 'rutas/cumplimiento/andamiaje.ts')
    writeFileSync(nueva, 'export const andamiaje = true\n', 'utf8')
    writeFileSync(rutas, rutasAntes.replace('= []', "= [{ path: 'andamiaje', loadComponent: () => import('./andamiaje').then((m) => m.andamiaje as never) }]"), 'utf8')
    try {
      // La huella se toma UNA vez: recalcularla dentro del bucle leía el árbol entero
      // por cada archivo, y con el árbol de hoy eso se pasa del plazo de la prueba.
      const despues = huella()
      for (const [archivo, contenido] of antes) expect(despues.get(archivo)).toBe(contenido)
    } finally {
      rmSync(nueva, { force: true })
      writeFileSync(rutas, rutasAntes, 'utf8')
    }
  })
})
