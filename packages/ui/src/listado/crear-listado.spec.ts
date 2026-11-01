import { signal } from '@angular/core'
import { describe, expect, it } from 'vitest'
import { comparar, crearListado, normalizar } from './crear-listado'

type Fila = { id: string; nombre: string; estado: string; monto: string }
const filas = (n: number): Fila[] =>
  Array.from({ length: n }, (_, i) => ({ id: `F-${i + 1}`, nombre: i === 0 ? 'José Pérez' : `Persona ${i + 1}`, estado: i % 2 ? 'ACTIVO' : 'BAJA', monto: String((i + 1) * 10) + '.50' }))

function montar(n = 25, porPagina = 10) {
  const datos = signal<readonly Fila[]>(filas(n))
  const l = crearListado<Fila>({
    filas: datos,
    camposDeBusqueda: ['id', 'nombre', (f) => f.estado],
    filtros: [{ nombre: 'estado', etiqueta: 'Estado', valor: (f) => f.estado }],
    porPagina,
  })
  return { datos, l }
}

describe('crearListado', () => {
  it('aceptado: 25 filas con 10 por página son 3 páginas y la tercera trae 5', () => {
    const { l } = montar()
    expect(l.totalDePaginas()).toBe(3)
    l.irAPagina(3)
    expect(l.visibles().map((f) => f.id)).toEqual(['F-21', 'F-22', 'F-23', 'F-24', 'F-25'])
  })

  it('aceptado: la búsqueda no distingue acentos ni mayúsculas y cada palabra tiene que aparecer', () => {
    const { l } = montar()
    l.buscar('jose PEREZ')
    expect(l.filtradas().map((f) => f.id)).toEqual(['F-1'])
    l.buscar('jose activo')
    expect(l.total()).toBe(0)
  })

  it('aceptado: busca en todos los campos declarados, incluidos los calculados', () => {
    const { l } = montar(4)
    l.buscar('baja')
    expect(l.filtradas().map((f) => f.id)).toEqual(['F-1', 'F-3'])
  })

  it('aceptado: los filtros se combinan con la búsqueda y las opciones salen de los datos', () => {
    const { l } = montar()
    expect(l.selectores()[0]!.opciones.map((o) => o.valor)).toEqual(['ACTIVO', 'BAJA'])
    l.filtrar('estado', 'ACTIVO')
    l.buscar('persona 2')
    expect(l.filtradas().every((f) => f.estado === 'ACTIVO')).toBe(true)
    expect(l.filtradas().map((f) => f.id)).toEqual(['F-2', 'F-12', 'F-20', 'F-22', 'F-24'])
  })

  it('aceptado: ordena importes como números y es estable', () => {
    const { l } = montar(12)
    l.orden.set({ clave: 'monto', sentido: 'desc' })
    expect(l.filtradas()[0]!.monto).toBe('120.50')
    expect(l.filtradas().at(-1)!.monto).toBe('10.50')
  })

  it('límite: cambiar búsqueda, filtro o tamaño vuelve a la página 1', () => {
    const { l } = montar()
    l.irAPagina(3)
    l.buscar('persona')
    expect(l.pagina()).toBe(1)
    l.irAPagina(2)
    l.filtrar('estado', 'BAJA')
    expect(l.pagina()).toBe(1)
    l.irAPagina(2)
    l.cambiarPorPagina(50)
    expect(l.pagina()).toBe(1)
    expect(l.totalDePaginas()).toBe(1)
  })

  it('límite: si los datos se achican la página se acota a la última que existe', () => {
    const { datos, l } = montar()
    l.irAPagina(3)
    datos.set(filas(12))
    expect(l.pagina()).toBe(2)
    expect(l.visibles().length).toBe(2)
  })

  it('límite: sin filas hay una página vacía, no cero páginas', () => {
    const { l } = montar(0)
    expect(l.totalDePaginas()).toBe(1)
    expect(l.visibles()).toEqual([])
    expect(l.hayCriterios()).toBe(false)
  })

  it('límite: exactamente una página llena no ofrece una segunda', () => {
    const { l } = montar(10)
    expect(l.totalDePaginas()).toBe(1)
  })

  it('inválido: ir a una página fuera de rango o con basura se acota; un tamaño inválido no cambia nada', () => {
    const { l } = montar()
    l.irAPagina(99)
    expect(l.pagina()).toBe(3)
    l.irAPagina(Number.NaN)
    expect(l.pagina()).toBe(1)
    l.irAPagina(-4)
    expect(l.pagina()).toBe(1)
    l.cambiarPorPagina(0)
    l.cambiarPorPagina(Number.NaN)
    expect(l.porPagina()).toBe(10)
  })

  it('inválido: un filtro por un valor que no existe deja la lista vacía y «limpiar» la recupera', () => {
    const { l } = montar()
    l.filtrar('estado', 'NO-EXISTE')
    expect(l.total()).toBe(0)
    expect(l.hayCriterios()).toBe(true)
    l.limpiar()
    expect(l.total()).toBe(25)
    expect(l.hayCriterios()).toBe(false)
  })
})

describe('normalizar y comparar', () => {
  it('normaliza acentos, eñes y espacios', () => {
    expect(normalizar('  ÁÉÍÓÚ Ñandú ')).toBe('aeiou nandu')
    expect(normalizar(null)).toBe('')
  })
  it('los vacíos van al final y los textos se comparan en español', () => {
    expect([null, 'b', 'á', ''].sort(comparar)).toEqual(['á', 'b', null, ''])
    expect(comparar('9', '10')).toBeLessThan(0)
  })
})
