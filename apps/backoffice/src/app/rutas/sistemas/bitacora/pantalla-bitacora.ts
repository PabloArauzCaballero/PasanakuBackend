import { ChangeDetectionStrategy, Component, computed, effect, inject, signal, untracked } from '@angular/core'
import { AccionesDeFila } from '@aportaya/ui/acciones-de-fila/acciones-de-fila'
import { BarraDeFiltros, type Selector } from '@aportaya/ui/barra-de-filtros/barra-de-filtros'
import { Boton } from '@aportaya/ui/boton/boton'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { Esqueleto } from '@aportaya/ui/esqueleto/esqueleto'
import { EstadoDeError } from '@aportaya/ui/estado-de-error/estado-de-error'
import { EstadoVacio } from '@aportaya/ui/estado-vacio/estado-vacio'
import { ExportarListado } from '@aportaya/ui/exportar-listado/exportar-listado'
import { crearListado } from '@aportaya/ui/listado/crear-listado'
import { Paginacion } from '@aportaya/ui/paginacion/paginacion'
import { TablaDeDatos } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { cargaDe, filasDe } from '../compartido/carga'
import { CifrasDeSistemas, type Cifra } from '../compartido/cifras'
import { bitacora, type EventoBitacora, type PaginaDeBitacora } from '../dominio/bitacora'
import { textosSistemas } from '../textos'
import { CajonDetalleEvento } from './cajon-detalle-evento'
import { ACCIONES, COLUMNAS, ENTIDADES, EXPORTABLES, acortar, tonoAccion, type FilaBitacora } from './columnas-de-bitacora'
import { crearPaginadorPorCursor } from './paginador-por-cursor'
import { Exportador } from '../../../nucleo/exportador'

/**
 * Registro inmutable del sistema (`comun.bitacora_evento`, encadenado por hash SHA-256). Es **append-only**: la única
 * acción de fila es Ver detalle (regla 91.1.3), nunca editar ni eliminar.
 *
 * El contrato pagina en el servidor por cursor (`antesDe`/`antesDeId`, `limite` ≤ 100) y filtra por entidad y
 * acción: esos filtros viajan en la consulta y la página se pide con `crearPaginadorPorCursor`. No hay búsqueda libre
 * en el contrato, así que el buscador busca **en la página cargada** (lo dice su ayuda) y CSV/JSON bajan esa página
 * filtrada.
 */
