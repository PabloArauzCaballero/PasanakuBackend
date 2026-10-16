import { ChangeDetectionStrategy, Component, input } from '@angular/core'
import { DatePipe } from '@angular/common'
import { EstadoDePantalla } from '@aportaya/ui/estado-de-pantalla/estado-de-pantalla'
import { PosicionesDeSaldo } from '@aportaya/ui/posiciones-de-saldo/posiciones-de-saldo'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { BandaDeProposito } from '@aportaya/ui/banda-de-proposito/banda-de-proposito'
import { posicionesDe, saldoDe, saldoEnCero } from '../dominio/cu13-consultar-saldo'
import { textosOperacion } from '../textos'

/**
 * La pantalla real de la fase F0: compone organismos, sin lógica. `cuentaId` entra por
 * la URL (`withComponentInputBinding`), para que un oficial pueda pegar el enlace en un
 * expediente. Sus cuatro estados los pinta `EstadoDePantalla`; el importe, `Monto`.
 */
@Component({
  selector: 'ap-pantalla-de-billetera',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [EstadoDePantalla, PosicionesDeSaldo, Alerta, BandaDeProposito, DatePipe],
  template: `
    <ap-banda-de-proposito [texto]="t.proposito" />
    <section class="pantalla">
      <h1 data-tutorial-id="operacion-billetera">{{ t.titulo }}</h1>
      <ap-estado-de-pantalla
        data-tutorial-id="operacion-saldo"
        [recurso]="saldo"
        [vacio]="enCero"
        [mensajeVacio]="t.enCero"
        [etiquetaDeCarga]="t.cargando"
        (reintentar)="saldo.reload()"
      >
        @if (saldo.hasValue() && saldo.value(); as s) {
          @if (lectura(s); as l) {
            @if (l.ok) {
              <ap-posiciones-de-saldo [posiciones]="l.posiciones" perspectiva="soporte">{{ t.alcance }}</ap-posiciones-de-saldo>
            } @else {
              <ap-alerta tono="error">{{ t.incoherente }}</ap-alerta>
            }
          }
          <p class="corte">{{ t.alCorteDe }} <time [attr.datetime]="s.alCorteDe">{{ s.alCorteDe | date: 'dd/MM/yyyy HH:mm' : '-0400' }} (La Paz)</time></p>
        }
      </ap-estado-de-pantalla>
    </section>
  `,
  styles: `
    .pantalla { padding: var(--s5); max-width: 40rem; }
    h1 { margin-bottom: var(--s4); }
    .corte { margin: var(--s3) 0 0; color: var(--text-2); font-size: .875rem; }
  `,
})
export class PantallaDeBilletera {
  readonly cuentaId = input.required<string>()
  protected readonly t = textosOperacion.billetera
  protected readonly saldo = saldoDe(this.cuentaId)
  protected readonly enCero = saldoEnCero
  protected readonly lectura = posicionesDe
}
