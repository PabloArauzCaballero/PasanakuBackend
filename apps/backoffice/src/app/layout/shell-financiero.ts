import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core'
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router'
import { finalize } from 'rxjs'
import { Sesion } from '../nucleo/sesion'
import { EncendidoDeTutorial } from '@aportaya/tutoriales/encendido'
import { AnfitrionDeTutorial } from '@aportaya/tutoriales/anfitrion-de-tutorial'
import { LanzadorDeTutorial } from '@aportaya/tutoriales/lanzador-de-tutorial'
import { crearCerrarSesion, rolLegible } from './dominio/cerrar-sesion'
import { IconoDeSeccion, type Seccion as NombreDeSeccion } from './icono-de-seccion'
import { SimboloDeMarca } from './simbolo-de-marca'

/** `destino` cuando la ruta no es `/<ruta>` (el catálogo vive bajo el tablero). */
type Seccion = { ruta: NombreDeSeccion; texto: string; permiso: string; destino?: string }
type Grupo = { titulo: string | null; secciones: Seccion[] }

/**
 * El shell «financiero»: barra lateral con la marca y los dominios agrupados, cabecera
 * con el rol de quien opera, y el `router-outlet` de las pantallas. Misma familia visual
 * que el ingreso (panel oscuro de marca, contenido claro).
 *
 * **El menú oculta por comodidad**; el permiso real lo exige `canMatch` en cada ruta y lo
 * verifica el servidor — este componente no es la barrera de seguridad, solo evita
 * ofrecer un enlace que va a terminar en un 403.
 */
