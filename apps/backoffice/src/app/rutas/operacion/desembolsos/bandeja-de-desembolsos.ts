import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core'
import { toSignal } from '@angular/core/rxjs-interop'
import { ActivatedRoute, Router } from '@angular/router'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import type { Filtro } from '@aportaya/ui/chips-de-filtro/chips-de-filtro'
import { Fecha } from '@aportaya/ui/fecha/fecha'
import { Monto } from '@aportaya/ui/monto/monto'
import { BarraDeFiltros } from '../../../nucleo/filtros/barra-de-filtros'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { TablaDeDatosVirtualizada } from '../../../nucleo/tabla/tabla-de-datos-virtualizada'
import type { ColumnaVirtual } from '../../../nucleo/tabla/tipos'
import { EstadoDeBandejaVista } from '../comun/estado-de-bandeja'
import { bandejaDeDesembolsos, type DesembolsoEnLista } from './dominio/cu28-desembolsos'
import { textosDesembolsos } from './textos'

const T = textosDesembolsos
type Estado = keyof typeof T.estados
const TONO: Record<Estado, 'ok' | 'aviso' | 'error' | 'info' | 'neutro'> = {
  CREADA: 'info', ENVIADA_A_PROVEEDOR: 'info', EN_PROCESO: 'aviso', ACREDITADA: 'ok', RECHAZADA: 'error', DEVUELTA_POR_BANCO: 'error', CANCELADA: 'neutro',
}
const CLAVES = ['creadaEn', 'monto', 'estado', 'intentos', 'ultimoResultado', 'ultimoCodigoError', 'reintentableEn', 'entregaId'] as const
const COLUMNAS: ColumnaVirtual<DesembolsoEnLista>[] = CLAVES.map((clave) => ({ clave, titulo: T.columnas[clave], numerica: clave === 'monto' || clave === 'intentos' }))

/**
 * CU-28 · bandeja de desembolsos (solo lectura).
 */
@Component({
  selector: 'ap-bandeja-de-desembolsos',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CabeceraDePagina, ChipEstado, Fecha, Monto, BarraDeFiltros, TablaDeDatosVirtualizada, EstadoDeBandejaVista],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina [titulo]="t.titulo" [descripcion]="t.proposito" icono="entrega" />
      <ap-barra-de-filtros [etiqueta]="t.filtroEtiqueta" [incluirBusqueda]="false" [definiciones]="filtrosDeEstado" />
      <ap-estado-de-bandeja [estado]="b.estado()" [error]="b.error()" [vacio]="t.vacio" [explicacionVacio]="t.explicacionVacio" [denegado]="t.denegado" [conFiltro]="!!filtrosActivos()['estados']" (reintentar)="b.reintentar()" (quitarFiltros)="quitarFiltros()" />
      @if (b.estado() !== 'denegado' && b.estado() !== 'vacio') {
        @for (v of [b.version()]; track v) {
          <ap-tabla-de-datos-virtualizada [titulo]="t.titulo" [columnas]="COLUMNAS" [cargador]="b.cargador" [ordenPermitido]="[]" [identidad]="identidad" [filtros]="filtrosActivos()" [tamanoDePagina]="25">
            <ng-template #celda let-r let-columna="columna">
              @switch (columna.clave) {
                @case ('creadaEn') { <ap-fecha [iso]="r.creadaEn" /> }
                @case ('reintentableEn') { @if (r.reintentableEn) { <ap-fecha [iso]="r.reintentableEn" /> } @else { {{ t.ninguno }} } }
                @case ('monto') { <ap-monto [monto]="r.monto.monto" [moneda]="r.monto.moneda" [etiqueta]="t.columnas.monto" /> }
                @case ('estado') { <ap-chip-estado [tono]="tonoDe(r)">{{ estadoDe(r) }}</ap-chip-estado> }
                @case ('ultimoResultado') { {{ r.ultimoResultado ? resultadoDe(r) : t.ninguno }} }
                @default { {{ r[columna.clave] ?? t.ninguno }} }
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
export class BandejaDeDesembolsos {
  constructor() {
    inject(ExplicadorDeVista).fijar(this.t.proposito)
  }
  private readonly router = inject(Router)
  private readonly ruta = inject(ActivatedRoute)
  protected readonly t = T
  protected readonly COLUMNAS = COLUMNAS
  protected readonly b = bandejaDeDesembolsos()
  protected readonly identidad = (r: DesembolsoEnLista) => r.ordenId
  protected readonly filtrosDeEstado: Filtro[] = (Object.keys(T.estados) as Estado[]).map((valor) => ({ valor, texto: T.estados[valor] }))
  private readonly queryParamMap = toSignal(this.ruta.queryParamMap, { requireSync: false })
  protected readonly filtrosActivos = computed<Record<string, string>>(() => {
    const elegidos = this.queryParamMap()?.get('filtro')?.split(',').filter(Boolean) ?? []
    const filtros: Record<string, string> = {}
    if (elegidos.length) filtros['estados'] = elegidos.join(',')
    return filtros
  })

  protected estadoDe(r: DesembolsoEnLista): string {
    return T.estados[r.estado as Estado]
  }

  protected tonoDe(r: DesembolsoEnLista): 'ok' | 'aviso' | 'error' | 'info' | 'neutro' {
    return TONO[r.estado as Estado]
  }

  protected resultadoDe(r: DesembolsoEnLista): string {
    return T.resultados[r.ultimoResultado as keyof typeof T.resultados]
  }

  protected quitarFiltros(): void {
    void this.router.navigate([], { relativeTo: this.ruta, queryParams: { filtro: null }, queryParamsHandling: 'merge' })
  }
}
