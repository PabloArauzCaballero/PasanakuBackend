import { ChangeDetectionStrategy, Component, computed, output, signal } from '@angular/core'
import { AreaDeTexto } from '@aportaya/ui/area-de-texto/area-de-texto'
import { Campo } from '@aportaya/ui/campo/campo'
import { Dialogo } from '@aportaya/ui/dialogo/dialogo'
import { ErrorDeEnvio, type ErrorParaVista } from '@aportaya/ui/error-de-envio/error-de-envio'
import { ListaDeOpciones, type Opcion } from '@aportaya/ui/lista-de-opciones/lista-de-opciones'
import { EntradaActualizarUsuarioInternoEstadoEnum as EstadoEnum, type UsuarioInternoResumen } from 'clientes/angular/identidad'
import { map, type Observable } from 'rxjs'
import { mensajeDeError } from '../compartido/carga'
import { accionesDeAccesos } from '../dominio/gobierno-de-accesos'
import { DEPARTAMENTOS_INTERNOS, actualizadorUsuarioInterno, desbloqueadorUsuarioInterno } from '../dominio/usuarios-internos'
import { textosSistemas } from '../textos'
import { DialogoDeActo } from './dialogo-de-acto'

type Modo = 'editar' | 'estado' | 'desbloqueo' | 'baja'
const MOTIVO_MINIMO = 5

/** Lo que el servidor dijo de la baja (`SalidaBajaUsuarioInterno`), para contarlo en claro. */
type ResultadoDeBaja = { asignacionesRevocadas?: number; sesionesCerradas?: number; yaEstabaDeBaja?: boolean }

/**
 * Los diálogos de las operaciones que el contrato tiene sobre un usuario interno (`identidad.yaml`): **Editar**
 * (PATCH departamento/cargo con motivo; no es un acto de gobierno, no pide segundo factor), y los actos de gobierno
 * **Suspender/Reactivar**, **Desbloquear** y **Dar de baja** (GU-11, GU-14), que piden motivo y el código del segundo
 * factor y dicen su efecto antes de confirmar (`ap-dialogo-de-acto`). Los usan el listado y la ficha.
 *
 * Suspender revoca todos los roles y reactivar vuelve **sin roles** (decisión de Pablo, 2026-11-24): el diálogo lo dice.
 * La baja laboral deja la cuenta en `BAJA_LABORAL` y conserva el historial. Ante un fallo el diálogo queda abierto con
 * lo escrito y el error traducido; mientras viaja, no se cierra ni se envía dos veces.
 */
