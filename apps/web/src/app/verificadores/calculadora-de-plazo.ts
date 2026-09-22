import { httpResource } from '@angular/common/http'
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core'
import type { SalidaPlazoHabil } from 'clientes/angular/grupos'
import { EstadoDePantalla } from '@aportaya/ui/estado-de-pantalla/estado-de-pantalla'
import { GATEWAY } from '../nucleo/gateway'

/** La única lectura pública con contrato hoy (CU-59). Con sus cuatro estados. */
@Component({
  selector: 'ap-calculadora-de-plazo',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [EstadoDePantalla],
  template: `
    <form (submit)="$event.preventDefault(); consultar()">
      <label for="desde">Desde el día</label>
      <input id="desde" type="date" [value]="desde()" (input)="desde.set(($any($event.target)).value)" />
      <label for="dias">Días hábiles</label>
      <input id="dias" type="number" min="1" max="60" [value]="dias()" (input)="dias.set(+($any($event.target)).value)" />
      <button type="submit" data-tutorial-id="plazos-calcular">Calcular</button>
    </form>
    <ap-estado-de-pantalla [recurso]="plazo" mensajeVacio="Elegí una fecha para calcular." etiquetaDeCarga="Calculando el plazo" (reintentar)="plazo.reload()">
      @if (plazo.hasValue() && plazo.value(); as p) {
        <p class="resultado" aria-live="polite">Vence el <strong>{{ p.fechaLimite }}</strong>@if (p.diasSalteados.length) { , saltando {{ p.diasSalteados.length }} día(s) no hábil(es)}.</p>
      }
    </ap-estado-de-pantalla>
  `,
  styles: `
    form { display: grid; gap: var(--s2); grid-template-columns: auto 1fr; align-items: center; margin-bottom: var(--s5); }
    input { min-height: var(--area-tactil); padding: 0 var(--s3); border: var(--borde-fino) solid var(--field-border); border-radius: var(--r-md); background: var(--field); color: var(--text); font: inherit; }
    button { grid-column: 1 / -1; min-height: var(--area-tactil); border: 0; border-radius: var(--r-md); background: var(--accent); color: var(--accent-ink); font: inherit; font-weight: 600; }
    .resultado { padding: var(--s4); background: var(--okbg); color: var(--ok-texto); border-radius: var(--r-md); }
  `,
})
export class CalculadoraDePlazo {
  private readonly gateway = inject(GATEWAY)
  protected readonly desde = signal('')
  protected readonly dias = signal(5)
  private readonly consulta = signal<{ desde: string; dias: number } | undefined>(undefined)

  protected readonly plazo = httpResource<SalidaPlazoHabil>(() => {
    const c = this.consulta()
    return c ? { url: `${this.gateway}/grupos/calendario/calcular`, params: { desde: c.desde, dias: c.dias, alcance: 'NACIONAL' } } : undefined
  })

  protected consultar(): void {
    if (this.desde()) this.consulta.set({ desde: this.desde(), dias: this.dias() })
  }
}
