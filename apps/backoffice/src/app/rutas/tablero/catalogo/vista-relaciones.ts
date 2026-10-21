import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core'
import { RouterLink } from '@angular/router'
import { CabeceraDeSeccion } from '@aportaya/ui/cabecera-de-seccion/cabecera-de-seccion'
import { EstadoVacio } from '@aportaya/ui/estado-vacio/estado-vacio'
import { TablaDeDatos, type Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import type { TablaDelCatalogo } from 'clientes/angular/auditoria'
import { filasDeRelaciones, type FilaDeRelacion } from './dominio'
import { textosCatalogo } from './textos'

/**
 * Pestaña «Relaciones»: a qué apunta la tabla y quién la apunta. Cada relación dice su cardinalidad, si es
 * obligatoria, qué hace la base al borrar la fila referida y el nombre de la restricción, tal como la aplica el DDL
 * (el portal de Atlas no lo tiene: acá el modelo lo sabe). Cada una enlaza a la ficha del otro extremo.
 */
@Component({
  selector: 'ap-vista-relaciones',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CabeceraDeSeccion, EstadoVacio, RouterLink, TablaDeDatos],
  template: `
    <section class="relaciones">
      @for (bloque of bloques(); track bloque.titulo) {
        <div class="bloque">
          <ap-cabecera-de-seccion [titulo]="bloque.titulo" [descripcion]="bloque.descripcion" />
          @if (bloque.filas.length) {
            <ap-tabla-de-datos [titulo]="bloque.titulo" [columnas]="columnas" [filas]="bloque.filas" [identidad]="porId" densidad="compacta" [cabecera]="true" [textoRegistros]="t.relaciones.registros" [totalDeRegistros]="bloque.filas.length">
              <ng-template #celda let-r let-columna="columna">
                @switch (columna.clave) {
                  @case ('tabla') { <a [routerLink]="['/tablero/catalogo-de-datos', r.esquema, r.tabla]">{{ r.esquema }}.{{ r.tabla }}</a> }
                  @case ('columna') { <code>{{ r.columna }}</code> }
                  @case ('restriccion') { <code class="tenue">{{ r.restriccion }}</code> }
                  @default { {{ r[columna.clave] }} }
                }
              </ng-template>
            </ap-tabla-de-datos>
          } @else {
            <ap-estado-vacio [titulo]="t.relaciones.sinRelaciones" [explicacion]="bloque.vacio" />
          }
        </div>
      }
    </section>
  `,
  styles: `
    .relaciones { display: grid; gap: var(--s5); grid-template-columns: minmax(0, 1fr); }
    .bloque { display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s3); min-width: 0; }
    a { color: var(--text); font-family: var(--mono); font-size: .92em; font-weight: 600; overflow-wrap: anywhere; }
    a:hover { color: var(--brand-texto); }
    a:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
    code { font-family: var(--mono); font-size: .92em; overflow-wrap: anywhere; }
    .tenue { color: var(--text-2); }
  `,
})
export class VistaRelaciones {
  readonly tabla = input.required<TablaDelCatalogo>()
  protected readonly t = textosCatalogo.ficha
  protected readonly porId = (r: FilaDeRelacion) => r.id
  protected readonly columnas: Columna<FilaDeRelacion>[] = [
    { clave: 'columna', titulo: textosCatalogo.ficha.relaciones.columna },
    { clave: 'tabla', titulo: textosCatalogo.ficha.relaciones.tabla },
    { clave: 'cardinalidad', titulo: textosCatalogo.ficha.relaciones.cardinalidad },
    { clave: 'obligatoria', titulo: textosCatalogo.ficha.relaciones.obligatoria },
    { clave: 'alBorrar', titulo: textosCatalogo.ficha.relaciones.alBorrar },
    { clave: 'restriccion', titulo: textosCatalogo.ficha.relaciones.restriccion },
  ]
  protected readonly bloques = computed(() => {
    const r = this.tabla().relaciones
    const t = this.t.relaciones
    return [
      { titulo: t.salientes, descripcion: t.salientesDescripcion, vacio: t.sinSalientes, filas: filasDeRelaciones(r.salientes) },
      { titulo: t.entrantes, descripcion: t.entrantesDescripcion, vacio: t.sinEntrantes, filas: filasDeRelaciones(r.entrantes) },
    ]
  })
}
