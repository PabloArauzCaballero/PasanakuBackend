import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core'
import { RouterLink } from '@angular/router'
import { CabeceraDeSeccion } from '@aportaya/ui/cabecera-de-seccion/cabecera-de-seccion'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { EstadoVacio } from '@aportaya/ui/estado-vacio/estado-vacio'
import { Paginacion } from '@aportaya/ui/paginacion/paginacion'
import { TablaDeDatos, type Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import type { Tono } from '@aportaya/ui/tono/tono'
import type { TablaDelCatalogo } from 'clientes/angular/auditoria'
import { ServicioOperaciones } from '../../sistemas/operaciones/dominio/servicio-operaciones'
import { textosCatalogo } from './textos'

type FilaOperacionTabla = {
  id: string
  metodo: string
  ruta: string
  operationId: string
  servicio: string
  operacionDb: string
  impacto: string
}

/**
 * Pestaña «Operaciones» de la ficha, con las dos fuentes que existen y que no se infieren una de la otra:
 *
 * 1. **Operaciones directas** (catálogo de operaciones de `sistemas/`): qué endpoint lee o escribe la tabla y qué
 *    hace sobre ella, con el enlace a su ficha en `/sistemas/operaciones/:id`.
 * 2. **Casos de uso que la nombran** (la pestaña de endpoints del portal de Atlas): el `[[tabla]]` de cada
 *    `docs/CasosDeUso/CU-*.md` encadenado con la matriz de endpoints. Viene en la ficha (`operaciones`) desde
 *    `auditoria`; la pantalla dice que el caso de uso la involucra, no que cada endpoint la escriba.
 */
@Component({
  selector: 'ap-vista-operaciones',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, CabeceraDeSeccion, ChipEstado, EstadoVacio, Paginacion, TablaDeDatos],
  template: `
    <section class="directas" [attr.aria-label]="t.directas">
      <ap-cabecera-de-seccion [titulo]="t.directas" [descripcion]="t.directasDescripcion" />
      @if (filas().length === 0) {
        <ap-estado-vacio motivo="sinDatos" [titulo]="t.sinDirectas" [explicacion]="casos().length ? t.sinDirectasExplicacion : t.sinDirectasSinCasos" />
      } @else {
        <ap-tabla-de-datos
          [titulo]="t.directas + ' (' + filas().length + ')'"
          [columnas]="columnas"
          [filas]="filasDePagina()"
          [identidad]="porId"
          densidad="compacta"
        >
          <ng-template #celda let-op let-columna="columna">
            @switch (columna.clave) {
              @case ('metodo') {
                <ap-chip-estado [tono]="tonoMetodo(op.metodo)">{{ op.metodo }}</ap-chip-estado>
              }
              @case ('ruta') {
                <div class="col-ruta">
                  <a [routerLink]="['/sistemas/operaciones', op.id]" class="enlace-op">
                    <code>@for (parte of partes(op.ruta); track $index) {<wbr />/{{ parte }}}</code>
                  </a>
                  <span class="sub-op">{{ op.operationId }}</span>
                </div>
              }
              @case ('servicio') {
                <ap-chip-estado tono="neutro">{{ op.servicio }}</ap-chip-estado>
              }
              @case ('operacionDb') {
                <ap-chip-estado [tono]="op.operacionDb === 'SELECT' ? 'info' : 'aviso'">
                  {{ op.operacionDb }}
                </ap-chip-estado>
              }
              @case ('impacto') {
                <span class="texto-impacto">{{ op.impacto }}</span>
              }
              @default {
                {{ op[columna.clave] }}
              }
            }
          </ng-template>
        </ap-tabla-de-datos>
        <ap-paginacion
          etiqueta="Paginación de operaciones directas"
          [(pagina)]="pagina"
          [totalDeFilas]="filas().length"
          [porPagina]="POR_PAGINA"
          [nombreDeRegistros]="t.registros"
        />
      }
    </section>

    <section class="casos-de-uso">
      <ap-cabecera-de-seccion [titulo]="t.casos" [descripcion]="t.casosDescripcion" />
      @if (casos().length) {
        <ul class="casos" [attr.aria-label]="t.casos">
          @for (o of casos(); track o.casoDeUso) {
            <li class="caso">
              <h3><span class="codigo">{{ o.casoDeUso }}</span> {{ o.titulo }}</h3>
              @if (o.endpoints.length) {
                <ul class="endpoints" [attr.aria-label]="t.endpointsDe(o.casoDeUso)">
                  @for (e of o.endpoints; track e.operationId) {
                    <li>
                      <code class="metodo">{{ e.metodo }}</code>
                      <span class="destino">
                        <code class="ruta">@for (parte of partes(e.ruta); track $index) {<wbr />/{{ parte }}}</code>
                        <span class="detalle">
                          @if (fichaDe(e.operationId); as id) {
                            <a [routerLink]="['/sistemas/operaciones', id]" class="enlace-op">{{ e.operationId }}</a>
                          } @else {
                            {{ e.operationId }}
                          }
                          · {{ e.servicio }}
                        </span>
                      </span>
                    </li>
                  }
                </ul>
              } @else {
                <p class="tenue">{{ t.sinEndpoint }}</p>
              }
            </li>
          }
        </ul>
      } @else {
        <ap-estado-vacio motivo="sinDatos" [titulo]="t.vacio" [explicacion]="t.vacioExplicacion" />
      }
    </section>
  `,
  styleUrl: './vista-operaciones.css',
})
export class VistaOperaciones {
  readonly tabla = input.required<TablaDelCatalogo>()
  protected readonly t = textosCatalogo.ficha.operaciones
  private readonly servicioOps = inject(ServicioOperaciones)

