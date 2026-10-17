import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core'
import { toSignal } from '@angular/core/rxjs-interop'
import { ActivatedRoute } from '@angular/router'
import { BandaDeProposito } from '@aportaya/ui/banda-de-proposito/banda-de-proposito'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { Paginacion } from '@aportaya/ui/paginacion/paginacion'
import { TablaDeDatos, type Columna, type EstadoColeccion, type Orden } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import { BarraDeFiltros } from '../../../nucleo/filtros/barra-de-filtros'
import { Exportador } from '../../../nucleo/exportador'
import { filasConSalud, horaDeConsulta, NIVELES, recursoSaludDeServicios, type FilaDeEstado, type SinSalud } from './dominio/estado-del-sistema'

// La salud va pegada al servicio: es lo que se viene a mirar, y la columna larga («por qué») va al final y se parte.
const COLUMNAS: Columna<FilaDeEstado>[] = [
  { clave: 'servicio', titulo: 'Servicio', ordenable: true },
  { clave: 'salud', titulo: 'Salud ahora', ordenable: true },
  { clave: 'latencia', titulo: 'Tardó', numerica: true },
  { clave: 'nivel', titulo: 'Nivel', ordenable: true },
  { clave: 'replicas', titulo: 'Réplicas exigidas (producción)', ordenable: true, numerica: true },
  { clave: 'porQue', titulo: 'Por qué ese nivel' },
]
const TAMANO_DE_PAGINA = 20

/**
 * `operacion/estado` de la maqueta («Arquitectura y estado del proyecto»): el nivel de
 * criticidad de cada servicio (ADR-037) y cuántas réplicas exige. Migrada al organismo
 * canónico `@aportaya/ui/tabla-de-datos` (PR7 §H4.S1.M1) — antes usaba
 * `TablaDeDatosVirtualizada`, pero `SERVICIOS` es una constante local (no hay red de por
 * medio, ver JSDoc de `dominio/estado-del-sistema.ts`), así que pagina y ordena con un
 * `computed` síncrono en vez de con el `effect`+`cargador` asíncrono que esa pieza exige.
 *
 * **Corrección de un defecto preexistente, encontrado al migrar:** la versión anterior no
 * pasaba `[filtros]` a `ap-tabla-de-datos-virtualizada`, así que el filtro por nivel de la
 * URL nunca llegaba al cargador — el tercer caso de `pantalla-de-estado.spec.ts`
 * ("pegar la URL con un filtro ya aplicado") pasaba solo porque nunca leyó el DOM filtrado
 * de verdad. Acá el filtro se lee de la misma `queryParamMap` que ya usa `BarraDeFiltros`
 * y sí llega a los datos mostrados; además, `paginaAcotada` evita una página vacía cuando
 * un filtro reduce el total por debajo de la página en la que el operador estaba parado.
 */
@Component({
  selector: 'ap-pantalla-de-estado',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BandaDeProposito, BarraDeFiltros, TablaDeDatos, ChipEstado, Paginacion],
  template: `
    <ap-banda-de-proposito
      texto="Qué hay debajo y en qué estado está de verdad: cada servicio contesta ahora si está arriba, y al lado su nivel de criticidad y las réplicas que exige en producción."
    />
    <section class="pantalla">
      <div class="cabecera">
        <h1>Arquitectura y estado del proyecto</h1>
        @if (exportador.puedeExportar('estado-plataforma')) {
          <button type="button" (click)="exportar()">Exportar</button>
        }
      </div>
      <div class="consulta" role="status" aria-live="polite">
        <span>{{ leyenda() }}</span>
        @if (!salud.isLoading()) {
          <button type="button" class="secundario" (click)="salud.reload()">Volver a consultar</button>
        }
      </div>
      <ap-barra-de-filtros etiqueta="Nivel" [incluirBusqueda]="false" [definiciones]="filtrosDeNivel" />
      <ap-tabla-de-datos
        titulo="Servicios por nivel de criticidad"
        [columnas]="columnas"
        [estado]="estado()"
        [(orden)]="orden"
        textoVacio="Ningún servicio coincide con el filtro elegido."
      >
        <ng-template #celda let-fila let-columna="columna">
          @if (columna.clave === 'salud') {
            <ap-chip-estado [tono]="fila.tono">{{ fila.salud }}</ap-chip-estado>
          } @else if (columna.clave === 'nivel') {
            <ap-chip-estado [tono]="fila.nivel === 'N1' ? 'error' : fila.nivel === 'N2' ? 'aviso' : 'info'">{{ fila.nivel }}</ap-chip-estado>
          } @else if (columna.clave === 'porQue') {
            <span class="largo">{{ fila.porQue }}</span>
          } @else {
            {{ fila[columna.clave] }}
          }
        </ng-template>
      </ap-tabla-de-datos>
      <ap-paginacion [pagina]="paginaAcotada()" (paginaChange)="pagina.set($event)" [totalDeFilas]="total()" [porPagina]="tamanoDePagina" />
    </section>
  `,
  styles: `
    .pantalla { padding: var(--s5); display: flex; flex-direction: column; gap: var(--s4); }
    .cabecera { display: flex; align-items: center; justify-content: space-between; gap: var(--s3); }
    h1 { margin: 0; }
    button { min-height: var(--area-tactil); padding: 0 var(--s5); border: 0; border-radius: var(--r-md); background: var(--verde-solido); color: var(--sobre-verde-solido); font: inherit; font-weight: 600; cursor: pointer; }
    .pantalla > * { min-width: 0; }
    .largo { display: block; white-space: normal; overflow-wrap: anywhere; }
    /* Desde tablet la tabla es tabla (en móvil, tarjetas): sin ancho mínimo el «por qué» se parte en filas altas y vacías. */
    @media (min-width: 40rem) { .largo { min-width: 16rem; } }
    .consulta { display: flex; flex-wrap: wrap; align-items: center; gap: var(--s3); color: var(--text-2); }
    .secundario { background: transparent; color: var(--text); border: var(--borde-fino) solid var(--border); }
    button:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
  `,
})
export class PantallaDeEstado {
  protected readonly exportador = inject(Exportador)
  private readonly ruta = inject(ActivatedRoute)
  protected readonly columnas = COLUMNAS
  protected readonly tamanoDePagina = TAMANO_DE_PAGINA
  protected readonly filtrosDeNivel = NIVELES.map((n) => ({ valor: n.valor, texto: n.texto }))

