import { ChangeDetectionStrategy, Component, input } from '@angular/core'
import { DatePipe } from '@angular/common'
import { EstadoDePantalla } from '@aportaya/ui/estado-de-pantalla/estado-de-pantalla'
import { Monto } from '@aportaya/ui/monto/monto'
import { BandaDeProposito } from '@aportaya/ui/banda-de-proposito/banda-de-proposito'
import { saldoDe, saldoEnCero } from '../dominio/cu13-consultar-saldo'
import { textosOperacion } from '../textos'

/**
 * La pantalla real de la fase F0: compone organismos, sin lógica. `cuentaId` entra por
 * la URL (`withComponentInputBinding`), para que un oficial pueda pegar el enlace en un
 * expediente. Sus cuatro estados los pinta `EstadoDePantalla`; el importe, `Monto`.
 */
@Component({
  selector: 'ap-pantalla-de-billetera',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [EstadoDePantalla, Monto, BandaDeProposito, DatePipe],
  template: `
    <ap-banda-de-proposito [texto]="t.proposito" />
    <main>
      <h1>{{ t.titulo }}</h1>
      <ap-estado-de-pantalla
        [recurso]="saldo"
        [vacio]="enCero"
        [mensajeVacio]="t.enCero"
        [etiquetaDeCarga]="t.cargando"
        (reintentar)="saldo.reload()"
      >
        @if (saldo.hasValue() && saldo.value(); as s) {
          <dl class="saldo">
            <dt>{{ t.disponible }}</dt>
            <dd><ap-monto [monto]="s.disponible.monto" [moneda]="s.disponible.moneda" [etiqueta]="t.disponible" /></dd>
            <dt>{{ t.retenido }}</dt>
            <dd><ap-monto [monto]="s.retenido.monto" [moneda]="s.retenido.moneda" [etiqueta]="t.retenido" /></dd>
            <dt>{{ t.alCorteDe }}</dt>
            <dd><time [attr.datetime]="s.alCorteDe">{{ s.alCorteDe | date: 'dd/MM/yyyy HH:mm' : '-0400' }} (La Paz)</time></dd>
          </dl>
        }
      </ap-estado-de-pantalla>
    </main>
  `,
  styles: `
    main { padding: var(--s5); max-width: 40rem; }
    h1 { margin-bottom: var(--s4); }
    .saldo { display: grid; grid-template-columns: auto 1fr; gap: var(--s2) var(--s5); margin: 0; padding: var(--s5); background: var(--surface); border: var(--borde-fino) solid var(--border); border-radius: var(--r-lg); box-shadow: var(--sh-1); }
    dt { color: var(--text-2); }
    dd { margin: 0; text-align: right; }
  `,
})
export class PantallaDeBilletera {
  readonly cuentaId = input.required<string>()
  protected readonly t = textosOperacion.billetera
  protected readonly saldo = saldoDe(this.cuentaId)
  protected readonly enCero = saldoEnCero
}
