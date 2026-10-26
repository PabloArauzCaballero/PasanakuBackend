import { ChangeDetectionStrategy, Component, computed, input, linkedSignal, output, signal } from '@angular/core'
import { AreaDeTexto } from '@aportaya/ui/area-de-texto/area-de-texto'
import { Casilla } from '@aportaya/ui/casilla/casilla'
import { Dialogo } from '@aportaya/ui/dialogo/dialogo'
import { ErrorDeEnvio, type ErrorParaVista } from '@aportaya/ui/error-de-envio/error-de-envio'
import { Esqueleto } from '@aportaya/ui/esqueleto/esqueleto'
import type { EntradaAsignarRolesUsuarioInterno } from 'clientes/angular/identidad'
import { mensajeDeError } from '../compartido/carga'
import { asignadorRolesUsuarioInterno, catalogoRoles } from '../dominio/usuarios-internos'

/**
 * CU-08 · asignar los roles de un usuario interno (PUT `/usuarios-internos/{id}/roles` con motivo). Se monta con
 * `@if` y abre el `ap-dialogo` nativo al nacer: foco atrapado, `Escape` cierra y el foco vuelve a quien lo abrió.
 * La selección arranca en los roles vigentes (`linkedSignal`, sin el `setTimeout` de antes) y un fallo conserva la
 * selección y el motivo, con el error traducido.
 */
@Component({
  selector: 'ap-modal-asignar-roles',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AreaDeTexto, Casilla, Dialogo, ErrorDeEnvio, Esqueleto],
  template: `
    <ap-dialogo titulo="Modificar roles asignados (CU-08)" [textoDeConfirmar]="enviando() ? 'Guardando…' : 'Confirmar roles'" [cargando]="enviando()" [confirmarDeshabilitado]="!esValido() || enviando()" [puedeDescartar]="puedeCerrar" [abierto]="true" (confirmar)="guardar()" (cancelar)="cerrar.emit()">
      <p class="ayuda">Marcá los roles que corresponden a las funciones del operador. Tiene que quedar al menos uno.</p>
      <fieldset class="lista-seleccion" [disabled]="enviando()">
        <legend>Roles disponibles</legend>
        @if (roles.isLoading()) {
          <ap-esqueleto [lineas]="3" etiqueta="Cargando el catálogo de roles" />
        } @else if (roles.error()) {
          <p class="subtexto">{{ errorDeCatalogo() }}</p>
        } @else {
          @for (r of roles.value() ?? []; track r.id) {
            <ap-casilla [marcada]="rolesSeleccionados().includes(r.codigo)" (marcadaChange)="toggleRol(r.codigo)">
              <strong>{{ r.nombre }}</strong> <span class="subtexto">{{ r.codigo }} · {{ r.departamento ?? 'Sin departamento' }}</span>
            </ap-casilla>
          }
        }
      </fieldset>
      <ap-area-de-texto etiqueta="Motivo formal del cambio de roles" [(valor)]="motivo" [maximo]="200" [deshabilitado]="enviando()" ayuda="Obligatorio para la auditoría. Mínimo 5 caracteres." />
      <ap-error-de-envio [error]="error()" />
    </ap-dialogo>
  `,
  styles: `
    .ayuda { margin: 0 0 var(--s3); font: var(--t-cuerpo-chico); color: var(--text-2); }
    .lista-seleccion { display: grid; gap: var(--s1); max-height: calc(var(--s7) * 5); overflow-y: auto; margin: 0 0 var(--s3); padding: var(--s3); border: var(--borde-fino) solid var(--border); border-radius: var(--r-md); background: var(--surface-2); }
    legend { padding: 0 var(--s1); font: var(--t-campo-etiqueta); color: var(--text-2); }
    .subtexto { font: var(--t-ayuda); color: var(--text-3); overflow-wrap: anywhere; }
  `,
})
export class ModalAsignarRoles {
  readonly usuarioId = input.required<string>()
  readonly rolesActuales = input<string[]>([])
  readonly cerrar = output<void>()
  readonly guardado = output<void>()

  private readonly ejecutarAsignacion = asignadorRolesUsuarioInterno()
  protected readonly roles = catalogoRoles()
  protected readonly rolesSeleccionados = linkedSignal<string[]>(() => [...this.rolesActuales()])
  protected readonly motivo = signal('')
  protected readonly enviando = signal(false)
  protected readonly error = signal<ErrorParaVista | null>(null)
  protected readonly puedeCerrar = () => !this.enviando()
  protected readonly errorDeCatalogo = computed(() => mensajeDeError(this.roles.error(), 'No se pudo cargar el catálogo de roles.').mensaje)

  protected toggleRol(codigo: string): void {
    const actuales = this.rolesSeleccionados()
    this.rolesSeleccionados.set(actuales.includes(codigo) ? actuales.filter((c) => c !== codigo) : [...actuales, codigo])
  }

  protected esValido(): boolean {
    return this.rolesSeleccionados().length > 0 && this.motivo().trim().length >= 5
  }

  protected guardar(): void {
    if (!this.esValido() || this.enviando()) return
    this.enviando.set(true)
    this.error.set(null)
    const payload: EntradaAsignarRolesUsuarioInterno = { roles: this.rolesSeleccionados(), motivo: this.motivo().trim() }
    this.ejecutarAsignacion(this.usuarioId(), payload).subscribe({
      next: () => {
        this.enviando.set(false)
        this.guardado.emit()
      },
      error: (e: unknown) => {
        this.enviando.set(false)
        this.error.set(mensajeDeError(e, 'No se pudieron asignar los roles. Probá de nuevo.'))
      },
    })
  }
}
