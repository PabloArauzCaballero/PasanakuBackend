import { HttpClient } from '@angular/common/http'
import { inject } from '@angular/core'
import type { DefinicionDeReporteEnLista } from 'clientes/angular/auditoria/model/definicionDeReporteEnLista'
import type { EjecucionDeReporteEnLista } from 'clientes/angular/auditoria/model/ejecucionDeReporteEnLista'
import { GATEWAY } from '../../../nucleo/gateway'
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
