import { HttpClient } from '@angular/common/http'
import { inject } from '@angular/core'
import { firstValueFrom } from 'rxjs'
import {
  EntradaRequerimientoDeAutoridadAutoridadEnum,
  type EntradaRequerimientoDeAutoridad,
  type EntradaRespuestaRequerimiento,
  type RequerimientoEnLista,
  type SalidaRequerimientoDeAutoridad,
} from 'clientes/angular/cumplimiento'
import { GATEWAY } from '../../../nucleo/gateway'
import { contextoIdempotente } from '../../../nucleo/idempotencia.interceptor'
import { cargadorPorCursor } from '../../../nucleo/tabla/cursor'
import type { CargadorDePagina } from '../../../nucleo/tabla/tipos'
import type { Recibo } from './cu43-reportes'
import { FORMA_H9, pedirPagina } from './paginas'
import { aIsoLaPaz, esUuid, type Errores } from './validacion'

/**
 * CU-45 · requerimientos de autoridad (`listarRequerimientosDeAutoridad`,
 * `registrarRequerimientoDeAutoridad`, `responderRequerimientoDeAutoridad`). Sin el oficio y
 * su hash no se actúa; se responde siempre, dentro o fuera de plazo; el plazo y el número de
 * oficio son los registrados, nunca los del cliente. La lista no trae alcance ni documento.
 */
export function cargadorDeRequerimientos(): CargadorDePagina<RequerimientoEnLista> {
  const url = `${inject(GATEWAY)}/uif/requerimientos`
  return cargadorPorCursor(pedirPagina<RequerimientoEnLista>(inject(HttpClient), url, FORMA_H9, ['estados']))
}

export function accionesDeRequerimientos() {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  const enviar = async <E>(ruta: string, cuerpo: E): Promise<Recibo<SalidaRequerimientoDeAutoridad>> => {
    const r = await firstValueFrom(http.post<SalidaRequerimientoDeAutoridad>(`${gateway}${ruta}`, cuerpo, { context: contextoIdempotente(), observe: 'response' }))
    return { salida: r.body as SalidaRequerimientoDeAutoridad, yaExistia: r.status === 200 }
  }
  return {
    registrar: (entrada: EntradaRequerimientoDeAutoridad) => enviar('/uif/requerimientos', entrada),
    responder: (requerimientoId: string, entrada: EntradaRespuestaRequerimiento) =>
      enviar(`/uif/requerimientos/${encodeURIComponent(requerimientoId)}/respuesta`, entrada),
  }
}

export const AUTORIDADES: readonly string[] = Object.values(EntradaRequerimientoDeAutoridadAutoridadEnum)

export type DatosDeRequerimiento = {
  autoridad: string
  numeroOficio: string
  plazoLocal: string
  usuarioAfectadoId: string
  alcance: string
  documentoUrl: string
  hashDocumento: string
  exigeBloqueoDeSaldo: boolean
}
export type CampoDeRequerimiento = 'autoridad' | 'numeroOficio' | 'plazoLocal' | 'usuarioAfectadoId' | 'alcance' | 'documentoUrl' | 'hashDocumento'

export function validarRequerimiento(d: DatosDeRequerimiento): Errores<CampoDeRequerimiento> {
  const e: Errores<CampoDeRequerimiento> = {}
  if (!AUTORIDADES.includes(d.autoridad)) e.autoridad = 'Elegí la autoridad que lo emite.'
  const oficio = d.numeroOficio.trim()
  if (!oficio || oficio.length > 60) e.numeroOficio = 'Escribí el número de oficio (hasta 60 caracteres).'
  if (!aIsoLaPaz(d.plazoLocal)) e.plazoLocal = 'Indicá fecha y hora del plazo de respuesta.'
  if (d.usuarioAfectadoId.trim() && !esUuid(d.usuarioAfectadoId)) e.usuarioAfectadoId = 'Si hay una persona alcanzada, pegá su identificador (UUID).'
  if (!d.alcance.trim() || d.alcance.trim().length > 300) e.alcance = 'Describí el alcance pedido (hasta 300 caracteres).'
  if (!d.documentoUrl.trim() || d.documentoUrl.trim().length > 255) e.documentoUrl = 'Pegá el enlace al oficio (hasta 255 caracteres).'
  if (!/^[0-9a-f]{64}$/.test(d.hashDocumento.trim())) e.hashDocumento = 'El hash SHA-256 del oficio: 64 caracteres hexadecimales en minúscula.'
  return e
}

export function cuerpoDeRequerimiento(d: DatosDeRequerimiento): EntradaRequerimientoDeAutoridad {
  const cuerpo: EntradaRequerimientoDeAutoridad = {
    autoridad: d.autoridad as EntradaRequerimientoDeAutoridadAutoridadEnum,
    numeroOficio: d.numeroOficio.trim(),
    plazoRespuesta: aIsoLaPaz(d.plazoLocal) as string,
    alcance: d.alcance.trim(),
    documentoUrl: d.documentoUrl.trim(),
    hashDocumento: d.hashDocumento.trim(),
    exigeBloqueoDeSaldo: d.exigeBloqueoDeSaldo,
  }
  if (d.usuarioAfectadoId.trim()) cuerpo.usuarioAfectadoId = d.usuarioAfectadoId.trim()
  return cuerpo
}

export const validarRespuesta = (respuestaUrl: string): string | undefined =>
  !respuestaUrl.trim() || respuestaUrl.trim().length > 255 ? 'Pegá el enlace a la respuesta entregada (hasta 255 caracteres).' : undefined

/** Un requerimiento respondido o archivado ya no se responde desde acá. */
export const sePuedeResponder = (r: RequerimientoEnLista): boolean => !['RESPONDIDO', 'ARCHIVADO'].includes(r.estado)
