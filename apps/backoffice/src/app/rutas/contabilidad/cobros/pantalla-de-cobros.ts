import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core'
import { toSignal } from '@angular/core/rxjs-interop'
import { ActivatedRoute } from '@angular/router'
import { Boton } from '@aportaya/ui/boton/boton'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { Fecha } from '@aportaya/ui/fecha/fecha'
import { Monto } from '@aportaya/ui/monto/monto'
import type { Filtro } from '@aportaya/ui/chips-de-filtro/chips-de-filtro'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { BarraDeFiltros } from '../../../nucleo/filtros/barra-de-filtros'
import { Sesion } from '../../../nucleo/sesion'
import { TablaDeDatosVirtualizada } from '../../../nucleo/tabla/tabla-de-datos-virtualizada'
import type { ColumnaVirtual } from '../../../nucleo/tabla/tipos'
import { cargadorDeCuentas, motivoParaNoCobrar, type CuentaPorCobrar } from '../dominio/cu104-cobros'
import { FichaDeCobro } from './ficha-de-cobro'
import { EstadoDeListado, observarListado } from '../listado/estado-de-listado'
import { textosContabilidad } from '../textos'

const COLUMNAS: ColumnaVirtual<CuentaPorCobrar>[] = [
  { clave: 'origenTipo', titulo: 'Origen' },
  { clave: 'terceroRazonSocial', titulo: 'Tercero' },
  { clave: 'fechaVencimiento', titulo: 'Vence' },
  { clave: 'monto', titulo: 'Monto', numerica: true },
  { clave: 'saldoPendiente', titulo: 'Saldo pendiente', numerica: true },
  { clave: 'estado', titulo: 'Estado' },
  { clave: 'cuentaPorCobrarId', titulo: 'Cobro' },
]

/**
 * CU-104 · Cuentas por cobrar, sobre `TablaDeDatosVirtualizada` (la del shell), paginada
 * por cursor en el orden del servidor (vencimiento descendente). Una cuenta `INCOBRABLE` no ofrece cobrar: muestra
 * el motivo, porque el `AP-CU104-03` del backend tiene que confirmar la decisión, no
 * explicarla por primera vez.
 */
@Component({
  selector: 'ap-pantalla-de-cobros',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BarraDeFiltros, Boton, CabeceraDePagina, ChipEstado, EstadoDeListado, Fecha, Monto, TablaDeDatosVirtualizada, FichaDeCobro],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina [titulo]="t.titulo" [descripcion]="t.proposito" icono="recarga" />
      <ap-barra-de-filtros etiqueta="Estado" [incluirBusqueda]="false" [definiciones]="filtros" />
      <ap-estado-de-listado [error]="listado.error()" [vacio]="listado.vacio()" [mensajeVacio]="t.enCero" [filtrado]="!!filtrosActivos()['estado']" />
      <ap-tabla-de-datos-virtualizada
        [titulo]="t.titulo"
        [columnas]="COLUMNAS"
        [cargador]="listado.cargador"
        [ordenPermitido]="[]"
        [identidad]="identidad"
        [filtros]="filtrosActivos()"
      >
        <ng-template #celda let-c let-columna="columna">
          @switch (columna.clave) {
            @case ('origenTipo') {
              {{ origen(c) }}
            }
            @case ('terceroRazonSocial') {
              {{ c.terceroRazonSocial ?? t.sinTercero }}
            }
            @case ('fechaVencimiento') {
              <ap-fecha [iso]="c.fechaVencimiento" [conHora]="false" />
            }
            @case ('monto') {
              <ap-monto [monto]="c.monto.monto" [moneda]="c.monto.moneda" etiqueta="Monto de la cuenta" />
            }
            @case ('saldoPendiente') {
              <ap-monto [monto]="c.saldoPendiente.monto" [moneda]="c.saldoPendiente.moneda" [etiqueta]="t.saldoPendiente" />
            }
            @case ('estado') {
              <ap-chip-estado [tono]="c.estado === 'COBRADA' ? 'ok' : c.estado === 'INCOBRABLE' ? 'error' : 'aviso'">{{ etiqueta(c) }}</ap-chip-estado>
            }
            @case ('cuentaPorCobrarId') {
              @if (motivo(c); as porQueNo) {
                <span class="motivo">{{ porQueNo }}</span>
              } @else {
                <ap-boton (pulsado)="elegida.set(c)">{{ t.cobrar }}</ap-boton>
              }
            }
            @default {
              {{ c[columna.clave] }}
            }
          }
        </ng-template>
      </ap-tabla-de-datos-virtualizada>
    </section>

    @if (elegida(); as c) {
      <ap-ficha-de-cobro [cuenta]="c" (cerrada)="elegida.set(null)" />
    }
  `,
  styles: `
    .pantalla { padding: var(--s5); max-width: 76rem; display: grid; gap: var(--s4); }
    .motivo { color: var(--text-2); white-space: normal; }
  `,
})
export class PantallaDeCobros {
  constructor() {
    inject(ExplicadorDeVista).fijar(this.t.proposito)
  }

  protected readonly t = textosContabilidad.cobros
  protected readonly COLUMNAS = COLUMNAS
  protected readonly origen = (c: CuentaPorCobrar) => this.t.origenes[c.origenTipo]
  protected readonly listado = observarListado(cargadorDeCuentas(), textosContabilidad.erroresDeListado)
  protected readonly identidad = (c: CuentaPorCobrar) => c.cuentaPorCobrarId
  protected readonly etiqueta = (c: CuentaPorCobrar) => this.t.estados[c.estado]
  protected readonly elegida = signal<CuentaPorCobrar | null>(null)
  private readonly sesion = inject(Sesion)
  protected readonly puedeCobrar = computed(() => this.sesion.puede('CONTABILIDAD_ERP_COBRAR'))
  protected motivo = (c: CuentaPorCobrar): string | null => motivoParaNoCobrar(c, this.puedeCobrar())
  protected readonly filtros: Filtro[] = [
    { valor: 'PENDIENTE', texto: 'Pendiente' },
    { valor: 'COBRADA_PARCIAL', texto: 'Cobrada en parte' },
    { valor: 'COBRADA', texto: 'Cobrada' },
    { valor: 'INCOBRABLE', texto: 'Incobrable' },
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
