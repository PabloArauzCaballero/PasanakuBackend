import { InjectionToken } from '@angular/core'
import type { Observable } from 'rxjs'
import type { ProgresoDeTutorial } from './tipos'

/**
 * **El puerto del progreso.** El motor no sabe si esto termina en `localStorage` o en
 * una tabla del servidor: pide leer, guardar y reiniciar, y alguien cumple.
 *
 * Hoy el adaptador por omisión es local (`almacen-local.ts`) porque el backend todavía
 * no tiene dónde guardar esto; el remoto (`almacen-remoto.ts`) ya está escrito contra
 * el contrato documentado en `docs/Frontend/Motor de tutoriales.md` §11 y se enciende
 * cambiando un `provide` en `app.config.ts`. Ningún componente cambia.
 */
export interface AlmacenDeProgreso {
  /** Todo el progreso de quien tiene la sesión abierta. */
  leer(): Observable<readonly ProgresoDeTutorial[]>
  /** Guarda una fila entera. Es idempotente: la misma fila dos veces deja lo mismo. */
  guardar(progreso: ProgresoDeTutorial): Observable<ProgresoDeTutorial>
  /** Borra el progreso de UN tutorial. Reiniciar es empezar de cero, no «volver al paso 1». */
  reiniciar(tutorialId: string): Observable<void>
}

export const ALMACEN_DE_PROGRESO = new InjectionToken<AlmacenDeProgreso>('AlmacenDeProgreso')
