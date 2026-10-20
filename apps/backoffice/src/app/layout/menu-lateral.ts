import { ChangeDetectionStrategy, Component, computed, inject, input, output } from '@angular/core'
import { Router, RouterLink, RouterLinkActive } from '@angular/router'
import { toSignal } from '@angular/core/rxjs-interop'
import { filter, map, startWith } from 'rxjs'
import { NavigationEnd } from '@angular/router'
import { MENU, type Entrada, type Grupo } from '../nucleo/menu'
import { Sesion } from '../nucleo/sesion'
import { IconoDeSeccion } from './icono-de-seccion'

/**
 * La navegación del backoffice: grupos con rótulo en mayúsculas, una entrada por
 * sección y sus pantallas siempre abiertas con su respectivo ícono. Se usa en la barra lateral y en el cajón del celular.
 */
@Component({
  selector: 'ap-menu-lateral',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, RouterLinkActive, IconoDeSeccion],
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
              </div>
              @if (e.hijos?.length) {
                <ul class="hijos">
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
              }
            </li>
          }
        </ul>
      </section>
    }
  `,
  styles: `
    :host { display: grid; align-content: start; gap: var(--s3); color: var(--white); }
    .grupo { display: grid; gap: var(--s1); }
    .rotulo {
      display: flex;
      align-items: center;
      padding: var(--s2) var(--s3) var(--s1);
      color: color-mix(in srgb, var(--white) 48%, transparent);
      font: var(--t-eyebrow);
      letter-spacing: var(--t-eyebrow-track);
      text-transform: uppercase;
      user-select: none;
    }
    ul { list-style: none; margin: 0; padding: 0; display: grid; gap: calc(var(--s1) / 2); }
    .entrada-fila { display: flex; align-items: center; width: 100%; border-radius: var(--r-md); position: relative; }
    .entrada-fila a { flex: 1; min-width: 0; }
    a {
      display: flex;
      align-items: center;
      gap: var(--s2);
      min-height: calc(var(--area-tactil) - var(--s2));
      padding: var(--s2) var(--s3);
      border-radius: var(--r-md);
      font: var(--t-cuerpo-chico);
      font-weight: 500;
      color: color-mix(in srgb, var(--white) 80%, transparent);
      text-decoration: none;
      transition: background-color var(--mov-micro) cubic-bezier(0.16, 1, 0.3, 1),
                  color var(--mov-micro) cubic-bezier(0.16, 1, 0.3, 1),
                  transform var(--mov-micro) cubic-bezier(0.16, 1, 0.3, 1);
      position: relative;
    }
    a:hover {
      background: color-mix(in srgb, var(--white) 9%, transparent);
      color: var(--white);
      transform: translateX(calc(var(--s1) / 2));
    }
    a.activo {
      background: color-mix(in srgb, var(--g400) 24%, transparent);
      color: var(--white);
      font-weight: 600;
      box-shadow: inset var(--borde-foco) 0 0 var(--g300), var(--sombra-2);
    }
    a.activo .icono-padre,
    a.activo .icono-hijo {
      color: var(--g300);
      opacity: 1;
    }
    a:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
    .texto-entrada { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .icono-padre { flex: none; width: 1.25rem; height: 1.25rem; opacity: 0.85; transition: transform var(--mov-micro) ease; }
    a:hover .icono-padre { opacity: 1; transform: scale(1.08); }

    .hijos {
      margin: calc(var(--s1) / 2) 0 var(--s2) var(--s3);
      padding-left: var(--s2);
      border-left: var(--borde-fino) solid color-mix(in srgb, var(--white) 12%, transparent);
      display: grid;
      gap: calc(var(--s1) / 2);
    }
    .hijos a {
      min-height: 2.125rem;
      padding: var(--s1) var(--s2);
      gap: var(--s2);
      font-size: 0.8125rem;
      color: color-mix(in srgb, var(--white) 65%, transparent);
    }
    .hijos a:hover {
      color: var(--white);
      background: color-mix(in srgb, var(--white) 7%, transparent);
      transform: translateX(calc(var(--s1) / 2));
    }
    .hijos a.activo {
      color: var(--white);
      background: color-mix(in srgb, var(--g400) 28%, transparent);
      font-weight: 600;
      box-shadow: inset var(--borde-desfase) 0 0 var(--g300);
    }
    .texto-hijo { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .icono-hijo {
      flex: none;
      width: 1rem;
      height: 1rem;
      opacity: 0.75;
      transition: transform var(--mov-micro) ease, opacity var(--mov-micro) ease;
    }
    .hijos a:hover .icono-hijo,
    .hijos a.activo .icono-hijo {
      opacity: 1;
      color: var(--g300);
    }
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

  readonly visibles = computed(() =>
    this.menu()
      .map((g) => ({ ...g, entradas: g.entradas.filter((e) => this.sesion.puede(e.permiso)) }))
      .filter((g) => g.entradas.length > 0),
  )

  idDe(g: Grupo): string {
    return 'menu-grupo-' + (g.titulo ?? 'inicio').toLowerCase()
  }

  /** Todas las secciones y pantallas están siempre abiertas. */
  abierto(): boolean {
    return true
  }

  hijosAbiertos(): boolean {
    return true
  }

  activa(e: Entrada): boolean {
    const url = this.url().split('?')[0]!
    return url === e.destino || url.startsWith(e.destino + '/')
  }

  alNavegar(): void {
    this.navegado.emit()
  }
}
