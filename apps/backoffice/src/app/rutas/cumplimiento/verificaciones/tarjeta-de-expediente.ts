import { ChangeDetectionStrategy, Component, input, output } from '@angular/core'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { ErrorDeEnvio } from '@aportaya/ui/error-de-envio/error-de-envio'
import { Icono } from '@aportaya/ui/icono/icono'
import { CaraDelExpediente, type DecisionDeVerificacionDecisionEnum, type ExpedienteEnRevision } from 'clientes/angular/identidad'
import type { ErrorTraducido } from '../../../nucleo/errores'
import { esPendiente, tonoDelExpediente, vencimientoDe } from '../dominio/cu02-expedientes'
import { textosCumplimiento } from '../textos'
import { DecisionDeExpediente, type Decision } from './decision-de-expediente'
import { TiraDeFotos } from './tira-de-fotos'
import { VencimientoDelDocumento } from './vencimiento-del-documento'

const CARAS = Object.values(CaraDelExpediente)

/**
 * Un expediente de la cola (CU-02) como tarjeta: los datos de contacto con su ícono, el estado, la tira de fotos y,
 * si espera una persona, la decisión. Presentación pura: la decisión sale como intención y la pantalla la envía.
 */
@Component({
  selector: 'ap-tarjeta-de-expediente',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ChipEstado, ErrorDeEnvio, Icono, TiraDeFotos, DecisionDeExpediente, VencimientoDelDocumento],
  template: `
    @let e = expediente();
    <header>
      <div class="titular">
        <h2><ap-icono nombre="persona" tamano="s4" />{{ e.nombreCompleto }}</h2>
        <ul class="datos">
          <li class="telefono"><ap-icono nombre="telefono" tamano="s4" /><span>{{ t.telefono }}: {{ e.telefono }}</span></li>
          <li class="correo"><ap-icono nombre="correo" tamano="s4" /><span>{{ t.correo }}: {{ e.correo || t.sinCorreo }}</span></li>
          <li class="doc"><ap-icono nombre="documento" tamano="s4" /><span>{{ e.documento ?? t.sinDocumento }}</span></li>
        </ul>
        <ap-vencimiento-del-documento [expediente]="e" />
      </div>
      <ap-chip-estado [tono]="tono(e)">{{ t.estados[e.estado] ?? e.estado }}</ap-chip-estado>
    </header>
    <ap-tira-de-fotos [expediente]="e" />
    @if (pendiente(e)) {
      <ap-decision-de-expediente [verificacionId]="e.verificacionId" [completo]="completo(e)" [vencimiento]="vencimiento(e)" [ocupado]="ocupado()" [enCurso]="enCurso()" (decidir)="decidir.emit($event)" />
      <ap-error-de-envio [error]="error()" />
    } @else if (e.motivoRechazo) {
      <p class="aviso"><ap-icono nombre="advertencia" tamano="s4" />{{ t.motivo }}: {{ e.motivoRechazo }}</p>
    }
  `,
  styles: `
    :host { display: flex; flex-direction: column; gap: var(--s3); min-width: 0; padding: var(--s4); border: var(--borde-fino) solid var(--border); border-radius: var(--r-lg); background: var(--surface); box-shadow: var(--sombra-1); }
    header { display: flex; justify-content: space-between; align-items: flex-start; gap: var(--s3); flex-wrap: wrap; }
    .titular { display: grid; gap: var(--s2); min-width: 0; }
    h2 { display: flex; align-items: center; gap: var(--s2); margin: 0; font: var(--t-titulo-3); overflow-wrap: anywhere; }
    .datos { display: flex; flex-wrap: wrap; gap: var(--s1) var(--s4); margin: 0; padding: 0; list-style: none; color: var(--text-2); font: var(--t-cuerpo-chico); }
    .datos li { display: inline-flex; align-items: center; gap: var(--s1); min-width: 0; overflow-wrap: anywhere; }
    .aviso { display: flex; align-items: center; gap: var(--s1); margin: 0; color: var(--aviso-texto); font: var(--t-cuerpo-chico); }
  `,
})
export class TarjetaDeExpediente {
  protected readonly t = textosCumplimiento.verificaciones
  readonly expediente = input.required<ExpedienteEnRevision>()
  readonly ocupado = input(false)
  readonly enCurso = input<DecisionDeVerificacionDecisionEnum | null>(null)
  readonly error = input<ErrorTraducido | null>(null)
  readonly decidir = output<Decision>()
  protected readonly pendiente = esPendiente
  protected readonly tono = tonoDelExpediente
  protected readonly vencimiento = vencimientoDe
  /** Sin las cinco fotos no se aprueba: aprobar a ciegas es no revisar. */
  protected readonly completo = (e: ExpedienteEnRevision) => CARAS.every((c) => e.fotos.includes(c))
}
