import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core'
import { RouterLink } from '@angular/router'
import { EstadoDePantalla } from '@aportaya/ui/estado-de-pantalla/estado-de-pantalla'
import { TablaDeDatos, type Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import { CatalogoDeDatosRemoto, filasDeColumnas, type FilaDeColumna } from './dominio'
import { SeccionDeGobierno } from './seccion-de-gobierno'
import { textosCatalogo } from './textos'

/**
 * La ficha de una tabla del catálogo: por qué existe (negocio), cómo vive (sistema), sus columnas con su sensibilidad
 * dicha en palabras y sus relaciones, que llevan a la ficha de la otra tabla. `tabla` llega del path (`withComponentInputBinding`).
 */
@Component({
  selector: 'ap-ficha-de-tabla',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [EstadoDePantalla, RouterLink, SeccionDeGobierno, TablaDeDatos],
  template: `
    <section class="pantalla">
      <a routerLink="/tablero/catalogo-de-datos" class="volver">{{ t.volver }}</a>
      <ap-estado-de-pantalla [recurso]="remoto.recurso" [mensajeVacio]="t.noExiste(tabla())" [etiquetaDeCarga]="'Consultando el catálogo'" (reintentar)="remoto.recurso.reload()">
        @if (ficha(); as f) {
          <h1>{{ f.tabla }} <span class="esquema">{{ f.esquema }}</span></h1>
          <ap-seccion-de-gobierno [tabla]="f" [politicas]="remoto.recurso.value()?.politicasDeRetencion ?? []" (cambio)="remoto.recurso.reload()" />
          <dl>
            @if (f.negocio.queEs) { <dt>{{ t.queEs }}</dt><dd>{{ f.negocio.queEs }}</dd> }
            @if (f.negocio.negocio) { <dt>{{ t.negocio }}</dt><dd>{{ f.negocio.negocio }}</dd> }
            @if (f.negocio.porQue) { <dt>{{ t.porQue }}</dt><dd>{{ f.negocio.porQue }}</dd> }
            <dt>{{ t.sistema }}</dt>
            <dd>
              {{ f.sistema }}
              @if (f.appendOnly) { <strong class="nota">{{ t.appendOnly }}</strong> }
            </dd>
            @if (f.notaDeSistema) { <dt>{{ t.notaDeSistema }}</dt><dd>{{ f.notaDeSistema }}</dd> }
            @if (f.reglas.length) { <dt>{{ t.reglas }}</dt><dd>{{ f.reglas.join(' · ') }}</dd> }
          </dl>
          <h2>{{ t.columnas }}</h2>
          <ap-tabla-de-datos [titulo]="t.columnas + ' de ' + f.tabla" [columnas]="columnas" [filas]="columnasDeLaFicha()" [identidad]="porNombre" densidad="compacta">
            <ng-template #celda let-c let-columna="columna">
              @if (columna.clave === 'negocio' || columna.clave === 'sistema') { <span class="largo">{{ c[columna.clave] }}</span> } @else { {{ c[columna.clave] }} }
            </ng-template>
          </ap-tabla-de-datos>
          <h2>{{ t.relaciones }}</h2>
          <div class="relaciones">
            <div>
              <h3>{{ t.salientes }}</h3>
              <ul>
                @for (r of f.relaciones.salientes; track r.columna) {
                  <li><code>{{ r.columna }}</code> → <a [routerLink]="['/tablero/catalogo-de-datos', r.tabla]">{{ r.esquema }}.{{ r.tabla }}</a></li>
                } @empty { <li>{{ t.sinRelaciones }}</li> }
              </ul>
            </div>
            <div>
              <h3>{{ t.entrantes }}</h3>
              <ul>
                @for (r of f.relaciones.entrantes; track r.tabla + r.columna) {
                  <li><a [routerLink]="['/tablero/catalogo-de-datos', r.tabla]">{{ r.esquema }}.{{ r.tabla }}</a> (<code>{{ r.columna }}</code>)</li>
                } @empty { <li>{{ t.sinRelaciones }}</li> }
              </ul>
            </div>
          </div>
        } @else {
          <p role="status" class="nota">{{ t.noExiste(tabla()) }}</p>
        }
      </ap-estado-de-pantalla>
    </section>
  `,
  styles: `
    .pantalla { padding: var(--s5); max-width: 72rem; display: grid; gap: var(--s4); }
    .pantalla > * { min-width: 0; }
    .largo { display: block; min-width: min(12rem, 100%); white-space: normal; overflow-wrap: anywhere; }
    /* Nombres como fecha_hora_acreditacion no tienen dónde cortar: en el celular empujaban la página. */
    ap-tabla-de-datos { overflow-wrap: anywhere; }
    h1 { margin: 0; overflow-wrap: anywhere; }
    h2, h3 { margin: 0; }
    .esquema { color: var(--text-2); font-weight: 400; font-size: .7em; }
    dl { display: grid; gap: var(--s2); margin: 0; }
    dt { font-weight: 600; }
    dd { margin: 0; color: var(--text-2); }
    .nota { display: block; margin-top: var(--s2); color: var(--text); }
    .relaciones { display: grid; gap: var(--s4); grid-template-columns: repeat(auto-fit, minmax(16rem, 1fr)); }
    ul { margin: var(--s2) 0 0; padding-left: var(--s5); display: grid; gap: var(--s1); overflow-wrap: anywhere; }
    a { color: var(--text); }
    a:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
    .volver { justify-self: start; min-height: var(--area-tactil); display: inline-flex; align-items: center; }
  `,
})
export class FichaDeTabla {
  readonly tabla = input.required<string>()
  protected readonly t = textosCatalogo.ficha
  protected readonly remoto = inject(CatalogoDeDatosRemoto)
  protected readonly ficha = computed(() =>
    this.remoto.recurso.hasValue() ? this.remoto.recurso.value()?.tablas.find((t) => t.tabla === this.tabla()) : undefined,
  )
  protected readonly columnasDeLaFicha = computed(() => {
    const f = this.ficha()
    return f ? filasDeColumnas(f) : []
  })
  protected readonly porNombre = (c: FilaDeColumna) => c.id
  protected readonly columnas: Columna<FilaDeColumna>[] = [
    { clave: 'nombre', titulo: textosCatalogo.ficha.columnasTabla.nombre },
    { clave: 'tipo', titulo: textosCatalogo.ficha.columnasTabla.tipo },
    { clave: 'negocio', titulo: textosCatalogo.ficha.columnasTabla.negocio },
    { clave: 'sistema', titulo: textosCatalogo.ficha.columnasTabla.sistema },
    { clave: 'sensibilidad', titulo: textosCatalogo.ficha.columnasTabla.sensibilidad },
  ]
}
