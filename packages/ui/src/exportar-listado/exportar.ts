/**
 * Exportación en el navegador de **lo que el listado muestra ya filtrado**: mismas filas, mismas columnas y los
 * valores tal como se ven (lo enmascarado sale enmascarado). No reemplaza a CU-58 (`nucleo/exportador.ts` del
 * backoffice), que arma en el servidor la exportación con huella y tope de descargas.
 */
export type ColumnaExportable<T> = {
  /** La clave del campo en el JSON. */
  clave: string
  /** El encabezado en el CSV. */
  titulo: string
  /** El valor tal como se muestra en la tabla. Sin él, se toma `fila[clave]`. */
  valor?: (fila: T) => string | number | boolean | null | undefined
}

type Celda = string | number | boolean | null | undefined

function valorDe<T>(fila: T, c: ColumnaExportable<T>): Celda {
  if (c.valor) return c.valor(fila)
  const v = (fila as Record<string, unknown>)[c.clave]
  if (v === null || v === undefined) return v
  return typeof v === 'object' ? JSON.stringify(v) : (v as Celda)
}

/**
 * Un número o un importe con su moneda delante o detrás: «-359.90», «-1.250,00», «-Bs 1.240,50», «-359.90 Bs»,
 * «BOB -359.90». Solo monedas conocidas: «-2 cmd» o «-1 SUM» no pasan y se neutralizan.
 */
const MONEDA = '(?:Bs|BOB|USD|US\\$|\\$)'
const NUMERO = new RegExp(`^-?\\s?(?:${MONEDA}\\s?)?-?\\d[\\d.,]*(?:\\s?${MONEDA})?$`)

/**
 * Una celda de CSV según RFC 4180: entre comillas si trae separador, comillas o saltos, con las comillas
 * duplicadas. Además, **contra la inyección de fórmulas** (OWASP CSV injection): un texto que empieza con
 * `=`, `+`, `-`, `@`, tabulación o retorno —y no es un número— se antepone con `'` para que la planilla no lo
 * ejecute.
 */
export function celdaCsv(v: Celda): string {
  if (v === null || v === undefined) return ''
  let t = String(v)
  if (typeof v === 'string' && /^[=+\-@\t\r]/.test(t) && !NUMERO.test(t)) t = `'${t}`
  return /[",\r\n;]/.test(t) || t !== t.trim() ? `"${t.replace(/"/g, '""')}"` : t
}

/** CSV con BOM (para que Excel lea los acentos) y fin de línea CRLF. Sin filas: solo el encabezado. */
export function aCsv<T>(columnas: readonly ColumnaExportable<T>[], filas: readonly T[]): string {
  const lineas = [columnas.map((c) => celdaCsv(c.titulo)).join(','), ...filas.map((f) => columnas.map((c) => celdaCsv(valorDe(f, c))).join(','))]
  return '﻿' + lineas.join('\r\n') + '\r\n'
}

/** JSON con una entrada por fila, solo con las columnas exportables, indentado para leerse. */
export function aJson<T>(columnas: readonly ColumnaExportable<T>[], filas: readonly T[]): string {
  return JSON.stringify(
    filas.map((f) => Object.fromEntries(columnas.map((c) => [c.clave, valorDe(f, c) ?? null]))),
    null,
    2,
  )
}

/** `retiros-2026-10-26.csv`: el nombre dice qué y de cuándo, sin espacios ni acentos. */
export function nombreDeArchivo(base: string, extension: 'csv' | 'json', hoy = new Date()): string {
  const limpio = base.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '') || 'listado'
  const fecha = `${hoy.getFullYear()}-${String(hoy.getMonth() + 1).padStart(2, '0')}-${String(hoy.getDate()).padStart(2, '0')}`
  return `${limpio}-${fecha}.${extension}`
}

/** Baja el contenido como archivo. Devuelve `false` si el entorno no puede (sin `document` o sin `URL.createObjectURL`). */
export function descargar(contenido: string, nombre: string, tipo: string): boolean {
  if (typeof document === 'undefined' || typeof URL === 'undefined' || typeof URL.createObjectURL !== 'function') return false
  const url = URL.createObjectURL(new Blob([contenido], { type: tipo }))
  const a = document.createElement('a')
  a.href = url
  a.download = nombre
  a.rel = 'noopener'
  document.body.appendChild(a)
  a.click()
  a.remove()
  setTimeout(() => URL.revokeObjectURL(url), 0)
  return true
}
