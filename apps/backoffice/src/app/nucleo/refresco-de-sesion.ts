import { HttpBackend, HttpClient } from '@angular/common/http'
import { inject, Injectable } from '@angular/core'
import { finalize, Observable, shareReplay } from 'rxjs'
import { GATEWAY } from './gateway'

export interface RespuestaRefresco {
  acceso: string
  permisos?: string[]
  rol?: string
}

/**
 * Un solo refresco en vuelo por vez: N suscriptores concurrentes a `refrescar()` comparten
 * el mismo `Observable` en vez de disparar un `POST /sesion/refrescar` cada uno (el defecto
 * que caracteriza `sesion.interceptor.spec.ts`). El `POST` sale por un `HttpClient` armado
 * sobre `HttpBackend`, sin pasar por la cadena de interceptores — ver
 * `entregables/decision-httpbackend.md`.
 *
 * A propósito NO toca `Sesion`: lo usan `sesionInterceptor` (H2, cierra sesión ante
 * cualquier fallo) y `AuthBootstrap` (H3, distingue `401` de un error de `identidad`) con
 * semánticas de fallo distintas. Cada consumidor decide qué hacer con el resultado.
 */
@Injectable({ providedIn: 'root' })
export class RefrescoDeSesion {
  private readonly http = new HttpClient(inject(HttpBackend))
  private readonly gateway = inject(GATEWAY)
  private enVuelo: Observable<RespuestaRefresco> | null = null

  refrescar(): Observable<RespuestaRefresco> {
    if (!this.enVuelo) this.enVuelo = this.crear()
    return this.enVuelo
  }

  private crear(): Observable<RespuestaRefresco> {
    return this.http.post<RespuestaRefresco>(`${this.gateway}/sesion/refrescar`, {}, { withCredentials: true }).pipe(
      // Se limpia ANTES del multicast: la próxima llamada, ya sea porque el refresco
      // anterior terminó bien o mal, arranca una ejecución nueva, no reutiliza el
      // `ReplaySubject` de `shareReplay`, que seguiría devolviendo el valor o el error viejo.
      finalize(() => {
        this.enVuelo = null
      }),
      shareReplay(1),
    )
  }
}
