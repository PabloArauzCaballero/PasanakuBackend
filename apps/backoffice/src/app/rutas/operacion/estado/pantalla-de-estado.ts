import { ChangeDetectionStrategy, Component, computed, effect, inject, untracked } from '@angular/core'
import { ActivatedRoute, Router } from '@angular/router'
import { Boton } from '@aportaya/ui/boton/boton'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { crearListado } from '@aportaya/ui/listado/crear-listado'
import { ListadoDeDatos } from '@aportaya/ui/listado/listado'
import type { Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import { TarjetaKPI } from '@aportaya/ui/tarjeta-kpi/tarjeta-kpi'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
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

const PROPOSITO_ESTADO =
  'Qué hay debajo y en qué estado está de verdad: cada servicio contesta ahora si está arriba, y al lado su nivel de criticidad y las réplicas que exige en producción.'

/**
 * `operacion/estado` de la maqueta («Arquitectura y estado del proyecto»). La arquitectura es constante; la salud la
 * pregunta `auditoria` ahora. Es una colección chica que ya está en el navegador: va con el kit de listado (búsqueda,
 * filtros por nivel y salud, CSV/JSON de lo filtrado). La exportación con huella del servidor sigue aparte.
 */
@Component({
  selector: 'ap-pantalla-de-estado',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Boton, CabeceraDePagina, ChipEstado, ListadoDeDatos, TarjetaKPI],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina class="cabecera" eyebrow="Operación · arquitectura" titulo="Arquitectura y estado del proyecto" [descripcion]="proposito">
        <ap-boton acciones variante="fantasma" icono="recarga" [cargando]="salud.isLoading()" (pulsado)="salud.reload()">Volver a consultar</ap-boton>
        @if (exportador.puedeExportar('estado-plataforma')) {
          <ap-boton acciones variante="primario" icono="descargar" (pulsado)="exportar()">Exportar con huella</ap-boton>
        }
      </ap-cabecera-de-pagina>
      <div class="cifras">
        <ap-tarjeta-kpi etiqueta="Servicios" [valor]="'' + filas().length" icono="servidor" [pista]="sinDato()" />
        <ap-tarjeta-kpi etiqueta="Arriba" [valor]="cuenta('ok')" icono="verificado" tono="ok" [pista]="pista()" />
        <ap-tarjeta-kpi etiqueta="Con problemas" [valor]="cuenta('aviso')" icono="advertencia" tono="aviso" [pista]="pista()" />
        <ap-tarjeta-kpi etiqueta="No responden" [valor]="cuenta('error')" icono="alerta" tono="error" [pista]="pista()" />
      </div>
      <p class="consulta" role="status" aria-live="polite">{{ leyenda() }}</p>
      <ap-listado titulo="Servicios por nivel de criticidad" [columnas]="columnas" [listado]="listado" [identidad]="identidad" ayudaDeBusqueda="Busca por servicio o por el motivo del nivel." nombreDeRegistros="servicios" nombreDeExportacion="estado de los servicios" textoVacio="No hay servicios en la arquitectura.">
        <ng-template #celda let-fila let-columna="columna">
          @switch (columna.clave) {
            @case ('salud') { <ap-chip-estado [tono]="fila.tono">{{ fila.salud }}</ap-chip-estado> }
            @case ('nivel') { <ap-chip-estado [tono]="fila.nivel === 'N1' ? 'error' : fila.nivel === 'N2' ? 'aviso' : 'info'">{{ fila.nivel }}</ap-chip-estado> }
            @case ('porQue') { <span class="largo">{{ fila.porQue }}</span> }
            @default { {{ fila[columna.clave] }} }
          }
        </ng-template>
      </ap-listado>
    </section>
  `,
  styles: `
    .pantalla { display: flex; flex-direction: column; gap: var(--s5); }
    .pantalla > * { min-width: 0; }
    /* ap-listado no acota su columna (pedido a H0 en franjas/b.md): sin esto la tabla empuja la página a 768 px. */
    ap-listado { grid-template-columns: minmax(0, 1fr); }
    .cifras { display: grid; gap: var(--s3); grid-template-columns: repeat(auto-fit, minmax(11rem, 1fr)); }
    .largo { display: block; white-space: normal; overflow-wrap: anywhere; }
    /* Desde tablet la tabla es tabla (en móvil, tarjetas): sin ancho mínimo el «por qué» se parte en filas altas y vacías. */
    @media (min-width: 40rem) { .largo { min-width: 16rem; } }
    .consulta { margin: 0; color: var(--text-2); font: var(--t-cuerpo-chico); }
  `,
})
export class PantallaDeEstado {
  protected readonly proposito = PROPOSITO_ESTADO
  constructor() {
    inject(ExplicadorDeVista).fijar(PROPOSITO_ESTADO)
    // El nivel vive en la URL (`?filtro=N1`): un enlace pegado o una recarga muestran la misma tabla filtrada.
    const inicial = this.ruta.snapshot.queryParamMap.get('filtro')?.split(',')[0] ?? ''
    if (NIVELES.some((n) => n.valor === inicial)) this.listado.filtrar('nivel', inicial)
    effect(() => {
      const nivel = this.listado.selectores().find((s) => s.nombre === 'nivel')?.valor ?? ''
      untracked(() => {
        if ((this.ruta.snapshot.queryParamMap.get('filtro') ?? '') === nivel) return
        void this.router.navigate([], { relativeTo: this.ruta, queryParams: { filtro: nivel || null }, queryParamsHandling: 'merge', replaceUrl: true })
      })
    })
  }
  private readonly ruta = inject(ActivatedRoute)
  private readonly router = inject(Router)
  protected readonly exportador = inject(Exportador)
  protected readonly columnas = COLUMNAS
  protected readonly identidad = (f: FilaDeEstado) => f.servicio

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
  /** Sin respuesta de salud, las tarjetas no dicen «0 caídos»: dicen que no hay dato. */
  protected readonly pista = computed(() => (this.sinSalud() ? 'Sin dato de salud' : 'Según la última consulta'))

  protected readonly filas = computed(() => filasConSalud(this.salud.hasValue() ? this.salud.value() : undefined, this.sinSalud()))
  protected readonly listado = crearListado<FilaDeEstado>({
    filas: this.filas,
    camposDeBusqueda: ['servicio', 'porQue', 'salud'],
    filtros: [
      { nombre: 'nivel', etiqueta: 'Nivel', valor: (f) => f.nivel, opciones: NIVELES.map((n) => ({ valor: n.valor, texto: n.texto })) },
      { nombre: 'salud', etiqueta: 'Salud', valor: (f) => f.salud },
    ],
    // 14 servicios caben en una página; la primera opción es la elegida (ver «Pedidos» en franjas/b.md).
    porPagina: 20,
    opcionesPorPagina: [20, 50, 100],
  })

  /** Los servicios de la arquitectura que la consulta no nombró: se dicen, no se cuentan como «arriba» ni «caídos». */
  protected readonly sinDato = computed(() => {
    const n = this.filas().filter((f) => f.tono === 'neutro').length
    return n === 0 ? 'Todos con dato de salud' : `${n} sin dato de salud`
  })

  protected cuenta(tono: 'ok' | 'aviso' | 'error'): string {
    return this.sinSalud() ? '—' : String(this.filas().filter((f) => f.tono === tono).length)
  }

  exportar(): void {
    this.exportador.solicitar({ recurso: 'estado-plataforma', formato: 'csv', filtros: {} }).subscribe()
  }
}
