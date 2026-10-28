import { ChangeDetectionStrategy, Component, inject } from '@angular/core'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import type { ColumnaExportable } from '@aportaya/ui/exportar-listado/exportar'
import { Fecha } from '@aportaya/ui/fecha/fecha'
import { Monto } from '@aportaya/ui/monto/monto'
import type { Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { fechaComoSeVe, montoComoSeVe } from '../comun/como-se-ve'
import { crearListadoPorCursor } from '../comun/listado-por-cursor'
import { ListadoPorCursorVista } from '../comun/listado-por-cursor-vista'
import { pedidorDeDesembolsos, type DesembolsoEnLista } from './dominio/cu28-desembolsos'
import { textosDesembolsos } from './textos'

const T = textosDesembolsos
const C = T.columnas
type Estado = keyof typeof T.estados
type Resultado = keyof typeof T.resultados
const TONO: Record<Estado, 'ok' | 'aviso' | 'error' | 'info' | 'neutro'> = {
  CREADA: 'info', ENVIADA_A_PROVEEDOR: 'info', EN_PROCESO: 'aviso', ACREDITADA: 'ok', RECHAZADA: 'error', DEVUELTA_POR_BANCO: 'error', CANCELADA: 'neutro',
}
const CLAVES = ['creadaEn', 'monto', 'estado', 'intentos', 'ultimoResultado', 'ultimoCodigoError', 'reintentableEn', 'entregaId'] as const
const COLUMNAS: Columna<DesembolsoEnLista>[] = CLAVES.map((clave) => ({ clave, titulo: C[clave], numerica: clave === 'monto' || clave === 'intentos' }))
const estadoDe = (r: DesembolsoEnLista) => T.estados[r.estado as Estado] ?? r.estado
const resultadoDe = (r: DesembolsoEnLista) => (r.ultimoResultado ? (T.resultados[r.ultimoResultado as Resultado] ?? r.ultimoResultado) : T.ninguno)
const EXPORTABLES: ColumnaExportable<DesembolsoEnLista>[] = [
  { clave: 'ordenId', titulo: C.ordenId },
  { clave: 'creadaEn', titulo: C.creadaEn, valor: (r) => fechaComoSeVe(r.creadaEn) },
  { clave: 'monto', titulo: C.monto, valor: (r) => montoComoSeVe(r.monto) },
  { clave: 'estado', titulo: C.estado, valor: estadoDe },
  { clave: 'intentos', titulo: C.intentos },
  { clave: 'ultimoResultado', titulo: C.ultimoResultado, valor: resultadoDe },
  { clave: 'ultimoCodigoError', titulo: C.ultimoCodigoError, valor: (r) => r.ultimoCodigoError ?? T.ninguno },
  { clave: 'reintentableEn', titulo: C.reintentableEn, valor: (r) => fechaComoSeVe(r.reintentableEn) || T.ninguno },
  { clave: 'entregaId', titulo: C.entregaId },
]

/**
 * CU-28 · bandeja de desembolsos (solo lectura). Un desembolso es un movimiento de fondos: no se edita ni se elimina,
 * y el contrato no publica reintento ni cancelación desde acá, así que la fila no ofrece acciones (un botón sin
 * endpoint sería un botón muerto). El estado viaja al servidor; sin estado, el servidor devuelve las excepciones.
 */
@Component({
  selector: 'ap-bandeja-de-desembolsos',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CabeceraDePagina, ChipEstado, Fecha, Monto, ListadoPorCursorVista],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina eyebrow="Operación" [titulo]="t.titulo" [descripcion]="t.proposito" icono="entrega" />
      <ap-listado-por-cursor [titulo]="t.titulo" [columnas]="COLUMNAS" [fuente]="l" [exportables]="EXPORTABLES" [identidad]="identidad" [ayudaDeBusqueda]="t.ayudaBusqueda" [textoVacio]="t.vacio" [explicacionVacio]="t.explicacionVacio" [textoDenegado]="t.denegado" nombreDeRegistros="desembolsos" nombreDeRegistro="desembolso">
        <ng-template #celda let-r let-columna="columna">
          @switch (columna.clave) {
            @case ('creadaEn') { <ap-fecha [iso]="r.creadaEn" /> }
            @case ('reintentableEn') { @if (r.reintentableEn) { <ap-fecha [iso]="r.reintentableEn" /> } @else { {{ t.ninguno }} } }
            @case ('monto') { <ap-monto [monto]="r.monto.monto" [moneda]="r.monto.moneda" [etiqueta]="t.columnas.monto" /> }
            @case ('estado') { <ap-chip-estado [tono]="tonoDe(r)">{{ estadoDe(r) }}</ap-chip-estado> }
            @case ('ultimoResultado') { {{ resultadoDe(r) }} }
            @default { <span class="id" [attr.title]="r[columna.clave] ?? t.ninguno">{{ r[columna.clave] ?? t.ninguno }}</span> }
          }
        </ng-template>
      </ap-listado-por-cursor>
    </section>
  `,
  styles: `
    .pantalla { display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s5); min-width: 0; }
    /* Identificadores en una línea, recortados con «…»: el valor completo va en el title y en la descarga (antes se partían letra a letra y la fila medía 200 px). */
    .id { display: inline-block; max-width: 10ch; overflow: hidden; text-overflow: ellipsis; vertical-align: bottom; font-family: var(--mono); font-size: .92em; }
  `,
})
export class BandejaDeDesembolsos {
  constructor() {
    inject(ExplicadorDeVista).fijar(this.t.proposito)
  }
  protected readonly t = T
  protected readonly tonoDe = (r: DesembolsoEnLista) => TONO[r.estado as Estado]
  protected readonly COLUMNAS = COLUMNAS
  protected readonly EXPORTABLES = EXPORTABLES
  protected readonly estadoDe = estadoDe
  protected readonly resultadoDe = resultadoDe
  protected readonly identidad = (r: DesembolsoEnLista) => r.ordenId
  protected readonly l = crearListadoPorCursor({
    pedir: pedidorDeDesembolsos(),
    camposDeBusqueda: ['entregaId', 'ordenId', 'ultimoCodigoError', (r) => montoComoSeVe(r.monto), estadoDe, resultadoDe],
    filtrosDeServidor: [{ nombre: 'estados', etiqueta: T.filtroEtiqueta, opciones: (Object.keys(T.estados) as Estado[]).map((valor) => ({ valor, texto: T.estados[valor] })), textoTodos: 'Las excepciones' }],
    filtrosLocales: [{ nombre: 'resultado', etiqueta: C.ultimoResultado, valor: resultadoDe }],
  })
}
