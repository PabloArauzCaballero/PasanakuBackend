import { ChangeDetectionStrategy, Component, inject, resource } from '@angular/core'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { EstadoDePantalla } from '@aportaya/ui/estado-de-pantalla/estado-de-pantalla'
import { TablaDeDatos, type Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { PUERTO_OUTBOX, type MensajeDescartado, type MensajeOutbox, type OutboxYDescartados } from '../dominio/puertos'
import { textosSistemas } from '../textos'

/** GATE: la cola de descartados es visible, con el motivo de cada mensaje a la vista (no en un detalle escondido). */
@Component({
  selector: 'ap-pantalla-outbox',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CabeceraDePagina, TablaDeDatos, ChipEstado, EstadoDePantalla],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina [titulo]="t.titulo" [descripcion]="t.proposito" icono="campana" />
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
    </section>
  `,
  styles: `.pantalla { padding: 0; max-width: 60rem; display: flex; flex-direction: column; gap: var(--s5); }`,
})
export class PantallaOutbox {
  constructor() {
    inject(ExplicadorDeVista).fijar(this.t.proposito)
  }

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
