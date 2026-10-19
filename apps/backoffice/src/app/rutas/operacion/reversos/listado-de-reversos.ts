import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core'
import { toSignal } from '@angular/core/rxjs-interop'
import { ActivatedRoute, Router } from '@angular/router'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import type { Filtro } from '@aportaya/ui/chips-de-filtro/chips-de-filtro'
import { Fecha } from '@aportaya/ui/fecha/fecha'
import { BarraDeFiltros } from '../../../nucleo/filtros/barra-de-filtros'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { TablaDeDatosVirtualizada } from '../../../nucleo/tabla/tabla-de-datos-virtualizada'
import type { ColumnaVirtual } from '../../../nucleo/tabla/tipos'
import { EstadoDeBandejaVista } from '../comun/estado-de-bandeja'
import { ImporteSinMoneda } from '../comun/importe-sin-moneda'
import { bandejaDeReversos, type ReversoEnLista } from './dominio/cu14-reversos'
import { textosReversos } from './textos'

const T = textosReversos
type Estado = keyof typeof T.estados
const TONO: Record<Estado, 'ok' | 'aviso' | 'error' | 'info'> = { SOLICITADO: 'info', AUTORIZADO: 'aviso', EJECUTADO: 'ok', RECHAZADO: 'error' }
const CLAVES = ['solicitadaEn', 'tipo', 'estado', 'montoReversado', 'motivo', 'autorizadaPor', 'transaccionOriginalId', 'ejecutadaEn'] as const
const COLUMNAS: ColumnaVirtual<ReversoEnLista>[] = CLAVES.map((clave) => ({ clave, titulo: T.columnas[clave], numerica: clave === 'montoReversado' }))

/**
 * CU-14 · listado de reversos de solo lectura para Tesorería y Auditoría (AUDITORIA_LEER).
 */
@Component({
  selector: 'ap-listado-de-reversos',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CabeceraDePagina, ChipEstado, Fecha, BarraDeFiltros, TablaDeDatosVirtualizada, EstadoDeBandejaVista, ImporteSinMoneda],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina [titulo]="t.titulo" [descripcion]="t.proposito" icono="recarga" />
      <ap-barra-de-filtros [etiqueta]="t.filtroEtiqueta" [incluirBusqueda]="false" [definiciones]="filtrosDeEstado" />
      <ap-estado-de-bandeja [estado]="b.estado()" [error]="b.error()" [vacio]="t.vacio" [explicacionVacio]="t.explicacionVacio" [denegado]="t.denegado" [conFiltro]="!!filtrosActivos()['estados']" (reintentar)="b.reintentar()" (quitarFiltros)="quitarFiltros()" />
      @if (b.estado() !== 'denegado') {
        @for (v of [b.version()]; track v) {
          <ap-tabla-de-datos-virtualizada [titulo]="t.titulo" [columnas]="COLUMNAS" [cargador]="b.cargador" [ordenPermitido]="[]" [identidad]="identidad" [filtros]="filtrosActivos()" [tamanoDePagina]="25">
            <ng-template #celda let-r let-columna="columna">
              @switch (columna.clave) {
                @case ('solicitadaEn') { <ap-fecha [iso]="r.solicitadaEn" /> }
                @case ('ejecutadaEn') { @if (r.ejecutadaEn) { <ap-fecha [iso]="r.ejecutadaEn" /> } @else { {{ t.pendiente }} } }
                @case ('tipo') { {{ tipoDe(r) }} }
                @case ('estado') { <ap-chip-estado [tono]="tonoDe(r)">{{ estadoDe(r) }}</ap-chip-estado> }
                @case ('montoReversado') { <ap-importe-sin-moneda [monto]="r.montoReversado" /> }
                @default { {{ r[columna.clave] }} }
              }
            </ng-template>
          </ap-tabla-de-datos-virtualizada>
        }
      }
    </section>
  `,
  styles: `
    .pantalla { padding: var(--s5); max-width: 80rem; display: grid; gap: var(--s4); }
  `,
})
export class ListadoDeReversos {
  constructor() {
    inject(ExplicadorDeVista).fijar(this.t.proposito)
  }
  private readonly router = inject(Router)
  private readonly ruta = inject(ActivatedRoute)
  protected readonly t = T
  protected readonly COLUMNAS = COLUMNAS
  protected readonly b = bandejaDeReversos()
  protected readonly identidad = (r: ReversoEnLista) => r.reversoId
  protected readonly filtrosDeEstado: Filtro[] = (Object.keys(T.estados) as Estado[]).map((valor) => ({ valor, texto: T.estados[valor] }))
  private readonly queryParamMap = toSignal(this.ruta.queryParamMap, { requireSync: false })
  protected readonly filtrosActivos = computed<Record<string, string>>(() => {
    const elegidos = this.queryParamMap()?.get('filtro')?.split(',').filter(Boolean) ?? []
    const filtros: Record<string, string> = {}
    if (elegidos.length) filtros['estados'] = elegidos.join(',')
    return filtros
  })

  protected estadoDe(r: ReversoEnLista): string {
    return T.estados[r.estado as Estado]
  }

  protected tonoDe(r: ReversoEnLista): 'ok' | 'aviso' | 'error' | 'info' {
    return TONO[r.estado as Estado]
  }

  protected tipoDe(r: ReversoEnLista): string {
    return T.tipos[r.tipo as keyof typeof T.tipos]
  }

  protected quitarFiltros(): void {
    void this.router.navigate([], { relativeTo: this.ruta, queryParams: { filtro: null }, queryParamsHandling: 'merge' })
  }
}
