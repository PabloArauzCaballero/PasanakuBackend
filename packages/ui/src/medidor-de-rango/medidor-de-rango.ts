import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core'

/**
 * Dónde cae un valor dentro de un rango con zonas (bajo/medio/alto). Un `meter` nativo
 * detrás para el lector; encima, la aguja sobre las zonas del tema.
 */
@Component({
  selector: 'ap-medidor-de-rango',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <p class="etiqueta">{{ etiqueta() }} <strong>{{ zona() }}</strong></p>
    <meter [min]="minimo()" [max]="maximo()" [value]="valor()" [low]="bajo()" [high]="alto()" [attr.aria-label]="etiqueta() + ': ' + zona()">{{ valor() }}</meter>
    <div class="zonas" aria-hidden="true">
      <span class="baja"></span><span class="media"></span><span class="alta"></span>
      <span class="aguja" [style.left.%]="posicion()"></span>
    </div>
    <p class="extremos" aria-hidden="true"><span>{{ nombreBajo() }}</span><span>{{ nombreAlto() }}</span></p>
  `,
  styles: `
    :host { display: block; }
    p { margin: 0; }
    .etiqueta { color: var(--text-2); font-weight: 600; margin-bottom: var(--s2); }
    strong { color: var(--text); }
    meter { position: absolute; width: var(--borde-fino); height: var(--borde-fino); opacity: 0; }
    .zonas { position: relative; display: flex; height: var(--s2); border-radius: var(--r-pill); overflow: visible; }
    .zonas > span:not(.aguja) { flex: 1; }
    .baja { background: var(--ok); border-radius: var(--r-pill) 0 0 var(--r-pill); }
    .media { background: var(--warn); }
    .alta { background: var(--err); border-radius: 0 var(--r-pill) var(--r-pill) 0; }
    .aguja { position: absolute; top: calc(var(--s1) * -1); width: var(--s1); height: var(--s4); border-radius: var(--r-pill); background: var(--ink); transform: translateX(-50%); box-shadow: 0 0 0 var(--borde-desfase) var(--surface); }
    .extremos { display: flex; justify-content: space-between; margin-top: var(--s1); color: var(--text-3); font-size: .8em; }
  `,
})
export class MedidorDeRango {
  readonly etiqueta = input.required<string>()
  readonly valor = input.required<number>()
  readonly minimo = input(0)
  readonly maximo = input(100)
  readonly bajo = input(33)
  readonly alto = input(66)
  readonly nombreBajo = input('Bajo')
  readonly nombreAlto = input('Alto')
  readonly posicion = computed(() => (Math.min(this.maximo(), Math.max(this.minimo(), this.valor())) - this.minimo()) / (this.maximo() - this.minimo()) * 100)
  readonly zona = computed(() => (this.valor() < this.bajo() ? this.nombreBajo().toLowerCase() : this.valor() < this.alto() ? 'medio' : this.nombreAlto().toLowerCase()))
}
