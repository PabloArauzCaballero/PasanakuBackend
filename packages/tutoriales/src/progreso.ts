import { Injectable, computed, inject, signal } from '@angular/core'
import type { Observable } from 'rxjs'
import { ALMACEN_DE_PROGRESO } from './almacen'
import type { ProgresoDeTutorial } from './tipos'

/**
 * **La única copia viva del avance.** El motor escribe por acá y el centro de tutoriales
 * lee de acá, así una tarjeta se actualiza sola al terminar un recorrido sin que nadie
 * recargue nada.
 *
 * El almacén es el puerto (local hoy, remoto mañana); esta clase es la caché en
 * señales que evita ir a buscarlo en cada pintada.
 */
@Injectable({ providedIn: 'root' })
export class ProgresoDeTutoriales {
  private readonly almacen = inject(ALMACEN_DE_PROGRESO)
  private readonly filas = signal<readonly ProgresoDeTutorial[]>([])
  private readonly cargado = signal(false)

  readonly mapa = computed<ReadonlyMap<string, ProgresoDeTutorial>>(() => new Map(this.filas().map((p) => [p.tutorialId, p])))
  readonly listo = this.cargado.asReadonly()

  /** Se llama al entrar al centro de ayuda y al montar el shell. Es idempotente. */
  cargar(): void {
    this.almacen.leer().subscribe({
      next: (filas) => {
        this.filas.set(filas)
        this.cargado.set(true)
      },
      // Sin almacén disponible el avance arranca vacío: se pierde el historial, no la
      // posibilidad de hacer el tutorial. Quien mira lo ve como «pendiente».
      error: () => this.cargado.set(true),
    })
  }

  de(tutorialId: string): ProgresoDeTutorial | undefined {
    return this.mapa().get(tutorialId)
  }

  guardar(progreso: ProgresoDeTutorial): Observable<ProgresoDeTutorial> {
    this.filas.update((previas) => [...previas.filter((p) => p.tutorialId !== progreso.tutorialId), progreso])
    return this.almacen.guardar(progreso)
  }

  /** Reiniciar borra la fila: el tutorial vuelve a estar como el primer día. */
  reiniciar(tutorialId: string): Observable<void> {
    this.filas.update((previas) => previas.filter((p) => p.tutorialId !== tutorialId))
    return this.almacen.reiniciar(tutorialId)
  }
}
