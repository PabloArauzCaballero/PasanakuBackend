import { ChangeDetectionStrategy, Component, computed, input, model } from '@angular/core'
import { Boton } from '@aportaya/ui/boton/boton'
import { CajonLateral } from '@aportaya/ui/cajon-lateral/cajon-lateral'
import { SeccionClaveValor, type ClaveValor } from '@aportaya/ui/seccion-clave-valor/seccion-clave-valor'
import type { EventoBitacora } from '../dominio/bitacora'
import { textosSistemas } from '../textos'
import { formatearFecha } from '@aportaya/ui/fecha/fecha'

/**
 * Cajón lateral con el detalle exhaustivo de un evento de bitácora (comun.bitacora_evento):
 * Metadatos inmutables, actor, origen, hashes encadenados y visor de cambio JSON (valorAnterior -> valorNuevo).
 */
@Component({
  selector: 'ap-cajon-detalle-evento',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Boton, CajonLateral, SeccionClaveValor],
  template: `
    <ap-cajon-lateral [titulo]="titulo()" [(abierto)]="abierto">
      @if (evento(); as e) {
        <div class="cuerpo">
          <ap-seccion-clave-valor titulo="Metadatos del evento" [items]="metadatos()" />

          <div class="seccion-valores">
            <h3 class="titulo-valores">Cambios sobre los datos</h3>
            @if (e.valorAnterior || e.valorNuevo) {
              <div class="grilla-json">
                @if (e.valorAnterior) {
                  <div class="tarjeta-json">
                    <span class="subtitulo-json">Valor anterior</span>
                    <pre class="codigo-json">{{ jsonFormateado(e.valorAnterior) }}</pre>
                  </div>
                }
                @if (e.valorNuevo) {
                  <div class="tarjeta-json">
                    <span class="subtitulo-json">Valor nuevo</span>
                    <pre class="codigo-json">{{ jsonFormateado(e.valorNuevo) }}</pre>
                  </div>
                }
              </div>
            } @else {
              <p class="sin-cambios">{{ t.sinCambios }}</p>
            }
          </div>
        </div>
      }
      <ap-boton acciones variante="fantasma" (pulsado)="abierto.set(false)">Cerrar</ap-boton>
    </ap-cajon-lateral>
  `,
  styles: `
    .cuerpo { display: flex; flex-direction: column; gap: var(--s4); }
    .seccion-valores { display: flex; flex-direction: column; gap: var(--s2); }
    .titulo-valores { margin: 0; font: var(--t-cuerpo-fuerte); color: var(--text); }
    .grilla-json { display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s3); }
    .tarjeta-json { display: flex; flex-direction: column; gap: var(--s1); background: var(--surface-2); border: var(--borde-fino) solid var(--border); border-radius: var(--r-md); padding: var(--s3); }
    .subtitulo-json { font: var(--t-cuerpo-chico); font-weight: 600; color: var(--text-2); }
    .codigo-json { margin: 0; font-family: var(--f-mono); font-size: 0.8em; line-height: 1.4; color: var(--text); overflow-x: auto; max-height: 18rem; white-space: pre-wrap; word-break: break-word; }
    .sin-cambios { margin: 0; font: var(--t-cuerpo-chico); color: var(--text-3); }
  `,
})
export class CajonDetalleEvento {
  readonly evento = input<EventoBitacora | null>(null)
  readonly abierto = model(false)
  protected readonly t = textosSistemas.bitacora

  protected readonly titulo = computed(() => {
    const e = this.evento()
    if (!e) return this.t.detalleTitulo
    return `${this.t.detalleTitulo} · #${e.secuencia}`
  })

  protected readonly metadatos = computed<readonly ClaveValor[]>(() => {
    const e = this.evento()
    if (!e) return []
    return [
      { etiqueta: 'ID Evento', valor: e.id, mono: true },
      { etiqueta: 'Secuencia', valor: `#${e.secuencia}` },
      { etiqueta: 'Fecha y hora', valor: formatearFecha(e.fechaHora) },
      { etiqueta: 'Entidad', valor: e.entidad },
      { etiqueta: 'ID Entidad', valor: e.entidadId, mono: true },
      { etiqueta: 'Acción', valor: e.accion },
      { etiqueta: 'Origen', valor: e.origen },
      { etiqueta: 'Actor (Rol)', valor: e.actorRol ?? '—' },
      { etiqueta: 'Actor (Usuario ID)', valor: e.actorUsuarioId ?? '—', mono: Boolean(e.actorUsuarioId) },
      { etiqueta: 'IP Origen', valor: e.ipOrigen ?? '—' },
      { etiqueta: 'Agente de usuario', valor: e.agenteUsuario ?? '—' },
      { etiqueta: 'Correlation ID', valor: e.correlationId, mono: true },
      { etiqueta: 'Motivo', valor: e.motivo ?? '—' },
      { etiqueta: 'Campos modificados', valor: e.camposModificados ?? '—' },
      { etiqueta: 'Hash del registro', valor: e.hashRegistro, mono: true },
      { etiqueta: 'Hash anterior', valor: e.hashAnterior ?? '—', mono: Boolean(e.hashAnterior) },
    ]
  })

  protected jsonFormateado(obj: unknown): string {
    if (!obj) return ''
    try {
      return typeof obj === 'string' ? obj : JSON.stringify(obj, null, 2)
    } catch {
      return String(obj)
    }
  }
}
