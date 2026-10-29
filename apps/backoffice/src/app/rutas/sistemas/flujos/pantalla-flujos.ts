import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core'
import { RouterLink } from '@angular/router'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { TarjetaKPI } from '@aportaya/ui/tarjeta-kpi/tarjeta-kpi'
import type { Tono } from '@aportaya/ui/tono/tono'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { ServicioOperaciones } from '../operaciones/dominio/servicio-operaciones'
import { ServicioFlujos } from './dominio/servicio-flujos'

@Component({
  selector: 'ap-pantalla-flujos',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, CabeceraDePagina, ChipEstado, TarjetaKPI],
  template: `
    <section class="pantalla-flujos">
      <ap-cabecera-de-pagina
        eyebrow="Sistemas · Recorridos"
        titulo="Mapa de flujos del sistema"
        descripcion="Catálogo transversal de los ciclos y procesos de AportaYa: cada recorrido encadena operaciones, contratos, precondiciones y todas las bifurcaciones de decisión posibles."
      />

      <div class="kpis">
        <ap-tarjeta-kpi etiqueta="Flujos catalogados" [valor]="'' + flujos().length" icono="libro" tono="info" />
        <ap-tarjeta-kpi etiqueta="Flujos con dinero" [valor]="'' + flujosConDinero()" icono="aporte" tono="ok" />
        <ap-tarjeta-kpi etiqueta="Operaciones enlazadas" [valor]="'' + operaciones().length" icono="verificado" tono="neutro" />
      </div>

      <div class="lista-flujos">
        @for (f of flujos(); track f.id) {
          <article class="tarjeta-flujo">
            <header class="cabecera-flujo">
              <div class="info-principal">
                <span class="codigo-flujo"><code>{{ f.id }}</code></span>
                <h3 class="titulo-flujo">{{ f.nombre }}</h3>
              </div>
              <div class="badges-flujo">
                @if (f.dinero) {
                  <ap-chip-estado tono="ok">Mueve dinero</ap-chip-estado>
                }
                <span class="chip-cus">{{ f.cus.join(' · ') }}</span>
              </div>
            </header>

            <p class="objetivo-flujo">{{ f.objetivo }}</p>

            <div class="pasos-operaciones">
              <h4 class="subtitulo-pasos">Operaciones participantes en este flujo:</h4>
              <div class="grilla-ops">
                @for (op of operacionesPorFlujo(f.id); track op.id) {
                  <a [routerLink]="['/sistemas/operaciones', op.id]" class="card-op-enlace">
                    <div class="op-fila-sup">
                      <ap-chip-estado [tono]="tonoMetodo(op.metodo)">{{ op.metodo }}</ap-chip-estado>
                      <code class="op-ruta">{{ op.ruta }}</code>
                    </div>
                    <span class="op-prop">{{ op.proposito }}</span>
                    <span class="op-decisiones">{{ op.flujo.decisiones.length }} decisiones / respuestas</span>
                  </a>
                }
              </div>
            </div>
          </article>
        }
      </div>
    </section>
  `,
  styles: `
    :host { display: block; width: 100%; min-width: 0; }
    .pantalla-flujos { display: flex; flex-direction: column; gap: var(--s4); width: 100%; }
    .kpis { display: grid; gap: var(--s3); grid-template-columns: repeat(auto-fit, minmax(12rem, 1fr)); }
    .lista-flujos { display: flex; flex-direction: column; gap: var(--s4); }
    .tarjeta-flujo { display: flex; flex-direction: column; gap: var(--s3); padding: var(--s4); background: var(--fondo-tarjeta); border: var(--borde-fino) solid var(--borde-tarjeta); border-radius: var(--r-md); }
    .cabecera-flujo { display: flex; justify-content: space-between; align-items: flex-start; gap: var(--s3); flex-wrap: wrap; }
    .info-principal { display: flex; flex-direction: column; gap: 0.25rem; }
    .codigo-flujo { font-family: var(--f-mono, monospace); font-size: 0.75rem; color: var(--text-apagado); }
    .titulo-flujo { margin: 0; font-size: 1.15rem; font-weight: 600; color: var(--text); }
    .badges-flujo { display: flex; align-items: center; gap: 0.5rem; }
    .chip-cus { font-size: 0.75rem; font-weight: 600; padding: 0.2rem 0.5rem; border-radius: var(--r-sm); background: var(--fondo-control); color: var(--text); }
    .objetivo-flujo { margin: 0; font-size: 0.875rem; line-height: 1.45; color: var(--text-suave); }
    .pasos-operaciones { display: flex; flex-direction: column; gap: var(--s2); }
    .subtitulo-pasos { margin: 0; font-size: 0.8125rem; font-weight: 600; color: var(--text-apagado); text-transform: uppercase; letter-spacing: 0.05em; }
    .grilla-ops { display: grid; gap: var(--s2); grid-template-columns: repeat(auto-fit, minmax(min(100%, 20rem), 1fr)); }
    .card-op-enlace { display: flex; flex-direction: column; gap: 0.35rem; padding: 0.65rem 0.85rem; border-radius: var(--r-sm); background: var(--fondo-control); border: var(--borde-fino) solid var(--borde-control); text-decoration: none; }
    .card-op-enlace:hover { background: var(--fondo-control-hover); border-color: var(--brand-borde); }
    .op-fila-sup { display: flex; align-items: center; gap: 0.5rem; overflow: hidden; }
    .op-ruta { font-size: 0.75rem; color: var(--text); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .op-prop { font-size: 0.75rem; color: var(--text-suave); display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
    .op-decisiones { font-size: 0.7rem; color: var(--brand-texto); font-weight: 600; margin-top: auto; }
  `,
})
export class PantallaFlujos {
  private readonly servicioFlujos = inject(ServicioFlujos)
  private readonly servicioOps = inject(ServicioOperaciones)

  protected readonly flujos = this.servicioFlujos.flujos
  protected readonly operaciones = this.servicioOps.operaciones

  protected readonly flujosConDinero = computed(() => this.flujos().filter((f) => f.dinero).length)

  constructor() {
    inject(ExplicadorDeVista).fijar(
      'Mapa de flujos del sistema: visión holística de los recorridos de negocio de AportaYa con sus operaciones participantes, precondiciones y bifurcaciones de decisión.'
    )
  }

  protected operacionesPorFlujo(flujoId: string) {
    return this.operaciones().filter((op) => op.flujo.id === flujoId)
  }

  protected tonoMetodo(m: string): Tono {
    switch (m) {
      case 'GET': return 'info'
      case 'POST': return 'ok'
      case 'PUT': case 'PATCH': return 'aviso'
      case 'DELETE': return 'error'
      default: return 'neutro'
    }
  }
}
