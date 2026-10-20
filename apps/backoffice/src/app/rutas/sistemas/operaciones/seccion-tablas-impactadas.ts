import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core'
import { RouterLink } from '@angular/router'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { TablaDeDatos, type Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import type { TablaImpactada } from './dominio/tipos'

@Component({
  selector: 'ap-seccion-tablas-impactadas',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, ChipEstado, TablaDeDatos],
  template: `
    <section class="seccion-tablas">
      <div class="intro">
        <h3 class="titulo-sec">Tablas del modelo afectadas por esta operación</h3>
        <p class="desc-sec">Detalle de persistencia: qué tablas consulta o muta este endpoint, con qué tipo de operación y qué impacto genera en el estado transaccional.</p>
      </div>

      <ap-tabla-de-datos
        [titulo]="'Tablas impactadas (' + tablas().length + ')'"
        [columnas]="columnas"
        [filas]="filas()"
        [identidad]="porTabla"
        densidad="compacta"
      >
        <ng-template #celda let-t let-columna="columna">
          @switch (columna.clave) {
            @case ('tabla') {
              <div class="col-entidad">
                <a [routerLink]="['/tablero/catalogo-de-datos', t.tabla]" class="link-tabla">
                  <code>{{ t.esquema }}.{{ t.tabla }}</code>
                </a>
                <span class="tipo-rol">{{ t.principal ? 'Entidad principal de dominio' : 'Registro de auditoría / outbox' }}</span>
              </div>
            }
            @case ('operacion') {
              <ap-chip-estado [tono]="t.operacion === 'SELECT' ? 'info' : 'aviso'">
                {{ t.operacion }}
              </ap-chip-estado>
            }
            @case ('impacto') {
              <span class="texto-impacto">{{ t.impacto }}</span>
            }
            @case ('acciones') {
              <a [routerLink]="['/tablero/catalogo-de-datos', t.tabla]" class="btn-ir">
                Ver en catálogo
              </a>
            }
            @default {
              {{ t[columna.clave] }}
            }
          }
        </ng-template>
      </ap-tabla-de-datos>
    </section>
  `,
  styles: `
    :host { display: block; width: 100%; }
    .seccion-tablas { display: flex; flex-direction: column; gap: var(--s3); }
    .intro { display: flex; flex-direction: column; gap: 0.25rem; }
    .titulo-sec { margin: 0; font-size: 1.05rem; font-weight: 600; color: var(--text); }
    .desc-sec { margin: 0; font-size: 0.8125rem; color: var(--text-suave); }
    .col-entidad { display: flex; flex-direction: column; gap: 0.15rem; }
    .link-tabla { font-family: var(--f-mono, monospace); font-size: 0.875rem; color: var(--brand-texto); text-decoration: none; font-weight: 600; }
    .link-tabla:hover { text-decoration: underline; }
    .tipo-rol { font-size: 0.7rem; color: var(--text-apagado); }
    .texto-impacto { font-size: 0.8125rem; line-height: 1.4; color: var(--text); }
    .btn-ir { font-size: 0.75rem; font-weight: 600; color: var(--brand-texto); text-decoration: none; padding: var(--s1) var(--s2); border-radius: var(--r-sm); border: var(--borde-fino) solid var(--borde-control); background: var(--fondo-control); }
    .btn-ir:hover { background: var(--fondo-control-hover); }
  `,
})
export class SeccionTablasImpactadas {
  readonly tablas = input.required<readonly TablaImpactada[]>()
  protected readonly filas = computed(() => [...this.tablas()])

  protected readonly porTabla = (t: TablaImpactada) => t.tabla

  protected readonly columnas: Columna<TablaImpactada & { acciones?: string }>[] = [
    { clave: 'tabla', titulo: 'Tabla / Esquema', mono: true },
    { clave: 'operacion', titulo: 'Operación DB' },
    { clave: 'impacto', titulo: 'Impacto en el negocio' },
    { clave: 'acciones', titulo: 'Acciones', fijaDerecha: true },
  ]
}
