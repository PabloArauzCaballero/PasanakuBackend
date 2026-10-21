import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core'
import { toSignal } from '@angular/core/rxjs-interop'
import { ActivatedRoute, Router } from '@angular/router'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { Boton } from '@aportaya/ui/boton/boton'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import type { Filtro } from '@aportaya/ui/chips-de-filtro/chips-de-filtro'
import { RelojDePlazo } from '@aportaya/ui/reloj-de-plazo/reloj-de-plazo'
import { BarraDeFiltros } from '../../../nucleo/filtros/barra-de-filtros'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { Sesion } from '../../../nucleo/sesion'
import { TablaDeDatosVirtualizada } from '../../../nucleo/tabla/tabla-de-datos-virtualizada'
import type { ColumnaVirtual } from '../../../nucleo/tabla/tipos'
import { EstadoDeBandejaVista } from '../comun/estado-de-bandeja'
import { ImporteSinMoneda } from '../comun/importe-sin-moneda'
import { bandejaDeReclamos, sePuedeResponder, type EstadoReclamo, type ReclamoEnBandeja } from '../dominio/cu52-reclamos'
import { RespuestaAReclamo } from './respuesta-a-reclamo'
import { textosReclamos } from './textos'

const T = textosReclamos
const TONO: Record<EstadoReclamo, 'ok' | 'aviso' | 'error' | 'info' | 'neutro'> = { INGRESADO: 'info', EN_ANALISIS: 'aviso', RESPONDIDO: 'ok', CERRADO: 'neutro', ELEVADO: 'error' }
const COLUMNAS: ColumnaVirtual<ReclamoEnBandeja>[] = (['codigo', 'categoria', 'canalIngreso', 'montoReclamado', 'limiteVigente', 'estado', 'reclamoId'] as const).map((clave) => ({
  clave,
  titulo: T.columnas[clave],
  numerica: clave === 'montoReclamado',
}))

/**
 * D-18 · Bandeja de reclamos (CU-52) sobre `listarBandejaDeReclamos`.
 */
@Component({
  selector: 'ap-bandeja-de-reclamos',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Alerta, Boton, CabeceraDePagina, ChipEstado, RelojDePlazo, TablaDeDatosVirtualizada, BarraDeFiltros, EstadoDeBandejaVista, ImporteSinMoneda, RespuestaAReclamo],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina data-tutorial-id="operacion-reclamos" [titulo]="t.titulo" [descripcion]="t.proposito" icono="advertencia" />
      @if (hecho(); as h) { <ap-alerta tono="ok">{{ h }}</ap-alerta> }
      <ap-barra-de-filtros [etiqueta]="t.filtroEtiqueta" [incluirBusqueda]="false" [definiciones]="filtrosDeEstado" />
      <ap-estado-de-bandeja [estado]="b.estado()" [error]="b.error()" [vacio]="t.vacio" [explicacionVacio]="t.explicacionVacio" [denegado]="t.denegado" [conFiltro]="!!filtrosActivos()['estados']" (reintentar)="b.reintentar()" (quitarFiltros)="quitarFiltros()" />
      @if (b.estado() !== 'denegado' && b.estado() !== 'vacio') {
        @for (v of [b.version()]; track v) {
          <ap-tabla-de-datos-virtualizada [titulo]="t.titulo" [columnas]="COLUMNAS" [cargador]="b.cargador" [ordenPermitido]="[]" [identidad]="identidad" [filtros]="filtrosActivos()" [tamanoDePagina]="25">
            <ng-template #celda let-r let-columna="columna">
              @switch (columna.clave) {
                @case ('estado') { <ap-chip-estado [tono]="tonoDe(r)">{{ etiquetaDe(r) }}</ap-chip-estado> }
                @case ('limiteVigente') {
                  <ap-reloj-de-plazo [etiqueta]="t.plazo" [venceIso]="r.limiteVigente" [ahoraIso]="ahoraIso" [norma]="t.norma" />
                  @if (r.vencido) { <span class="vencido">{{ t.vencido }}</span> }
                }
                @case ('montoReclamado') {
                  @if (r.montoReclamado) { <ap-importe-sin-moneda [monto]="r.montoReclamado" /> } @else { {{ t.sinMonto }} }
                }
                @case ('reclamoId') {
                  @if (puedeAtender() && sePuedeResponder(r)) { <ap-boton (pulsado)="abrir(r)">{{ t.responder }}</ap-boton> }
                }
                @default { {{ r[columna.clave] }} }
              }
            </ng-template>
          </ap-tabla-de-datos-virtualizada>
        }
      }
    </section>
    @if (seleccionado(); as r) {
      <ap-respuesta-a-reclamo [reclamo]="r" (respondido)="alResponder(r)" (cerrar)="seleccionado.set(null)" />
    }
  `,
  styles: `
    .pantalla { padding: var(--s5); max-width: 72rem; display: grid; gap: var(--s4); }
    .vencido { display: block; color: var(--err-texto); font-weight: 600; }
  `,
})
export class BandejaDeReclamos {
  constructor() {
    inject(ExplicadorDeVista).fijar(this.t.proposito)
  }
  private readonly sesion = inject(Sesion)
  private readonly router = inject(Router)
  private readonly ruta = inject(ActivatedRoute)
  protected readonly t = T
  protected readonly COLUMNAS = COLUMNAS
  protected readonly ahoraIso = new Date().toISOString()
  protected readonly b = bandejaDeReclamos()
  protected readonly identidad = (r: ReclamoEnBandeja) => r.reclamoId
  protected readonly sePuedeResponder = sePuedeResponder
  protected readonly filtrosDeEstado: Filtro[] = (Object.keys(T.estados) as EstadoReclamo[]).map((valor) => ({ valor, texto: T.estados[valor] }))
  private readonly queryParamMap = toSignal(this.ruta.queryParamMap, { requireSync: false })
  /** `?filtro=A,B` → `{ estados: 'A,B' }`: el cargador lo manda tal cual al servidor. */
  protected readonly filtrosActivos = computed<Record<string, string>>(() => {
    const elegidos = this.queryParamMap()?.get('filtro')?.split(',').filter(Boolean) ?? []
    const filtros: Record<string, string> = {}
    if (elegidos.length) filtros['estados'] = elegidos.join(',')
    return filtros
  })
  protected readonly puedeAtender = computed(() => this.sesion.puede('RECLAMO_ATENDER'))
  protected readonly seleccionado = signal<ReclamoEnBandeja | null>(null)
  protected readonly hecho = signal('')

  protected tonoDe(r: ReclamoEnBandeja): 'ok' | 'aviso' | 'error' | 'info' | 'neutro' {
    return TONO[r.estado as EstadoReclamo]
  }

  protected etiquetaDe(r: ReclamoEnBandeja): string {
    return T.estados[r.estado as EstadoReclamo]
  }

  protected abrir(r: ReclamoEnBandeja): void {
    if (!this.puedeAtender() || !sePuedeResponder(r)) return
    this.hecho.set('')
    this.seleccionado.set(r)
  }

  /** El éxito se anuncia con el 200 y la bandeja se vuelve a pedir: el reclamo cerrado sale de los abiertos. */
  protected alResponder(r: ReclamoEnBandeja): void {
    this.seleccionado.set(null)
    this.hecho.set(T.respuesta.hecho(r.codigo))
    this.b.reintentar()
  }

  protected quitarFiltros(): void {
    void this.router.navigate([], { relativeTo: this.ruta, queryParams: { filtro: null }, queryParamsHandling: 'merge' })
  }
}
