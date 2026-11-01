import { HttpClient, httpResource } from '@angular/common/http'
import { inject, Signal } from '@angular/core'
import type { Observable } from 'rxjs'
import type { Habilitacion, SalidaHabilitacion } from 'clientes/angular/organizador/model/models'
import { GATEWAY } from '../../../nucleo/gateway'
import { contextoIdempotente } from '../../../nucleo/idempotencia.interceptor'

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
 * `POST .../habilitacion` (`habilitarOrganizador`, CU-90). Responde `SalidaHabilitacion`: el estado que quedó y, si no
 * se habilitó, lo que falta (`faltantes`). Clave de idempotencia
 * nueva por intento (invariante 7); el HTTP vive acá, nunca en la pantalla
 * (`scripts/verificar_frontend.py`: "sin red en vista"). Se llama en un campo de la
 * clase (contexto de inyección) y devuelve la función que de verdad dispara el POST.
 */
export function crearHabilitar(): (organizadorId: string) => Observable<SalidaHabilitacion> {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  return (organizadorId: string) => {
    const contexto = contextoIdempotente()
    return http.post<SalidaHabilitacion>(`${gateway}/organizadores/${organizadorId}/habilitacion`, {}, { context: contexto })
  }
}
