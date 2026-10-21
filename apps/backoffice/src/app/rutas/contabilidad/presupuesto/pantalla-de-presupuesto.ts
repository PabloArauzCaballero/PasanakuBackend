import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core'
import { toSignal } from '@angular/core/rxjs-interop'
import { ActivatedRoute } from '@angular/router'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { Monto } from '@aportaya/ui/monto/monto'
import type { Filtro } from '@aportaya/ui/chips-de-filtro/chips-de-filtro'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { BarraDeFiltros } from '../../../nucleo/filtros/barra-de-filtros'
import { TablaDeDatosVirtualizada } from '../../../nucleo/tabla/tabla-de-datos-virtualizada'
import type { ColumnaVirtual } from '../../../nucleo/tabla/tipos'
import { calcularDesviacion, cargadorDePartidas, sobreEjecutada, type PartidaPresupuestaria } from '../dominio/cu101-presupuestos'
import { nombreDelPeriodo } from '../dominio/cu100-periodos'
import { EstadoDeListado, observarListado } from '../listado/estado-de-listado'
import { textosContabilidad } from '../textos'

const COLUMNAS: ColumnaVirtual<PartidaPresupuestaria>[] = [
  { clave: 'centroCostoNombre', titulo: 'Centro de costo' },
  { clave: 'presupuestoNombre', titulo: 'Presupuesto' },
  { clave: 'fechaInicioPeriodo', titulo: 'Período' },
  { clave: 'montoPresupuestado', titulo: 'Presupuestado', numerica: true },
  { clave: 'montoEjecutado', titulo: 'Ejecutado', numerica: true },
  { clave: 'partidaId', titulo: 'Desviación', numerica: true },
  { clave: 'estado', titulo: 'Estado' },
]

/**
 * CU-101 · Presupuestar por centro de costo, sobre `TablaDeDatosVirtualizada` (la del
 * shell: no se duplica ninguna grilla), paginada por cursor en el orden del servidor
 * (inicio del período, descendente): no se ordena por columna. La desviación se resalta.
 *
 * El presupuesto **mide, no bloquea** (CU-101, 4a): pasarse no deshabilita nada acá,
 * registra una desviación que alguien tiene que explicar. Todos los importes, incluida
 * la desviación, pasan por el átomo `Monto`.
 */
@Component({
  selector: 'ap-pantalla-de-presupuesto',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BarraDeFiltros, CabeceraDePagina, ChipEstado, EstadoDeListado, Monto, TablaDeDatosVirtualizada],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina [titulo]="t.titulo" [descripcion]="t.proposito" icono="comision" />
      <p class="aviso">{{ t.noBloquea }}</p>
      <ap-barra-de-filtros etiqueta="Estado" [incluirBusqueda]="false" [definiciones]="filtros" />
      <ap-estado-de-listado [error]="listado.error()" [vacio]="listado.vacio()" [mensajeVacio]="t.enCero" [filtrado]="!!filtrosActivos()['estado']" />
      @if (!listado.error() && !listado.vacio()) {
        <ap-tabla-de-datos-virtualizada
          [titulo]="t.titulo"
          [columnas]="COLUMNAS"
          [cargador]="listado.cargador"
          [ordenPermitido]="[]"
          [identidad]="identidad"
          [filtros]="filtrosActivos()"
        >
          <ng-template #celda let-p let-columna="columna">
            @switch (columna.clave) {
              @case ('montoPresupuestado') {
                <ap-monto [monto]="p.montoPresupuestado.monto" [moneda]="p.montoPresupuestado.moneda" [etiqueta]="t.presupuestado" />
              }
              @case ('montoEjecutado') {
                <ap-monto [monto]="p.montoEjecutado.monto" [moneda]="p.montoEjecutado.moneda" [etiqueta]="t.ejecutado" />
              }
              @case ('partidaId') {
                <span class="desviacion" [class.sobre]="sobre(p)">
                  <ap-monto [monto]="desviacion(p).monto" [moneda]="desviacion(p).moneda" [etiqueta]="t.desviacion" />
                  @if (sobre(p)) { <span class="marca">{{ t.sobreEjecutado }}</span> }
                </span>
              }
              @case ('fechaInicioPeriodo') {
                {{ periodo(p) }}
              }
              @case ('estado') {
                <ap-chip-estado [tono]="p.estado === 'APROBADO' ? 'ok' : 'neutro'">{{ etiqueta(p) }}</ap-chip-estado>
              }
              @default {
                {{ p[columna.clave] }}
              }
            }
          </ng-template>
        </ap-tabla-de-datos-virtualizada>
      }
    </section>
  `,
  styles: `
    .pantalla { padding: var(--s5); max-width: 72rem; display: grid; gap: var(--s4); }
    .aviso { color: var(--text-2); margin: 0; }
    .desviacion { display: inline-flex; align-items: center; gap: var(--s2); justify-content: flex-end; }
    .desviacion.sobre { color: var(--err-texto); }
    .marca { font-size: .8em; }
  `,
})
export class PantallaDePresupuesto {
  constructor() {
    inject(ExplicadorDeVista).fijar(this.t.proposito)
  }

  protected readonly t = textosContabilidad.presupuesto
  protected readonly COLUMNAS = COLUMNAS
  protected readonly listado = observarListado(cargadorDePartidas(), textosContabilidad.erroresDeListado)
  protected readonly periodo = (p: PartidaPresupuestaria) => nombreDelPeriodo(p.anio, p.mes)
  protected readonly identidad = (p: PartidaPresupuestaria) => p.partidaId
  protected readonly desviacion = calcularDesviacion
  protected readonly sobre = sobreEjecutada
  protected readonly etiqueta = (p: PartidaPresupuestaria) => this.t.estados[p.estado]
  protected readonly filtros: Filtro[] = [
    { valor: 'BORRADOR', texto: 'Borrador' },
    { valor: 'APROBADO', texto: 'Aprobado' },
    { valor: 'CERRADO', texto: 'Cerrado' },
  ]
  private readonly ruta = inject(ActivatedRoute)
  private readonly queryParamMap = toSignal(this.ruta.queryParamMap, { requireSync: false })
  protected readonly filtrosActivos = computed<Record<string, string>>(() => {
    const elegido = this.queryParamMap()?.get('filtro')?.split(',').filter(Boolean)[0]
    const filtros: Record<string, string> = {}
    if (elegido) filtros['estado'] = elegido
    return filtros
  })
}
