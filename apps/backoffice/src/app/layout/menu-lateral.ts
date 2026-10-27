import { ChangeDetectionStrategy, Component, computed, inject, input, output, signal } from '@angular/core'
import { RouterLink } from '@angular/router'
import { Icono } from '@aportaya/ui/icono/icono'
import { MENU, type Entrada, type Grupo } from '../nucleo/menu'
import { entradaDe, entradasVisibles } from '../nucleo/navegacion'
import { Sesion } from '../nucleo/sesion'
import { IconoDeSeccion } from './icono-de-seccion'
import { UrlActual } from './url-actual'

const CLAVE = 'ap-menu:plegados'

/**
 * La navegación del backoffice: grupos plegables con rótulo en mayúsculas y **una entrada por sección**. Las
 * pantallas de cada sección no se despliegan acá: viven en las pestañas de sección (`ap-pestanas-de-seccion`), como
 * en el portal interno de Atlas — el menú pasó de más de cuarenta enlaces a uno por sección. El grupo de la ruta
 * actual arranca abierto; lo que se pliega a mano se recuerda en la sesión. Se usa en la barra lateral y en el cajón
 * del celular.
 *
 * Una entrada se dibuja solo si la sesión puede abrir **alguna** de sus pestañas, y lleva a la primera que puede abrir
 * (`entradasVisibles`): antes filtraba solo por el permiso de sección y «Sistemas» rebotaba a Operación a quien no
 * tenía el rol de sistemas (F1).
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
          <button type="button" class="rotulo" [attr.aria-expanded]="abierto(g)" [attr.aria-controls]="idDe(g)" (click)="alternar(g)">
            <span>{{ g.titulo }}</span>
            <ap-icono nombre="chevronAbajo" tamano="s4" class="chevron" [class.plegado]="!abierto(g)" />
          </button>
        }
        <ul [id]="idDe(g)" [hidden]="!abierto(g)">
          @for (e of g.entradas; track e.ruta) {
            <li>
              <a [routerLink]="e.destino" [attr.data-tutorial-id]="'menu-' + e.ruta" [class.activo]="activa(e)" [attr.aria-current]="activa(e) ? 'page' : null" (click)="navegado.emit()">
                <ap-icono-de-seccion [nombre]="e.ruta" /><span>{{ e.texto }}</span>
              </a>
            </li>
          }
        </ul>
      </section>
    }
  `,
  styles: `
    :host { display: grid; align-content: start; gap: var(--s2); color: var(--white); }
    .rotulo { display: flex; width: 100%; align-items: center; gap: var(--s2); min-height: var(--area-tactil); padding: 0 var(--s3); border: 0; border-radius: var(--r-md); background: transparent; color: color-mix(in srgb, var(--white) 55%, transparent); font: var(--t-eyebrow); letter-spacing: var(--t-eyebrow-track); text-transform: uppercase; text-align: left; cursor: pointer; }
    .rotulo:hover { color: var(--white); background: color-mix(in srgb, var(--white) 6%, transparent); }
    .rotulo:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
    .rotulo span { flex: 1; }
    .chevron { transition: transform var(--mov-entrada) var(--curva-llega); }
    .chevron.plegado { transform: rotate(-90deg); }
    ul { list-style: none; margin: 0; padding: 0; display: grid; gap: var(--s1); }
    /* El atributo hidden pierde contra un display propio: un grupo plegado se vería igual que abierto. */
    ul[hidden] { display: none; }
    a { display: flex; align-items: center; gap: var(--s3); min-height: var(--area-tactil); padding: var(--s2) var(--s3); border-radius: var(--r-md); font: var(--t-cuerpo); color: color-mix(in srgb, var(--white) 78%, transparent); text-decoration: none; transition: background-color var(--mov-micro) ease-out, color var(--mov-micro) ease-out; }
    a:hover { background: color-mix(in srgb, var(--white) 8%, transparent); color: var(--white); }
    /* La activa: superficie apenas más clara y una barra verde a la izquierda, no solo un color (95.4.6). */
    a.activo { background: color-mix(in srgb, var(--white) 9%, transparent); color: var(--white); font-weight: 600; box-shadow: inset var(--borde-foco) 0 0 0 var(--g300); }
    a.activo ap-icono-de-seccion { color: var(--g300); }
    a:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
  `,
})
export class MenuLateral {
  private readonly sesion = inject(Sesion)
  private readonly url = inject(UrlActual).url
  /** El cajón del celular se cierra al navegar. */
  readonly navegado = output<void>()
  readonly menu = input<readonly Grupo[]>(MENU)
  private readonly plegados = signal<string[]>(leerPlegados())

  readonly visibles = computed(() => entradasVisibles(this.sesion, this.menu()))

  idDe(g: Grupo): string {
    return 'menu-grupo-' + (g.titulo ?? 'inicio').toLowerCase()
  }
  /** Abierto salvo que se haya plegado a mano; el grupo de la ruta activa se abre siempre. */
  abierto(g: Grupo): boolean {
    if (!g.titulo) return true
    if (g.entradas.some((e) => this.activa(e))) return true
    return !this.plegados().includes(g.titulo)
  }
  /** La sección dueña de la URL: `/tablero/catalogo-de-datos` es Configuración, no Inicio. */
  private readonly entradaActiva = computed(() => entradaDe(this.url(), this.menu()))
  activa(e: Entrada): boolean {
    return this.entradaActiva()?.ruta === e.ruta
  }
  alternar(g: Grupo): void {
    if (!g.titulo) return
    const t = g.titulo
    this.plegados.update((p) => (p.includes(t) ? p.filter((x) => x !== t) : [...p, t]))
    guardarPlegados(this.plegados())
  }
}

function leerPlegados(): string[] {
  return leerAlmacen(CLAVE)
}
function guardarPlegados(p: string[]): void {
  guardarAlmacen(CLAVE, p)
}
function leerAlmacen(clave: string): string[] {
  try {
    return JSON.parse(sessionStorage.getItem(clave) ?? '[]') as string[]
  } catch {
    return []
  }
}
function guardarAlmacen(clave: string, p: string[]): void {
  try {
    sessionStorage.setItem(clave, JSON.stringify(p))
  } catch {
    // Sin almacenamiento el menú funciona igual, solo no recuerda.
  }
}