  private readonly queryParamMap = toSignal(this.ruta.queryParamMap, { requireSync: false })
  private readonly nivelesElegidos = computed(() => this.queryParamMap()?.get('filtro')?.split(',').filter(Boolean) ?? [])

  protected readonly pagina = signal(1)
  protected readonly orden = signal<Orden | null>(null)

  protected readonly salud = recursoSaludDeServicios()
  /** Por qué no hay salud que mostrar todavía; `undefined` cuando llegó la respuesta. */
  private readonly sinSalud = computed<SinSalud | undefined>(() => {
    if (this.salud.hasValue()) return undefined
    const e = this.salud.error() as { estado?: number; status?: number } | undefined
    // En la app el interceptor entrega un `ErrorTraducido` (`estado`); sin interceptor llega el `HttpErrorResponse` (`status`).
    if (e) return (e.estado ?? e.status) === 403 ? 'sinPermiso' : 'error'
    return 'consultando'
  })
  protected readonly leyenda = computed(() => {
    const motivo = this.sinSalud()
    if (motivo === 'consultando') return 'Preguntando a cada servicio si está arriba…'
    if (motivo === 'sinPermiso') return 'Tu rol no puede ver la salud de los servicios (hace falta el tablero de sistemas). El resto de la tabla es la arquitectura.'
    if (motivo === 'error') return 'No se pudo consultar la salud: la tabla muestra la arquitectura, sin estado. Probá volver a consultar.'
    return `Salud consultada a las ${horaDeConsulta(this.salud.value()!.consultadoEn)} (hora de La Paz).`
  })

  private readonly filasFiltradasYOrdenadas = computed(() => {
    const niveles = this.nivelesElegidos()
    const todas = filasConSalud(this.salud.hasValue() ? this.salud.value() : undefined, this.sinSalud())
    let filas: FilaDeEstado[] = niveles.length ? todas.filter((s) => niveles.includes(s.nivel)) : todas
    const orden = this.orden()
    if (orden) {
      const { clave, sentido } = orden
      filas = [...filas].sort((a, b) => {
        const va = a[clave as keyof FilaDeEstado]
        const vb = b[clave as keyof FilaDeEstado]
        const cmp = va < vb ? -1 : va > vb ? 1 : 0
        return sentido === 'asc' ? cmp : -cmp
      })
    }
    return filas
  })
  protected readonly total = computed(() => this.filasFiltradasYOrdenadas().length)
  /** Si un filtro deja menos páginas de las que había, se acota a la última válida en vez de mostrar una página vacía. */
  protected readonly paginaAcotada = computed(() => Math.min(this.pagina(), Math.max(1, Math.ceil(this.total() / TAMANO_DE_PAGINA))))
  protected readonly estado = computed<EstadoColeccion<FilaDeEstado>>(() => {
    const desde = (this.paginaAcotada() - 1) * TAMANO_DE_PAGINA
    return { tipo: 'lista', filas: this.filasFiltradasYOrdenadas().slice(desde, desde + TAMANO_DE_PAGINA) }
  })

  exportar(): void {
    this.exportador.solicitar({ recurso: 'estado-plataforma', formato: 'csv', filtros: {} }).subscribe()
  }
}