@Component({
  selector: 'ap-gestor-de-usuario-interno',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AreaDeTexto, Campo, Dialogo, DialogoDeActo, ErrorDeEnvio, ListaDeOpciones],
  template: `
    @if (usuario(); as u) {
      @if (modo() === 'editar') {
        <ap-dialogo titulo="Editar datos del usuario interno" [textoDeConfirmar]="enviando() ? 'Guardando…' : 'Guardar cambios'" [cargando]="enviando()" [confirmarDeshabilitado]="!valido() || enviando()" [puedeDescartar]="puedeCerrar" [(abierto)]="abiertoEdicion" (confirmar)="guardarEdicion()" (cancelar)="abiertoEdicion.set(false)">
          <p class="quien">{{ u.nombres }} {{ u.apellidos }} · {{ u.correo }}</p>
          <div class="campos">
            <ap-lista-de-opciones etiqueta="Departamento" [opciones]="departamentos()" [(valor)]="departamento" [deshabilitado]="enviando()" />
            <ap-campo etiqueta="Cargo" [(valor)]="cargo" [deshabilitado]="enviando()" [error]="cargo().trim().length < 2 ? 'El cargo tiene que tener al menos 2 caracteres.' : undefined" />
          </div>
          <ap-area-de-texto etiqueta="Motivo (queda en la auditoría)" [(valor)]="motivo" [maximo]="200" [deshabilitado]="enviando()" [ayuda]="t.motivoObligatorio + ' Mínimo 5 caracteres.'" />
          <ap-error-de-envio [error]="error()" />
        </ap-dialogo>
      } @else {
        <ap-dialogo-de-acto [titulo]="titulo()" [textoDeConfirmar]="textoDeConfirmar()" [destructivo]="destructivo()" [efecto]="efecto()" [minimoDeMotivo]="minimoDeMotivo()" [maximoDeMotivo]="maximoDeMotivo()" [accion]="accion()" [(abierto)]="abiertoActo" (completado)="alCompletar($event)">
          <p class="quien">{{ u.nombres }} {{ u.apellidos }} · {{ u.correo }}</p>
        </ap-dialogo-de-acto>
      }
    }
  `,
  styles: `
    .quien { margin: 0 0 var(--s3); font: var(--t-cuerpo-chico); color: var(--text-2); overflow-wrap: anywhere; }
    .campos { display: grid; grid-template-columns: repeat(auto-fit, minmax(min(100%, 14rem), 1fr)); gap: var(--s3); margin-bottom: var(--s3); }
  `,
})
export class GestorDeUsuarioInterno {
  /** El servidor confirmó el cambio: quien aloja recarga sus datos. */
  readonly actualizado = output<void>()
  /** Una frase con lo que pasó, para que quien aloja la muestre (p. ej. «se revocaron 2 roles»). */
  readonly resumen = output<string>()
  protected readonly t = textosSistemas.usuariosInternos
  private readonly actualizar = actualizadorUsuarioInterno()
  private readonly desbloquearUsuario = desbloqueadorUsuarioInterno()
  private readonly accionesDeAccesos = accionesDeAccesos()

  protected readonly usuario = signal<UsuarioInternoResumen | null>(null)
  protected readonly modo = signal<Modo>('editar')
  protected readonly abiertoEdicion = signal(false)
  protected readonly abiertoActo = signal(false)
  protected readonly departamento = signal('')
  protected readonly cargo = signal('')
  protected readonly motivo = signal('')
  protected readonly enviando = signal(false)
  protected readonly error = signal<ErrorParaVista | null>(null)
  protected readonly puedeCerrar = () => !this.enviando()

  private readonly suspende = computed(() => this.modo() === 'estado' && this.usuario()?.estado === 'ACTIVO')
  protected readonly destructivo = computed(() => this.suspende() || this.modo() === 'baja')
  protected readonly titulo = computed(() => {
    const m = this.modo()
    if (m === 'desbloqueo') return 'Desbloquear usuario interno'
    if (m === 'baja') return 'Dar de baja al empleado'
    return this.suspende() ? 'Suspender usuario interno' : 'Reactivar usuario interno'
  })
  protected readonly textoDeConfirmar = computed(() => {
    const m = this.modo()
    if (m === 'desbloqueo') return 'Desbloquear'
    if (m === 'baja') return 'Dar de baja'
    return this.suspende() ? 'Suspender' : 'Reactivar'
  })
  /** Lo que el acto provoca, dicho antes de confirmar. */
  protected readonly efecto = computed(() => {
    const m = this.modo()
    if (m === 'desbloqueo') return 'Se levanta el bloqueo de acceso. Sus roles no cambian.'
    if (m === 'baja') return 'Baja laboral: se revocan todos sus roles, se cierran todas sus sesiones y la cuenta queda en BAJA_LABORAL. No se borra nada: el historial de quién otorgó qué se conserva.'
    return this.suspende()
      ? 'Suspender revoca todos sus roles. Al reactivar la cuenta vuelve sin roles: hay que asignarlos de nuevo, uno por uno.'
      : 'La cuenta vuelve a estar activa sin roles. Después hay que asignarle los que correspondan, uno por uno, desde su ficha.'
  })
  protected readonly minimoDeMotivo = computed(() => (this.modo() === 'baja' ? 10 : MOTIVO_MINIMO))
  protected readonly maximoDeMotivo = computed(() => (this.modo() === 'baja' ? 120 : 200))
  protected readonly departamentos = computed<Opcion[]>(() => {
    const actual = this.usuario()?.departamento
    const todos = actual && !(DEPARTAMENTOS_INTERNOS as readonly string[]).includes(actual) ? [actual, ...DEPARTAMENTOS_INTERNOS] : [...DEPARTAMENTOS_INTERNOS]
    return todos.map((d) => ({ valor: d, texto: d }))
  })
  protected readonly valido = computed(() => this.motivo().trim().length >= MOTIVO_MINIMO && this.cargo().trim().length >= 2 && this.departamento() !== '')

