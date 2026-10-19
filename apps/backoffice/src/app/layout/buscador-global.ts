import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core'
import { Router } from '@angular/router'
import { Icono } from '@aportaya/ui/icono/icono'
import { IndiceDeBusqueda, type Resultado } from '../nucleo/indice-de-busqueda'

const CLAVE = 'ap-buscador:recientes'

/**
 * El buscador global de la cabecera: un combobox sobre pantallas y tablas (`IndiceDeBusqueda`).
 * Flechas para moverse, Enter abre, Escape cierra; `⌘K` / `Ctrl+K` lo enfoca desde cualquier
 * lado. Sin consulta muestra las búsquedas recientes de la sesión.
 */
@Component({
  selector: 'ap-buscador-global',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Icono],
  host: { '(document:keydown)': 'atajo($event)' },
  template: `
    <div class="caja" [class.abierto]="abierto()">
      <ap-icono nombre="buscar" tamano="s4" class="lupa" />
      <input
        #entrada
        type="search"
        role="combobox"
        aria-label="Buscar en el backoffice"
        placeholder="Buscar pantalla o tabla…"
        [attr.aria-expanded]="abierto()"
        [attr.aria-controls]="id"
        [attr.aria-activedescendant]="activo() >= 0 ? id + '-' + activo() : null"
        [value]="consulta()"
        (input)="escribir($any($event.target).value)"
        (focus)="abierto.set(true)"
        (blur)="cerrarLuego()"
        (keydown)="teclas($event)"
      />
      <kbd aria-hidden="true">⌘K</kbd>
    </div>
    @if (abierto()) {
      <ul [id]="id" role="listbox" aria-label="Resultados de la búsqueda">
        @if (consulta().length < 2) {
          @for (r of recientes(); track r; let i = $index) {
            <li role="option" [id]="id + '-' + i" [attr.aria-selected]="activo() === i" (mousedown)="$event.preventDefault(); escribir(r)"><ap-icono nombre="reloj" tamano="s4" />{{ r }}</li>
          } @empty { <li class="nada">Escribí el nombre de una pantalla o de una tabla.</li> }
        } @else {
          @for (r of resultados(); track r.ruta; let i = $index) {
            <li role="option" [id]="id + '-' + i" [attr.aria-selected]="activo() === i" (mousedown)="$event.preventDefault(); abrir(r)">
              <span class="texto">{{ r.texto }}</span><span class="detalle">{{ r.detalle }}</span>
            </li>
          } @empty { <li class="nada">Nada coincide con «{{ consulta() }}».</li> }
        }
      </ul>
    }
  `,
  styles: `
    :host { position: relative; display: block; width: 100%; max-width: calc(var(--s7) * 8); }
    .caja { display: flex; align-items: center; gap: var(--s2); min-height: calc(var(--area-tactil) - var(--s1)); padding: 0 var(--s3); border: var(--borde-fino) solid var(--border); border-radius: var(--r-md); background: var(--surface-2); color: var(--text-3); transition: background-color var(--mov-micro) ease-out, border-color var(--mov-micro) ease-out; }
    .caja:focus-within { background: var(--surface); border-color: var(--brand); box-shadow: 0 0 0 var(--borde-foco) var(--g300); }
    input { flex: 1; min-width: 0; border: 0; background: transparent; color: var(--text); font: inherit; outline: none; }
    input::-webkit-search-cancel-button { display: none; }
    kbd { padding: 0 var(--s1); border: var(--borde-fino) solid var(--border); border-radius: var(--r-sm); font: var(--t-ayuda); color: var(--text-3); }
    ul { position: absolute; z-index: 30; inset: calc(100% + var(--s1)) 0 auto 0; margin: 0; padding: var(--s1); list-style: none; border: var(--borde-fino) solid var(--border); border-radius: var(--r-lg); background: var(--surface); box-shadow: var(--sombra-3); }
    li { display: flex; align-items: center; gap: var(--s2); min-height: var(--area-tactil); padding: var(--s1) var(--s3); border-radius: var(--r-md); cursor: pointer; color: var(--text); }
    li[aria-selected='true'], li:hover { background: var(--surface-2); }
    .texto { font-weight: 600; } .detalle { margin-left: auto; font: var(--t-ayuda); color: var(--text-3); }
    .nada { color: var(--text-3); cursor: default; font: var(--t-cuerpo-chico); }
    @media (max-width: 48rem) { kbd { display: none; } }
  `,
})
export class BuscadorGlobal {
  private static secuencia = 0
  readonly id = `ap-buscador-${++BuscadorGlobal.secuencia}`
  private readonly indice = inject(IndiceDeBusqueda)
  private readonly router = inject(Router)
  readonly consulta = signal('')
  readonly abierto = signal(false)
  readonly activo = signal(-1)
  readonly recientes = signal<string[]>(leer())
  readonly resultados = computed(() => this.indice.buscar(this.consulta()))
  private temporizador: ReturnType<typeof setTimeout> | undefined

  escribir(v: string): void {
    this.consulta.set(v)
    this.activo.set(-1)
    this.abierto.set(true)
  }
  abrir(r: Resultado): void {
    this.recordar(this.consulta())
    this.consulta.set('')
    this.abierto.set(false)
    void this.router.navigateByUrl(r.ruta)
  }
  teclas(e: KeyboardEvent): void {
    const n = this.consulta().length < 2 ? this.recientes().length : this.resultados().length
    if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
      e.preventDefault()
      this.abierto.set(true)
      if (n === 0) return
      // Sin activo, ↓ va al primero y ↑ al último; después recorre en círculo.
      const desde = this.activo() < 0 ? (e.key === 'ArrowDown' ? -1 : 0) : this.activo()
      this.activo.set((desde + (e.key === 'ArrowDown' ? 1 : -1) + n) % n)
    } else if (e.key === 'Enter') {
      const i = this.activo() < 0 ? 0 : this.activo()
      if (this.consulta().length < 2) {
        const r = this.recientes()[i]
        if (r) this.escribir(r)
      } else {
        const r = this.resultados()[i]
        if (r) this.abrir(r)
      }
    } else if (e.key === 'Escape') {
      this.abierto.set(false)
      ;(e.target as HTMLInputElement).blur()
    }
  }
  atajo(e: KeyboardEvent): void {
    if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'k') {
      e.preventDefault()
      ;(document.querySelector('ap-buscador-global input') as HTMLInputElement | null)?.focus()
    }
  }
  /** Al perder el foco se cierra después de que un clic en la lista haya podido ejecutarse. */
  cerrarLuego(): void {
    clearTimeout(this.temporizador)
    this.temporizador = setTimeout(() => this.abierto.set(false), 120)
  }
  private recordar(q: string): void {
    const lista = [q, ...this.recientes().filter((x) => x !== q)].slice(0, 5)
    this.recientes.set(lista)
    try {
      sessionStorage.setItem(CLAVE, JSON.stringify(lista))
    } catch {
      // sin almacenamiento, sin recientes
    }
  }
}

function leer(): string[] {
  try {
    return JSON.parse(sessionStorage.getItem(CLAVE) ?? '[]') as string[]
  } catch {
    return []
  }
}
