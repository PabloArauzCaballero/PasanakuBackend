import { ChangeDetectionStrategy, Component } from '@angular/core'
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router'
import { BannerDatosDeEjemplo } from '../../rutas/sistemas/banner-datos-de-ejemplo/banner-datos-de-ejemplo'
import { textosSistemas } from '../../rutas/sistemas/textos'

/**
 * El shell propio del backoffice de sistemas — el que `F6` dejó preparado, vacío, en
 * `layout/sistemas/` para que `B5` lo llenara. **No importa nada de `layout/shell-
 * financiero.ts` ni de ningún otro dominio** (delta D-2: dos productos, dos shells, dos
 * roles) — y `shell-financiero.ts` no importa nada de acá tampoco (verificado por
 * `grep`, ver el informe del carril). El menú de sistemas es de PLATAFORMA y SEGURIDAD,
 * nunca de un rol financiero.
 */
@Component({
  selector: 'ap-shell-sistemas',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, RouterLinkActive, RouterOutlet, BannerDatosDeEjemplo],
  template: `
    <!-- H2.S2.M3: el banner es del shell, no de cada pantalla — así se ve en las
         nueve sin que cada una tenga que acordarse de ponerlo, y nunca tapa el nav
         (va arriba de la grilla, no dentro de "contenido"). -->
    <ap-banner-datos-de-ejemplo />
    <div class="marco">
      <nav aria-label="Menú de sistemas">
        <h1>{{ t.titulo }}</h1>
        <ul>
          <li><a routerLink="servicios" routerLinkActive="activo">{{ t.servicios }}</a></li>
          <li><a routerLink="despliegues" routerLinkActive="activo">{{ t.despliegues }}</a></li>
          <li><a routerLink="base-de-datos" routerLinkActive="activo">{{ t.baseDeDatos }}</a></li>
          <li><a routerLink="respaldos" routerLinkActive="activo">{{ t.respaldos }}</a></li>
          <li><a routerLink="proveedores" routerLinkActive="activo">{{ t.proveedores }}</a></li>
          <li><a routerLink="outbox" routerLinkActive="activo">{{ t.outbox }}</a></li>
          <li><a routerLink="webhooks" routerLinkActive="activo">{{ t.webhooks }}</a></li>
          <li><a routerLink="accesos" routerLinkActive="activo">{{ t.accesos }}</a></li>
          <li><a routerLink="incidentes" routerLinkActive="activo">{{ t.incidentes }}</a></li>
        </ul>
      </nav>
      <div class="contenido"><router-outlet /></div>
    </div>
  `,
  styles: `
    .marco { display: grid; grid-template-columns: 16rem 1fr; min-height: 100dvh; }
    nav { padding: var(--s5); background: var(--surface-2); border-right: var(--borde-fino) solid var(--border); }
    h1 { font-size: 1.1em; margin-bottom: var(--s4); }
    ul { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: var(--s1); }
    a { display: block; min-height: var(--area-tactil); padding: var(--s2) var(--s3); border-radius: var(--r-md); color: var(--text-2); text-decoration: none; }
    a.activo, a:hover { background: var(--g100); color: var(--text); }
    .contenido { padding: var(--s5); min-width: 0; }
    @media (max-width: 40rem) { .marco { grid-template-columns: 1fr; } nav { border-right: 0; border-bottom: var(--borde-fino) solid var(--border); } }
  `,
})
export class ShellSistemas {
  protected readonly t = textosSistemas.menu
}
