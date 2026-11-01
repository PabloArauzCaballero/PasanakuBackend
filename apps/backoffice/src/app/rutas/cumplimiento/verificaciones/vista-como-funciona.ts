import { ChangeDetectionStrategy, Component } from '@angular/core'
import { bloquesDeTexto } from '@aportaya/ui/seccion-clave-valor/seccion-clave-valor'
import { COMO_FUNCIONA } from './textos-como-funciona'

/**
 * La pestaña «Cómo funciona» de la ficha: el negocio y el sistema detrás de un expediente de identidad, en secciones
 * con su fuente. Es para quien revisa por primera vez y para quien audita: qué se decide, qué hace cumplir el
 * servidor y qué dato personal se ve y cuál no. El índice de arriba salta a cada sección.
 */
@Component({
  selector: 'ap-vista-como-funciona',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <nav aria-label="Secciones de cómo funciona">
      <ul class="indice">
        @for (s of secciones; track s.id) { <li><a [href]="'#como-' + s.id" (click)="ir($event, s.id)">{{ s.titulo }}</a></li> }
      </ul>
    </nav>
    @for (s of secciones; track s.id) {
      <section [id]="'como-' + s.id" [attr.aria-labelledby]="'titulo-como-' + s.id" tabindex="-1">
        <h2 [id]="'titulo-como-' + s.id">{{ s.titulo }}</h2>
        @for (b of bloques(s.cuerpo); track $index) {
          @switch (b.tipo) {
            @case ('lista') { <ul>@for (x of b.items; track $index) { <li>{{ x }}</li> }</ul> }
            @case ('numerada') { <ol>@for (x of b.items; track $index) { <li>{{ x }}</li> }</ol> }
            @default { <p>{{ b.texto }}</p> }
          }
        }
        @if (s.fuente) { <p class="fuente">Fuente: {{ s.fuente }}</p> }
      </section>
    }
  `,
  styles: `
    :host { display: grid; grid-template-columns: repeat(auto-fit, minmax(min(100%, 28rem), 1fr)); gap: var(--s4); min-width: 0; }
    nav { grid-column: 1 / -1; }
    .indice { list-style: none; margin: 0; padding: 0; display: flex; flex-wrap: wrap; gap: var(--s2); }
    .indice a { display: inline-flex; align-items: center; min-height: var(--area-tactil); padding: 0 var(--s3); border: var(--borde-fino) solid var(--border); border-radius: var(--r-pill); color: var(--brand-texto); font: var(--t-cuerpo-chico); font-weight: 600; text-decoration: none; }
    .indice a:hover { background: var(--brand-bg); }
    a:focus-visible, section:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
    section { display: grid; gap: var(--s2); align-content: start; padding: var(--s4); border: var(--borde-fino) solid var(--border); border-radius: var(--r-md); background: var(--surface); }
    h2 { margin: 0; font: var(--t-cuerpo-fuerte); color: var(--text); }
    p, ul, ol { margin: 0; font: var(--t-cuerpo-chico); color: var(--text-2); }
    ul, ol { display: grid; gap: var(--s1); padding-left: var(--s4); }
    .fuente { color: var(--text-3); font-style: italic; }
  `,
})
export class VistaComoFunciona {
  protected readonly secciones = COMO_FUNCIONA
  protected readonly bloques = bloquesDeTexto

  /** El ancla salta y lleva el foco a la sección: con teclado, el salto se ve y se lee. */
  protected ir(evento: Event, id: string): void {
    const destino = (evento.currentTarget as HTMLElement).ownerDocument.getElementById('como-' + id)
    if (!destino) return
    evento.preventDefault()
    destino.scrollIntoView({ block: 'start' })
    destino.focus()
  }
}
