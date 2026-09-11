import { httpResource } from '@angular/common/http'
import { ChangeDetectionStrategy, Component, computed, inject, input, resource } from '@angular/core'
import type { SalidaVerificacionSorteo } from 'clientes/angular/transparencia'
import { barajarDeterminista, verificarCompromiso } from '@aportaya/dominio-cliente'
import { EstadoDePantalla } from '@aportaya/ui/estado-de-pantalla/estado-de-pantalla'
import { GATEWAY } from '../nucleo/gateway'

/**
 * CU-61 — Verificar públicamente el sorteo.
 *
 * Muestra el veredicto del servidor y, además, **recomputa en el navegador** con
 * `@aportaya/dominio-cliente` (los mismos átomos que usa el backend, ver planes/14
 * F9.3): el punto de esta página es que no haga falta creernos.
 */
@Component({
  selector: 'ap-verificador-de-sorteo',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [EstadoDePantalla],
  template: `
    <ap-estado-de-pantalla [recurso]="paquete" mensajeVacio="No hay nada para verificar." etiquetaDeCarga="Consultando el paquete del sorteo" (reintentar)="paquete.reload()">
      @if (paquete.hasValue() && paquete.value(); as p) {
        <section aria-labelledby="veredicto-servidor">
          <h2 id="veredicto-servidor">Veredicto del servidor</h2>
          <p role="status" [class.ok]="p.verifica && p.ordenCoincide" [class.mal]="!(p.verifica && p.ordenCoincide)">
            @if (p.verifica && p.ordenCoincide) {
              Coincide: el hash comprometido y el orden publicado son los que resultan de la semilla revelada.
            } @else {
              No coincide@if (p.primerCupoDiscrepante !== undefined) {, desde el cupo {{ p.primerCupoDiscrepante }}}. Esto es un incidente operativo, no un problema de quien verifica.
            }
          </p>
        </section>

        <section aria-labelledby="veredicto-cliente">
          <h2 id="veredicto-cliente">Recómputo en este navegador</h2>
          @if (veredictoCliente.isLoading()) {
            <p role="status">Recomputando con el mismo algoritmo que el backend…</p>
          } @else if (veredictoCliente.hasValue() && veredictoCliente.value(); as v) {
            <p role="status" [class.ok]="v.hashCoincide" [class.mal]="!v.hashCoincide">
              {{ v.hashCoincide ? 'El hash comprometido se reprodujo de forma independiente en este navegador.' : 'El navegador NO reprodujo el hash comprometido.' }}
            </p>
            <p class="ayuda">
              Orden recomputado a partir de la semilla: comparalo con el orden de turnos que muestra la página del grupo.
            </p>
            <ol class="orden">
              @for (cupo of v.orden; track cupo) { <li>Cupo {{ cupo }}</li> }
            </ol>
          }
        </section>

        <details>
          <summary>Paquete público (JSON, para verificar con tu propio código)</summary>
          <pre>{{ paqueteComoTexto() }}</pre>
        </details>
      }
    </ap-estado-de-pantalla>
  `,
  styles: `
    section { margin-bottom: var(--s5); }
    h2 { font-size: 1rem; margin-bottom: var(--s2); }
    .ok { color: var(--ok-texto); }
    .mal { color: var(--danger-texto, var(--error-texto)); }
    pre { white-space: pre-wrap; word-break: break-all; background: var(--surface-2); padding: var(--s4); border-radius: var(--r-md); font-size: 0.8rem; }
    details { margin-top: var(--s5); }
    .ayuda { color: var(--text-muted); font-size: 0.9rem; }
    .orden { display: flex; flex-wrap: wrap; gap: var(--s2); list-style: none; padding: 0; }
    .orden li { padding: var(--s2) var(--s3); background: var(--surface-2); border-radius: var(--r-sm); }
  `,
})
export class VerificadorDeSorteo {
  private readonly gateway = inject(GATEWAY)
  readonly sorteoId = input.required<string>()

  protected readonly paquete = httpResource<SalidaVerificacionSorteo>(() => ({
    url: `${this.gateway}/publico/sorteos/${this.sorteoId()}/verificacion`,
  }))

  protected readonly paqueteComoTexto = computed(() => {
    const p = this.paquete.value()
    return p ? JSON.stringify(p.paquete, null, 2) : ''
  })

  protected readonly veredictoCliente = resource({
    params: () => this.paquete.value(),
    loader: async ({ params }) => {
      if (!params) return undefined
      const { semilla, entropias, cupos } = params.paquete
      const hashCoincide = await verificarCompromiso(semilla, entropias, params.hashEsperado)
      const orden = await barajarDeterminista(semilla, cupos)
      return { hashCoincide, orden }
    },
  })
}
