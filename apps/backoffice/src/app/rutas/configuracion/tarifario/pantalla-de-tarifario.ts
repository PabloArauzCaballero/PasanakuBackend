import { ChangeDetectionStrategy, Component, inject } from '@angular/core'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { Fecha } from '@aportaya/ui/fecha/fecha'
import type { Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import type { Tono } from '@aportaya/ui/tono/tono'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { crearListadoPorCursor, filtroInicialDeLaUrl, opcionesDe } from '../../contabilidad/listado/listado-por-cursor'
import { TablaPorCursor } from '../../contabilidad/listado/tabla-por-cursor'
import { cargadorDeTarifarios, type TarifarioEnLista } from '../dominio/cu34-tarifarios'
import { NO_DISPONIBLE } from '../dominio/listado'
import { textosConfiguracion } from '../textos'

const t = textosConfiguracion.tarifario
const ESTADOS: Record<string, string> = t.estados
const simulacion = (r: TarifarioEnLista) => (r.simulacion ? `${r.simulacion.deltaIngresoEstimado} (${t.deltaSinMoneda}) · ${r.simulacion.usuariosImpactados} ${t.usuarios}` : NO_DISPONIBLE)

const COLUMNAS: Columna<TarifarioEnLista>[] = [
  { clave: 'codigo', titulo: 'Código', mono: true },
  { clave: 'version', titulo: 'Versión', numerica: true },
  { clave: 'nombre', titulo: 'Nombre' },
  { clave: 'estado', titulo: 'Estado' },
  { clave: 'monedaBase', titulo: 'Moneda' },
  { clave: 'vigenteDesde', titulo: 'Vigente desde' },
  { clave: 'vigenteHasta', titulo: 'Vigente hasta' },
  { clave: 'diasPreaviso', titulo: 'Preaviso (días)', numerica: true },
  { clave: 'simulacion', titulo: 'Última simulación' },
]

const EXPORTABLES = [
  { clave: 'codigo', titulo: 'Código' },
  { clave: 'version', titulo: 'Versión' },
  { clave: 'nombre', titulo: 'Nombre' },
  { clave: 'estado', titulo: 'Estado', valor: (r: TarifarioEnLista) => ESTADOS[r.estado] ?? r.estado },
  { clave: 'monedaBase', titulo: 'Moneda' },
  { clave: 'vigenteDesde', titulo: 'Vigente desde' },
  { clave: 'vigenteHasta', titulo: 'Vigente hasta', valor: (r: TarifarioEnLista) => r.vigenteHasta ?? NO_DISPONIBLE },
  { clave: 'diasPreaviso', titulo: 'Preaviso (días)' },
  { clave: 'simulacion', titulo: 'Última simulación', valor: simulacion },
]

const TONO: Record<TarifarioEnLista['estado'], Tono> = { VIGENTE: 'ok', EN_PREAVISO: 'aviso', BORRADOR: 'info', SUSTITUIDO: 'neutro', ARCHIVADO: 'neutro' }

/**
 * CU-34 · Historial del tarifario, **de solo lectura** (carril L4): `tarifas.yaml` publica `publicarTarifario` y
 * `ponerTarifarioVigente`, pero no editar ni borrar una versión, y esta pantalla no publica — así que no hay Editar ni
 * Eliminar. Listado por cursor del servidor (vigencia descendente): el estado viaja como `estados`; el buscador y la
 * moneda filtran la página cargada. `simulacion.deltaIngresoEstimado` viaja SIN moneda: se muestra como texto, no como
 * importe. Un campo opcional ausente se dice «no disponible».
 */
@Component({
  selector: 'ap-pantalla-de-tarifario',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Alerta, CabeceraDePagina, ChipEstado, Fecha, TablaPorCursor],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina eyebrow="Configuración · CU-34" [titulo]="t.titulo" [descripcion]="t.proposito" icono="etiqueta" />
      <ap-alerta tono="info">{{ comun.soloLectura }}</ap-alerta>
      <ap-tabla-por-cursor [titulo]="t.titulo" [columnas]="COLUMNAS" [listado]="listado" [identidad]="identidad" [exportables]="EXPORTABLES" nombreDeRegistros="versiones" [textoVacio]="t.vacio" ayudaDeBusqueda="Busca por código o nombre en la página cargada: el servidor no busca por texto.">
        <ng-template #celda let-r let-columna="columna">
          @switch (columna.clave) {
            @case ('estado') { <ap-chip-estado [tono]="tonoDe(r)">{{ etiqueta(r) }}</ap-chip-estado> }
            @case ('vigenteDesde') { <ap-fecha [iso]="r.vigenteDesde" /> }
            @case ('vigenteHasta') { @if (r.vigenteHasta) { <ap-fecha [iso]="r.vigenteHasta" /> } @else { {{ noDisponible }} } }
            @case ('simulacion') { {{ simulacion(r) }} }
            @default { {{ r[columna.clave] ?? noDisponible }} }
          }
        </ng-template>
      </ap-tabla-por-cursor>
    </section>
  `,
  styles: `
    .pantalla { padding: var(--s5); display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s4); min-width: 0; }
  `,
})
export class PantallaDeTarifario {
  constructor() {
    inject(ExplicadorDeVista).fijar(t.proposito)
  }

  protected readonly t = t
  protected readonly comun = textosConfiguracion.comun
  protected readonly COLUMNAS = COLUMNAS
  protected readonly EXPORTABLES = EXPORTABLES
  protected readonly noDisponible = NO_DISPONIBLE
  protected readonly simulacion = simulacion
  protected readonly identidad = (r: TarifarioEnLista) => r.tarifarioId
  protected readonly etiqueta = (r: TarifarioEnLista) => ESTADOS[r.estado] ?? r.estado
  protected readonly listado = crearListadoPorCursor<TarifarioEnLista>({
    cargador: cargadorDeTarifarios(),
    camposDeBusqueda: ['codigo', 'nombre'],
    servidor: { nombre: 'estado', etiqueta: textosConfiguracion.comun.estado, opciones: opcionesDe(ESTADOS), inicial: filtroInicialDeLaUrl(opcionesDe(ESTADOS)) },
    filtros: [{ nombre: 'moneda', etiqueta: 'Moneda', valor: (r) => r.monedaBase }],
  })
  protected tonoDe(r: TarifarioEnLista): Tono {
    return TONO[r.estado] ?? 'neutro'
  }
}
