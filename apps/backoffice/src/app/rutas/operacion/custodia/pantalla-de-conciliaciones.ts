import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { Boton } from '@aportaya/ui/boton/boton'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import type { ColumnaExportable } from '@aportaya/ui/exportar-listado/exportar'
import { formatearDia } from '@aportaya/ui/fecha/fecha'
import { Icono } from '@aportaya/ui/icono/icono'
import { Monto } from '@aportaya/ui/monto/monto'
import type { Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { Sesion } from '../../../nucleo/sesion'
import { montoComoSeVe } from '../comun/como-se-ve'
import { crearListadoPorCursor } from '../comun/listado-por-cursor'
import { ListadoPorCursorVista } from '../comun/listado-por-cursor-vista'
import { esCero, pedidorDeConciliaciones, type ConciliacionEnLista } from './dominio/cu50-conciliaciones'
import { FormularioDeConciliacion } from './formulario-de-conciliacion'
import { textosCustodia } from './textos'

const T = textosCustodia
const C = T.columnas
type Estado = keyof typeof T.estados
const CLAVES = ['fecha', 'cuentaCustodiaId', 'saldoEmitido', 'saldoCustodia', 'saldoEnTransito', 'diferencia', 'ratioCobertura', 'estado', 'descuadresAbiertos'] as const
const COLUMNAS: Columna<ConciliacionEnLista>[] = CLAVES.map((clave) => ({ clave, titulo: C[clave], numerica: clave.startsWith('saldo') || clave === 'diferencia' }))
const estadoDe = (r: ConciliacionEnLista) => T.estados[r.estado as Estado] ?? r.estado
const encajeDe = (r: ConciliacionEnLista) => (r.cumpleEncaje ? T.cumpleEncaje : T.noCumpleEncaje)
const cuadreDe = (r: ConciliacionEnLista) => (esCero(r.diferencia.monto) ? T.cuadra : T.descuadre)
const EXPORTABLES: ColumnaExportable<ConciliacionEnLista>[] = [
  { clave: 'conciliacionId', titulo: C.conciliacionId },
  { clave: 'fecha', titulo: C.fecha, valor: (r) => formatearDia(r.fecha) },
  { clave: 'cuentaCustodiaId', titulo: C.cuentaCustodiaId },
  { clave: 'saldoEmitido', titulo: C.saldoEmitido, valor: (r) => montoComoSeVe(r.saldoEmitido) },
  { clave: 'saldoCustodia', titulo: C.saldoCustodia, valor: (r) => montoComoSeVe(r.saldoCustodia) },
  { clave: 'saldoEnTransito', titulo: C.saldoEnTransito, valor: (r) => montoComoSeVe(r.saldoEnTransito) },
  { clave: 'diferencia', titulo: C.diferencia, valor: (r) => `${cuadreDe(r)} ${montoComoSeVe(r.diferencia)}` },
  { clave: 'ratioCobertura', titulo: C.ratioCobertura, valor: (r) => `${r.ratioCobertura} · ${encajeDe(r)}` },
  { clave: 'estado', titulo: C.estado, valor: estadoDe },
  { clave: 'descuadresAbiertos', titulo: C.descuadresAbiertos },
]

/**
 * CU-50 · historial de conciliaciones de custodia (AUDITORIA_LEER) y, para Tesorería, el registro de una nueva.
 * Una conciliación registrada no se edita ni se elimina (es la prueba del encaje de ese día): la fila no tiene acciones.
 */
@Component({
  selector: 'ap-pantalla-de-conciliaciones',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Alerta, Boton, CabeceraDePagina, ChipEstado, Icono, Monto, ListadoPorCursorVista, FormularioDeConciliacion],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina eyebrow="Tesorería · CU-50" [titulo]="t.titulo" [descripcion]="t.proposito" icono="balanza">
        @if (puedeConciliar() && !formularioAbierto()) {
          <ap-boton acciones variante="primario" icono="mas" (pulsado)="formularioAbierto.set(true)">{{ t.formulario.abrir }}</ap-boton>
        }
      </ap-cabecera-de-pagina>
      @if (!puedeConciliar()) {
        <ap-alerta tono="info">{{ t.formulario.soloTesoreria }}</ap-alerta>
      } @else if (formularioAbierto()) {
        <section class="tarjeta" [attr.aria-label]="t.formulario.titulo">
          <ap-formulario-de-conciliacion (registrada)="l.recargar()" />
          <div class="cerrar"><ap-boton variante="fantasma" icono="cerrar" (pulsado)="formularioAbierto.set(false)">{{ t.formulario.cerrar }}</ap-boton></div>
        </section>
      }
      <ap-listado-por-cursor [titulo]="t.historial" [columnas]="COLUMNAS" [fuente]="l" [exportables]="EXPORTABLES" [identidad]="identidad" [ayudaDeBusqueda]="t.ayudaBusqueda" [textoVacio]="t.vacio" [explicacionVacio]="t.explicacionVacio" [textoDenegado]="t.denegado" nombreDeRegistros="conciliaciones" nombreDeRegistro="conciliación">
        <ng-template #celda let-r let-columna="columna">
          @switch (columna.clave) {
            @case ('fecha') { {{ dia(r.fecha) }} }
            @case ('cuentaCustodiaId') { <span class="id">{{ r.cuentaCustodiaId }}</span> }
            @case ('diferencia') {
              <span class="diferencia" [class.descuadre]="!esCero(r.diferencia.monto)"><ap-icono [nombre]="esCero(r.diferencia.monto) ? 'verificado' : 'advertencia'" tamano="s4" /><strong>{{ cuadreDe(r) }}</strong> <ap-monto [monto]="r.diferencia.monto" [moneda]="r.diferencia.moneda" [etiqueta]="t.columnas.diferencia" /></span>
            }
            @case ('ratioCobertura') { {{ r.ratioCobertura }} · {{ encajeDe(r) }} }
            @case ('estado') { <ap-chip-estado [tono]="r.estado === 'CUADRADA' ? 'ok' : r.estado === 'DESCUADRADA' ? 'error' : 'aviso'">{{ estadoDe(r) }}</ap-chip-estado> }
            @case ('descuadresAbiertos') { {{ r.descuadresAbiertos }} }
            @default { <ap-monto [monto]="r[columna.clave].monto" [moneda]="r[columna.clave].moneda" [etiqueta]="columna.titulo" /> }
          }
        </ng-template>
      </ap-listado-por-cursor>
    </section>
  `,
  styles: `
    .pantalla { padding: var(--s5); display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s5); min-width: 0; }
    .tarjeta { display: grid; gap: var(--s4); padding: var(--s5); border: var(--borde-fino) solid var(--border); border-radius: var(--r-lg); background: var(--surface); box-shadow: var(--sombra-1); }
    .cerrar { display: flex; justify-content: flex-end; }
    .id { display: block; white-space: normal; overflow-wrap: anywhere; }
    .diferencia { display: inline-flex; align-items: center; gap: var(--s1); flex-wrap: wrap; }
    .descuadre { color: var(--err-texto); }
  `,
})
export class PantallaDeConciliaciones {
  constructor() {
    inject(ExplicadorDeVista).fijar(this.t.proposito)
  }
  private readonly sesion = inject(Sesion)
  protected readonly t = T
  protected readonly COLUMNAS = COLUMNAS
  protected readonly EXPORTABLES = EXPORTABLES
  protected readonly identidad = (r: ConciliacionEnLista) => r.conciliacionId
  protected readonly puedeConciliar = computed(() => this.sesion.puede('TESORERIA'))
  protected readonly formularioAbierto = signal(false)
  protected readonly esCero = esCero
  protected readonly dia = formatearDia
  protected readonly estadoDe = estadoDe
  protected readonly encajeDe = encajeDe
  protected readonly cuadreDe = cuadreDe
  protected readonly l = crearListadoPorCursor({
    pedir: pedidorDeConciliaciones(),
    camposDeBusqueda: ['cuentaCustodiaId', 'conciliacionId', (r) => formatearDia(r.fecha), (r) => montoComoSeVe(r.diferencia), estadoDe, cuadreDe],
    filtrosLocales: [
      { nombre: 'estado', etiqueta: C.estado, valor: (r) => r.estado, texto: (v) => T.estados[v as Estado] ?? v },
      { nombre: 'encaje', etiqueta: T.encaje, valor: encajeDe },
    ],
  })
}
