import { HttpClient } from '@angular/common/http'
import { inject } from '@angular/core'
import { firstValueFrom } from 'rxjs'
import type { BloqueoEnLista } from 'clientes/angular/nucleo-financiero/model/bloqueoEnLista'
import type { SalidaLevantamientoBloqueo } from 'clientes/angular/nucleo-financiero/model/salidaLevantamientoBloqueo'
import { GATEWAY } from '../../../nucleo/gateway'
import { contextoIdempotente } from '../../../nucleo/idempotencia.interceptor'
import { cargadorPorCursor } from '../../../nucleo/tabla/cursor'
import type { CargadorDePagina } from '../../../nucleo/tabla/tipos'
import { FORMA_H9, pedirPagina } from './paginas'

/**
 * CU-17 · los bloqueos por orden de autoridad (`nucleo-financiero.yaml`: `listarBloqueosPorAutoridad`,
 * `levantarBloqueoPorAutoridad`). El listado usa el cursor descendente `antesDe` + `antesDeId` (la
 * misma forma que las colas H9) y un solo filtro, `estado`. Levantar no lleva cuerpo: el servidor
 * toma a quien levanta de la sesión y rechaza a quien registró el bloqueo (R-BIL-32, cuatro ojos).
 */
export function cargadorDeBloqueos(): CargadorDePagina<BloqueoEnLista> {
  const url = `${inject(GATEWAY)}/billetera/bloqueos`
  return cargadorPorCursor(pedirPagina<BloqueoEnLista>(inject(HttpClient), url, FORMA_H9, ['estado']))
}

export function accionesDeBloqueos() {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  return {
    levantar: (bloqueoId: string): Promise<SalidaLevantamientoBloqueo> =>
      firstValueFrom(
        http.post<SalidaLevantamientoBloqueo>(`${gateway}/billetera/bloqueos/${encodeURIComponent(bloqueoId)}/levantamiento`, null, {
          context: contextoIdempotente(),
        }),
      ),
  }
}
