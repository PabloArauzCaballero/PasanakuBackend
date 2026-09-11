import { ChangeDetectionStrategy, Component } from '@angular/core'
import { RouterLink, RouterOutlet } from '@angular/router'

@Component({
  selector: 'app-root',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, RouterLink],
  template: `
    <header>
      <a routerLink="/" class="marca">Aporta<span>Ya</span></a>
      <nav aria-label="Principal">
        <a routerLink="/como-funciona">Cómo funciona</a>
        <a routerLink="/tarifas">Tarifas</a>
        <a routerLink="/transparencia">Transparencia</a>
        <a routerLink="/preguntas">Preguntas</a>
        <a routerLink="/plazos">Plazos</a>
      </nav>
    </header>
    <router-outlet />
    <footer>
      <nav aria-label="Legal y soporte">
        <a routerLink="/seguridad">Seguridad</a>
        <a routerLink="/privacidad">Privacidad</a>
        <a routerLink="/reclamos">Reclamos</a>
        <a routerLink="/contrato-de-adhesion">Contrato de adhesión</a>
        <a routerLink="/legal/estado-regulatorio">Estado regulatorio</a>
        <a routerLink="/descargar">Descargar</a>
      </nav>
      <p class="identidad">AportaYa · Bolivia · solicitud de licencia en trámite ante la ASFI</p>
    </footer>
  `,
  styles: `
    header { display: flex; align-items: center; gap: var(--s4); padding: var(--s3) var(--s5); border-bottom: var(--borde-fino) solid var(--border); }
    .marca { display: inline-flex; align-items: center; min-height: var(--area-tactil); font-family: var(--font-d); font-weight: 700; color: var(--text); text-decoration: none; }
    .marca span { color: var(--accent-texto); }
    nav a { display: inline-flex; align-items: center; min-height: var(--area-tactil); color: var(--text-2); text-decoration: none; padding: 0 var(--s3); border-radius: var(--r-pill); }
    nav a:hover { background: var(--surface-2); }
    footer { padding: var(--s5); border-top: var(--borde-fino) solid var(--border); margin-top: var(--s7); }
    footer nav { display: flex; flex-wrap: wrap; gap: var(--s2); }
    .identidad { color: var(--text-3); font-size: 0.85rem; margin-top: var(--s3); }
  `,
})
export class App {}
