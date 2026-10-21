import { Injectable, signal } from '@angular/core'
import datosFlujos from './flujos.json'
import type { FlujoInfo } from './tipos'

@Injectable({ providedIn: 'root' })
export class ServicioFlujos {
  private readonly todos = signal<readonly FlujoInfo[]>(datosFlujos as FlujoInfo[])
  readonly flujos = this.todos.asReadonly()

  buscarPorId(id: string): FlujoInfo | undefined {
    return this.todos().find((f) => f.id === id)
  }
}
