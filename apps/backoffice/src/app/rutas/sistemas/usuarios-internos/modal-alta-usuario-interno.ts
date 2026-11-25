import { ChangeDetectionStrategy, Component, computed, linkedSignal, output, signal } from '@angular/core'
import { Campo } from '@aportaya/ui/campo/campo'
import { ListaDeOpciones, type Opcion } from '@aportaya/ui/lista-de-opciones/lista-de-opciones'
import { mensajeDeError } from '../compartido/carga'
import type { ErrorDeGobierno } from '../dominio/errores-de-gobierno'
import { rolesAsignables } from '../dominio/gobierno-de-accesos'
import { DEPARTAMENTOS_INTERNOS, creadorUsuarioInterno, type EntradaDeAlta } from '../dominio/usuarios-internos'
import { DialogoDeActo } from './dialogo-de-acto'

const CORREO = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

/**
 * Alta de usuario interno (POST `/usuarios-internos` con `Idempotency-Key`, que pone `contextoIdempotente`) con motivo y
 * segundo factor (GU-11, `ap-dialogo-de-acto`). El rol inicial arranca en el **primero que trae el catálogo** —antes arrancaba en un código fijo que el catálogo podía no
 * tener— y cada campo dice su error. Un rechazo del servidor deja todo lo escrito y muestra el mensaje traducido.
 */
@Component({
  selector: 'ap-modal-alta-usuario-interno',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Campo, DialogoDeActo, ListaDeOpciones],
  template: `
    <ap-dialogo-de-acto titulo="Dar de alta un usuario interno" textoDeConfirmar="Crear usuario" etiquetaDeMotivo="Por qué se da de alta" [minimoDeMotivo]="10" [maximoDeMotivo]="200" [valido]="esValido()" [accion]="accion" [abierto]="true" (abiertoChange)="!$event && cerrar.emit()" [(error)]="error" (completado)="guardado.emit()">
      <div class="campos">
        <ap-campo etiqueta="Nombres" [(valor)]="nombres" autocompletar="given-name" [error]="errorDe('nombres')" />
        <ap-campo etiqueta="Apellidos" [(valor)]="apellidos" autocompletar="family-name" [error]="errorDe('apellidos')" />
        <ap-campo etiqueta="Correo institucional" tipo="email" [(valor)]="correo" autocompletar="email" [error]="errorDe('correo')" />
        <ap-campo etiqueta="Teléfono móvil" tipo="tel" [(valor)]="telefono" marcador="+591…" autocompletar="tel" [error]="errorDe('telefono')" />
        <ap-lista-de-opciones etiqueta="Departamento" [opciones]="departamentos" [(valor)]="departamento" />
        <ap-campo etiqueta="Cargo" [(valor)]="cargo" [error]="errorDe('cargo')" />
      </div>
      <ap-lista-de-opciones etiqueta="Rol inicial" [opciones]="opcionesDeRol()" [(valor)]="rolCodigo" [deshabilitado]="opcionesDeRol().length === 0" [error]="errorDeRoles() ?? errorDeRol()" ayuda="Solo se ofrecen roles de personal interno." />
      <p class="nota">Se le asigna una contraseña provisional que tiene que cambiar en su primer ingreso.</p>
    </ap-dialogo-de-acto>
  `,
  styles: `
    .campos { display: grid; grid-template-columns: repeat(auto-fit, minmax(min(100%, 14rem), 1fr)); gap: var(--s3); margin-bottom: var(--s3); }
    .nota { margin: var(--s3) 0 0; font: var(--t-ayuda); color: var(--text-3); }
  `,
})
export class ModalAltaUsuarioInterno {
  readonly cerrar = output<void>()
  readonly guardado = output<void>()

  private readonly ejecutarCreacion = creadorUsuarioInterno()
  protected readonly roles = rolesAsignables()
  protected readonly departamentos: Opcion[] = DEPARTAMENTOS_INTERNOS.map((d) => ({ valor: d, texto: d }))
  protected readonly opcionesDeRol = computed<Opcion[]>(() => (this.roles.hasValue() ? (this.roles.value() ?? []) : []).map((r) => ({ valor: r.codigo, texto: r.departamento ? `${r.nombre} (${r.departamento})` : r.nombre })))
  protected readonly nombres = signal('')
  protected readonly apellidos = signal('')
  protected readonly correo = signal('')
  protected readonly telefono = signal('')
  protected readonly departamento = signal<string>(DEPARTAMENTOS_INTERNOS[0])
  protected readonly cargo = signal('')
  protected readonly rolCodigo = linkedSignal(() => this.opcionesDeRol()[0]?.valor ?? '')
  protected readonly error = signal<ErrorDeGobierno | null>(null)
  protected readonly errorDeRol = computed(() => (this.error()?.campo === 'rol' ? this.error()!.mensaje : undefined))
  protected readonly errorDeRoles = computed(() => (this.roles.error() ? mensajeDeError(this.roles.error(), 'No se pudo cargar el catálogo de roles.').mensaje : undefined))

  /** El error de un campo recién cuando la persona escribió algo: un formulario vacío no arranca en rojo. */
  protected errorDe(campo: 'nombres' | 'apellidos' | 'correo' | 'telefono' | 'cargo'): string | undefined {
    const v = this[campo]().trim()
    if (v === '') return undefined
    if (campo === 'correo') return CORREO.test(v) ? undefined : 'Escribí un correo válido (nombre@dominio).'
    if (campo === 'telefono') return v.length >= 8 ? undefined : 'El teléfono tiene que tener al menos 8 caracteres.'
    return v.length >= 2 ? undefined : 'Tiene que tener al menos 2 caracteres.'
  }

  protected esValido(): boolean {
    const campos = [this.nombres(), this.apellidos(), this.correo(), this.telefono(), this.cargo()].map((v) => v.trim())
    return campos.every((v) => v !== '') && (['nombres', 'apellidos', 'correo', 'telefono', 'cargo'] as const).every((c) => !this.errorDe(c)) && this.rolCodigo() !== ''
  }

  /** El alta con la evidencia del segundo factor y el motivo (GU-11): el servidor exige ambos. */
  protected readonly accion = (evidenciaMfa: string, motivo: string) => {
    const payload: EntradaDeAlta = {
      nombres: this.nombres().trim(),
      apellidos: this.apellidos().trim(),
      correo: this.correo().trim().toLowerCase(),
      telefono: this.telefono().trim(),
      departamento: this.departamento(),
      cargo: this.cargo().trim(),
      rolCodigo: this.rolCodigo(),
      motivo,
      evidenciaMfa,
    }
    return this.ejecutarCreacion(payload)
  }
}
