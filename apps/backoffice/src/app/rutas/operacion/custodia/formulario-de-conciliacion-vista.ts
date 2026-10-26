import { ChangeDetectionStrategy, Component, input, model, output } from '@angular/core'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { Boton } from '@aportaya/ui/boton/boton'
import { Campo } from '@aportaya/ui/campo/campo'
import { GrupoRadio, type OpcionDeRadio } from '@aportaya/ui/grupo-radio/grupo-radio'
import { textosCustodia } from './textos'

/**
 * Presentacional puro (Dumb): inputs y controles del formulario de conciliación.
 * Solo maneja bindings de entrada/salida y eventos hacia el smart container.
 */
@Component({
  selector: 'ap-formulario-de-conciliacion-vista',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Alerta, Boton, Campo, GrupoRadio],
  template: `
    @if (aviso()) {
      <ap-alerta [tono]="tonoAviso()">{{ aviso() }}</ap-alerta>
    }
    <form (submit)="$event.preventDefault(); solicitarRevision.emit()" novalidate>
      <ap-campo
        class="ancho"
        [etiqueta]="t.cuenta"
        [ayuda]="t.cuentaAyuda"
        [error]="errores()['cuenta']"
        [deshabilitado]="deshabilitado()"
        [(valor)]="cuenta"
      />
      <ap-campo
        tipo="date"
        [etiqueta]="t.fecha"
        [error]="errores()['fecha']"
        [deshabilitado]="deshabilitado()"
        [(valor)]="fecha"
      />
      <fieldset class="bloque" [disabled]="deshabilitado()">
        <ap-grupo-radio [etiqueta]="t.moneda" [opciones]="opcionesMoneda()" [(elegido)]="moneda" />
      </fieldset>
      @if (errores()['moneda']) {
        <p class="error-campo ancho" role="alert">{{ errores()['moneda'] }}</p>
      }
      <ap-campo
        modoDeEntrada="decimal"
        marcador="0,00"
        [prefijo]="moneda() ?? ''"
        [etiqueta]="t.saldoCustodia"
        [error]="errores()['custodia']"
        [deshabilitado]="deshabilitado()"
        [(valor)]="custodia"
      />
      <ap-campo
        modoDeEntrada="decimal"
        marcador="0,00"
        [prefijo]="moneda() ?? ''"
        [etiqueta]="t.saldoEnTransito"
        [error]="errores()['transito']"
        [deshabilitado]="deshabilitado()"
        [(valor)]="transito"
      />
      <div class="ancho">
        <ap-boton variante="primario" tipo="submit" icono="balanza" [deshabilitado]="deshabilitado()">
          {{ t.revisar }}
        </ap-boton>
      </div>
    </form>
  `,
  styles: `
    :host { display: grid; gap: var(--s3); }
    form { display: grid; gap: var(--s3); }
    /* A lo ancho de la tarjeta: dos columnas desde tablet, sin una columna vacía al lado. */
    @media (min-width: 48rem) { form { grid-template-columns: 1fr 1fr; gap: var(--s4); } .ancho { grid-column: 1 / -1; } }
    .bloque { border: 0; margin: 0; padding: 0; min-width: 0; }
    .error-campo { margin: 0; color: var(--err-texto); font-size: .875rem; }
  `,
})
export class FormularioDeConciliacionVista {
  readonly cuenta = model<string>('')
  readonly fecha = model<string>('')
  readonly moneda = model<string | null>(null)
  readonly custodia = model<string>('')
  readonly transito = model<string>('')

  readonly errores = input<Record<string, string>>({})
  readonly deshabilitado = input<boolean>(false)
  readonly aviso = input<string>('')
  readonly tonoAviso = input<'aviso' | 'error'>('error')
  readonly opcionesMoneda = input<OpcionDeRadio[]>([
    { valor: 'BOB', texto: 'BOB' },
    { valor: 'USD', texto: 'USD' },
  ])

  readonly solicitarRevision = output<void>()

  protected readonly t = textosCustodia.formulario
}
