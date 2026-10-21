import { httpResource, type HttpResourceRef } from '@angular/common/http'
import { Injectable, inject } from '@angular/core'
import {
  ColumnaDelCatalogoSensibilidadEnum as S,
  type ColumnaDelCatalogo,
  type EsquemaDelCatalogo,
  type FilaDelCatalogo,
  type PaginaDeTablasDelCatalogo,
  type RelacionDelCatalogo,
  type ResumenDelCatalogo,
  type TablaDelCatalogo,
} from 'clientes/angular/auditoria'
import type { Tono } from '@aportaya/ui/tono/tono'
import { GATEWAY } from '../../../nucleo/gateway'
import { ESTADOS, REVISIONES, POR_OMISION, tonoDeEstado, tonoDeRevision } from './gobierno'

/**
 * Catálogo de datos · los contratos `obtenerResumenDelCatalogo`, `listarTablasDelCatalogo` y `obtenerTablaDelCatalogo`
 * de auditoria.yaml, con los tipos del cliente generado. Como en el portal de Atlas, el catálogo viaja por partes: el
 * resumen (unos KB), la página pedida (filtrada en el servidor) y la ficha de una tabla. Antes se bajaba entero
 * (~2 MB) para mostrar veinte filas.
 */
export type { ColumnaDelCatalogo, EsquemaDelCatalogo, FilaDelCatalogo, PaginaDeTablasDelCatalogo, RelacionDelCatalogo, ResumenDelCatalogo, TablaDelCatalogo }

const BASE = '/indicadores/catalogo-de-datos'

/** El resumen se pide una vez por sesión de la pantalla: cifras, esquemas, módulos y políticas de retención. */
@Injectable({ providedIn: 'root' })
export class ResumenDelCatalogoRemoto {
  private readonly gateway = inject(GATEWAY)
  readonly recurso = httpResource<ResumenDelCatalogo>(() => `${this.gateway}${BASE}/resumen`)
}

/** Los filtros de la lista, tal como viven en la URL. Vacío es «todos». */
export type FiltrosDeLista = { q: string; esquema: string; tipo: string; estado: string; revision: string; sensibilidad: string }

export const SIN_FILTROS: FiltrosDeLista = { q: '', esquema: '', tipo: '', estado: '', revision: '', sensibilidad: '' }

/** La URL de una página: solo los filtros con valor, en orden fijo (la misma vista pide siempre la misma URL). */
export function urlDeTablas(gateway: string, f: FiltrosDeLista, pagina: number, limite: number): string {
  const params = new URLSearchParams()
  const q = f.q.trim()
  if (q) params.set('q', q.slice(0, 80))
  for (const clave of ['esquema', 'tipo', 'estado', 'revision', 'sensibilidad'] as const) {
    if (f[clave]) params.set(clave, f[clave])
  }
  params.set('pagina', String(Math.max(1, pagina)))
  params.set('limite', String(limite))
  return `${gateway}${BASE}/tablas?${params.toString()}`
}

export const urlDeFicha = (gateway: string, esquema: string, tabla: string): string =>
  `${gateway}${BASE}/tablas/${encodeURIComponent(esquema)}/${encodeURIComponent(tabla)}`

/** La ficha de `esquema.tabla`. Se llama en el inicializador del componente (contexto de inyección). */
export function fichaRemota(esquema: () => string, tabla: () => string): HttpResourceRef<TablaDelCatalogo | undefined> {
  const gateway = inject(GATEWAY)
  return httpResource<TablaDelCatalogo>(() => urlDeFicha(gateway, esquema(), tabla()))
}

/** Sin acentos ni mayúsculas: «Obligación» encuentra «obligacion_aporte». */
export function normalizar(valor: string): string {
  return valor
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .toLocaleLowerCase('es')
    .trim()
}

