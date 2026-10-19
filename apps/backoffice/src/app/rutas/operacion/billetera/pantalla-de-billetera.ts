import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core'
import { DatePipe } from '@angular/common'
import { EstadoDePantalla } from '@aportaya/ui/estado-de-pantalla/estado-de-pantalla'
import { PosicionesDeSaldo } from '@aportaya/ui/posiciones-de-saldo/posiciones-de-saldo'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { posicionesDe, saldoDe, saldoEnCero } from '../dominio/cu13-consultar-saldo'
import { textosOperacion } from '../textos'

/**
 * La pantalla real de la fase F0: compone organismos, sin lógica. `cuentaId` entra por
 * la URL (`withComponentInputBinding`).
 */
@Component({
  selector: 'ap-pantalla-de-billetera',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [EstadoDePantalla, PosicionesDeSaldo, Alerta, CabeceraDePagina, DatePipe],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina data-tutorial-id="operacion-billetera" [titulo]="t.titulo" [descripcion]="t.proposito" icono="recarga" />
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
    .corte { margin: var(--s3) 0 0; color: var(--text-2); font-size: .875rem; }
  `,
})
export class PantallaDeBilletera {
  constructor() {
    inject(ExplicadorDeVista).fijar(this.t.proposito)
  }
  readonly cuentaId = input.required<string>()
  protected readonly t = textosOperacion.billetera
  protected readonly saldo = saldoDe(this.cuentaId)
  protected readonly enCero = saldoEnCero
  protected readonly lectura = posicionesDe
}
