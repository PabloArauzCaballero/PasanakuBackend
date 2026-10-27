import { ChangeDetectionStrategy, Component, computed, output, signal } from '@angular/core'
import { AreaDeTexto } from '@aportaya/ui/area-de-texto/area-de-texto'
import { Campo } from '@aportaya/ui/campo/campo'
import { Dialogo } from '@aportaya/ui/dialogo/dialogo'
import { ErrorDeEnvio, type ErrorParaVista } from '@aportaya/ui/error-de-envio/error-de-envio'
import { ListaDeOpciones, type Opcion } from '@aportaya/ui/lista-de-opciones/lista-de-opciones'
import { EntradaActualizarUsuarioInternoEstadoEnum as EstadoEnum, type UsuarioInternoResumen } from 'clientes/angular/identidad'
import type { Observable } from 'rxjs'
import { mensajeDeError } from '../compartido/carga'
import { DEPARTAMENTOS_INTERNOS, actualizadorUsuarioInterno, desbloqueadorUsuarioInterno } from '../dominio/usuarios-internos'
import { textosSistemas } from '../textos'

type Modo = 'editar' | 'estado' | 'desbloqueo'
const MOTIVO_MINIMO = 5

/**
 * Los diálogos de las operaciones que el contrato tiene sobre un usuario interno (`identidad.yaml`): **Editar**
 * (PATCH departamento/cargo con motivo), **Suspender/Reactivar** (PATCH `estado` con motivo — no hay DELETE: dar de
 * baja es suspender) y **Desbloquear** (POST `/desbloquear` con motivo). Los usan el listado y la ficha.
 *
 * Reemplaza a los `prompt()` de la ficha, que no mostraban nada si el servidor rechazaba. Ante un fallo el diálogo
 * queda abierto con lo escrito y el error traducido; mientras viaja, no se cierra ni se envía dos veces.
 */
@Component({
  selector: 'ap-gestor-de-usuario-interno',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AreaDeTexto, Campo, Dialogo, ErrorDeEnvio, ListaDeOpciones],
  template: `
    <ap-dialogo [titulo]="titulo()" [textoDeConfirmar]="enviando() ? 'Guardando…' : textoDeConfirmar()" [destructivo]="esSuspension()" [cargando]="enviando()" [confirmarDeshabilitado]="!valido() || enviando()" [puedeDescartar]="puedeCerrar" [(abierto)]="abierto" (confirmar)="enviar()" (cancelar)="cerrar()">
      @if (usuario(); as u) {
        <p class="quien">{{ u.nombres }} {{ u.apellidos }} · {{ u.correo }}</p>
        @if (modo() === 'editar') {
          <div class="campos">
            <ap-lista-de-opciones etiqueta="Departamento" [opciones]="departamentos()" [(valor)]="departamento" [deshabilitado]="enviando()" />
            <ap-campo etiqueta="Cargo" [(valor)]="cargo" [deshabilitado]="enviando()" [error]="cargo().trim().length < 2 ? 'El cargo tiene que tener al menos 2 caracteres.' : undefined" />
          </div>
        }
        <ap-area-de-texto etiqueta="Motivo (queda en la auditoría)" [(valor)]="motivo" [maximo]="200" [deshabilitado]="enviando()" [ayuda]="t.motivoObligatorio + ' Mínimo 5 caracteres.'" />
        <ap-error-de-envio [error]="error()" />
      }
    </ap-dialogo>
  `,
  styles: `
    .quien { margin: 0 0 var(--s3); font: var(--t-cuerpo-chico); color: var(--text-2); overflow-wrap: anywhere; }
    .campos { display: grid; grid-template-columns: repeat(auto-fit, minmax(min(100%, 14rem), 1fr)); gap: var(--s3); margin-bottom: var(--s3); }
  `,
})
export class GestorDeUsuarioInterno {
  /** El servidor confirmó el cambio: quien aloja recarga sus datos. */
  readonly actualizado = output<void>()
  protected readonly t = textosSistemas.usuariosInternos
  private readonly actualizar = actualizadorUsuarioInterno()
  private readonly desbloquearUsuario = desbloqueadorUsuarioInterno()

