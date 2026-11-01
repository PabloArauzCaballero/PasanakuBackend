import { describe, expect, it } from 'vitest'
import { MENU, type Credenciales } from './menu'
import { cumple, destinoDe, entradaDe, entradasVisibles, hojas, migasDe, pantallasVisibles } from './navegacion'
import { alcanza } from './secciones'

/** Una sesión de prueba con los permisos reales que le pasan y un rol. */
const sesion = (permisos: string[], rol: string | null = 'oficial'): Credenciales => ({ puede: (p) => alcanza(p, permisos, true), rol: () => rol })
const entrada = (ruta: string) => MENU.flatMap((g) => g.entradas).find((e) => e.ruta === ruta)!
const textos = (c: Credenciales) => entradasVisibles(c).flatMap((g) => g.entradas.map((e) => e.texto))

/** El árbol del menú (A1), sus requisitos (F1) y las migas que salen de la URL. */
describe('menú del backoffice', () => {
  it('correcto: seis entradas de primer nivel (sin «Inicio»: se vuelve con el logo, 2026-10-28), ninguna sección con más de ocho pestañas, todas con ícono y ruta absoluta', () => {
    const entradas = MENU.flatMap((g) => g.entradas)
    expect(entradas.map((e) => e.texto)).toEqual(['Operación', 'Cumplimiento', 'Contabilidad', 'Publicidad', 'Sistemas', 'Configuración'])
    for (const e of entradas) {
      expect(e.pantallas?.length ?? 0, e.texto).toBeLessThanOrEqual(8)
      for (const h of (e.pantallas ?? []).flatMap(hojas)) {
        expect(h.ruta.startsWith('/'), h.ruta).toBe(true)
        expect(h.icono, h.ruta).toBeTruthy()
        expect(h.requiere?.length, h.ruta).toBeGreaterThan(0)
      }
    }
  })

  it('correcto: ninguna pantalla aparece dos veces (sin duplicados entre secciones)', () => {
    const rutas = MENU.flatMap((g) => g.entradas).flatMap((e) => (e.pantallas ?? []).flatMap(hojas).map((h) => h.ruta))
    expect(new Set(rutas).size).toBe(rutas.length)
    const nombres = MENU.flatMap((g) => g.entradas).flatMap((e) => (e.pantallas ?? []).flatMap(hojas).map((h) => h.texto))
    expect(new Set(nombres).size).toBe(nombres.length)
  })

  it('correcto (F1): con todos los permisos y rol PLATAFORMA, Sistemas aparece y lleva a /sistemas/servicios', () => {
    const c = sesion(['ver:sistemas', 'ver:operacion', 'RETIRO_APROBAR'], 'PLATAFORMA')
    expect(textos(c)).toContain('Sistemas')
    expect(destinoDe(entrada('sistemas'), c)).toBe('/sistemas/servicios')
  })

  it('límite (F1): con el permiso de sistemas pero rol oficial, Sistemas no aparece (la guardia de rol lo rebotaría)', () => {
    const c = sesion(['ACCESOS_ADMINISTRAR', 'CATALOGO_EDITAR'], 'oficial')
    expect(textos(c)).not.toContain('Sistemas')
    // Catálogos operativos vive en Configuración pero exige lo mismo que Sistemas: tampoco aparece.
    expect(pantallasVisibles(entrada('configuracion'), c).map((p) => p.texto)).not.toContain('Catálogos operativos')
  })

  it('límite: la entrada lleva a la primera pestaña que la sesión puede abrir, no a una fija', () => {
    expect(destinoDe(entrada('publicidad'), sesion(['PUBLICIDAD_MODERAR']))).toBe('/publicidad/moderacion')
    expect(destinoDe(entrada('operacion'), sesion(['RECLAMO_VER']))).toBe('/operacion/reclamos')
    expect(destinoDe(entrada('operacion'), sesion(['ADMIN_PLATAFORMA']))).toBe('/admisiones')
    // Un grupo con una sola pantalla posible queda con esa sola.
    const riesgo = pantallasVisibles(entrada('cumplimiento'), sesion(['RESPONSABLE_SEGURIDAD'], 'RESPONSABLE_SEGURIDAD')).find((p) => p.grupo)
    expect(riesgo?.grupo?.map((h) => h.texto)).toEqual(['Incidentes de seguridad'])
  })

  it('inválido: una sección cuyas pestañas no puede abrir ninguna no se dibuja, aunque tenga el permiso de sección', () => {
    // `ver:operacion` por TESORERIA, sin AUDITORIA_LEER ni los permisos finos: ninguna pestaña de Operación abre.
    expect(textos(sesion(['TESORERIA']))).toEqual([])
    // Con AUDITORIA_LEER abre custodia y cierres, que son de Operación: no aparece una «Contabilidad» que no tiene.
    expect(textos(sesion(['TESORERIA', 'AUDITORIA_LEER']))).toEqual(['Operación'])
    expect(cumple([{ permisos: [], roles: [] }], sesion(['TESORERIA']))).toBe(false)
    expect(cumple([{ roles: ['PLATAFORMA'] }], sesion([], ''))).toBe(false)
  })

  it('correcto: la entrada de una URL es la de prefijo o pestaña más larga que la contiene', () => {
    expect(entradaDe('/tablero/catalogo-de-datos/comun/pago')?.ruta).toBe('configuracion')
    // Sin entrada «Inicio» (2026-10-28) el tablero no es de ninguna sección: igual que antes, sin migas ni pestañas.
    expect(entradaDe('/tablero/financiero')).toBeUndefined()
    expect(entradaDe('/sistemas/outbox?pagina=2')?.ruta).toBe('sistemas')
    expect(entradaDe('/operacion/custodia')?.ruta).toBe('operacion')
    expect(entradaDe('/operacion/estado')?.ruta).toBe('operacion')
    expect(entradaDe('/admisiones/9a')?.ruta).toBe('operacion')
    expect(entradaDe('/otra')).toBeUndefined()
  })

  it('migas: Inicio › Sección › [Grupo ›] Pantalla; una ficha agrega el título; el inicio no tiene migas', () => {
    expect(migasDe('/tablero', 'Tablero · AportaYa')).toEqual([])
    expect(migasDe('/sistemas/outbox', 'Outbox por servicio · Sistemas · AportaYa')).toEqual([
      { texto: 'Inicio', ruta: '/tablero' },
      { texto: 'Sistemas', ruta: '/sistemas/servicios' },
      { texto: 'Integraciones', ruta: '/sistemas/proveedores' },
      { texto: 'Outbox por servicio' },
    ])
    expect(migasDe('/cumplimiento/casos/8f1', 'Caso de cumplimiento · AportaYa')).toEqual([
      { texto: 'Inicio', ruta: '/tablero' },
      { texto: 'Cumplimiento', ruta: '/cumplimiento/verificaciones' },
      { texto: 'Casos de investigación', ruta: '/cumplimiento/casos' },
      { texto: 'Caso de cumplimiento' },
    ])
  })

  it('migas: con credenciales, el enlace de la sección va a lo que la sesión puede abrir; una URL fuera de las pestañas usa el título', () => {
    expect(migasDe('/operacion/reclamos', 'Reclamos · AportaYa', sesion(['RECLAMO_VER']))[1]).toEqual({ texto: 'Operación', ruta: '/operacion/reclamos' })
    expect(migasDe('/operacion/estado', 'Arquitectura y estado · AportaYa')).toEqual([
      { texto: 'Inicio', ruta: '/tablero' },
      { texto: 'Operación', ruta: '/operacion/retiros' },
      { texto: 'Arquitectura y estado' },
    ])
    expect(migasDe('/tablero/catalogo-de-datos/comun/pago', 'Ficha de tabla · AportaYa').slice(1)).toEqual([
      { texto: 'Configuración', ruta: '/configuracion/tarifario' },
      { texto: 'Catálogo de datos', ruta: '/tablero/catalogo-de-datos' },
      { texto: 'Ficha de tabla' },
    ])
  })
})
