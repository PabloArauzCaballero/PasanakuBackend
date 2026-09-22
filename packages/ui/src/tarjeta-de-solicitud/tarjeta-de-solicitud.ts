import { ChangeDetectionStrategy, Component, input, output, signal } from '@angular/core'
import { Avatar } from '../avatar/avatar'
import { Boton } from '../boton/boton'
import { Campo } from '../campo/campo'
import { Fecha } from '../fecha/fecha'

/**
 * Una persona pide entrar a un grupo. Aceptar es un toque; **rechazar exige motivo**:
 * la persona lo va a leer, y un rechazo sin palabras es una puerta cerrada en la cara.
 */
@Component({
  selector: 'ap-tarjeta-de-solicitud',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Avatar, Boton, Campo, Fecha],
  host: { role: 'article', '[attr.aria-label]': '"Solicitud de " + nombre()' },
  template: `
    <header>
      <ap-avatar [nombre]="nombre()" tamano="lg" />
      <div><p class="nombre">{{ nombre() }}</p><p class="detalle">{{ detalle() }} · <ap-fecha [iso]="cuandoIso()" /></p></div>
    </header>
    @if (rechazando()) {
      <ap-campo etiqueta="Motivo del rechazo (lo va a leer la persona)" [(valor)]="motivo" [error]="motivoFaltante() ? 'Escribí el motivo para poder rechazar' : undefined" />
      <div class="acciones">
        <ap-boton variante="fantasma" (pulsado)="rechazando.set(false)">Volver</ap-boton>
        <ap-boton variante="peligro" (pulsado)="confirmarRechazo()">Rechazar con este motivo</ap-boton>
      </div>
    } @else {
      <div class="acciones">
        <ap-boton variante="fantasma" (pulsado)="rechazando.set(true)">Rechazar</ap-boton>
        <ap-boton variante="secundario" (pulsado)="aceptar.emit()">Aceptar en el grupo</ap-boton>
      </div>
    }
  `,
  styles: `
    :host { display: block; padding: var(--s4); border: var(--borde-fino) solid var(--border); border-radius: var(--r-lg); background: var(--surface); }
    header { display: flex; align-items: center; gap: var(--s3); margin-bottom: var(--s3); }
    p { margin: 0; }
    .nombre { font-weight: 600; color: var(--text); }
    .detalle { color: var(--text-3); font-size: .85em; }
    .acciones { display: flex; flex-wrap: wrap; justify-content: flex-end; gap: var(--s2); margin-top: var(--s3); }
  `,
})
export class TarjetaDeSolicitud {
  readonly nombre = input.required<string>()
  readonly detalle = input.required<string>()
  readonly cuandoIso = input.required<string>()
  readonly aceptar = output<void>()
  readonly rechazar = output<string>()
  readonly rechazando = signal(false)
  readonly motivo = signal('')
  readonly motivoFaltante = signal(false)
  confirmarRechazo(): void {
    const m = this.motivo().trim()
    this.motivoFaltante.set(m.length === 0)
    if (m.length > 0) this.rechazar.emit(m)
  }
}
