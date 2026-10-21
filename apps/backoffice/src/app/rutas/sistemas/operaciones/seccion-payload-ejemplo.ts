import { ChangeDetectionStrategy, Component, computed, input, signal } from '@angular/core'
import { Boton } from '@aportaya/ui/boton/boton'

@Component({
  selector: 'ap-seccion-payload-ejemplo',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Boton],
  template: `
    <section class="seccion-payload">
      <div class="bloque-codigo">
        <div class="codigo-cabecera">
          <span class="titulo-payload">Payload de entrada (JSON de ejemplo)</span>
          <ap-boton variante="secundario" tamano="sm" (pulsado)="copiarEntrada()">
            {{ copiadoEntrada() ? '¡Copiado!' : 'Copiar JSON' }}
          </ap-boton>
        </div>
        <pre class="visor-json"><code>{{ jsonEntrada() }}</code></pre>
      </div>

      @if (payloadRespuesta()) {
        <div class="bloque-codigo">
          <div class="codigo-cabecera">
            <span class="titulo-payload">Respuesta esperada (HTTP 200/201 JSON)</span>
            <ap-boton variante="secundario" tamano="sm" (pulsado)="copiarRespuesta()">
              {{ copiadoRespuesta() ? '¡Copiado!' : 'Copiar respuesta' }}
            </ap-boton>
          </div>
          <pre class="visor-json"><code>{{ jsonRespuesta() }}</code></pre>
        </div>
      }
    </section>
  `,
  styles: `
    :host { display: block; width: 100%; }
    .seccion-payload { display: flex; flex-direction: column; gap: var(--s4); }
    .bloque-codigo { display: flex; flex-direction: column; background: var(--fondo-tarjeta); border: var(--borde-fino) solid var(--borde-tarjeta); border-radius: var(--r-md); overflow: hidden; }
    .codigo-cabecera { display: flex; justify-content: space-between; align-items: center; padding: var(--s2) var(--s3); background: var(--fondo-control); border-bottom: var(--borde-fino) solid var(--borde-control); }
    .titulo-payload { font-size: 0.8125rem; font-weight: 600; color: var(--text); }
    .visor-json { margin: 0; padding: var(--s3); overflow-x: auto; font-family: var(--f-mono, monospace); font-size: 0.8125rem; line-height: 1.5; color: var(--brand-texto); background: var(--fondo-panel); }
  `,
})
export class SeccionPayloadEjemplo {
  readonly payloadEntrada = input<unknown>(null)
  readonly payloadRespuesta = input<unknown>(null)

  protected readonly copiadoEntrada = signal(false)
  protected readonly copiadoRespuesta = signal(false)

  protected readonly jsonEntrada = computed(() =>
    this.payloadEntrada() ? JSON.stringify(this.payloadEntrada(), null, 2) : '// No requiere body en la petición (GET / DELETE)'
  )

  protected readonly jsonRespuesta = computed(() =>
    this.payloadRespuesta() ? JSON.stringify(this.payloadRespuesta(), null, 2) : '// Sin contenido de respuesta'
  )

  protected copiarEntrada(): void {
    void navigator.clipboard?.writeText(this.jsonEntrada())
    this.copiadoEntrada.set(true)
    setTimeout(() => this.copiadoEntrada.set(false), 2000)
  }

  protected copiarRespuesta(): void {
    void navigator.clipboard?.writeText(this.jsonRespuesta())
    this.copiadoRespuesta.set(true)
    setTimeout(() => this.copiadoRespuesta.set(false), 2000)
  }
}
