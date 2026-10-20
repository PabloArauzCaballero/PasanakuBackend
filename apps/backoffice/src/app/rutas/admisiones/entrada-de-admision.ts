import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core'
import { toSignal } from '@angular/core/rxjs-interop'
import { ActivatedRoute, Router, RouterLink } from '@angular/router'
import { BarraDeFiltros, type Selector } from '@aportaya/ui/barra-de-filtros/barra-de-filtros'
import { Boton } from '@aportaya/ui/boton/boton'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { Campo } from '@aportaya/ui/campo/campo'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { EstadoDePantalla } from '@aportaya/ui/estado-de-pantalla/estado-de-pantalla'
import { EstadoVacio } from '@aportaya/ui/estado-vacio/estado-vacio'
import { formatearDia } from '@aportaya/ui/fecha/fecha'
import { Paginacion } from '@aportaya/ui/paginacion/paginacion'
import { TablaDeDatos, type Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import type { Tono } from '@aportaya/ui/tono/tono'
import { ExplicadorDeVista } from '../../nucleo/explicador-de-vista'
import { solicitudesDeAdmision, type SolicitudDeAdmision } from './dominio/cu68-admision'
import { textosAdmisiones } from './textos'

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i
const FILAS_POR_PAGINA = [10, 20, 50]

/**
 * Bandeja de solicitudes de admisión humana a grupos (CU-68).
 * Exhibe la lista paginada del servidor con filtros por estado y buscador textual,
 * sincronizando el estado con los parámetros de la URL, y preserva la acción rápida
 * de apertura directa por UUID.
 */
@Component({
  selector: 'ap-entrada-de-admision',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    BarraDeFiltros,
    Boton,
    CabeceraDePagina,
    Campo,
    ChipEstado,
    EstadoDePantalla,
    EstadoVacio,
    Paginacion,
    RouterLink,
    TablaDeDatos,
  ],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina
        [titulo]="t.bandejaTitulo"
        [descripcion]="t.bandejaProposito"
        icono="grupo"
      />

      <section class="tarjeta-directa" aria-labelledby="titulo-abrir-directo">
        <div class="cabecera-directa">
          <h2 id="titulo-abrir-directo" class="titulo-directo">{{ t.buscarTitulo }}</h2>
          <p class="descripcion-directa">{{ t.buscarProposito }}</p>
        </div>
        <form class="formulario-directo" (submit)="$event.preventDefault(); abrir()" novalidate>
          <div class="campo-directo">
            <ap-campo
              [etiqueta]="t.buscarEtiqueta"
              [ayuda]="t.buscarAyuda"
              [error]="error()"
              [(valor)]="id"
            />
          </div>
          <div class="accion-directa">
            <ap-boton variante="primario" tipo="submit">{{ t.buscarAccion }}</ap-boton>
          </div>
        </form>
      </section>

      <ap-barra-de-filtros
        [etiquetaDeBusqueda]="t.busqueda"
        [ayudaDeBusqueda]="t.ayudaBusqueda"
        [busqueda]="filtros().q"
        [selectores]="selectores()"
        [textoLimpiar]="t.limpiarFiltros"
        (busquedaChange)="filtrar('q', $event)"
        (selectorChange)="filtrar($event.nombre, $event.valor)"
        (limpiar)="limpiarTodo()"
      />

      <ap-estado-de-pantalla
        [recurso]="recurso"
        [vacio]="esVacio"
        [mensajeVacio]="t.vacioTitulo"
        [etiquetaDeCarga]="t.cargando"
        (reintentar)="recurso.reload()"
      >
        @if (encontradas().length === 0) {
          <ap-estado-vacio
            motivo="porFiltro"
            [titulo]="t.vacioFiltroTitulo"
            [explicacion]="t.vacioFiltroExplicacion"
            (actuar)="limpiarTodo()"
          />
        } @else {
          <ap-tabla-de-datos
            [titulo]="t.bandejaTitulo"
            [columnas]="columnas"
            [filas]="filasPaginadas()"
            [identidad]="identidad"
            densidad="compacta"
            [cabecera]="true"
            [textoRegistros]="t.registros"
            [totalDeRegistros]="encontradas().length"
          >
            <ng-template #celda let-fila let-columna="columna">
              @switch (columna.clave) {
                @case ('solicitudId') {
                  <a
                    [routerLink]="['/admisiones', fila.solicitudId]"
                    class="enlace-mono"
                    [attr.aria-label]="'Abrir solicitud ' + fila.solicitudId"
                  >
                    <code>{{ acortarUuid(fila.solicitudId) }}</code>
                  </a>
                }
                @case ('postulante') {
                  <strong>{{ fila.postulante }}</strong>
                }
                @case ('grupoCodigo') {
                  <span class="chip-grupo">{{ fila.grupoCodigo }}</span>
                }
                @case ('cuposSolicitados') {
                  <span class="cupos">{{ fila.cuposSolicitados }}</span>
                }
                @case ('puntaje') {
                  <span class="puntaje">{{ fila.puntaje ?? '—' }}</span>
                }
                @case ('fechaSolicitud') {
                  <time [attr.datetime]="fila.fechaSolicitud">{{ dia(fila.fechaSolicitud) }}</time>
                }
                @case ('estado') {
                  <ap-chip-estado [tono]="tonoEstado(fila.estado)">
                    {{ fila.estado }}
                  </ap-chip-estado>
                }
                @case ('acciones') {
                  <ap-boton
                    variante="fantasma"
                    tamano="sm"
                    [routerLink]="['/admisiones', fila.solicitudId]"
                  >
                    {{ t.verExpediente }}
                  </ap-boton>
                }
                @default {
                  {{ fila[columna.clave] }}
                }
              }
            </ng-template>
          </ap-tabla-de-datos>

          <ap-paginacion
            etiqueta="Paginación de solicitudes de ingreso"
            [pagina]="paginaActual()"
            (paginaChange)="filtrar('pagina', '' + $event)"
            [totalDeFilas]="encontradas().length"
            [porPagina]="porPagina()"
            (porPaginaChange)="filtrar('filas', '' + $event)"
            [opcionesPorPagina]="opcionesDeFilas"
            [nombreDeRegistros]="t.registros"
          />
        }
      </ap-estado-de-pantalla>
    </section>
  `,
  styles: `
    .pantalla {
      padding: var(--s5);
      max-width: 76rem;
      display: flex;
      flex-direction: column;
      gap: var(--s4);
    }
    .tarjeta-directa {
      padding: var(--s4);
      background: var(--surface);
      border: var(--borde-fino) solid var(--border);
      border-radius: var(--r-lg);
      display: flex;
      flex-direction: column;
      gap: var(--s3);
      box-shadow: var(--sombra-1);
    }
    .titulo-directo {
      margin: 0;
      font-size: 1.1rem;
      font-weight: 600;
      color: var(--text);
    }
    .descripcion-directa {
      margin: var(--s1) 0 0;
      font-size: 0.875rem;
      color: var(--text-2);
    }
    .formulario-directo {
      display: flex;
      align-items: flex-start;
      gap: var(--s3);
      flex-wrap: wrap;
    }
    .campo-directo {
      flex: 1 1 20rem;
      min-width: min(20rem, 100%);
    }
    .accion-directa {
      padding-top: 1.625rem;
    }
    .enlace-mono {
      color: var(--color-primario);
      text-decoration: none;
      font-weight: 600;
    }
    .enlace-mono:hover {
      text-decoration: underline;
    }
    .chip-grupo {
      font-family: var(--f-mono, monospace);
      font-size: 0.85em;
      padding: var(--s1) var(--s2);
      border-radius: var(--r-sm);
      background: var(--surface-2);
      border: var(--borde-fino) solid var(--border);
      color: var(--text);
    }
    .cupos,
    .puntaje {
      font-variant-numeric: tabular-nums;
      font-weight: 500;
    }
  `,
})
export class EntradaDeAdmision {
  protected readonly t = textosAdmisiones
  protected readonly opcionesDeFilas = FILAS_POR_PAGINA

  private readonly router = inject(Router)
  private readonly ruta = inject(ActivatedRoute)
  private readonly consulta = toSignal(this.ruta.queryParamMap)

  readonly id = signal('')
  readonly error = signal('')

  constructor() {
    inject(ExplicadorDeVista).fijar(textosAdmisiones.bandejaProposito)
  }

  protected readonly filtros = computed(() => {
    const q = this.consulta()
    return {
      q: q?.get('q') ?? '',
      estado: q?.get('estado') ?? '',
    }
  })

  protected readonly paginaActual = computed(() => Number(this.consulta()?.get('pagina') ?? 1) || 1)
  protected readonly porPagina = computed(() => {
    const n = Number(this.consulta()?.get('filas') ?? 20)
    return FILAS_POR_PAGINA.includes(n) ? n : 20
  })

  protected readonly recurso = solicitudesDeAdmision()

  protected readonly esVacio = (items: SolicitudDeAdmision[]) => !items || items.length === 0

  protected readonly encontradas = computed<SolicitudDeAdmision[]>(() => {
    const items = this.recurso.value() ?? []
    const f = this.filtros()
    const termino = f.q.trim().toLowerCase()
    const filtroEstado = f.estado.trim().toUpperCase()

    return items.filter((item) => {
      const coincideEstado = !filtroEstado || item.estado.toUpperCase() === filtroEstado
      if (!coincideEstado) return false
      if (!termino) return true
      return (
        item.postulante.toLowerCase().includes(termino) ||
        item.grupoCodigo.toLowerCase().includes(termino) ||
        item.solicitudId.toLowerCase().includes(termino) ||
        (item.mensaje && item.mensaje.toLowerCase().includes(termino))
      )
    })
  })

  protected readonly filasPaginadas = computed<SolicitudDeAdmision[]>(() => {
    const inicio = (this.paginaActual() - 1) * this.porPagina()
    return this.encontradas().slice(inicio, inicio + this.porPagina())
  })

  protected readonly selectores = computed<Selector[]>(() => [
    {
      nombre: 'estado',
      etiqueta: this.t.filtrarEstado,
      valor: this.filtros().estado,
      textoTodos: this.t.estadoTodos,
      opciones: [
        { valor: 'PENDIENTE', texto: this.t.estadoPendiente },
        { valor: 'ACEPTADA', texto: this.t.estadoAceptada },
        { valor: 'RECHAZADA', texto: this.t.estadoRechazada },
        { valor: 'ESCALADA', texto: this.t.estadoEscalada },
      ],
    },
  ])

  protected readonly identidad = (s: SolicitudDeAdmision) => s.solicitudId

  protected readonly columnas: Columna<SolicitudDeAdmision & { acciones?: string }>[] = [
    { clave: 'solicitudId', titulo: 'Identificador', mono: true },
    { clave: 'postulante', titulo: 'Postulante', ordenable: true },
    { clave: 'grupoCodigo', titulo: 'Grupo', mono: true },
    { clave: 'cuposSolicitados', titulo: 'Cupos', numerica: true },
    { clave: 'puntaje', titulo: 'Puntaje', numerica: true },
    { clave: 'fechaSolicitud', titulo: 'Fecha', ordenable: true },
    { clave: 'estado', titulo: 'Estado' },
    { clave: 'acciones', titulo: 'Acción', fijaDerecha: true },
  ]

  protected tonoEstado(estado: string): Tono {
    const est = estado.toUpperCase()
    if (est === 'ACEPTADA' || est === 'ACTIVO') return 'ok'
    if (est === 'RECHAZADA' || est === 'CANCELADA') return 'error'
    if (est === 'PENDIENTE' || est === 'ESCALADA') return 'aviso'
    return 'neutro'
  }

  protected dia(iso: string): string {
    if (!iso) return '—'
    return formatearDia(iso.slice(0, 10))
  }

  protected acortarUuid(uuid: string): string {
    if (!uuid || uuid.length <= 12) return uuid
    return `${uuid.slice(0, 8)}…`
  }

  protected filtrar(clave: string, valor: string): void {
    const cambios: Record<string, string | null> = { [clave]: valor || null }
    if (clave !== 'pagina') cambios['pagina'] = null
    void this.router.navigate([], {
      relativeTo: this.ruta,
      queryParams: cambios,
      queryParamsHandling: 'merge',
    })
  }

  protected limpiarTodo(): void {
    void this.router.navigate([], {
      relativeTo: this.ruta,
      queryParams: { q: null, estado: null, pagina: null },
      queryParamsHandling: 'merge',
    })
  }

  protected abrir(): void {
    const valor = this.id().trim()
    if (!UUID.test(valor)) {
      this.error.set(this.t.buscarError)
      return
    }
    this.error.set('')
    void this.router.navigate(['/admisiones', valor.toLowerCase()])
  }
}
