import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core'
import { Alerta } from '../alerta/alerta'
import { ChipEstado } from '../chip-estado/chip-estado'
import { Monto } from '../monto/monto'

/**
 * Las posiciones de una billetera ya separadas (las calcula `separarPosiciones` de
 * `@aportaya/dominio-cliente/posiciones`; este componente solo las pinta, sin sumar nada).
 * Forma estructural a propósito: la biblioteca de UI no depende del paquete de dominio.
 */
export type PosicionesParaVista = {
  readonly moneda: string
  readonly gastable: string | null
  readonly comprometidoEnPozos: string | null
  readonly disponible: string
  readonly retenido: string
  readonly totalDeLaCuenta: string
  readonly separado: boolean
  readonly advertencias: readonly string[]
}

type Perspectiva = 'titular' | 'soporte'

const TEXTOS = {
  titular: {
    gastable: 'Lo que podés gastar ahora',
    comprometido: 'Comprometido en pozos de grupos',
    retenido: 'Retenido por una operación en curso',
    ayudaGastable: 'Es lo único que podés retirar, transferir o invertir.',
    ayudaComprometido: 'Es plata tuya que ya está afectada a un aporte o a un pozo. Va al grupo, no se gasta.',
    ayudaRetenido: 'Apartado hasta que la operación termine. Si se cancela, vuelve a tu disponible.',
    sinSeparar: 'Todavía no podemos decirte cuánto de tu disponible está comprometido en pozos, así que no lo mostramos como gastable. Consultá de nuevo en un momento.',
  },
  soporte: {
    gastable: 'Gastable por el titular ahora',
    comprometido: 'Comprometido en pozos de grupos',
    retenido: 'Retenido por una operación en curso',
    ayudaGastable: 'Es lo único que el titular puede retirar, transferir o invertir.',
    ayudaComprometido: 'Parte del disponible afectada a aportes y pozos de grupos: no es gastable a título personal.',
    ayudaRetenido: 'Apartado por una operación en curso. No es gastable hasta que se libere.',
    sinSeparar: 'El libro todavía no informa cuánto del disponible está comprometido en pozos. Por eso no se muestra ninguna cifra como gastable: no se la puede afirmar sin ese dato.',
  },
} as const

const AVISO_EXCEDE = {
  titular: 'Lo comprometido figura por encima de tu disponible. Mostramos el tope y lo gastable en cero; si ves esto de nuevo, avisanos.',
  soporte: 'El libro informa más comprometido en pozos que disponible. Se muestra acotado al disponible y lo gastable en cero: revisar la conciliación de la cuenta.',
} as const

/**
 * Qué parte del dinero de una cuenta se puede gastar y cuál no. Dinero retenido, comprometido en un
 * pozo o invertido **no** se presenta como gastable personal: cada posición lleva su palabra
 * («Gastable» / «No gastable») además de su lugar, nunca solo color. Las tres suman el total.
 */
@Component({
  selector: 'ap-posiciones-de-saldo',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Alerta, ChipEstado, Monto],
  host: { role: 'group', '[attr.aria-label]': '"Posiciones de la cuenta"' },
  template: `
    @if (excede()) { <ap-alerta tono="aviso">{{ aviso() }}</ap-alerta> }
    @if (!p().separado) { <ap-alerta tono="aviso" titulo="Disponible sin separar">{{ t().sinSeparar }}</ap-alerta> }
    <dl>
      @if (p().separado) {
        <div class="fila fila--gastable">
          <dt>{{ t().gastable }}<small>{{ t().ayudaGastable }}</small></dt>
          <dd><ap-monto [monto]="p().gastable!" [moneda]="p().moneda" [etiqueta]="t().gastable" /> <ap-chip-estado tono="ok" icono="verificado">Gastable</ap-chip-estado></dd>
        </div>
        <div class="fila">
          <dt>{{ t().comprometido }}<small>{{ t().ayudaComprometido }}</small></dt>
          <dd><ap-monto [monto]="p().comprometidoEnPozos!" [moneda]="p().moneda" [etiqueta]="t().comprometido" /> <ap-chip-estado tono="neutro" icono="candado">No gastable</ap-chip-estado></dd>
        </div>
      } @else {
        <div class="fila fila--gastable">
          <dt>Saldo disponible<small>Según el libro. Incluye lo que pueda estar comprometido en pozos.</small></dt>
          <dd><ap-monto [monto]="p().disponible" [moneda]="p().moneda" etiqueta="Saldo disponible" /> <ap-chip-estado tono="aviso" icono="alerta">Sin separar</ap-chip-estado></dd>
        </div>
      }
      <div class="fila">
        <dt>{{ t().retenido }}<small>{{ t().ayudaRetenido }}</small></dt>
        <dd><ap-monto [monto]="p().retenido" [moneda]="p().moneda" [etiqueta]="t().retenido" /> <ap-chip-estado tono="neutro" icono="candado">No gastable</ap-chip-estado></dd>
      </div>
      <div class="fila fila--total">
        <dt>Total de la cuenta en el libro<small>{{ p().separado ? 'Gastable + comprometido + retenido.' : 'Disponible + retenido.' }}</small></dt>
        <dd><ap-monto [monto]="p().totalDeLaCuenta" [moneda]="p().moneda" etiqueta="Total de la cuenta en el libro" /></dd>
      </div>
    </dl>
    <p class="alcance"><ng-content /></p>
  `,
  styles: `
    :host { display: grid; gap: var(--s3); }
    dl { margin: 0; display: grid; padding: var(--s2) var(--s5); background: var(--surface); border: var(--borde-fino) solid var(--border); border-radius: var(--r-lg); box-shadow: var(--sombra-1); }
    .fila { display: grid; grid-template-columns: 1fr auto; gap: var(--s2) var(--s5); align-items: center; padding: var(--s3) 0; border-bottom: var(--borde-fino) solid var(--border); }
    .fila:last-child { border-bottom: 0; }
    .fila--gastable dd { font-size: 1.15em; font-weight: 600; }
    .fila--total { border-top: var(--borde-fino) solid var(--text-3); font-weight: 600; }
    dt { color: var(--text); }
    dt small { display: block; color: var(--text-3); font-size: .8125rem; font-weight: 400; }
    dd { margin: 0; display: flex; flex-wrap: wrap; gap: var(--s2); align-items: center; justify-content: flex-end; text-align: right; }
    .alcance { margin: 0; color: var(--text-2); font-size: .875rem; }
    .alcance:empty { display: none; }
    @media (max-width: 40em) { .fila { grid-template-columns: 1fr; } dd { justify-content: flex-start; text-align: left; } }
  `,
})
export class PosicionesDeSaldo {
  readonly posiciones = input.required<PosicionesParaVista>()
  readonly perspectiva = input<Perspectiva>('soporte')
  protected readonly p = computed(() => this.posiciones())
  protected readonly t = computed(() => TEXTOS[this.perspectiva()])
  protected readonly excede = computed(() => this.p().advertencias.includes('comprometido-excede-disponible'))
  protected readonly aviso = computed(() => AVISO_EXCEDE[this.perspectiva()])
}