  protected readonly usuario = signal<UsuarioInternoResumen | null>(null)
  protected readonly modo = signal<Modo>('editar')
  protected readonly abierto = signal(false)
  protected readonly departamento = signal('')
  protected readonly cargo = signal('')
  protected readonly motivo = signal('')
  protected readonly enviando = signal(false)
  protected readonly error = signal<ErrorParaVista | null>(null)
  protected readonly puedeCerrar = () => !this.enviando()

  protected readonly esSuspension = computed(() => this.modo() === 'estado' && this.usuario()?.estado === 'ACTIVO')
  protected readonly titulo = computed(() => {
    const m = this.modo()
    if (m === 'editar') return 'Editar datos del usuario interno'
    if (m === 'desbloqueo') return 'Desbloquear usuario interno'
    return this.esSuspension() ? 'Suspender usuario interno' : 'Reactivar usuario interno'
  })
  protected readonly textoDeConfirmar = computed(() => {
    const m = this.modo()
    if (m === 'editar') return 'Guardar cambios'
    if (m === 'desbloqueo') return 'Desbloquear'
    return this.esSuspension() ? 'Suspender' : 'Reactivar'
  })
  protected readonly departamentos = computed<Opcion[]>(() => {
    const actual = this.usuario()?.departamento
    const todos = actual && !(DEPARTAMENTOS_INTERNOS as readonly string[]).includes(actual) ? [actual, ...DEPARTAMENTOS_INTERNOS] : [...DEPARTAMENTOS_INTERNOS]
    return todos.map((d) => ({ valor: d, texto: d }))
  })
  protected readonly valido = computed(() => this.motivo().trim().length >= MOTIVO_MINIMO && (this.modo() !== 'editar' || (this.cargo().trim().length >= 2 && this.departamento() !== '')))

  editar(u: UsuarioInternoResumen): void {
    this.abrir(u, 'editar')
  }
  cambiarEstado(u: UsuarioInternoResumen): void {
    this.abrir(u, 'estado')
  }
  desbloquear(u: UsuarioInternoResumen): void {
    this.abrir(u, 'desbloqueo')
  }

  private abrir(u: UsuarioInternoResumen, modo: Modo): void {
    this.usuario.set(u)
    this.modo.set(modo)
    this.departamento.set(u.departamento)
    this.cargo.set(u.cargo)
    this.motivo.set('')
    this.error.set(null)
    this.abierto.set(true)
  }

  protected cerrar(): void {
    this.abierto.set(false)
  }

  protected enviar(): void {
    const u = this.usuario()
    if (!u || !this.valido() || this.enviando()) return
    this.enviando.set(true)
    this.error.set(null)
    this.peticion(u, this.motivo().trim()).subscribe({
      next: () => {
        this.enviando.set(false)
        this.abierto.set(false)
        this.actualizado.emit()
      },
      // Lo escrito queda: el diálogo sigue abierto con el error traducido y el código de seguimiento.
      error: (e: unknown) => {
        this.enviando.set(false)
        this.error.set(mensajeDeError(e, 'No se pudo guardar el cambio. Probá de nuevo.'))
      },
    })
  }

  private peticion(u: UsuarioInternoResumen, motivo: string): Observable<unknown> {
    const m = this.modo()
    if (m === 'desbloqueo') return this.desbloquearUsuario(u.id, motivo)
    if (m === 'editar') return this.actualizar(u.id, { departamento: this.departamento(), cargo: this.cargo().trim(), motivo })
    return this.actualizar(u.id, { estado: u.estado === 'ACTIVO' ? EstadoEnum.Suspendido : EstadoEnum.Activo, motivo })
  }
}
