import { AfterViewInit, ChangeDetectionStrategy, Component, ElementRef, OnDestroy, effect, input, model, output, viewChild } from '@angular/core'
import { Icono } from '../icono/icono'

/**
 * Un panel que entra desde la derecha para una acción sobre una fila («Revisar columna X»), sin
 * abandonar el listado. Es un `<dialog>` modal: foco atrapado, `Escape` y el fondo cierran, y
 * el foco vuelve a quien lo abrió. Cabecera fija con el título y el botón de cerrar (con nombre);
 * el cuerpo se desplaza. En el celular ocupa todo el ancho. `lado="izquierda"` es el menú de
 * navegación del celular, que entra desde la izquierda.
 */
@Component({
  selector: 'ap-cajon-lateral',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Icono],
  host: { '[class.izquierda]': 'lado() === "izquierda"' },
  template: `
    <dialog #caja [attr.aria-labelledby]="id + '-titulo'" (close)="abierto.set(false)" (cancel)="$event.preventDefault(); cerrar()">
      <header>
        <h2 [id]="id + '-titulo'">{{ titulo() }}</h2>
        <button type="button" class="cerrar" aria-label="Cerrar el panel" (click)="cerrar()"><ap-icono nombre="cerrar" tamano="s4" /></button>
      </header>
      <div class="cuerpo"><ng-content /></div>
      <footer><ng-content select="[acciones]" /></footer>
    </dialog>
  `,
  styles: `
    :host(.izquierda) dialog { inset: 0 auto 0 0; border-left: 0; border-right: var(--borde-fino) solid var(--border); }
    :host(.izquierda) dialog[open] { animation-name: entrar-izquierda; }
    @keyframes entrar-izquierda { from { transform: translateX(calc(var(--s6) * -1)); opacity: 0; } }
    dialog { position: fixed; inset: 0 0 0 auto; width: min(100%, calc(var(--s7) * 12)); max-width: 100%; height: 100%; max-height: none; margin: 0; padding: 0; display: none; flex-direction: column; border: 0; border-left: var(--borde-fino) solid var(--border); background: var(--surface); color: var(--text); box-shadow: var(--sombra-3); }
    dialog[open] { display: flex; animation: entrar var(--mov-entrada) var(--curva-llega); }
    dialog::backdrop { background: color-mix(in srgb, var(--ink) 45%, transparent); }
    header { position: sticky; top: 0; display: flex; align-items: center; justify-content: space-between; gap: var(--s3); padding: var(--s3) var(--s4); border-bottom: var(--borde-fino) solid var(--border); background: var(--surface); }
    h2 { margin: 0; font: var(--t-titulo-3); letter-spacing: var(--t-titulo-3-track); overflow-wrap: anywhere; }
    .cerrar { display: inline-flex; flex: none; align-items: center; justify-content: center; width: var(--area-tactil); height: var(--area-tactil); border: 0; border-radius: var(--r-md); background: transparent; color: var(--text-2); cursor: pointer; }
    .cerrar:hover { background: var(--surface-2); }
    .cerrar:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
    .cuerpo { flex: 1; overflow: auto; padding: var(--s4); }
    footer { display: flex; flex-wrap: wrap; justify-content: flex-end; gap: var(--s2); padding: var(--s3) var(--s4); border-top: var(--borde-fino) solid var(--border); background: var(--surface); }
    footer:empty { display: none; }
    @keyframes entrar { from { transform: translateX(var(--s6)); opacity: 0; } }
    @media (prefers-reduced-motion: reduce) { dialog[open] { animation: none; } }
  `,
})
export class CajonLateral implements AfterViewInit, OnDestroy {
  private static secuencia = 0
  readonly id = `ap-cajon-${++CajonLateral.secuencia}`
  readonly titulo = input.required<string>()
  readonly lado = input<'derecha' | 'izquierda'>('derecha')
  readonly abierto = model(false)
  readonly cerrado = output<void>()
  private readonly caja = viewChild.required<ElementRef<HTMLDialogElement>>('caja')
  private quienAbrio: HTMLElement | null = null
  private readonly clicDeFondo = (e: MouseEvent): void => {
    if (e.target === this.caja().nativeElement) this.cerrar()
  }

  constructor() {
    effect(() => {
      const d = this.caja().nativeElement
      if (this.abierto() && !d.open) {
        this.quienAbrio = document.activeElement instanceof HTMLElement ? document.activeElement : null
        if (typeof d.showModal === 'function') d.showModal()
        else d.setAttribute('open', '')
      }
      if (!this.abierto() && d.open) this.cerrarElemento(d)
    })
  }

  cerrar(): void {
    this.abierto.set(false)
    this.cerrado.emit()
  }

  ngAfterViewInit(): void {
    this.caja().nativeElement.addEventListener('click', this.clicDeFondo)
  }

  ngOnDestroy(): void {
    const d = this.caja().nativeElement
    d.removeEventListener('click', this.clicDeFondo)
    if (d.open) this.cerrarElemento(d)
  }

  private cerrarElemento(d: HTMLDialogElement): void {
    if (typeof d.close === 'function') d.close()
    else d.removeAttribute('open')
    const destino = this.quienAbrio
    this.quienAbrio = null
    if (destino?.isConnected && (document.activeElement === null || document.activeElement === document.body)) destino.focus()
  }
}
