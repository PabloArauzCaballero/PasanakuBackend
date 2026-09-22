import { Injectable, InjectionToken, signal } from '@angular/core'

/** Lo que vale la pena saber de un recorrido, para mejorarlo después. */
export type EventoDeTutorial =
  | { readonly tipo: 'inicio'; readonly tutorialId: string }
  | { readonly tipo: 'paso'; readonly tutorialId: string; readonly pasoId: string; readonly indice: number }
  | { readonly tipo: 'fin'; readonly tutorialId: string }
  | { readonly tipo: 'omitido'; readonly tutorialId: string; readonly pasoId: string | null }
  | { readonly tipo: 'problema'; readonly tutorialId: string; readonly pasoId: string; readonly detalle: string }

/**
 * **El puerto de analítica.** El motor anota qué pasó; quién escucha es otro asunto.
 * Se declara como puerto desde el primer día para que el día que haya un destino real
 * (un endpoint de producto, un panel) no haya que tocar el motor.
 */
export interface BitacoraDeTutoriales {
  anotar(evento: EventoDeTutorial): void
}

export const BITACORA_DE_TUTORIALES = new InjectionToken<BitacoraDeTutoriales>('BitacoraDeTutoriales')

/** Cuántos eventos se recuerdan. Alcanza para entender el recorrido de una sesión. */
const CUPO = 50

/**
 * El adaptador por omisión: guarda los últimos eventos **en memoria** y los ofrece al
 * diagnóstico del centro de ayuda. No sale a la red y no escribe en consola —`console.*`
 * es un fallo de lint, y además nadie lee una consola de producción.
 */
@Injectable()
export class BitacoraEnMemoria implements BitacoraDeTutoriales {
  private readonly eventos = signal<readonly EventoDeTutorial[]>([])
  readonly ultimos = this.eventos.asReadonly()

  anotar(evento: EventoDeTutorial): void {
    this.eventos.update((previos) => [...previos, evento].slice(-CUPO))
  }
}
