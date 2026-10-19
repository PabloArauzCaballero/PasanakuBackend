import { describe, expect, it } from 'vitest'
import { catalogo, OBLIGACION } from './catalogo-sintetico'
import { filasDeTablas, filtrarTablas, normalizar, pagina } from './dominio'
import { filasDeColumnas } from './revision'

describe('filtrarTablas · búsqueda normalizada por tabla, columna, esquema y concepto', () => {
  it('correcto: sin acentos ni mayúsculas, «Período» encuentra la ficha que dice «período»', () => {
    expect(normalizar('  PeRÍOdo ')).toBe('periodo')
    expect(filtrarTablas(catalogo(), 'Período', []).map((t) => t.tabla)).toEqual(['obligacion_aporte'])
  })

  it('correcto: por columna, y con espacios en lugar de guiones bajos', () => {
    expect(filtrarTablas(catalogo(), 'monto esperado', []).map((t) => t.tabla)).toEqual(['obligacion_aporte'])
  })

  it('límite: sin búsqueda ni esquema vuelven todas, ordenadas por esquema y tabla', () => {
    expect(filtrarTablas(catalogo(), '', []).map((t) => `${t.esquema}.${t.tabla}`)).toEqual([
      'aportes.obligacion_aporte',
      'grupos.grupo',
      'identidad.credencial_acceso',
    ])
  })

  it('límite: el filtro de esquema se combina con la búsqueda', () => {
    expect(filtrarTablas(catalogo(), 'id', ['grupos']).map((t) => t.tabla)).toEqual(['grupo'])
  })

  it('inválido: una palabra que no está en ninguna tabla no devuelve nada; sin catálogo, nada', () => {
    expect(filtrarTablas(catalogo(), 'zzz-inexistente', [])).toEqual([])
    expect(filtrarTablas(undefined, '', [])).toEqual([])
  })
})

describe('filas · la tabla recibe texto', () => {
  it('correcto: una tabla de un esquema compartido dice «Compartido» como servicio', () => {
    const filas = filasDeTablas(filtrarTablas(catalogo(), 'credencial', []))
    expect(filas).toEqual([expect.objectContaining({ id: 'identidad.credencial_acceso', servicio: 'Compartido', columnas: '1' })])
  })

  it('correcto: la columna de convención lo aclara, la que no tiene nota lo dice y la sensibilidad va en palabras', () => {
    const [id, monto, grupo] = filasDeColumnas(OBLIGACION)
    expect(id!.negocio).toBe('Identificador estable (convención)')
    expect(monto).toMatchObject({ negocio: 'Lo que se pactó', sensibilidad: 'Dato financiero' })
    expect(grupo!.negocio).toBe('Sin nota de negocio')
  })
})

describe('pagina · veinte por página', () => {
  const veinticinco = Array.from({ length: 25 }, (_, i) => i)
  it('límite: la segunda página trae las cinco que sobran', () => {
    expect(pagina(veinticinco, 2)).toEqual([20, 21, 22, 23, 24])
  })
  it('inválido: una página fuera de rango (0, 99) cae en la primera o la última que existe', () => {
    expect(pagina(veinticinco, 0)).toHaveLength(20)
    expect(pagina(veinticinco, 99)).toEqual([20, 21, 22, 23, 24])
  })
})
