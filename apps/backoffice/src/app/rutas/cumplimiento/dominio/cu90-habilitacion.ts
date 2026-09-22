import { HttpClient, httpResource, HttpContext } from '@angular/common/http'
import { inject, Signal } from '@angular/core'
import type { Observable } from 'rxjs'
import type { Habilitacion } from 'clientes/angular/organizador'
import { GATEWAY } from '../../../nucleo/gateway'
import { CLAVE_IDEMPOTENCIA, claveDeIdempotencia } from '../../../nucleo/idempotencia.interceptor'

/**
 * CU-90 · Postular a organizador y habilitarse. Contrato real: `GET
 * /organizadores/{organizadorId}/habilitacion` (`consultarHabilitacion`, sin ruta HTTP
 * propia con operationId salvo la de consulta — habilitar/aprobar son POST en la misma
 * familia). Un archivo por caso de uso, sobre el tipo GENERADO (invariante 2).
 */
export function habilitacionDe(organizadorId: Signal<string>) {
  const gateway = inject(GATEWAY)
  return httpResource<Habilitacion>(() => `${gateway}/organizadores/${organizadorId()}/habilitacion`)
}

/**
 * No habilitado NO es "vacío": sigue siendo información accionable (el botón de
 * habilitar). `EstadoDePantalla` solo trata como vacío la ausencia real de dato, que
 * esta consulta nunca devuelve.
 */
export const habilitacionVacia = (): boolean => false

/**
 * `POST .../habilitacion` (`habilitarOrganizador`, CU-90). Clave de idempotencia
 * nueva por intento (invariante 7); el HTTP vive acá, nunca en la pantalla
 * (`scripts/verificar_frontend.py`: "sin red en vista"). Se llama en un campo de la
 * clase (contexto de inyección) y devuelve la función que de verdad dispara el POST.
 */
export function crearHabilitar(): (organizadorId: string) => Observable<Habilitacion> {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  return (organizadorId: string) => {
    const contexto = new HttpContext().set(CLAVE_IDEMPOTENCIA, claveDeIdempotencia())
    return http.post<Habilitacion>(`${gateway}/organizadores/${organizadorId}/habilitacion`, {}, { context: contexto })
  }
}
