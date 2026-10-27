import { ChangeDetectionStrategy, Component, ElementRef, afterRenderEffect, computed, inject, signal, viewChild } from '@angular/core'
import { RouterLink } from '@angular/router'
import { Icono } from '@aportaya/ui/icono/icono'
import { entradaDe, pantallaActiva, pantallasVisibles } from '../nucleo/navegacion'
import { Sesion } from '../nucleo/sesion'
import { UrlActual } from './url-actual'

/**
 * Las pantallas de la sección actual, como las pestañas de sección del portal interno de Atlas (`section-tabs.tsx`):
 * el menú lateral lleva una entrada por sección y las pantallas de cada una viven acá, arriba del contenido.
 *
 * Solo se dibujan las pestañas que la sesión puede abrir (las mismas guardias que la ruta, escritas en `menu.ts`):
 * una pestaña que rebota al tablero es un botón que no funciona. Una pestaña de grupo («Integraciones») despliega sus
 * pantallas en una segunda fila. Cada pestaña lleva ícono y texto; la activa, una línea además del color (95.4.6).
 */
@Component({
  selector: 'ap-pestanas-de-seccion',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, Icono],
  template: `
    @if (seccion(); as s) {
      @if (pantallas().length) {
        <nav #carril [attr.aria-label]="'Pantallas de ' + s.texto" [class.mas-izq]="masIzq()" [class.mas-der]="masDer()" (scroll)="medir(carril)">
          <ul>
            @for (p of pantallas(); track p.texto) {
              <li>
                <a [routerLink]="p.ruta" [class.activa]="p.texto === activa()?.pantalla?.texto" [attr.aria-current]="p.texto === activa()?.pantalla?.texto && !p.grupo ? 'page' : null"
                  ><ap-icono [nombre]="p.icono" tamano="s4" />{{ p.texto }}</a
                >
              </li>
            }
          </ul>
        </nav>
      }
      @if (subpantallas().length > 1) {
        <nav class="sub" [attr.aria-label]="'Pantallas de ' + activa()!.pantalla.texto">
          <ul>
            @for (h of subpantallas(); track h.ruta) {
              <li>
                <a [routerLink]="h.ruta" [class.activa]="h.ruta === activa()?.hoja?.ruta" [attr.aria-current]="h.ruta === activa()?.hoja?.ruta ? 'page' : null"
                  ><ap-icono [nombre]="h.icono" tamano="s4" />{{ h.texto }}</a
                >
              </li>
            }
          </ul>
        </nav>
      }
    }
  `,
  styles: `
    :host { display: grid; gap: var(--s2); min-width: 0; }
    nav { border-bottom: var(--borde-fino) solid var(--border); overflow-x: auto; scrollbar-width: none; scroll-behavior: smooth; }
    /* Si hay pestañas fuera de la vista, el borde se desvanece de ese lado: se ve que el carril sigue (antes cortaba «Adm…» en seco). */
    nav.mas-der { mask-image: linear-gradient(to right, var(--n900) calc(100% - var(--s7)), transparent); }
    nav.mas-izq { mask-image: linear-gradient(to left, var(--n900) calc(100% - var(--s7)), transparent); }
    nav.mas-izq.mas-der { mask-image: linear-gradient(to right, transparent, var(--n900) var(--s7), var(--n900) calc(100% - var(--s7)), transparent); }
    nav::-webkit-scrollbar { display: none; }
    ul { list-style: none; display: flex; gap: var(--s1); margin: 0; padding: 0; }
    a {
      position: relative; display: inline-flex; align-items: center; gap: var(--s2); min-height: var(--area-tactil); padding: 0 var(--s3);
      color: var(--text-2); font: var(--t-cuerpo-chico); font-weight: 500; text-decoration: none; white-space: nowrap;
      border-radius: var(--r-sm) var(--r-sm) 0 0; transition: color var(--mov-micro) ease-out, background-color var(--mov-micro) ease-out;
    }
    a:hover { color: var(--text); background: var(--surface-2); }
    a.activa { color: var(--text); font-weight: 600; }
    a ap-icono { color: var(--text-3); }
    a.activa ap-icono { color: var(--brand); }
    /* La marca de la pestaña activa es una línea, no solo un color (95.4.6). */
    a.activa::after { content: ''; position: absolute; left: var(--s2); right: var(--s2); bottom: calc(var(--borde-fino) * -1); height: var(--borde-foco); border-radius: var(--r-pill); background: var(--brand); }
    a:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: calc(var(--borde-desfase) * -1); }
    /* La segunda fila: subpestañas en píldora, sin línea inferior, para que no se lea como otra sección. */
    .sub { border-bottom: 0; }
    .sub ul { gap: var(--s2); }
    .sub a { min-height: calc(var(--area-tactil) - var(--s2)); border-radius: var(--r-pill); border: var(--borde-fino) solid var(--border); font: var(--t-cuerpo-chico); }
    .sub a.activa { background: var(--surface-2); border-color: var(--brand); }
    .sub a.activa::after { content: none; }
    @media (prefers-reduced-motion: reduce) { a { transition: none; } }
  `,
})
export class PestanasDeSeccion {
  private readonly sesion = inject(Sesion)
  private readonly url = inject(UrlActual).url

  protected readonly seccion = computed(() => {
    const e = entradaDe(this.url())
    return e?.pantallas?.length ? e : undefined
  })
  protected readonly pantallas = computed(() => {
    const e = this.seccion()
    return e ? pantallasVisibles(e, this.sesion) : []
  })
  protected readonly activa = computed(() => {
    const e = this.seccion()
    if (!e) return undefined
    const a = pantallaActiva(e, this.url())
    // La pestaña visible que corresponde (su grupo ya viene filtrado a lo que la sesión puede abrir).
    return a ? { hoja: a.hoja, pantalla: this.pantallas().find((p) => p.texto === a.pantalla.texto) ?? a.pantalla } : undefined
  })
  protected readonly subpantallas = computed(() => this.activa()?.pantalla.grupo ?? [])
  protected readonly masIzq = signal(false)
  protected readonly masDer = signal(false)
  private readonly carril = viewChild<ElementRef<HTMLElement>>('carril')

  constructor() {
    // Después de pintar: la pestaña activa queda a la vista aunque esté al final del carril, y se mide si sobra carril.
    afterRenderEffect(() => {
      this.activa()
      const nav = this.carril()?.nativeElement
      if (!nav) return
      nav.querySelector<HTMLElement>('a.activa')?.scrollIntoView?.({ block: 'nearest', inline: 'nearest' })
      this.medir(nav)
    })
  }

  protected medir(nav: HTMLElement): void {
    this.masIzq.set(nav.scrollLeft > 1)
    this.masDer.set(nav.scrollLeft + nav.clientWidth < nav.scrollWidth - 1)
  }
}
