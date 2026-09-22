import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core'

export type NombreDeIcono = keyof typeof TRAZOS

/** Trazos de 24×24 (stroke), uno por nombre. El ícono dice **qué pasó**, nunca si sube o baja. */
const TRAZOS = {
  aporte: 'M12 3v12m0 0-4-4m4 4 4-4M4 21h16',
  entrega: 'M3 8h18v11H3zM3 12h18M8 8V5h8v3',
  recarga: 'M4 12a8 8 0 1 0 2.3-5.7M4 4v5h5',
  retiro: 'M12 21V9m0 0-4 4m4-4 4 4M4 3h16',
  mora: 'M12 8v5m0 3h.01M12 3 2 20h20z',
  comision: 'M7 7h10M7 12h10M7 17h6',
  reloj: 'M12 7v5l3 2M12 21a9 9 0 1 1 0-18 9 9 0 0 1 0 18z',
  verificado: 'M9 12l2 2 4-4M12 21a9 9 0 1 1 0-18 9 9 0 0 1 0 18z',
  alerta: 'M12 8v5m0 3h.01M12 21a9 9 0 1 1 0-18 9 9 0 0 1 0 18z',
  cerrar: 'M6 6l12 12M18 6 6 18',
  buscar: 'M21 21l-4.3-4.3M17 11a6 6 0 1 1-12 0 6 6 0 0 1 12 0z',
  ojo: 'M2 12s4-7 10-7 10 7 10 7-4 7-10 7S2 12 2 12zm10 3a3 3 0 1 0 0-6 3 3 0 0 0 0 6z',
  ojoCerrado: 'M3 3l18 18M10 6a10 10 0 0 1 12 6 12 12 0 0 1-3 3.5M6.5 6.5A12 12 0 0 0 2 12s4 7 10 7a9 9 0 0 0 3.5-.7',
  copiar: 'M9 9h10v12H9zM5 15V3h10',
  qr: 'M4 4h6v6H4zM14 4h6v6h-6zM4 14h6v6H4zM14 14h2v2h-2zM18 14h2v2h-2zM14 18h2v2h-2zM18 18h2v2h-2z',
  chevronDerecha: 'M9 6l6 6-6 6',
  chevronIzquierda: 'M15 6l-6 6 6 6',
  chevronAbajo: 'M6 9l6 6 6-6',
  mas: 'M12 5v14M5 12h14',
  menos: 'M5 12h14',
  estrella: 'M12 3l2.8 5.9 6.4.8-4.7 4.4 1.2 6.4L12 17.4 6.3 20.5l1.2-6.4L2.8 9.7l6.4-.8z',
  campana: 'M6 16V11a6 6 0 0 1 12 0v5l2 2H4zM10 21h4',
  calendario: 'M4 6h16v14H4zM4 10h16M8 3v4M16 3v4',
  persona: 'M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8zM4 21a8 8 0 0 1 16 0',
  grupo: 'M9 11a3 3 0 1 0 0-6 3 3 0 0 0 0 6zM17 11a3 3 0 1 0 0-6M3 20a6 6 0 0 1 12 0M15 14a6 6 0 0 1 6 6',
  candado: 'M6 11h12v10H6zM8 11V7a4 4 0 0 1 8 0v4',
  filtro: 'M3 5h18l-7 8v6l-4 2v-8z',
  info: 'M12 11v5m0-8h.01M12 21a9 9 0 1 1 0-18 9 9 0 0 1 0 18z',
} as const

/**
 * Un ícono de trazo que hereda el color del texto. Es decorativo por omisión
 * (`aria-hidden`); con `etiqueta` pasa a ser una imagen con nombre.
 */
@Component({
  selector: 'ap-icono',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '[attr.role]': 'etiqueta() ? "img" : null',
    '[attr.aria-label]': 'etiqueta() ?? null',
    '[attr.aria-hidden]': 'etiqueta() ? null : "true"',
    '[style.--tamano]': 'tamanoCss()',
  },
  template: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path [attr.d]="trazo()" /></svg>`,
  styles: `
    :host { display: inline-flex; width: var(--tamano); height: var(--tamano); flex: none; vertical-align: middle; }
    svg { width: 100%; height: 100%; }
  `,
})
export class Icono {
  readonly nombre = input.required<NombreDeIcono>()
  readonly etiqueta = input<string>()
  /** `s4` (16), `s5` (24) o `s6` (32): escalas del sistema, nunca un número. */
  readonly tamano = input<'s4' | 'S5' | 's5' | 's6'>('s5')
  readonly trazo = computed(() => TRAZOS[this.nombre()])
  readonly tamanoCss = computed(() => `var(--${this.tamano().toLowerCase()})`)
}
