import { ChangeDetectionStrategy, Component, effect, input, model, output, signal } from '@angular/core'
import { Dialogo } from '@aportaya/ui/dialogo/dialogo'
import { ErrorDeEnvio } from '@aportaya/ui/error-de-envio/error-de-envio'
import type { BloqueoEnLista } from 'clientes/angular/nucleo-financiero/model/bloqueoEnLista'
import type { SalidaLevantamientoBloqueo } from 'clientes/angular/nucleo-financiero/model/salidaLevantamientoBloqueo'
import type { ErrorTraducido } from '../../../nucleo/errores'
import { traducirError } from '../compartido/errores-de-cumplimiento'
import { accionesDeBloqueos } from '../dominio/cu17-bloqueos'
import { textosBloqueos } from '../textos-bloqueos'

/**
 * CU-17 · levantar un bloqueo por orden de autoridad (`levantarBloqueoPorAutoridad`). No hay campos: quien
 * levanta es la sesión y el servidor rechaza a quien registró (AP-CU17-04). La confirmación dice la
 * consecuencia; el botón queda ocupado mientras viaja y la clave de idempotencia evita un segundo
 * levantamiento si se reintenta. Nada dice «levantado» antes del 2xx.
 */
@Component({
  selector: 'ap-levantar-bloqueo',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Dialogo, ErrorDeEnvio],
  template: `
    <ap-dialogo [titulo]="t.titulo(orden())" [textoDeConfirmar]="t.confirmar" [destructivo]="true" [abierto]="abierto()" (abiertoChange)="abierto.set($event)" [cargando]="enviando()" (confirmar)="enviar()" (cancelar)="abierto.set(false)">
      <div class="cuerpo">
        <p>{{ t.consecuencia }}</p>
        <p>{{ t.cuatroOjos }}</p>
        <ap-error-de-envio [error]="error()" />
      </div>
    </ap-dialogo>
  `,
  styles: `
    .cuerpo { display: grid; gap: var(--s3); }
    p { margin: 0; }
  `,
})
export class LevantarBloqueo {
  private readonly acciones = accionesDeBloqueos()
  protected readonly t = textosBloqueos.levantamiento

  readonly abierto = model(false)
  readonly bloqueo = input<BloqueoEnLista | null>(null)
  readonly levanto = output<SalidaLevantamientoBloqueo>()

  protected readonly enviando = signal(false)
  protected readonly error = signal<ErrorTraducido | null>(null)

  constructor() {
    // Otro bloqueo, sin el error del anterior: el motivo de un rechazo no se arrastra a otra orden.
    effect(() => {
      this.bloqueo()
      this.error.set(null)
    })
  }

  protected orden(): string {
    const b = this.bloqueo()
    if (!b) return ''
    return `${textosBloqueos.ordenes[b.tipoOrden] ?? b.tipoOrden} de ${textosBloqueos.autoridades[b.autoridad] ?? b.autoridad}`
  }

  async enviar(): Promise<void> {
    const bloqueo = this.bloqueo()
    if (this.enviando() || !bloqueo) return
    this.enviando.set(true)
    this.error.set(null)
    try {
      const salida = await this.acciones.levantar(bloqueo.bloqueoId)
      this.abierto.set(false)
      this.levanto.emit(salida)
    } catch (e) {
      this.error.set(traducirError(e))
    } finally {
      this.enviando.set(false)
    }
  }
}
