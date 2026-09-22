import { httpResource } from '@angular/common/http'
import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core'
import type { SalidaVerificacionCadena } from 'clientes/angular/transparencia'
import { EstadoDePantalla } from '@aportaya/ui/estado-de-pantalla/estado-de-pantalla'
import { GATEWAY } from '../nucleo/gateway'

/**
 * CU-73 — Verificar la cadena de transparencia.
 *
 * Fuente de verdad: `GET /publico/grupos/:codigo/verificacion` (backend real, no
 * simulado — `servicios/transparencia` ya publica este contrato). El recómputo
 * íntegro en el navegador con `@aportaya/dominio-cliente` (`recorrerCadena`) queda
 * documentado como pendiente en `packages/dominio-cliente/src/cadena.ts`: esos
 * átomos todavía no tienen equivalente Java fijado bloque a bloque, así que
 * mostrarlo acá como "verificado" sería inventar una garantía que no existe. Se
 * muestra el veredicto del servidor, que sí es real.
 */
@Component({
  selector: 'ap-verificador-de-cadena',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [EstadoDePantalla],
  template: `
    <ap-estado-de-pantalla [recurso]="veredicto" mensajeVacio="No hay nada para verificar." etiquetaDeCarga="Recorriendo la cadena de bloques" (reintentar)="veredicto.reload()">
      @if (veredicto.hasValue() && veredicto.value(); as v) {
        <p role="status" [class.ok]="v.integra" [class.mal]="!v.integra">
          @if (v.integra) {
            Cadena íntegra: se verificaron {{ v.bloquesVerificados }} bloque(s) sin ninguna discrepancia.
          } @else {
            Cadena rota desde el bloque {{ v.primerBloqueFallido }}@if (v.componenteFallido) { ({{ v.componenteFallido }})}. Esto abre un incidente operativo de forma automática.
          }
        </p>
        @if (v.ultimoSellado) {
          <p class="ayuda">Último bloque sellado: {{ v.ultimoSellado }}</p>
        }
        <p class="ayuda">
          El recómputo íntegro en este navegador (independiente del servidor) todavía no está disponible: los átomos
          <code>serializarCanonico</code> y <code>hashDeBloque</code> aún no tienen una implementación de referencia publicada por el backend
          (hueco declarado en el informe del carril). Este veredicto es el que calculó <code>servicios/transparencia</code>.
        </p>
      }
    </ap-estado-de-pantalla>
  `,
  styles: `
    .ok { color: var(--ok-texto); }
    .mal { color: var(--danger-texto, var(--error-texto)); }
    .ayuda { color: var(--text-muted); font-size: 0.9rem; }
  `,
})
export class VerificadorDeCadena {
  private readonly gateway = inject(GATEWAY)
  readonly codigoGrupo = input.required<string>()

  protected readonly veredicto = httpResource<SalidaVerificacionCadena>(() => ({
    url: `${this.gateway}/publico/grupos/${this.codigoGrupo()}/verificacion`,
  }))
}
