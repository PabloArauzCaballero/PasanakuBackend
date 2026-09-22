import { readFileSync, readdirSync, rmSync, statSync, writeFileSync } from 'node:fs'
import { join } from 'node:path'
import { describe, expect, it } from 'vitest'

/** Agregar una página no cambia nada fuera de `paginas/` salvo su entrada en `app.routes.ts`. */
const SRC = join(__dirname, '..')
const PAGINAS = join(__dirname, 'paginas')
const archivosDe = (c: string): string[] =>
  readdirSync(c).flatMap((e) => (statSync(join(c, e)).isDirectory() ? archivosDe(join(c, e)) : [join(c, e)]))

describe('una carpeta por página', () => {
  it('cada carpeta de paginas/ tiene su componente y nada más la conoce salvo app.routes.ts', () => {
    const rutas = readFileSync(join(__dirname, 'app.routes.ts'), 'utf8')
    for (const carpeta of readdirSync(PAGINAS)) {
      expect(rutas).toContain(`./paginas/${carpeta}/`)
    }
  })

  it('agregar una página vacía no cambia ningún archivo fuera de paginas/ y app.routes.ts', () => {
    const huella = () => new Map(archivosDe(SRC).filter((a) => !a.includes('/paginas/') && !a.endsWith('app.routes.ts')).map((a) => [a, readFileSync(a, 'utf8')]))
    const antes = huella()
    const nueva = join(PAGINAS, 'andamiaje')
    try {
      writeFileSync(join(PAGINAS, 'andamiaje.tmp.ts'), 'export const andamiaje = true\n', 'utf8')
      for (const [archivo, contenido] of antes) expect(huella().get(archivo)).toBe(contenido)
    } finally {
      rmSync(join(PAGINAS, 'andamiaje.tmp.ts'), { force: true })
      rmSync(nueva, { force: true, recursive: true })
    }
  })
})
