import { moduloTableroExplicacion } from './explicaciones-tablero'
import { moduloOperacionExplicacion } from './explicaciones-operacion'
import { moduloCumplimientoExplicacion } from './explicaciones-cumplimiento'
import { moduloSistemasExplicacion } from './explicaciones-sistemas'
import {
  moduloContabilidadExplicacion,
  moduloPublicidadExplicacion,
} from './explicaciones-finanzas-publicidad'
import {
  moduloConfiguracionExplicacion,
  moduloAyudaExplicacion,
} from './explicaciones-plataforma'
import type {
  ExplicacionDeModulo,
  ExplicacionDeVista,
  ExplicacionResuelta,
} from './tipos'

export type { ExplicacionDeModulo, ExplicacionDeVista, ExplicacionResuelta }

export const modulosExplicaciones: readonly ExplicacionDeModulo[] = [
  moduloTableroExplicacion,
  moduloOperacionExplicacion,
  moduloCumplimientoExplicacion,
  moduloSistemasExplicacion,
  moduloContabilidadExplicacion,
  moduloPublicidadExplicacion,
  moduloConfiguracionExplicacion,
  moduloAyudaExplicacion,
]

/**
 * Comprueba si `pathname` coincide con un patrón `prefijo`.
 * Soporta parámetros en corchetes como `[id]`, `[tabla]`, `[ordenId]`.
 * Por ejemplo: `/operacion/retiros/abc-123` coincide con `/operacion/retiros/[ordenId]`.
 */
export function coincidePrefijo(pathname: string, prefijo: string): boolean {
  const rutaLimpia = pathname.split('?')[0]!.replace(/\/$/, '')
  const patronLimpio = prefijo.replace(/\/$/, '')

  if (!patronLimpio.includes('[')) {
    return rutaLimpia === patronLimpio || rutaLimpia.startsWith(patronLimpio + '/')
  }

  const segmentosRuta = rutaLimpia.split('/').filter(Boolean)
  const segmentosPatron = patronLimpio.split('/').filter(Boolean)

  if (segmentosRuta.length < segmentosPatron.length) return false

  return segmentosPatron.every((seg, idx) => {
    if (seg.startsWith('[') && seg.endsWith(']')) {
      return Boolean(segmentosRuta[idx])
    }
    return seg === segmentosRuta[idx]
  })
}

/**
 * Resuelve la explicación de módulo y de vista para una URL dada.
 * Gana el match de mayor especificidad (prefijo más largo) tanto a nivel de módulo como de vista.
 */
export function resolverExplicacion(url: string): ExplicacionResuelta | null {
  const pathname = url.split('?')[0]!.trim()
  if (!pathname || pathname === '/') {
    return {
      modulo: moduloTableroExplicacion,
      vista: moduloTableroExplicacion.vistas['/tablero'] ?? null,
    }
  }

  let mejorModulo: ExplicacionDeModulo | null = null
  let longitudModulo = -1

  for (const mod of modulosExplicaciones) {
    for (const pref of mod.prefijos) {
      const coincide = mod.exacto
        ? pathname === pref
        : pathname === pref || pathname.startsWith(pref + '/') || pref === pathname
      if (coincide && pref.length > longitudModulo) {
        mejorModulo = mod
        longitudModulo = pref.length
      }
    }
  }

  if (!mejorModulo) return null

  let mejorVista: ExplicacionDeVista | null = null
  let longitudVista = -1

  for (const [patron, vista] of Object.entries(mejorModulo.vistas)) {
    if (coincidePrefijo(pathname, patron) && patron.length > longitudVista) {
      mejorVista = vista
      longitudVista = patron.length
    }
  }

  return {
    modulo: mejorModulo,
    vista: mejorVista,
  }
}
