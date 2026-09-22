import { NgTemplateOutlet } from '@angular/common'
import { ChangeDetectionStrategy, Component, computed, contentChild, input, output, TemplateRef } from '@angular/core'
import type { ResourceRef } from '@angular/core'

export type MotivoVacio = 'sinDatos' | 'porFiltro' | 'porPermiso'

/** Lo que el interceptor de errores adjunta: el mensaje ya traducido y la traza. */
export type ErrorTraducido = { mensaje: string; trazaId?: string; sinConexion?: boolean; estado?: number }

/**
 * El único organismo que sabe pintar los cuatro estados sobre un `ResourceRef`.
 * Se usa con contenido proyectado para el éxito:
 *
 *   <ap-estado-de-pantalla [recurso]="saldo" [vacio]="esVacio" mensajeVacio="…" (reintentar)="saldo.reload()">
 *     …lo que se muestra cuando hay dato…
 *   </ap-estado-de-pantalla>
 */
@Component({
  selector: 'ap-estado-de-pantalla',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [NgTemplateOutlet],
  template: `
    @if (recurso().isLoading()) {
      <div class="esqueleto" role="status" aria-live="polite" [attr.aria-label]="etiquetaDeCarga()">
        <span></span><span></span><span></span>
      </div>
    } @else if (recurso().error()) {
      <section class="error" role="alert" aria-live="assertive">
        @if (plantillaError(); as tpl) {
          <ng-container *ngTemplateOutlet="tpl; context: { $implicit: error() }" />
        } @else {
          <p class="mensaje">{{ error().mensaje }}</p>
          @if (error().trazaId) { <p class="traza">Código de seguimiento: {{ error().trazaId }}</p> }
          <button type="button" (click)="reintentar.emit()">Volver a intentar</button>
        }
      </section>
    } @else if (estaVacio()) {
      <section class="vacio" role="status" aria-live="polite" [class]="'vacio motivo-' + motivoVacio()">
        @if (plantillaVacio(); as tpl) {
          <ng-container *ngTemplateOutlet="tpl; context: { $implicit: motivoVacio() }" />
        } @else {
          <p>{{ mensajeVacio() }}</p>
        }
      </section>
    } @else {
      <ng-content />
    }
  `,
  styles: `
    :host { display: block; }
    .esqueleto span { display: block; height: var(--s5); margin-bottom: var(--s3); border-radius: var(--r-sm); background: var(--surface-2); }
    .esqueleto span:nth-child(1) { width: 40%; } .esqueleto span:nth-child(2) { width: 60%; } .esqueleto span:nth-child(3) { width: 30%; }
    .error, .vacio { padding: var(--s5); text-align: center; }
    .mensaje { color: var(--text); margin: 0 0 var(--s2); }
    .traza { color: var(--text-3); font-size: 0.85em; margin: 0 0 var(--s4); }
    .vacio p { color: var(--text-2); margin: 0; }
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
  readonly reintentar = output<void>()
  /**
   * H2.S1.M1: el contenido de cada rama lo aporta el consumidor por plantilla con contexto
   * tipado. Opcional — si no se proyecta `<ng-template #plantillaVacio let-motivo>`, la rama
   * vacía sigue mostrando `mensajeVacio()` exactamente como antes (cero cambio de apariencia
   * para los 11 consumidores reales que no la usan).
   */
  readonly plantillaVacio = contentChild<TemplateRef<{ $implicit: MotivoVacio }>, TemplateRef<{ $implicit: MotivoVacio }>>('plantillaVacio', { read: TemplateRef })
  /** Ídem para la rama de error; contexto tipado = `ErrorTraducido` completo (nunca el error crudo del backend). */
  readonly plantillaError = contentChild<TemplateRef<{ $implicit: ErrorTraducido }>, TemplateRef<{ $implicit: ErrorTraducido }>>('plantillaError', { read: TemplateRef })

  readonly estaVacio = computed(() => {
    const r = this.recurso()
    const pred = this.vacio()
    // Sin petición todavía (`idle`) la pantalla está vacía: dice qué hacer, no queda en blanco.
    if (r.status() === 'idle') return true
    // `value()` lanza en estado de error: se pregunta antes.
    if (!r.hasValue() || pred === undefined) return false
    const dato = r.value()
    return dato !== undefined && pred(dato as T)
  })

  readonly error = computed<ErrorTraducido>(() => {
    const e = this.recurso().error() as unknown
    if (e && typeof e === 'object' && 'mensaje' in e) return e as ErrorTraducido
    return { mensaje: 'Algo salió mal de nuestro lado. Probá de nuevo en un momento.' }
  })
}
