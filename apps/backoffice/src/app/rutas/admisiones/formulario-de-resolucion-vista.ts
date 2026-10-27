import { ChangeDetectionStrategy, Component, input, model, output } from '@angular/core'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { Boton } from '@aportaya/ui/boton/boton'
import { Campo } from '@aportaya/ui/campo/campo'
import { GrupoRadio, type OpcionDeRadio } from '@aportaya/ui/grupo-radio/grupo-radio'
import type { ErrorTraducido } from '../../nucleo/errores'
import { textosAdmisiones } from './textos'

export type EnvioResolucion = 'inactivo' | 'enviando' | 'confirmado' | 'sinConfirmar' | 'verificando' | 'noRegistrada' | 'rechazado'

/**
 * Presentacional puro (Dumb): formulario de resolución humana de admisión y sus estados de alerta.
 */
@Component({
  selector: 'ap-formulario-de-resolucion-vista',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Alerta, Boton, Campo, GrupoRadio],
  template: `
    @if (envio() === 'confirmado') {
      <ap-alerta tono="ok">{{ t.confirmada }}</ap-alerta>
    } @else {
      <section aria-labelledby="titulo-resolver">
        <h2 id="titulo-resolver">{{ t.resolverTitulo }}</h2>
        <p class="ayuda">{{ t.resolverAyuda }}</p>

        @if (sinRed()) {
          <ap-alerta tono="aviso">{{ t.sinConexion }}</ap-alerta>
        }
        @if (avisoDeCambio()) {
          <ap-alerta tono="aviso">{{ t.cambio }}</ap-alerta>
        }
        @if (envio() === 'sinConfirmar' || envio() === 'verificando') {
          <ap-alerta tono="aviso" [titulo]="t.sinConfirmar">
            {{ t.sinConfirmarAyuda }}
            <div accion class="acciones">
              <ap-boton
                variante="primario"
                icono="buscar"
                [cargando]="envio() === 'verificando'"
                [deshabilitado]="envio() === 'verificando'"
                (pulsado)="verificar.emit()"
              >
                {{ envio() === 'verificando' ? t.verificando : t.verificar }}
              </ap-boton>
            </div>
          </ap-alerta>
        }
        @if (envio() === 'noRegistrada') {
          <ap-alerta tono="aviso">{{ t.noRegistrada }}</ap-alerta>
        }
        @if (otraPersonaResolvio()) {
          <ap-alerta tono="error">{{ t.otraResolucion }}</ap-alerta>
        }
        @if (errorDelServidor(); as e) {
          <ap-alerta tono="error">{{ e.mensaje }}</ap-alerta>
        }

        <form (submit)="$event.preventDefault(); enviar.emit()" novalidate>
          <fieldset class="bloque" [disabled]="bloqueado()">
            <ap-grupo-radio [etiqueta]="t.decisionEtiqueta" [opciones]="opciones()" [(elegido)]="decision" />
          </fieldset>
          @if (errorDecision()) {
            <p class="error-campo" role="alert">{{ errorDecision() }}</p>
          }
          <ap-campo
            [etiqueta]="t.motivoEtiqueta"
            [ayuda]="t.motivoAyuda"
            [error]="errorMotivo()"
            [deshabilitado]="bloqueado()"
            [(valor)]="motivo"
          />
          <div class="acciones">
            <ap-boton variante="primario" tipo="submit" icono="enviar" [cargando]="enviando()" [deshabilitado]="bloqueado()">
              {{ envio() === 'noRegistrada' ? t.reintentar : enviando() ? t.resolviendo : t.resolver }}
            </ap-boton>
          </div>
        </form>
      </section>
    }
  `,
  styles: `
    section { display: flex; flex-direction: column; gap: var(--s3); }
    section > * { margin: 0; }
    h2 { font-size: 1.1rem; }
    .ayuda { color: var(--text-2); }
    form { display: flex; flex-direction: column; gap: var(--s4); }
    .bloque { border: 0; margin: 0; padding: 0; min-width: 0; }
    .error-campo { margin: 0; color: var(--err-texto); font-size: .875rem; }
    .acciones { display: flex; gap: var(--s2); flex-wrap: wrap; margin-top: var(--s2); }
  `,
})
export class FormularioDeResolucionVista {
  readonly decision = model<string | null>(null)
  readonly motivo = model<string>('')

  readonly errorDecision = input<string>('')
  readonly errorMotivo = input<string>('')
  readonly envio = input<EnvioResolucion>('inactivo')
  readonly sinRed = input<boolean>(false)
  readonly avisoDeCambio = input<boolean>(false)
  readonly otraPersonaResolvio = input<boolean>(false)
  readonly errorDelServidor = input<ErrorTraducido | null>(null)
  readonly bloqueado = input<boolean>(false)
  readonly enviando = input<boolean>(false)
  readonly opciones = input<OpcionDeRadio[]>([])

  readonly enviar = output<void>()
  readonly verificar = output<void>()

  protected readonly t = textosAdmisiones
}
