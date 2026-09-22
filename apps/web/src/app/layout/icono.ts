import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core'
import { NombreDeTrazo, TRAZOS } from './trazos'

type Forma = { tipo: 'path' | 'circle' | 'rect'; a: Record<string, string> }

/**
 * Las formas se dibujan como elementos SVG de la plantilla, NO con `innerHTML`. El DOM
 * del servidor (prerender) no implementa `innerHTML` sobre SVG: fallaba con
 * `NotYetImplemented` y el HTML prerenderizado salía con todos los íconos vacíos.
 */
const FORMAS = new Map<NombreDeTrazo, Forma[]>(
  (Object.keys(TRAZOS) as NombreDeTrazo[]).map((nombre) => [
    nombre,
    [...TRAZOS[nombre].matchAll(/<(path|circle|rect)\s([^>]*?)\/>/g)].map(([, tipo, atributos]) => ({
      tipo: tipo as Forma['tipo'],
      a: Object.fromEntries([...atributos!.matchAll(/([\w-]+)="([^"]*)"/g)].map(([, k, v]) => [k!, v!])),
    })),
  ]),
)

/** Un ícono de trazo del sitio. Decorativo: el texto que lo acompaña dice qué significa. */
@Component({
  selector: 'ap-icono',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { style: 'display: contents' },
  template: `
    <svg [attr.width]="tamano()" [attr.height]="tamano()" viewBox="0 0 24 24" fill="none" stroke="currentColor"
      [attr.stroke-width]="grosor()" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false">
      @for (f of formas(); track $index) {
        @switch (f.tipo) {
          @case ('path') { <path [attr.d]="f.a['d']" [attr.fill]="f.a['fill'] ?? null" [attr.stroke]="f.a['stroke'] ?? null" /> }
          @case ('circle') { <circle [attr.cx]="f.a['cx']" [attr.cy]="f.a['cy']" [attr.r]="f.a['r']" [attr.fill]="f.a['fill'] ?? null" [attr.stroke]="f.a['stroke'] ?? null" /> }
          @case ('rect') { <rect [attr.x]="f.a['x']" [attr.y]="f.a['y']" [attr.width]="f.a['width']" [attr.height]="f.a['height']" [attr.rx]="f.a['rx'] ?? null" /> }
        }
      }
    </svg>
  `,
})
export class Icono {
  readonly nombre = input.required<NombreDeTrazo>()
  readonly tamano = input(20)
  readonly grosor = input(2)
  protected readonly formas = computed(() => FORMAS.get(this.nombre()) ?? [])
}
