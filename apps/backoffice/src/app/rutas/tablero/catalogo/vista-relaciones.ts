import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core'
import { RouterLink } from '@angular/router'
import { CabeceraDeSeccion } from '@aportaya/ui/cabecera-de-seccion/cabecera-de-seccion'
import { EstadoVacio } from '@aportaya/ui/estado-vacio/estado-vacio'
import { TablaDeDatos, type Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import type { TablaDelCatalogo } from 'clientes/angular/auditoria'
import { textosCatalogo } from './textos'

type FilaDeRelacion = { id: string; columna: string; tabla: string; esquema: string }

/** Pestaña «Relaciones»: a qué apunta la tabla y quién la apunta, cada relación con enlace a la otra ficha. */
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
            <ap-tabla-de-datos [titulo]="bloque.titulo" [columnas]="columnas" [filas]="bloque.filas" [identidad]="porId" densidad="compacta" [cabecera]="true" [textoRegistros]="t.relaciones.tabla">
              <ng-template #celda let-r let-columna="columna">
                @if (columna.clave === 'tabla') { <a [routerLink]="['/tablero/catalogo-de-datos', r.tabla]">{{ r.esquema }}.{{ r.tabla }}</a> } @else { <code>{{ r.columna }}</code> }
              </ng-template>
            </ap-tabla-de-datos>
          } @else {
            <ap-estado-vacio [titulo]="t.relaciones.sinRelaciones" explicacion="" />
          }
        </div>
      }
    </section>
  `,
  styles: `
    .relaciones { display: grid; gap: var(--s4); grid-template-columns: repeat(auto-fit, minmax(min(100%, 20rem), 1fr)); align-items: start; }
    .bloque { display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s3); min-width: 0; }
    a { color: var(--text); font-family: var(--mono); font-size: .92em; font-weight: 600; }
    a:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
    code { font-family: var(--mono); font-size: .92em; }
  `,
})
export class VistaRelaciones {
  readonly tabla = input.required<TablaDelCatalogo>()
  protected readonly t = textosCatalogo.ficha
  protected readonly porId = (r: FilaDeRelacion) => r.id
  protected readonly columnas: Columna<FilaDeRelacion>[] = [
    { clave: 'columna', titulo: textosCatalogo.ficha.relaciones.columna },
    { clave: 'tabla', titulo: textosCatalogo.ficha.relaciones.tabla },
  ]
  protected readonly bloques = computed(() => {
    const r = this.tabla().relaciones
    const fila = (x: { columna: string; tabla: string; esquema: string }, i: number): FilaDeRelacion => ({ id: `${x.esquema}.${x.tabla}.${x.columna}.${i}`, ...x })
    return [
      { titulo: this.t.relaciones.salientes, descripcion: this.t.relaciones.salientesDescripcion, filas: r.salientes.map(fila) },
      { titulo: this.t.relaciones.entrantes, descripcion: this.t.relaciones.entrantesDescripcion, filas: r.entrantes.map(fila) },
    ]
  })
}
