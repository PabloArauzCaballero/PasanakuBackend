import { ChangeDetectionStrategy, Component } from '@angular/core'
import { BandaDeProposito } from '@aportaya/ui/banda-de-proposito/banda-de-proposito'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { TablaDeDatos, type Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import { MensajeDescartado, MensajeOutbox, descartadosSimulados, outboxSimulado } from '../dominio/datos-simulados'
import { textosSistemas } from '../textos'

/** GATE: la cola de descartados es visible, con el motivo de cada mensaje a la vista (no en un detalle escondido). */
@Component({
  selector: 'ap-pantalla-outbox',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BandaDeProposito, TablaDeDatos, ChipEstado],
  template: `
    <ap-banda-de-proposito [texto]="t.proposito" />
    <main>
      <h1>{{ t.titulo }}</h1>
      <section>
        <ap-tabla-de-datos [titulo]="'Bandeja de salida'" [columnas]="columnasOutbox" [filas]="outbox">
          <ng-template #celda let-fila let-columna="columna">
            @if (columna.clave === 'estado') {
              <ap-chip-estado [tono]="fila.estado === 'entregado' ? 'ok' : 'aviso'">{{ fila.estado }}</ap-chip-estado>
            } @else {
              {{ fila[columna.clave] }}
            }
          </ng-template>
        </ap-tabla-de-datos>
      </section>
      <section>
        <h2>{{ t.tituloDescartados }}</h2>
        <ap-tabla-de-datos [titulo]="t.tituloDescartados" [columnas]="columnasDescartados" [filas]="descartados" />
      </section>
    </main>
  `,
  styles: `main { padding: 0; max-width: 60rem; display: flex; flex-direction: column; gap: var(--s5); } h1 { margin-bottom: 0; }`,
})
export class PantallaOutbox {
  protected readonly t = textosSistemas.outbox
  protected readonly outbox: MensajeOutbox[] = outboxSimulado
  protected readonly descartados: MensajeDescartado[] = descartadosSimulados
  protected readonly columnasOutbox: Columna<MensajeOutbox>[] = [
    { clave: 'tipo', titulo: 'Tipo', ordenable: true },
    { clave: 'creadoEl', titulo: 'Creado el' },
    { clave: 'estado', titulo: 'Estado' },
  ]
  protected readonly columnasDescartados: Columna<MensajeDescartado>[] = [
    { clave: 'tipo', titulo: 'Tipo', ordenable: true },
    { clave: 'motivo', titulo: 'Motivo' },
    { clave: 'descartadoEl', titulo: 'Descartado el' },
  ]
}
