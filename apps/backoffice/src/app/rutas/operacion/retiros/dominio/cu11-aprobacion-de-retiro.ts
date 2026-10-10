import { HttpClient } from '@angular/common/http'
import { inject } from '@angular/core'
import type { Observable } from 'rxjs'
import { GATEWAY } from '../../../../nucleo/gateway'
import { contextoIdempotente } from '../../../../nucleo/idempotencia.interceptor'

/**
 * CU-11 · doble aprobación de un retiro (`aprobarRetiro`, `POST /billetera/retiros/{ordenId}/aprobacion`).
 *
 * Los tipos son los de `nucleo-financiero.yaml` (`EntradaAprobacionRetiro`, `SalidaAprobacionRetiro`). El contrato NO
 * publica un listado ni una consulta de órdenes en revisión: la pantalla trabaja con el identificador que llega en el
 * aviso de la orden y lo dice, en vez de simular una bandeja que no existe. Lo que no está en el contrato no existe acá:
 * no se agregan campos (el backend rechaza con 400 los de más).
 */
export type DesenlaceDeRetiro = 'AUTORIZADA' | 'RECHAZADA'

export type EntradaAprobacionRetiro = { desenlace: DesenlaceDeRetiro; motivo?: string }
export type SalidaAprobacionRetiro = { ordenRetiroId: string; estado: DesenlaceDeRetiro; aprobadaPor: string }

/** El motivo que acepta el contrato (`maxLength: 500`). */
export const MOTIVO_MAXIMO_RETIRO = 500

/**
 * El envío. La clave de idempotencia la gobierna el contenido (orden + desenlace + motivo): reintentar lo mismo tras
 * perder la respuesta sale con la MISMA clave y el servidor devuelve lo ya registrado; cambiar el desenlace es otra operación.
 */
export function crearAprobarRetiro(): (ordenId: string, entrada: EntradaAprobacionRetiro) => Observable<SalidaAprobacionRetiro> {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  return (ordenId, entrada) =>
    http.post<SalidaAprobacionRetiro>(`${gateway}/billetera/retiros/${ordenId}/aprobacion`, entrada, { context: contextoIdempotente() })
}
