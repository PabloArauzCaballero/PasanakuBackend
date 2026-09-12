import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core'
import { toSignal } from '@angular/core/rxjs-interop'
import { ActivatedRoute } from '@angular/router'
import { BandaDeProposito } from '@aportaya/ui/banda-de-proposito/banda-de-proposito'
import { Boton } from '@aportaya/ui/boton/boton'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { Fecha } from '@aportaya/ui/fecha/fecha'
import { Monto } from '@aportaya/ui/monto/monto'
import type { Filtro } from '@aportaya/ui/chips-de-filtro/chips-de-filtro'
import { BarraDeFiltros } from '../../../nucleo/filtros/barra-de-filtros'
import { Sesion } from '../../../nucleo/sesion'
import { TablaDeDatosVirtualizada } from '../../../nucleo/tabla/tabla-de-datos-virtualizada'
import type { ColumnaVirtual } from '../../../nucleo/tabla/tipos'
import { cargadorDeCuentas, motivoParaNoCobrar, type CuentaPorCobrar } from '../dominio/cu104-cobros'
import { FichaDeCobro } from './ficha-de-cobro'
import { textosContabilidad } from '../textos'

const COLUMNAS: ColumnaVirtual<CuentaPorCobrar>[] = [
  { clave: 'origenTipo', titulo: 'Origen', ordenable: true },
  { clave: 'terceroRazonSocial', titulo: 'Tercero', ordenable: true },
  { clave: 'fechaVencimiento', titulo: 'Vence', ordenable: true },
  { clave: 'monto', titulo: 'Monto', numerica: true, ordenable: true },
  { clave: 'saldoPendiente', titulo: 'Saldo pendiente', numerica: true },
  { clave: 'estado', titulo: 'Estado' },
  { clave: 'cuentaPorCobrarId', titulo: 'Cobro' },
]

/**
 * CU-104 · Cuentas por cobrar, sobre `TablaDeDatosVirtualizada` (la del shell), ordenada
 * por vencimiento desde el `cargador`. Una cuenta `INCOBRABLE` no ofrece cobrar: muestra
 * el motivo, porque el `AP-CU104-03` del backend tiene que confirmar la decisión, no
 * explicarla por primera vez.
 */
@Component({
  selector: 'ap-pantalla-de-cobros',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BandaDeProposito, BarraDeFiltros, Boton, ChipEstado, Fecha, Monto, TablaDeDatosVirtualizada, FichaDeCobro],
  template: `
    <ap-banda-de-proposito [texto]="t.proposito" />
    <main>
      <h1>{{ t.titulo }}</h1>
      <ap-barra-de-filtros etiqueta="Estado" [incluirBusqueda]="false" [definiciones]="filtros" />
      <ap-tabla-de-datos-virtualizada
        [titulo]="t.titulo"
        [columnas]="COLUMNAS"
        [cargador]="cargador"
        [ordenPermitido]="['origenTipo', 'terceroRazonSocial', 'fechaVencimiento', 'monto']"
        [identidad]="identidad"
        [filtros]="filtrosActivos()"
      >
        <ng-template #celda let-c let-columna="columna">
          @switch (columna.clave) {
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
    </main>

    @if (elegida(); as c) {
      <ap-ficha-de-cobro [cuenta]="c" (cerrada)="elegida.set(null)" />
    }
  `,
  styles: `
    main { padding: var(--s5); max-width: 76rem; display: grid; gap: var(--s4); }
    h1 { margin: 0; }
    .motivo { color: var(--text-2); white-space: normal; }
  `,
})
export class PantallaDeCobros {
  protected readonly t = textosContabilidad.cobros
  protected readonly COLUMNAS = COLUMNAS
  protected readonly cargador = cargadorDeCuentas()
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
