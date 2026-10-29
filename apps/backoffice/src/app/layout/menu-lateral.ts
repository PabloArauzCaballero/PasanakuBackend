import { ChangeDetectionStrategy, Component, computed, inject, input, output } from '@angular/core'
import { RouterLink } from '@angular/router'
import { Icono } from '@aportaya/ui/icono/icono'
import { MENU, type Entrada, type Grupo, type Pantalla } from '../nucleo/menu'
import { entradaDe, entradasVisibles, pantallaActiva, pantallasVisibles } from '../nucleo/navegacion'
import { Sesion } from '../nucleo/sesion'
import { IconoDeSeccion } from './icono-de-seccion'
import { UrlActual } from './url-actual'

/**
 * La navegación del backoffice: grupos **fijos** con rótulo en mayúsculas y, debajo de cada sección, **todas** sus
 * pantallas con su ícono — también las de un subgrupo («Integraciones», «Personal y accesos»), con sangría. Nada se
 * pliega (2026-10-28, pedido de Pablo: «fijo, no desplegable, scrolleable hacia abajo»): la que se desplaza es la barra
 * lateral (`.lateral { overflow-y: auto }` en `shell-financiero.css`). Se usa en la barra lateral y en el cajón del
 * celular.
 *
 * El rótulo de un grupo («Plataforma») lleva su ícono y es un enlace a la primera sección del grupo que la sesión
 * puede abrir: fijo, nunca desplegable (2026-10-28).
 *
 * Una entrada se dibuja solo si la sesión puede abrir **alguna** de sus pantallas, y lleva a la primera que puede abrir
 * (`entradasVisibles`); debajo se listan solo las que puede abrir (`pantallasVisibles`): el menú no ofrece lo que
 * después rebota (F1). El permiso real lo sigue exigiendo la guardia de cada ruta.
 */
