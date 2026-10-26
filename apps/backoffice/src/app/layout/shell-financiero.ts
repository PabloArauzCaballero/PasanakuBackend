import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core'
import { Title } from '@angular/platform-browser'
import { Router, RouterLink, RouterOutlet } from '@angular/router'
import { finalize } from 'rxjs'
import { CajonLateral } from '@aportaya/ui/cajon-lateral/cajon-lateral'
import { Icono } from '@aportaya/ui/icono/icono'
import { Migas } from '@aportaya/ui/migas/migas'
import { NotaDeVista } from '@aportaya/ui/nota-de-vista/nota-de-vista'
import { EncendidoDeTutorial } from '@aportaya/tutoriales/encendido'
import { AnfitrionDeTutorial } from '@aportaya/tutoriales/anfitrion-de-tutorial'
import { LanzadorDeTutorial } from '@aportaya/tutoriales/lanzador-de-tutorial'
import { ExplicadorDeVista } from '../nucleo/explicador-de-vista'
import { migasDe } from '../nucleo/navegacion'
import { Sesion } from '../nucleo/sesion'
import { BuscadorGlobal } from './buscador-global'
import { CampanaDeNotificaciones } from './campana-de-notificaciones'
import { crearCerrarSesion } from './dominio/cerrar-sesion'
import { IconoDeSeccion } from './icono-de-seccion'
import { MenuDeUsuario } from './menu-de-usuario'
import { MenuLateral } from './menu-lateral'
import { PestanasDeSeccion } from './pestanas-de-seccion'
import { SimboloDeMarca } from './simbolo-de-marca'
import { UrlActual } from './url-actual'

// Por debajo del escritorio la barra lateral es un cajón, como en el portal de Atlas (lg): en tablet apretaba la cabecera.
const ANGOSTO = '(max-width: 64rem)'

/**
 * El shell del backoffice con la anatomía del portal interno de Atlas: barra lateral oscura con la
 * marca y el menú por grupos (`ap-menu-lateral`, una entrada por sección), cabecera fija con migas,
 * título de la vista, buscador global, lanzador del tutorial y menú de quien opera; abajo, las
 * pestañas de la sección, las «Notas de la vista» plegadas y el `router-outlet`. En el celular la barra es un cajón que abre la hamburguesa.
 *
 * **El menú oculta por comodidad**; el permiso real lo exige `canMatch` en cada ruta y lo verifica
 * el servidor — este componente no es la barrera de seguridad.
 */
@Component({
  selector: 'ap-shell-financiero',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, RouterLink, CajonLateral, Icono, Migas, NotaDeVista, LanzadorDeTutorial, AnfitrionDeTutorial, BuscadorGlobal, CampanaDeNotificaciones, IconoDeSeccion, MenuDeUsuario, MenuLateral, PestanasDeSeccion, SimboloDeMarca],
  template: `
    <div class="shell" [class.angosto]="angosto()">
      @if (!angosto()) {
        <aside class="lateral">
          <a class="marca" routerLink="/tablero"><ap-simbolo-de-marca [sobreOscuro]="true" /><span><strong>AportaYa</strong><small>Backoffice</small></span></a>
          <ap-menu-lateral class="menu" />
          <button type="button" class="salir" [disabled]="saliendo()" (click)="salir()"><ap-icono-de-seccion nombre="salir" />{{ saliendo() ? 'Saliendo…' : 'Salir' }}</button>
        </aside>
      } @else {
        <ap-cajon-lateral lado="izquierda" titulo="Secciones" [(abierto)]="menuAbierto">
          @if (menuAbierto()) { <ap-menu-lateral (navegado)="menuAbierto.set(false)" /> }
          <ap-lanzador-de-tutorial acciones data-tutorial-id="lanzador-de-tutorial" />
          <button acciones type="button" class="salir" [disabled]="saliendo()" (click)="salir()"><ap-icono-de-seccion nombre="salir" />{{ saliendo() ? 'Saliendo…' : 'Salir' }}</button>
        </ap-cajon-lateral>
      }
      <header class="cabecera">
        @if (angosto()) {
          <button type="button" class="hamburguesa" aria-label="Abrir el menú" (click)="menuAbierto.set(true)"><ap-icono nombre="menu" tamano="s5" /></button>
          <a class="marca-chica" routerLink="/tablero" aria-label="Inicio"><ap-simbolo-de-marca /></a>
        }
        <div class="vista">
          <!-- La última miga es la pantalla actual, que ya es el título de abajo: no se repite (antes se apilaban en tres líneas). -->
          @if (migas().length > 1) { <ap-migas [migas]="migas().slice(0, -1)" (navegar)="ir($event)" /> }
          <p class="titulo-vista">{{ tituloDeVista() }}</p>
        </div>
        <ap-buscador-global class="buscador" />
        <!-- En el celular el lanzador vive en el cajón del menú: la cabecera no tiene lugar para su texto. -->
        @if (!angosto()) { <ap-lanzador-de-tutorial data-tutorial-id="lanzador-de-tutorial" /> }
        <ap-campana-de-notificaciones />
        <ap-menu-de-usuario [saliendo]="saliendo()" (salir)="salir()" />
      </header>
      <main id="contenido">
        <ap-pestanas-de-seccion />
        @if (explicador.texto(); as texto) { <ap-nota-de-vista clave="vista">{{ texto }}</ap-nota-de-vista> }
        <router-outlet />
      </main>
      @defer (when tutorial.activo()) { <ap-anfitrion-de-tutorial /> }
    </div>
  `,
  styleUrl: './shell-financiero.css',
})
export class ShellFinanciero {
  protected readonly sesion = inject(Sesion)
  protected readonly tutorial = inject(EncendidoDeTutorial)
  protected readonly explicador = inject(ExplicadorDeVista)
  private readonly router = inject(Router)
  private readonly titulo = inject(Title)
  private readonly cerrarSesion = crearCerrarSesion()
  protected readonly saliendo = signal(false)
  protected readonly menuAbierto = signal(false)
  protected readonly angosto = signal(typeof matchMedia === 'function' && matchMedia(ANGOSTO).matches)
  private readonly url = inject(UrlActual).url
  protected readonly migas = computed(() => migasDe(this.url(), this.titulo.getTitle(), this.sesion))
  protected readonly tituloDeVista = computed(() => this.migas().at(-1)?.texto ?? this.titulo.getTitle().replace(/\s*·\s*AportaYa\s*$/, ''))

  constructor() {
    if (typeof matchMedia !== 'function') return
    const consulta = matchMedia(ANGOSTO)
    const alCambiar = (e: MediaQueryListEvent) => {
      this.angosto.set(e.matches)
      if (!e.matches) this.menuAbierto.set(false)
    }
    consulta.addEventListener('change', alCambiar)
    inject(DestroyRef).onDestroy(() => consulta.removeEventListener('change', alCambiar))
  }

  protected ir(ruta: string): void {
    void this.router.navigateByUrl(ruta)
  }

  /** La sesión se cierra en el navegador aunque el servidor no conteste: quien pide salir, sale. */
  protected salir(): void {
    if (this.saliendo()) return
    this.saliendo.set(true)
    this.cerrarSesion()
      .pipe(
        finalize(() => {
          this.sesion.cerrar()
          void this.router.navigateByUrl('/ingreso')
        }),
      )
      .subscribe({ error: () => undefined })
  }
}
