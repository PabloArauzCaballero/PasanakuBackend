import { ChangeDetectionStrategy, Component, input, model } from '@angular/core'

export type OpcionDeRadio = { valor: string; texto: string; detalle?: string }

/**
 * Una entre varias, con `fieldset`/`legend`. El elegido lleva relleno de marca en el aro.
 *
 * `variante="tarjetas"` (2026-11-24): cada opción es una tarjeta seleccionable entera, en grilla, para catálogos
 * cortos con detalle (la causal de un caso). Sigue siendo un radio nativo —flechas, espacio y foco del navegador—,
 * declarado `radiogroup` con el nombre de su leyenda. La elegida se distingue por borde, relleno **y** la marca
 * del aro: nunca solo por color.
 */
@Component({
  selector: 'ap-grupo-radio',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '[class.tarjetas]': 'variante() === "tarjetas"' },
  template: `
    <fieldset [attr.role]="variante() === 'tarjetas' ? 'radiogroup' : null" [attr.aria-labelledby]="variante() === 'tarjetas' ? nombre + '-leyenda' : null">
      <legend [id]="nombre + '-leyenda'">{{ etiqueta() }}</legend>
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
    fieldset { border: 0; margin: 0; padding: 0; min-width: 0; }
    legend { font-weight: 600; color: var(--text-2); margin-bottom: var(--s2); }
    label { display: flex; align-items: center; gap: var(--s3); min-height: var(--area-tactil); cursor: pointer; color: var(--text); }
    input { position: absolute; width: var(--borde-fino); height: var(--borde-fino); opacity: 0; }
    .aro { width: var(--s5); height: var(--s5); flex: none; border: var(--borde-desfase) solid var(--borde-control); border-radius: var(--r-pill); background: var(--field); }
    input:checked + .aro { border-color: var(--verde-solido); border-width: calc(var(--s2) - var(--borde-fino)); }
    input:focus-visible + .aro { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
    .texto { display: flex; flex-direction: column; min-width: 0; }
    small { color: var(--text-3); }

    :host(.tarjetas) { container-type: inline-size; display: block; }
    :host(.tarjetas) fieldset { display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s2); }
    :host(.tarjetas) legend { padding: 0; margin-bottom: var(--s3); font: var(--t-cuerpo-fuerte); color: var(--text); }
    :host(.tarjetas) label { position: relative; align-items: flex-start; padding: var(--s3) var(--s4); border: var(--borde-fino) solid var(--field-border); border-radius: var(--r-md); background: var(--surface); transition: border-color var(--mov-micro) var(--curva-llega), background-color var(--mov-micro) var(--curva-llega); }
    :host(.tarjetas) label:hover { border-color: var(--brand-texto); }
    :host(.tarjetas) label:has(input:checked) { border-color: var(--verde-solido); background: var(--brand-bg); box-shadow: inset 0 0 0 var(--borde-fino) var(--verde-solido); }
    :host(.tarjetas) label:has(input:focus-visible) { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
    :host(.tarjetas) input:focus-visible + .aro { outline: none; }
    :host(.tarjetas) .aro { margin-top: var(--borde-desfase); }
    :host(.tarjetas) .texto { gap: var(--s1); font: var(--t-cuerpo-fuerte); }
    :host(.tarjetas) small { font: var(--t-cuerpo-chico); color: var(--text-2); }
    /* Sobre el relleno de marca, el gris secundario no llega a 4,5:1 en oscuro: el detalle pasa al color del texto. */
    :host(.tarjetas) label:has(input:checked) small { color: var(--text); }
    @container (min-width: 36rem) { :host(.tarjetas) fieldset { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
    @media (prefers-reduced-motion: reduce) { :host(.tarjetas) label { transition: none; } }
  `,
})
export class GrupoRadio {
  private static secuencia = 0
  readonly nombre = `ap-radio-${++GrupoRadio.secuencia}`
  readonly etiqueta = input.required<string>()
  readonly opciones = input.required<OpcionDeRadio[]>()
  readonly elegido = model<string | null>(null)
  /** `lista` (por omisión) es la forma de siempre; `tarjetas`, opciones con detalle en grilla. */
  readonly variante = input<'lista' | 'tarjetas'>('lista')
}
