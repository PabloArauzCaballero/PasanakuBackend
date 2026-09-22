import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core'
import { create } from 'qrcode'

/**
 * QR como SVG en línea (se ve en SSR y se imprime nítido). El contenido no se lee en
 * voz alta: la etiqueta dice **para qué** es el código, y el texto plano va al lado.
 */
@Component({
  selector: 'ap-codigo-qr',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { role: 'img', '[attr.aria-label]': 'etiqueta()' },
  template: `
    <svg [attr.viewBox]="'0 0 ' + lado() + ' ' + lado()" shape-rendering="crispEdges" aria-hidden="true">
      <rect width="100%" height="100%" fill="var(--white)" />
      <path [attr.d]="trazo()" fill="var(--ink)" />
    </svg>
  `,
  styles: `
    :host { display: inline-block; width: 100%; max-width: calc(var(--s7) * 5); padding: var(--s3); border-radius: var(--r-lg); background: var(--white); border: var(--borde-fino) solid var(--border); }
    svg { display: block; width: 100%; height: auto; }
  `,
})
export class CodigoQR {
  readonly contenido = input.required<string>()
  readonly etiqueta = input.required<string>()
  private readonly modulos = computed(() => {
    const qr = create(this.contenido(), { errorCorrectionLevel: 'M' })
    return { lado: qr.modules.size, datos: qr.modules.data as Uint8Array }
  })
  readonly lado = computed(() => this.modulos().lado)
  readonly trazo = computed(() => {
    const { lado, datos } = this.modulos()
    let d = ''
    for (let y = 0; y < lado; y += 1) for (let x = 0; x < lado; x += 1) if (datos[y * lado + x]) d += `M${x} ${y}h1v1h-1z`
    return d
  })
}
