import { HttpClient } from '@angular/common/http'
import { inject } from '@angular/core'
import type { Observable } from 'rxjs'
import type { DefinicionDeReporteEnLista } from 'clientes/angular/auditoria/model/definicionDeReporteEnLista'
import type { EjecucionDeReporteEnLista } from 'clientes/angular/auditoria/model/ejecucionDeReporteEnLista'
import type { EntradaReporte } from 'clientes/angular/auditoria/model/entradaReporte'
import type { SalidaReporte } from 'clientes/angular/auditoria/model/salidaReporte'
import { GATEWAY } from '../../../nucleo/gateway'
import { contextoIdempotente } from '../../../nucleo/idempotencia.interceptor'
import type { CargadorDePagina } from '../../../nucleo/tabla/tipos'
import { CURSOR_DESCENDENTE, cargadorDeListado, type NombresDeCursor } from './listado'

export type { DefinicionDeReporteEnLista, EjecucionDeReporteEnLista }

/** El catálogo pagina por nombre, ascendente: (`despuesDe`, `despuesDeId`) → (`proximoDespuesDe`, `proximoDespuesDeId`). */
export const CURSOR_POR_NOMBRE: NombresDeCursor = { parametros: ['despuesDe', 'despuesDeId'], proximo: ['proximoDespuesDe', 'proximoDespuesDeId'] }

/**
 * CU-58 · `listarDefinicionesDeReporte` (`auditoria.yaml`, `GET /reportes`, sin filtro de estado) y
 * `listarEjecucionesDeReporte` (`GET /reportes/ejecuciones`, sin parámetros ni mensaje de error de la ejecución). Ambas
 * exigen AUDITORIA_LEER. Ejecutar un reporte (`ejecutarReporte`) no es parte de esta pantalla de solo lectura.
 */
export const cargarDefiniciones = (http: HttpClient, gateway: string): CargadorDePagina<DefinicionDeReporteEnLista> =>
  cargadorDeListado<DefinicionDeReporteEnLista>(http, `${gateway}/reportes`, CURSOR_POR_NOMBRE)

export const cargarEjecuciones = (http: HttpClient, gateway: string): CargadorDePagina<EjecucionDeReporteEnLista> =>
  cargadorDeListado<EjecucionDeReporteEnLista>(http, `${gateway}/reportes/ejecuciones`, CURSOR_DESCENDENTE)

export const cargadorDeDefiniciones = (): CargadorDePagina<DefinicionDeReporteEnLista> => cargarDefiniciones(inject(HttpClient), inject(GATEWAY))
export const cargadorDeEjecuciones = (): CargadorDePagina<EjecucionDeReporteEnLista> => cargarEjecuciones(inject(HttpClient), inject(GATEWAY))

export type ParametroEsperado = NonNullable<DefinicionDeReporteEnLista['parametrosEsperados']>[number]
export type { EntradaReporte, SalidaReporte }

/** Los formatos que admite `ck_exportacion_reporte_formato` (contrato `EntradaReporte.formato`). Vacío: sin exportar. */
export const FORMATOS: readonly string[] = ['CSV', 'JSON', 'PDF', 'XLSX']
/** `justificacion` del contrato: 10..300, obligatoria si la definición tiene datos sensibles (`ck_acceso_justificacion`). */
export const JUSTIFICACION_MINIMA = 10
export const JUSTIFICACION_MAXIMA = 300

/**
 * `POST /reportes/{definicionId}/ejecuciones` (`ejecutarReporte`, CU-58). La clave la gobierna el contenido: reintentar
 * lo mismo tras un corte no ejecuta dos veces. El servidor valida los parámetros contra su lista blanca y exige el
 * `permisoRequerido` de la definición.
 */
export function crearEjecutarReporte(): (definicionId: string, entrada: EntradaReporte) => Observable<SalidaReporte> {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  return (definicionId, entrada) =>
    http.post<SalidaReporte>(`${gateway}/reportes/${definicionId}/ejecuciones`, entrada, { context: contextoIdempotente() })
}
