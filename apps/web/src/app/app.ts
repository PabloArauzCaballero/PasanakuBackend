import { ChangeDetectionStrategy, Component } from '@angular/core'
import { RouterLink, RouterOutlet } from '@angular/router'

@Component({
  selector: 'app-root',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, RouterLink],
  template: `
    <header>
      <a routerLink="/" class="marca">Aporta<span>Ya</span></a>
      <nav aria-label="Principal"><a routerLink="/plazos">Plazos</a></nav>
    </header>
    <router-outlet />
  `,
  styles: `
    header { display: flex; align-items: center; gap: var(--s4); padding: var(--s3) var(--s5); border-bottom: var(--borde-fino) solid var(--border); }
    .marca { display: inline-flex; align-items: center; min-height: var(--area-tactil); font-family: var(--font-d); font-weight: 700; color: var(--text); text-decoration: none; }
    .marca span { color: var(--accent-texto); }
    nav a { display: inline-flex; align-items: center; min-height: var(--area-tactil); color: var(--text-2); text-decoration: none; padding: 0 var(--s3); border-radius: var(--r-pill); }
    nav a:hover { background: var(--surface-2); }
  `,
})
export class App {}
