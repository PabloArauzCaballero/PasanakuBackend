import { ChangeDetectionStrategy, Component, computed } from '@angular/core'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { Icono } from '@aportaya/ui/icono/icono'
import { crearListado } from '@aportaya/ui/listado/crear-listado'
import { ListadoDeDatos, type CargaDeListado } from '@aportaya/ui/listado/listado'
import type { Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import type { ColumnaExportable } from '@aportaya/ui/exportar-listado/exportar'
import { mensajeDe, type ErrorTraducido } from '../../nucleo/errores'
import { filasDeEndpoints, recursoEndpointsDelDia, type FilaDeEndpoint } from './dominio/endpoints-del-dia'
import { textosTablero } from './textos'

/**
 * Tablero de sistemas · los endpoints tocados HOY, en vivo, con la ruta PLANTILLA (nunca la URL con identificadores),
 * con la anatomía única del listado: busca por servicio, método o ruta, filtra por servicio, método y si tuvo errores,
 * ordena por solicitudes o p95 y baja lo filtrado en CSV/JSON. «Fuente no disponible» (Prometheus no respondió) es
 * distinto de «nadie llamó»: lo primero no se sabe, lo segundo sí. No hay acciones de fila: es una lectura de métricas.
 */
@Component({
  selector: 'ap-tabla-endpoints-del-dia',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Alerta, ChipEstado, Icono, ListadoDeDatos],
  template: `
    <section class="bloque" aria-labelledby="tablero-endpoints">
      <h2 id="tablero-endpoints"><ap-icono nombre="servidor" tamano="s5" />{{ t.titulo }}</h2>
      <p class="explicacion">{{ t.explicacion }}</p>
      @if (sinFuente()) {
        <ap-alerta tono="aviso">{{ t.sinFuente }}</ap-alerta>
      } @else {
        <ap-listado [titulo]="t.titulo" [columnas]="columnas" [listado]="listado" [carga]="carga()" [identidad]="porId" densidad="compacta" [exportables]="exportables" nombreDeRegistros="endpoints" [ayudaDeBusqueda]="t.ayudaBusqueda" [textoVacio]="t.vacio" (reintentar)="endpoints.reload()">
          <ng-template #celda let-f let-columna="columna">
            @switch (columna.clave) {
              @case ('ruta') { <code class="ruta">@for (s of segmentos(f.ruta); track $index) {<span>{{ s }}</span><wbr />}</code> }
              @case ('metodo') { <ap-chip-estado tono="info">{{ f.metodo }}</ap-chip-estado> }
              @case ('errores') { <span [class.con-errores]="f.orden.errores > 0">{{ f.errores }}</span> }
              @default { {{ f[columna.clave] }} }
            }
          </ng-template>
        </ap-listado>
      }
    </section>
  `,
  styles: `
    .bloque { display: grid; gap: var(--s3); min-width: 0; }
    .bloque > * { min-width: 0; }
    /* Una ruta plantilla no tiene espacios: sin corte, en móvil ensancha la tarjeta (prueba visual H9). */
    .ruta { min-width: 0; white-space: normal; overflow-wrap: anywhere; }
    h2 { display: flex; align-items: center; gap: var(--s2); margin: 0; font-size: 1.15em; }
    .explicacion { margin: 0; color: var(--text-2); }
    .con-errores { color: var(--err-texto); font-weight: 600; }
  `,
})
export class TablaEndpointsDelDia {
  protected readonly t = textosTablero.porRol.endpoints
  protected readonly endpoints = recursoEndpointsDelDia()
  private readonly salida = computed(() => (this.endpoints.hasValue() ? this.endpoints.value() : undefined))
  protected readonly sinFuente = computed(() => this.salida()?.fuenteDisponible === false)
  protected readonly filas = computed(() => filasDeEndpoints(this.salida()))
  protected readonly carga = computed<CargaDeListado>(() => {
    if (this.endpoints.isLoading()) return { tipo: 'cargando' }
    const e = this.endpoints.error() as Partial<ErrorTraducido> | undefined
    if (e) return { tipo: 'error', mensaje: e.mensaje ?? mensajeDe(undefined, 500), idPeticion: e.trazaId }
    return { tipo: 'lista' }
  })
  protected readonly porId = (f: FilaDeEndpoint) => f.id
  /** La ruta cortada después de cada barra: en móvil corta entre segmentos, no a mitad de palabra. */
  protected readonly segmentos = (ruta: string) => ruta.split(/(?<=\/)/)
  protected readonly listado = crearListado<FilaDeEndpoint>({
    filas: this.filas,
    camposDeBusqueda: ['servicio', 'metodo', 'ruta'],
    filtros: [
      { nombre: 'servicio', etiqueta: 'Servicio', valor: (f) => f.servicio },
      { nombre: 'metodo', etiqueta: 'Método', valor: (f) => f.metodo },
      { nombre: 'errores', etiqueta: 'Errores', valor: (f) => (f.orden.errores > 0 ? 'CON' : 'SIN'), opciones: [{ valor: 'CON', texto: 'Con errores' }, { valor: 'SIN', texto: 'Sin errores' }] },
    ],
    ordenInicial: { clave: 'solicitudes', sentido: 'desc' },
    valorParaOrden: (f, clave) => (clave in f.orden ? f.orden[clave as keyof FilaDeEndpoint['orden']] : (f as Record<string, unknown>)[clave]),
  })
  protected readonly columnas: Columna<FilaDeEndpoint>[] = [
    { clave: 'servicio', titulo: 'Servicio', ordenable: true },
    { clave: 'metodo', titulo: 'Método', ordenable: true },
    { clave: 'ruta', titulo: 'Ruta', ordenable: true },
    { clave: 'solicitudes', titulo: 'Solicitudes', numerica: true, ordenable: true },
    { clave: 'errores', titulo: 'Errores', ordenable: true },
    { clave: 'p95', titulo: 'p95', numerica: true, ordenable: true },
  ]
  protected readonly exportables: ColumnaExportable<FilaDeEndpoint>[] = this.columnas.map((c) => ({ clave: c.clave, titulo: c.titulo }))
}
