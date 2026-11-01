import { ChangeDetectionStrategy, Component, computed, linkedSignal, output, signal } from '@angular/core'
import { Campo } from '@aportaya/ui/campo/campo'
import { Dialogo } from '@aportaya/ui/dialogo/dialogo'
import { ErrorDeEnvio, type ErrorParaVista } from '@aportaya/ui/error-de-envio/error-de-envio'
import { ListaDeOpciones, type Opcion } from '@aportaya/ui/lista-de-opciones/lista-de-opciones'
import type { EntradaAltaUsuarioInterno } from 'clientes/angular/identidad'
import { mensajeDeError } from '../compartido/carga'
import { DEPARTAMENTOS_INTERNOS, catalogoRoles, creadorUsuarioInterno } from '../dominio/usuarios-internos'

const CORREO = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

/**
 * Alta de usuario interno (POST `/usuarios-internos` con `Idempotency-Key`, que pone `contextoIdempotente`). El rol
 * inicial arranca en el **primero que trae el catálogo** —antes arrancaba en un código fijo que el catálogo podía no
 * tener— y cada campo dice su error. Un rechazo del servidor deja todo lo escrito y muestra el mensaje traducido.
 */
@Component({
  selector: 'ap-modal-alta-usuario-interno',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Campo, Dialogo, ErrorDeEnvio, ListaDeOpciones],
  template: `
    <ap-dialogo titulo="Dar de alta un usuario interno" [textoDeConfirmar]="enviando() ? 'Creando…' : 'Crear usuario'" [cargando]="enviando()" [confirmarDeshabilitado]="!esValido() || enviando()" [puedeDescartar]="puedeCerrar" [abierto]="true" (confirmar)="guardar()" (cancelar)="cerrar.emit()">
      <div class="campos">
        <ap-campo etiqueta="Nombres" [(valor)]="nombres" autocompletar="given-name" [deshabilitado]="enviando()" [error]="errorDe('nombres')" />
        <ap-campo etiqueta="Apellidos" [(valor)]="apellidos" autocompletar="family-name" [deshabilitado]="enviando()" [error]="errorDe('apellidos')" />
        <ap-campo etiqueta="Correo institucional" tipo="email" [(valor)]="correo" autocompletar="email" [deshabilitado]="enviando()" [error]="errorDe('correo')" />
        <ap-campo etiqueta="Teléfono móvil" tipo="tel" [(valor)]="telefono" marcador="+591…" autocompletar="tel" [deshabilitado]="enviando()" [error]="errorDe('telefono')" />
        <ap-lista-de-opciones etiqueta="Departamento" [opciones]="departamentos" [(valor)]="departamento" [deshabilitado]="enviando()" />
        <ap-campo etiqueta="Cargo" [(valor)]="cargo" [deshabilitado]="enviando()" [error]="errorDe('cargo')" />
      </div>
      <ap-lista-de-opciones etiqueta="Rol inicial" [opciones]="opcionesDeRol()" [(valor)]="rolCodigo" [deshabilitado]="enviando() || opcionesDeRol().length === 0" [error]="errorDeRoles()" />
      <p class="nota">Se le asigna una contraseña provisional que tiene que cambiar en su primer ingreso.</p>
      <ap-error-de-envio [error]="error()" />
    </ap-dialogo>
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
  protected readonly roles = catalogoRoles()
  protected readonly departamentos: Opcion[] = DEPARTAMENTOS_INTERNOS.map((d) => ({ valor: d, texto: d }))
  protected readonly opcionesDeRol = computed<Opcion[]>(() => (this.roles.hasValue() ? (this.roles.value() ?? []) : []).map((r) => ({ valor: r.codigo, texto: r.departamento ? `${r.nombre} (${r.departamento})` : r.nombre })))
  protected readonly nombres = signal('')
  protected readonly apellidos = signal('')
  protected readonly correo = signal('')
  protected readonly telefono = signal('')
  protected readonly departamento = signal<string>(DEPARTAMENTOS_INTERNOS[0])
  protected readonly cargo = signal('')
  protected readonly rolCodigo = linkedSignal(() => this.opcionesDeRol()[0]?.valor ?? '')
  protected readonly enviando = signal(false)
  protected readonly error = signal<ErrorParaVista | null>(null)
  protected readonly puedeCerrar = () => !this.enviando()
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
      error: (e: unknown) => {
        this.enviando.set(false)
        this.error.set(mensajeDeError(e, 'No se pudo dar de alta el usuario interno. Probá de nuevo.'))
      },
    })
  }
}
