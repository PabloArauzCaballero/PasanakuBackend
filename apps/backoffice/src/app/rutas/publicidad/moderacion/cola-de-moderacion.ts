import { ChangeDetectionStrategy, Component, computed, inject, signal, viewChild } from '@angular/core'
import { AccionesDeFila } from '@aportaya/ui/acciones-de-fila/acciones-de-fila'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { Fecha } from '@aportaya/ui/fecha/fecha'
import { Icono } from '@aportaya/ui/icono/icono'
import { crearListado } from '@aportaya/ui/listado/crear-listado'
import { ListadoDeDatos } from '@aportaya/ui/listado/listado'
import { TarjetaKPI } from '@aportaya/ui/tarjeta-kpi/tarjeta-kpi'
import type { Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import type { ColumnaExportable } from '@aportaya/ui/exportar-listado/exportar'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { Sesion } from '../../../nucleo/sesion'
import { BarraDeLotes } from '../barra-de-lotes'
import { lotesDePiezas, type EstadoModeracion, type PiezaCreativa } from '../dominio/cu112-moderacion'
import { filtroEnElServidor } from '../dominio/lotes-por-cursor'
import { textosPublicidad } from '../textos'
import { DialogoDeModeracion, type DecisionDeModeracion } from './dialogo-de-moderacion'

const t = textosPublicidad.moderacion
const RECURSOS: Record<PiezaCreativa['tipoRecurso'], string> = { IMAGEN: 'Imagen', VIDEO: 'Video' }

/**
 * CU-112 · **Cola de moderación — gate de entrada del carril.** Toda pieza nace PENDIENTE y solo pasa a APROBADA o
 * RECHAZADA acá. Abre en la cola (PENDIENTE, lo que el contrato devuelve sin filtro); el selector de estado deja mirar
 * lo ya resuelto, sin acciones. Cada fila ofrece «Ver pieza» (el recurso, en otra pestaña) y, si está pendiente y la
 * sesión tiene `PUBLICIDAD_MODERAR`, «Aprobar» y «Rechazar». Lo que el servidor confirma queda marcado en la fila.
 */
@Component({
  selector: 'ap-cola-de-moderacion',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AccionesDeFila, BarraDeLotes, CabeceraDePagina, ChipEstado, DialogoDeModeracion, Fecha, Icono, ListadoDeDatos, TarjetaKPI],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina eyebrow="Publicidad" [titulo]="t.titulo" [descripcion]="t.proposito" icono="escudo" />
      <!-- Las cifras cuentan lo cargado: sin carga (error o cargando) no se muestran, un «0» afirmaría algo que no se midió. -->
      @if (lotes.carga().tipo === 'lista') {
        <div class="cifras">
          <ap-tarjeta-kpi etiqueta="En la vista" [valor]="'' + lotes.filas().length" icono="imagen" [pista]="lotes.hayMas() ? 'Hay más en el servidor' : 'Todas las del filtro'" />
          <ap-tarjeta-kpi etiqueta="Aprobadas ahora" [valor]="'' + cuenta('APROBADA')" icono="aprobar" tono="ok" pista="En esta sesión" />
          <ap-tarjeta-kpi etiqueta="Rechazadas ahora" [valor]="'' + cuenta('RECHAZADA')" icono="rechazar" [tono]="cuenta('RECHAZADA') ? 'error' : 'neutro'" pista="En esta sesión" />
        </div>
      }
      <ap-listado [titulo]="t.titulo" [columnas]="columnas" [listado]="listado" [carga]="lotes.carga()" [identidad]="porId" [exportables]="exportables" nombreDeRegistros="piezas" [ayudaDeBusqueda]="t.ayudaBusqueda" [textoVacio]="t.sinPiezas" [explicacionVacio]="t.explicacionVacio" (reintentar)="lotes.recargar()">
        <ap-barra-de-lotes accionesDeListado [hayMas]="lotes.hayMas()" [cargando]="lotes.cargandoMas()" [error]="lotes.errorDeMas()" (cargarMas)="lotes.cargarMas()" />
        <ng-template #celda let-p let-columna="columna">
          @switch (columna.clave) {
            @case ('titulo') { <span class="titulo">{{ p.titulo }}</span> }
            @case ('texto') { <span class="texto">{{ p.texto || '—' }}</span> }
            @case ('tipoRecurso') { <span class="recurso"><ap-icono [nombre]="p.tipoRecurso === 'VIDEO' ? 'reproducir' : 'imagen'" tamano="s4" />{{ textoDeRecurso(p) }}</span> }
            @case ('estadoModeracion') { <ap-chip-estado [tono]="tonoDe(p)">{{ textoDeEstado(p) }}</ap-chip-estado> }
            @case ('creadaEn') { <ap-fecha [iso]="p.creadaEn" [conHora]="false" /> }
            @default { {{ p[columna.clave] }} }
          }
        </ng-template>
        <ng-template #acciones let-p>
          <ap-acciones-de-fila [fila]="p.titulo">
            <a apAccionDeFila [href]="p.urlRecurso" target="_blank" rel="noopener noreferrer" [attr.aria-label]="t.verPieza(p.titulo)"><ap-icono nombre="externo" tamano="s4" /><span>Ver pieza</span></a>
            @if (estadoDe(p) === 'PENDIENTE' && puedeModerar()) {
              <button type="button" apAccionDeFila [attr.aria-label]="t.aprobar + ' ' + p.titulo" (click)="dialogo().abrir(p, 'APROBADA')"><ap-icono nombre="aprobar" tamano="s4" /><span>Aprobar</span></button>
              <button type="button" apAccionDeFila class="peligro" [attr.aria-label]="t.rechazar + ' ' + p.titulo" (click)="dialogo().abrir(p, 'RECHAZADA')"><ap-icono nombre="rechazar" tamano="s4" /><span>Rechazar</span></button>
            }
          </ap-acciones-de-fila>
        </ng-template>
      </ap-listado>
    </section>
    <ap-dialogo-de-moderacion (resuelta)="alResolver($event.pieza, $event.decision)" />
  `,
  styles: `
    /* El host de ap-listado es una grilla de columna automática: sin esto, una tabla ancha lo ensancha (pedido al dueño del kit). */
    ap-listado { grid-template-columns: minmax(0, 1fr); }
    .pantalla { display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s4); min-width: 0; }
    .cifras { display: grid; gap: var(--s3); grid-template-columns: repeat(auto-fit, minmax(min(100%, 11rem), 1fr)); }
    .titulo { display: block; min-width: 14ch; max-width: 34ch; font-weight: 600; color: var(--text); white-space: normal; overflow-wrap: anywhere; }
    .texto { display: block; min-width: 18ch; max-width: 40ch; color: var(--text-2); font: var(--t-cuerpo-chico); white-space: normal; overflow-wrap: anywhere; }
    .recurso { display: inline-flex; align-items: center; gap: var(--s1); }
    .peligro { color: var(--err-texto); }
  `,
})
export class ColaDeModeracion {
  private readonly sesion = inject(Sesion)
  protected readonly t = t
  protected readonly dialogo = viewChild.required(DialogoDeModeracion)
  protected readonly lotes = lotesDePiezas()
  /** Lo que el servidor ya confirmó (2xx), por pieza: la fila cambia de estado sin esperar a recargar. */
  protected readonly resueltas = signal<Readonly<Record<string, DecisionDeModeracion>>>({})
  private readonly filas = computed(() => this.lotes.filas().map((p) => ({ ...p, estadoModeracion: this.resueltas()[p.piezaCreativaId] ?? p.estadoModeracion })))
  protected readonly porId = (p: PiezaCreativa) => p.piezaCreativaId
  protected readonly puedeModerar = () => this.sesion.puede('PUBLICIDAD_MODERAR')
  protected readonly estadoDe = (p: PiezaCreativa) => p.estadoModeracion
  protected readonly textoDeEstado = (p: PiezaCreativa) => t.estados[p.estadoModeracion]
  protected readonly textoDeRecurso = (p: PiezaCreativa) => RECURSOS[p.tipoRecurso]
  protected readonly tonoDe = (p: PiezaCreativa) => (p.estadoModeracion === 'APROBADA' ? 'ok' : p.estadoModeracion === 'RECHAZADA' ? 'error' : 'aviso')
  protected readonly cuenta = (d: DecisionDeModeracion) => Object.values(this.resueltas()).filter((x) => x === d).length

  protected readonly listado = crearListado<PiezaCreativa>({
    filas: this.filas,
    camposDeBusqueda: ['titulo', 'texto', (p) => RECURSOS[p.tipoRecurso], 'anuncianteId', 'piezaCreativaId'],
    filtros: [
      {
        nombre: 'estado',
        etiqueta: 'Estado de moderación',
        valor: (p) => (p.estadoModeracion === 'PENDIENTE' ? '' : p.estadoModeracion),
        opciones: (['APROBADA', 'RECHAZADA'] as EstadoModeracion[]).map((v) => ({ valor: v, texto: t.estados[v] })),
        textoTodos: 'Pendientes (la cola)',
        ayuda: 'Se consulta al servidor. Sin elegir, la cola de pendientes.',
      },
      { nombre: 'tipo', etiqueta: 'Recurso', valor: (p) => p.tipoRecurso, texto: (v) => RECURSOS[v as PiezaCreativa['tipoRecurso']] ?? v },
    ],
  })
  protected readonly columnas: Columna<PiezaCreativa>[] = [
    { clave: 'titulo', titulo: 'Título', ordenable: true },
    { clave: 'texto', titulo: 'Texto' },
    { clave: 'tipoRecurso', titulo: 'Recurso', ordenable: true },
    { clave: 'estadoModeracion', titulo: 'Estado', ordenable: true },
    { clave: 'creadaEn', titulo: 'Subida', ordenable: true },
  ]
  protected readonly exportables: ColumnaExportable<PiezaCreativa>[] = [
    { clave: 'piezaCreativaId', titulo: 'Identificador' },
    { clave: 'titulo', titulo: 'Título' },
    { clave: 'texto', titulo: 'Texto' },
    { clave: 'tipoRecurso', titulo: 'Recurso', valor: (p) => RECURSOS[p.tipoRecurso] },
    { clave: 'estadoModeracion', titulo: 'Estado', valor: (p) => t.estados[p.estadoModeracion] },
    { clave: 'urlRecurso', titulo: 'Dirección del recurso' },
    { clave: 'anuncianteId', titulo: 'Anunciante' },
    { clave: 'creadaEn', titulo: 'Subida' },
  ]

  constructor() {
    inject(ExplicadorDeVista).fijar(t.proposito)
    filtroEnElServidor(this.listado, this.lotes)
  }

  protected alResolver(p: PiezaCreativa, decision: DecisionDeModeracion): void {
    this.resueltas.update((r) => ({ ...r, [p.piezaCreativaId]: decision }))
  }
}
