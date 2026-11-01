import { ChangeDetectionStrategy, Component, ElementRef, afterRenderEffect, computed, inject, input, output, signal } from '@angular/core'
import { RouterLink } from '@angular/router'
import { Icono } from '@aportaya/ui/icono/icono'
import { Sesion } from '../nucleo/sesion'
import { IconoDeSeccion } from './icono-de-seccion'
import { rolLegible } from './dominio/cerrar-sesion'

/**
 * El menú de quien opera, en la cabecera: iniciales en un círculo, el rol en texto legible, y un
 * desplegable con el centro de ayuda y Salir, cada uno con ícono y texto. Al abrirlo el foco va a la
 * primera opción; flechas, Inicio y Fin recorren; Escape cierra y devuelve el foco al disparador; el clic afuera cierra.
 *
 * «Mi perfil» salió (2026-10-26): llevaba a `/configuracion`, que no es un perfil y rebota al tablero a quien no tiene
 * esa sección. No hay pantalla de perfil con ruta; cuando exista, vuelve acá. Ayuda vino del menú lateral (A1).
 */
@Component({
  selector: 'ap-menu-de-usuario',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, Icono, IconoDeSeccion],
  host: { '(document:click)': 'clicAfuera($event)', '(keydown)': 'teclas($event)' },
  template: `
    <button type="button" class="disparador" [attr.aria-expanded]="abierto()" [attr.aria-controls]="id" aria-haspopup="menu" (click)="abierto.set(!abierto())">
      <span class="avatar" aria-hidden="true">{{ iniciales() }}</span>
      <span class="rol" data-tutorial-id="cabecera-rol">{{ rol() }}</span>
      <ap-icono nombre="chevronAbajo" tamano="s4" class="chevron" />
    </button>
    @if (abierto()) {
      <ul [id]="id" role="menu" aria-label="Cuenta">
        <li role="none" class="quien"><ap-icono nombre="persona" tamano="s4" />{{ rol() }}</li>
        <li role="none"><a role="menuitem" routerLink="/ayuda" data-tutorial-id="menu-ayuda" (click)="abierto.set(false)"><ap-icono nombre="libro" tamano="s4" />Centro de ayuda</a></li>
        <li role="none"><button role="menuitem" type="button" [disabled]="saliendo()" (click)="salir.emit()"><ap-icono-de-seccion nombre="salir" />{{ saliendo() ? 'Saliendo…' : 'Salir' }}</button></li>
      </ul>
    }
  `,
  styles: `
    :host { position: relative; display: inline-block; }
    .disparador { display: inline-flex; align-items: center; gap: var(--s2); min-height: var(--area-tactil); padding: 0 var(--s2) 0 var(--s1); border: var(--borde-fino) solid transparent; border-radius: var(--r-pill); background: transparent; color: var(--text); font: inherit; cursor: pointer; }
    .disparador:hover { background: var(--surface-2); }
    .disparador:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
    .avatar { display: inline-flex; align-items: center; justify-content: center; width: var(--s6); height: var(--s6); border-radius: var(--r-pill); background: var(--brand); color: var(--accent-ink); font: var(--t-etiqueta); }
    .rol { font: var(--t-cuerpo-chico); }
    .chevron { color: var(--text-3); }
    ul { position: absolute; right: 0; top: calc(100% + var(--s1)); z-index: 30; min-width: calc(var(--s7) * 4); margin: 0; padding: var(--s1); list-style: none; border: var(--borde-fino) solid var(--border); border-radius: var(--r-lg); background: var(--surface); box-shadow: var(--sombra-3); }
    a, li button { display: flex; width: 100%; align-items: center; gap: var(--s2); min-height: var(--area-tactil); padding: 0 var(--s3); border: 0; border-radius: var(--r-md); background: transparent; color: var(--text); font: inherit; text-align: left; text-decoration: none; cursor: pointer; }
    a:hover, li button:hover:not(:disabled) { background: var(--surface-2); }
    a:focus-visible, li button:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: calc(var(--borde-desfase) * -1); }
    li button:disabled { cursor: progress; opacity: .7; }
    .quien { display: flex; align-items: center; gap: var(--s2); min-height: var(--area-tactil); padding: 0 var(--s3); color: var(--text-3); font: var(--t-cuerpo-chico); border-bottom: var(--borde-fino) solid var(--border); margin-bottom: var(--s1); }
    @media (max-width: 40rem) { .rol { display: none; } }
  `,
})
export class MenuDeUsuario {
  private static secuencia = 0
  readonly id = `ap-menu-usuario-${++MenuDeUsuario.secuencia}`
  private readonly sesion = inject(Sesion)
  readonly saliendo = input(false)
  readonly salir = output<void>()
  readonly abierto = signal(false)
  private readonly el = inject<ElementRef<HTMLElement>>(ElementRef)
  readonly rol = computed(() => {
    const r = this.sesion.rol()
    return r ? rolLegible(r) : 'Operador'
  })
  readonly iniciales = computed(() =>
    this.rol()
      .split(/\s+/)
      .slice(0, 2)
      .map((p) => p[0]?.toUpperCase() ?? '')
      .join(''),
  )

  constructor() {
    // Abrir con clic o teclado deja el foco en la primera opción (patrón de menú de WAI-ARIA).
    afterRenderEffect(() => {
      if (this.abierto()) this.opciones()[0]?.focus()
    })
  }

  private opciones(): HTMLElement[] {
    return Array.from(this.el.nativeElement.querySelectorAll<HTMLElement>('[role="menuitem"]:not([disabled])'))
  }

  teclas(e: KeyboardEvent): void {
    if (!this.abierto()) return
    if (e.key === 'Escape') {
      this.abierto.set(false)
      this.el.nativeElement.querySelector<HTMLElement>('.disparador')?.focus()
      return
    }
    const ops = this.opciones()
    const i = ops.indexOf(document.activeElement as HTMLElement)
    const destino = { ArrowDown: (i + 1) % ops.length, ArrowUp: (i - 1 + ops.length) % ops.length, Home: 0, End: ops.length - 1 }[e.key]
    if (destino === undefined || !ops.length) return
    e.preventDefault()
    ops[destino]?.focus()
  }

  clicAfuera(e: Event): void {
    if (this.abierto() && !(e.target instanceof Node && this.el.nativeElement.contains(e.target))) this.abierto.set(false)
  }
}
