import { ChangeDetectionStrategy, Component, computed, inject, input, output, signal } from '@angular/core'
import { Router, RouterLink, RouterLinkActive } from '@angular/router'
import { toSignal } from '@angular/core/rxjs-interop'
import { filter, map, startWith } from 'rxjs'
import { NavigationEnd } from '@angular/router'
import { Icono } from '@aportaya/ui/icono/icono'
import { MENU, type Entrada, type Grupo } from '../nucleo/menu'
import { Sesion } from '../nucleo/sesion'
import { IconoDeSeccion } from './icono-de-seccion'

const CLAVE = 'ap-menu:plegados'
const CLAVE_PLEGADOS_HIJOS = 'ap-menu:hijos-plegados'
const CLAVE_DESPLEGADOS_HIJOS = 'ap-menu:hijos-desplegados'

/**
 * La navegación del backoffice: grupos plegables con rótulo en mayúsculas, una entrada por
 * sección y sus pantallas desplegables. El grupo y la sección de la ruta actual arrancan abiertos; lo
 * que se pliega o despliega a mano se recuerda en la sesión. Se usa en la barra lateral y en el cajón del celular.
 */
@Component({
  selector: 'ap-menu-lateral',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, RouterLinkActive, Icono, IconoDeSeccion],
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
              <div class="entrada-fila">
                <a [routerLink]="e.destino" [attr.data-tutorial-id]="'menu-' + e.ruta" routerLinkActive="activo" ariaCurrentWhenActive="page" [routerLinkActiveOptions]="{ exact: e.destino === '/tablero' }" (click)="alNavegar(e)">
                  <ap-icono-de-seccion [nombre]="e.ruta" /><span>{{ e.texto }}</span>
                </a>
                @if (e.hijos?.length) {
                  <button type="button" class="boton-chevron-hijos" [attr.aria-expanded]="hijosAbiertos(e)" [attr.aria-label]="(hijosAbiertos(e) ? 'Plegar opciones de ' : 'Desplegar opciones de ') + e.texto" (click)="alternarHijos(e, $event)">
                    <ap-icono nombre="chevronAbajo" tamano="s4" class="chevron-hijos" [class.plegado]="!hijosAbiertos(e)" />
                  </button>
                }
              </div>
              @if (e.hijos?.length && hijosAbiertos(e)) {
                <ul class="hijos">
                  @for (h of e.hijos; track h.ruta) {
                    <li><a [routerLink]="h.ruta ? e.destino + '/' + h.ruta : e.destino" routerLinkActive="activo" ariaCurrentWhenActive="page" [routerLinkActiveOptions]="{ exact: true }" (click)="navegado.emit()">{{ h.texto }}</a></li>
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
    .entrada-fila { display: flex; align-items: center; width: 100%; border-radius: var(--r-md); position: relative; }
    .entrada-fila a { flex: 1; min-width: 0; }
    a { display: flex; align-items: center; gap: var(--s3); min-height: var(--area-tactil); padding: var(--s2) var(--s3); border-radius: var(--r-md); font: var(--t-cuerpo); color: color-mix(in srgb, var(--white) 78%, transparent); text-decoration: none; transition: background-color var(--mov-micro) ease-out, color var(--mov-micro) ease-out; }
    a:hover { background: color-mix(in srgb, var(--white) 8%, transparent); color: var(--white); }
    a.activo { background: color-mix(in srgb, var(--white) 14%, transparent); color: var(--white); font-weight: 600; box-shadow: inset var(--borde-desfase) 0 0 var(--g300); }
    a:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
    .boton-chevron-hijos { display: flex; align-items: center; justify-content: center; width: var(--area-tactil); height: var(--area-tactil); padding: 0; border: 0; border-radius: var(--r-md); background: transparent; color: color-mix(in srgb, var(--white) 60%, transparent); cursor: pointer; transition: color var(--mov-micro) ease-out, background-color var(--mov-micro) ease-out; }
    .boton-chevron-hijos:hover { color: var(--white); background: color-mix(in srgb, var(--white) 10%, transparent); }
    .boton-chevron-hijos:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
    .chevron-hijos { transition: transform var(--mov-entrada) var(--curva-llega); }
    .chevron-hijos.plegado { transform: rotate(-90deg); }
    .hijos { margin: var(--s1) 0 var(--s1) var(--s4); padding-left: var(--s3); border-left: var(--borde-fino) solid color-mix(in srgb, var(--white) 14%, transparent); }
    .hijos a { min-height: calc(var(--area-tactil) - var(--s2)); padding: var(--s1) var(--s3); font: var(--t-cuerpo-chico); }
    .hijos a.activo { box-shadow: none; }
  `,
})
export class MenuLateral {
  private readonly sesion = inject(Sesion)
  private readonly router = inject(Router)
  /** El cajón del celular se cierra al navegar. */
  readonly navegado = output<void>()
  readonly menu = input<readonly Grupo[]>(MENU)
  private readonly url = toSignal(
    this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd),
      map((e) => e.urlAfterRedirects),
      startWith(this.router.url),
    ),
    { initialValue: this.router.url },
  )
  private readonly plegados = signal<string[]>(leerPlegados())
  private readonly hijosPlegados = signal<string[]>(leerAlmacen(CLAVE_PLEGADOS_HIJOS))
  private readonly hijosDesplegados = signal<string[]>(leerAlmacen(CLAVE_DESPLEGADOS_HIJOS))

  readonly visibles = computed(() =>
    this.menu()
      .map((g) => ({ ...g, entradas: g.entradas.filter((e) => this.sesion.puede(e.permiso)) }))
      .filter((g) => g.entradas.length > 0),
  )

  idDe(g: Grupo): string {
    return 'menu-grupo-' + (g.titulo ?? 'inicio').toLowerCase()
  }
  /** Abierto salvo que se haya plegado a mano; el grupo de la ruta activa se abre siempre. */
  abierto(g: Grupo): boolean {
    if (!g.titulo) return true
    if (g.entradas.some((e) => this.activa(e))) return true
    return !this.plegados().includes(g.titulo)
  }
  activa(e: Entrada): boolean {
    const url = this.url().split('?')[0]!
    return url === e.destino || url.startsWith(e.destino + '/')
  }
  alternar(g: Grupo): void {
    if (!g.titulo) return
    const t = g.titulo
    this.plegados.update((p) => (p.includes(t) ? p.filter((x) => x !== t) : [...p, t]))
    guardarPlegados(this.plegados())
  }
  hijosAbiertos(e: Entrada): boolean {
    if (!e.hijos?.length) return false
    if (this.hijosPlegados().includes(e.ruta)) return false
    if (this.hijosDesplegados().includes(e.ruta)) return true
    return this.activa(e)
  }
  alternarHijos(e: Entrada, ev: Event): void {
    ev.stopPropagation()
    ev.preventDefault()
    const r = e.ruta
    const abierta = this.hijosAbiertos(e)
    if (abierta) {
      this.hijosDesplegados.update((l) => l.filter((x) => x !== r))
      this.hijosPlegados.update((l) => (l.includes(r) ? l : [...l, r]))
    } else {
      this.hijosPlegados.update((l) => l.filter((x) => x !== r))
      this.hijosDesplegados.update((l) => (l.includes(r) ? l : [...l, r]))
    }
    guardarAlmacen(CLAVE_PLEGADOS_HIJOS, this.hijosPlegados())
    guardarAlmacen(CLAVE_DESPLEGADOS_HIJOS, this.hijosDesplegados())
  }
  alNavegar(e: Entrada): void {
    if (e.hijos?.length) {
      this.hijosPlegados.update((l) => l.filter((x) => x !== e.ruta))
      this.hijosDesplegados.update((l) => (l.includes(e.ruta) ? l : [...l, e.ruta]))
      guardarAlmacen(CLAVE_PLEGADOS_HIJOS, this.hijosPlegados())
      guardarAlmacen(CLAVE_DESPLEGADOS_HIJOS, this.hijosDesplegados())
    }
    this.navegado.emit()
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
