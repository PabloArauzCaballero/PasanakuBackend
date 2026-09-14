import { HttpClient, httpResource, HttpContext } from '@angular/common/http'
import { inject, Signal } from '@angular/core'
import type { Observable } from 'rxjs'
import type { DecisionDeVerificacion, EnlaceDeFoto, ExpedienteEnRevision } from 'clientes/angular/identidad'
import { GATEWAY } from '../../../nucleo/gateway'
import { CLAVE_IDEMPOTENCIA, claveDeIdempotencia } from '../../../nucleo/idempotencia.interceptor'

/**
 * CU-02 · el portal de riesgo: la cola de expedientes de identidad, sus fotos y la
 * decisión manual. Contrato real de `identidad`: `listarVerificaciones`,
 * `verFotoDelExpediente` y `resolverVerificacion`.
 *
 * El HTTP vive acá y nunca en la pantalla (`scripts/verificar_frontend.py`: «sin red
 * en vista»), y los tipos son los GENERADOS del contrato (invariante 2).
 */
export function expedientesEnEstado(estado: Signal<string>) {
  const gateway = inject(GATEWAY)
  return httpResource<ExpedienteEnRevision[]>(() => `${gateway}/identidad/verificaciones?estado=${estado()}`)
}

/** Una cola vacía es una cola vacía: no hay nada que revisar, y eso es una buena noticia. */
export const colaVacia = (cola: ExpedienteEnRevision[]): boolean => cola.length === 0

/**
 * El enlace de una foto. Se pide de a una y **al mirarla**, no al listar: cada foto
 * de una cédula es un dato personal sensible, y cada lectura queda registrada. El
 * enlace vive diez minutos.
 */
export function crearPedirFoto(): (verificacionId: string, cara: string) => Observable<EnlaceDeFoto> {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  return (verificacionId: string, cara: string) =>
    http.get<EnlaceDeFoto>(`${gateway}/identidad/verificaciones/${verificacionId}/fotos/${cara}`)
}

/**
 * La decisión, a mano. Clave de idempotencia nueva por intento (invariante 7): una
 * decisión que se reenvía por un doble clic no puede resolverse dos veces.
 */
export function crearResolver(): (verificacionId: string, cuerpo: DecisionDeVerificacion) => Observable<ExpedienteEnRevision> {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  return (verificacionId: string, cuerpo: DecisionDeVerificacion) => {
    const contexto = new HttpContext().set(CLAVE_IDEMPOTENCIA, claveDeIdempotencia())
    return http.post<ExpedienteEnRevision>(`${gateway}/identidad/verificaciones/${verificacionId}/decision`, cuerpo, {
      context: contexto,
    })
  }
}
