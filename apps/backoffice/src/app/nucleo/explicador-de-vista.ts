import { Injectable, computed, inject, signal } from '@angular/core'
import { NavigationEnd, Router } from '@angular/router'
import { toSignal } from '@angular/core/rxjs-interop'
import { filter, map, startWith } from 'rxjs'
import { resolverExplicacion, type ExplicacionResuelta } from './explicaciones'

/**
 * Servicio central del explicador de vistas de AportaYa.
 *
 * Resuelve automáticamente la explicación en dos planos (negocio y sistemas)
 * para cualquier pantalla del backoffice a partir de su URL, garantizando
 * exhaustividad y comprensión para el usuario común (patrón Atlas Admin Portal).
 *
 * Mantiene compatibilidad con `fijar()` para vistas que deseen adjuntar
 * una nota contextual complementaria sin duplicar la cabecera.
 */
@Injectable({ providedIn: 'root' })
export class ExplicadorDeVista {
  private readonly router = inject(Router)

  /** Nota manual fijada por una pantalla específica (opcional/retrocompatible). */
  readonly texto = signal<string | null>(null)

  /** URL activa normalizada tras redirecciones. */
  readonly urlActual = toSignal(
    this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd),
      map((e) => e.urlAfterRedirects),
      startWith(this.router.url),
    ),
    { initialValue: this.router.url },
  )

  /** Explicación de módulo y vista resuelta automáticamente por URL. */
  readonly explicacion = computed<ExplicacionResuelta | null>(() =>
    resolverExplicacion(this.urlActual()),
  )

  constructor() {
    this.router.events.pipe(filter((e) => e instanceof NavigationEnd)).subscribe(() => {
      this.texto.set(null)
    })
  }

  /** Fija una nota manual complementaria para la vista activa. */
  fijar(texto: string): void {
    this.texto.set(texto)
  }
}
