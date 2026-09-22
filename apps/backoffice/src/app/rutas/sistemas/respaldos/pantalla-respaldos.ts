import { ChangeDetectionStrategy, Component } from '@angular/core'
import { BandaDeProposito } from '@aportaya/ui/banda-de-proposito/banda-de-proposito'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { TablaDeDatos, type Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import { Respaldo, respaldosSimulados, restauracionVencida } from '../dominio/datos-simulados'
import { textosSistemas } from '../textos'

/** GATE: una restauración probada hace más de 30 días aparece marcada VENCIDA, con datos de ejemplo. */
@Component({
  selector: 'ap-pantalla-respaldos',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BandaDeProposito, TablaDeDatos, ChipEstado],
  template: `
    <ap-banda-de-proposito [texto]="t.proposito" />
    <main>
      <h1>{{ t.titulo }}</h1>
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
    </main>
  `,
  styles: `main { padding: 0; max-width: 60rem; } h1 { margin-bottom: var(--s4); }`,
})
export class PantallaRespaldos {
  protected readonly t = textosSistemas.respaldos
  protected readonly filas = respaldosSimulados
  protected readonly columnas: Columna<Respaldo>[] = [
    { clave: 'origen', titulo: 'Origen', ordenable: true },
    { clave: 'tomadoEl', titulo: 'Tomado el' },
    { clave: 'ultimaRestauracionProbadaEl', titulo: 'Última restauración probada' },
  ]
  protected vencida(r: Respaldo): boolean {
    return restauracionVencida(r)
  }
}
