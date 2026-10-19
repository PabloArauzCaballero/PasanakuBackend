import { ChangeDetectionStrategy, Component } from '@angular/core'
import { RouterOutlet } from '@angular/router'
import { BannerDatosDeEjemplo } from '../../rutas/sistemas/banner-datos-de-ejemplo/banner-datos-de-ejemplo'

/**
 * El marco del backoffice de sistemas: el banner de datos de ejemplo (es del shell, no de cada
 * pantalla, así se ve en las nueve sin que cada una se acuerde) y el `router-outlet`.
 *
 * Hasta el plan backoffice-como-atlas (H2) tenía además su propio menú lateral con las nueve
 * pantallas. Ese menú se retiró: el menú principal muestra las pantallas de la sección activa
 * (`layout/menu-lateral.ts`), y dos navegaciones iguales una al lado de la otra confundían —y la
 * de acá, con `--g100` de fondo, era ilegible en modo oscuro—. El guard de rol sigue en la ruta.
 */
@Component({
  selector: 'ap-shell-sistemas',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, BannerDatosDeEjemplo],
  template: `
    <ap-banner-datos-de-ejemplo />
    <router-outlet />
  `,
  styles: `:host { display: grid; gap: var(--s4); }`,
})
export class ShellSistemas {}
