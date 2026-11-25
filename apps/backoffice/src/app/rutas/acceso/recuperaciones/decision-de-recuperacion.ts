import { ChangeDetectionStrategy, Component, computed, input, output, signal } from '@angular/core'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { AreaDeTexto } from '@aportaya/ui/area-de-texto/area-de-texto'
import { Dialogo } from '@aportaya/ui/dialogo/dialogo'
import type { ErrorTraducido } from '../../../nucleo/errores'
import { crearDecidirRecuperacion, type Decision, type SolicitudDeRecuperacion } from '../dominio/cuenta'
import { errorDeCuenta } from '../dominio/errores-de-cuenta'

export const MOTIVO_MINIMO = 10
export const MOTIVO_MAXIMO = 200
const TEXTOS = {
  aprobacion: {
    titulo: (n: string) => `Aprobar la recuperación de ${n}`,
    confirmar: 'Aprobar recuperación',
    consecuencia: 'Se abre un plazo de espera. Recién al cumplirse se le envía a la persona un enlace de un solo uso. Podés ver cuándo termina apenas apruebes.',
  },
  rechazo: {
    titulo: (n: string) => `Rechazar la recuperación de ${n}`,
    confirmar: 'Rechazar recuperación',
    consecuencia: 'No se emite ningún enlace. La persona tendría que pedir otra recuperación si lo necesita.',
  },
} as const

/**
 * Aprobar o rechazar una recuperación (`aprobarRecuperacion` / `rechazarRecuperacion`, `SEGURIDAD_ACCESO_RESTABLECER`). Modal sobre
 * `ap-dialogo`: rol y nombre, foco atrapado, `Escape` cierra y el foco vuelve a quien lo abrió. El motivo (10 a 200 caracteres) queda
 * en el expediente; el confirmar se bloquea mientras envía y lo escrito se conserva ante cualquier fallo. Que el titular no pueda
 * decidir la suya (`AP-CU09-09`) lo exige el servidor: acá solo se explica.
 */
@Component({
  selector: 'ap-decision-de-recuperacion',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Alerta, AreaDeTexto, Dialogo],
  template: `
    <ap-dialogo [titulo]="t().titulo(nombre())" [textoDeConfirmar]="t().confirmar" [destructivo]="decision() === 'rechazo'" [abierto]="true" [cargando]="enviando()" [hayCambiosSinGuardar]="!!motivo().trim() && !enviando()" (confirmar)="enviar()" (cancelar)="cerrar.emit()">
      <p>{{ t().consecuencia }}</p>
      @if (aviso(); as a) { <ap-alerta tono="error">{{ a }}</ap-alerta> }
      <ap-area-de-texto etiqueta="Motivo de la decisión" ayuda="Queda en el expediente. Entre 10 y 200 caracteres." [maximo]="maximo" [(valor)]="motivo" [error]="errorMotivo()" [deshabilitado]="enviando()" />
    </ap-dialogo>
  `,
})
export class DecisionDeRecuperacion {
  private readonly decidir = crearDecidirRecuperacion()
  readonly solicitud = input.required<SolicitudDeRecuperacion>()
  readonly decision = input.required<Decision>()
  readonly decidida = output<SolicitudDeRecuperacion>()
  readonly cerrar = output<void>()
  protected readonly maximo = MOTIVO_MAXIMO
  protected readonly motivo = signal('')
  protected readonly enviando = signal(false)
  protected readonly errorMotivo = signal<string | undefined>(undefined)
  protected readonly aviso = signal<string | null>(null)
  protected readonly t = computed(() => TEXTOS[this.decision()])
  protected readonly nombre = computed(() => `${this.solicitud().nombres} ${this.solicitud().apellidos}`)

  protected enviar(): void {
    if (this.enviando()) return
    const motivo = this.motivo().trim()
    this.errorMotivo.set(motivo.length < MOTIVO_MINIMO || motivo.length > MOTIVO_MAXIMO ? `El motivo tiene entre ${MOTIVO_MINIMO} y ${MOTIVO_MAXIMO} caracteres: contá por qué decidís así.` : undefined)
    if (this.errorMotivo()) return
    this.enviando.set(true)
    this.aviso.set(null)
    this.decidir(this.solicitud().id, this.decision(), { motivo }).subscribe({
      next: (s) => { this.enviando.set(false); this.decidida.emit(s) },
      error: (e: ErrorTraducido) => { this.enviando.set(false); this.aviso.set(errorDeCuenta(e).mensaje) },
    })
  }
}
