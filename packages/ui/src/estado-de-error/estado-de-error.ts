import { ChangeDetectionStrategy, Component, input, output } from '@angular/core'
import { Boton } from '../boton/boton'
import { Icono } from '../icono/icono'

/**
 * Un error accionable, como en el portal de Atlas: ícono en loseta, título, qué pasó, el código
 * de seguimiento para soporte y «Reintentar». Nunca el error crudo del servidor. Sin `reintentar`
 * no hay botón (un 404 no se reintenta).
 *
 * Sin rojo (2026-10-27): una lectura que falló no es una alarma de la persona que opera. Tarjeta neutra con el ícono en
 * ámbar y «Volver a intentar» con su ícono; el rojo queda para lo destructivo y lo vencido.
 *
 * Suelto es su propia alerta. Dentro de un contenedor que ya es la región de alerta (`ap-estado-de-pantalla`) se
 * monta con `[anunciar]="false"`: dos `role="alert"` anidados se anuncian dos veces.
 */
@Component({
  selector: 'ap-estado-de-error',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Boton, Icono],
  host: { '[attr.role]': "anunciar() ? 'alert' : null" },
  template: `
    <span class="loseta" aria-hidden="true"><ap-icono nombre="advertencia" tamano="s5" /></span>
    <div class="textos">
      <p class="titulo">{{ titulo() }}</p>
      @if (mensaje()) { <p class="mensaje">{{ mensaje() }}</p> }
      @if (trazaId()) { <p class="traza">Código de seguimiento: <code>{{ trazaId() }}</code></p> }
      @if (conReintento()) { <ap-boton variante="secundario" tamano="sm" icono="recarga" (pulsado)="reintentar.emit()">{{ textoReintentar() }}</ap-boton> }
    </div>
  `,
  styles: `
    :host { display: flex; gap: var(--s3); padding: var(--s4); border: var(--borde-fino) solid var(--border); border-left: var(--s1) solid var(--warn); border-radius: var(--r-lg); background: var(--surface); color: var(--text); }
    .loseta { display: inline-flex; flex: none; align-items: center; justify-content: center; width: calc(var(--s6) + var(--s2)); height: calc(var(--s6) + var(--s2)); border-radius: var(--r-md); background: var(--warn-bg); color: var(--aviso-texto); }
    .textos { display: grid; gap: var(--s2); min-width: 0; justify-items: start; }
    p { margin: 0; overflow-wrap: anywhere; }
    .titulo { font: var(--t-cuerpo-fuerte); }
    .mensaje { font: var(--t-cuerpo-chico); color: var(--text-2); }
    .traza { font: var(--t-ayuda); color: var(--text-3); } code { font-family: var(--mono); }
    ap-boton { margin-top: var(--s1); }
  `,
})
export class EstadoDeError {
  readonly titulo = input('No se pudo cargar la información.')
  readonly mensaje = input<string>()
  readonly trazaId = input<string>()
  readonly conReintento = input(true)
  readonly textoReintentar = input('Volver a intentar')
  readonly anunciar = input(true)
  readonly reintentar = output<void>()
}