  /** Lo que se hace con la evidencia del segundo factor, según el acto elegido. */
  protected readonly accion = computed<(evidencia: string, motivo: string) => Observable<unknown>>(() => {
    const u = this.usuario()
    const m = this.modo()
    if (!u) return () => {
      throw new Error('Sin usuario elegido')
    }
    if (m === 'desbloqueo') return (evidenciaMfa, motivo) => this.desbloquearUsuario(u.id, motivo, evidenciaMfa)
    if (m === 'baja') return (evidenciaMfa, motivo) => this.accionesDeAccesos.darDeBaja(u.id, { motivo, evidenciaMfa })
    const estado = u.estado === 'ACTIVO' ? EstadoEnum.Suspendido : EstadoEnum.Activo
    return (evidenciaMfa, motivo) => this.actualizar(u.id, { estado, motivo, evidenciaMfa })
  })

  editar(u: UsuarioInternoResumen): void {
    this.abrir(u, 'editar')
  }
  cambiarEstado(u: UsuarioInternoResumen): void {
    this.abrir(u, 'estado')
  }
  desbloquear(u: UsuarioInternoResumen): void {
    this.abrir(u, 'desbloqueo')
  }
  darDeBaja(u: UsuarioInternoResumen): void {
    this.abrir(u, 'baja')
  }

  private abrir(u: UsuarioInternoResumen, modo: Modo): void {
    this.usuario.set(u)
    this.modo.set(modo)
    this.departamento.set(u.departamento)
    this.cargo.set(u.cargo)
    this.motivo.set('')
    this.error.set(null)
    if (modo === 'editar') this.abiertoEdicion.set(true)
    else this.abiertoActo.set(true)
  }

  protected alCompletar(resultado: unknown): void {
    const m = this.modo()
    const u = this.usuario()
    if (m === 'baja') {
      const r = (resultado ?? {}) as ResultadoDeBaja
      this.resumen.emit(r.yaEstabaDeBaja ? 'La cuenta ya estaba dada de baja: no se cambió nada.' : `Baja registrada: se revocaron ${r.asignacionesRevocadas ?? 0} roles y se cerraron ${r.sesionesCerradas ?? 0} sesiones.`)
    } else if (m === 'desbloqueo') this.resumen.emit('Cuenta desbloqueada. Sus roles no cambiaron.')
    else this.resumen.emit(u?.estado === 'ACTIVO' ? 'Cuenta suspendida: se revocaron todos sus roles.' : 'Cuenta reactivada sin roles: asignale los que correspondan, uno por uno.')
    this.actualizado.emit()
  }

  protected guardarEdicion(): void {
    const u = this.usuario()
    if (!u || !this.valido() || this.enviando()) return
    this.enviando.set(true)
    this.error.set(null)
    this.actualizar(u.id, { departamento: this.departamento(), cargo: this.cargo().trim(), motivo: this.motivo().trim() })
      .pipe(map(() => true))
      .subscribe({
        next: () => {
          this.enviando.set(false)
          this.abiertoEdicion.set(false)
          this.resumen.emit('Datos guardados.')
          this.actualizado.emit()
        },
        // Lo escrito queda: el diálogo sigue abierto con el error traducido y el código de seguimiento.
        error: (e: unknown) => {
          this.enviando.set(false)
          this.error.set(mensajeDeError(e, 'No se pudo guardar el cambio. Probá de nuevo.'))
        },
      })
  }
}
