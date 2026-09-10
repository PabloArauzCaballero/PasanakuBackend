import { ChangeDetectionStrategy, Component, input, output } from '@angular/core'
import { Avatar } from '../avatar/avatar'
import { Boton } from '../boton/boton'
import { ChipEstado } from '../chip-estado/chip-estado'
import { Monto } from '../monto/monto'

/**
 * Alguien ofrece cambiar de turno. Se ve quién, qué turno da y cuál pide, y **cuánto
 * cuesta** (por `ap-monto`). El riesgo lo calcula el servidor y llega como palabra.
 */
@Component({
  selector: 'ap-tarjeta-de-oferta',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Avatar, Boton, ChipEstado, Monto],
  host: { role: 'article', '[attr.aria-label]': '"Oferta de " + nombre()' },
  template: `
    <header>
      <ap-avatar [nombre]="nombre()" />
      <p class="nombre">{{ nombre() }}</p>
      @if (riesgo()) { <ap-chip-estado [tono]="riesgo() === 'alto' ? 'error' : riesgo() === 'medio' ? 'aviso' : 'ok'">Riesgo {{ riesgo() }}</ap-chip-estado> }
    </header>
    <p class="cambio">Da el turno <strong>{{ turnoQueDa() }}</strong> y pide el <strong>{{ turnoQuePide() }}</strong></p>
    <p class="costo">Por <ap-monto [monto]="costo()" [moneda]="moneda()" etiqueta="Costo del cambio" /></p>
    @if (aceptable()) { <ap-boton variante="secundario" [ancho]="true" (pulsado)="aceptar.emit()">Aceptar el cambio</ap-boton> }
  `,
  styles: `
    :host { display: block; padding: var(--s4); border: var(--borde-fino) solid var(--border); border-radius: var(--r-lg); background: var(--surface); }
    header { display: flex; align-items: center; gap: var(--s2); margin-bottom: var(--s2); }
    p { margin: 0; }
    .nombre { flex: 1; font-weight: 600; color: var(--text); }
    .cambio { color: var(--text-2); }
    strong { color: var(--text); }
    .costo { margin: var(--s2) 0 var(--s3); color: var(--text); font-size: 1.1em; }
  `,
})
export class TarjetaDeOferta {
  readonly nombre = input.required<string>()
  readonly turnoQueDa = input.required<number>()
  readonly turnoQuePide = input.required<number>()
  readonly costo = input.required<string>()
  readonly moneda = input.required<string>()
  readonly riesgo = input<'bajo' | 'medio' | 'alto'>()
  readonly aceptable = input(true)
  readonly aceptar = output<void>()
}