@Component({
  selector: 'ap-menu-lateral',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, Icono, IconoDeSeccion],
  host: { role: 'navigation', 'aria-label': 'Secciones del backoffice', 'data-tutorial-id': 'menu-principal' },
  template: `
    @for (g of visibles(); track g.titulo) {
      <section class="grupo">
        @if (g.titulo) {
          <h2 class="rotulo"><a [routerLink]="g.entradas[0]?.destino" [title]="g.titulo" [attr.data-tutorial-id]="'menu-grupo-' + g.icono" (click)="navegado.emit()"><ap-icono-de-seccion [nombre]="g.icono" /><span>{{ g.titulo }}</span></a></h2>
        }
        <ul>
          @for (e of g.entradas; track e.ruta) {
            <li>
              <a class="seccion" [routerLink]="e.destino" [title]="e.texto" [attr.data-tutorial-id]="'menu-' + e.ruta" [class.activo]="activa(e)" (click)="navegado.emit()">
                <ap-icono-de-seccion [nombre]="e.ruta" /><span>{{ e.texto }}</span>
              </a>
              @let pantallas = pantallasDe(e);
              @if (pantallas.length) {
                <ul class="pantallas" [attr.aria-label]="'Pantallas de ' + e.texto">
                  @for (p of pantallas; track p.texto) {
                    <li>
                      @if (p.grupo) {
                        <span class="subgrupo" [title]="p.texto"><ap-icono [nombre]="p.icono" tamano="s4" /><span>{{ p.texto }}</span></span>
                        <ul class="pantallas anidadas" [attr.aria-label]="p.texto">
                          @for (h of p.grupo; track h.ruta) {
                            <li>
                              <a [routerLink]="h.ruta" [title]="h.texto" [class.activo]="esActual(h.ruta)" [attr.aria-current]="esActual(h.ruta) ? 'page' : null" (click)="navegado.emit()">
                                <ap-icono [nombre]="h.icono" tamano="s4" /><span>{{ h.texto }}</span>
                              </a>
                            </li>
                          }
                        </ul>
                      } @else {
                        <a [routerLink]="p.ruta" [title]="p.texto" [class.activo]="esActual(p.ruta)" [attr.aria-current]="esActual(p.ruta) ? 'page' : null" (click)="navegado.emit()">
                          <ap-icono [nombre]="p.icono" tamano="s4" /><span>{{ p.texto }}</span>
                        </a>
                      }
                    </li>
                  }
                </ul>
              }
            </li>
          }
        </ul>
      </section>
    }
  `,
  styles: `
    /*
     * Una sola columna que no crece con el texto (2026-10-29): un ítem de grid tiene «min-width: auto» y la pista se
     * estiraba al rótulo más largo — «Postulaciones a organizador» ensanchaba todo el menú más allá de la barra (scroll
     * horizontal, la píldora activa cortada contra el borde y, con fuentes más anchas, el texto cortado sin aviso).
     */
    :host { display: grid; grid-template-columns: minmax(0, 1fr); align-content: start; gap: var(--s3); color: var(--white); }
    .grupo { display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s1); }
    /* El rótulo de grupo: ícono + texto fijos, y un enlace que abre la primera sección del grupo (2026-10-28). */
    .rotulo { margin: 0; font: inherit; }
    .rotulo a { color: color-mix(in srgb, var(--white) 62%, transparent); font: var(--t-cuerpo-chico); font-weight: 600; }
    ul { list-style: none; margin: 0; padding: 0; display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s1); }
    a { display: flex; align-items: center; gap: var(--s3); min-height: var(--area-tactil); padding: var(--s2) var(--s3); border-radius: var(--r-md); font: var(--t-cuerpo); color: color-mix(in srgb, var(--white) 78%, transparent); text-decoration: none; transition: background-color var(--mov-micro) ease-out, color var(--mov-micro) ease-out; }
    a:hover { background: color-mix(in srgb, var(--white) 8%, transparent); color: var(--white); }
    /* La activa: superficie apenas más clara y una barra verde a la izquierda, no solo un color (95.4.6). */
    a.activo { background: color-mix(in srgb, var(--white) 9%, transparent); color: var(--white); font-weight: 600; box-shadow: inset var(--borde-foco) 0 0 0 var(--g300); }
    a.activo ap-icono-de-seccion, a.activo ap-icono { color: var(--g300); }
    a:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
    /* Las pantallas de la sección, siempre a la vista: sangría y un filete que las ata a su sección. */
    .pantallas { gap: calc(var(--s1) / 2); margin: calc(var(--s1) / 2) 0 var(--s2) var(--s4); padding-left: var(--s2); border-left: var(--borde-fino) solid color-mix(in srgb, var(--white) 14%, transparent); }
    .pantallas a { gap: var(--s2); min-height: calc(var(--area-tactil) - var(--s2)); padding: var(--s1) var(--s2); font: var(--t-cuerpo-chico); color: color-mix(in srgb, var(--white) 70%, transparent); }
    /*
     * El rótulo largo baja a una segunda línea y, si ni así entra, termina en «…»; el texto completo queda en «title»
     * y el nombre accesible no cambia (sale del contenido). El ícono («flex: none» en su propio host) no se achica.
     */
    a > span, .subgrupo > span { min-width: 0; overflow-wrap: anywhere; display: -webkit-box; -webkit-box-orient: vertical; -webkit-line-clamp: 2; line-clamp: 2; overflow: hidden; }
    .subgrupo { display: flex; align-items: center; gap: var(--s2); padding: var(--s1) var(--s2); font: var(--t-cuerpo-chico); font-weight: 600; color: color-mix(in srgb, var(--white) 60%, transparent); }
    .anidadas { margin-left: var(--s3); }
    @media (prefers-reduced-motion: reduce) { a { transition: none; } }
  `,
})
export class MenuLateral {
  private readonly sesion = inject(Sesion)
  private readonly url = inject(UrlActual).url
  /** El cajón del celular se cierra al navegar. */
  readonly navegado = output<void>()
  readonly menu = input<readonly Grupo[]>(MENU)

  readonly visibles = computed(() => entradasVisibles(this.sesion, this.menu()))

  /** Las pantallas que esta sesión puede abrir, con los subgrupos ya filtrados. */
  pantallasDe(e: Entrada): Pantalla[] {
    return pantallasVisibles(e, this.sesion)
  }
  /** La sección dueña de la URL: `/tablero/catalogo-de-datos` es Configuración, no Inicio. */
  private readonly entradaActiva = computed(() => entradaDe(this.url(), this.menu()))
  activa(e: Entrada): boolean {
    return this.entradaActiva()?.ruta === e.ruta
  }
  /** La pantalla de la URL: la de ruta más larga que la contiene (`pantallaActiva`), no cualquier prefijo. */
  private readonly hojaActual = computed(() => {
    const e = this.entradaActiva()
    return e ? pantallaActiva(e, this.url())?.hoja.ruta : undefined
  })
  esActual(ruta: string): boolean {
    return this.hojaActual() === ruta
  }
}
