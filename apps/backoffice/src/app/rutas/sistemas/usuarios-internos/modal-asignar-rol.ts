import { ChangeDetectionStrategy, Component, computed, input, output, signal } from '@angular/core'
import { Campo } from '@aportaya/ui/campo/campo'
import { Esqueleto } from '@aportaya/ui/esqueleto/esqueleto'
import { ListaDeOpciones, type Opcion } from '@aportaya/ui/lista-de-opciones/lista-de-opciones'
import { mensajeDeError } from '../compartido/carga'
import { accionesDeAccesos, rolesAsignables, type AmbitoDeRol, type SalidaAsignacionDeRol } from '../dominio/gobierno-de-accesos'
import type { ErrorDeGobierno } from '../dominio/errores-de-gobierno'
import { DialogoDeActo } from './dialogo-de-acto'

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i
const AMBITOS: Opcion[] = [
  { valor: 'GLOBAL', texto: 'Global (toda la plataforma)' },
  { valor: 'GRUPO', texto: 'Un grupo' },
  { valor: 'ORGANIZACION', texto: 'Una organización' },
]

/** El fin de un día (fecha local) como instante ISO; nulo si no hay fecha. */
export function finDelDia(fecha: string): string | null {
  return fecha ? new Date(`${fecha}T23:59:59`).toISOString() : null
}

/**
 * Asignar **un** rol a un operador (CU-08 por asignación, GU-05; decisión de Pablo 2026-11-24): rol, ámbito, vigencia
 * opcional y justificación, más el código del segundo factor (`ap-dialogo-de-acto`). Reemplaza al `PUT .../roles` que
 * pisaba todos los roles de golpe. Solo se ofrecen los roles asignables a personal interno (`GET /roles?asignablesAPersonalInterno=true`):
 * ni participante, ni organizador, ni anunciante, ni `SERVICIO_*`, ni de ámbito GRUPO. Sin fecha de fin, el acceso no vence: se dice.
 */
@Component({
  selector: 'ap-modal-asignar-rol',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Campo, DialogoDeActo, Esqueleto, ListaDeOpciones],
  template: `
    <ap-dialogo-de-acto titulo="Asignar un rol" textoDeConfirmar="Asignar rol" etiquetaDeMotivo="Justificación" [minimoDeMotivo]="10" [maximoDeMotivo]="200" [valido]="esValido()" [accion]="accion" [abierto]="true" (abiertoChange)="!$event && cerrar.emit()" [(error)]="error" (completado)="alCompletar($event)">
      @if (roles.isLoading()) {
        <ap-esqueleto [lineas]="3" etiqueta="Cargando los roles que se pueden asignar" />
      } @else if (roles.error()) {
        <p class="aviso" role="alert">{{ errorDeCatalogo() }}</p>
      } @else {
        <div class="campos">
          <ap-lista-de-opciones etiqueta="Rol" [opciones]="opcionesDeRol()" [(valor)]="rolId" textoVacio="Elegí un rol" [error]="errorDeCampo('rol')" ayuda="Solo se ofrecen roles de personal interno." />
          <ap-lista-de-opciones etiqueta="Ámbito" [opciones]="ambitos" [(valor)]="ambito" [error]="errorDeCampo('ambito')" />
          @if (ambito() === 'GRUPO') {
            <ap-campo etiqueta="Identificador del grupo" [(valor)]="ambitoId" marcador="UUID del grupo" [error]="ambitoId() !== '' && !idValido() ? 'Tiene que ser un identificador (UUID) válido.' : undefined" />
          }
          <ap-campo etiqueta="Vigente hasta (opcional)" tipo="date" [(valor)]="hasta" [error]="errorDeVigencia()" [ayuda]="hasta() ? 'El acceso vence al terminar ese día.' : 'Sin fecha, el acceso no vence.'" />
        </div>
      }
    </ap-dialogo-de-acto>
  `,
  styles: `
    .campos { display: grid; grid-template-columns: repeat(auto-fit, minmax(min(100%, 14rem), 1fr)); gap: var(--s3); }
    .aviso { margin: 0; font: var(--t-cuerpo-chico); color: var(--err-texto); }
  `,
})
export class ModalAsignarRol {
  readonly usuarioId = input.required<string>()
  readonly cerrar = output<void>()
  /** La asignación quedó (o ya existía): quien aloja recarga y cuenta lo que pasó. */
  readonly guardado = output<SalidaAsignacionDeRol>()

  private readonly acciones = accionesDeAccesos()
  protected readonly roles = rolesAsignables()
  protected readonly ambitos = AMBITOS
  protected readonly error = signal<ErrorDeGobierno | null>(null)
  protected readonly rolId = signal('')
  protected readonly ambito = signal<string>('GLOBAL')
  protected readonly ambitoId = signal('')
  protected readonly hasta = signal('')

  protected readonly opcionesDeRol = computed<Opcion[]>(() => (this.roles.hasValue() ? (this.roles.value() ?? []) : []).map((r) => ({ valor: r.id, texto: r.departamento ? `${r.nombre} (${r.departamento})` : r.nombre })))
  protected readonly errorDeCatalogo = computed(() => mensajeDeError(this.roles.error(), 'No se pudo cargar la lista de roles. Cerrá y volvé a abrir.').mensaje)
  protected readonly idValido = computed(() => UUID.test(this.ambitoId().trim()))
  protected readonly errorDeVigencia = computed(() => {
    const fin = finDelDia(this.hasta())
    if (fin && new Date(fin).getTime() <= Date.now()) return 'La fecha de fin tiene que ser futura.'
    return this.error()?.campo === 'vigencia' ? this.error()!.mensaje : undefined
  })
  protected readonly esValido = computed(() => this.rolId() !== '' && (this.ambito() !== 'GRUPO' || this.idValido()) && !this.errorDeVigencia())

  protected errorDeCampo(campo: 'rol' | 'ambito'): string | undefined {
    const e = this.error()
    return e?.campo === campo ? e.mensaje : undefined
  }

  protected readonly accion = (evidenciaMfa: string, justificacion: string) =>
    this.acciones.asignar(this.usuarioId(), {
      rolId: this.rolId(),
      ambito: this.ambito() as AmbitoDeRol,
      ambitoId: this.ambito() === 'GRUPO' ? this.ambitoId().trim() : null,
      vigenteHasta: finDelDia(this.hasta()),
      justificacion,
      evidenciaMfa,
    })

  protected alCompletar(salida: unknown): void {
    this.guardado.emit(salida as SalidaAsignacionDeRol)
  }
}
