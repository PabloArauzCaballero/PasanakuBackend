import { ChangeDetectionStrategy, Component, computed, inject, linkedSignal } from '@angular/core'
import { toSignal } from '@angular/core/rxjs-interop'
import { ActivatedRoute, Router, RouterLink } from '@angular/router'
import { Boton } from '@aportaya/ui/boton/boton'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { EstadoDePantalla } from '@aportaya/ui/estado-de-pantalla/estado-de-pantalla'
import { formatearDia } from '@aportaya/ui/fecha/fecha'
import { SelectorSegmentado, type Segmento } from '@aportaya/ui/selector-segmentado/selector-segmentado'
import { TablaDeDatos, type Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import type { Tono } from '@aportaya/ui/tono/tono'
import { ExplicadorDeVista } from '../../nucleo/explicador-de-vista'
import { accionDe, bandejaDePostulaciones, type Cursor, type EstadoDePostulacion, type PaginaDeBandeja, type ResumenDeHabilitacion } from './dominio/cu90-postulaciones'
import { textosPostulaciones } from './textos'

const ESTADOS: readonly EstadoDePostulacion[] = ['PENDIENTE', 'EN_REVISION', 'APROBADA', 'RECHAZADA']

/** El tono del estado: lo que espera una decisión llama la atención, lo cerrado no. */
export function tonoDeEstado(estado: string): Tono {
  if (estado === 'APROBADA') return 'ok'
  if (estado === 'RECHAZADA') return 'error'
  if (estado === 'PENDIENTE' || estado === 'EN_REVISION') return 'aviso'
  return 'neutro'
}

/**
 * CU-90 · Bandeja de postulaciones a organizador. Lista paginada por cursor del servidor (la más antigua primero),
 * filtrable por estado; el estado vive en la URL para poder compartir el enlace. Cada fila lleva a su expediente.
 */
@Component({
  selector: 'ap-bandeja-de-postulaciones',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Boton, CabeceraDePagina, ChipEstado, EstadoDePantalla, RouterLink, SelectorSegmentado, TablaDeDatos],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina [titulo]="t.bandejaTitulo" [descripcion]="t.bandejaProposito" />
      <ap-selector-segmentado [etiqueta]="t.filtrarEstado" [segmentos]="segmentos" [elegido]="estado()" (elegidoChange)="filtrar($event)" />
      <ap-estado-de-pantalla [recurso]="bandeja" [vacio]="esVacia" [mensajeVacio]="t.vacioTitulo" [etiquetaDeCarga]="t.cargando" (reintentar)="bandeja.reload()">
        @if (bandeja.hasValue()) {
          <ap-tabla-de-datos
            [titulo]="t.bandejaTitulo"
            [columnas]="columnas"
            [filas]="filas()"
            [identidad]="identidad"
            densidad="compacta"
            [cabecera]="true"
            [textoRegistros]="t.registros"
            [totalDeRegistros]="filas().length"
          >
            <ng-template #celda let-fila let-columna="columna">
              @switch (columna.clave) {
                @case ('solicitudId') {
                  <a [routerLink]="['/postulaciones-organizador', fila.solicitudId]" class="enlace-mono" [title]="fila.solicitudId" [attr.aria-label]="t.verExpediente + ' ' + fila.solicitudId">
                    <code>{{ acortar(fila.solicitudId) }}</code>
                  </a>
                }
                @case ('fechaSolicitud') { <time [attr.datetime]="fila.fechaSolicitud">{{ dia(fila.fechaSolicitud) }}</time> }
                @case ('reputacionAlSolicitar') { <span class="cifra">{{ fila.reputacionAlSolicitar ?? '—' }}</span> }
                @case ('estado') {
                  <ap-chip-estado [tono]="tono(fila.estado)">{{ t.estados[fila.estado] ?? fila.estado }}</ap-chip-estado>
                  @if (accion(fila)) { <span class="pendiente">{{ t.pendienteDeTi }}</span> }
                }
                @case ('acciones') {
                  <ap-boton variante="fantasma" tamano="sm" icono="ojo" [routerLink]="['/postulaciones-organizador', fila.solicitudId]">{{ t.verExpediente }}</ap-boton>
                }
              }
            </ng-template>
          </ap-tabla-de-datos>
          <nav class="paginas" [attr.aria-label]="t.paginacion">
            <ap-boton variante="secundario" tamano="sm" [deshabilitado]="!cursor()" (pulsado)="cursor.set(null)">{{ t.masAntiguas }}</ap-boton>
            <ap-boton variante="secundario" tamano="sm" [deshabilitado]="!siguiente()" (pulsado)="avanzar()">{{ t.siguientes }}</ap-boton>
          </nav>
        }
      </ap-estado-de-pantalla>
    </section>
  `,
  styles: `
    .pantalla { display: flex; flex-direction: column; gap: var(--s4); }
    .enlace-mono { color: var(--color-primario); text-decoration: none; font-weight: 600; }
    .enlace-mono:hover { text-decoration: underline; }
    .cifra { font-variant-numeric: tabular-nums; }
    .pendiente { display: block; margin-top: var(--s1); font-size: .8125rem; color: var(--text-2); }
    .paginas { display: flex; gap: var(--s2); justify-content: flex-end; flex-wrap: wrap; margin-top: var(--s4); }
    /* «15 ene 2026» en un renglón: la regla global parte el texto de las celdas y la fecha corta quedaba en dos. */
    time { white-space: nowrap; }
    /* Cinco estados no entran a 390 px: el selector desplaza dentro de sí en vez de empujar la página (+173 px). */
    ap-selector-segmentado { max-width: 100%; overflow-x: auto; scrollbar-width: thin; }
  `,
})
export class BandejaDePostulaciones {
  protected readonly t = textosPostulaciones
  private readonly router = inject(Router)
  private readonly ruta = inject(ActivatedRoute)
  private readonly consulta = toSignal(this.ruta.queryParamMap)

  constructor() {
    inject(ExplicadorDeVista).fijar(textosPostulaciones.bandejaProposito)
  }

  protected readonly segmentos: Segmento[] = [{ valor: '', texto: this.t.estadoTodos }, ...ESTADOS.map((e) => ({ valor: e, texto: this.t.estados[e] ?? e }))]

  /** El estado sale de la URL; uno que el contrato no conoce se ignora en vez de mandarse al servidor. */
  protected readonly estado = computed<EstadoDePostulacion | ''>(() => {
    const e = this.consulta()?.get('estado') ?? ''
    return (ESTADOS as readonly string[]).includes(e) ? (e as EstadoDePostulacion) : ''
  })
  /** Cambiar el filtro vuelve a la primera página. */
  protected readonly cursor = linkedSignal<EstadoDePostulacion | '', Cursor>({ source: this.estado, computation: () => null })
  protected readonly bandeja = bandejaDePostulaciones(this.estado, this.cursor)

  protected readonly filas = computed<ResumenDeHabilitacion[]>(() => (this.bandeja.hasValue() ? (this.bandeja.value()?.items ?? []) : []))
  protected readonly siguiente = computed<Cursor>(() => {
    const p = this.bandeja.hasValue() ? this.bandeja.value() : undefined
    return p?.proximoFecha && p.proximoId ? { fecha: p.proximoFecha, id: p.proximoId } : null
  })
  protected readonly esVacia = (p: PaginaDeBandeja): boolean => !p.items?.length
  protected readonly identidad = (r: ResumenDeHabilitacion): string => r.solicitudId

  protected readonly columnas: Columna<ResumenDeHabilitacion & { acciones?: unknown }>[] = [
    { clave: 'solicitudId', titulo: this.t.columnaSolicitud, mono: true },
    { clave: 'fechaSolicitud', titulo: this.t.columnaFecha },
    { clave: 'reputacionAlSolicitar', titulo: this.t.columnaReputacion, numerica: true },
    { clave: 'estado', titulo: this.t.columnaEstado },
    { clave: 'acciones', titulo: this.t.columnaAccion, fijaDerecha: true },
  ]

  protected filtrar(estado: string): void {
    void this.router.navigate([], { relativeTo: this.ruta, queryParams: { estado: estado || null }, queryParamsHandling: 'merge' })
  }

  protected avanzar(): void {
    const s = this.siguiente()
    if (s) this.cursor.set(s)
  }

  protected readonly accion = accionDe
  protected readonly tono = tonoDeEstado
  protected dia(iso: string): string {
    return iso ? formatearDia(iso.slice(0, 10)) : '—'
  }
  protected acortar(uuid: string): string {
    return uuid.length > 12 ? `${uuid.slice(0, 8)}…` : uuid
  }
}
