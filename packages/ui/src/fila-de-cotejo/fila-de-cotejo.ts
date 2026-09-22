import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core'
import { ChipEstado } from '../chip-estado/chip-estado'

/** Lo que dijo la persona contra lo que dice el documento. Coincide o no, con palabra y color. */
@Component({
  selector: 'ap-fila-de-cotejo',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ChipEstado],
  host: { role: 'row' },
  template: `
    <span class="campo" role="cell">{{ campo() }}</span>
    <span role="cell"><small>Declarado</small>{{ declarado() }}</span>
    <span role="cell"><small>Documento</small>{{ documento() }}</span>
    <span role="cell"><ap-chip-estado [tono]="coincide() ? 'ok' : 'error'" [icono]="coincide() ? 'verificado' : 'alerta'">{{ coincide() ? 'Coincide' : 'No coincide' }}</ap-chip-estado></span>
  `,
  styles: `
    :host { display: grid; grid-template-columns: 1fr 1fr 1fr auto; gap: var(--s3); align-items: center; padding: var(--s2) 0; border-bottom: var(--borde-fino) solid var(--border); color: var(--text); }
    .campo { font-weight: 600; color: var(--text-2); }
    small { display: block; color: var(--text-3); font-size: .8em; }
    @media (max-width: 40em) { :host { grid-template-columns: 1fr 1fr; } }
  `,
})
export class FilaDeCotejo {
  readonly campo = input.required<string>()
  readonly declarado = input.required<string>()
  readonly documento = input.required<string>()
  readonly coincide = computed(() => normalizar(this.declarado()) === normalizar(this.documento()))
}

/** Sin acentos, sin espacios dobles, sin mayúsculas: «José  Pérez» = «jose perez». */
export function normalizar(texto: string): string {
  return texto.normalize('NFD').replace(/[̀-ͯ]/g, '').replace(/\s+/g, ' ').trim().toLowerCase()
}
