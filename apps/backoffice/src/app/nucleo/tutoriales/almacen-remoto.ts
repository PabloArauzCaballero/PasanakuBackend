import { HttpClient, HttpContext } from '@angular/common/http'
import { Injectable, inject } from '@angular/core'
import { map, type Observable } from 'rxjs'
import { GATEWAY } from '../gateway'
import { CLAVE_IDEMPOTENCIA, claveDeIdempotencia } from '../idempotencia.interceptor'
import type { AlmacenDeProgreso } from '@aportaya/tutoriales/almacen'
import type { ProgresoDeTutorial } from '@aportaya/tutoriales/tipos'

/**
 * **El progreso en el servidor**, contra el contrato descrito en
 * `docs/Frontend/Motor de tutoriales.md` §11 (`GET`, `PUT` y `DELETE` sobre
 * `/identidad/tutoriales/progreso`).
 *
 * Está escrito, tipado y probado, pero **no está enchufado**: el servicio `identidad`
 * todavía no expone esas operaciones, así que `app.config.ts` provee `AlmacenLocal`.
 * El día que el contrato exista y se regenere `clientes/angular/identidad`, esto se
 * enciende cambiando UN `provide` — y los tipos de acá se reemplazan por los generados,
 * que es la regla del proyecto (invariante 2: los tipos salen del contrato).
 *
 * El servidor decide de quién es el progreso por el token; el cliente **nunca** manda
 * un identificador de usuario, justamente para que nadie pueda escribir el avance de
 * otro cambiando un campo del cuerpo.
 */
@Injectable()
export class AlmacenRemoto implements AlmacenDeProgreso {
  private readonly http = inject(HttpClient)
  private readonly gateway = inject(GATEWAY)
  private get base(): string {
    return `${this.gateway}/identidad/tutoriales/progreso`
  }

  leer(): Observable<readonly ProgresoDeTutorial[]> {
    return this.http.get<ProgresoDeTutorial[]>(this.base)
  }

  /**
   * `PUT` sobre el tutorial, no `POST` a una colección: guardar el mismo paso dos veces
   * tiene que dejar lo mismo (§11, operaciones idempotentes). La clave de idempotencia
   * cubre el reintento de red; el `PUT` cubre el doble clic.
   */
  guardar(progreso: ProgresoDeTutorial): Observable<ProgresoDeTutorial> {
    const contexto = new HttpContext().set(CLAVE_IDEMPOTENCIA, claveDeIdempotencia())
    return this.http.put<ProgresoDeTutorial>(`${this.base}/${progreso.tutorialId}`, progreso, { context: contexto })
  }

  reiniciar(tutorialId: string): Observable<void> {
    return this.http.delete<unknown>(`${this.base}/${tutorialId}`).pipe(map(() => undefined))
  }
}
