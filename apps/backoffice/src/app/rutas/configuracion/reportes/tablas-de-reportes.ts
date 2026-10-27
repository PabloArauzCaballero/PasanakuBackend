import { ChangeDetectionStrategy, Component, input } from '@angular/core'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { Fecha } from '@aportaya/ui/fecha/fecha'
import type { Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import type { ListadoPorCursor } from '../../contabilidad/listado/listado-por-cursor'
import { TablaPorCursor } from '../../contabilidad/listado/tabla-por-cursor'
import type { DefinicionDeReporteEnLista, EjecucionDeReporteEnLista } from '../dominio/cu58-reportes'
import { NO_DISPONIBLE } from '../dominio/listado'
import { textosConfiguracion } from '../textos'

const t = textosConfiguracion.reportes
const ESTADOS: Record<string, string> = t.estados
const sensibilidad = (r: DefinicionDeReporteEnLista) => (r.contieneDatosSensibles ? t.sensible : t.noSensible)

const COLUMNAS_CATALOGO: Columna<DefinicionDeReporteEnLista>[] = [
  { clave: 'nombre', titulo: 'Reporte' },
  { clave: 'tipo', titulo: 'Tipo' },
  { clave: 'descripcion', titulo: 'Descripción' },
  { clave: 'permisoRequerido', titulo: 'Permiso para ejecutarlo', mono: true },
  { clave: 'contieneDatosSensibles', titulo: 'Sensibilidad' },
]

const EXPORTABLES_CATALOGO = [
  { clave: 'nombre', titulo: 'Reporte' },
  { clave: 'tipo', titulo: 'Tipo' },
  { clave: 'descripcion', titulo: 'Descripción' },
  { clave: 'permisoRequerido', titulo: 'Permiso para ejecutarlo' },
  { clave: 'sensibilidad', titulo: 'Sensibilidad', valor: sensibilidad },
]

/** CU-58 · El catálogo de reportes, presentacional: el listado lo arma la pantalla. Sin acciones (solo lectura). */
@Component({
  selector: 'ap-tabla-de-catalogo',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ChipEstado, TablaPorCursor],
  template: `
    <ap-tabla-por-cursor [titulo]="t.definiciones.titulo" [columnas]="COLUMNAS" [listado]="listado()" [identidad]="identidad" [exportables]="EXPORTABLES" nombreDeRegistros="reportes" [textoVacio]="t.definiciones.vacio" ayudaDeBusqueda="Busca por nombre, tipo o descripción en la página cargada: el servidor no busca por texto.">
      <ng-template #celda let-r let-columna="columna">
        @switch (columna.clave) {
          @case ('contieneDatosSensibles') { <ap-chip-estado [tono]="r.contieneDatosSensibles ? 'aviso' : 'neutro'" [icono]="r.contieneDatosSensibles ? 'candado' : undefined">{{ sensibilidad(r) }}</ap-chip-estado> }
          @default { {{ r[columna.clave] ?? noDisponible }} }
        }
      </ng-template>
    </ap-tabla-por-cursor>
  `,
  styles: `:host { display: block; min-width: 0; }`,
})
export class TablaDeCatalogo {
  readonly listado = input.required<ListadoPorCursor<DefinicionDeReporteEnLista>>()
  protected readonly t = t
  protected readonly COLUMNAS = COLUMNAS_CATALOGO
  protected readonly EXPORTABLES = EXPORTABLES_CATALOGO
  protected readonly noDisponible = NO_DISPONIBLE
  protected readonly sensibilidad = sensibilidad
  protected readonly identidad = (r: DefinicionDeReporteEnLista) => r.definicionId
}

/** Sin quién lo pidió ni sus parámetros: el contrato no publica los parámetros y la persona no hace falta para auditar. */
const COLUMNAS_EJECUCIONES: Columna<EjecucionDeReporteEnLista>[] = [
  { clave: 'iniciadaEn', titulo: 'Iniciada' },
  { clave: 'definicionId', titulo: 'Reporte' },
  { clave: 'estado', titulo: 'Estado' },
  { clave: 'filasGeneradas', titulo: 'Filas', numerica: true },
  { clave: 'duracionMs', titulo: 'Duración (ms)', numerica: true },
  { clave: 'hashResultado', titulo: 'Huella del resultado', mono: true },
  { clave: 'finalizadaEn', titulo: 'Finalizada' },
]

/** CU-58 · El historial de ejecuciones, presentacional. El nombre sale del catálogo ya cargado o queda el identificador. */
@Component({
  selector: 'ap-tabla-de-ejecuciones',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ChipEstado, Fecha, TablaPorCursor],
  template: `
    <p class="nota">{{ t.ejecuciones.corte }}</p>
    <ap-tabla-por-cursor [titulo]="t.ejecuciones.titulo" [columnas]="COLUMNAS" [listado]="listado()" [identidad]="identidad" [exportables]="exportables" nombreDeRegistros="ejecuciones" [textoVacio]="t.ejecuciones.vacio" ayudaDeBusqueda="Busca por reporte o huella en la página cargada: el servidor no busca por texto.">
      <ng-template #celda let-r let-columna="columna">
        @switch (columna.clave) {
          @case ('iniciadaEn') { <ap-fecha [iso]="r.iniciadaEn" /> }
          @case ('finalizadaEn') { @if (r.finalizadaEn) { <ap-fecha [iso]="r.finalizadaEn" /> } @else { {{ noDisponible }} } }
          @case ('definicionId') { {{ nombre(r) }} }
          @case ('estado') { <ap-chip-estado [tono]="r.estado === 'COMPLETADA' ? 'ok' : r.estado === 'FALLIDA' || r.estado === 'EXPIRADA' ? 'error' : 'neutro'">{{ estado(r) }}</ap-chip-estado> }
          @default { {{ r[columna.clave] ?? noDisponible }} }
        }
      </ng-template>
    </ap-tabla-por-cursor>
  `,
  styles: `
    :host { display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s4); min-width: 0; }
    .nota { margin: 0; color: var(--text-2); }
  `,
})
export class TablaDeEjecuciones {
  readonly listado = input.required<ListadoPorCursor<EjecucionDeReporteEnLista>>()
  readonly nombres = input<Readonly<Record<string, string>>>({})
  protected readonly t = t
  protected readonly COLUMNAS = COLUMNAS_EJECUCIONES
  protected readonly noDisponible = NO_DISPONIBLE
  protected readonly identidad = (r: EjecucionDeReporteEnLista) => r.ejecucionId
  protected readonly estado = (r: EjecucionDeReporteEnLista) => ESTADOS[r.estado] ?? r.estado
  protected readonly nombre = (r: EjecucionDeReporteEnLista) => this.nombres()[r.definicionId] ?? r.definicionId
  protected readonly exportables = [
    { clave: 'iniciadaEn', titulo: 'Iniciada' },
    { clave: 'reporte', titulo: 'Reporte', valor: this.nombre },
    { clave: 'estado', titulo: 'Estado', valor: this.estado },
    { clave: 'filasGeneradas', titulo: 'Filas' },
    { clave: 'duracionMs', titulo: 'Duración (ms)' },
    { clave: 'hashResultado', titulo: 'Huella del resultado', valor: (r: EjecucionDeReporteEnLista) => r.hashResultado ?? NO_DISPONIBLE },
    { clave: 'finalizadaEn', titulo: 'Finalizada', valor: (r: EjecucionDeReporteEnLista) => r.finalizadaEn ?? NO_DISPONIBLE },
  ]
}
