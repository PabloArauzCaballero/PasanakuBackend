import { ChangeDetectionStrategy, Component, inject, resource } from '@angular/core'
import { BandaDeProposito } from '@aportaya/ui/banda-de-proposito/banda-de-proposito'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { EstadoDePantalla } from '@aportaya/ui/estado-de-pantalla/estado-de-pantalla'
import { TablaDeDatos, type Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import { restauracionVencida } from '../dominio/datos-simulados'
import { PUERTO_RESPALDOS, type Respaldo } from '../dominio/puertos'
import { textosSistemas } from '../textos'

/** GATE: una restauración probada hace más de 30 días aparece marcada VENCIDA. */
@Component({
  selector: 'ap-pantalla-respaldos',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BandaDeProposito, TablaDeDatos, ChipEstado, EstadoDePantalla],
  template: `
    <ap-banda-de-proposito [texto]="t.proposito" />
    <main>
      <h1>{{ t.titulo }}</h1>
      <ap-estado-de-pantalla [recurso]="respaldos" [vacio]="esVacio" [mensajeVacio]="t.vacio" [etiquetaDeCarga]="t.cargando" (reintentar)="respaldos.reload()">
        @if (respaldos.hasValue() && respaldos.value(); as filas) {
          <ap-tabla-de-datos [titulo]="t.titulo" [columnas]="columnas" [filas]="filas">
            <ng-template #celda let-fila let-columna="columna">
              @if (columna.clave === 'ultimaRestauracionProbadaEl') {
                @if (vencida(fila)) {
                  <ap-chip-estado tono="error">{{ fila.ultimaRestauracionProbadaEl ?? 'nunca probada' }} · VENCIDA</ap-chip-estado>
                } @else {
                  <ap-chip-estado tono="ok">{{ fila.ultimaRestauracionProbadaEl }}</ap-chip-estado>
                }
              } @else {
                {{ fila[columna.clave] }}
              }
            </ng-template>
          </ap-tabla-de-datos>
        }
      </ap-estado-de-pantalla>
    </main>
  `,
  styles: `main { padding: 0; max-width: 60rem; } h1 { margin-bottom: var(--s4); }`,
})
export class PantallaRespaldos {
  protected readonly t = textosSistemas.respaldos
  private readonly puerto = inject(PUERTO_RESPALDOS)
  protected readonly respaldos = resource({ loader: () => this.puerto.obtener() })
  protected readonly esVacio = (filas: Respaldo[]) => filas.length === 0
  protected readonly columnas: Columna<Respaldo>[] = [
    { clave: 'origen', titulo: 'Origen', ordenable: true },
    { clave: 'tomadoEl', titulo: 'Tomado el' },
    { clave: 'ultimaRestauracionProbadaEl', titulo: 'Última restauración probada' },
  ]
  protected vencida(r: Respaldo): boolean {
    return restauracionVencida(r)
  }
}
