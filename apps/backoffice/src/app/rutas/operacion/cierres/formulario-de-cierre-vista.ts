import { ChangeDetectionStrategy, Component, input, model, output } from '@angular/core'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { Boton } from '@aportaya/ui/boton/boton'
import { Campo } from '@aportaya/ui/campo/campo'
import { textosCierres } from './textos'

/**
 * Presentacional puro (Dumb): lista de prerrequisitos, campo de fecha y botón de envío.
 */
@Component({
  selector: 'ap-formulario-de-cierre-vista',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Alerta, Boton, Campo],
  template: `
    <ul class="requisitos">
      @for (r of requisitos(); track r) {
        <li>{{ r }}</li>
      }
    </ul>
    @if (aviso()) {
      <ap-alerta [tono]="tonoAviso()">{{ aviso() }}</ap-alerta>
    }
    <form (submit)="$event.preventDefault(); solicitarRevision.emit()" novalidate>
      <ap-campo
        tipo="date"
        [etiqueta]="t.fecha"
        [error]="errorFecha()"
        [deshabilitado]="deshabilitado()"
        [(valor)]="fecha"
      />
      <div>
        <ap-boton variante="primario" tipo="submit" icono="candado" [deshabilitado]="deshabilitado()">
          {{ t.revisar }}
        </ap-boton>
      </div>
    </form>
  `,
  styles: `
    :host { display: grid; gap: var(--s3); }
    .requisitos { margin: 0; padding-left: var(--s5); color: var(--text-2); display: grid; gap: var(--s1); }
    form { display: grid; gap: var(--s3); align-content: start; }
    /* A lo ancho de la tarjeta: los requisitos a un lado y el día al otro, sin una columna vacía. */
    @media (min-width: 48rem) { :host { grid-template-columns: 3fr 2fr; column-gap: var(--s5); } ap-alerta { grid-column: 1 / -1; order: -1; } }
  `,
})
export class FormularioDeCierreVista {
  readonly fecha = model<string>('')
  readonly errorFecha = input<string>('')
  readonly deshabilitado = input<boolean>(false)
  readonly aviso = input<string>('')
  readonly tonoAviso = input<'aviso' | 'error'>('error')
  readonly requisitos = input<readonly string[]>(textosCierres.formulario.requisitos)

  readonly solicitarRevision = output<void>()

  protected readonly t = textosCierres.formulario
}
