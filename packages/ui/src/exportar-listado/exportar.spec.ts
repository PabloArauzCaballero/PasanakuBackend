import { describe, expect, it } from 'vitest'
import { aCsv, aJson, celdaCsv, nombreDeArchivo, type ColumnaExportable } from './exportar'

type Fila = { id: string; nombre: string; monto: number | null; extra?: { a: number } }
const columnas: ColumnaExportable<Fila>[] = [
  { clave: 'id', titulo: 'Código' },
  { clave: 'nombre', titulo: 'Nombre' },
  { clave: 'monto', titulo: 'Monto (Bs)', valor: (f) => (f.monto === null ? '—' : f.monto.toFixed(2)) }, // permitido: prueba
]

describe('exportar listado', () => {
  it('aceptado: CSV con BOM, encabezado con los títulos y CRLF', () => {
    const csv = aCsv(columnas, [{ id: 'R-1', nombre: 'Ana', monto: 10 }])
    expect(csv.startsWith('﻿')).toBe(true)
    expect(csv.slice(1)).toBe('Código,Nombre,Monto (Bs)\r\nR-1,Ana,10.00\r\n')
  })

  it('aceptado: JSON usa las claves y los valores tal como se muestran', () => {
    expect(JSON.parse(aJson(columnas, [{ id: 'R-1', nombre: 'Ana', monto: null }]))).toEqual([{ id: 'R-1', nombre: 'Ana', monto: '—' }])
  })

  it('límite: sin filas el CSV trae solo el encabezado y el JSON un arreglo vacío', () => {
    expect(aCsv(columnas, []).slice(1)).toBe('Código,Nombre,Monto (Bs)\r\n')
    expect(aJson(columnas, [])).toBe('[]')
  })

  it('límite: comillas, comas, punto y coma, saltos y espacios de borde van entre comillas', () => {
    expect(celdaCsv('Pérez, "Juana"')).toBe('"Pérez, ""Juana"""')
    expect(celdaCsv('a;b')).toBe('"a;b"')
    expect(celdaCsv('línea 1\nlínea 2')).toBe('"línea 1\nlínea 2"')
    expect(celdaCsv(' borde ')).toBe('" borde "')
    expect(celdaCsv(null)).toBe('')
    expect(celdaCsv(0)).toBe('0')
  })

  it('inválido: una celda que empieza como fórmula se neutraliza; un número negativo no', () => {
    expect(celdaCsv('=HYPERLINK("x")')).toBe(`"'=HYPERLINK(""x"")"`)
    expect(celdaCsv('+1+1')).toBe("'+1+1")
    expect(celdaCsv('@SUMA')).toBe("'@SUMA")
    expect(celdaCsv('-12.50')).toBe('-12.50')
    expect(celdaCsv('-cmd')).toBe("'-cmd")
    expect(celdaCsv('-1,250.00')).toBe('"-1,250.00"')
    expect(celdaCsv('-359.90 Bs')).toBe('-359.90 Bs')
    expect(celdaCsv('-1+2')).toBe("'-1+2")
    expect(celdaCsv('-Bs 1.240,50')).toBe('"-Bs 1.240,50"')
    expect(celdaCsv('-US$ 10,00')).toBe('"-US$ 10,00"')
    expect(celdaCsv('-2 cmd')).toBe("'-2 cmd")
    expect(celdaCsv('-1 SUM')).toBe("'-1 SUM")
  })

  it('inválido: un objeto anidado sin formateador sale como JSON, no como [object Object]', () => {
    const csv = aCsv<Fila>([{ clave: 'extra', titulo: 'Extra' }], [{ id: '1', nombre: 'x', monto: 1, extra: { a: 1 } }])
    expect(csv).toContain('"{""a"":1}"')
  })

  it('el nombre del archivo es legible, sin acentos ni espacios, y fechado', () => {
    expect(nombreDeArchivo('Retiros en revisión', 'csv', new Date(2026, 9, 6))).toBe('retiros-en-revision-2026-10-06.csv')
    expect(nombreDeArchivo('¡¡!!', 'json', new Date(2026, 0, 1))).toBe('listado-2026-01-01.json')
  })
})
