import { HttpClient } from '@angular/common/http'
import { inject } from '@angular/core'
import type { Observable } from 'rxjs'
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

/** Lo que responde `ponerTarifarioVigente`. */
export type TarifarioPuestoEnVigencia = { tarifarioId: string; estado: 'VIGENTE'; desde: string }

/**
 * `POST /tarifas/tarifarios/{id}/vigencia` (`ponerTarifarioVigente`, CU-34): pone en vigencia una versión cuyo preaviso
 * ya se cumplió; la anterior queda SUSTITUIDA. Sin cuerpo ni clave en el contrato: repetirla sobre una versión ya
 * vigente la rechaza el servidor (AP-CU34-02), no la duplica. Si el preaviso vence, también lo decide el servidor.
 */
export function crearPonerVigente(): (tarifarioId: string) => Observable<TarifarioPuestoEnVigencia> {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  return (tarifarioId) => http.post<TarifarioPuestoEnVigencia>(`${gateway}/tarifas/tarifarios/${tarifarioId}/vigencia`, null)
}
