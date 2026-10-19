import { ChangeDetectionStrategy, Component, OnDestroy, input, signal } from '@angular/core'
import { Icono } from '../icono/icono'
import { Tooltip } from '../tooltip/tooltip'

/**
 * Copia un texto al portapapeles y lo dice («Copiado») en una región viva, además de cambiar el
 * ícono por dos segundos. Es un botón de solo ícono: lleva nombre accesible y tooltip. Si el
 * portapapeles no está disponible (contexto inseguro, permiso negado), avisa en vez de fingir.
 */
@Component({
  selector: 'ap-boton-copiar',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Icono, Tooltip],
  template: `
    <ap-tooltip [texto]="estado() === 'copiado' ? 'Copiado' : estado() === 'fallo' ? 'No se pudo copiar' : etiqueta()">
      <button type="button" [attr.aria-label]="etiqueta()" (click)="copiar()">
        <ap-icono [nombre]="estado() === 'copiado' ? 'verificado' : 'copiar'" tamano="s4" />
      </button>
    </ap-tooltip>
    <span class="oculto" role="status" aria-live="polite">{{ aviso() }}</span>
  `,
  styles: `
    :host { display: inline-flex; }
    button { display: inline-flex; align-items: center; justify-content: center; width: var(--area-tactil); height: var(--area-tactil); padding: 0; border: 0; border-radius: var(--r-md); background: transparent; color: var(--text-3); cursor: pointer; transition: background-color var(--mov-micro) ease-out, color var(--mov-micro) ease-out; }
    button:hover { background: var(--surface-2); color: var(--text); }
    button:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
    .oculto { position: absolute; width: var(--borde-fino); height: var(--borde-fino); overflow: hidden; clip: rect(0 0 0 0); }
  `,
})
export class BotonCopiar implements OnDestroy {
  /** El texto a copiar, o una función que lo arma recién al pulsar (una tabla entera). */
  readonly valor = input.required<string | (() => string)>()
  readonly etiqueta = input('Copiar')
  readonly estado = signal<'quieto' | 'copiado' | 'fallo'>('quieto')
  readonly aviso = signal('')
  private temporizador: ReturnType<typeof setTimeout> | undefined

  async copiar(): Promise<void> {
    const v = this.valor()
    const texto = typeof v === 'function' ? v() : v
    try {
      await navigator.clipboard.writeText(texto)
      this.estado.set('copiado')
      this.aviso.set('Copiado al portapapeles')
    } catch {
      this.estado.set('fallo')
      this.aviso.set('No se pudo copiar')
    }
    clearTimeout(this.temporizador)
    this.temporizador = setTimeout(() => {
      this.estado.set('quieto')
      this.aviso.set('')
    }, 2000)
  }

  ngOnDestroy(): void {
    clearTimeout(this.temporizador)
  }
}
