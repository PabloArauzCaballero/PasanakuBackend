import { describe, expect, it } from 'vitest'
import { rutasPublicidad } from './publicidad.routes'

/**
 * Gate del carril: segregación de funciones en campañas. `campanas` (gestión) y
 * `campanas/:campanaId/aprobacion` (aprobación) son rutas **distintas**, cada una con
 * su propio `canMatch` — el rol que puede gestionar no monta, por eso solo, el panel de
 * aprobación, y viceversa.
 */
describe('rutasPublicidad · segregación de funciones en campañas', () => {
  it('la ruta de gestión y la de aprobación existen por separado, cada una con su propio canMatch', () => {
    const gestion = rutasPublicidad.find((r) => r.path === 'campanas')
    const aprobacion = rutasPublicidad.find((r) => r.path === 'campanas/:campanaId/aprobacion')
    expect(gestion?.canMatch).toBeDefined()
    expect(aprobacion?.canMatch).toBeDefined()
    expect(gestion?.canMatch).not.toBe(aprobacion?.canMatch)
  })

  it('las cinco pantallas del alcance del carril están cargadas', () => {
    const rutas = rutasPublicidad.map((r) => r.path)
    expect(rutas).toEqual(expect.arrayContaining(['anunciantes', 'campanas', 'campanas/:campanaId/aprobacion', 'moderacion', 'desempeno', 'liquidacion']))
  })

  it('cada ruta con canMatch lo trae (ninguna pantalla queda sin guarda de permiso)', () => {
    for (const r of rutasPublicidad) {
      if (r.path === '') continue
      expect(r.canMatch, `la ruta "${r.path}" no tiene canMatch`).toBeDefined()
      expect(r.canMatch!.length).toBeGreaterThan(0)
    }
  })
})
