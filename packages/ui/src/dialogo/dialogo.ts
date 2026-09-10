import { ChangeDetectionStrategy, Component, ElementRef, effect, input, model, output, viewChild } from '@angular/core'
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
    <dialog #caja [attr.aria-labelledby]="id + '-titulo'" (close)="abierto.set(false)" (cancel)="cancelar.emit()">
      <h2 [id]="id + '-titulo'">{{ titulo() }}</h2>
      <div class="cuerpo"><ng-content /></div>
      <div class="acciones">
        <ap-boton variante="fantasma" (pulsado)="cerrar()">{{ textoDeCancelar() }}</ap-boton>
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
  readonly abierto = model(false)
  readonly confirmar = output<void>()
  readonly cancelar = output<void>()
  private readonly caja = viewChild.required<ElementRef<HTMLDialogElement>>('caja')

  constructor() {
    effect(() => {
      const d = this.caja().nativeElement
      if (this.abierto() && !d.open) {
        // jsdom no implementa showModal: en pruebas se abre como atributo.
        if (typeof d.showModal === 'function') d.showModal()
        else d.setAttribute('open', '')
      }
      if (!this.abierto() && d.open) d.close()
    })
  }
  cerrar(): void {
    this.abierto.set(false)
    this.cancelar.emit()
  }
}
