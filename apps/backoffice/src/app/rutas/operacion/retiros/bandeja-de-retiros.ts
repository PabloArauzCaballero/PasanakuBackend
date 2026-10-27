import { ChangeDetectionStrategy, Component, inject } from '@angular/core'
import { ActivatedRoute, Router } from '@angular/router'
import { AccionesDeFila } from '@aportaya/ui/acciones-de-fila/acciones-de-fila'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import type { ColumnaExportable } from '@aportaya/ui/exportar-listado/exportar'
import { Fecha } from '@aportaya/ui/fecha/fecha'
import { Icono } from '@aportaya/ui/icono/icono'
import { Monto } from '@aportaya/ui/monto/monto'
import type { Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { fechaComoSeVe, montoComoSeVe } from '../comun/como-se-ve'
import { crearListadoPorCursor } from '../comun/listado-por-cursor'
import { ListadoPorCursorVista } from '../comun/listado-por-cursor-vista'
import { pedidorDeRetirosEnRevision, type RetiroEnRevision } from './dominio/cu11-retiros-en-revision'
import { textosRetiros } from './textos'

const T = textosRetiros.bandeja
const C = T.columnas
const COLUMNAS: Columna<RetiroEnRevision>[] = [
  { clave: 'solicitadaEn', titulo: C.solicitadaEn },
  { clave: 'solicitado', titulo: C.solicitado, numerica: true },
  { clave: 'costo', titulo: C.costo, numerica: true },
  { clave: 'neto', titulo: C.neto, numerica: true },
  { clave: 'mfaVerificado', titulo: C.mfaVerificado },
  { clave: 'ventanaEnfriamientoHasta', titulo: C.ventanaEnfriamientoHasta },
]
const segundoFactor = (r: RetiroEnRevision) => (r.mfaVerificado ? T.conSegundoFactor : T.sinSegundoFactor)
const quienDecide = (r: RetiroEnRevision) => (r.puedeDecidir ? T.decideOtro : T.propia)
const EXPORTABLES: ColumnaExportable<RetiroEnRevision>[] = [
  { clave: 'ordenRetiroId', titulo: C.ordenRetiroId },
  { clave: 'solicitadaEn', titulo: C.solicitadaEn, valor: (r) => fechaComoSeVe(r.solicitadaEn) },
  { clave: 'solicitado', titulo: C.solicitado, valor: (r) => montoComoSeVe(r.solicitado) },
  { clave: 'costo', titulo: C.costo, valor: (r) => montoComoSeVe(r.costo) },
  { clave: 'neto', titulo: C.neto, valor: (r) => montoComoSeVe(r.neto) },
  { clave: 'mfaVerificado', titulo: C.mfaVerificado, valor: segundoFactor },
  { clave: 'ventanaEnfriamientoHasta', titulo: C.ventanaEnfriamientoHasta, valor: (r) => fechaComoSeVe(r.ventanaEnfriamientoHasta) || T.sinEnfriamiento },
  { clave: 'puedeDecidir', titulo: C.quienDecide, valor: quienDecide },
]

/**
 * CU-11 · bandeja de retiros en revisión (`listarRetirosEnRevision`, cursor ascendente). La única acción de fila es
 * la del dominio, «Revisar y decidir» (`aprobarRetiro`); una orden propia no se decide (lo vuelve a rechazar el servidor).
 */
@Component({
  selector: 'ap-bandeja-de-retiros',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AccionesDeFila, CabeceraDePagina, ChipEstado, Fecha, Icono, Monto, ListadoPorCursorVista],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina eyebrow="Operación · CU-11" [titulo]="t.titulo" [descripcion]="t.proposito" icono="retiro" />
      <ap-listado-por-cursor [titulo]="t.titulo" [columnas]="COLUMNAS" [fuente]="l" [exportables]="EXPORTABLES" [identidad]="identidad" [ayudaDeBusqueda]="t.ayudaBusqueda" [textoVacio]="t.vacio" [explicacionVacio]="t.explicacionVacio" [textoDenegado]="t.denegado" nombreDeRegistros="retiros" nombreDeRegistro="retiro">
        <ng-template #celda let-r let-columna="columna">
          @switch (columna.clave) {
            @case ('solicitadaEn') { <ap-fecha [iso]="r.solicitadaEn" /> }
            @case ('mfaVerificado') { <ap-chip-estado [tono]="r.mfaVerificado ? 'ok' : 'aviso'">{{ segundoFactor(r) }}</ap-chip-estado> }
            @case ('ventanaEnfriamientoHasta') { @if (r.ventanaEnfriamientoHasta) { <ap-fecha [iso]="r.ventanaEnfriamientoHasta" /> } @else { {{ t.sinEnfriamiento }} } }
            @default { <ap-monto [monto]="r[columna.clave].monto" [moneda]="r[columna.clave].moneda" [etiqueta]="columna.titulo" /> }
          }
        </ng-template>
        <ng-template #acciones let-r>
          <ap-acciones-de-fila [fila]="'retiro ' + r.ordenRetiroId">
            @if (r.puedeDecidir) {
              <button type="button" apAccionDeFila [attr.aria-label]="t.decidir + ' el retiro ' + r.ordenRetiroId" (click)="abrir(r)"><ap-icono nombre="aprobar" tamano="s4" /><span>{{ t.decidir }}</span></button>
            } @else {
              <span class="propia"><ap-icono nombre="candado" tamano="s4" />{{ t.propia }}</span>
            }
          </ap-acciones-de-fila>
        </ng-template>
      </ap-listado-por-cursor>
    </section>
  `,
  styles: `
    .pantalla { padding: var(--s5); display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s5); min-width: 0; }
    .propia { display: inline-flex; align-items: center; gap: var(--s1); color: var(--text-2); font: var(--t-cuerpo-chico); white-space: normal; }
  `,
})
export class BandejaDeRetiros {
  constructor() {
    inject(ExplicadorDeVista).fijar(this.t.proposito)
  }
  protected readonly t = T
  protected readonly COLUMNAS = COLUMNAS
  protected readonly EXPORTABLES = EXPORTABLES
  protected readonly segundoFactor = segundoFactor
  protected readonly identidad = (r: RetiroEnRevision) => r.ordenRetiroId
  protected readonly l = crearListadoPorCursor({
    pedir: pedidorDeRetirosEnRevision(),
    camposDeBusqueda: ['ordenRetiroId', (r) => montoComoSeVe(r.solicitado), (r) => montoComoSeVe(r.neto), (r) => fechaComoSeVe(r.solicitadaEn), segundoFactor],
    filtrosLocales: [
      { nombre: 'segundoFactor', etiqueta: C.mfaVerificado, valor: segundoFactor },
      { nombre: 'quienDecide', etiqueta: C.quienDecide, valor: quienDecide },
    ],
  })
  private readonly router = inject(Router)
  private readonly ruta = inject(ActivatedRoute)

  /** La ficha recibe la fila por el estado de la navegación: el contrato no publica una consulta de la orden. */
  protected abrir(r: RetiroEnRevision): void {
    if (!r.puedeDecidir) return
    void this.router.navigate([r.ordenRetiroId], { relativeTo: this.ruta, state: { retiro: r } })
  }
}
