import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core'
import { formatearDia } from '@aportaya/ui/fecha/fecha'
import type { ExpedienteEnRevision } from 'clientes/angular/identidad'
import { vencimientoDe } from '../dominio/cu02-expedientes'
import { textosCumplimiento } from '../textos'

/**
 * «Vence el 10 may 2027» bajo el documento, en la tarjeta del expediente. Es lo que el
 * revisor coteja contra la foto del anverso; vencido o sin fecha se marca, porque con
 * eso no se aprueba (AP-CU01-10/11).
 */
@Component({
  selector: 'ap-vencimiento-del-documento',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <p [class.marcado]="estado() !== 'vigente'">
      @if (expediente().fechaExpiracionDocumento; as vence) {
        {{ t.venceEl }} {{ dia(vence) }}
        @if (estado() === 'vencido') {
          · {{ t.documentoVencido }}
        }
      } @else {
        {{ t.sinVencimiento }}
      }
    </p>
  `,
  styles: `
    p { margin: 0; color: var(--text-2); font-size: .875rem; }
    .marcado { color: var(--aviso-texto); font-weight: 600; }
  `,
})
export class VencimientoDelDocumento {
  protected readonly t = textosCumplimiento.verificaciones
  protected readonly dia = formatearDia

  readonly expediente = input.required<ExpedienteEnRevision>()
  protected readonly estado = computed(() => vencimientoDe(this.expediente()))
}
