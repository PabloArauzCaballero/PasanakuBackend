import { ChangeDetectionStrategy, Component, HostListener, output, signal } from '@angular/core'
import { FormsModule } from '@angular/forms'
import { Boton } from '@aportaya/ui/boton/boton'
import { ErrorDeEnvio, type ErrorParaVista } from '@aportaya/ui/error-de-envio/error-de-envio'
import type { EntradaAltaUsuarioInterno } from 'clientes/angular/identidad'
import { catalogoRoles, creadorUsuarioInterno } from '../dominio/usuarios-internos'

@Component({
  selector: 'ap-modal-alta-usuario-interno',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Boton, FormsModule, ErrorDeEnvio],
  template: `
    <div class="telon">
      <div class="caja" role="dialog" aria-modal="true" aria-labelledby="modal-alta-titulo">
        <header>
          <h2 id="modal-alta-titulo">Dar de alta usuario interno</h2>
          <ap-boton variante="fantasma" (pulsado)="cerrar.emit()" aria-label="Cerrar modal">✕</ap-boton>
        </header>

        <form (ngSubmit)="guardar()">
          <div class="fila-doble">
            <label>
              <span>Nombres</span>
              <input type="text" [(ngModel)]="nombres" name="nombres" required [disabled]="enviando()" />
            </label>
            <label>
              <span>Apellidos</span>
              <input type="text" [(ngModel)]="apellidos" name="apellidos" required [disabled]="enviando()" />
            </label>
          </div>

          <div class="fila-doble">
            <label>
              <span>Correo institucional</span>
              <input type="email" [(ngModel)]="correo" name="correo" required [disabled]="enviando()" />
            </label>
            <label>
              <span>Teléfono móvil</span>
              <input type="tel" [(ngModel)]="telefono" name="telefono" placeholder="+591..." required [disabled]="enviando()" />
            </label>
          </div>

          <div class="fila-doble">
            <label>
              <span>Departamento</span>
              <select [(ngModel)]="departamento" name="departamento" required [disabled]="enviando()">
                <option value="OPERACIONES">OPERACIONES</option>
                <option value="RIESGOS">RIESGOS</option>
                <option value="CUMPLIMIENTO">CUMPLIMIENTO</option>
                <option value="TECNOLOGIA">TECNOLOGIA</option>
                <option value="COBRANZAS">COBRANZAS</option>
                <option value="GOBERNANZA_DATOS">GOBERNANZA_DATOS</option>
                <option value="AUDITORIA">AUDITORIA</option>
                <option value="EJECUTIVO">EJECUTIVO</option>
              </select>
            </label>
            <label>
              <span>Cargo</span>
              <input type="text" [(ngModel)]="cargo" name="cargo" required [disabled]="enviando()" />
            </label>
          </div>

          <label>
            <span>Rol inicial</span>
            <select [(ngModel)]="rolCodigo" name="rolCodigo" required [disabled]="enviando()">
              @for (r of roles.value() ?? []; track r.id) {
                <option [value]="r.codigo">{{ r.nombre }} ({{ r.departamento }})</option>
              }
            </select>
          </label>

          <p class="nota">Al crear el usuario, se le asignará una contraseña provisional que deberá cambiar en su primer ingreso.</p>
          <ap-error-de-envio [error]="error()" />

          <footer>
            <ap-boton variante="fantasma" (pulsado)="cerrar.emit()" [deshabilitado]="enviando()">Cancelar</ap-boton>
            <ap-boton tipo="submit" variante="primario" [deshabilitado]="enviando() || !esValido()">
              {{ enviando() ? 'Creando...' : 'Crear usuario' }}
            </ap-boton>
          </footer>
        </form>
      </div>
    </div>
  `,
  styles: `
    .telon { position: fixed; inset: 0; background: var(--overlay); display: flex; align-items: center; justify-content: center; z-index: var(--z-modal); padding: var(--s4); }
    .caja { background: var(--surface); border: var(--borde-fino) solid var(--border); border-radius: var(--r-xl); padding: var(--s5); width: 100%; max-width: 36rem; max-height: 90vh; overflow-y: auto; display: flex; flex-direction: column; gap: var(--s4); box-shadow: var(--sombra-lg); }
    header { display: flex; justify-content: space-between; align-items: center; }
    h2 { margin: 0; font-size: 1.25rem; font-weight: 600; color: var(--text); }
    form { display: flex; flex-direction: column; gap: var(--s3); }
    .fila-doble { display: grid; grid-template-columns: 1fr 1fr; gap: var(--s3); }
    label { display: flex; flex-direction: column; gap: var(--s1); font-size: .875rem; color: var(--text-2); }
    input, select { min-height: 2.75rem; padding: 0 var(--s3); border: var(--borde-fino) solid var(--border); border-radius: var(--r-md); background: var(--surface-2); color: var(--text); font: inherit; }
    .nota { margin: 0; font-size: .8125rem; color: var(--text-3); }
    footer { display: flex; justify-content: flex-end; gap: var(--s2); margin-top: var(--s2); }
  `,
})
export class ModalAltaUsuarioInterno {
  readonly cerrar = output<void>()
  readonly guardado = output<void>()

  @HostListener('document:keydown.escape')
  protected alPresionarEscape(): void {
    if (!this.enviando()) {
      this.cerrar.emit()
    }
  }

  private readonly ejecutarCreacion = creadorUsuarioInterno()

  protected readonly roles = catalogoRoles()
  protected readonly nombres = signal('')
  protected readonly apellidos = signal('')
  protected readonly correo = signal('')
  protected readonly telefono = signal('')
  protected readonly departamento = signal('OPERACIONES')
  protected readonly cargo = signal('')
  protected readonly rolCodigo = signal('OPERATIONS_ANALYST')
  protected readonly enviando = signal(false)
  protected readonly error = signal<ErrorParaVista | null>(null)

  protected esValido(): boolean {
    return (
      this.nombres().trim().length >= 2 &&
      this.apellidos().trim().length >= 2 &&
      this.correo().includes('@') &&
      this.telefono().trim().length >= 8 &&
      this.cargo().trim().length >= 2
    )
  }

  protected guardar(): void {
    if (!this.esValido() || this.enviando()) return
    this.enviando.set(true)
    this.error.set(null)

    const payload: EntradaAltaUsuarioInterno = {
      nombres: this.nombres().trim(),
      apellidos: this.apellidos().trim(),
      correo: this.correo().trim().toLowerCase(),
      telefono: this.telefono().trim(),
      departamento: this.departamento(),
      cargo: this.cargo().trim(),
      rolCodigo: this.rolCodigo(),
    }

    this.ejecutarCreacion(payload).subscribe({
      next: () => {
        this.enviando.set(false)
        this.guardado.emit()
      },
      error: (err) => {
        this.enviando.set(false)
        this.error.set({ mensaje: err.error?.mensaje ?? 'Error al dar de alta el usuario interno.' })
      },
    })
  }
}
