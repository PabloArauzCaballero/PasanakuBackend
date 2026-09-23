import { ChangeDetectionStrategy, Component, inject, resource } from '@angular/core'
import { BandaDeProposito } from '@aportaya/ui/banda-de-proposito/banda-de-proposito'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { EstadoDePantalla } from '@aportaya/ui/estado-de-pantalla/estado-de-pantalla'
import { TablaDeDatos, type Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import { PUERTO_BASE_DE_DATOS, type Migracion } from '../dominio/puertos'
import { textosSistemas } from '../textos'

@Component({
  selector: 'ap-pantalla-base-datos',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BandaDeProposito, TablaDeDatos, ChipEstado, EstadoDePantalla],
  template: `
    <ap-banda-de-proposito [texto]="t.proposito" />
    <main>
      <h1>{{ t.titulo }}</h1>
      <ap-estado-de-pantalla [recurso]="migraciones" [vacio]="esVacio" [mensajeVacio]="t.vacio" [etiquetaDeCarga]="t.cargando" (reintentar)="migraciones.reload()">
        @if (migraciones.hasValue() && migraciones.value(); as filas) {
          <ap-tabla-de-datos [titulo]="t.titulo" [columnas]="columnas" [filas]="filas">
            <ng-template #celda let-fila let-columna="columna">
              @if (columna.clave === 'estado') {
                <ap-chip-estado [tono]="fila.estado === 'aplicada' ? 'ok' : 'aviso'">{{ fila.estado }}</ap-chip-estado>
              } @else {
                {{ fila[columna.clave] ?? '—' }}
              }
            </ng-template>
          </ap-tabla-de-datos>
        }
      </ap-estado-de-pantalla>
    </main>
  `,
  styles: `main { padding: 0; max-width: 60rem; } h1 { margin-bottom: var(--s4); }`,
})
export class PantallaBaseDeDatos {
  protected readonly t = textosSistemas.baseDeDatos
  private readonly puerto = inject(PUERTO_BASE_DE_DATOS)
  protected readonly migraciones = resource({ loader: () => this.puerto.obtener() })
  protected readonly esVacio = (filas: Migracion[]) => filas.length === 0
  protected readonly columnas: Columna<Migracion>[] = [
    { clave: 'nombre', titulo: 'Migración', ordenable: true },
    { clave: 'estado', titulo: 'Estado' },
    { clave: 'aplicadaEl', titulo: 'Aplicada el' },
  ]
}
