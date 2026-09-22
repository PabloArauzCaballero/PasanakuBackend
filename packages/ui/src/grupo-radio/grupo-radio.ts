import { ChangeDetectionStrategy, Component, input, model } from '@angular/core'

export type OpcionDeRadio = { valor: string; texto: string; detalle?: string }

/** Una entre varias, con `fieldset`/`legend`. El elegido lleva relleno de marca en el aro. */
@Component({
  selector: 'ap-grupo-radio',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <fieldset>
      <legend>{{ etiqueta() }}</legend>
      @for (o of opciones(); track o.valor) {
        <label>
          <input type="radio" [name]="nombre" [value]="o.valor" [checked]="elegido() === o.valor" (change)="elegido.set(o.valor)" />
          <span class="aro" aria-hidden="true"></span>
          <span class="texto">{{ o.texto }} @if (o.detalle) { <small>{{ o.detalle }}</small> }</span>
        </label>
      }
    </fieldset>
  `,
  styles: `
    fieldset { border: 0; margin: 0; padding: 0; }
    legend { font-weight: 600; color: var(--text-2); margin-bottom: var(--s2); }
    label { display: flex; align-items: center; gap: var(--s3); min-height: var(--area-tactil); cursor: pointer; color: var(--text); }
    input { position: absolute; width: var(--borde-fino); height: var(--borde-fino); opacity: 0; }
    .aro { width: var(--s5); height: var(--s5); flex: none; border: var(--borde-desfase) solid var(--field-border); border-radius: var(--r-pill); background: var(--field); }
    input:checked + .aro { border-color: var(--verde-solido); border-width: calc(var(--s2) - var(--borde-fino)); }
    input:focus-visible + .aro { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
    .texto { display: flex; flex-direction: column; }
    small { color: var(--text-3); }
  `,
})
export class GrupoRadio {
  private static secuencia = 0
  readonly nombre = `ap-radio-${++GrupoRadio.secuencia}`
  readonly etiqueta = input.required<string>()
  readonly opciones = input.required<OpcionDeRadio[]>()
  readonly elegido = model<string | null>(null)
}
