import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core'
import { toSignal } from '@angular/core/rxjs-interop'
import { ActivatedRoute, Router } from '@angular/router'
import { BarraDeFiltros, type Selector } from '@aportaya/ui/barra-de-filtros/barra-de-filtros'
import { Boton } from '@aportaya/ui/boton/boton'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { Campo } from '@aportaya/ui/campo/campo'
import { EstadoDePantalla } from '@aportaya/ui/estado-de-pantalla/estado-de-pantalla'
import { EstadoVacio } from '@aportaya/ui/estado-vacio/estado-vacio'
import { Paginacion } from '@aportaya/ui/paginacion/paginacion'
import { ExplicadorDeVista } from '../../nucleo/explicador-de-vista'
import { solicitudesDeAdmision, type SolicitudDeAdmision } from './dominio/cu68-admision'
import { TablaDeAdmisiones } from './tabla-de-admisiones'
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
  imports: [BarraDeFiltros, Boton, CabeceraDePagina, Campo, EstadoDePantalla, EstadoVacio, Paginacion, TablaDeAdmisiones],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina [titulo]="t.bandejaTitulo" [descripcion]="t.bandejaProposito" />

      <section class="tarjeta-directa" aria-labelledby="titulo-abrir-directo">
        <div class="cabecera-directa">
          <h2 id="titulo-abrir-directo" class="titulo-directo">{{ t.buscarTitulo }}</h2>
          <p class="descripcion-directa">{{ t.buscarProposito }}</p>
        </div>
        <form class="formulario-directo" (submit)="$event.preventDefault(); abrir()" novalidate>
          <div class="campo-directo">
            <ap-campo [etiqueta]="t.buscarEtiqueta" [ayuda]="t.buscarAyuda" [error]="error()" [(valor)]="id" />
          </div>
          <div class="accion-directa">
            <ap-boton variante="primario" tipo="submit" icono="buscar">{{ t.buscarAccion }}</ap-boton>
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
          <ap-estado-vacio motivo="porFiltro" [titulo]="t.vacioFiltroTitulo" [explicacion]="t.vacioFiltroExplicacion" (actuar)="limpiarTodo()" />
        } @else {
          <ap-tabla-de-admisiones [filas]="filasPaginadas()" [total]="encontradas().length" />
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
    .pantalla { display: flex; flex-direction: column; gap: var(--s4); }
    .tarjeta-directa { padding: var(--s4); background: var(--surface); border: var(--borde-fino) solid var(--border); border-radius: var(--r-lg); display: flex; flex-direction: column; gap: var(--s3); box-shadow: var(--sombra-1); }
    .titulo-directo { margin: 0; font-size: 1.1rem; font-weight: 600; color: var(--text); }
    .descripcion-directa { margin: var(--s1) 0 0; font-size: 0.875rem; color: var(--text-2); }
    .formulario-directo { display: flex; align-items: flex-start; gap: var(--s3); flex-wrap: wrap; }
    .campo-directo { flex: 1 1 20rem; min-width: min(20rem, 100%); }
    .accion-directa { padding-top: 1.625rem; }
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
    return { q: q?.get('q') ?? '', estado: q?.get('estado') ?? '' }
  })

  protected readonly paginaActual = computed(() => Number(this.consulta()?.get('pagina') ?? 1) || 1)
  protected readonly porPagina = computed(() => {
    const n = Number(this.consulta()?.get('filas') ?? 20)
    return FILAS_POR_PAGINA.includes(n) ? n : 20
  })

  protected readonly recurso = solicitudesDeAdmision()
  protected readonly esVacio = (items: SolicitudDeAdmision[]) => !items || items.length === 0

  protected readonly encontradas = computed<SolicitudDeAdmision[]>(() => {
    const items = this.recurso.hasValue() ? (this.recurso.value() ?? []) : []
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
      nombre: 'estado', etiqueta: this.t.filtrarEstado, valor: this.filtros().estado, textoTodos: this.t.estadoTodos,
      opciones: [
        { valor: 'PENDIENTE', texto: this.t.estadoPendiente }, { valor: 'ACEPTADA', texto: this.t.estadoAceptada },
        { valor: 'RECHAZADA', texto: this.t.estadoRechazada }, { valor: 'ESCALADA', texto: this.t.estadoEscalada },
      ],
    },
  ])

  protected filtrar(clave: string, valor: string): void {
    const cambios: Record<string, string | null> = { [clave]: valor || null }
    if (clave !== 'pagina') cambios['pagina'] = null
    void this.router.navigate([], { relativeTo: this.ruta, queryParams: cambios, queryParamsHandling: 'merge' })
  }

  protected limpiarTodo(): void {
    void this.router.navigate([], { relativeTo: this.ruta, queryParams: { q: null, estado: null, pagina: null }, queryParamsHandling: 'merge' })
  }

  protected abrir(): void {
    const valor = this.id().trim()
    if (!valor || !UUID.test(valor)) {
      this.error.set(this.t.buscarError)
      return
    }
    this.error.set('')
    void this.router.navigate(['/admisiones', valor])
  }
}
