import { ChangeDetectionStrategy, Component, input, output } from '@angular/core'
import { Boton } from '../boton/boton'
import { Icono } from '../icono/icono'

/**
 * Un error accionable, como en el portal de Atlas: ícono en loseta, título, qué pasó, el código
 * de seguimiento para soporte y «Reintentar». Nunca el error crudo del servidor. Sin `reintentar`
 * no hay botón (un 404 no se reintenta).
 */
@Component({
  selector: 'ap-estado-de-error',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Boton, Icono],
  host: { role: 'alert' },
  template: `
    <span class="loseta" aria-hidden="true"><ap-icono nombre="advertencia" tamano="s5" /></span>
    <div class="textos">
      <p class="titulo">{{ titulo() }}</p>
      @if (mensaje()) { <p class="mensaje">{{ mensaje() }}</p> }
      @if (trazaId()) { <p class="traza">Código de seguimiento: <code>{{ trazaId() }}</code></p> }
      @if (conReintento()) { <ap-boton variante="secundario" tamano="sm" (pulsado)="reintentar.emit()">{{ textoReintentar() }}</ap-boton> }
    </div>
  `,
  styles: `
    :host { display: flex; gap: var(--s3); padding: var(--s4); border: var(--borde-fino) solid color-mix(in srgb, var(--err) 35%, transparent); border-radius: var(--r-lg); background: var(--err-bg); color: var(--err-texto); }
    .loseta { display: inline-flex; flex: none; align-items: center; justify-content: center; width: calc(var(--s6) + var(--s2)); height: calc(var(--s6) + var(--s2)); border-radius: var(--r-md); background: color-mix(in srgb, var(--err) 18%, transparent); }
    .textos { display: grid; gap: var(--s2); min-width: 0; justify-items: start; }
    p { margin: 0; overflow-wrap: anywhere; }
    .titulo { font: var(--t-cuerpo-fuerte); }
    .mensaje { font: var(--t-cuerpo-chico); }
    .traza { font: var(--t-ayuda); } code { font-family: var(--mono); }
    ap-boton { margin-top: var(--s1); }
  `,
})
export class EstadoDeError {
  readonly titulo = input('No se pudo cargar la información.')
  readonly mensaje = input<string>()
  readonly trazaId = input<string>()
  readonly conReintento = input(true)
  readonly textoReintentar = input('Volver a intentar')
  readonly reintentar = output<void>()
}
