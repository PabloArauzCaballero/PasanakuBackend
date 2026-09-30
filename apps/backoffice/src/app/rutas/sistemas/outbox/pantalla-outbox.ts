import { ChangeDetectionStrategy, Component, inject, resource } from '@angular/core'
import { BandaDeProposito } from '@aportaya/ui/banda-de-proposito/banda-de-proposito'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { EstadoDePantalla } from '@aportaya/ui/estado-de-pantalla/estado-de-pantalla'
import { TablaDeDatos, type Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import { PUERTO_OUTBOX, type MensajeDescartado, type MensajeOutbox, type OutboxYDescartados } from '../dominio/puertos'
import { textosSistemas } from '../textos'

/** GATE: la cola de descartados es visible, con el motivo de cada mensaje a la vista (no en un detalle escondido). */
@Component({
  selector: 'ap-pantalla-outbox',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BandaDeProposito, TablaDeDatos, ChipEstado, EstadoDePantalla],
  template: `
    <ap-banda-de-proposito [texto]="t.proposito" />
    <main>
      <h1>{{ t.titulo }}</h1>
      <ap-estado-de-pantalla [recurso]="outbox" [vacio]="esVacio" [mensajeVacio]="t.vacio" [etiquetaDeCarga]="t.cargando" (reintentar)="outbox.reload()">
        @if (outbox.hasValue() && outbox.value(); as datos) {
          <section>
            <ap-tabla-de-datos [titulo]="'Bandeja de salida'" [columnas]="columnasOutbox" [filas]="datos.mensajes">
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
            @if (datos.descartados.length === 0) {
              <p>{{ t.vacioDescartados }}</p>
            } @else {
              <ap-tabla-de-datos [titulo]="t.tituloDescartados" [columnas]="columnasDescartados" [filas]="datos.descartados" />
            }
          </section>
        }
      </ap-estado-de-pantalla>
    </main>
  `,
  styles: `main { padding: 0; max-width: 60rem; display: flex; flex-direction: column; gap: var(--s5); } h1 { margin-bottom: 0; }`,
})
export class PantallaOutbox {
  protected readonly t = textosSistemas.outbox
  private readonly puerto = inject(PUERTO_OUTBOX)
  protected readonly outbox = resource({ loader: () => this.puerto.obtener() })
  protected readonly esVacio = (datos: OutboxYDescartados) => datos.mensajes.length === 0
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
