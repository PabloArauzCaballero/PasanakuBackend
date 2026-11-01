import { ChangeDetectionStrategy, Component, computed, effect, inject, input, output, signal, untracked } from '@angular/core'
import { Router, RouterLink, RouterLinkActive } from '@angular/router'
import { toSignal } from '@angular/core/rxjs-interop'
import { filter, map, startWith } from 'rxjs'
import { NavigationEnd } from '@angular/router'
import { MENU, type Entrada, type Grupo } from '../nucleo/menu'
import { Sesion } from '../nucleo/sesion'
import { Icono } from '@aportaya/ui/icono/icono'
import { IconoDeSeccion } from './icono-de-seccion'

/**
 * La navegación del backoffice: grupos con rótulo en mayúsculas, una entrada por
 * sección y sus pantallas con su respectivo ícono. Solo la sección activa queda desplegada;
 * las demás se pliegan y se abren con el botón de la flecha. Se usa en la barra lateral y en el cajón del celular.
 */
@Component({
  selector: 'ap-menu-lateral',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, RouterLinkActive, IconoDeSeccion, Icono],
  host: { role: 'navigation', 'aria-label': 'Secciones del backoffice', 'data-tutorial-id': 'menu-principal' },
  template: `
    @for (g of visibles(); track g.titulo) {
      <section class="grupo">
        @if (g.titulo) {
          <div class="rotulo" [id]="idDe(g)">
            <span>{{ g.titulo }}</span>
          </div>
        }
        <ul [id]="idDe(g) + '-lista'">
          @for (e of g.entradas; track e.ruta) {
            <li>
              <div class="entrada-fila">
                <a [routerLink]="e.destino" [attr.data-tutorial-id]="'menu-' + e.ruta" routerLinkActive="activo" ariaCurrentWhenActive="page" [routerLinkActiveOptions]="{ exact: e.destino === '/tablero' }" (click)="alNavegar()">
                  <ap-icono-de-seccion [nombre]="e.ruta" class="icono-padre" />
                  <span class="texto-entrada">{{ e.texto }}</span>
                </a>
                @if (e.hijos?.length) {
                  <button
                    type="button"
                    class="plegar"
                    [class.abierto]="abierta(e)"
                    [attr.aria-expanded]="abierta(e)"
                    [attr.aria-controls]="'menu-hijos-' + e.ruta"
                    [attr.aria-label]="(abierta(e) ? 'Ocultar las pantallas de ' : 'Mostrar las pantallas de ') + e.texto"
                    [attr.title]="abierta(e) ? 'Ocultar pantallas' : 'Mostrar pantallas'"
                    (click)="alternar(e)"
                  >
                    <ap-icono nombre="chevronDerecha" tamano="s4" />
                  </button>
                }
              </div>
              @if (e.hijos?.length) {
                <div class="plegable" [class.abierto]="abierta(e)" [attr.inert]="abierta(e) ? null : ''">
                <ul class="hijos" [id]="'menu-hijos-' + e.ruta">
                  @for (h of e.hijos; track h.ruta) {
                    <li>
                      <a [routerLink]="h.ruta ? e.destino + '/' + h.ruta : e.destino" routerLinkActive="activo" ariaCurrentWhenActive="page" [routerLinkActiveOptions]="{ exact: true }" (click)="navegado.emit()">
                        @if (h.icono) {
                          <ap-icono-de-seccion [nombre]="h.icono" class="icono-hijo" />
                        }
                        <span class="texto-hijo">{{ h.texto }}</span>
                      </a>
                    </li>
                  }
                </ul>
                </div>
              }
            </li>
          }
        </ul>
      </section>
    }
  `,
  styleUrl: './menu-lateral.css',
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

  readonly visibles = computed(() =>
    this.menu()
      .map((g) => ({ ...g, entradas: g.entradas.filter((e) => this.sesion.puede(e.permiso)) }))
      .filter((g) => g.entradas.length > 0),
  )

  idDe(g: Grupo): string {
    return 'menu-grupo-' + (g.titulo ?? 'inicio').toLowerCase()
  }

  /**
   * Lo que la persona plegó o desplegó a mano, por sección. Se vacía al cambiar de sección:
   * así el menú vuelve a mostrar abierta solo la sección en la que está.
   */
  private readonly aMano = signal<Readonly<Record<string, boolean>>>({})
  private readonly seccionActual = computed(() => this.visibles().flatMap((g) => g.entradas).find((e) => this.activa(e))?.ruta ?? null)

  constructor() {
    effect(() => {
      this.seccionActual()
      untracked(() => this.aMano.set({}))
    })
  }

  /** Abierta si la persona la abrió a mano o, si no la tocó, cuando es la sección activa. */
  abierta(e: Entrada): boolean {
    return this.aMano()[e.ruta] ?? this.activa(e)
  }

  alternar(e: Entrada): void {
    const siguiente = !this.abierta(e)
    this.aMano.update((m) => ({ ...m, [e.ruta]: siguiente }))
  }

  activa(e: Entrada): boolean {
    const url = this.url().split('?')[0]!
    return url === e.destino || url.startsWith(e.destino + '/')
  }

  alNavegar(): void {
    this.navegado.emit()
  }
}
