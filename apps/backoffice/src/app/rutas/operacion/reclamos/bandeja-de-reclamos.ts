import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core'
import { AccionesDeFila } from '@aportaya/ui/acciones-de-fila/acciones-de-fila'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import type { ColumnaExportable } from '@aportaya/ui/exportar-listado/exportar'
import { Icono } from '@aportaya/ui/icono/icono'
import { RelojDePlazo } from '@aportaya/ui/reloj-de-plazo/reloj-de-plazo'
import type { Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { Sesion } from '../../../nucleo/sesion'
import { fechaComoSeVe, sinMonedaComoSeVe } from '../comun/como-se-ve'
import { ImporteSinMoneda } from '../comun/importe-sin-moneda'
import { crearListadoPorCursor } from '../comun/listado-por-cursor'
import { ListadoPorCursorVista } from '../comun/listado-por-cursor-vista'
import { pedidorDeReclamos, sePuedeResponder, type EstadoReclamo, type ReclamoEnBandeja } from '../dominio/cu52-reclamos'
import { RespuestaAReclamo } from './respuesta-a-reclamo'
import { textosReclamos } from './textos'

const T = textosReclamos
const C = T.columnas
type Categoria = keyof typeof T.categorias
type Canal = keyof typeof T.canales
const TONO: Record<EstadoReclamo, 'ok' | 'aviso' | 'error' | 'info' | 'neutro'> = { INGRESADO: 'info', EN_ANALISIS: 'aviso', RESPONDIDO: 'ok', CERRADO: 'neutro', ELEVADO: 'error' }
const CLAVES = ['codigo', 'categoria', 'canalIngreso', 'montoReclamado', 'limiteVigente', 'estado'] as const
const COLUMNAS: Columna<ReclamoEnBandeja>[] = CLAVES.map((clave) => ({ clave, titulo: C[clave], numerica: clave === 'montoReclamado', mono: clave === 'codigo' }))
const estadoDe = (r: ReclamoEnBandeja) => T.estados[r.estado as EstadoReclamo] ?? r.estado
const categoriaDe = (r: ReclamoEnBandeja) => T.categorias[r.categoria as Categoria] ?? r.categoria
const canalDe = (r: ReclamoEnBandeja) => T.canales[r.canalIngreso as Canal] ?? r.canalIngreso
const plazoDe = (r: ReclamoEnBandeja) => (r.vencido ? T.vencido : T.enPlazo)
const EXPORTABLES: ColumnaExportable<ReclamoEnBandeja>[] = [
  { clave: 'codigo', titulo: C.codigo },
  { clave: 'categoria', titulo: C.categoria, valor: categoriaDe },
  { clave: 'canalIngreso', titulo: C.canalIngreso, valor: canalDe },
  { clave: 'montoReclamado', titulo: C.montoReclamado, valor: (r) => (r.montoReclamado ? sinMonedaComoSeVe(r.montoReclamado) : T.sinMonto) },
  { clave: 'limiteVigente', titulo: C.limiteVigente, valor: (r) => fechaComoSeVe(r.limiteVigente) },
  { clave: 'vencido', titulo: C.vencido, valor: plazoDe },
  { clave: 'estado', titulo: C.estado, valor: estadoDe },
]

/**
 * D-18 · Bandeja de reclamos (CU-52) sobre `listarBandejaDeReclamos`. El estado viaja al servidor (sin estado: los
 * abiertos); categoría, canal, plazo y búsqueda miran la página cargada. La acción de fila es la del dominio,
 * «Responder» (`responderReclamo`): un reclamo no se edita ni se elimina, se responde y queda en el expediente.
 */
@Component({
  selector: 'ap-bandeja-de-reclamos',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AccionesDeFila, Alerta, CabeceraDePagina, ChipEstado, Icono, RelojDePlazo, ImporteSinMoneda, ListadoPorCursorVista, RespuestaAReclamo],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina data-tutorial-id="operacion-reclamos" eyebrow="Operación · CU-52" [titulo]="t.titulo" [descripcion]="t.proposito" icono="mensaje" />
      @if (hecho(); as h) { <ap-alerta tono="ok">{{ h }}</ap-alerta> }
      <ap-listado-por-cursor [titulo]="t.titulo" [columnas]="COLUMNAS" [fuente]="l" [exportables]="EXPORTABLES" [identidad]="identidad" [ayudaDeBusqueda]="t.ayudaBusqueda" [textoVacio]="t.vacio" [explicacionVacio]="t.explicacionVacio" [textoDenegado]="t.denegado" nombreDeRegistros="reclamos" nombreDeRegistro="reclamo">
        <ng-template #celda let-r let-columna="columna">
          @switch (columna.clave) {
            @case ('estado') { <ap-chip-estado [tono]="tonoDe(r)">{{ estadoDe(r) }}</ap-chip-estado> }
            @case ('categoria') { {{ categoriaDe(r) }} }
            @case ('canalIngreso') { {{ canalDe(r) }} }
            @case ('limiteVigente') {
              <ap-reloj-de-plazo [etiqueta]="t.plazo" [venceIso]="r.limiteVigente" [ahoraIso]="ahoraIso" [norma]="t.norma" />
              @if (r.vencido) { <span class="vencido"><ap-icono nombre="advertencia" tamano="s4" />{{ t.vencido }}</span> }
            }
            @case ('montoReclamado') { @if (r.montoReclamado) { <ap-importe-sin-moneda [monto]="r.montoReclamado" /> } @else { {{ t.sinMonto }} } }
            @default { <span class="id">{{ r[columna.clave] }}</span> }
          }
        </ng-template>
        <ng-template #acciones let-r>
          <ap-acciones-de-fila [fila]="'reclamo ' + r.codigo">
            @if (puedeAtender() && sePuedeResponder(r)) {
              <button type="button" apAccionDeFila [attr.aria-label]="t.responder + ' el reclamo ' + r.codigo" (click)="abrir(r)"><ap-icono nombre="mensaje" tamano="s4" /><span>{{ t.responder }}</span></button>
            } @else {
              <span class="nota">{{ puedeAtender() ? t.yaRespondido : t.soloLectura }}</span>
            }
          </ap-acciones-de-fila>
        </ng-template>
      </ap-listado-por-cursor>
    </section>
    @if (seleccionado(); as r) {
      <ap-respuesta-a-reclamo [reclamo]="r" (respondido)="alResponder(r)" (cerrar)="seleccionado.set(null)" />
    }
  `,
  styles: `
    .pantalla { padding: var(--s5); display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s5); min-width: 0; }
    .vencido { display: inline-flex; align-items: center; gap: var(--s1); color: var(--err-texto); font-weight: 600; }
    .id { display: block; white-space: normal; overflow-wrap: anywhere; }
    .nota { color: var(--text-2); font: var(--t-cuerpo-chico); white-space: normal; }
  `,
})
export class BandejaDeReclamos {
  constructor() {
    inject(ExplicadorDeVista).fijar(this.t.proposito)
  }
  private readonly sesion = inject(Sesion)
  protected readonly t = T
  protected readonly tonoDe = (r: ReclamoEnBandeja) => TONO[r.estado as EstadoReclamo]
  protected readonly COLUMNAS = COLUMNAS
  protected readonly EXPORTABLES = EXPORTABLES
  protected readonly ahoraIso = new Date().toISOString()
  protected readonly identidad = (r: ReclamoEnBandeja) => r.reclamoId
  protected readonly sePuedeResponder = sePuedeResponder
  protected readonly estadoDe = estadoDe
  protected readonly categoriaDe = categoriaDe
  protected readonly canalDe = canalDe
  protected readonly l = crearListadoPorCursor({
    pedir: pedidorDeReclamos(),
    camposDeBusqueda: ['codigo', 'producto', categoriaDe, canalDe, estadoDe, (r) => sinMonedaComoSeVe(r.montoReclamado)],
    filtrosDeServidor: [{ nombre: 'estados', etiqueta: T.filtroEtiqueta, opciones: (Object.keys(T.estados) as EstadoReclamo[]).map((valor) => ({ valor, texto: T.estados[valor] })), textoTodos: 'Abiertos' }],
    filtrosLocales: [
      { nombre: 'categoria', etiqueta: C.categoria, valor: (r) => r.categoria, texto: (v) => T.categorias[v as Categoria] ?? v },
      { nombre: 'canal', etiqueta: C.canalIngreso, valor: (r) => r.canalIngreso, texto: (v) => T.canales[v as Canal] ?? v },
      { nombre: 'plazo', etiqueta: C.vencido, valor: plazoDe },
    ],
  })
  protected readonly puedeAtender = computed(() => this.sesion.puede('RECLAMO_ATENDER'))
  protected readonly seleccionado = signal<ReclamoEnBandeja | null>(null)
  protected readonly hecho = signal('')

  protected abrir(r: ReclamoEnBandeja): void {
    if (!this.puedeAtender() || !sePuedeResponder(r)) return
    this.hecho.set('')
    this.seleccionado.set(r)
  }

  /** El éxito se anuncia con el 200 y la bandeja se vuelve a pedir: el reclamo cerrado sale de los abiertos. */
  protected alResponder(r: ReclamoEnBandeja): void {
    this.seleccionado.set(null)
    this.hecho.set(T.respuesta.hecho(r.codigo))
    this.l.recargar()
  }
}
