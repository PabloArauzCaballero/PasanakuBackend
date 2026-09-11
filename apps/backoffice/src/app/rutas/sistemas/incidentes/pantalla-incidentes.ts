import { ChangeDetectionStrategy, Component } from '@angular/core'
import { BandaDeProposito } from '@aportaya/ui/banda-de-proposito/banda-de-proposito'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { TablaDeDatos, type Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import { Incidente, incidentesSimulados } from '../dominio/datos-simulados'
import { textosSistemas } from '../textos'

@Component({
  selector: 'ap-pantalla-incidentes',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BandaDeProposito, TablaDeDatos, ChipEstado],
  template: `
    <ap-banda-de-proposito [texto]="t.proposito" />
    <main>
      <h1>{{ t.titulo }}</h1>
      <ap-tabla-de-datos [titulo]="t.titulo" [columnas]="columnas" [filas]="filas">
        <ng-template #celda let-fila let-columna="columna">
          @if (columna.clave === 'estado') {
            <ap-chip-estado [tono]="fila.estado === 'abierto' ? 'aviso' : 'ok'">{{ fila.estado }}</ap-chip-estado>
          } @else {
            {{ fila[columna.clave] }}
          }
        </ng-template>
      </ap-tabla-de-datos>
    </main>
  `,
  styles: `main { padding: 0; max-width: 60rem; } h1 { margin-bottom: var(--s4); }`,
})
export class PantallaIncidentes {
  protected readonly t = textosSistemas.incidentes
  protected readonly filas = incidentesSimulados
  protected readonly columnas: Columna<Incidente>[] = [
    { clave: 'titulo', titulo: 'Incidente', ordenable: true },
    { clave: 'severidad', titulo: 'Severidad' },
    { clave: 'estado', titulo: 'Estado' },
    { clave: 'abiertoEl', titulo: 'Abierto el' },
  ]
}
