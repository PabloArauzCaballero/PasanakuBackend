import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core'
import { toSignal } from '@angular/core/rxjs-interop'
import { NavigationEnd, Router, RouterLink } from '@angular/router'
import { filter, map, startWith } from 'rxjs'
import { entradaDe } from '../nucleo/menu'

/**
 * Las pantallas de la sección actual, como las pestañas de sección del portal interno de Atlas (`section-tabs.tsx`):
 * el menú lateral lleva una entrada por sección y las pantallas de cada una viven acá, arriba del contenido. Antes el
 * menú desplegaba hasta catorce pantallas por sección y la barra lateral pasaba los cuarenta enlaces.
 *
 * Una sección sin pantallas no muestra nada. La pestaña activa es la de ruta más larga que prefija la URL.
 */
@Component({
  selector: 'ap-pestanas-de-seccion',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink],
  template: `
    @if (seccion(); as s) {
      <nav [attr.aria-label]="'Pantallas de ' + s.texto">
        <ul>
          @for (p of pantallas(); track p.ruta) {
            <li>
              <a [routerLink]="p.ruta" [class.activa]="p.ruta === activa()" [attr.aria-current]="p.ruta === activa() ? 'page' : null">{{ p.texto }}</a>
            </li>
          }
        </ul>
      </nav>
    }
  `,
  styles: `
    :host { display: block; min-width: 0; }
    nav { border-bottom: var(--borde-fino) solid var(--border); overflow-x: auto; scrollbar-width: none; }
    nav::-webkit-scrollbar { display: none; }
    ul { list-style: none; display: flex; gap: var(--s1); margin: 0; padding: 0; }
    a {
      position: relative; display: inline-flex; align-items: center; min-height: var(--area-tactil); padding: 0 var(--s3);
      color: var(--text-2); font: var(--t-etiqueta); letter-spacing: var(--t-etiqueta-track); text-decoration: none; white-space: nowrap;
      border-radius: var(--r-sm) var(--r-sm) 0 0; transition: color var(--mov-micro) ease-out, background-color var(--mov-micro) ease-out;
    }
    a:hover { color: var(--text); background: var(--surface-2); }
    a.activa { color: var(--text); font-weight: 600; }
    /* La marca de la pestaña activa es una línea, no solo un color (95.4.6). */
    a.activa::after { content: ''; position: absolute; left: var(--s2); right: var(--s2); bottom: calc(var(--borde-fino) * -1); height: var(--borde-foco); border-radius: var(--r-pill); background: var(--brand); }
    a:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: calc(var(--borde-desfase) * -1); }
  `,
})
export class PestanasDeSeccion {
  private readonly router = inject(Router)
  private readonly url = toSignal(
    this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd),
      map((e) => e.urlAfterRedirects),
      startWith(this.router.url),
    ),
    { initialValue: this.router.url },
  )

  protected readonly seccion = computed(() => {
    const e = entradaDe(this.url())
    return e?.hijos?.length ? e : undefined
  })
  protected readonly pantallas = computed(() => {
    const e = this.seccion()
    return (e?.hijos ?? []).map((h) => ({ texto: h.texto, ruta: h.ruta ? `${e!.destino}/${h.ruta}` : e!.destino }))
  })
  protected readonly activa = computed(() => {
    const ruta = this.url().split('?')[0]!
    return this.pantallas()
      .map((p) => p.ruta)
      .filter((r) => ruta === r || ruta.startsWith(r + '/'))
      .sort((a, b) => b.length - a.length)[0]
  })
}
