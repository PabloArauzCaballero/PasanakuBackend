import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core'
import { toSignal } from '@angular/core/rxjs-interop'
import { ActivatedRoute, Router, RouterLink } from '@angular/router'
import { BarraDeFiltros, type Selector } from '@aportaya/ui/barra-de-filtros/barra-de-filtros'
import { Boton } from '@aportaya/ui/boton/boton'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { EstadoDePantalla } from '@aportaya/ui/estado-de-pantalla/estado-de-pantalla'
import { EstadoVacio } from '@aportaya/ui/estado-vacio/estado-vacio'
import { formatearDia } from '@aportaya/ui/fecha/fecha'
import { Monto } from '@aportaya/ui/monto/monto'
import { TablaDeDatos, type Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import type { Tono } from '@aportaya/ui/tono/tono'
import { ExplicadorDeVista } from '../../nucleo/explicador-de-vista'
import { ESTADOS_DE_GRUPO, gruposDeBackoffice, type GrupoDeBackoffice } from './dominio/consulta-de-grupos'
import { textosGrupos } from './textos'

/** El tono del estado del grupo: en marcha es bueno, en pausa avisa, terminado mal es error, el resto neutro. */
export function tonoDeGrupo(estado: string): Tono {
  if (estado === 'EN_CURSO' || estado === 'ACTIVO' || estado === 'CONFORMADO') return 'ok'
  if (estado === 'SUSPENDIDO' || estado === 'ABIERTO_A_INSCRIPCION') return 'aviso'
  if (estado === 'CANCELADO' || estado === 'DISUELTO_ANTICIPADAMENTE') return 'error'
  return 'neutro'
}

/**
 * Consulta de grupos del backoffice (solo lectura). El estado filtra en el servidor y vive en la URL; la búsqueda por
 * nombre o código filtra en el cliente sobre lo traído (hasta 200, el tope del contrato).
 */
@Component({
  selector: 'ap-listado-de-grupos',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BarraDeFiltros, Boton, CabeceraDePagina, ChipEstado, EstadoDePantalla, EstadoVacio, Monto, RouterLink, TablaDeDatos],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina [titulo]="t.titulo" [descripcion]="t.proposito" />
      <ap-barra-de-filtros
        [etiquetaDeBusqueda]="t.busqueda"
        [ayudaDeBusqueda]="t.ayudaBusqueda"
        [busqueda]="q()"
        [selectores]="selectores()"
        [textoLimpiar]="t.limpiar"
        (busquedaChange)="filtrar('q', $event)"
        (selectorChange)="filtrar($event.nombre, $event.valor)"
        (limpiar)="limpiar()"
      />
      <ap-estado-de-pantalla [recurso]="grupos" [vacio]="esVacio" [mensajeVacio]="t.vacioTitulo" [etiquetaDeCarga]="t.cargando" (reintentar)="grupos.reload()">
        @if (encontrados().length === 0) {
          <ap-estado-vacio motivo="porFiltro" [titulo]="t.vacioFiltroTitulo" [explicacion]="t.vacioFiltroExplicacion" (actuar)="limpiar()" />
        } @else {
          <ap-tabla-de-datos
            [titulo]="t.titulo"
            [columnas]="columnas"
            [filas]="encontrados()"
            [identidad]="identidad"
            densidad="compacta"
            [cabecera]="true"
            [textoRegistros]="t.registros"
            [totalDeRegistros]="encontrados().length"
          >
            <ng-template #celda let-fila let-columna="columna">
              @switch (columna.clave) {
                @case ('codigo') {
                  <a [routerLink]="['/grupos', fila.grupoId]" class="enlace-mono" [attr.aria-label]="t.verGrupo + ' ' + fila.codigo"><code>{{ fila.codigo }}</code></a>
                }
                @case ('nombre') { <strong>{{ fila.nombre }}</strong> }
                @case ('montoAporte') { <ap-monto [monto]="fila.montoAporte" [moneda]="fila.moneda" /> }
                @case ('cuposOcupados') { <span class="cifra">{{ fila.cuposOcupados }} / {{ fila.cuposTotales }}</span> }
                @case ('fechaInicio') { {{ fila.fechaInicio ? dia(fila.fechaInicio) : t.sinFecha }} }
                @case ('estado') { <ap-chip-estado [tono]="tono(fila.estado)">{{ t.estados[fila.estado] ?? fila.estado }}</ap-chip-estado> }
                @case ('acciones') {
                  <ap-boton variante="fantasma" tamano="sm" icono="ojo" [routerLink]="['/grupos', fila.grupoId]">{{ t.verGrupo }}</ap-boton>
                }
              }
            </ng-template>
          </ap-tabla-de-datos>
        }
      </ap-estado-de-pantalla>
    </section>
  `,
  styles: `
    .pantalla { display: flex; flex-direction: column; gap: var(--s4); }
    .enlace-mono { color: var(--color-primario); text-decoration: none; font-weight: 600; }
    .enlace-mono:hover { text-decoration: underline; }
    .cifra { font-variant-numeric: tabular-nums; }
  `,
})
export class ListadoDeGrupos {
  protected readonly t = textosGrupos
  private readonly router = inject(Router)
  private readonly ruta = inject(ActivatedRoute)
  private readonly consulta = toSignal(this.ruta.queryParamMap)

  constructor() {
    inject(ExplicadorDeVista).fijar(textosGrupos.proposito)
  }

  protected readonly q = computed(() => this.consulta()?.get('q') ?? '')
  /** Un estado que el esquema no conoce no se manda al servidor. */
  protected readonly estado = computed(() => {
    const e = this.consulta()?.get('estado') ?? ''
    return (ESTADOS_DE_GRUPO as readonly string[]).includes(e) ? e : ''
  })
  protected readonly grupos = gruposDeBackoffice(this.estado)

  protected readonly selectores = computed<Selector[]>(() => [
    {
      nombre: 'estado',
      etiqueta: this.t.filtrarEstado,
      valor: this.estado(),
      textoTodos: this.t.todos,
      opciones: ESTADOS_DE_GRUPO.map((e) => ({ valor: e, texto: this.t.estados[e] ?? e })),
    },
  ])

  protected readonly encontrados = computed<GrupoDeBackoffice[]>(() => {
    const todos = this.grupos.hasValue() ? (this.grupos.value() ?? []) : []
    const termino = this.q().trim().toLowerCase()
    if (!termino) return todos
    return todos.filter((g) => g.nombre.toLowerCase().includes(termino) || g.codigo.toLowerCase().includes(termino))
  })
  protected readonly esVacio = (g: GrupoDeBackoffice[]): boolean => !g?.length
  protected readonly identidad = (g: GrupoDeBackoffice): string => g.grupoId

  protected readonly columnas: Columna<GrupoDeBackoffice & { acciones?: unknown }>[] = [
    { clave: 'codigo', titulo: this.t.columnaCodigo, mono: true },
    { clave: 'nombre', titulo: this.t.columnaNombre },
    { clave: 'montoAporte', titulo: this.t.columnaAporte, numerica: true },
    { clave: 'cuposOcupados', titulo: this.t.columnaCupos, numerica: true },
    { clave: 'fechaInicio', titulo: this.t.columnaInicio },
    { clave: 'estado', titulo: this.t.columnaEstado },
    { clave: 'acciones', titulo: this.t.columnaAccion, fijaDerecha: true },
  ]

  protected filtrar(clave: string, valor: string): void {
    void this.router.navigate([], { relativeTo: this.ruta, queryParams: { [clave]: valor || null }, queryParamsHandling: 'merge' })
  }

  protected limpiar(): void {
    void this.router.navigate([], { relativeTo: this.ruta, queryParams: {} })
  }

  protected readonly tono = tonoDeGrupo
  protected dia(iso: string): string {
    return formatearDia(iso.slice(0, 10))
  }
}
