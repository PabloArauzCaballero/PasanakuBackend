import { afterNextRender, ChangeDetectionStrategy, Component, signal } from '@angular/core'
import { Icono } from './icono'

/**
 * «Abrir AportaYa en el navegador». La dirección la pone el servidor en
 * `<meta name="aportaya-app">` (APORTAYA_URL_APP): es de un despliegue, no del código.
 *
 * Se lee después del primer pintado, así el HTML del servidor y el del navegador son
 * iguales al hidratar. Si el entorno no la declara, el botón no aparece: mejor ningún
 * botón que uno que lleva a ninguna parte.
 */
@Component({
  selector: 'ap-enlace-a-la-app',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Icono],
  host: { style: 'display: contents' },
  template: `
    @if (url(); as destino) {
      <a class="boton boton--principal" [href]="destino" rel="noopener">
        <ap-icono nombre="flecha" [tamano]="18" />
        Abrir AportaYa en el navegador
      </a>
    }
  `,
})
export class EnlaceALaApp {
  protected readonly url = signal<string | null>(null)

  constructor() {
    afterNextRender(() => {
      const meta = document.querySelector('meta[name="aportaya-app"]')?.getAttribute('content')
      this.url.set(meta && /^https?:\/\//.test(meta) ? meta : null)
    })
  }
}