@Component({
  selector: 'ap-shell-financiero',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, LanzadorDeTutorial, AnfitrionDeTutorial, IconoDeSeccion, SimboloDeMarca],
  template: `
    <div class="shell">
      <aside class="lateral">
        <a class="marca" routerLink="/tablero">
          <ap-simbolo-de-marca [sobreOscuro]="true" />
          <span><strong>AportaYa</strong><small>Backoffice</small></span>
        </a>
        <nav class="menu" data-tutorial-id="menu-principal" aria-label="Secciones del backoffice">
          @for (g of grupos; track g.titulo) {
            @if (visibles(g).length) {
              @if (g.titulo) { <p class="grupo">{{ g.titulo }}</p> }
              <ul>
                @for (s of visibles(g); track s.ruta) {
                  <li>
                    <a [routerLink]="s.destino ?? ['/', s.ruta]" [attr.data-tutorial-id]="'menu-' + s.ruta" routerLinkActive="activo" ariaCurrentWhenActive="page">
                      <ap-icono-de-seccion [nombre]="s.ruta" />{{ s.texto }}
                    </a>
                  </li>
                }
              </ul>
            }
          }
        </nav>
        <button type="button" class="salir" (click)="salir()" [disabled]="saliendo()">
          <ap-icono-de-seccion nombre="salir" />{{ saliendo() ? 'Saliendo…' : 'Salir' }}
        </button>
      </aside>
      <header class="cabecera">
        @defer (on idle) {
          <ap-lanzador-de-tutorial data-tutorial-id="lanzador-de-tutorial" />
        }
        @if (rol(); as r) {
          <p class="rol" data-tutorial-id="cabecera-rol"><span class="punto" aria-hidden="true"></span>{{ r }}</p>
        }
      </header>
      <main id="contenido">
        <router-outlet />
      </main>
      <!--
        El anfitrión del tutorial (velo, globo, teclado) se trae recién cuando alguien
        enciende un recorrido: el shell se monta en todas las pantallas y nadie debería
        pagar el peso de la ayuda para mirar un saldo.
      -->
      @defer (when tutorial.activo()) {
        <ap-anfitrion-de-tutorial />
      }
    </div>
  `,
  styles: `
    .shell { display: grid; grid-template-columns: 16rem minmax(0, 1fr); grid-template-rows: auto 1fr; grid-template-areas: 'lateral cabecera' 'lateral contenido'; min-height: 100dvh; background: var(--bg); }
    .lateral {
      grid-area: lateral; position: sticky; top: 0; height: 100dvh; display: flex; flex-direction: column; gap: var(--s4); padding: var(--s5) var(--s3);
      color: var(--white); overflow-y: auto;
      background: radial-gradient(circle at 0% 0%, color-mix(in srgb, var(--g400) 22%, transparent), transparent 45%), linear-gradient(180deg, var(--g800), var(--g900));
    }
    .marca { display: flex; align-items: center; gap: var(--s3); padding: 0 var(--s2) var(--s2); color: inherit; text-decoration: none; }
    .marca span { display: grid; line-height: 1.2; } .marca strong { font: var(--t-cuerpo-fuerte); letter-spacing: .06em; text-transform: uppercase; }
    .marca small { font: var(--t-cuerpo-chico); color: color-mix(in srgb, var(--white) 65%, transparent); }
    .menu { flex: 1; display: grid; align-content: start; gap: var(--s1); }
    .grupo { margin: var(--s3) 0 var(--s1); padding: 0 var(--s3); font: var(--t-etiqueta); letter-spacing: .1em; text-transform: uppercase; color: color-mix(in srgb, var(--white) 50%, transparent); }
    ul { list-style: none; margin: 0; padding: 0; display: grid; gap: var(--s1); }
    .menu a, .salir {
      display: flex; align-items: center; gap: var(--s3); width: 100%; min-height: var(--area-tactil); padding: var(--s2) var(--s3); border: 0; border-radius: var(--r-md);
      font: var(--t-cuerpo); color: color-mix(in srgb, var(--white) 78%, transparent); background: transparent; text-decoration: none; cursor: pointer;
      transition: background-color var(--mov-micro) ease-out, color var(--mov-micro) ease-out;
    }
    .menu a:hover, .salir:hover:not(:disabled) { background: color-mix(in srgb, var(--white) 8%, transparent); color: var(--white); }
    .menu a.activo { background: color-mix(in srgb, var(--white) 14%, transparent); color: var(--white); font-weight: 600; box-shadow: inset var(--borde-desfase) 0 0 var(--g300); }
    .menu a:focus-visible, .salir:focus-visible, .marca:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
    .salir { border-top: var(--borde-fino) solid color-mix(in srgb, var(--white) 12%, transparent); border-radius: 0; padding-top: var(--s3); }
    .salir:disabled { cursor: progress; opacity: .7; }
    .cabecera { grid-area: cabecera; display: flex; flex-wrap: wrap; align-items: center; justify-content: flex-end; gap: var(--s2) var(--s3); padding: var(--s3) var(--s5); border-bottom: var(--borde-fino) solid var(--border); background: var(--surface); }
    .rol { display: inline-flex; align-items: center; gap: var(--s2); margin: 0; padding: var(--s1) var(--s3); border-radius: var(--r-pill); background: var(--brand-bg); color: var(--text); font: var(--t-cuerpo-chico); overflow-wrap: anywhere; }
    .punto { width: .5rem; height: .5rem; border-radius: 50%; background: var(--ok); }
    main { grid-area: contenido; min-width: 0; }
    @media (max-width: 40rem) {
      .shell { grid-template-columns: minmax(0, 1fr); grid-template-rows: auto auto 1fr; grid-template-areas: 'lateral' 'cabecera' 'contenido'; }
      /* Una sola fila que se desliza: el menú no puede comerse media pantalla antes del contenido. */
      .lateral { position: static; height: auto; display: grid; grid-template-columns: minmax(0, 1fr) auto; gap: var(--s2); padding: var(--s3); min-width: 0; }
      /* min-width 0: sin esto la fila mide lo que todos sus enlaces juntos y empuja la página (visto en Linux, 14 px a 360). */
      .menu { grid-column: 1 / -1; grid-row: 2; display: flex; min-width: 0; overflow-x: auto; scrollbar-width: none; }
      .grupo { display: none; } ul { display: flex; flex-wrap: nowrap; } .menu a { white-space: nowrap; }
      .salir { grid-column: 2; grid-row: 1; width: auto; border-top: 0; padding-top: var(--s2); }
    }
  `,
})
export class ShellFinanciero {
  protected readonly sesion = inject(Sesion)
  protected readonly tutorial = inject(EncendidoDeTutorial)
  private readonly router = inject(Router)
  private readonly cerrarSesion = crearCerrarSesion()
  protected readonly saliendo = signal(false)
  protected readonly rol = computed(() => {
    const r = this.sesion.rol()
    return r ? rolLegible(r) : null
  })
  protected readonly grupos: Grupo[] = [
    { titulo: null, secciones: [{ ruta: 'tablero', texto: 'Tablero', permiso: 'ver:tablero' }] },
    { titulo: 'Operación', secciones: [
      { ruta: 'operacion', texto: 'Operación', permiso: 'ver:operacion' },
      { ruta: 'admisiones', texto: 'Admisiones', permiso: 'ver:admisiones' },
    ] },
    { titulo: 'Control', secciones: [
      { ruta: 'cumplimiento', texto: 'Cumplimiento', permiso: 'ver:cumplimiento' },
      { ruta: 'sistemas', texto: 'Sistemas', permiso: 'ver:sistemas' },
      { ruta: 'catalogo', destino: '/tablero/catalogo-de-datos', texto: 'Catálogo de datos', permiso: 'ver:catalogo' },
    ] },
    { titulo: 'Finanzas', secciones: [
      { ruta: 'contabilidad', texto: 'Contabilidad', permiso: 'ver:contabilidad' },
      { ruta: 'publicidad', texto: 'Publicidad', permiso: 'ver:publicidad' },
    ] },
    // Última a propósito: es la sección a la que se vuelve, no por la que se empieza.
    { titulo: 'Plataforma', secciones: [
      { ruta: 'configuracion', texto: 'Configuración', permiso: 'ver:configuracion' },
      { ruta: 'ayuda', texto: 'Ayuda', permiso: 'ver:ayuda' },
    ] },
  ]

  protected visibles(g: Grupo): Seccion[] {
    return g.secciones.filter((s) => this.sesion.puede(s.permiso))
  }

  /** La sesión se cierra en el navegador aunque el servidor no conteste: quien pide salir, sale. */
  protected salir(): void {
    if (this.saliendo()) return
    this.saliendo.set(true)
    this.cerrarSesion()
      .pipe(finalize(() => {
        this.sesion.cerrar()
        void this.router.navigateByUrl('/ingreso')
      }))
      .subscribe({ error: () => undefined })
  }
}
