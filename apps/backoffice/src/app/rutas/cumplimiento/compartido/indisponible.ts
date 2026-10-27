import { ChangeDetectionStrategy, Component, effect, inject, input } from '@angular/core'
import { RouterLink } from '@angular/router'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { EstadoVacio } from '@aportaya/ui/estado-vacio/estado-vacio'
import { Icono } from '@aportaya/ui/icono/icono'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'

/**
 * Indisponibilidad honesta (plan B4): cuando `cumplimiento.yaml` no publica la operación que
 * una pantalla necesita, se dice eso —y adónde ir— en vez de dibujar datos de ejemplo que
 * se leen como reales.
 */
@Component({
  selector: 'ap-indisponible',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CabeceraDePagina, EstadoVacio, Icono, RouterLink],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina eyebrow="Cumplimiento" [titulo]="titulo()" [descripcion]="proposito()" icono="advertencia" />
      <div class="tarjeta">
      <ap-estado-vacio titulo="Esta consulta todavía no está disponible." [explicacion]="motivo()" />
      @if (destino()) { <a [routerLink]="destino()">{{ textoDestino() }}<ap-icono nombre="flechaDerecha" tamano="s4" /></a> }
      </div>
    </section>
  `,
  styles: `
    .pantalla { padding: var(--s5) var(--s4); display: flex; flex-direction: column; gap: var(--s4); min-width: 0; }
    .tarjeta { display: grid; justify-items: center; gap: var(--s3); padding: var(--s5); border: var(--borde-fino) solid var(--border); border-radius: var(--r-lg); background: var(--surface); box-shadow: var(--sombra-1); }
    a { display: inline-flex; align-items: center; gap: var(--s1); min-height: var(--area-tactil); color: var(--brand-texto); font-weight: 600; text-decoration: none; }
    a:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
  `,
})
export class Indisponible {
  readonly titulo = input.required<string>()
  readonly proposito = input.required<string>()
  readonly motivo = input.required<string>()
  readonly destino = input<string>()
  readonly textoDestino = input('')
  private readonly explicador = inject(ExplicadorDeVista)

  constructor() {
    effect(() => {
      this.explicador.fijar(this.proposito())
    })
  }
}
