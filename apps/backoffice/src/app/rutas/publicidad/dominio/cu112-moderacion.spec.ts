import { describe, expect, it } from 'vitest'
import { readFileSync, readdirSync, statSync } from 'node:fs'
import { join } from 'node:path'
import { puedeModerar, puedeUsarseEnUnAnuncio, type PiezaCreativa } from './cu112-moderacion'

const PENDIENTE: Pick<PiezaCreativa, 'estadoModeracion'> = { estadoModeracion: 'PENDIENTE' }
const APROBADA: Pick<PiezaCreativa, 'estadoModeracion'> = { estadoModeracion: 'APROBADA' }
const RECHAZADA: Pick<PiezaCreativa, 'estadoModeracion'> = { estadoModeracion: 'RECHAZADA' }

const RUTAS = join(__dirname, '..')

function archivosDe(carpeta: string): string[] {
  return readdirSync(carpeta).flatMap((e) => {
    const ruta = join(carpeta, e)
    return statSync(ruta).isDirectory() ? archivosDe(ruta) : [ruta]
  })
}

describe('CU-112 · gate: la moderación es previa a la entrega', () => {
  it('R-PUB-04: solo una pieza APROBADA puede usarse en un anuncio', () => {
    expect(puedeUsarseEnUnAnuncio(PENDIENTE)).toBe(false)
    expect(puedeUsarseEnUnAnuncio(RECHAZADA)).toBe(false)
    expect(puedeUsarseEnUnAnuncio(APROBADA)).toBe(true)
  })

  it('R-PUB-05: quien subió la pieza no puede moderarla', () => {
    expect(puedeModerar({ subidaPor: 'op-1' }, 'op-1')).toBe(false)
    expect(puedeModerar({ subidaPor: 'op-1' }, 'op-2')).toBe(true)
  })

  it('no existe, en todo el carril, ninguna ruta ni componente que programe un anuncio saltando la moderación', () => {
    const archivos = archivosDe(RUTAS)
    const contenido = archivos.map((a) => readFileSync(a, 'utf8')).join('\n')
    // El único lugar que puede mover una pieza fuera de PENDIENTE es `moderarPieza` (este dominio).
    const llamadasAPost = contenido.match(/\/publicidad\/(anuncios|piezas-creativas\/[^'"`]*\/revision)/g) ?? []
    for (const llamada of llamadasAPost) {
      expect(llamada.includes('/revision')).toBe(true)
    }
    // No hay ninguna ruta cargada en publicidad.routes.ts hacia una pantalla de "programar anuncio":
    // el alcance del carril (ficha F14) es anunciantes, campañas, moderación, desempeño y liquidación.
    const rutas = readFileSync(join(RUTAS, 'publicidad.routes.ts'), 'utf8')
    expect(rutas).not.toMatch(/anuncios/)
  })
})
