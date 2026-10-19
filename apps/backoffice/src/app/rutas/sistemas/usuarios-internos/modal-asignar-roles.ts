import { ChangeDetectionStrategy, Component, HostListener, input, output, signal } from '@angular/core'
import { FormsModule } from '@angular/forms'
import { Boton } from '@aportaya/ui/boton/boton'
import { ErrorDeEnvio, type ErrorParaVista } from '@aportaya/ui/error-de-envio/error-de-envio'
import type { EntradaAsignarRolesUsuarioInterno } from 'clientes/angular/identidad'
import { asignadorRolesUsuarioInterno, catalogoRoles } from '../dominio/usuarios-internos'

@Component({
  selector: 'ap-modal-asignar-roles',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Boton, FormsModule, ErrorDeEnvio],
  template: `
    <div class="telon">
      <div class="caja" role="dialog" aria-modal="true" aria-labelledby="modal-roles-titulo">
        <header>
          <h2 id="modal-roles-titulo">Modificar roles asignados (CU-08)</h2>
          <ap-boton variante="fantasma" (pulsado)="cerrar.emit()" aria-label="Cerrar modal">✕</ap-boton>
        </header>

        <form (ngSubmit)="guardar()">
          <p class="ayuda">Seleccioná los roles que corresponden a las funciones del operador:</p>

          <div class="lista-seleccion">
            @for (r of roles.value() ?? []; track r.id) {
              <label class="item-rol">
                <input type="checkbox" [checked]="rolesSeleccionados().includes(r.codigo)" (change)="toggleRol(r.codigo)" [disabled]="enviando()" />
                <div>
                  <strong>{{ r.nombre }}</strong>
                  <span class="subtexto">{{ r.departamento }} · {{ r.descripcion }}</span>
                </div>
              </label>
            }
          </div>

          <label class="motivo-label">
            <span>Motivo formal del cambio de roles (obligatorio)</span>
            <textarea [(ngModel)]="motivo" name="motivo" rows="3" placeholder="Ej: Aprobado por el comité de operaciones..." required [disabled]="enviando()"></textarea>
          </label>

          <ap-error-de-envio [error]="error()" />

          <footer>
            <ap-boton variante="fantasma" (pulsado)="cerrar.emit()" [deshabilitado]="enviando()">Cancelar</ap-boton>
            <ap-boton tipo="submit" variante="primario" [deshabilitado]="enviando() || !esValido()">
              {{ enviando() ? 'Guardando...' : 'Confirmar roles' }}
            </ap-boton>
          </footer>
        </form>
      </div>
    </div>
  `,
  styles: `
    .telon { position: fixed; inset: 0; background: var(--overlay); display: flex; align-items: center; justify-content: center; z-index: var(--z-modal); padding: var(--s4); }
    .caja { background: var(--surface); border: var(--borde-fino) solid var(--border); border-radius: var(--r-xl); padding: var(--s5); width: 100%; max-width: 38rem; max-height: 90vh; overflow-y: auto; display: flex; flex-direction: column; gap: var(--s4); box-shadow: var(--sombra-lg); }
    header { display: flex; justify-content: space-between; align-items: center; }
    h2 { margin: 0; font-size: 1.25rem; font-weight: 600; color: var(--text); }
    p.ayuda { margin: 0; font-size: .875rem; color: var(--text-2); }
    form { display: flex; flex-direction: column; gap: var(--s3); }
    .lista-seleccion { max-height: 18rem; overflow-y: auto; display: flex; flex-direction: column; gap: var(--s2); border: var(--borde-fino) solid var(--border); border-radius: var(--r-md); padding: var(--s3); background: var(--surface-2); }
    .item-rol { display: flex; gap: var(--s3); align-items: flex-start; cursor: pointer; }
    .subtexto { display: block; font-size: .75rem; color: var(--text-3); }
    .motivo-label { display: flex; flex-direction: column; gap: var(--s1); font-size: .875rem; color: var(--text-2); }
    textarea { padding: var(--s2); border: var(--borde-fino) solid var(--border); border-radius: var(--r-md); background: var(--surface-2); color: var(--text); font: inherit; resize: vertical; }
    footer { display: flex; justify-content: flex-end; gap: var(--s2); margin-top: var(--s2); }
  `,
})
export class ModalAsignarRoles {
  readonly usuarioId = input.required<string>()
  readonly rolesActuales = input<string[]>([])
  readonly cerrar = output<void>()
  readonly guardado = output<void>()

  @HostListener('document:keydown.escape')
  protected alPresionarEscape(): void {
    if (!this.enviando()) {
      this.cerrar.emit()
    }
  }

  private readonly ejecutarAsignacion = asignadorRolesUsuarioInterno()

  protected readonly roles = catalogoRoles()
  protected readonly rolesSeleccionados = signal<string[]>([])
  protected readonly motivo = signal('')
  protected readonly enviando = signal(false)
  protected readonly error = signal<ErrorParaVista | null>(null)

  constructor() {
    // Inicializar roles seleccionados con los vigentes
    setTimeout(() => {
      this.rolesSeleccionados.set([...this.rolesActuales()])
    }, 0)
  }

  protected toggleRol(codigo: string): void {
    const actuales = this.rolesSeleccionados()
    if (actuales.includes(codigo)) {
      this.rolesSeleccionados.set(actuales.filter((c) => c !== codigo))
    } else {
      this.rolesSeleccionados.set([...actuales, codigo])
    }
  }

  protected esValido(): boolean {
    return this.rolesSeleccionados().length > 0 && this.motivo().trim().length >= 5
  }

  protected guardar(): void {
    if (!this.esValido() || this.enviando()) return
    this.enviando.set(true)
    this.error.set(null)

    const payload: EntradaAsignarRolesUsuarioInterno = {
      roles: this.rolesSeleccionados(),
      motivo: this.motivo().trim(),
    }

    this.ejecutarAsignacion(this.usuarioId(), payload).subscribe({
      next: () => {
        this.enviando.set(false)
        this.guardado.emit()
      },
      error: (err) => {
        this.enviando.set(false)
        this.error.set({ mensaje: err.error?.mensaje ?? 'Error al asignar los roles.' })
      },
    })
  }
}
