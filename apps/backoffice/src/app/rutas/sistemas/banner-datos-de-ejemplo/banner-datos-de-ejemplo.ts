import { ChangeDetectionStrategy, Component, inject } from '@angular/core'
import { ES_FUENTE_SIMULADA } from '../dominio/proveedor-fuentes'

/**
 * H2.S2.M3 — el banner persistente que dice "esto es de ejemplo": visible en las
 * nueve pantallas de `sistemas/` cuando (y solo cuando) `ES_FUENTE_SIMULADA` es
 * `true`. Con la fuente no disponible, este banner **no se renderiza** — el estado de
 * error de `EstadoDePantalla` ya es explícito por su cuenta, y dos avisos pisándose
 * confunden más de lo que aclaran.
 */
@Component({
  selector: 'ap-banner-datos-de-ejemplo',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (esSimulada) {
      <div class="banner" role="status">
        <strong>Datos de ejemplo.</strong>
        <span>Esta sección muestra información sintética para demostración — no son cifras reales.</span>
      </div>
    }
  `,
  styles: `
    .banner {
      display: flex; flex-wrap: wrap; gap: var(--s2);
      padding: var(--s3) var(--s4);
      background: var(--warn-bg);
      color: var(--aviso-texto);
      border-bottom: var(--borde-fino) solid var(--aviso-texto);
    }
    strong { font-weight: 700; }
  `,
})
export class BannerDatosDeEjemplo {
  protected readonly esSimulada = inject(ES_FUENTE_SIMULADA, { optional: true }) ?? false
}
