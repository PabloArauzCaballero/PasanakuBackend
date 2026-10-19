import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core'
import { RouterLink } from '@angular/router'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { ExplicadorDeVista } from '../../nucleo/explicador-de-vista'
import { Sesion } from '../../nucleo/sesion'
import { textosConfiguracion } from './textos'

/** La entrada de configuración: a qué listado de solo lectura puede entrar el rol (los mismos permisos que su ruta). */
@Component({
  selector: 'ap-pantalla-de-configuracion',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CabeceraDePagina, RouterLink],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina [titulo]="t.titulo" [descripcion]="t.proposito" icono="candado" />
      @if (secciones().length === 0) {
        <p role="status" class="vacio">{{ t.sinSecciones }}</p>
      } @else {
        <ul class="accesos">
          @for (s of secciones(); track s.ruta) {
            <li>
              <a [routerLink]="s.ruta" [attr.data-seccion]="s.ruta">
                <span class="texto">{{ s.texto }}</span>
                <span class="descripcion">{{ s.descripcion }}</span>
              </a>
            </li>
          }
        </ul>
      }
    </section>
  `,
  styles: `
    .pantalla { padding: var(--s5); display: grid; gap: var(--s4); }
    .vacio { margin: 0; color: var(--text-2); }
    .accesos { list-style: none; margin: 0; padding: 0; display: grid; grid-template-columns: repeat(auto-fill, minmax(16rem, 1fr)); gap: var(--s4); }
    a { display: block; min-height: var(--area-tactil); padding: var(--s4); border: var(--borde-fino) solid var(--border); border-radius: var(--r-lg); background: var(--surface); text-decoration: none; }
    a:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
    .texto { display: block; font-weight: 600; color: var(--text); }
    .descripcion { display: block; margin-top: var(--s1); color: var(--text-2); font-size: .9em; }
  `,
})
export class PantallaDeConfiguracion {
  private readonly sesion = inject(Sesion)
  protected readonly t = textosConfiguracion.inicio
  protected readonly secciones = computed(() => this.t.secciones.filter((s) => s.permisos.some((p) => this.sesion.puede(p))))

  constructor() {
    inject(ExplicadorDeVista).fijar(this.t.proposito)
  }
}
