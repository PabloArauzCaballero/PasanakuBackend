import type { Tono } from '@aportaya/ui/tono/tono'
import { DecisionDeRevision, type ColumnaDelCatalogo, type EntradaRevisionDeColumna, type RevisionDeColumna, type TablaDelCatalogo } from 'clientes/angular/auditoria'
import { normalizar, sensibilidadEnPalabras } from './dominio'

/**
 * CU-131 · la revisión de cada columna: decisión, confianza y motivo, como la revisión de columnas del
 * portal de Atlas. Una columna sin revisión guardada figura «Sin revisar».
 */
export type { EntradaRevisionDeColumna, RevisionDeColumna }

export const DECISIONES: Readonly<Record<string, string>> = { APROBADA: 'Aprobada', RECHAZADA: 'Rechazada', NECESITA_REVISION: 'Necesita revisión' }
export const CONFIANZAS: Readonly<Record<string, string>> = { BAJA: 'Baja', MEDIA: 'Media', ALTA: 'Alta' }
const TONO_DECISION: Record<string, Tono> = { APROBADA: 'ok', RECHAZADA: 'error', NECESITA_REVISION: 'aviso' }
const TONO_SENSIBILIDAD: Record<ColumnaDelCatalogo['sensibilidad'], Tono> = { NINGUNA: 'apagado', PERSONAL: 'pii', FINANCIERO: 'info', SECRETO: 'pii', DERIVADO: 'neutro' }

export const SIN_REVISAR = 'Sin revisar'
export const decisionEnPalabras = (r?: RevisionDeColumna): string => (r ? (DECISIONES[r.decision] ?? r.decision) : SIN_REVISAR)
export const tonoDeDecision = (r?: RevisionDeColumna): Tono => (r ? (TONO_DECISION[r.decision] ?? 'neutro') : 'apagado')
export const tonoDeSensibilidad = (c: ColumnaDelCatalogo): Tono => TONO_SENSIBILIDAD[c.sensibilidad]
export const esPersonal = (c: ColumnaDelCatalogo): boolean => c.sensibilidad === 'PERSONAL' || c.sensibilidad === 'SECRETO'

/** Una fila de la tabla de columnas de la ficha, ya en texto; `columna` queda para el cajón de revisión. */
export type FilaDeColumna = {
  id: string
  nombre: string
  tipo: string
  negocio: string
  sistema: string
  sensibilidad: string
  tonoSensibilidad: Tono
  nulos: string
  revision: string
  tonoRevision: Tono
  confianza: string
  acciones: string
  columna: ColumnaDelCatalogo
}

export function filasDeColumnas(t: TablaDelCatalogo): FilaDeColumna[] {
  return t.columnas.map((c) => ({
    id: c.nombre,
    nombre: c.nombre,
    tipo: c.tipo,
    negocio: c.negocio ? (c.origenNegocio === 'convencion' ? `${c.negocio} (convención)` : c.negocio) : 'Sin nota de negocio',
    sistema: c.sistema,
    sensibilidad: sensibilidadEnPalabras(c),
    tonoSensibilidad: tonoDeSensibilidad(c),
    nulos: c.nulable ? 'Admite' : 'No',
    revision: decisionEnPalabras(c.revision),
    tonoRevision: tonoDeDecision(c.revision),
    confianza: c.revision?.confianza ? (CONFIANZAS[c.revision.confianza] ?? c.revision.confianza) : '—',
    acciones: '',
    columna: c,
  }))
}

export type MetricasDeColumnas = { columnas: number; personales: number; financieras: number; descritas: number; aprobadas: number }

/** Las cifras de la pestaña Columnas, sobre todas las columnas de la tabla (no sobre lo filtrado). */
export function metricasDeColumnas(t: TablaDelCatalogo): MetricasDeColumnas {
  return {
    columnas: t.columnas.length,
    personales: t.columnas.filter(esPersonal).length,
    financieras: t.columnas.filter((c) => c.sensibilidad === 'FINANCIERO').length,
    descritas: t.columnas.filter((c) => !!c.negocio).length,
    aprobadas: t.columnas.filter((c) => c.revision?.decision === DecisionDeRevision.Aprobada).length,
  }
}

export type FiltrosDeColumnas = { q: string; sensibilidad: string; revision: string }

/** Busca por nombre, nota de negocio, tipo o sistema (sin acentos) y filtra por sensibilidad y revisión. `sinRevisar` es un valor propio del filtro. */
export function filtrarColumnas(t: TablaDelCatalogo, f: FiltrosDeColumnas): ColumnaDelCatalogo[] {
  const q = normalizar(f.q)
  return t.columnas.filter((c) => {
    const texto = normalizar([c.nombre, c.nombre.replaceAll('_', ' '), c.negocio ?? '', c.tipo, c.sistema].join(' '))
    const revision = c.revision?.decision ?? 'sinRevisar'
    return (!q || texto.includes(q)) && (!f.sensibilidad || c.sensibilidad === f.sensibilidad) && (!f.revision || revision === f.revision)
  })
}

export type BorradorDeRevision = { decision: string; confianza: string; motivo: string }

export const borradorDeRevision = (r?: RevisionDeColumna): BorradorDeRevision => ({
  decision: r?.decision ?? '',
  confianza: r?.confianza ?? '',
  motivo: r?.motivo ?? '',
})

/** Qué le falta al borrador para poder guardarse: la decisión siempre; el motivo, al rechazar. */
export function faltaEnRevision(b: BorradorDeRevision): { decision?: string; motivo?: string } {
  const faltas: { decision?: string; motivo?: string } = {}
  if (!b.decision) faltas.decision = 'Elegí una decisión.'
  if (b.decision === DecisionDeRevision.Rechazada && !b.motivo.trim()) faltas.motivo = 'Para rechazar hay que decir por qué.'
  return faltas
}

/** El cuerpo del PUT: sin cadenas vacías, con la versión leída (0 si la columna no tenía revisión). */
export function entradaDeRevision(b: BorradorDeRevision, versionLeida: number): EntradaRevisionDeColumna {
  const motivo = b.motivo.trim()
  return {
    decision: b.decision as EntradaRevisionDeColumna['decision'],
    version: versionLeida,
    ...(b.confianza ? { confianza: b.confianza as EntradaRevisionDeColumna['confianza'] } : {}),
    ...(motivo ? { motivo } : {}),
  }
}
