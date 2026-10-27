import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core'
import { AccionesDeFila } from '@aportaya/ui/acciones-de-fila/acciones-de-fila'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { Fecha } from '@aportaya/ui/fecha/fecha'
import { Icono } from '@aportaya/ui/icono/icono'
import { Monto } from '@aportaya/ui/monto/monto'
import { TarjetaKPI } from '@aportaya/ui/tarjeta-kpi/tarjeta-kpi'
import type { Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { Sesion } from '../../../nucleo/sesion'
import { textoDeImporte } from '../dominio/contrato-erp'
import { cargadorDeCuentas, motivoParaNoCobrar, type CuentaPorCobrar } from '../dominio/cu104-cobros'
import { crearListadoPorCursor, filtroInicialDeLaUrl, opcionesDe } from '../listado/listado-por-cursor'
import { TablaPorCursor } from '../listado/tabla-por-cursor'
import { FichaDeCobro } from './ficha-de-cobro'
import { textosContabilidad } from '../textos'

const t = textosContabilidad.cobros
const origen = (c: CuentaPorCobrar) => t.origenes[c.origenTipo]
const tercero = (c: CuentaPorCobrar) => c.terceroRazonSocial ?? t.sinTercero

const COLUMNAS: Columna<CuentaPorCobrar>[] = [
  { clave: 'origenTipo', titulo: 'Origen' },
  { clave: 'terceroRazonSocial', titulo: 'Tercero' },
  { clave: 'fechaVencimiento', titulo: 'Vence' },
  { clave: 'monto', titulo: 'Monto', numerica: true },
  { clave: 'saldoPendiente', titulo: 'Saldo pendiente', numerica: true },
  { clave: 'estado', titulo: 'Estado' },
]

const EXPORTABLES = [
  { clave: 'origen', titulo: 'Origen', valor: origen },
  { clave: 'tercero', titulo: 'Tercero', valor: tercero },
  { clave: 'fechaVencimiento', titulo: 'Vence' },
  { clave: 'monto', titulo: 'Monto', valor: (c: CuentaPorCobrar) => textoDeImporte(c.monto) },
  { clave: 'cobrado', titulo: 'Cobrado', valor: (c: CuentaPorCobrar) => textoDeImporte(c.cobrado) },
  { clave: 'saldoPendiente', titulo: 'Saldo pendiente', valor: (c: CuentaPorCobrar) => textoDeImporte(c.saldoPendiente) },
  { clave: 'estado', titulo: 'Estado', valor: (c: CuentaPorCobrar) => t.estados[c.estado] },
]

/**
 * CU-104 · Cuentas por cobrar, listado por cursor del servidor (vencimiento descendente). El estado viaja como
 * `estados`; el buscador, el origen y el tercero filtran la página cargada.
 *
 * **Acción de fila: «Registrar el cobro»** (`registrarCobro`, con Idempotency-Key), solo con `CONTABILIDAD_ERP_COBRAR`
 * y saldo que cobrar. Una cuenta `INCOBRABLE` no ofrece cobrar: muestra el motivo. Un cobro registrado es append-only:
 * no hay Editar ni Eliminar. Tras el cobro, la lista se recarga y lo dice.
 */
@Component({
  selector: 'ap-pantalla-de-cobros',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AccionesDeFila, Alerta, CabeceraDePagina, ChipEstado, Fecha, Icono, Monto, TablaPorCursor, TarjetaKPI, FichaDeCobro],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina eyebrow="Contabilidad" [titulo]="t.titulo" [descripcion]="t.proposito" icono="moneda" />
      <div class="kpis">
        <ap-tarjeta-kpi etiqueta="Cuentas en la página" [valor]="'' + listado.filas().length" icono="lista" />
        <ap-tarjeta-kpi etiqueta="Por cobrar en la página" [valor]="'' + porCobrar()" icono="reloj" tono="aviso" pista="Pendientes o cobradas en parte" />
        <ap-tarjeta-kpi etiqueta="Incobrables en la página" [valor]="'' + incobrables()" icono="prohibido" [tono]="incobrables() > 0 ? 'error' : 'neutro'" />
      </div>
      @if (cobrada(); as nombre) { <ap-alerta tono="ok" titulo="Cobro registrado">El cobro de {{ nombre }} quedó registrado. La lista ya muestra el saldo nuevo.</ap-alerta> }
      <ap-tabla-por-cursor [titulo]="t.titulo" [columnas]="COLUMNAS" [listado]="listado" [identidad]="identidad" [exportables]="EXPORTABLES" nombreDeRegistros="cuentas" [textoVacio]="t.enCero" ayudaDeBusqueda="Busca por origen o tercero en la página cargada: el servidor no busca por texto.">
        <ng-template #celda let-c let-columna="columna">
          @switch (columna.clave) {
            @case ('origenTipo') { <span class="texto">{{ origen(c) }}</span> }
            @case ('terceroRazonSocial') { <span class="texto">{{ tercero(c) }}</span> }
            @case ('fechaVencimiento') { <ap-fecha [iso]="c.fechaVencimiento" [conHora]="false" /> }
            @case ('monto') { <ap-monto [monto]="c.monto.monto" [moneda]="c.monto.moneda" etiqueta="Monto de la cuenta" /> }
            @case ('saldoPendiente') { <ap-monto [monto]="c.saldoPendiente.monto" [moneda]="c.saldoPendiente.moneda" [etiqueta]="t.saldoPendiente" /> }
            @case ('estado') { <ap-chip-estado [tono]="c.estado === 'COBRADA' ? 'ok' : c.estado === 'INCOBRABLE' ? 'error' : 'aviso'">{{ etiqueta(c) }}</ap-chip-estado> }
            @default { {{ c[columna.clave] }} }
          }
        </ng-template>
        <ng-template #acciones let-c>
          @if (motivo(c); as porQueNo) {
            <span class="motivo">{{ porQueNo }}</span>
          } @else {
            <ap-acciones-de-fila [fila]="origen(c) + ' de ' + tercero(c)">
              <button type="button" apAccionDeFila [attr.aria-label]="t.cobrar + ' de ' + tercero(c)" (click)="elegida.set(c)"><ap-icono nombre="moneda" tamano="s4" /><span>{{ t.cobrar }}</span></button>
            </ap-acciones-de-fila>
          }
        </ng-template>
      </ap-tabla-por-cursor>
    </section>

    @if (elegida(); as c) {
      <ap-ficha-de-cobro [cuenta]="c" (cobrado)="alCobrar(c)" (cerrada)="elegida.set(null)" />
    }
  `,
  styles: `
    /* Los textos largos se parten en la celda: así la tabla entra en su marco y la columna fija de acciones no tapa un importe. */
    .texto { display: inline-block; min-width: 8rem; white-space: normal; overflow-wrap: anywhere; }
    .pantalla { display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s4); min-width: 0; }
    .kpis { display: grid; grid-template-columns: repeat(auto-fit, minmax(min(100%, 14rem), 1fr)); gap: var(--s3); }
    .motivo { display: block; max-width: 13rem; color: var(--text-2); white-space: normal; font: var(--t-cuerpo-chico); }
  `,
})
export class PantallaDeCobros {
  constructor() {
    inject(ExplicadorDeVista).fijar(t.proposito)
  }

  protected readonly t = t
  protected readonly COLUMNAS = COLUMNAS
  protected readonly EXPORTABLES = EXPORTABLES
  protected readonly origen = origen
  protected readonly tercero = tercero
  protected readonly identidad = (c: CuentaPorCobrar) => c.cuentaPorCobrarId
  protected readonly etiqueta = (c: CuentaPorCobrar) => t.estados[c.estado]
  protected readonly elegida = signal<CuentaPorCobrar | null>(null)
  protected readonly cobrada = signal<string | null>(null)
  private readonly sesion = inject(Sesion)
  protected readonly puedeCobrar = computed(() => this.sesion.puede('CONTABILIDAD_ERP_COBRAR'))
  protected readonly motivo = (c: CuentaPorCobrar): string | null => motivoParaNoCobrar(c, this.puedeCobrar())
  protected readonly listado = crearListadoPorCursor<CuentaPorCobrar>({
    cargador: cargadorDeCuentas(),
    camposDeBusqueda: [origen, tercero],
    servidor: { nombre: 'estado', etiqueta: 'Estado de la cuenta', opciones: opcionesDe(t.estados), inicial: filtroInicialDeLaUrl(opcionesDe(t.estados)) },
    filtros: [
      { nombre: 'origen', etiqueta: 'Origen', valor: (c) => c.origenTipo, opciones: opcionesDe(t.origenes) },
      { nombre: 'tercero', etiqueta: 'Tercero', valor: tercero },
    ],
    textosDeError: textosContabilidad.erroresDeListado,
  })
  protected readonly porCobrar = computed(() => this.listado.filas().filter((c) => c.estado === 'PENDIENTE' || c.estado === 'COBRADA_PARCIAL').length)
  protected readonly incobrables = computed(() => this.listado.filas().filter((c) => c.estado === 'INCOBRABLE').length)

  protected alCobrar(c: CuentaPorCobrar): void {
    this.cobrada.set(tercero(c))
    this.listado.recargar()
  }
}
