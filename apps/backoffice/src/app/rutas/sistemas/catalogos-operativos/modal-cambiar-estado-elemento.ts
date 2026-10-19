import { ChangeDetectionStrategy, Component, HostListener, input, output, signal } from '@angular/core'
import { FormsModule } from '@angular/forms'
import { Boton } from '@aportaya/ui/boton/boton'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { ErrorDeEnvio, type ErrorParaVista } from '@aportaya/ui/error-de-envio/error-de-envio'
import {
  actualizadorElementoCatalogo,
  type ElementoCatalogoOperativo,
} from '../dominio/catalogos-operativos'
import { textosSistemas } from '../textos'

@Component({
  selector: 'ap-modal-cambiar-estado-elemento',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Boton, ChipEstado, FormsModule, ErrorDeEnvio],
  template: `
    <div class="telon">
      <div class="caja" role="dialog" aria-modal="true" aria-labelledby="modal-estado-titulo">
        <header>
          <h2 id="modal-estado-titulo">{{ t.cambiarEstadoTitulo }}</h2>
          <ap-boton variante="fantasma" (pulsado)="cerrar.emit()" aria-label="Cerrar modal">✕</ap-boton>
        </header>

        <form (ngSubmit)="guardar()">
          <div class="resumen-elemento">
            <div class="campo">
              <span class="etiqueta">Elemento:</span>
              <strong>{{ elemento().nombre }} (<code>{{ elemento().codigo }}</code>)</strong>
            </div>
            <div class="campo">
              <span class="etiqueta">Nuevo estado:</span>
              <ap-chip-estado [tono]="nuevoEstado() ? 'ok' : 'neutro'">
                {{ nuevoEstado() ? 'Activo' : 'Inactivo' }}
              </ap-chip-estado>
            </div>
          </div>

          <label class="motivo-label">
            <span>{{ t.motivoObligatorio }}</span>
            <textarea
              [ngModel]="motivo()"
              (ngModelChange)="motivo.set($event)"
              name="motivo"
              rows="3"
              placeholder="Ej: Desactivado por resolución del comité de riesgos..."
              required
              [disabled]="enviando()"
            ></textarea>
          </label>

          <ap-error-de-envio [error]="error()" />

          <footer>
            <ap-boton variante="fantasma" (pulsado)="cerrar.emit()" [deshabilitado]="enviando()">
              {{ t.cancelar }}
            </ap-boton>
            <ap-boton tipo="submit" variante="primario" [deshabilitado]="enviando() || !esValido()">
              {{ enviando() ? 'Guardando...' : t.guardar }}
            </ap-boton>
          </footer>
        </form>
      </div>
    </div>
  `,
  styles: `
    .telon { position: fixed; inset: 0; background: var(--overlay); display: flex; align-items: center; justify-content: center; z-index: var(--z-modal); padding: var(--s4); }
    .caja { background: var(--surface); border: var(--borde-fino) solid var(--border); border-radius: var(--r-xl); padding: var(--s5); width: 100%; max-width: 32rem; max-height: 90vh; overflow-y: auto; display: flex; flex-direction: column; gap: var(--s4); box-shadow: var(--sombra-lg); }
    header { display: flex; justify-content: space-between; align-items: center; }
    h2 { margin: 0; font-size: 1.25rem; font-weight: 600; color: var(--text); }
    form { display: flex; flex-direction: column; gap: var(--s3); }
    .resumen-elemento { display: flex; flex-direction: column; gap: var(--s2); padding: var(--s3); border: var(--borde-fino) solid var(--border); border-radius: var(--r-md); background: var(--surface-2); }
    .campo { display: flex; align-items: center; gap: var(--s2); font-size: 0.875rem; color: var(--text); }
    .etiqueta { color: var(--text-2); min-width: 6rem; }
    code { font-family: var(--f-mono); font-size: 0.85em; background: var(--surface-3); padding: var(--s1) var(--s2); border-radius: var(--r-sm); }
    .motivo-label { display: flex; flex-direction: column; gap: var(--s1); font-size: 0.875rem; color: var(--text-2); }
    textarea { padding: var(--s2); border: var(--borde-fino) solid var(--border); border-radius: var(--r-md); background: var(--surface-2); color: var(--text); font: inherit; resize: vertical; }
    footer { display: flex; justify-content: flex-end; gap: var(--s2); margin-top: var(--s2); }
  `,
})
export class ModalCambiarEstadoElemento {
  readonly catalogoCodigo = input.required<string>()
  readonly elemento = input.required<ElementoCatalogoOperativo>()
  readonly cerrar = output<void>()
  readonly guardado = output<void>()

  protected readonly t = textosSistemas.catalogosOperativos
  protected readonly motivo = signal('')
  protected readonly enviando = signal(false)
  protected readonly error = signal<ErrorParaVista | null>(null)

  private readonly actualizar = actualizadorElementoCatalogo()

  @HostListener('document:keydown.escape')
  protected alPresionarEscape(): void {
    if (!this.enviando()) {
      this.cerrar.emit()
    }
  }

  protected nuevoEstado(): boolean {
    return !this.elemento().activo
  }

  protected esValido(): boolean {
    return this.motivo().trim().length >= 5
  }

  protected guardar(): void {
    if (!this.esValido() || this.enviando()) return
    this.enviando.set(true)
    this.error.set(null)

    this.actualizar(this.catalogoCodigo(), this.elemento().id, {
      activo: this.nuevoEstado(),
      motivo: this.motivo().trim(),
    }).subscribe({
      next: () => {
        this.enviando.set(false)
        this.guardado.emit()
      },
      error: (err) => {
        this.enviando.set(false)
        this.error.set({
          mensaje: err.error?.mensaje ?? err.message ?? 'Error al actualizar el elemento.',
        })
      },
    })
  }
}
