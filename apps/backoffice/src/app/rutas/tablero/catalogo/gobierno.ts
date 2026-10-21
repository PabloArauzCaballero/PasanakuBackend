import type { Tono } from '@aportaya/ui/tono/tono'
import { EstadoDeRevision, EstadoDeTabla, type EntradaFichaDeGobierno, type FichaDeGobierno, type TablaDelCatalogo } from 'clientes/angular/auditoria'

/**
 * CU-130 · la ficha de gobierno de cada tabla: estado (en uso o no), revisión, responsable y retención — como el
 * catálogo de datos del portal de Atlas. La sensibilidad no se edita: se deriva de las columnas del modelo.
 */
export type { EntradaFichaDeGobierno, FichaDeGobierno }

export const ESTADOS: Readonly<Record<string, string>> = {
  ACTIVA: 'Activa',
  PLANIFICADA: 'Planificada',
  CANDIDATA_A_RETIRO: 'Candidata a retiro',
  RETIRADA: 'Retirada',
  DESHABILITADA: 'Deshabilitada',
}

export const REVISIONES: Readonly<Record<string, string>> = {
  DETECTADA: 'Detectada',
  NECESITA_REVISION: 'Necesita revisión',
  APROBADA: 'Aprobada',
  RECHAZADA: 'Rechazada',
}

const TONO_ESTADO: Record<string, Tono> = { ACTIVA: 'ok', PLANIFICADA: 'info', CANDIDATA_A_RETIRO: 'aviso', RETIRADA: 'neutro', DESHABILITADA: 'error' }
const TONO_REVISION: Record<string, Tono> = { DETECTADA: 'neutro', NECESITA_REVISION: 'aviso', APROBADA: 'ok', RECHAZADA: 'error' }

/** Sin ficha en la respuesta (servidor viejo): la misma omisión que el servidor, activa y detectada. */
export const POR_OMISION: FichaDeGobierno = { estado: EstadoDeTabla.Activa, estadoRevision: EstadoDeRevision.Detectada, criticaParaAuditoria: false, version: 0 }

export const gobiernoDe = (t: TablaDelCatalogo): FichaDeGobierno => t.gobierno ?? POR_OMISION
export const tonoDeEstado = (estado: string): Tono => TONO_ESTADO[estado] ?? 'neutro'
export const tonoDeRevision = (revision: string): Tono => TONO_REVISION[revision] ?? 'neutro'

export const tieneDatosPersonales = (t: TablaDelCatalogo): boolean => t.columnas.some((c) => c.sensibilidad === 'PERSONAL' || c.sensibilidad === 'SECRETO')
export const tieneDatosFinancieros = (t: TablaDelCatalogo): boolean => t.columnas.some((c) => c.sensibilidad === 'FINANCIERO')

/** Lo que el formulario edita, en texto (los campos del `ap-campo` y las listas son cadenas). */
export type Borrador = { estado: string; estadoRevision: string; responsable: string; politicaRetencionId: string; criticaParaAuditoria: boolean; notaRevision: string }

export const borradorDe = (g: FichaDeGobierno): Borrador => ({
  estado: g.estado,
  estadoRevision: g.estadoRevision,
  responsable: g.responsable ?? '',
  politicaRetencionId: g.politicaRetencionId ?? '',
  criticaParaAuditoria: g.criticaParaAuditoria,
  notaRevision: g.notaRevision ?? '',
})

/** El cuerpo del PUT: recortado, sin cadenas vacías (el contrato exige mínimo 1) y con la versión que se leyó. */
export function entradaDe(b: Borrador, versionLeida: number): EntradaFichaDeGobierno {
  const responsable = b.responsable.trim()
  const nota = b.notaRevision.trim()
  return {
    estado: b.estado as EntradaFichaDeGobierno['estado'],
    estadoRevision: b.estadoRevision as EntradaFichaDeGobierno['estadoRevision'],
    criticaParaAuditoria: b.criticaParaAuditoria,
    version: versionLeida,
    ...(responsable ? { responsable } : {}),
    ...(b.politicaRetencionId ? { politicaRetencionId: b.politicaRetencionId } : {}),
    ...(nota ? { notaRevision: nota } : {}),
  }
}
