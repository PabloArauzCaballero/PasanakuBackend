import { HttpClient } from '@angular/common/http'
import { inject } from '@angular/core'
import { firstValueFrom } from 'rxjs'
import {
  EntradaEnvioReporteCanalEnum,
  EntradaGeneracionReporteFormularioEnum,
  type EntradaEnvioReporte,
  type EntradaGeneracionReporte,
  type ReporteRegulatorioEnLista,
  type SalidaReporteRegulatorio,
} from 'clientes/angular/cumplimiento'
import { GATEWAY } from '../../../nucleo/gateway'
import { contextoIdempotente } from '../../../nucleo/idempotencia.interceptor'
import { cargadorPorCursor } from '../../../nucleo/tabla/cursor'
import type { CargadorDePagina } from '../../../nucleo/tabla/tipos'
import { FORMA_H9, pedirPagina } from './paginas'
import { esPeriodo, type Errores } from './validacion'

/**
 * CU-43 · reportes regulatorios a la UIF (`listarReportesRegulatorios`,
 * `generarReporteRegulatorio`, `aprobarYEnviarReporte`). Son TRES momentos distintos y la
 * interfaz no los mezcla:
 *
 * 1. **Preparar** (generar): arma el archivo del período y su hash. Nada sale de la casa.
 * 2. **Aprobar y registrar el envío**: quien aprueba (la sesión) no puede ser quien generó
 *    (R-SEG-04); el servidor deja constancia del canal y del acuse del organismo.
 * 3. **El envío externo** lo hace la persona por el canal oficial; el sistema NO transmite el
 *    archivo, registra que se envió.
 *
 * Repetir lo mismo devuelve lo que ya hay con 200; lo nuevo, 201: el recibo lo distingue.
 */
export type Recibo<T> = { salida: T; yaExistia: boolean }

export function cargadorDeReportes(): CargadorDePagina<ReporteRegulatorioEnLista> {
  const url = `${inject(GATEWAY)}/uif/reportes`
  return cargadorPorCursor(pedirPagina<ReporteRegulatorioEnLista>(inject(HttpClient), url, FORMA_H9, ['estados']))
}

export function accionesDeReportes() {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  const enviar = async <E>(ruta: string, cuerpo: E): Promise<Recibo<SalidaReporteRegulatorio>> => {
    const r = await firstValueFrom(http.post<SalidaReporteRegulatorio>(`${gateway}${ruta}`, cuerpo, { context: contextoIdempotente(), observe: 'response' }))
    return { salida: r.body as SalidaReporteRegulatorio, yaExistia: r.status === 200 }
  }
  return {
    generar: (entrada: EntradaGeneracionReporte) => enviar('/uif/reportes', entrada),
    aprobarYEnviar: (entrada: EntradaEnvioReporte) => enviar('/uif/reportes/envios', entrada),
  }
}

export const FORMULARIOS: readonly string[] = Object.values(EntradaGeneracionReporteFormularioEnum)
export const CANALES: readonly string[] = Object.values(EntradaEnvioReporteCanalEnum)

export type CampoDeGeneracion = 'periodo' | 'catalogoCodigo' | 'formulario'
export type CampoDeEnvio = 'periodo' | 'catalogoCodigo' | 'canal' | 'numeroConstancia' | 'reintentos'

const validarCatalogo = (codigo: string): string | undefined =>
  !codigo.trim() ? 'Escribí el código del catálogo de reportes.' : codigo.trim().length > 30 ? 'El código admite hasta 30 caracteres.' : undefined

export function validarGeneracion(periodo: string, catalogoCodigo: string, formulario: string): Errores<CampoDeGeneracion> {
  const errores: Errores<CampoDeGeneracion> = {}
  if (!esPeriodo(periodo)) errores.periodo = 'El período va como año-mes, por ejemplo 2026-09.'
  const catalogo = validarCatalogo(catalogoCodigo)
  if (catalogo) errores.catalogoCodigo = catalogo
  if (!FORMULARIOS.includes(formulario)) errores.formulario = 'Elegí el formulario.'
  return errores
}

export const cuerpoDeGeneracion = (periodo: string, catalogoCodigo: string, formulario: string): EntradaGeneracionReporte => ({
  periodo: periodo.trim(),
  catalogoCodigo: catalogoCodigo.trim(),
  formulario: formulario as EntradaGeneracionReporteFormularioEnum,
})

export type DatosDeEnvio = { periodo: string; catalogoCodigo: string; canal: string; numeroConstancia: string; aceptado: boolean; reintentos: string }

export function validarEnvio(d: DatosDeEnvio): Errores<CampoDeEnvio> {
  const errores: Errores<CampoDeEnvio> = {}
  if (!esPeriodo(d.periodo)) errores.periodo = 'El período va como año-mes, por ejemplo 2026-09.'
  const catalogo = validarCatalogo(d.catalogoCodigo)
  if (catalogo) errores.catalogoCodigo = catalogo
  if (!CANALES.includes(d.canal)) errores.canal = 'Elegí el canal por el que lo enviaste.'
  if (d.numeroConstancia.trim().length > 60) errores.numeroConstancia = 'La constancia admite hasta 60 caracteres.'
  if (!/^\d+$/.test(d.reintentos.trim())) errores.reintentos = 'Los reintentos son un número entero, 0 o más.'
  return errores
}

export function cuerpoDeEnvio(d: DatosDeEnvio): EntradaEnvioReporte {
  const cuerpo: EntradaEnvioReporte = {
    periodo: d.periodo.trim(),
    catalogoCodigo: d.catalogoCodigo.trim(),
    canal: d.canal as EntradaEnvioReporteCanalEnum,
    aceptadoPorElOrganismo: d.aceptado,
    reintentos: Number(d.reintentos.trim()),
  }
  if (d.numeroConstancia.trim()) cuerpo.numeroConstancia = d.numeroConstancia.trim()
  return cuerpo
}

/** Solo un reporte GENERADO se puede aprobar y registrar como enviado (AP-CU43-03 para el resto). */
export const sePuedeEnviar = (r: ReporteRegulatorioEnLista): boolean => String(r.estado) === 'GENERADO'
