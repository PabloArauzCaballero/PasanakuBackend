import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { Monto } from '@aportaya/ui/monto/monto'
import { TarjetaKPI } from '@aportaya/ui/tarjeta-kpi/tarjeta-kpi'
import type { Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { textoDeImporte } from '../dominio/contrato-erp'
import { calcularDesviacion, cargadorDePartidas, sobreEjecutada, type PartidaPresupuestaria } from '../dominio/cu101-presupuestos'
import { nombreDelPeriodo } from '../dominio/cu100-periodos'
import { crearListadoPorCursor, filtroInicialDeLaUrl, opcionesDe } from '../listado/listado-por-cursor'
import { TablaPorCursor } from '../listado/tabla-por-cursor'
import { textosContabilidad } from '../textos'

const t = textosContabilidad.presupuesto
const periodo = (p: PartidaPresupuestaria) => nombreDelPeriodo(p.anio, p.mes)

const COLUMNAS: Columna<PartidaPresupuestaria>[] = [
  { clave: 'centroCostoNombre', titulo: 'Centro de costo' },
  { clave: 'presupuestoNombre', titulo: 'Presupuesto' },
  { clave: 'fechaInicioPeriodo', titulo: 'Período' },
  { clave: 'montoPresupuestado', titulo: 'Presupuestado', numerica: true },
  { clave: 'montoEjecutado', titulo: 'Ejecutado', numerica: true },
  { clave: 'partidaId', titulo: 'Desviación', numerica: true },
  { clave: 'estado', titulo: 'Estado' },
]

/** Los importes salen como la cadena del contrato con su moneda: nunca un `Number` (regla 91.1.1). */
const EXPORTABLES = [
  { clave: 'centroCosto', titulo: 'Centro de costo', valor: (p: PartidaPresupuestaria) => p.centroCostoNombre },
  { clave: 'presupuesto', titulo: 'Presupuesto', valor: (p: PartidaPresupuestaria) => p.presupuestoNombre },
  { clave: 'periodo', titulo: 'Período', valor: periodo },
  { clave: 'presupuestado', titulo: 'Presupuestado', valor: (p: PartidaPresupuestaria) => textoDeImporte(p.montoPresupuestado) },
  { clave: 'ejecutado', titulo: 'Ejecutado', valor: (p: PartidaPresupuestaria) => textoDeImporte(p.montoEjecutado) },
  { clave: 'desviacion', titulo: 'Desviación', valor: (p: PartidaPresupuestaria) => textoDeImporte(calcularDesviacion(p)) },
  { clave: 'estado', titulo: 'Estado', valor: (p: PartidaPresupuestaria) => t.estados[p.estado] },
]

/**
 * CU-101 · Presupuestar por centro de costo. Listado por cursor del servidor (`listarPartidasPresupuestarias`, inicio del
 * período descendente) con `crearListadoPorCursor`: el estado del presupuesto viaja como `estados`; el buscador, el centro
 * de costo y la desviación filtran la página cargada. **Solo lectura**: `erp.yaml` no tiene editar ni borrar partidas, y
 * aprobar es del presupuesto entero (no de una fila), así que la tabla no lleva acciones.
 *
 * El presupuesto **mide, no bloquea** (CU-101, 4a): pasarse no deshabilita nada, registra una desviación que alguien
 * tiene que explicar. Todos los importes, incluida la desviación, pasan por `ap-monto`. Las cifras de arriba son
 * **conteos** de la página: sumar importes acá sería un total ad-hoc (regla 91.1.5).
 */
@Component({
  selector: 'ap-pantalla-de-presupuesto',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Alerta, CabeceraDePagina, ChipEstado, Monto, TablaPorCursor, TarjetaKPI],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina eyebrow="Contabilidad · CU-101" [titulo]="t.titulo" [descripcion]="t.proposito" icono="grafico" />
      <div class="kpis">
        <ap-tarjeta-kpi etiqueta="Partidas en la página" [valor]="'' + listado.filas().length" icono="lista" />
        <ap-tarjeta-kpi etiqueta="Sobre lo presupuestado" [valor]="'' + sobreEnPagina()" icono="advertencia" [tono]="sobreEnPagina() > 0 ? 'error' : 'neutro'" pista="Desviaciones a explicar" />
        <ap-tarjeta-kpi etiqueta="Aprobadas" [valor]="'' + aprobadasEnPagina()" icono="aprobar" tono="ok" />
      </div>
      <ap-alerta tono="info" titulo="Mide, no bloquea">{{ t.noBloquea }}</ap-alerta>
      <ap-tabla-por-cursor [titulo]="t.titulo" [columnas]="COLUMNAS" [listado]="listado" [identidad]="identidad" [exportables]="EXPORTABLES" nombreDeRegistros="partidas" [textoVacio]="t.enCero" explicacionVacio="Las partidas aparecen cuando el Directorio aprueba un presupuesto para el ejercicio." ayudaDeBusqueda="Busca por centro de costo, presupuesto o período en la página cargada: el servidor no busca por texto.">
        <ng-template #celda let-p let-columna="columna">
          @switch (columna.clave) {
            @case ('montoPresupuestado') { <ap-monto [monto]="p.montoPresupuestado.monto" [moneda]="p.montoPresupuestado.moneda" [etiqueta]="t.presupuestado" /> }
            @case ('montoEjecutado') { <ap-monto [monto]="p.montoEjecutado.monto" [moneda]="p.montoEjecutado.moneda" [etiqueta]="t.ejecutado" /> }
            @case ('partidaId') {
              <span class="desviacion" [class.sobre]="sobre(p)">
                <ap-monto [monto]="desviacion(p).monto" [moneda]="desviacion(p).moneda" [etiqueta]="t.desviacion" />
                @if (sobre(p)) { <span class="marca">{{ t.sobreEjecutado }}</span> }
              </span>
            }
            @case ('fechaInicioPeriodo') { {{ periodo(p) }} }
            @case ('estado') { <ap-chip-estado [tono]="p.estado === 'APROBADO' ? 'ok' : 'neutro'">{{ etiqueta(p) }}</ap-chip-estado> }
            @default { {{ p[columna.clave] }} }
          }
        </ng-template>
      </ap-tabla-por-cursor>
    </section>
  `,
  styles: `
    .pantalla { padding: var(--s5); display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s4); min-width: 0; }
    .kpis { display: grid; grid-template-columns: repeat(auto-fit, minmax(min(100%, 14rem), 1fr)); gap: var(--s3); }
    .desviacion { display: inline-flex; flex-wrap: wrap; align-items: center; gap: var(--s2); justify-content: flex-end; }
    .desviacion.sobre { color: var(--err-texto); }
    .marca { font: var(--t-ayuda); }
  `,
})
export class PantallaDePresupuesto {
  constructor() {
    inject(ExplicadorDeVista).fijar(t.proposito)
  }

  protected readonly t = t
  protected readonly COLUMNAS = COLUMNAS
  protected readonly EXPORTABLES = EXPORTABLES
  protected readonly periodo = periodo
  protected readonly identidad = (p: PartidaPresupuestaria) => p.partidaId
  protected readonly etiqueta = (p: PartidaPresupuestaria) => t.estados[p.estado]
  protected readonly desviacion = calcularDesviacion
  protected readonly sobre = sobreEjecutada
  protected readonly listado = crearListadoPorCursor<PartidaPresupuestaria>({
    cargador: cargadorDePartidas(),
    camposDeBusqueda: ['centroCostoNombre', 'presupuestoNombre', periodo],
    servidor: { nombre: 'estado', etiqueta: 'Estado del presupuesto', opciones: opcionesDe(t.estados), inicial: filtroInicialDeLaUrl(opcionesDe(t.estados)) },
    filtros: [
      { nombre: 'centro', etiqueta: 'Centro de costo', valor: (p) => p.centroCostoNombre },
      { nombre: 'desviacion', etiqueta: 'Desviación', valor: (p) => (sobreEjecutada(p) ? 'SOBRE' : 'DENTRO'), opciones: [{ valor: 'SOBRE', texto: 'Sobre lo presupuestado' }, { valor: 'DENTRO', texto: 'Dentro de lo presupuestado' }] },
    ],
    textosDeError: textosContabilidad.erroresDeListado,
  })
  protected readonly sobreEnPagina = computed(() => this.listado.filas().filter(sobreEjecutada).length)
  protected readonly aprobadasEnPagina = computed(() => this.listado.filas().filter((p) => p.estado === 'APROBADO').length)
}
