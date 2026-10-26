import { httpResource } from '@angular/common/http'
import { inject } from '@angular/core'
import type { EndpointDelDia, SalidaEndpointsDelDia } from 'clientes/angular/auditoria'
import { GATEWAY } from '../../../nucleo/gateway'
import { conUnidad, decimal } from './indicadores'

/** Una fila de la tabla de endpoints del día, ya en texto (la tabla no formatea). */
export type FilaDeEndpoint = {
  id: string
  servicio: string
  metodo: string
  ruta: string
  solicitudes: string
  errores: string
  p95: string
  /** Los números crudos, para ordenar como números (el texto ya trae separador de miles). */
  orden: { solicitudes: number; errores: number; p95: number }
}

export function filasDeEndpoints(salida: SalidaEndpointsDelDia | undefined): FilaDeEndpoint[] {
  return (salida?.endpoints ?? []).map((e: EndpointDelDia) => ({
    id: `${e.servicio}|${e.metodo}|${e.ruta}`,
    servicio: e.servicio,
    metodo: e.metodo,
    ruta: e.ruta,
    solicitudes: decimal(String(e.solicitudes)),
    errores: `${decimal(String(e.errores5xx))} 5xx · ${decimal(String(e.errores4xx))} 4xx`,
    p95: e.p95Ms ? conUnidad(e.p95Ms, 'MILISEGUNDOS') : 'Sin histograma',
    // Conteos y milisegundos, no dinero: caben sin pérdida en `number` y solo se usan para ordenar.
    orden: { solicitudes: Number(e.solicitudes), errores: Number(e.errores5xx) + Number(e.errores4xx), p95: e.p95Ms ? Number(e.p95Ms) : -1 },
  }))
}

/** Los endpoints tocados hoy, en vivo (no es del catálogo de CU-98). */
export function recursoEndpointsDelDia() {
  const gateway = inject(GATEWAY)
  return httpResource<SalidaEndpointsDelDia>(() => ({ url: `${gateway}/indicadores/operacion/endpoints`, params: { limite: 50 } }))
}
