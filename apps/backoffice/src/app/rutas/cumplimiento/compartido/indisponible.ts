import { ChangeDetectionStrategy, Component, input } from '@angular/core'
import { RouterLink } from '@angular/router'
import { BandaDeProposito } from '@aportaya/ui/banda-de-proposito/banda-de-proposito'
import { EstadoVacio } from '@aportaya/ui/estado-vacio/estado-vacio'

/**
 * Indisponibilidad honesta (plan B4): cuando `cumplimiento.yaml` no publica la operación que
 * una pantalla necesita, se dice eso —y adónde ir— en vez de dibujar datos de ejemplo que
 * se leen como reales.
 */
@Component({
  selector: 'ap-indisponible',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BandaDeProposito, EstadoVacio, RouterLink],
  template: `
    <ap-banda-de-proposito [texto]="proposito()" />
    <section class="pantalla">
      <h1>{{ titulo() }}</h1>
      <ap-estado-vacio titulo="Esta consulta todavía no está disponible." [explicacion]="motivo()" />
      @if (destino()) { <a [routerLink]="destino()">{{ textoDestino() }}</a> }
    </section>
  `,
  styles: `
    .pantalla { padding: var(--s5); max-width: 40rem; display: flex; flex-direction: column; gap: var(--s4); }
    a { color: var(--text); min-height: var(--area-tactil); display: inline-flex; align-items: center; }
    a:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
  `,
})
export class Indisponible {
  readonly titulo = input.required<string>()
  readonly proposito = input.required<string>()
  readonly motivo = input.required<string>()
  readonly destino = input<string>()
  readonly textoDestino = input('')
}
