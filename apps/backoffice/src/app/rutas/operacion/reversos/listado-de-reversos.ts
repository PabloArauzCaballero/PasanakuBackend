import { ChangeDetectionStrategy, Component, inject } from '@angular/core'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import type { ColumnaExportable } from '@aportaya/ui/exportar-listado/exportar'
import { Fecha } from '@aportaya/ui/fecha/fecha'
import type { Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { fechaComoSeVe, sinMonedaComoSeVe } from '../comun/como-se-ve'
import { ImporteSinMoneda } from '../comun/importe-sin-moneda'
import { crearListadoPorCursor } from '../comun/listado-por-cursor'
import { ListadoPorCursorVista } from '../comun/listado-por-cursor-vista'
import { pedidorDeReversos, type ReversoEnLista } from './dominio/cu14-reversos'
import { textosReversos } from './textos'

const T = textosReversos
type Estado = keyof typeof T.estados
type Tipo = keyof typeof T.tipos
const TONO: Record<Estado, 'ok' | 'aviso' | 'error' | 'info'> = { SOLICITADO: 'info', AUTORIZADO: 'aviso', EJECUTADO: 'ok', RECHAZADO: 'error' }
const CLAVES = ['solicitadaEn', 'tipo', 'estado', 'montoReversado', 'motivo', 'autorizadaPor', 'transaccionOriginalId', 'ejecutadaEn'] as const
const COLUMNAS: Columna<ReversoEnLista>[] = CLAVES.map((clave) => ({ clave, titulo: T.columnas[clave], numerica: clave === 'montoReversado' }))
const estadoDe = (r: ReversoEnLista) => T.estados[r.estado as Estado] ?? r.estado
const tipoDe = (r: ReversoEnLista) => T.tipos[r.tipo as Tipo] ?? r.tipo
const C = T.columnas
const EXPORTABLES: ColumnaExportable<ReversoEnLista>[] = [
  { clave: 'reversoId', titulo: C.reversoId },
  { clave: 'solicitadaEn', titulo: C.solicitadaEn, valor: (r) => fechaComoSeVe(r.solicitadaEn) },
  { clave: 'tipo', titulo: C.tipo, valor: tipoDe },
  { clave: 'estado', titulo: C.estado, valor: estadoDe },
  { clave: 'montoReversado', titulo: C.montoReversado, valor: (r) => sinMonedaComoSeVe(r.montoReversado) },
  { clave: 'motivo', titulo: C.motivo },
  { clave: 'autorizadaPor', titulo: C.autorizadaPor },
  { clave: 'transaccionOriginalId', titulo: C.transaccionOriginalId },
  { clave: 'ejecutadaEn', titulo: C.ejecutadaEn, valor: (r) => fechaComoSeVe(r.ejecutadaEn) || T.pendiente },
]

/**
 * CU-14 · listado de reversos de solo lectura para Tesorería y Auditoría (AUDITORIA_LEER). Un reverso es un registro
 * contable: no se edita ni se elimina (regla 91.1.3), así que la fila no tiene acciones. El estado viaja al servidor
 * (`estados`); tipo, búsqueda y descarga trabajan sobre la página cargada.
 */
@Component({
  selector: 'ap-listado-de-reversos',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CabeceraDePagina, ChipEstado, Fecha, ImporteSinMoneda, ListadoPorCursorVista],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina eyebrow="Operación · CU-14" [titulo]="t.titulo" [descripcion]="t.proposito" icono="deshacer" />
      <ap-listado-por-cursor [titulo]="t.titulo" [columnas]="COLUMNAS" [fuente]="l" [exportables]="EXPORTABLES" [identidad]="identidad" [ayudaDeBusqueda]="t.ayudaBusqueda" [textoVacio]="t.vacio" [explicacionVacio]="t.explicacionVacio" [textoDenegado]="t.denegado" nombreDeRegistros="reversos" nombreDeRegistro="reverso">
        <ng-template #celda let-r let-columna="columna">
          @switch (columna.clave) {
            @case ('solicitadaEn') { <ap-fecha [iso]="r.solicitadaEn" /> }
            @case ('ejecutadaEn') { @if (r.ejecutadaEn) { <ap-fecha [iso]="r.ejecutadaEn" /> } @else { {{ t.pendiente }} } }
            @case ('tipo') { {{ tipoDe(r) }} }
            @case ('estado') { <ap-chip-estado [tono]="tonoDe(r)">{{ estadoDe(r) }}</ap-chip-estado> }
            @case ('montoReversado') { <ap-importe-sin-moneda [monto]="r.montoReversado" /> }
            @case ('motivo') { <span class="largo">{{ r.motivo }}</span> }
            @default { <span class="id">{{ r[columna.clave] }}</span> }
          }
        </ng-template>
      </ap-listado-por-cursor>
    </section>
  `,
  styles: `
    .pantalla { padding: var(--s5); display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s5); min-width: 0; }
    .largo, .id { display: block; white-space: normal; overflow-wrap: anywhere; }
  `,
})
export class ListadoDeReversos {
  constructor() {
    inject(ExplicadorDeVista).fijar(this.t.proposito)
  }
  protected readonly t = T
  protected readonly tonoDe = (r: ReversoEnLista) => TONO[r.estado as Estado]
  protected readonly COLUMNAS = COLUMNAS
  protected readonly EXPORTABLES = EXPORTABLES
  protected readonly estadoDe = estadoDe
  protected readonly tipoDe = tipoDe
  protected readonly identidad = (r: ReversoEnLista) => r.reversoId
  protected readonly l = crearListadoPorCursor({
    pedir: pedidorDeReversos(),
    camposDeBusqueda: ['motivo', 'autorizadaPor', 'transaccionOriginalId', 'reversoId', (r) => sinMonedaComoSeVe(r.montoReversado), tipoDe, estadoDe],
    filtrosDeServidor: [{ nombre: 'estados', etiqueta: T.filtroEtiqueta, opciones: (Object.keys(T.estados) as Estado[]).map((valor) => ({ valor, texto: T.estados[valor] })), textoTodos: 'Todos' }],
    filtrosLocales: [{ nombre: 'tipo', etiqueta: T.columnas.tipo, valor: (r) => r.tipo, texto: (v) => T.tipos[v as Tipo] ?? v }],
  })
}
