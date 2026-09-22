import { ChangeDetectionStrategy, Component, ElementRef, effect, input, model, output, viewChild } from '@angular/core'
import { Boton } from '../boton/boton'

/**
 * Confirmación modal sobre `<dialog>` nativo: foco atrapado y restaurado al elemento que
 * lo abrió (comportamiento nativo de `showModal`/`close`, HTML Standard §4.11.4 — no hay
 * que reimplementarlo). El botón de confirmar **dice la acción exacta** («Confirmar aporte
 * de Bs 250»); si es destructiva va en peligro.
 *
 * **Política de descarte única (H3.S2):** las tres rutas de cierre —botón, `Escape` y clic
 * en el fondo— pasan por el mismo punto: `intentarCerrar()`. Si el consumidor pasa
 * `[puedeDescartar]` y devuelve `false` (borrador sucio), el cierre se cancela en las tres
 * por igual: en `Escape` con `event.preventDefault()` sobre el evento nativo `cancel`
 * (cancelable, HTML Standard), en el fondo sin invocar `close()`, y en el botón sin tocar
 * `abierto`. Ninguna ruta interna puede saltear el predicado porque las tres llaman a la
 * misma función.
 */
@Component({
  selector: 'ap-dialogo',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Boton],
  template: `
    <!--
      El clic es un cierre suplementario del fondo, no el único camino: Escape (evento
      cancel nativo, manejado en onCancelNativo) ya hace exactamente lo mismo por teclado,
      y el botón "Cancelar" también. WCAG 2.1.1 exige que la funcionalidad esté disponible por
      teclado, no que cada gesto de mouse tenga un evento de teclado calcado — acá ya lo está,
      dos veces. El propio dialog no es, ni debe ser, un control enfocable: es el contenedor
      modal (ya atrapa el foco de forma nativa); agregarle tabindex para conformar al lint
      sería peor accesibilidad, no mejor.
    -->
    <!-- eslint-disable-next-line @angular-eslint/template/click-events-have-key-events, @angular-eslint/template/interactive-supports-focus -->
    <dialog #caja [attr.aria-labelledby]="id + '-titulo'" (close)="abierto.set(false)" (cancel)="onCancelNativo($event)" (click)="onClickEnFondo($event)">
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
export class Dialogo {
  private static secuencia = 0
  readonly id = `ap-dialogo-${++Dialogo.secuencia}`
  readonly titulo = input.required<string>()
  readonly textoDeConfirmar = input.required<string>()
  readonly textoDeCancelar = input('Cancelar')
  readonly destructivo = input(false)
  readonly cargando = input(false)
  /**
   * Predicado de la política de descarte. Devuelve `true` si se puede cerrar sin
   * preguntar (borrador limpio o sin borrador) y `false` si el cierre debe cancelarse
   * (típicamente porque ya preguntó al usuario y este eligió quedarse). Por omisión
   * siempre permite cerrar: un diálogo sin formulario no necesita protección.
   */
  readonly puedeDescartar = input<() => boolean>(() => true)
  readonly abierto = model(false)
  readonly confirmar = output<void>()
  readonly cancelar = output<void>()
  private readonly caja = viewChild.required<ElementRef<HTMLDialogElement>>('caja')

  constructor() {
    effect(() => {
      const d = this.caja().nativeElement
      if (this.abierto() && !d.open) {
        // jsdom (28.1.0, la instalada) no implementa `showModal`/`close` de `HTMLDialogElement`
        // — confirmado corriendo `new JSDOM('<dialog></dialog>')` y leyendo `typeof`, no
        // supuesto: en pruebas el diálogo se abre y se cierra por el atributo `open`, sin el
        // foco atrapado real (eso se verifica en E2E con navegador de verdad, no acá).
        if (typeof d.showModal === 'function') d.showModal()
        else d.setAttribute('open', '')
      }
      if (!this.abierto() && d.open) {
        if (typeof d.close === 'function') d.close()
        else d.removeAttribute('open')
      }
    })
  }

  /** Punto único de las tres rutas de cierre. No lo llames dos veces por el mismo gesto. */
  intentarCerrar(): void {
    if (!this.puedeDescartar()()) return
    this.abierto.set(false)
    this.cancelar.emit()
  }

  /** `Escape` dispara `cancel` (cancelable) y luego, si no se cancela, `close`. */
  onCancelNativo(evento: Event): void {
    if (!this.puedeDescartar()()) {
      evento.preventDefault()
      return
    }
    this.cancelar.emit()
  }

  /** El backdrop nativo no cierra `<dialog>` por sí solo: un clic sobre `dialog` (no sobre
   * `.cuerpo`/`.acciones`, que son hijos) es un clic en el fondo. */
  onClickEnFondo(evento: MouseEvent): void {
    if (evento.target === this.caja().nativeElement) this.intentarCerrar()
  }
}
