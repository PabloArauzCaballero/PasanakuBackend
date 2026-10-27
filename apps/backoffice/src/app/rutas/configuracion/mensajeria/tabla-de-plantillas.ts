import { ChangeDetectionStrategy, Component } from '@angular/core'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { Fecha } from '@aportaya/ui/fecha/fecha'
import type { Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import { crearListadoPorCursor, opcionesDe } from '../../contabilidad/listado/listado-por-cursor'
import { TablaPorCursor } from '../../contabilidad/listado/tabla-por-cursor'
import { cargadorDePlantillas, type PlantillaEnLista } from '../dominio/cu80-cu83-mensajeria'
import { NO_DISPONIBLE } from '../dominio/listado'
import { textosConfiguracion } from '../textos'

const t = textosConfiguracion.mensajeria
const APROBACION: Record<string, string> = t.estadosPlantilla
const aprobacion = (r: PlantillaEnLista) => APROBACION[r.estadoAprobacion] ?? r.estadoAprobacion
const activa = (r: PlantillaEnLista) => (r.activa ? t.activa : t.inactiva)

const COLUMNAS: Columna<PlantillaEnLista>[] = [
  { clave: 'codigo', titulo: 'Código', mono: true },
  { clave: 'evento', titulo: 'Evento' },
  { clave: 'canal', titulo: 'Canal' },
  { clave: 'estadoAprobacion', titulo: 'Aprobación' },
  { clave: 'activa', titulo: 'Activa' },
  { clave: 'version', titulo: 'Versión', numerica: true },
  { clave: 'idioma', titulo: 'Idioma' },
  { clave: 'vigenteDesde', titulo: 'Vigente desde' },
  { clave: 'vigenteHasta', titulo: 'Vigente hasta' },
]

const EXPORTABLES = [
  { clave: 'codigo', titulo: 'Código' },
  { clave: 'evento', titulo: 'Evento' },
  { clave: 'canal', titulo: 'Canal' },
  { clave: 'aprobacion', titulo: 'Aprobación', valor: aprobacion },
  { clave: 'activa', titulo: 'Activa', valor: activa },
  { clave: 'version', titulo: 'Versión' },
  { clave: 'idioma', titulo: 'Idioma' },
  { clave: 'vigenteDesde', titulo: 'Vigente desde' },
  { clave: 'vigenteHasta', titulo: 'Vigente hasta', valor: (r: PlantillaEnLista) => r.vigenteHasta ?? NO_DISPONIBLE },
]

/**
 * CU-80 · Plantillas, de solo lectura: `notificaciones.yaml` no publica editar ni borrar una versión, así que no hay
 * acciones de fila. Una fila por versión, SIN asunto, cuerpo ni botones (el contrato no los trae). El estado de
 * aprobación viaja como `estados`; el buscador, el canal y «activa» filtran la página cargada.
 */
@Component({
  selector: 'ap-tabla-de-plantillas',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ChipEstado, Fecha, TablaPorCursor],
  template: `
    <ap-tabla-por-cursor [titulo]="t.plantillas.titulo" [columnas]="COLUMNAS" [listado]="listado" [identidad]="identidad" [exportables]="EXPORTABLES" nombreDeRegistros="versiones" [textoVacio]="t.plantillas.vacio" ayudaDeBusqueda="Busca por código, evento o idioma en la página cargada: el servidor no busca por texto.">
      <ng-template #celda let-r let-columna="columna">
        @switch (columna.clave) {
          @case ('estadoAprobacion') { <ap-chip-estado [tono]="r.estadoAprobacion === 'APROBADA' ? 'ok' : r.estadoAprobacion === 'RECHAZADA' ? 'error' : 'neutro'">{{ aprobacion(r) }}</ap-chip-estado> }
          @case ('activa') { {{ activa(r) }} }
          @case ('vigenteDesde') { <ap-fecha [iso]="r.vigenteDesde" /> }
          @case ('vigenteHasta') { @if (r.vigenteHasta) { <ap-fecha [iso]="r.vigenteHasta" /> } @else { {{ noDisponible }} } }
          @default { {{ r[columna.clave] ?? noDisponible }} }
        }
      </ng-template>
    </ap-tabla-por-cursor>
  `,
  styles: `:host { display: block; min-width: 0; }`,
})
export class TablaDePlantillas {
  protected readonly t = t
  protected readonly COLUMNAS = COLUMNAS
  protected readonly EXPORTABLES = EXPORTABLES
  protected readonly noDisponible = NO_DISPONIBLE
  protected readonly aprobacion = aprobacion
  protected readonly activa = activa
  protected readonly identidad = (r: PlantillaEnLista) => r.versionId
  protected readonly listado = crearListadoPorCursor<PlantillaEnLista>({
    cargador: cargadorDePlantillas(),
    camposDeBusqueda: ['codigo', 'evento', 'idioma'],
    servidor: { nombre: 'estado', etiqueta: 'Aprobación', opciones: opcionesDe(APROBACION) },
    filtros: [
      { nombre: 'canal', etiqueta: 'Canal', valor: (r) => r.canal },
      { nombre: 'activa', etiqueta: 'Activa', valor: (r) => (r.activa ? 'SI' : 'NO'), opciones: [{ valor: 'SI', texto: t.activa }, { valor: 'NO', texto: t.inactiva }] },
    ],
  })
}
