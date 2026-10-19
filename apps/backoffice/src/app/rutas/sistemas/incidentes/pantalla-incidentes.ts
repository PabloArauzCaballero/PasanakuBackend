import { ChangeDetectionStrategy, Component, inject, resource } from '@angular/core'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { EstadoDePantalla } from '@aportaya/ui/estado-de-pantalla/estado-de-pantalla'
import { TablaDeDatos, type Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { PUERTO_INCIDENTES, type Incidente } from '../dominio/puertos'
import { textosSistemas } from '../textos'

@Component({
  selector: 'ap-pantalla-incidentes',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CabeceraDePagina, TablaDeDatos, ChipEstado, EstadoDePantalla],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina [titulo]="t.titulo" [descripcion]="t.proposito" icono="advertencia" />
      <ap-estado-de-pantalla [recurso]="incidentes" [vacio]="esVacio" [mensajeVacio]="t.vacio" [etiquetaDeCarga]="t.cargando" (reintentar)="incidentes.reload()">
        @if (incidentes.hasValue() && incidentes.value(); as filas) {
          <ap-tabla-de-datos [titulo]="t.titulo" [columnas]="columnas" [filas]="filas">
            <ng-template #celda let-fila let-columna="columna">
              @if (columna.clave === 'estado') {
                <ap-chip-estado [tono]="fila.estado === 'abierto' ? 'aviso' : 'ok'">{{ fila.estado }}</ap-chip-estado>
              } @else {
                {{ fila[columna.clave] }}
              }
            </ng-template>
          </ap-tabla-de-datos>
        }
      </ap-estado-de-pantalla>
    </section>
  `,
  styles: `.pantalla { padding: 0; max-width: 60rem; display: flex; flex-direction: column; gap: var(--s4); }`,
})
export class PantallaIncidentes {
  constructor() {
    inject(ExplicadorDeVista).fijar(this.t.proposito)
  }

  protected readonly t = textosSistemas.incidentes
  private readonly puerto = inject(PUERTO_INCIDENTES)
  protected readonly incidentes = resource({ loader: () => this.puerto.obtener() })
  protected readonly esVacio = (filas: Incidente[]) => filas.length === 0
  protected readonly columnas: Columna<Incidente>[] = [
    { clave: 'titulo', titulo: 'Incidente', ordenable: true },
    { clave: 'severidad', titulo: 'Severidad' },
    { clave: 'estado', titulo: 'Estado' },
    { clave: 'abiertoEl', titulo: 'Abierto el' },
  ]
}
