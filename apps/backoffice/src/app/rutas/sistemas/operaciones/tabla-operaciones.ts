import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core'
import { RouterLink } from '@angular/router'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { TablaDeDatos, type Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import type { Tono } from '@aportaya/ui/tono/tono'
import type { OperacionDelSistema } from './dominio/tipos'

@Component({
  selector: 'ap-tabla-operaciones',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, ChipEstado, TablaDeDatos],
  template: `
    <ap-tabla-de-datos
      [titulo]="'Operaciones registradas (' + operaciones().length + ')'"
      [columnas]="columnas"
      [filas]="filas()"
      [identidad]="porId"
      densidad="compacta"
      [cabecera]="true"
      [altoMaximo]="true"
    >
      <ng-template #celda let-op let-columna="columna">
        @switch (columna.clave) {
          @case ('metodo') {
            <ap-chip-estado [tono]="tonoMetodo(op.metodo)">{{ op.metodo }}</ap-chip-estado>
          }
          @case ('ruta') {
            <div class="col-ruta">
              <a [routerLink]="['/sistemas/operaciones', op.id]" class="enlace-mono">
                <code>{{ op.ruta }}</code>
              </a>
              <span class="subtexto">{{ op.operationId }}</span>
            </div>
          }
          @case ('servicio') {
            <div class="servicio-tag">
              <ap-chip-estado tono="neutro">{{ op.servicio }}</ap-chip-estado>
              <small class="modulo">{{ op.modulo }}</small>
            </div>
          }
          @case ('proposito') {
            <div class="proposito">{{ op.proposito }}</div>
          }
          @case ('tablas') {
            <div class="chips-tablas">
              @for (t of op.tablasImpactadas.slice(0, 2); track t.tabla) {
                <span class="chip-tabla" [class.mutacion]="t.operacion !== 'SELECT'">{{ t.tabla }}</span>
              }
              @if (op.tablasImpactadas.length > 2) {
                <span class="chip-mas">+{{ op.tablasImpactadas.length - 2 }}</span>
              }
            </div>
          }
          @case ('flujo') {
            <span class="flujo-badge">{{ op.flujo.nombre }}</span>
          }
          @case ('riesgo') {
            <ap-chip-estado [tono]="tonoRiesgo(op.riesgo)">{{ op.riesgo }}</ap-chip-estado>
          }
          @case ('acciones') {
            <a [routerLink]="['/sistemas/operaciones', op.id]" class="boton-ver">Ver ficha</a>
          }
          @default {
            {{ op[columna.clave] }}
          }
        }
      </ng-template>
    </ap-tabla-de-datos>
  `,
  styles: `
    :host { display: block; width: 100%; min-width: 0; }
    .col-ruta { display: flex; flex-direction: column; gap: var(--s1); min-width: 14rem; }
    .enlace-mono { font-family: var(--f-mono, monospace); font-size: 0.875rem; color: var(--brand-texto); text-decoration: none; font-weight: 600; }
    .enlace-mono:hover { text-decoration: underline; }
    .subtexto { font-size: 0.75rem; color: var(--text-apagado); }
    .servicio-tag { display: flex; flex-direction: column; gap: 0.25rem; }
    .modulo { font-size: 0.7rem; color: var(--text-apagado); }
    .proposito { font-size: 0.8125rem; line-height: 1.35; max-width: 20rem; color: var(--text); }
    .chips-tablas { display: flex; flex-wrap: wrap; gap: 0.25rem; align-items: center; }
    .chip-tabla { font-family: var(--f-mono, monospace); font-size: 0.7rem; padding: 0.15rem 0.35rem; border-radius: var(--r-sm); background: var(--fondo-panel); border: var(--borde-fino) solid var(--borde-tabla, var(--border)); color: var(--text); }
    .chip-tabla.mutacion { border-color: var(--brand-borde); color: var(--brand-texto); }
    .chip-mas { font-size: 0.7rem; color: var(--text-apagado); font-weight: bold; }
    .flujo-badge { font-size: 0.75rem; color: var(--text-suave); max-width: 12rem; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
    .boton-ver { display: inline-flex; align-items: center; font-size: 0.8125rem; font-weight: 600; color: var(--brand-texto); text-decoration: none; padding: var(--s1) var(--s2); border-radius: var(--r-sm); background: var(--fondo-control); border: var(--borde-fino) solid var(--borde-control); }
    .boton-ver:hover { background: var(--fondo-control-hover); }
  `,
})
export class TablaOperaciones {
  readonly operaciones = input.required<readonly OperacionDelSistema[]>()
  protected readonly filas = computed(() => [...this.operaciones()])

  protected readonly porId = (op: OperacionDelSistema) => op.id

  protected readonly columnas: Columna<OperacionDelSistema & { tablas?: string; acciones?: string }>[] = [
    { clave: 'metodo', titulo: 'Método' },
    { clave: 'ruta', titulo: 'Ruta / Operación', mono: true },
    { clave: 'servicio', titulo: 'Servicio' },
    { clave: 'proposito', titulo: 'Propósito' },
    { clave: 'tablas', titulo: 'Tablas afectadas' },
    { clave: 'flujo', titulo: 'Flujo' },
    { clave: 'riesgo', titulo: 'Riesgo' },
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

  protected tonoRiesgo(r: string): Tono {
    switch (r) {
      case 'CRITICO': return 'error'
      case 'ALTO': return 'aviso'
      case 'MEDIO': return 'info'
      default: return 'neutro'
    }
  }
}
