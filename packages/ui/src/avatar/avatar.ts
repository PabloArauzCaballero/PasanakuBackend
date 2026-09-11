import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core'

/** Iniciales sobre fondo de marca. Una imagen opcional, siempre con el nombre como alternativa. */
@Component({
  selector: 'ap-avatar',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '[class]': '"t-" + tamano()', '[attr.aria-label]': 'nombre()', role: 'img' },
  template: `@if (imagen()) { <img [src]="imagen()" alt="" /> } @else { <span aria-hidden="true">{{ iniciales() }}</span> }`,
  styles: `
    :host { display: inline-flex; align-items: center; justify-content: center; flex: none; width: var(--tamano); height: var(--tamano); border-radius: var(--r-pill); background: var(--brand-bg); color: var(--brand-ink); font-family: var(--font-d); font-weight: 600; overflow: hidden; }
    :host(.t-sm) { --tamano: var(--s5); font-size: .6em; }
    :host(.t-md) { --tamano: var(--s6); font-size: .8em; }
    :host(.t-lg) { --tamano: calc(var(--s6) + var(--s2)); font-size: .95em; }
    :host(.t-xl) { --tamano: calc(var(--s7) + var(--s2)); font-size: 1.2em; }
    img { width: 100%; height: 100%; object-fit: cover; }
  `,
})
export class Avatar {
  readonly nombre = input.required<string>()
  readonly imagen = input<string>()
  readonly tamano = input<'sm' | 'md' | 'lg' | 'xl'>('md')
  readonly iniciales = computed(() => inicialesDe(this.nombre()))
}

/** «María Quispe» → «MQ»; un solo nombre → sus dos primeras letras. */
export function inicialesDe(nombre: string): string {
  const partes = nombre.trim().split(/\s+/).filter(Boolean)
  if (partes.length === 0) return '?'
  if (partes.length === 1) return partes[0]!.slice(0, 2).toUpperCase()
  return (partes[0]![0]! + partes[partes.length - 1]![0]!).toUpperCase()
}