/** Una fila de la lista, ya en texto (la tabla no formatea). */
export type FilaDeTabla = {
  id: string
  tabla: string
  esquema: string
  queEs: string
  servicio: string
  columnas: string
  responsable: string
  sensible: string
  tonoSensible: Tono
  estado: string
  tonoEstado: Tono
  revision: string
  tonoRevision: Tono
  infraestructura: boolean
}

export function filasDeTablas(filas: readonly FilaDelCatalogo[]): FilaDeTabla[] {
  return filas.map((f) => {
    const g = f.gobierno ?? POR_OMISION
    const c = f.clasificacion
    return {
      id: `${f.esquema}.${f.tabla}`,
      tabla: f.tabla,
      esquema: f.esquema,
      queEs: f.queEs ?? 'Sin ficha',
      servicio: f.servicio ?? 'Compartido',
      columnas: String(f.columnas),
      responsable: g.responsable ?? 'Sin asignar',
      sensible: c.personales || c.secretos ? 'Datos personales' : c.financieros ? 'Financiera' : 'Ninguna',
      tonoSensible: c.personales || c.secretos ? 'pii' : c.financieros ? 'info' : 'apagado',
      estado: ESTADOS[g.estado] ?? g.estado,
      tonoEstado: tonoDeEstado(g.estado),
      revision: REVISIONES[g.estadoRevision] ?? g.estadoRevision,
      tonoRevision: tonoDeRevision(g.estadoRevision),
      infraestructura: f.tipo === 'INFRAESTRUCTURA',
    }
  })
}

const SENSIBILIDAD: Record<ColumnaDelCatalogo['sensibilidad'], string> = {
  NINGUNA: 'Ninguna',
  PERSONAL: 'Dato personal',
  FINANCIERO: 'Dato financiero',
  SECRETO: 'Secreto',
  DERIVADO: 'Derivado de un secreto (hash)',
}

/** La sensibilidad en palabras: la ficha no la transmite solo con color (95.4.6). */
export const sensibilidadEnPalabras = (c: ColumnaDelCatalogo): string => SENSIBILIDAD[c.sensibilidad]

const AL_BORRAR: Record<string, string> = {
  RESTRICT: 'Impide borrar la referida',
  'SET NULL': 'Queda vacía al borrar la referida',
  CASCADE: 'Se borra con la referida',
  'NO ACTION': 'Impide borrar la referida',
}

/** Una relación en texto: qué cardinalidad tiene y qué hace la base al borrar la fila referida. */
export type FilaDeRelacion = {
  id: string
  columna: string
  tabla: string
  esquema: string
  cardinalidad: string
  alBorrar: string
  obligatoria: string
  restriccion: string
}

export function filasDeRelaciones(relaciones: readonly RelacionDelCatalogo[]): FilaDeRelacion[] {
  return relaciones.map((r, i) => ({
    id: `${r.esquema}.${r.tabla}.${r.columna}.${i}`,
    columna: r.columna,
    tabla: r.tabla,
    esquema: r.esquema,
    cardinalidad: r.cardinalidad === '1:1' ? 'Uno a uno' : r.cardinalidad === 'N:1' ? 'Muchos a uno' : '—',
    alBorrar: r.alBorrar ? (AL_BORRAR[r.alBorrar] ?? r.alBorrar) : '—',
    obligatoria: r.obligatoria === undefined ? '—' : r.obligatoria ? 'Obligatoria' : 'Opcional',
    restriccion: r.restriccion ?? '—',
  }))
}

/** Cuántas columnas de cada sensibilidad: la del servidor si viene, si no se cuenta de las columnas. */
export function clasificacionDe(t: TablaDelCatalogo): { personales: number; financieros: number; secretos: number; derivados: number } {
  if (t.clasificacion) return t.clasificacion
  const n = (s: ColumnaDelCatalogo['sensibilidad']) => t.columnas.filter((c) => c.sensibilidad === s).length
  return { personales: n(S.Personal), financieros: n(S.Financiero), secretos: n(S.Secreto), derivados: n(S.Derivado) }
}
