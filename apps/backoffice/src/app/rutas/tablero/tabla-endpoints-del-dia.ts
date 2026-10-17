import { ChangeDetectionStrategy, Component, computed } from '@angular/core'
import { EstadoDePantalla } from '@aportaya/ui/estado-de-pantalla/estado-de-pantalla'
import { TablaDeDatos, type Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import { filasDeEndpoints, recursoEndpointsDelDia, type FilaDeEndpoint } from './dominio/endpoints-del-dia'
import { textosTablero } from './textos'

/**
 * Tablero de sistemas · los endpoints tocados HOY, en vivo, con la ruta PLANTILLA (nunca la URL con identificadores).
 * «Fuente no disponible» (Prometheus no respondió) es distinto de «nadie llamó»: lo primero no se sabe, lo segundo sí.
 */
@Component({
  selector: 'ap-tabla-endpoints-del-dia',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [EstadoDePantalla, TablaDeDatos],
  template: `
    <section class="bloque" aria-labelledby="tablero-endpoints">
      <h2 id="tablero-endpoints">{{ t.titulo }}</h2>
      <p class="explicacion">{{ t.explicacion }}</p>
      <ap-estado-de-pantalla
        [recurso]="endpoints"
        [vacio]="sinFilas"
        [mensajeVacio]="t.vacio"
        [etiquetaDeCarga]="t.cargando"
        (reintentar)="endpoints.reload()"
      >
        @if (sinFuente()) {
          <p role="status" class="aviso">{{ t.sinFuente }}</p>
        } @else {
          <ap-tabla-de-datos [titulo]="t.titulo" [columnas]="columnas" [filas]="filas()" [identidad]="porId" densidad="compacta">
            <ng-template #celda let-f let-columna="columna">
              @if (columna.clave === 'ruta') { <code class="ruta">@for (s of segmentos(f.ruta); track $index) {<span>{{ s }}</span><wbr />}</code> } @else { {{ f[columna.clave] }} }
            </ng-template>
          </ap-tabla-de-datos>
        }
      </ap-estado-de-pantalla>
    </section>
  `,
  styles: `
    .bloque { display: grid; gap: var(--s3); min-width: 0; }
    .bloque > * { min-width: 0; }
    /* Una ruta plantilla no tiene espacios: sin corte, en móvil ensancha la tarjeta (prueba visual H9). */
    .ruta { min-width: 0; white-space: normal; overflow-wrap: anywhere; }
    h2 { margin: 0; font-size: 1.15em; }
    .explicacion, .aviso { margin: 0; color: var(--text-2); }
  `,
})
export class TablaEndpointsDelDia {
  protected readonly t = textosTablero.porRol.endpoints
  protected readonly endpoints = recursoEndpointsDelDia()
  private readonly salida = computed(() => (this.endpoints.hasValue() ? this.endpoints.value() : undefined))
  protected readonly sinFuente = computed(() => this.salida()?.fuenteDisponible === false)
  protected readonly filas = computed(() => filasDeEndpoints(this.salida()))
  /** Vacío solo si la fuente respondió y nadie llamó; sin fuente se dice otra cosa (rama de arriba). */
  protected readonly sinFilas = (s: { fuenteDisponible: boolean; endpoints: unknown[] }) => s.fuenteDisponible && s.endpoints.length === 0
  protected readonly porId = (f: FilaDeEndpoint) => f.id
  /** La ruta cortada después de cada barra: en móvil corta entre segmentos, no a mitad de palabra. */
  protected readonly segmentos = (ruta: string) => ruta.split(/(?<=\/)/)
  protected readonly columnas: Columna<FilaDeEndpoint>[] = [
    { clave: 'servicio', titulo: 'Servicio' },
    { clave: 'metodo', titulo: 'Método' },
    { clave: 'ruta', titulo: 'Ruta' },
    { clave: 'solicitudes', titulo: 'Solicitudes', numerica: true },
    { clave: 'errores', titulo: 'Errores' },
    { clave: 'p95', titulo: 'p95', numerica: true },
  ]
}
