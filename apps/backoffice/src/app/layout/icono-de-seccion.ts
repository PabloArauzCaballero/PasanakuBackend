import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core'

/** Trazos de 24×24, uno por sección del menú. Mismo grosor y remate que `ap-icono`. */
const TRAZOS = {
  tablero: 'M4 4h7v7H4zM13 4h7v7h-7zM4 13h7v7H4zM13 13h7v7h-7z',
  operacion: 'M3 12h4l3-8 4 16 3-8h4',
  cumplimiento: 'M12 3l8 3v6c0 5-3.5 8-8 9-4.5-1-8-4-8-9V6zM9 12l2 2 4-4',
  admisiones: 'M15 19c0-3-2.7-5-6-5s-6 2-6 5M9 11a4 4 0 100-8 4 4 0 000 8zM19 8v6M16 11h6',
  sistemas: 'M4 4h16v6H4zM4 14h16v6H4zM8 7h.01M8 17h.01',
  contabilidad: 'M5 4h12a2 2 0 012 2v14H7a2 2 0 01-2-2zM5 18a2 2 0 012-2h12M9 8h6M9 11h4',
  publicidad: 'M4 10v4h3l6 4V6L7 10zM16 9a4 4 0 010 6M19 6a8 8 0 010 12',
  configuracion: 'M4 7h9M17 7h3M4 17h3M11 17h9M15 5v4M9 15v4',
  catalogo: 'M4 6c0-1.7 3.6-3 8-3s8 1.3 8 3-3.6 3-8 3-8-1.3-8-3zM4 6v12c0 1.7 3.6 3 8 3s8-1.3 8-3V6M4 12c0 1.7 3.6 3 8 3s8-1.3 8-3',
  ayuda: 'M12 21a9 9 0 100-18 9 9 0 000 18zM9.5 9.5a2.5 2.5 0 014.6 1.3c0 1.7-2.1 2-2.1 3.7M12 17.5h.01',
  salir: 'M15 4h3a2 2 0 012 2v12a2 2 0 01-2 2h-3M10 16l-4-4 4-4M6 12h10',
} as const

export type Seccion = keyof typeof TRAZOS

/** El ícono de una sección del menú. Siempre decorativo: el texto va al lado. */
@Component({
  selector: 'ap-icono-de-seccion',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { 'aria-hidden': 'true' },
  template: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path [attr.d]="trazo()" /></svg>`,
  styles: `
    :host { display: inline-flex; width: 1.25rem; height: 1.25rem; flex: none; }
    svg { width: 100%; height: 100%; }
  `,
})
export class IconoDeSeccion {
  readonly nombre = input.required<Seccion>()
  protected readonly trazo = computed(() => TRAZOS[this.nombre()])
}
