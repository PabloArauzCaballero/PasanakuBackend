import { HttpClient } from '@angular/common/http'
import { inject } from '@angular/core'
import { firstValueFrom } from 'rxjs'
import {
  EntradaPruebaDeContinuidadTipoEnum,
  type EntradaPruebaDeContinuidad,
  type PruebaDeContinuidadEnLista,
  type SalidaPruebaDeContinuidad,
} from 'clientes/angular/cumplimiento'
import { GATEWAY } from '../../../nucleo/gateway'
import { contextoIdempotente } from '../../../nucleo/idempotencia.interceptor'
import { cargadorPorCursor } from '../../../nucleo/tabla/cursor'
import type { CargadorDePagina } from '../../../nucleo/tabla/tipos'
import type { Recibo } from './cu43-reportes'
import { FORMA_H9, pedirPagina } from './paginas'
import { esUuid, type Errores } from './validacion'

/**
 * CU-56 · pruebas de continuidad (`listarPruebasDeContinuidad`, `registrarPruebaDeContinuidad`).
 * La lista no admite `estados`. Registrar evalúa el RTO/RPO contra el plan y, si no se alcanza,
 * abre hallazgo y plan de acción (lo dice el recibo).
 *
 * `perdidaCuantificada` NO se captura: el contrato la define como importe SIN moneda, y un
 * importe sin moneda no entra por esta interfaz (regla 91.1.2). Queda registrado como ambigüedad.
 */
export function cargadorDePruebas(): CargadorDePagina<PruebaDeContinuidadEnLista> {
  const url = `${inject(GATEWAY)}/cumplimiento/continuidad/pruebas`
  return cargadorPorCursor(pedirPagina<PruebaDeContinuidadEnLista>(inject(HttpClient), url, FORMA_H9, []))
}

export function registrarPrueba() {
  const http = inject(HttpClient)
  const url = `${inject(GATEWAY)}/cumplimiento/continuidad/pruebas`
  return async (entrada: EntradaPruebaDeContinuidad): Promise<Recibo<SalidaPruebaDeContinuidad>> => {
    const r = await firstValueFrom(http.post<SalidaPruebaDeContinuidad>(url, entrada, { context: contextoIdempotente(), observe: 'response' }))
    return { salida: r.body as SalidaPruebaDeContinuidad, yaExistia: r.status === 200 }
  }
}

export const TIPOS_DE_PRUEBA: readonly string[] = Object.values(EntradaPruebaDeContinuidadTipoEnum)

export type DatosDePrueba = {
  planContinuidadId: string
  tipo: string
  fecha: string
  ejecutadaPor: string
  actaComiteId: string
  rto: string
  rpo: string
  evidenciaUrl: string
  impactoEnClientes: boolean
}
export type CampoDePrueba = 'planContinuidadId' | 'tipo' | 'fecha' | 'ejecutadaPor' | 'actaComiteId' | 'rto' | 'rpo' | 'evidenciaUrl'

const entero = (v: string) => /^\d+$/.test(v.trim())

export function validarPrueba(d: DatosDePrueba): Errores<CampoDePrueba> {
  const e: Errores<CampoDePrueba> = {}
  if (!esUuid(d.planContinuidadId)) e.planContinuidadId = 'Pegá el identificador del plan de continuidad (UUID).'
  if (!TIPOS_DE_PRUEBA.includes(d.tipo)) e.tipo = 'Elegí el tipo de prueba.'
  if (!/^\d{4}-\d{2}-\d{2}$/.test(d.fecha)) e.fecha = 'Indicá la fecha de la prueba.'
  if (!esUuid(d.ejecutadaPor)) e.ejecutadaPor = 'Pegá el identificador de quien la ejecutó (UUID).'
  if (d.actaComiteId.trim() && !esUuid(d.actaComiteId)) e.actaComiteId = 'Si hay acta, pegá su identificador (UUID).'
  if (!entero(d.rto)) e.rto = 'El RTO obtenido va en minutos, entero, 0 o más.'
  if (!entero(d.rpo)) e.rpo = 'El RPO obtenido va en minutos, entero, 0 o más.'
  if (d.evidenciaUrl.trim().length > 255) e.evidenciaUrl = 'El enlace admite hasta 255 caracteres.'
  return e
}

export function cuerpoDePrueba(d: DatosDePrueba): EntradaPruebaDeContinuidad {
  const cuerpo: EntradaPruebaDeContinuidad = {
    planContinuidadId: d.planContinuidadId.trim(),
    tipo: d.tipo as EntradaPruebaDeContinuidadTipoEnum,
    fecha: d.fecha,
    ejecutadaPor: d.ejecutadaPor.trim(),
    rtoObtenido: Number(d.rto.trim()),
    rpoObtenido: Number(d.rpo.trim()),
    impactoEnClientes: d.impactoEnClientes,
  }
  if (d.actaComiteId.trim()) cuerpo.actaComiteId = d.actaComiteId.trim()
  if (d.evidenciaUrl.trim()) cuerpo.evidenciaUrl = d.evidenciaUrl.trim()
  return cuerpo
}

/** AP-CU56-01 → acta; AP-CU56-03 → plan. AP-CU56-02 no es un campo: va como error general. */
export const CAMPO_DE_CODIGO_CU56: Readonly<Record<string, CampoDePrueba>> = { 'AP-CU56-01': 'actaComiteId', 'AP-CU56-03': 'planContinuidadId' }