  protected readonly porId = (f: FilaOperacionTabla) => f.id

  /** Un inventario puede nombrar cientos de operaciones para una tabla: se muestran por páginas, no todas de golpe. */
  protected readonly POR_PAGINA = 10
  protected readonly pagina = signal(1)

  /** Las directas: del catálogo de operaciones, por tabla. */
  protected readonly filas = computed<FilaOperacionTabla[]>(() => {
    const ops = this.servicioOps.buscarPorTabla(this.tabla().tabla)
    const tNombre = this.tabla().tabla.toLowerCase()
    return ops.map((op) => {
      const imp = op.tablasImpactadas.find((t) => t.tabla.toLowerCase() === tNombre)
      return {
        id: op.id,
        metodo: op.metodo,
        ruta: op.ruta,
        operationId: op.operationId,
        servicio: op.servicio,
        operacionDb: imp?.operacion ?? 'USO',
        impacto: imp?.impacto ?? op.proposito,
      }
    })
  })

  protected readonly filasDePagina = computed(() => {
    const inicio = (this.pagina() - 1) * this.POR_PAGINA
    return this.filas().slice(inicio, inicio + this.POR_PAGINA)
  })

  /** `/a/b/{c}` → `['a', 'b', '{c}']`: cada tramo lleva un corte posible antes de su «/», nunca dentro de una palabra o de unas llaves. */
  protected partes(ruta: string): string[] {
    return ruta.split('/').filter((p) => p.length > 0)
  }

  /** La ficha de la operación en `sistemas/operaciones`, si el catálogo la conoce; si no, el id se muestra sin enlace. */
  protected fichaDe(operationId: string): string | null {
    return this.servicioOps.buscarPorId(operationId)?.id ?? null
  }

  /** Los casos de uso: vienen en la ficha; una tabla vieja en caché puede no traerlos. */
  protected readonly casos = computed(() => this.tabla().operaciones ?? [])

  protected readonly columnas: Columna<FilaOperacionTabla>[] = [
    { clave: 'metodo', titulo: 'Método' },
    { clave: 'ruta', titulo: 'Ruta / Operación', mono: true },  // la ruta es el enlace a la ficha: una sola vía por fila
    { clave: 'servicio', titulo: 'Servicio' },
    { clave: 'operacionDb', titulo: 'Operación DB' },
    { clave: 'impacto', titulo: 'Impacto / Uso' },
  ]

  protected tonoMetodo(m: string): Tono {
    switch (m) {
      case 'GET': return 'info'
      case 'POST': return 'ok'
      case 'PUT': case 'PATCH': return 'aviso'
      case 'DELETE': return 'error'
      default: return 'neutro'
    }
  }
}
