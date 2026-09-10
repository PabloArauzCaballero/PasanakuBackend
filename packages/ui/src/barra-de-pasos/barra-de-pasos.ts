import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core'

/**
 * «Paso 3 de 8: Documento». Progreso por segmentos, el actual con relleno de marca y
 * los hechos en ok. Es lo que hace soportable un alta de ocho pasos.
 */
@Component({
  selector: 'ap-barra-de-pasos',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { role: 'group', '[attr.aria-label]': 'lectura()' },
  template: `
    <p aria-hidden="true">Paso {{ actual() }} de {{ pasos().length }}<span class="nombre">: {{ nombre() }}</span></p>
    <ol aria-hidden="true">
      @for (p of pasos(); track p; let i = $index) { <li [class.hecho]="i + 1 < actual()" [class.actual]="i + 1 === actual()"></li> }
    </ol>
  `,
  styles: `
    :host { display: block; }
    p { margin: 0 0 var(--s2); font-weight: 600; color: var(--text-2); }
    .nombre { color: var(--text); }
    ol { display: flex; gap: var(--s1); margin: 0; padding: 0; list-style: none; }
    li { flex: 1; height: var(--s1); border-radius: var(--r-pill); background: var(--surface-2); }
    li.hecho { background: var(--ok); }
    li.actual { background: var(--brand); }
  `,
})
export class BarraDePasos {
  readonly pasos = input.required<string[]>()
  readonly actual = input.required<number>()
  readonly nombre = computed(() => this.pasos()[this.actual() - 1] ?? '')
  readonly lectura = computed(() => `Paso ${this.actual()} de ${this.pasos().length}: ${this.nombre()}`)
}
