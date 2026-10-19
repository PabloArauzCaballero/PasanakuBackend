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

/** Los filtros de gobierno, leídos de la URL (`?estado=&revision=&modulo=`). Vacío es «todos». */
export type FiltrosDeGobierno = { estado: string; revision: string; modulo: string }

export function aplicarGobierno(tablas: readonly TablaDelCatalogo[], f: FiltrosDeGobierno): TablaDelCatalogo[] {
  return tablas.filter((t) => {
    const g = gobiernoDe(t)
    return (!f.estado || g.estado === f.estado) && (!f.revision || g.estadoRevision === f.revision) && (!f.modulo || t.modulo === f.modulo)
  })
}

export type Metricas = { total: number; activas: number; aRevisar: number; conDatosPersonales: number; haciaElRetiro: number }

/** Las cifras de la cabecera, sobre todo el catálogo (no sobre lo filtrado: es el estado del inventario). */
export function metricas(tablas: readonly TablaDelCatalogo[]): Metricas {
  const estados = tablas.map((t) => gobiernoDe(t))
  return {
    total: tablas.length,
    activas: estados.filter((g) => g.estado === 'ACTIVA').length,
    aRevisar: estados.filter((g) => g.estadoRevision === 'DETECTADA' || g.estadoRevision === 'NECESITA_REVISION').length,
    conDatosPersonales: tablas.filter(tieneDatosPersonales).length,
    haciaElRetiro: estados.filter((g) => ['CANDIDATA_A_RETIRO', 'RETIRADA', 'DESHABILITADA'].includes(g.estado)).length,
  }
}

/** Los módulos del modelo presentes en el catálogo, para el filtro. Orden por número. */
export function modulos(tablas: readonly TablaDelCatalogo[]): { valor: string; texto: string }[] {
  const vistos = new Map<string, string>()
  for (const t of tablas) vistos.set(t.modulo, `${t.modulo} · ${t.nombreModulo}`)
  return [...vistos].sort(([a], [b]) => a.localeCompare(b)).map(([valor, texto]) => ({ valor, texto }))
}

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
