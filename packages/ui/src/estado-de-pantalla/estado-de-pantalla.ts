import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core'
import type { ResourceRef, ResourceStatus } from '@angular/core'
import { EstadoVacio } from '../estado-vacio/estado-vacio'

export type MotivoVacio = 'sinDatos' | 'porFiltro' | 'porPermiso'

/** Lo que el interceptor de errores adjunta: el mensaje ya traducido y la traza. */
export type ErrorTraducido = { mensaje: string; trazaId?: string; sinConexion?: boolean; estado?: number }

/**
 * Contrato de estado real de este organismo (`entregables/contrato-view-state.md`): no hay
 * una unión propia — las ramas se derivan de `ResourceStatus` de Angular (`idle` · `error` ·
 * `loading` · `reloading` · `resolved` · `local`), más el detalle de `ErrorTraducido` para
 * distinguir sin conexión y no encontrado dentro de la rama de error. `RAMA_DE` está tipado
 * `Record<ResourceStatus, …>`: agregar un valor a `ResourceStatus` que falte acá, o sacar uno
 * de los seis existentes, rompe la compilación en vez de fallar en silencio (exhaustividad,
 * H1.S2.M3 — ver `entregables/contrato-view-state.md` §3bis).
 */
export const RAMA_DE: Record<ResourceStatus, 'cargando' | 'obsoleto' | 'error' | 'vacioOListo'> = {
  idle: 'vacioOListo',
  loading: 'cargando',
  reloading: 'obsoleto',
  error: 'error',
  resolved: 'vacioOListo',
  local: 'vacioOListo',
}

/**
 * El único organismo que sabe pintar los estados sobre un `ResourceRef`. Se usa con
 * contenido proyectado para el éxito:
 *
 *   <ap-estado-de-pantalla [recurso]="saldo" [vacio]="esVacio" mensajeVacio="…" (reintentar)="saldo.reload()">
 *     …lo que se muestra cuando hay dato…
 *   </ap-estado-de-pantalla>
 *
 * `reloading` (una recarga en segundo plano, ej. `reintentar` sobre datos que ya se veían)
 * se distingue de `loading` (la primera carga, sin nada que mostrar todavía): si ya hay un
 * valor, se seguía mostrando obsoleto con `aria-busy`, no se lo tapa con el esqueleto.
 */
@Component({
  selector: 'ap-estado-de-pantalla',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [EstadoVacio],
  host: { 'aria-live': 'polite' },
  template: `
    @switch (rama()) {
      @case ('cargando') {
        <div class="esqueleto" role="status" [attr.aria-label]="etiquetaDeCarga()">
          <span></span><span></span><span></span>
        </div>
      }
      @case ('error') {
        <section class="error" role="alert">
          <p class="mensaje">{{ error().mensaje }}</p>
          @if (error().trazaId) { <p class="traza">Código de seguimiento: {{ error().trazaId }}</p> }
          @if (!esNoEncontrado()) {
            <button type="button" (click)="reintentar.emit()">Volver a intentar</button>
          }
        </section>
      }
      @default {
        <div [attr.aria-busy]="esObsoleto() ? 'true' : null">
          @if (estaVacio()) {
            <ap-estado-vacio [motivo]="motivoVacio()" [titulo]="mensajeVacio()" explicacion="" [accionPropia]="accionVacio()" (actuar)="actuarEnVacio.emit()" />
          } @else {
            <ng-content />
          }
        </div>
      }
    }
  `,
  styles: `
    :host { display: block; }
    .esqueleto span { display: block; height: var(--s5); margin-bottom: var(--s3); border-radius: var(--r-sm); background: var(--surface-2); }
    .esqueleto span:nth-child(1) { width: 40%; } .esqueleto span:nth-child(2) { width: 60%; } .esqueleto span:nth-child(3) { width: 30%; }
    .error { padding: var(--s5); text-align: center; }
    .mensaje { color: var(--text); margin: 0 0 var(--s2); }
    .traza { color: var(--text-3); font-size: 0.85em; margin: 0 0 var(--s4); }
    button { min-height: var(--area-tactil); padding: 0 var(--s5); border: 0; border-radius: var(--r-md); background: var(--verde-solido); color: var(--sobre-verde-solido); font: inherit; font-weight: 600; cursor: pointer; }
    button:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
  `,
})
export class EstadoDePantalla<T> {
  readonly recurso = input.required<ResourceRef<T | undefined>>()
  readonly vacio = input<(dato: T) => boolean>()
  readonly mensajeVacio = input.required<string>()
  readonly motivoVacio = input<MotivoVacio>('sinDatos')
  readonly etiquetaDeCarga = input('Cargando')
  /** Acción de siguiente paso del estado vacío; sin ella, `EstadoVacio` la deduce del motivo. */
  readonly accionVacio = input<string>()
  readonly reintentar = output<void>()
  readonly actuarEnVacio = output<void>()

  readonly rama = computed(() => RAMA_DE[this.recurso().status()])
  readonly esObsoleto = computed(() => this.rama() === 'obsoleto')

  readonly estaVacio = computed(() => {
    const r = this.recurso()
    const pred = this.vacio()
    // Sin petición todavía (`idle`) la pantalla está vacía: dice qué hacer, no queda en blanco.
    if (r.status() === 'idle') return true
    // `value()` lanza en estado de error: se pregunta antes, y acá ya no es error.
    if (!r.hasValue() || pred === undefined) return false
    const dato = r.value()
    return dato !== undefined && pred(dato as T)
  })

  readonly error = computed<ErrorTraducido>(() => {
    const e = this.recurso().error() as unknown
    if (e && typeof e === 'object' && 'mensaje' in e) return e as ErrorTraducido
    return { mensaje: 'Algo salió mal de nuestro lado. Probá de nuevo en un momento.' }
  })

  /**
   * `estado` (código HTTP) ya viaja en `ErrorTraducido` desde el interceptor de errores de
   * cada app (`apps/backoffice/src/app/nucleo/errores.interceptor.ts`), que además ya traduce
   * el 404 a «No encontramos lo que buscabas.» (`errores.ts:24`) — este organismo **no**
   * reescribe `mensaje`, solo decide con él si tiene sentido ofrecer reintentar: un recurso
   * que no existe no se arregla reintentando la misma petición.
   */
  readonly esNoEncontrado = computed(() => this.error().estado === 404)
}
