import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core'
import { Icono, NombreDeIcono } from '../icono/icono'
import { Monto } from '../monto/monto'
import { coloresDe, Tono } from '../tono/tono'

/**
 * Un número con nombre, como la tarjeta de métrica del portal de Atlas: ícono en loseta con
 * tono, rótulo en mayúsculas, cifra tabular y una pista debajo. **El tono no es estética**: pinta
 * la cifra según lo que significa, así un contador de pendientes no se lee igual que un total.
 * Si es dinero va por `ap-monto`; la variación dice **qué pasó** («12 más que ayer»), no una flecha.
 */
@Component({
  selector: 'ap-tarjeta-kpi',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Icono, Monto],
  host: { role: 'group', '[attr.aria-label]': 'etiqueta()', '[style.--fondo]': 'colores().fondo', '[style.--frente]': 'colores().frente', '[class.con-tono]': 'tono() !== "neutro"' },
  template: `
    @if (icono()) { <span class="loseta" aria-hidden="true"><ap-icono [nombre]="icono()!" tamano="s4" /></span> }
    <div class="textos">
      <p class="rotulo">{{ etiqueta() }}</p>
      <p class="valor">
        @if (moneda()) { <ap-monto [monto]="valor()" [moneda]="moneda()!" [etiqueta]="etiqueta()" /> } @else { {{ valor() }} }
      </p>
      @if (pista()) { <p class="pista">{{ pista() }}</p> }
      @if (variacion()) { <p class="variacion">{{ variacion() }}</p> }
    </div>
  `,
  styles: `
    :host { position: relative; display: flex; gap: var(--s3); min-width: 0; overflow: hidden; padding: var(--s4); border: var(--borde-fino) solid var(--border); border-radius: var(--r-lg); background: var(--surface); box-shadow: var(--sombra-1); transition: transform var(--mov-micro) var(--curva-llega), box-shadow var(--mov-micro) ease-out, border-color var(--mov-micro) ease-out; }
    :host(:hover) { transform: translateY(calc(var(--borde-desfase) * -1)); box-shadow: var(--sombra-2); border-color: var(--borde-control); }
    .loseta { display: inline-flex; flex: none; align-items: center; justify-content: center; width: calc(var(--s6) + var(--s1)); height: calc(var(--s6) + var(--s1)); border-radius: var(--r-md); background: var(--fondo); color: var(--frente); transition: transform var(--mov-micro) var(--curva-llega); }
    :host(:hover) .loseta { transform: scale(1.08); }
    .textos { min-width: 0; }
    p { margin: 0; }
    .rotulo { font: var(--t-etiqueta); letter-spacing: var(--t-etiqueta-track); text-transform: uppercase; color: var(--text-3); }
    .valor { margin-top: var(--s1); font: var(--t-cifra); letter-spacing: var(--t-cifra-track); color: var(--text); font-variant-numeric: tabular-nums; }
    :host(.con-tono) .valor { color: var(--frente); }
    .pista, .variacion { margin-top: var(--s1); font: var(--t-ayuda); color: var(--text-3); }
    @media (prefers-reduced-motion: reduce) { :host(:hover), :host(:hover) .loseta { transform: none; } }
  `,
})
export class TarjetaKPI {
  readonly etiqueta = input.required<string>()
  readonly valor = input.required<string>()
  readonly moneda = input<string>()
  readonly variacion = input<string>()
  readonly pista = input<string>()
  readonly icono = input<NombreDeIcono>()
  readonly tono = input<Tono>('neutro')
  readonly colores = computed(() => coloresDe(this.tono()))
}
