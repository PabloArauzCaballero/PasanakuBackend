import { HttpClient } from '@angular/common/http'
import { inject } from '@angular/core'
import type { TarifarioEnLista } from 'clientes/angular/tarifas/model/tarifarioEnLista'
import { GATEWAY } from '../../../nucleo/gateway'
import type { CargadorDePagina } from '../../../nucleo/tabla/tipos'
import { CURSOR_DESCENDENTE, cargadorDeListado } from './listado'

export type { TarifarioEnLista }

/**
 * CU-34 · `listarTarifarios` (`tarifas.yaml`, `GET /tarifas/tarifarios`): versiones del tarifario, de la más reciente a la
 * más antigua, cursor (`antesDe`, `antesDeId`). Exige TARIFARIO_PUBLICAR o AUDITORIA_LEER. Solo lectura: publicar
 * (`publicarTarifario`) no se ofrece desde esta pantalla. `simulacion.deltaIngresoEstimado` viaja SIN moneda.
 */
export const cargarTarifarios = (http: HttpClient, gateway: string): CargadorDePagina<TarifarioEnLista> =>
  cargadorDeListado<TarifarioEnLista>(http, `${gateway}/tarifas/tarifarios`, CURSOR_DESCENDENTE)

/** Fábrica en contexto de inyección: la pantalla no importa `HttpClient`. */
export const cargadorDeTarifarios = (): CargadorDePagina<TarifarioEnLista> => cargarTarifarios(inject(HttpClient), inject(GATEWAY))
