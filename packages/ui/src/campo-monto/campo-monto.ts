import { ChangeDetectionStrategy, Component, computed, input, model } from '@angular/core'
import { Campo } from '../campo/campo'

/**
 * Monto con prefijo «Bs», teclado decimal y coma como separador. Guarda lo que la
 * persona escribe; `aCadenaDelContrato` lo pasa a `"1240.00"` para el API, sin flotantes.
 */
@Component({
  selector: 'ap-campo-monto',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Campo],
  template: `<ap-campo [etiqueta]="etiqueta()" [(valor)]="valor" prefijo="Bs" modoDeEntrada="decimal" marcador="0,00" [ayuda]="ayuda()" [error]="error() ?? errorDeFormato()" />`,
})
export class CampoMonto {
  readonly etiqueta = input('Monto')
  readonly valor = model('')
  readonly ayuda = input<string>()
  readonly error = input<string>()
  readonly errorDeFormato = computed(() => (this.valor() && aCadenaDelContrato(this.valor()) === null ? 'Escribí un monto como 1.240,00' : undefined))
  readonly contrato = computed(() => aCadenaDelContrato(this.valor()))
}

/** «1.240,5» → «1240.50»; «abc» → null. Sin `parseFloat`: la plata no se redondea. */
export function aCadenaDelContrato(texto: string): string | null {
  const sinEspacios = texto.replace(/\s/g, '')
  // Miles con punto en grupos de tres (o sin puntos), decimales con coma: «1.240,5», «1240», «12,50».
  if (!/^(\d{1,3}(\.\d{3})+|\d+)(,\d{0,2})?$/.test(sinEspacios)) return null
  const [entero, dec = ''] = sinEspacios.replace(/\./g, '').split(',')
  return `${entero}.${dec.padEnd(2, '0')}`
}
