import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core'
import { toSignal } from '@angular/core/rxjs-interop'
import { ActivatedRoute } from '@angular/router'
import { BandaDeProposito } from '@aportaya/ui/banda-de-proposito/banda-de-proposito'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { Paginacion } from '@aportaya/ui/paginacion/paginacion'
import { TablaDeDatos, type Columna, type EstadoColeccion, type Orden } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import { BarraDeFiltros } from '../../../nucleo/filtros/barra-de-filtros'
import { Exportador } from '../../../nucleo/exportador'
import { NIVELES, SERVICIOS, type ServicioDeArquitectura } from './dominio/estado-del-sistema'

const COLUMNAS: Columna<ServicioDeArquitectura>[] = [
  { clave: 'servicio', titulo: 'Servicio', ordenable: true },
  { clave: 'nivel', titulo: 'Nivel', ordenable: true },
  { clave: 'replicas', titulo: 'Réplicas', ordenable: true, numerica: true },
  { clave: 'porQue', titulo: 'Por qué ese nivel' },
  { clave: 'salud', titulo: 'Salud' },
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
      texto="Qué hay debajo y en qué estado está de verdad: el nivel de criticidad de cada servicio y cuántas réplicas exige."
    />
    <main>
      <div class="cabecera">
        <h1>Arquitectura y estado del proyecto</h1>
        @if (exportador.puedeExportar('estado-plataforma')) {
          <button type="button" (click)="exportar()">Exportar</button>
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
            <ap-chip-estado tono="ok">{{ fila.salud }}</ap-chip-estado>
          } @else if (columna.clave === 'nivel') {
            <ap-chip-estado [tono]="fila.nivel === 'N1' ? 'error' : fila.nivel === 'N2' ? 'aviso' : 'info'">{{ fila.nivel }}</ap-chip-estado>
          } @else {
            {{ fila[columna.clave] }}
          }
        </ng-template>
      </ap-tabla-de-datos>
      <ap-paginacion [pagina]="paginaAcotada()" (paginaChange)="pagina.set($event)" [totalDeFilas]="total()" [porPagina]="tamanoDePagina" />
    </main>
  `,
  styles: `
    main { padding: var(--s5); display: flex; flex-direction: column; gap: var(--s4); }
    .cabecera { display: flex; align-items: center; justify-content: space-between; gap: var(--s3); }
    h1 { margin: 0; }
    button { min-height: var(--area-tactil); padding: 0 var(--s5); border: 0; border-radius: var(--r-md); background: var(--verde-solido); color: var(--sobre-verde-solido); font: inherit; font-weight: 600; cursor: pointer; }
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

  private readonly filasFiltradasYOrdenadas = computed(() => {
    const niveles = this.nivelesElegidos()
    let filas: ServicioDeArquitectura[] = niveles.length ? SERVICIOS.filter((s) => niveles.includes(s.nivel)) : [...SERVICIOS]
    const orden = this.orden()
    if (orden) {
      const { clave, sentido } = orden
      filas = [...filas].sort((a, b) => {
        const va = a[clave as keyof ServicioDeArquitectura]
        const vb = b[clave as keyof ServicioDeArquitectura]
        const cmp = va < vb ? -1 : va > vb ? 1 : 0
        return sentido === 'asc' ? cmp : -cmp
      })
    }
    return filas
  })
  protected readonly total = computed(() => this.filasFiltradasYOrdenadas().length)
  /** Si un filtro deja menos páginas de las que había, se acota a la última válida en vez de mostrar una página vacía. */
  protected readonly paginaAcotada = computed(() => Math.min(this.pagina(), Math.max(1, Math.ceil(this.total() / TAMANO_DE_PAGINA))))
  protected readonly estado = computed<EstadoColeccion<ServicioDeArquitectura>>(() => {
    const desde = (this.paginaAcotada() - 1) * TAMANO_DE_PAGINA
    return { tipo: 'lista', filas: this.filasFiltradasYOrdenadas().slice(desde, desde + TAMANO_DE_PAGINA) }
  })

  exportar(): void {
    this.exportador.solicitar({ recurso: 'estado-plataforma', formato: 'csv', filtros: {} }).subscribe()
  }
}
