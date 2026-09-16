import { ChangeDetectionStrategy, Component, inject } from '@angular/core'
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router'
import { Sesion } from '../nucleo/sesion'

type Seccion = { ruta: string; texto: string; permiso: string }

/**
 * El shell «financiero»: menú de dominios, cabecera con el rol de quien opera, y el
 * `router-outlet` de las pantallas. **El menú oculta por comodidad**; el permiso real
 * lo exige `canMatch` en cada ruta y lo verifica el servidor — este componente no es la
 * barrera de seguridad, solo evita ofrecer un enlace que va a terminar en un 403.
 */
@Component({
  selector: 'ap-shell-financiero',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="shell">
      <header class="cabecera">
        <p class="marca">AportaYa · Backoffice</p>
        @if (sesion.rol(); as rol) {
          <p class="rol">{{ rol }}</p>
        }
      </header>
      <nav class="menu" aria-label="Secciones del backoffice">
        <ul>
          @for (s of secciones; track s.ruta) {
            @if (sesion.puede(s.permiso)) {
              <li><a [routerLink]="['/', s.ruta]" routerLinkActive="activo">{{ s.texto }}</a></li>
            }
          }
        </ul>
      </nav>
      <main id="contenido">
        <router-outlet />
      </main>
    </div>
  `,
  styles: `
    .shell { display: grid; grid-template-columns: minmax(12rem, 16rem) 1fr; grid-template-rows: auto 1fr; grid-template-areas: 'cabecera cabecera' 'menu contenido'; min-height: 100dvh; background: var(--bg); }
    .cabecera { grid-area: cabecera; display: flex; align-items: center; justify-content: space-between; padding: var(--s3) var(--s5); border-bottom: var(--borde-fino) solid var(--border); background: var(--surface); }
    .marca { margin: 0; font-weight: 700; color: var(--text); }
    .rol { margin: 0; color: var(--text-2); font-size: .9em; }
    .menu { grid-area: menu; border-right: var(--borde-fino) solid var(--border); background: var(--surface); padding: var(--s4); }
    .menu ul { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: var(--s1); }
    .menu a { display: block; min-height: var(--area-tactil); padding: var(--s2) var(--s3); border-radius: var(--r-md); color: var(--text-2); text-decoration: none; }
    .menu a:hover, .menu a.activo { background: var(--g100); color: var(--brand-texto); font-weight: 600; }
    .menu a:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
    main { grid-area: contenido; min-width: 0; }
    @media (max-width: 40rem) {
      .shell { grid-template-columns: 1fr; grid-template-areas: 'cabecera' 'menu' 'contenido'; }
      .menu ul { flex-direction: row; flex-wrap: wrap; }
    }
  `,
})
export class ShellFinanciero {
  protected readonly sesion = inject(Sesion)
  protected readonly secciones: Seccion[] = [
    { ruta: 'tablero', texto: 'Tablero', permiso: 'ver:tablero' },
    { ruta: 'operacion', texto: 'Operación', permiso: 'ver:operacion' },
    { ruta: 'cumplimiento', texto: 'Cumplimiento', permiso: 'ver:cumplimiento' },
    { ruta: 'sistemas', texto: 'Sistemas', permiso: 'ver:sistemas' },
    { ruta: 'contabilidad', texto: 'Contabilidad', permiso: 'ver:contabilidad' },
    { ruta: 'publicidad', texto: 'Publicidad', permiso: 'ver:publicidad' },
  ]
}
