import { ChangeDetectionStrategy, Component, input } from '@angular/core'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import type { Tono } from '@aportaya/ui/tono/tono'
import type { DecisionOperacion } from './dominio/tipos'

@Component({
  selector: 'ap-seccion-arbol-decisiones',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ChipEstado],
  template: `
    <section class="seccion-decisiones">
      <div class="intro">
        <h3 class="titulo-sec">Árbol de decisiones y bifurcaciones posibles</h3>
        <p class="desc-sec">Todas las respuestas previstas por el contrato para esta operación, las condiciones que las disparan y la transición que sigue en el flujo de negocio.</p>
      </div>

      <div class="arbol">
        @for (d of decisiones(); track d.codigo) {
          <div class="nodo" [class.exito]="d.http === 200 || d.http === 201" [class.error]="d.http === 422" [class.seguridad]="d.http === 401 || d.http === 403">
            <div class="nodo-header">
              <span class="codigo-chip"><code>{{ d.codigo }}</code></span>
              @if (d.http) {
                <ap-chip-estado [tono]="tonoHttp(d.http)">HTTP {{ d.http }}</ap-chip-estado>
              }
            </div>
            <div class="nodo-cuerpo">
              <strong class="etiqueta-condicion">Condición disparadora:</strong>
              <p class="texto-condicion">{{ d.condicion }}</p>
              <strong class="etiqueta-siguiente">Desenlace / Siguiente paso:</strong>
              <p class="texto-siguiente">{{ d.siguiente }}</p>
            </div>
          </div>
        }
      </div>
    </section>
  `,
  styles: `
    :host { display: block; width: 100%; }
    .seccion-decisiones { display: flex; flex-direction: column; gap: var(--s3); }
    .intro { display: flex; flex-direction: column; gap: 0.25rem; }
    .titulo-sec { margin: 0; font-size: 1.05rem; font-weight: 600; color: var(--text); }
    .desc-sec { margin: 0; font-size: 0.8125rem; color: var(--text-suave); }
    .arbol { display: flex; flex-direction: column; gap: var(--s3); }
    .nodo { display: flex; flex-direction: column; gap: var(--s2); padding: var(--s3); border-radius: var(--r-md); background: var(--fondo-tarjeta); border: var(--borde-fino) solid var(--borde-tarjeta); border-left: var(--s1) solid var(--borde-control); }
    .nodo.exito { border-left-color: var(--color-ok, var(--g400)); }
    .nodo.error { border-left-color: var(--color-aviso, var(--o500)); }
    .nodo.seguridad { border-left-color: var(--color-peligro, var(--o700)); }
    .nodo-header { display: flex; justify-content: space-between; align-items: center; gap: var(--s2); }
    .codigo-chip { font-family: var(--f-mono, monospace); font-weight: 700; font-size: 0.875rem; color: var(--text); }
    .nodo-cuerpo { display: grid; grid-template-columns: auto 1fr; gap: 0.35rem 0.75rem; align-items: baseline; font-size: 0.8125rem; }
    .etiqueta-condicion { color: var(--text-apagado); font-size: 0.75rem; font-weight: 600; }
    .texto-condicion { margin: 0; color: var(--text); font-weight: 500; }
    .etiqueta-siguiente { color: var(--text-apagado); font-size: 0.75rem; font-weight: 600; }
    .texto-siguiente { margin: 0; color: var(--brand-texto); }
  `,
})
export class SeccionArbolDecisiones {
  readonly decisiones = input.required<readonly DecisionOperacion[]>()

  protected tonoHttp(status: number): Tono {
    if (status >= 200 && status < 300) return 'ok'
    if (status === 422) return 'aviso'
    if (status === 401 || status === 403) return 'error'
    return 'info'
  }
}
