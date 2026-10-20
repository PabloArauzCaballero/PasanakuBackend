import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core'
import { RouterLink } from '@angular/router'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { EstadoVacio } from '@aportaya/ui/estado-vacio/estado-vacio'
import { TablaDeDatos, type Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import type { Tono } from '@aportaya/ui/tono/tono'
import type { TablaDelCatalogo } from 'clientes/angular/auditoria'
import { ServicioOperaciones } from '../../sistemas/operaciones/dominio/servicio-operaciones'

type FilaOperacionTabla = {
  id: string
  metodo: string
  ruta: string
  operationId: string
  servicio: string
  operacionDb: string
  impacto: string
  acciones?: string
}

@Component({
  selector: 'ap-vista-operaciones',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, ChipEstado, EstadoVacio, TablaDeDatos],
  template: `
    @if (filas().length === 0) {
      <ap-estado-vacio
        motivo="sinDatos"
        titulo="Sin operaciones directas registradas"
        mensaje="Ningún endpoint del catálogo tiene asignada una mutación o consulta directa sobre esta tabla."
      />
    } @else {
      <ap-tabla-de-datos
        [titulo]="'Operaciones que usan ' + tabla().tabla + ' (' + filas().length + ')'"
        [columnas]="columnas"
        [filas]="filas()"
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
                  <code>{{ op.ruta }}</code>
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
            @case ('acciones') {
              <a [routerLink]="['/sistemas/operaciones', op.id]" class="btn-detalle">Ver operación</a>
            }
            @default {
              {{ op[columna.clave] }}
            }
          }
        </ng-template>
      </ap-tabla-de-datos>
    }
  `,
  styles: `
    :host { display: block; width: 100%; }
    .col-ruta { display: flex; flex-direction: column; gap: 0.15rem; }
    .enlace-op { font-family: var(--f-mono, monospace); font-size: 0.8125rem; font-weight: 600; color: var(--brand-texto); text-decoration: none; }
    .enlace-op:hover { text-decoration: underline; }
    .sub-op { font-size: 0.7rem; color: var(--text-apagado); }
    .texto-impacto { font-size: 0.8125rem; line-height: 1.35; color: var(--text); }
    .btn-detalle { font-size: 0.75rem; font-weight: 600; color: var(--brand-texto); text-decoration: none; padding: var(--s1) var(--s2); border-radius: var(--r-sm); background: var(--fondo-control); border: var(--borde-fino) solid var(--borde-control); }
    .btn-detalle:hover { background: var(--fondo-control-hover); }
  `,
})
export class VistaOperaciones {
  readonly tabla = input.required<TablaDelCatalogo>()
  private readonly servicioOps = inject(ServicioOperaciones)

  protected readonly porId = (f: FilaOperacionTabla) => f.id

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

  protected readonly columnas: Columna<FilaOperacionTabla>[] = [
    { clave: 'metodo', titulo: 'Método' },
    { clave: 'ruta', titulo: 'Ruta / Operación', mono: true },
    { clave: 'servicio', titulo: 'Servicio' },
    { clave: 'operacionDb', titulo: 'Operación DB' },
    { clave: 'impacto', titulo: 'Impacto / Uso' },
    { clave: 'acciones', titulo: 'Acciones', fijaDerecha: true },
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
