import { Injectable, inject, signal } from '@angular/core'
import { NavigationStart, Router } from '@angular/router'

/**
 * El texto de «Notas de la vista» que el shell muestra plegado arriba de cada pantalla (como el
 * explicador del portal de Atlas). Cada pantalla lo fija al montarse con su propósito; al navegar
 * se borra, así una pantalla sin nota no hereda la de la anterior.
 *
 * Reemplaza a `ap-banda-de-proposito`: la misma frase, pero plegada en vez de pintar de color la
 * franja más ancha de la pantalla en todas las vistas.
 */
@Injectable({ providedIn: 'root' })
export class ExplicadorDeVista {
  private readonly router = inject(Router)
  readonly texto = signal<string | null>(null)

  constructor() {
    this.router.events.subscribe((e) => {
      if (e instanceof NavigationStart) this.texto.set(null)
    })
  }

  fijar(texto: string): void {
    this.texto.set(texto)
  }
}
