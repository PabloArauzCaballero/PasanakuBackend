import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core'
import { Avatar } from '../avatar/avatar'

export type Turno = { numero: number; nombre: string; estado: 'entregado' | 'actual' | 'pendiente'; esMio?: boolean }

/** El orden del grupo en una tira: entregados, el actual con relleno de marca, y «vos» marcado. */
@Component({
  selector: 'ap-riel-de-turnos',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Avatar],
  host: { role: 'list', '[attr.aria-label]': 'lectura()' },
  template: `
    @for (t of turnos(); track t.numero) {
      <div role="listitem" [class]="'turno ' + t.estado" [class.mio]="t.esMio" [attr.aria-label]="t.numero + ': ' + t.nombre + (t.esMio ? ' (vos)' : '') + ', ' + NOMBRE[t.estado]">
        <ap-avatar [nombre]="t.nombre" tamano="lg" />
        <span class="numero" aria-hidden="true">{{ t.numero }}</span>
        <span class="nombre" aria-hidden="true">{{ t.esMio ? 'Vos' : primerNombre(t.nombre) }}</span>
      </div>
    }
  `,
  styles: `
    :host { display: flex; gap: var(--s3); overflow-x: auto; padding: var(--s2) 0; scroll-snap-type: x proximity; }
    .turno { display: flex; flex-direction: column; align-items: center; gap: var(--s1); flex: none; width: calc(var(--s7) + var(--s4)); scroll-snap-align: start; color: var(--text-2); font-size: .8em; }
    .turno ap-avatar { box-shadow: 0 0 0 var(--borde-desfase) var(--border); }
    .entregado ap-avatar { box-shadow: 0 0 0 var(--borde-desfase) var(--ok); }
    .actual ap-avatar { box-shadow: 0 0 0 var(--borde-foco) var(--brand); }
    .actual .nombre { color: var(--brand-texto); font-weight: 600; }
    .mio .nombre { font-weight: 700; color: var(--text); }
    .numero { color: var(--text-3); font-family: var(--font-d); }
    .nombre { max-width: 100%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  `,
})
export class RielDeTurnos {
  readonly NOMBRE = { entregado: 'ya recibió', actual: 'turno actual', pendiente: 'todavía no' }
  readonly turnos = input.required<Turno[]>()
  readonly lectura = computed(() => {
    const mio = this.turnos().find((t) => t.esMio)
    const actual = this.turnos().find((t) => t.estado === 'actual')
    return mio && actual ? `Turnos: vas ${mio.numero} de ${this.turnos().length}, ahora va el ${actual.numero}` : `Turnos del grupo, ${this.turnos().length}`
  })
  primerNombre(n: string): string {
    return n.split(' ')[0] ?? n
  }
}
