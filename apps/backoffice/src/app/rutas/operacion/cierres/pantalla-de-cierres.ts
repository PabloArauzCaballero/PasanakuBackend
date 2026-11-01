import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { Boton } from '@aportaya/ui/boton/boton'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import type { ColumnaExportable } from '@aportaya/ui/exportar-listado/exportar'
import { Fecha, formatearDia } from '@aportaya/ui/fecha/fecha'
import type { Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { Sesion } from '../../../nucleo/sesion'
import { fechaComoSeVe, sinMonedaComoSeVe } from '../comun/como-se-ve'
import { ImporteSinMoneda } from '../comun/importe-sin-moneda'
import { crearListadoPorCursor } from '../comun/listado-por-cursor'
import { ListadoPorCursorVista } from '../comun/listado-por-cursor-vista'
import { pedidorDeCierresDiarios, type CierreDiarioEnLista } from './dominio/cu51-cierres-diarios'
import { FormularioDeCierre } from './formulario-de-cierre'
import { textosCierres } from './textos'

const T = textosCierres
const C = T.columnas
const CLAVES = ['fecha', 'totalRecaudado', 'totalConciliado', 'totalExcepciones', 'cantidadPagos', 'cuadrado', 'cerradoEn'] as const
const COLUMNAS: Columna<CierreDiarioEnLista>[] = CLAVES.map((clave) => ({ clave, titulo: C[clave], numerica: clave.startsWith('total') || clave === 'cantidadPagos' }))
const resultadoDe = (r: CierreDiarioEnLista) => (r.cuadrado ? T.cuadrado : T.conObservaciones) + (r.reabiertoEn ? ` · ${T.reabierto}` : '')
const EXPORTABLES: ColumnaExportable<CierreDiarioEnLista>[] = [
  { clave: 'cierreId', titulo: C.cierreId },
  { clave: 'fecha', titulo: C.fecha, valor: (r) => formatearDia(r.fecha) },
  { clave: 'totalRecaudado', titulo: C.totalRecaudado, valor: (r) => sinMonedaComoSeVe(r.totalRecaudado) },
  { clave: 'totalConciliado', titulo: C.totalConciliado, valor: (r) => sinMonedaComoSeVe(r.totalConciliado) },
  { clave: 'totalExcepciones', titulo: C.totalExcepciones, valor: (r) => sinMonedaComoSeVe(r.totalExcepciones) },
  { clave: 'cantidadPagos', titulo: C.cantidadPagos },
  { clave: 'cuadrado', titulo: C.cuadrado, valor: resultadoDe },
  { clave: 'cerradoEn', titulo: C.cerradoEn, valor: (r) => fechaComoSeVe(r.cerradoEn) },
]

/**
 * CU-51 · historial de cierres diarios (AUDITORIA_LEER) y, para Contabilidad, el cierre de un día. Un día cerrado no
 * se edita ni se elimina (el contrato solo publica cerrar, idempotente por fecha): la fila no tiene acciones.
 */
@Component({
  selector: 'ap-pantalla-de-cierres',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Alerta, Boton, CabeceraDePagina, ChipEstado, Fecha, ImporteSinMoneda, ListadoPorCursorVista, FormularioDeCierre],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina eyebrow="Contabilidad" [titulo]="t.titulo" [descripcion]="t.proposito">
        @if (puedeCerrar() && !formularioAbierto()) {
          <ap-boton acciones variante="primario" icono="candado" (pulsado)="formularioAbierto.set(true)">{{ t.formulario.abrir }}</ap-boton>
        }
      </ap-cabecera-de-pagina>
      @if (!puedeCerrar()) {
        <ap-alerta tono="info">{{ t.formulario.soloContabilidad }}</ap-alerta>
      } @else if (formularioAbierto()) {
        <section class="tarjeta" [attr.aria-label]="t.formulario.titulo">
          <ap-formulario-de-cierre (cerrado)="l.recargar()" />
          <div class="cerrar"><ap-boton variante="fantasma" icono="cerrar" (pulsado)="formularioAbierto.set(false)">{{ t.formulario.cerrar }}</ap-boton></div>
        </section>
      }
      <ap-listado-por-cursor [titulo]="t.historial" [columnas]="COLUMNAS" [fuente]="l" [exportables]="EXPORTABLES" [identidad]="identidad" [ayudaDeBusqueda]="t.ayudaBusqueda" [textoVacio]="t.vacio" [explicacionVacio]="t.explicacionVacio" [textoDenegado]="t.denegado" nombreDeRegistros="cierres" nombreDeRegistro="cierre">
        <ng-template #celda let-r let-columna="columna">
          @switch (columna.clave) {
            @case ('fecha') { <span class="dia">{{ dia(r.fecha) }}</span> }
            @case ('cantidadPagos') { {{ r.cantidadPagos }} }
            @case ('cuadrado') {
              <ap-chip-estado [tono]="r.cuadrado ? 'ok' : 'aviso'">{{ r.cuadrado ? t.cuadrado : t.conObservaciones }}</ap-chip-estado>
              @if (r.reabiertoEn) { <span class="nota">{{ t.reabierto }}</span> }
            }
            @case ('cerradoEn') { <ap-fecha [iso]="r.cerradoEn" /> }
            @default { <ap-importe-sin-moneda [monto]="r[columna.clave]" /> }
          }
        </ng-template>
      </ap-listado-por-cursor>
    </section>
  `,
  styles: `
    .pantalla { display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s5); min-width: 0; }
    .tarjeta { display: grid; gap: var(--s4); padding: var(--s5); border: var(--borde-fino) solid var(--border); border-radius: var(--r-lg); background: var(--surface); box-shadow: var(--sombra-1); }
    .cerrar { display: flex; justify-content: flex-end; }
    /* «15 ene 2026» en un renglón: la regla global parte el texto de las celdas y la fecha corta quedaba en dos. */
    .dia { white-space: nowrap; }
    .nota { display: block; color: var(--text-2); font: var(--t-cuerpo-chico); }
  `,
})
export class PantallaDeCierres {
  constructor() {
    inject(ExplicadorDeVista).fijar(this.t.proposito)
  }
  private readonly sesion = inject(Sesion)
  protected readonly t = T
  protected readonly COLUMNAS = COLUMNAS
  protected readonly EXPORTABLES = EXPORTABLES
  protected readonly identidad = (r: CierreDiarioEnLista) => r.cierreId
  protected readonly puedeCerrar = computed(() => this.sesion.puede('CONTABILIDAD'))
  protected readonly formularioAbierto = signal(false)
  protected readonly dia = formatearDia
  protected readonly l = crearListadoPorCursor({
    pedir: pedidorDeCierresDiarios(),
    camposDeBusqueda: ['cierreId', 'cerradoPor', (r) => formatearDia(r.fecha), (r) => sinMonedaComoSeVe(r.totalRecaudado), resultadoDe],
    filtrosLocales: [{ nombre: 'resultado', etiqueta: C.cuadrado, valor: (r) => (r.cuadrado ? T.cuadrado : T.conObservaciones) }],
  })
}
