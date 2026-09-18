import { HttpClient, httpResource, HttpContext } from '@angular/common/http'
import { inject, Signal } from '@angular/core'
import type { Observable } from 'rxjs'
import {
  ExpedienteEnRevisionEstadoEnum,
  type DecisionDeVerificacion,
  type EnlaceDeFoto,
  type ExpedienteEnRevision,
} from 'clientes/angular/identidad'
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
  return httpResource<ExpedienteEnRevision[]>(() => {
    const e = estado()
    // `POR_DECIDIR` no es un estado del contrato: es la cola de trabajo. Se pide sin
    // filtro y se recorta acá, porque el backend filtra por UN estado y los que
    // esperan una persona son DOS (ver `POR_DECIDIR` abajo).
    return e === POR_DECIDIR
      ? `${gateway}/identidad/verificaciones`
      : `${gateway}/identidad/verificaciones?estado=${e}`
  })
}

/**
 * La cola de trabajo: todo lo que espera una decisión humana.
 *
 * Son dos estados, no uno. El alta abre el expediente en `EN_REVISION`
 * (`RegistroRepositorio.iniciarVerificacion`) y el modelo además admite `PENDIENTE`.
 * Una pantalla que arrancaba filtrando `PENDIENTE` mostraba «no hay expedientes
 * esperando» con la cola llena — el peor mensaje posible: dice que no hay trabajo
 * cuando hay gente esperando su cuenta.
 */
export const POR_DECIDIR = 'POR_DECIDIR'

const ESPERAN_A_UNA_PERSONA: readonly string[] = [
  ExpedienteEnRevisionEstadoEnum.Pendiente,
  ExpedienteEnRevisionEstadoEnum.EnRevision,
]

/** ¿Este expediente espera que alguien lo mire? */
export const esperaDecision = (e: ExpedienteEnRevision): boolean => ESPERAN_A_UNA_PERSONA.includes(e.estado)

/** Lo que se muestra con el filtro elegido. `POR_DECIDIR` recorta; el resto ya viene filtrado. */
export function recortar(cola: readonly ExpedienteEnRevision[], estado: string): ExpedienteEnRevision[] {
  return estado === POR_DECIDIR ? cola.filter(esperaDecision) : [...cola]
}

/** Una cola vacía es una cola vacía: no hay nada que revisar, y eso es una buena noticia. */
export const colaVacia = (cola: ExpedienteEnRevision[]): boolean => cola.length === 0

/**
 * La carpeta del expediente en el servidor de archivos: `identidad/<usuarioId>/`.
 *
 * Se arma acá y no se pide al backend porque es una convención del almacén
 * (`DestinoDeObjeto.deExpediente`), no un dato de la fila. Sirve para ir a buscar el
 * expediente entero cuando hay que entregarlo o auditarlo.
 */
export const carpetaDelExpediente = (usuarioId: string): string => `identidad/${usuarioId}/`

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
