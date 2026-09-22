import { AfterViewInit, ChangeDetectionStrategy, Component, ElementRef, OnDestroy, effect, input, model, output, viewChild } from '@angular/core'
import { Boton } from '../boton/boton'

/**
 * Confirmación modal sobre `<dialog>` nativo: foco atrapado, Escape cierra, fondo
 * inerte. El botón de confirmar **dice la acción exacta** («Confirmar aporte de Bs 250»);
 * si es destructiva va en peligro.
 */
@Component({
  selector: 'ap-dialogo',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Boton],
  template: `
    <dialog
      #caja
      [attr.aria-labelledby]="id + '-titulo'"
      (close)="abierto.set(false)"
      (cancel)="onCancelNativo($event)"
    >
      <h2 [id]="id + '-titulo'">{{ titulo() }}</h2>
      <div class="cuerpo"><ng-content /></div>
      <div class="acciones">
        <ap-boton variante="fantasma" (pulsado)="intentarCerrar()">{{ textoDeCancelar() }}</ap-boton>
        <ap-boton [variante]="destructivo() ? 'peligro' : 'primario'" [cargando]="cargando()" (pulsado)="confirmar.emit()">{{ textoDeConfirmar() }}</ap-boton>
      </div>
    </dialog>
  `,
  styles: `
    dialog { width: min(100% - var(--s6), calc(var(--s7) * 10)); padding: var(--s5); border: 0; border-radius: var(--r-xl); background: var(--surface); color: var(--text); box-shadow: var(--sombra-3); }
    dialog::backdrop { background: color-mix(in srgb, var(--ink) 55%, transparent); }
    h2 { font-size: 1.2em; margin-bottom: var(--s3); }
    .cuerpo { color: var(--text-2); }
    .acciones { display: flex; flex-wrap: wrap; justify-content: flex-end; gap: var(--s2); margin-top: var(--s5); }
  `,
})
export class Dialogo implements AfterViewInit, OnDestroy {
  private static secuencia = 0
  readonly id = `ap-dialogo-${++Dialogo.secuencia}`
  readonly titulo = input.required<string>()
  readonly textoDeConfirmar = input.required<string>()
  readonly textoDeCancelar = input('Cancelar')
  readonly destructivo = input(false)
  readonly cargando = input(false)
  readonly abierto = model(false)
  /**
   * El diálogo no conoce el dominio del formulario (regla de alcance): el consumidor calcula
   * si hay cambios sin guardar y lo pasa acá. Con esto en `true`, las TRES rutas de cierre
   * —botón, `Escape`, clic en el fondo— pasan por la misma guardia (`intentarCerrar`); ninguna
   * la saltea. Sin esto, el cierre es directo (comportamiento previo, sin protección).
   */
  readonly hayCambiosSinGuardar = input(false)
  readonly mensajeDeDescarte = input('Hay cambios sin guardar. ¿Querés descartarlos?')
  readonly confirmar = output<void>()
  readonly cancelar = output<void>()
  private readonly caja = viewChild.required<ElementRef<HTMLDialogElement>>('caja')
  /**
   * El backdrop de `<dialog>` no es proyectable ni tiene un evento propio en la plantilla: se
   * detecta comparando `event.target` con el propio `<dialog>` (un clic en el contenido nunca
   * llega ahí, porque `h2`/`.cuerpo`/`.acciones` lo interceptan antes). Se cablea a mano —no con
   * `(click)` en la plantilla— para no marcar `<dialog>` como "interactivo" ante lectores de
   * pantalla (regla de accesibilidad: el backdrop no tiene equivalente de teclado propio, y no
   * lo necesita — `Escape` ya cierra por la ruta de `cancel`). Eso es también lo que da algo
   * real que limpiar en `ngOnDestroy` (H3.S1.M4).
   */
  private readonly manejarClicDeFondo = (evento: MouseEvent): void => {
    if (evento.target === this.caja().nativeElement) this.intentarCerrar()
  }

  constructor() {
    effect(() => {
      const d = this.caja().nativeElement
      if (this.abierto() && !d.open) {
        // jsdom no implementa showModal: en pruebas se abre como atributo.
        if (typeof d.showModal === 'function') d.showModal()
        else d.setAttribute('open', '')
      }
      if (!this.abierto() && d.open) {
        // jsdom tampoco implementa close(): mismo motivo que showModal() arriba.
        if (typeof d.close === 'function') d.close()
        else d.removeAttribute('open')
      }
    })
  }

  ngAfterViewInit(): void {
    this.caja().nativeElement.addEventListener('click', this.manejarClicDeFondo)
  }

  ngOnDestroy(): void {
    this.caja().nativeElement.removeEventListener('click', this.manejarClicDeFondo)
  }

  /** `Escape`: el navegador dispara `cancel` y cerraría solo. Se frena acá para que pase por la misma guardia que las otras dos rutas. */
  protected onCancelNativo(evento: Event): void {
    evento.preventDefault()
    this.intentarCerrar()
  }

  /** Única puerta de cierre. Botón, `Escape` y fondo llaman a este método — ninguna ruta cierra por otro camino. */
  intentarCerrar(): void {
    if (this.hayCambiosSinGuardar() && !this.confirmarDescarte(this.mensajeDeDescarte())) return
    this.abierto.set(false)
    this.cancelar.emit()
  }

  /** Separado para poder simularlo en tests sin depender de `window.confirm` real. */
  protected confirmarDescarte(mensaje: string): boolean {
    return window.confirm(mensaje)
  }
}