@Component({
  selector: 'ap-pantalla-bitacora',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AccionesDeFila, BarraDeFiltros, Boton, CabeceraDePagina, CajonDetalleEvento, ChipEstado, CifrasDeSistemas, Esqueleto, EstadoDeError, EstadoVacio, ExportarListado, Paginacion, TablaDeDatos],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina eyebrow="Sistemas · Auditoría" [titulo]="t.titulo" [descripcion]="t.proposito" icono="libro">
        <ap-boton acciones icono="recarga" [deshabilitado]="recurso.isLoading()" (pulsado)="recurso.reload()">Actualizar</ap-boton>
      </ap-cabecera-de-pagina>
      <ap-cifras-de-sistemas etiqueta="Resumen de la página del registro" [cifras]="cifras()" />
      <ap-barra-de-filtros etiqueta="Filtros del registro del sistema" [etiquetaDeBusqueda]="t.buscar" ayudaDeBusqueda="Busca en la página cargada por entidad, acción, rol del actor, origen, motivo o hash. Entidad y acción filtran en el servidor." [busqueda]="enPagina.busqueda()" [selectores]="selectores()" textoLimpiar="Limpiar filtros" (busquedaChange)="enPagina.buscar($event)" (selectorChange)="filtrar($event.nombre, $event.valor)" (limpiar)="limpiar()" />
      <div class="barra">
        <p class="resumen" aria-live="polite">{{ resumen() }}</p>
        @if (exportador.puedeExportar('bitacora')) { <ap-exportar-listado nombre="registro del sistema" [columnas]="exportables" [filas]="enPagina.filtradas()" [deshabilitado]="carga().tipo !== 'lista'" /> }
      </div>
      @switch (carga().tipo) {
        @case ('cargando') { <ap-esqueleto [lineas]="6" [etiqueta]="t.cargando" /> }
        @case ('error') { <ap-estado-de-error titulo="No se pudo cargar el registro del sistema." [mensaje]="mensajeDeError()" [trazaId]="idDePeticion()" (reintentar)="recurso.reload()" /> }
        @default {
          @if (enPagina.total() === 0) {
            @if (enPagina.hayCriterios() || hayFiltrosDeServidor()) {
              <ap-estado-vacio motivo="porFiltro" titulo="Ningún evento coincide con la búsqueda o los filtros." accionPropia="Quitar filtros" (actuar)="limpiar()" />
            } @else {
              <ap-estado-vacio [titulo]="t.vacio" explicacion="Cada cambio con efecto en el sistema deja un evento acá, encadenado por hash." />
            }
          } @else {
            <ap-tabla-de-datos [titulo]="t.titulo" [columnas]="columnas" [filas]="enPagina.filtradas()" [identidad]="identidad" [orden]="enPagina.orden()" (ordenado)="enPagina.orden.set($event)" [plantillaDeCelda]="celda" />
          }
          <ap-paginacion etiqueta="Paginación del registro del sistema" [pagina]="paginador.pagina()" (paginaChange)="paginador.irAPagina($event)" [totalDeFilas]="paginador.totalDeFilas()" [conRegistros]="paginador.conTotal()" [porPagina]="paginador.porPagina()" (porPaginaChange)="paginador.cambiarPorPagina($event)" [opcionesPorPagina]="paginador.opcionesPorPagina" nombreDeRegistros="eventos (aprox.)" />
        }
      }
      <ng-template #celda let-e let-c="columna">
        @switch (c.clave) {
          @case ('accion') { <ap-chip-estado [tono]="tono(e.accion)">{{ e.accion }}</ap-chip-estado> }
          @case ('hashRegistro') { <code class="hash" [attr.title]="e.hashRegistro">{{ corto(e.hashRegistro) }}</code> }
          @case ('actor') { {{ e.actorRol || corto(e.actorUsuarioId) || '—' }} }
          @case ('acciones') { <ap-acciones-de-fila [fila]="'evento #' + e.secuencia" [ver]="true" [textoVer]="t.verDetalle" (verPulsado)="abrirDetalle(e)" /> }
          @default { {{ e[c.clave] }} }
        }
      </ng-template>
      <ap-cajon-detalle-evento [evento]="eventoSeleccionado()" [(abierto)]="cajonAbierto" />
    </section>
  `,
  styles: `
    :host { display: block; min-width: 0; }
    /* La tabla se desplaza dentro de su marco: las acciones van en una sola línea en vez de apilarse y estirar la fila. */
    ap-acciones-de-fila { flex-wrap: nowrap; }
    .pantalla { display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s5); min-width: 0; }
    .barra { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: var(--s3); }
    .resumen { margin: 0; color: var(--text-2); font: var(--t-cuerpo-chico); }
    .hash { font-family: var(--f-mono); font: var(--t-ayuda); color: var(--text-2); background: var(--surface-2); padding: 0 var(--s1); border-radius: var(--r-sm); }
  `,
})
export class PantallaBitacora {
  /** La bitácora es auditoría: la descarga en el navegador solo con `exportar:bitacora` (regla 90.2). */
  protected readonly exportador = inject(Exportador)
  constructor() {
    inject(ExplicadorDeVista).fijar(this.t.proposito)
    // Cada página que llega le da al paginador el cursor de la siguiente (y lo deja avanzar si se pidió «Ir a N»).
    effect(() => {
      if (this.recurso.status() !== 'resolved' || !this.recurso.hasValue()) return
      const pagina = this.recurso.value()
      if (pagina) untracked(() => this.paginador.recibir(pagina))
    })
  }

  protected readonly t = textosSistemas.bitacora
  protected readonly columnas = COLUMNAS
  protected readonly exportables = EXPORTABLES
  readonly filtroEntidad = signal('')
  readonly filtroAccion = signal('')
  readonly eventoSeleccionado = signal<EventoBitacora | null>(null)
  readonly cajonAbierto = signal(false)
  protected readonly paginador = crearPaginadorPorCursor({ porPagina: 25 })

  protected readonly recurso = bitacora({ entidad: this.filtroEntidad, accion: this.filtroAccion, antesDe: this.paginador.antesDe, antesDeId: this.paginador.antesDeId, limite: this.paginador.porPagina })
  protected readonly carga = cargaDe(this.recurso)
  private readonly filas = filasDe<FilaBitacora, PaginaDeBitacora>(this.recurso, (p) => p.items)
  protected readonly identidad = (e: FilaBitacora) => e.id
  /** Búsqueda y orden sobre la página cargada; la paginación es del servidor, por eso `porPagina` cubre el máximo del contrato. */
  protected readonly enPagina = crearListado<FilaBitacora>({
    filas: this.filas,
    camposDeBusqueda: ['entidad', 'accion', 'actorRol', 'origen', 'motivo', 'hashRegistro', 'entidadId'],
    porPagina: 100,
    opcionesPorPagina: [100],
  })
  protected readonly hayFiltrosDeServidor = computed(() => this.filtroEntidad() !== '' || this.filtroAccion() !== '')
  protected readonly selectores = computed<Selector[]>(() => [
    { nombre: 'entidad', etiqueta: 'Entidad', valor: this.filtroEntidad(), opciones: ENTIDADES, textoTodos: 'Todas las entidades' },
    { nombre: 'accion', etiqueta: 'Acción', valor: this.filtroAccion(), opciones: ACCIONES, textoTodos: 'Todas las acciones' },
  ])
  protected readonly resumen = computed(() => {
    if (this.carga().tipo !== 'lista') return ''
    const enPagina = this.enPagina.filas().length
    const base = `Página ${this.paginador.pagina()} · ${enPagina} eventos`
    return this.enPagina.hayCriterios() ? `${this.enPagina.total()} de ${enPagina} eventos en la página ${this.paginador.pagina()}` : base
  })
  protected readonly cifras = computed<Cifra[]>(() => {
    const f = this.filas()
    const total = this.recurso.hasValue() ? this.recurso.value()?.totalAproximado : undefined
    return [
      { etiqueta: 'Eventos (aprox.)', valor: total === undefined ? '—' : String(total), icono: 'libro' },
      { etiqueta: 'En esta página', valor: String(f.length), icono: 'lista' },
      { etiqueta: 'Entidades', valor: String(new Set(f.map((e) => e.entidad)).size), icono: 'carpeta' },
      { etiqueta: 'Cambios de permisos', valor: String(f.filter((e) => e.accion === 'CAMBIO_PERMISOS').length), icono: 'escudo', tono: 'aviso' },
    ]
  })
  protected readonly mensajeDeError = computed(() => (this.carga().tipo === 'error' ? (this.carga() as { mensaje: string }).mensaje : undefined))
  protected readonly idDePeticion = computed(() => (this.carga().tipo === 'error' ? (this.carga() as { idPeticion?: string }).idPeticion : undefined))
  protected readonly tono = tonoAccion
  protected readonly corto = acortar

  protected filtrar(nombre: string, valor: string): void {
    ;(nombre === 'entidad' ? this.filtroEntidad : this.filtroAccion).set(valor)
    this.paginador.reiniciar()
  }

  protected limpiar(): void {
    this.filtroEntidad.set('')
    this.filtroAccion.set('')
    this.enPagina.limpiar()
    this.paginador.reiniciar()
  }

  protected abrirDetalle(fila: EventoBitacora): void {
    this.eventoSeleccionado.set(fila)
    this.cajonAbierto.set(true)
  }
}
