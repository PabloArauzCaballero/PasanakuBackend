import { describe, expect, it } from 'vitest'
import { MENU, entradaDe, migasDe } from './menu'

/** El árbol del menú y las migas que salen de la URL. */
describe('menú del backoffice', () => {
  it('correcto: cada entrada tiene destino absoluto y permiso ver:*; los grupos salen en el orden de Atlas', () => {
    expect(MENU.map((g) => g.titulo)).toEqual([null, 'Operación', 'Control', 'Datos', 'Finanzas', 'Plataforma'])
    for (const e of MENU.flatMap((g) => g.entradas)) {
      expect(e.destino.startsWith('/'), e.destino).toBe(true)
      expect(e.permiso.startsWith('ver:'), e.permiso).toBe(true)
    }
  })

  it('correcto: la entrada de una URL es la de destino más largo que la prefija', () => {
    expect(entradaDe('/tablero/catalogo-de-datos/pago')?.ruta).toBe('catalogo')
    expect(entradaDe('/tablero/financiero')?.ruta).toBe('tablero')
    expect(entradaDe('/sistemas/outbox?pagina=2')?.ruta).toBe('sistemas')
    expect(entradaDe('/otra')).toBeUndefined()
  })

  it('migas: Inicio › Sección › Pantalla; una ficha agrega el título de la página; el inicio no tiene migas', () => {
    expect(migasDe('/tablero', 'Tablero · AportaYa')).toEqual([])
    expect(migasDe('/sistemas/outbox', 'Outbox y descartados · Sistemas · AportaYa')).toEqual([
      { texto: 'Inicio', ruta: '/tablero' },
      { texto: 'Sistemas', ruta: '/sistemas' },
      { texto: 'Outbox y descartados' },
    ])
    expect(migasDe('/cumplimiento/casos/8f1', 'Caso de cumplimiento · AportaYa')).toEqual([
      { texto: 'Inicio', ruta: '/tablero' },
      { texto: 'Cumplimiento', ruta: '/cumplimiento' },
      { texto: 'Casos de investigación', ruta: '/cumplimiento/casos' },
      { texto: 'Caso de cumplimiento' },
    ])
  })

  it('migas: una sección sin subrutas o su raíz muestra Inicio › título', () => {
    expect(migasDe('/admisiones', 'Solicitudes de ingreso · AportaYa')).toEqual([{ texto: 'Inicio', ruta: '/tablero' }, { texto: 'Solicitudes de ingreso' }])
    expect(migasDe('/tablero/catalogo-de-datos/pago', 'Ficha de tabla · AportaYa')).toEqual([
      { texto: 'Inicio', ruta: '/tablero' },
      { texto: 'Catálogo de datos', ruta: '/tablero/catalogo-de-datos' },
      { texto: 'Ficha de tabla' },
    ])
  })
})
