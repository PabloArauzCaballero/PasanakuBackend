import { ChangeDetectionStrategy, Component } from '@angular/core'
import { RouterLink } from '@angular/router'

/**
 * El símbolo y la palabra, como en la landing y en la app. El aguayo naranja va por clase
 * (`.trazo-acento`, color del token) y no con el hex escrito en el SVG.
 */
@Component({
  selector: 'ap-marca',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink],
  host: { style: 'display: contents' },
  template: `
    <a class="marca" routerLink="/" aria-label="AportaYa, inicio">
      <svg class="simbolo" viewBox="0 0 200 200" aria-hidden="true" focusable="false">
        <g fill="none" stroke-linecap="round">
          <path d="M92 30 C74 66 52 108 40 168" stroke="currentColor" stroke-width="13"/>
          <path d="M108 30 C126 66 148 108 160 168" stroke="currentColor" stroke-width="13"/>
          <path d="M46 158 C86 140 114 140 154 158" stroke="currentColor" stroke-width="12"/>
          <path class="trazo-acento" d="M100 58 C84 92 68 130 62 170" stroke-width="12"/>
          <path class="trazo-acento" d="M100 58 C116 92 132 130 138 170" stroke-width="12"/>
          <path class="trazo-acento" d="M70 150 C100 134 100 134 130 150" stroke-width="10"/>
        </g>
      </svg>
      <span class="palabra">Aporta<span class="ya">Ya</span></span>
    </a>
  `,
})
export class Marca {}
