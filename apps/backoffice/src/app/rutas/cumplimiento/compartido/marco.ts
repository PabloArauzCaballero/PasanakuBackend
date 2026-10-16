import { ChangeDetectionStrategy, Component, input } from '@angular/core'
import { BandaDeProposito } from '@aportaya/ui/banda-de-proposito/banda-de-proposito'
import { NavegacionDeCumplimiento } from './navegacion'

/** El marco común de las bandejas de cumplimiento: propósito, navegación de la sección, título y acciones. */
@Component({
  selector: 'ap-marco-de-cumplimiento',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BandaDeProposito, NavegacionDeCumplimiento],
  template: `
    <ap-banda-de-proposito [texto]="proposito()" />
    <section class="pantalla">
      <ap-navegacion-de-cumplimiento />
      <header>
        <h1>{{ titulo() }}</h1>
        <div class="acciones"><ng-content select="[acciones]" /></div>
      </header>
      <ng-content />
    </section>
  `,
  styles: `
    .pantalla { padding: var(--s5); max-width: 72rem; display: grid; gap: var(--s4); min-width: 0; }
    header { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: var(--s3); }
    h1 { margin: 0; }
    .acciones { display: flex; flex-wrap: wrap; gap: var(--s2); }
  `,
})
export class MarcoDeCumplimiento {
  readonly titulo = input.required<string>()
  readonly proposito = input.required<string>()
}
